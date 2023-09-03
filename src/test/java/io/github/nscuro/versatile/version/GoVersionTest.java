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

import io.github.nscuro.versatile.version.AbstractVersionTest.ComparisonExpectation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class GoVersionTest {

    @Test
    void testParseWithMajor() {
        final var version = new GoVersion("v1");
        assertThat(version.major()).isEqualTo("1");
        assertThat(version.minor()).isEqualTo("0");
        assertThat(version.patch()).isEqualTo("0");
        assertThat(version.prerelease()).isNull();
        assertThat(version.build()).isNull();
    }

    @Test
    void testParseWithMajorMinor() {
        final var version = new GoVersion("v1.2");
        assertThat(version.major()).isEqualTo("1");
        assertThat(version.minor()).isEqualTo("2");
        assertThat(version.patch()).isEqualTo("0");
        assertThat(version.prerelease()).isNull();
        assertThat(version.build()).isNull();
    }

    @Test
    void testParseWithMajorMinorPatch() {
        final var version = new GoVersion("v1.2.3");
        assertThat(version.major()).isEqualTo("1");
        assertThat(version.minor()).isEqualTo("2");
        assertThat(version.patch()).isEqualTo("3");
        assertThat(version.prerelease()).isNull();
        assertThat(version.build()).isNull();
    }

    @Test
    void testParseWithoutPrefix() {
        final var version = new GoVersion("1.2.3");
        assertThat(version.major()).isEqualTo("1");
        assertThat(version.minor()).isEqualTo("2");
        assertThat(version.patch()).isEqualTo("3");
        assertThat(version.prerelease()).isNull();
        assertThat(version.build()).isNull();
    }

    @Test
    void testParseWithBuild() {
        final var version = new GoVersion("v1.2.3+meta-pre.sha.256a");
        assertThat(version.major()).isEqualTo("1");
        assertThat(version.minor()).isEqualTo("2");
        assertThat(version.patch()).isEqualTo("3");
        assertThat(version.prerelease()).isNull();
        assertThat(version.build()).isEqualTo("+meta-pre.sha.256a");
    }

    @Test
    void testParseWithPseudoVersion() {
        final var version = new GoVersion("v0.0.0-20210617225240-d185dfc1b5a1");
        assertThat(version.major()).isEqualTo("0");
        assertThat(version.minor()).isEqualTo("0");
        assertThat(version.patch()).isEqualTo("0");
        assertThat(version.prerelease()).isEqualTo("-20210617225240-d185dfc1b5a1");
        assertThat(version.build()).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "v1.0.0-alpha, v1.0.0-alpha.1, SMALLER",
            "v1.0.0-alpha.1, v1.0.0-alpha.beta, SMALLER",
            "v1.0.0-alpha.beta, v1.0.0-beta.2, SMALLER",
            "v1.0.0-beta.2, v1.0.0-beta.11, SMALLER",
            "v1.0.0-beta.11, v1.0.0-rc.1, SMALLER",
            "v1.0.0-rc.1, v1, SMALLER",
            "v1, v1.0, EQUAL",
            "v1.0, v1.0.0, EQUAL",
            "v1.0.0, v1.2, SMALLER",
            "v1.2, v1.2.0, EQUAL",
            "v1.2.0, v1.2.3-456, SMALLER",
            "v1.2.3-456, v1.2.3-456.789, SMALLER",
            "v1.2.3-456.789, v1.2.3-456-789, SMALLER",
            "v1.2.3-456-789, v1.2.3-456a, SMALLER",
            "v1.2.3-456a, v1.2.3-pre, SMALLER",
            "v1.2.3-pre, v1.2.3-pre+meta, EQUAL",
            "v1.2.3-pre+meta, v1.2.3-pre.1, SMALLER",
            "v1.2.3-pre.1, v1.2.3-zzz, SMALLER",
            "v1.2.3-zzz, v1.2.3, SMALLER",
            "v1.2.3, v1.2.3+meta, EQUAL",
            "v1.2.3+meta, v1.2.3+meta-pre, EQUAL",
            "v1.2.3+meta-pre, v1.2.3+meta-pre.sha.256a, EQUAL",
    })
    void testCompareTo(final String versionA, final String versionB, final ComparisonExpectation expectation) {
        expectation.evaluate(new GoVersion(versionA), new GoVersion(versionB));
    }

}