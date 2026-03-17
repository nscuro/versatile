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
package io.github.nscuro.versatile.version;

import io.github.nscuro.versatile.spi.InvalidVersionException;
import io.github.nscuro.versatile.version.AbstractVersionTest.ComparisonExpectation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NugetVersionTest {

    @Test
    void testParseWithMajorOnly() {
        final var version = new NugetVersion("3");
        assertThat(version.toString()).isEqualTo("3.0.0");
        assertThat(version.isStable()).isTrue();
    }

    @Test
    void testParseWithMajorMinor() {
        final var version = new NugetVersion("1.2");
        assertThat(version.toString()).isEqualTo("1.2.0");
        assertThat(version.isStable()).isTrue();
    }

    @Test
    void testParseWithMajorMinorPatch() {
        final var version = new NugetVersion("1.2.3");
        assertThat(version.toString()).isEqualTo("1.2.3");
        assertThat(version.isStable()).isTrue();
    }

    @Test
    void testParseWithRevision() {
        final var version = new NugetVersion("1.2.3.4");
        assertThat(version.toString()).isEqualTo("1.2.3.4");
        assertThat(version.isStable()).isTrue();
    }

    @Test
    void testParseWithRevisionZero() {
        final var version = new NugetVersion("1.0.0.0");
        assertThat(version.toString()).isEqualTo("1.0.0");
    }

    @Test
    void testParseWithPrerelease() {
        final var version = new NugetVersion("1.0.0-beta.1");
        assertThat(version.toString()).isEqualTo("1.0.0-beta.1");
        assertThat(version.isStable()).isFalse();
    }

    @Test
    void testParseWithMetadata() {
        final var version = new NugetVersion("1.0.0+build123");
        assertThat(version.toString()).isEqualTo("1.0.0+build123");
        assertThat(version.isStable()).isTrue();
    }

    @Test
    void testParseWithPrereleaseAndMetadata() {
        final var version = new NugetVersion("1.0.0-beta+build");
        assertThat(version.toString()).isEqualTo("1.0.0-beta+build");
        assertThat(version.isStable()).isFalse();
    }

    @Test
    void testParseLeadingZeros() {
        final var version = new NugetVersion("1.0.01");
        assertThat(version.toString()).isEqualTo("1.0.1");
    }

    @Test
    void testParseRealWorldPreview() {
        final var version = new NugetVersion("9.0.0-preview.1.24080.9");
        assertThat(version.toString()).isEqualTo("9.0.0-preview.1.24080.9");
        assertThat(version.isStable()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a.b.c", "1.2.3.4.5", "1.0.0-", "1.0.0-beta.", "1.0.0-beta..1"})
    void testParseInvalid(final String versionStr) {
        assertThatThrownBy(() -> new NugetVersion(versionStr))
                .isInstanceOf(InvalidVersionException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {
            // Basic numeric ordering
            "0.0.0, IS_LOWER_THAN, 1.0.0",
            "1.0.0, IS_LOWER_THAN, 1.1.0",
            "1.0.0, IS_LOWER_THAN, 1.0.1",
            "1.999.9999, IS_LOWER_THAN, 2.1.1",

            // Revision
            "1.0.0, IS_LOWER_THAN, 1.0.0.1",
            "1.0.0.0, IS_EQUAL_TO, 1.0.0",
            "0.9.9.1, IS_LOWER_THAN, 1.0.0",

            // Prerelease sorts lower than release
            "1.0.0-alpha, IS_LOWER_THAN, 1.0.0",
            "1.0.0-beta+AA, IS_LOWER_THAN, 1.0.0+aa",

            // Prerelease ordering
            "1.0.0-alpha, IS_LOWER_THAN, 1.0.0-beta",
            "1.0.0-BETA, IS_LOWER_THAN, 1.0.0-beta2",
            "1.0.0-beta, IS_LOWER_THAN, 1.0.0-beta.1",
            "1.0.0-beta.1, IS_LOWER_THAN, 1.0.0-beta.2",
            "1.0.0-1, IS_LOWER_THAN, 1.0.0-alpha",

            // Case-insensitive prerelease
            "1.0.0-BETA, IS_EQUAL_TO, 1.0.0-beta",
            "1.0.0-BETA.X.y.5.77.0+AA, IS_EQUAL_TO, 1.0.0-beta.x.y.5.77.0+aa",

            // Metadata ignored for comparison
            "1.0.0, IS_EQUAL_TO, 1.0.0+beta",
            "1.0.0+aa, IS_EQUAL_TO, 1.0.0+bb",

            // Defaults / leading zeros
            "1.0, IS_EQUAL_TO, 1.0.0.0",
            "1.0.01, IS_EQUAL_TO, 1.0.1.0",

            // Revision + prerelease interaction
            "1.0.0-pre, IS_LOWER_THAN, 1.0.0.1-alpha",
            "1.0.0, IS_LOWER_THAN, 1.0.0.1-alpha",
            "1.0.0.1-alpha, IS_LOWER_THAN, 1.0.0.1-pre",

            // Numeric labels sort before alphanumeric
            "1.0.0-5, IS_LOWER_THAN, 1.0.0-alpha",
            "1.0.0-beta.5.77.0, IS_LOWER_THAN, 1.0.0-beta.5.79.0",
            "1.0.0-beta.5.79.0, IS_LOWER_THAN, 1.0.0-beta.5.790.0",
    })
    void testCompareTo(String versionA, ComparisonExpectation expectation, String versionB) {
        expectation.evaluate(new NugetVersion(versionA), new NugetVersion(versionB));
    }

}
