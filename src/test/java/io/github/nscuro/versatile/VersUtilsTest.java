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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.github.jeremylong.openvulnerability.client.ghsa.GitHubSecurityAdvisoryClient;
import io.github.jeremylong.openvulnerability.client.ghsa.SecurityAdvisory;
import io.github.jeremylong.openvulnerability.client.ghsa.Vulnerabilities;
import io.github.nscuro.versatile.version.VersioningScheme;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static io.github.jeremylong.openvulnerability.client.ghsa.GitHubSecurityAdvisoryClientBuilder.aGitHubSecurityAdvisoryClient;
import static io.github.nscuro.versatile.VersUtils.schemeFromGhsaEcosystem;
import static io.github.nscuro.versatile.VersUtils.schemeFromOsvEcosystem;
import static io.github.nscuro.versatile.VersUtils.versFromGhsaRange;
import static io.github.nscuro.versatile.VersUtils.versFromNvdRange;
import static io.github.nscuro.versatile.VersUtils.versFromOsvRange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class VersUtilsTest {

    @ParameterizedTest
    @CsvSource(value = {
            "> 1.2.3, vers:generic/>1.2.3",
            ">= 1.2.3, vers:generic/>=1.2.3",
            "= 1.2.3, vers:generic/1.2.3",
            "'> 1.2.3, <= 3.2.1', vers:generic/>1.2.3|<=3.2.1",
            "'<= 3.2.1, > 1.2.3', vers:generic/>1.2.3|<=3.2.1",
            "<= 3.2.1, vers:generic/<=3.2.1",
            "< 3.2.1, vers:generic/<3.2.1",

    })
    void testVersFromGhsaRange(final String ghsaRange, final String expectedVers) {
        assertThat(versFromGhsaRange("other", ghsaRange)).hasToString(expectedVers);
    }

    private static Stream<Arguments> testVersFromOsvRangeArguments() {
        return Stream.of(
                arguments(
                        List.of(Map.entry("introduced", "1.2.3")),
                        "vers:generic/>=1.2.3"
                ),
                arguments(
                        List.of(Map.entry("introduced", "1.2.3"), Map.entry("fixed", "3.2.1")),
                        "vers:generic/>=1.2.3|<3.2.1"
                ),
                arguments(
                        List.of(Map.entry("introduced", "1.2.3"), Map.entry("last_affected", "3.2.1")),
                        "vers:generic/>=1.2.3|<=3.2.1"
                ),
                arguments(
                        List.of(Map.entry("last_affected", "3.2.1"), Map.entry("introduced", "1.2.3")),
                        "vers:generic/>=1.2.3|<=3.2.1"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testVersFromOsvRangeArguments")
    void testVersFromOsvRange(final List<Map.Entry<String, String>> events, final String expectedVers) {
        assertThat(versFromOsvRange("ecosystem", "other", events)).hasToString(expectedVers);
    }

    @Test
    void testVersFromOsvRangeWithInvalidRangeType() {
        final List<Map.Entry<String, String>> events = List.of(Map.entry("introduced", "0"));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> versFromOsvRange(null, "other", events));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> versFromOsvRange("", "other", events));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> versFromOsvRange("git", "other", events));
        assertThatNoException().isThrownBy(() -> versFromOsvRange("ecosystem", "other", events));
        assertThatNoException().isThrownBy(() -> versFromOsvRange("semver", "other", events));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "actions, GENERIC",
            "composer, GENERIC",
            "erlang, GENERIC",
            "go, GOLANG",
            "maven, MAVEN",
            "npm, NPM",
            "nuget, NUGET",
            "other, GENERIC",
            "pip, PYPI",
            "pub, GENERIC",
            "rubygems, GEM",
            "rust, GENERIC",
            "foo, GENERIC",
    })
    void testSchemeFromGhsaEcosystem(final String ecosystem, final VersioningScheme expectedScheme) {
        assertThat(schemeFromGhsaEcosystem(ecosystem)).isEqualTo(expectedScheme);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "AlmaLinux, GENERIC",
            "Alpine, ALPINE",
            "Android, GENERIC",
            "Bioconductor, GENERIC",
            "Bitnami, GENERIC",
            "CRAN, GENERIC",
            "ConanCenter, GENERIC",
            "Debian, DEB",
            "GHC, GENERIC",
            "GitHub Actions, GENERIC",
            "Go, GOLANG",
            "Hackage, GENERIC",
            "Hex, GENERIC",
            "Linux, GENERIC",
            "Maven, MAVEN",
            "OSS-Fuzz, GENERIC",
            "Packagist, GENERIC",
            "Photon OS, GENERIC",
            "Pub, GENERIC",
            "PyPI, PYPI",
            "Rocky Linux, GENERIC",
            "RubyGems, GEM",
            "SwiftURL, GENERIC",
            "crates.io, GENERIC",
            "npm, NPM",
    })
    void testSchemeFromOsvEcosystem(final String ecosystem, final VersioningScheme expectedScheme) {
        assertThat(schemeFromOsvEcosystem(ecosystem)).isEqualTo(expectedScheme);
    }

    @Test
    @Disabled
    void testVersFromGhsaRangeWithAllRanges() throws Exception {
        assumeTrue(System.getenv("GITHUB_TOKEN") != null, "GITHUB_TOKEN must be set");

        final GitHubSecurityAdvisoryClient ghsaClient = aGitHubSecurityAdvisoryClient()
                .withApiKey(System.getenv("GITHUB_TOKEN"))
                .build();

        final var arrayNode = JsonNodeFactory.instance.arrayNode();

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
                        .map(edge -> JsonNodeFactory.instance.objectNode()
                                .put("ecosystem", edge.getPackage().getEcosystem())
                                .put("package", edge.getPackage().getName())
                                .put("range", edge.getVulnerableVersionRange())
                                .put("vers", versFromGhsaRange(edge.getPackage().getEcosystem(), edge.getVulnerableVersionRange()).toString()))
                        .forEach(arrayNode::add);
            }
        }

        try (final var fos = Files.newOutputStream(Paths.get("ghsa.json"))) {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fos, arrayNode);
        }
    }

    @Disabled
    @ParameterizedTest
    @ValueSource(strings = {
            "Debian",
            "Go",
            "Maven",
            "npm"
    })
    void testVersFromOsvRangeWithAllRanges(final String ecosystem) throws Exception {
        final Path tempFile = Files.createTempFile(null, null);

        final HttpRequest request = HttpRequest.newBuilder().GET()
                .uri(URI.create("https://osv-vulnerabilities.storage.googleapis.com/%s/all.zip".formatted(ecosystem)))
                .build();
        final HttpResponse<Path> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofFile(tempFile));
        assertThat(response.statusCode()).isEqualTo(200);

        final var objectMapper = new ObjectMapper();
        final var jsonPathConfig = Configuration.defaultConfiguration()
                .jsonProvider(new JacksonJsonNodeJsonProvider(objectMapper))
                .mappingProvider(new JacksonMappingProvider(objectMapper));

        final var arrayNode = objectMapper.createArrayNode();

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

                        try {
                            final Vers vers = versFromOsvRange(range.get("type").asText(), ecosystem, events);
                            arrayNode.add(objectMapper.createObjectNode()
                                    .put("name", affected.get("package").get("name").asText())
                                    .putPOJO("events", events)
                                    .put("vers", vers.toString()));
                        } catch (VersException e) {
                            System.out.println("Failed to convert range %s: %s".formatted(range, e));
                        }
                    }
                }
            }
        }

        try (final var fos = Files.newOutputStream(Path.of("osv-%s.json".formatted(ecosystem)))) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(fos, arrayNode);
        }
    }

    private static Stream<Arguments> testVersFromNvdRangeArguments() {
        return Stream.of(
                arguments(
                        null, "2.2.0", null, "2.2.13", "*",
                        "vers:generic/>=2.2.0|<=2.2.13"
                ),
                arguments(
                        null, null, null, null, "6.0.7",
                        "vers:generic/6.0.7"
                ),
                arguments(
                        null, null, null, null, "*",
                        "vers:generic/*"
                ),
                arguments(
                        null, "2.2.0", null, null, "6.0.7",
                        "vers:generic/>=2.2.0"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testVersFromNvdRangeArguments")
    void testVersFromNvdRange(final String versionStartExcluding, final String versionStartIncluding,
                              final String versionEndExcluding, final String versionEndIncluding,
                              final String exactVersion, final String expectedVers) {
        assertThat(versFromNvdRange(versionStartExcluding, versionStartIncluding, versionEndExcluding, versionEndIncluding, exactVersion))
                .hasToString(expectedVers);
    }
}