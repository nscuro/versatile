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
 * Copyright (c) AboutCode, and contributors. All Rights Reserved.
 */

package io.github.nscuro.versatile;

import static org.assertj.core.api.Assertions.assertThat;
import io.github.nscuro.versatile.spi.Version;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

class VersSchemaBasedTest {

    private static Stream<Object[]> loadTestData(String fileName) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream inputStream = VersSchemaBasedTest.class.getClassLoader()
                .getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException(fileName + " not found in /vers-spec/tests/.");
            }

            JsonNode rootNode = objectMapper.readTree(inputStream);
            JsonNode testsNode = rootNode.get("tests");
            return StreamSupport.stream(testsNode.spliterator(), false)
                    .map(test -> new Object[] {
                            test.get("description").asText(),
                            test.get("test_group").asText(),
                            test.get("test_type").asText(),
                            test.get("input"),
                            test.get("expected_output")
                    });
        }
    }

    public static List<Version> getVersionsList(JsonNode versions, String scheme) {
        List<Version> versionsList = new ArrayList<>();

        for (JsonNode version : versions) {
            versionsList.add(VersionFactory.forScheme(scheme, version.asText()));
        }

        return versionsList;
    }

    public static Boolean comparison(JsonNode expectedOutput, String scheme) {
        final var expectedVersions = getVersionsList(expectedOutput, scheme);
        Version firstVersion = expectedVersions.get(0);
        Version secondVersion = expectedVersions.get(1);

        return firstVersion.compareTo(secondVersion) <= 0;
    }

    public static Boolean equality(JsonNode inputVersions, JsonNode expectedOutput, String scheme) {
        final var versions = getVersionsList(inputVersions, scheme);

        return versions.get(0).equals(versions.get(1)) == expectedOutput.asBoolean();
    }

    public static Boolean result(String description, String testGroup, String testType, JsonNode input,
            JsonNode expectedOutput) throws Exception {
        String scheme = input.get("input_scheme").asText();
        JsonNode versions = input.get("versions");

        if (testType.equals("comparison")) {
            return comparison(expectedOutput, scheme);
        } else if (testType.equals("equality")) {
            return equality(versions, expectedOutput, scheme);
        }
        throw new Exception("Unsupported test type " + testType);
    }

    private static Stream<Object[]> loadMavenTestData() throws IOException {
        return loadTestData("vers-spec/tests/maven_version_cmp_test.json");
    }

    @ParameterizedTest
    @MethodSource("loadMavenTestData")
    void testMavenComparison(String description, String testGroup, String testType, JsonNode input,
            JsonNode expectedOutput) throws Exception {

        boolean result = VersSchemaBasedTest.result(description, testGroup, testType, input, expectedOutput);

        assertThat(result).isTrue();

    }
}
