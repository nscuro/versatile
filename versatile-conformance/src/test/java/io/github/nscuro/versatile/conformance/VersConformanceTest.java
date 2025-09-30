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
package io.github.nscuro.versatile.conformance;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nscuro.versatile.VersionFactory;
import io.github.nscuro.versatile.conformance.schema.VersTest;
import io.github.nscuro.versatile.conformance.schema.VersTestSchema;
import io.github.nscuro.versatile.spi.Version;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class VersConformanceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersConformanceTest.class);

    private static Stream<Arguments> testVersConformanceArguments() throws IOException {
        final var testFilePaths = new HashSet<Path>();

        Files.walkFileTree(Path.of("./vers-spec/tests"), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                if (!attrs.isDirectory()
                        && file.getFileName().toString().endsWith("_test.json")
                        // TODO: Enable more schemes.
                        && file.getFileName().toString().startsWith("maven_")) {
                    testFilePaths.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        final var versTests = new ArrayList<VersTest>();
        final var objectMapper = new ObjectMapper();

        for (final Path testFilePath : testFilePaths) {
            try {
                final var versTestsObject = objectMapper.readValue(testFilePath.toFile(), VersTestSchema.class);
                versTests.addAll(versTestsObject.getTests());
            } catch (IOException e) {
                LOGGER.warn("Failed to deserialize test file {}", testFilePath, e);
            }
        }

        return versTests.stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("testVersConformanceArguments")
    void testVersConformance(final VersTest versTest) {
        switch (versTest.getTestType()) {
            case COMPARISON -> testComparison(versTest);
            case EQUALITY -> testEquality(versTest);
            default -> Assumptions.assumeTrue(true, "Test type not supported yet");
        }
    }

    @SuppressWarnings("unchecked")
    void testComparison(final VersTest versTest) {
        assertThat(versTest.getAdditionalProperties()).isNotNull();

        final var inputObject = (Map<String, Object>) versTest.getAdditionalProperties().get("input");
        assertThat(inputObject).isNotNull();

        final String inputScheme = (String) inputObject.get("input_scheme");
        assertThat(inputScheme).isNotNull();

        final var versions = (List<String>) inputObject.get("versions");
        assertThat(versions).isNotNull();
        assertThat(versions).hasSize(2);

        final var expectedOutput = (List<String>) versTest.getAdditionalProperties().get("expected_output");
        assertThat(expectedOutput).isNotNull();

        final List<String> sortedVersions = Stream.of(
                        VersionFactory.forScheme(inputScheme, versions.get(0)),
                        VersionFactory.forScheme(inputScheme, versions.get(1)))
                .sorted()
                .map(Version::toString)
                .toList();

        assertThat(sortedVersions)
                .as(versTest.getDescription())
                .isEqualTo(expectedOutput);
    }

    @SuppressWarnings("unchecked")
    void testEquality(final VersTest versTest) {
        assertThat(versTest.getAdditionalProperties()).isNotNull();

        final var inputObject = (Map<String, Object>) versTest.getAdditionalProperties().get("input");
        assertThat(inputObject).isNotNull();

        final String inputScheme = (String) inputObject.get("input_scheme");
        assertThat(inputScheme).isNotNull();

        final var versions = (List<String>) inputObject.get("versions");
        assertThat(versions).isNotNull();
        assertThat(versions).hasSize(2);

        final var expectedOutput = (Boolean) versTest.getAdditionalProperties().get("expected_output");
        assertThat(expectedOutput).isNotNull();

        final Version versionA = VersionFactory.forScheme(inputScheme, versions.get(0));
        final Version versionB = VersionFactory.forScheme(inputScheme, versions.get(1));

        if (expectedOutput) {
            assertThat(versionA)
                    .as(versTest.getDescription())
                    .isEqualTo(versionB);
        } else {
            assertThat(versionA)
                    .as(versTest.getDescription())
                    .isNotEqualTo(versionB);
        }
    }

}
