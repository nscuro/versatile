/*
 * This file is part of versatile.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Niklas Düster. All Rights Reserved.
 */
package io.github.nscuro.versatile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.github.jeremylong.openvulnerability.client.ghsa.GitHubSecurityAdvisoryClient;
import io.github.jeremylong.openvulnerability.client.ghsa.SecurityAdvisory;
import io.github.jeremylong.openvulnerability.client.ghsa.Vulnerabilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static io.github.jeremylong.openvulnerability.client.ghsa.GitHubSecurityAdvisoryClientBuilder.aGitHubSecurityAdvisoryClient;
import static io.github.nscuro.versatile.VersUtils.versFromGhsaRange;
import static io.github.nscuro.versatile.VersUtils.versFromOsvRange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER;

public class VersUtilsIT {

    @BeforeAll
    static void beforeAll() throws Exception {
        Files.createDirectory(Paths.get("target/results"));
    }

    @Test
    void testVersFromGhsaRangeWithAllRanges() throws Exception {
        assumeTrue(System.getenv("GITHUB_TOKEN") != null, "GITHUB_TOKEN must be set");

        final GitHubSecurityAdvisoryClient ghsaClient = aGitHubSecurityAdvisoryClient()
                .withApiKey(System.getenv("GITHUB_TOKEN"))
                .build();

        final var maxResults = 1000; // The entire advisory DB is too large.
        final var objectMapper = new ObjectMapper();
        final var resultsArrayNode = objectMapper.createArrayNode();
        final var failuresArrayNode = objectMapper.createArrayNode();

        try (ghsaClient) {
            while (ghsaClient.hasNext()) {
                Collection<SecurityAdvisory> result = ghsaClient.next();
                if (result == null || result.isEmpty()) {
                    continue;
                }

                result.stream()
                        .map(SecurityAdvisory::getVulnerabilities)
                        .map(Vulnerabilities::getEdges)
                        .flatMap(Collection::stream)
                        .forEach(edge -> {
                            final ObjectNode objectNode = objectMapper.createObjectNode()
                                    .put("ecosystem", edge.getPackage().getEcosystem())
                                    .put("package", edge.getPackage().getName())
                                    .put("range", edge.getVulnerableVersionRange());

                            try {
                                final Vers vers = versFromGhsaRange(edge.getPackage().getEcosystem(), edge.getVulnerableVersionRange())
                                        .simplify()
                                        .validate();
                                resultsArrayNode.add(objectNode.put("vers", vers.toString()));
                            } catch (RuntimeException e) {
                                failuresArrayNode.add(objectNode.put("failureReason", e.getMessage()));
                            }
                        });

                if (resultsArrayNode.size() + failuresArrayNode.size() >= maxResults) {
                    break;
                }
            }
        }

        try (final var resultsFileOutputStream = Files.newOutputStream(Paths.get("target/results/ghsa.json"));
             final var failuresFileOutputStream = Files.newOutputStream(Paths.get("target/results/ghsa-failures.json"))) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(resultsFileOutputStream, resultsArrayNode);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(failuresFileOutputStream, failuresArrayNode);
        }

        assertThat(failuresArrayNode).isEmpty();
    }

    @ParameterizedTest(name = ARGUMENTS_WITH_NAMES_PLACEHOLDER)
    @ValueSource(strings = {
            "AlmaLinux",
            "Alpine",
            "crates.io",
            "Debian",
            "Go",
            "Maven",
            "npm",
            "NuGet",
            "Packagist",
            "PyPI",
            "Rocky Linux",
            "RubyGems",
            "Ubuntu"
    })
    void testVersFromOsvRangeWithAllRanges(final String ecosystem) throws Exception {
        final Path tempFile = Files.createTempFile(null, null);

        final HttpRequest request = HttpRequest.newBuilder().GET()
                .uri(URI.create("https://osv-vulnerabilities.storage.googleapis.com/%s/all.zip".formatted(ecosystem.replace(" ", "%20"))))
                .build();
        final HttpResponse<Path> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofFile(tempFile));
        assertThat(response.statusCode()).isEqualTo(200);

        final var objectMapper = new ObjectMapper();
        final var jsonPathConfig = Configuration.defaultConfiguration()
                .jsonProvider(new JacksonJsonNodeJsonProvider(objectMapper))
                .mappingProvider(new JacksonMappingProvider(objectMapper));

        final var resultsArrayNode = objectMapper.createArrayNode();
        final var failuresArrayNode = objectMapper.createArrayNode();

        try (final var zipFile = new ZipFile(response.body().toFile())) {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final DocumentContext jsonPathCtx;
                try (final var entryInputStream = zipFile.getInputStream(entries.nextElement())) {
                    jsonPathCtx = JsonPath.parse(entryInputStream, jsonPathConfig);
                }

                final ArrayNode affectedWithPurlAndRanges =
                        jsonPathCtx.read("$.affected[?(!@.package.purl.empty && !@.ranges.empty)]");
                if (affectedWithPurlAndRanges.isEmpty()) {
                    continue;
                }

                for (final JsonNode affected : affectedWithPurlAndRanges) {
                    final ArrayNode ranges = (ArrayNode) affected.get("ranges");
                    if (ranges == null) {
                        continue;
                    }

                    for (final JsonNode range : ranges) {
                        if ("GIT".equals(range.get("type").asText())) {
                            continue;
                        }

                        final var events = new ArrayList<Map.Entry<String, String>>();
                        for (final JsonNode eventNode : range.get("events")) {
                            final String fieldName = eventNode.fieldNames().next();
                            events.add(Map.entry(fieldName, eventNode.get(fieldName).asText()));
                        }

                        final ObjectNode objectNode = objectMapper.createObjectNode()
                                .put("name", affected.get("package").get("name").asText())
                                .putPOJO("events", events);

                        try {
                            final Vers vers = versFromOsvRange(range.get("type").asText(), ecosystem, events, null)
                                    .simplify()
                                    .validate();

                            resultsArrayNode.add(objectNode.put("vers", vers.toString()));
                        } catch (RuntimeException e) {
                            failuresArrayNode.add(objectNode.put("failureReason", e.getMessage()));
                        }
                    }
                }
            }
        }

        try (final var resultsFileOutputStream = Files.newOutputStream(Paths.get("target/results/osv-%s.json".formatted(ecosystem)));
             final var failuresFileOutputStream = Files.newOutputStream(Paths.get("target/results/osv-%s-failures.json".formatted(ecosystem)))) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(resultsFileOutputStream, resultsArrayNode);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(failuresFileOutputStream, failuresArrayNode);
        }

        assertThat(failuresArrayNode).isEmpty();
    }

}
