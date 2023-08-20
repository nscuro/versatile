package io.github.nscuro.versatile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.packageurl.PackageURL;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.github.nscuro.versatile.version.InvalidVersionException;
import io.github.nscuro.versatile.version.VersioningScheme;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

class VersOsvTest {

    record ParsedVulnerability(String vulnId, Map<String, List<String>> affectedPackages) {
    }

    @Disabled
    @ParameterizedTest
    @ValueSource(strings = {
            "Debian",
            "Go",
            "Maven",
            "npm"
    })
    void test(final String ecosystem) throws Exception {
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

        final var parsedVulns = new ArrayList<ParsedVulnerability>();
        try (final var zipFile = new ZipFile(response.body().toFile())) {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final DocumentContext jsonPathCtx;
                try (final var entryInputStream = zipFile.getInputStream(entries.nextElement())) {
                    jsonPathCtx = JsonPath.parse(entryInputStream, jsonPathConfig);
                }

                final String vulnId = jsonPathCtx.read("$.id", String.class);
                final ArrayNode affectedWithPurlAndRanges =
                        jsonPathCtx.read("$.affected[?(!@.package.purl.empty && !@.ranges.empty)]");
                if (affectedWithPurlAndRanges.isEmpty()) {
                    continue;
                }

                final var affectedPackages = new HashMap<String, List<String>>();
                for (final JsonNode affected : affectedWithPurlAndRanges) {
                    final PackageURL purl = new PackageURL(affected.get("package").get("purl").asText());
                    final ArrayNode ranges = (ArrayNode) affected.get("ranges");
                    if (ranges == null) {
                        continue;
                    }

                    final Vers.Builder versBuilder = Vers.builder(VersioningScheme.fromPurlType(purl.getType()));
                    for (final JsonNode range : ranges) {
                        if ("GIT".equals(range.get("type").asText())) {
                            continue;
                        }

                        for (final JsonNode event : range.get("events")) {
                            JsonNode versionNode;
                            try {
                                if ((versionNode = event.get("introduced")) != null) {
                                    versBuilder.withConstraint(Comparator.GREATER_THAN_OR_EQUAL, versionNode.asText());
                                } else if ((versionNode = event.get("fixed")) != null) {
                                    versBuilder.withConstraint(Comparator.LESS_THAN, versionNode.asText());
                                } else if ((versionNode = event.get("last_affected")) != null) {
                                    versBuilder.withConstraint(Comparator.LESS_THAN_OR_EQUAL, versionNode.asText());
                                }
                            } catch (InvalidVersionException e) {
                                // Some Debian ranges use non-standard versions like "<end-of-life>" or "<unfixed>"
                                System.out.println(e.getMessage());
                            }
                        }
                    }

                    if (!versBuilder.hasConstraints()) {
                        continue;
                    }

                    affectedPackages
                            .computeIfAbsent(purl.canonicalize(), ignored -> new ArrayList<>())
                            .add(versBuilder.build().validate().toString());
                }

                parsedVulns.add(new ParsedVulnerability(vulnId, affectedPackages));
            }
        }

        try (final var fos = Files.newOutputStream(Path.of("%s.json".formatted(ecosystem)))) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(fos, parsedVulns);
        }
    }
}

