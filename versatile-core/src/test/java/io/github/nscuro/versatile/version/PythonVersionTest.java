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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PythonVersionTest extends AbstractVersionTest {

    @ParameterizedTest
    @CsvSource(value = {
            // Basic release comparisons
            "1.0, IS_LOWER_THAN, 2.0",
            "1.0, IS_EQUAL_TO, 1.0",
            "2.0, IS_HIGHER_THAN, 1.0",
            "1.0.0, IS_EQUAL_TO, 1.0",
            "1.2.3, IS_LOWER_THAN, 1.2.4",
            "1.2.3, IS_LOWER_THAN, 1.3.0",
            "1.2.3, IS_LOWER_THAN, 2.0.0",
            // Epoch comparisons
            "1!1.0, IS_HIGHER_THAN, 2.0",
            "2!1.0, IS_HIGHER_THAN, 1!2.0",
            "1!1.0, IS_EQUAL_TO, 1!1.0",
            // Pre-release comparisons (a < b < rc < final)
            "1.0a1, IS_LOWER_THAN, 1.0",
            "1.0b1, IS_LOWER_THAN, 1.0",
            "1.0rc1, IS_LOWER_THAN, 1.0",
            "1.0a1, IS_LOWER_THAN, 1.0b1",
            "1.0b1, IS_LOWER_THAN, 1.0rc1",
            "1.0a1, IS_LOWER_THAN, 1.0a2",
            "1.0a2, IS_LOWER_THAN, 1.0b1",
            "1.0rc1, IS_LOWER_THAN, 1.0rc2",
            // Post-release comparisons
            "1.0, IS_LOWER_THAN, 1.0.post1",
            "1.0.post1, IS_LOWER_THAN, 1.0.post2",
            "1.0.post1, IS_HIGHER_THAN, 1.0",
            // Dev release comparisons
            "1.0.dev1, IS_LOWER_THAN, 1.0a1",
            "1.0.dev1, IS_LOWER_THAN, 1.0",
            "1.0.dev1, IS_LOWER_THAN, 1.0.dev2",
            "1.0a1.dev1, IS_LOWER_THAN, 1.0a1",
            "1.0a1.dev1, IS_LOWER_THAN, 1.0a1.dev2",
            // Complex ordering (dev < alpha < beta < rc < release < post)
            "1.0.dev456, IS_LOWER_THAN, 1.0a1",
            "1.0a1, IS_LOWER_THAN, 1.0a2.dev456",
            "1.0a2.dev456, IS_LOWER_THAN, 1.0a12.dev456",
            "1.0a12.dev456, IS_LOWER_THAN, 1.0a12",
            "1.0a12, IS_LOWER_THAN, 1.0b2.post345.dev456",
            "1.0b2.post345.dev456, IS_LOWER_THAN, 1.0b2.post345",
            "1.0b2.post345, IS_LOWER_THAN, 1.0rc1.dev456",
            "1.0rc1.dev456, IS_LOWER_THAN, 1.0rc1",
            "1.0rc1, IS_LOWER_THAN, 1.0",
            "1.0, IS_LOWER_THAN, 1.0.post456.dev34",
            "1.0.post456.dev34, IS_LOWER_THAN, 1.0.post456",
            "1.0.post456, IS_LOWER_THAN, 1.1.dev1",
            // Local version comparisons
            "1.0, IS_LOWER_THAN, 1.0+local",
            "1.0+abc, IS_LOWER_THAN, 1.0+def",
            "1.0+local1, IS_LOWER_THAN, 1.0+local2",
            // Normalization equivalence
            "v1.0, IS_EQUAL_TO, 1.0",
            "1.0alpha1, IS_EQUAL_TO, 1.0a1",
            "1.0beta1, IS_EQUAL_TO, 1.0b1",
            "1.0c1, IS_EQUAL_TO, 1.0rc1",
            "1.0-post1, IS_EQUAL_TO, 1.0.post1",
            "1.0-1, IS_EQUAL_TO, 1.0.post1",
            "1.0.dev1, IS_EQUAL_TO, 1.0-dev1",
            // Real-world examples
            "2.0.0, IS_HIGHER_THAN, 2.0.0rc1",
            "3.0.0a1, IS_LOWER_THAN, 3.0.0",
            "1.2.3.post1, IS_HIGHER_THAN, 1.2.3",
            "0.9.0, IS_LOWER_THAN, 1.0.0",
            "1.0.0, IS_LOWER_THAN, 1.0.1",
            "1.11.0, IS_HIGHER_THAN, 1.2.0"
    })
    void testCompareTo(final String versionA, final ComparisonExpectation expectation, final String versionB) {
        expectation.evaluate(new PythonVersion(versionA), new PythonVersion(versionB));
    }

    @Nested
    class ParseTest {

        @Test
        void shouldParseEpoch() {
            final PythonVersion version = new PythonVersion("2!1.0");
            assertThat(version.epoch()).isEqualTo(2);
            assertThat(version.release()).containsExactly(1, 0);
        }

        @Test
        void shouldParseRelease() {
            final PythonVersion version = new PythonVersion("1.2.3");
            assertThat(version.epoch()).isZero();
            assertThat(version.release()).containsExactly(1, 2, 3);
        }

        @Test
        void shouldParsePreRelease() {
            final PythonVersion alpha = new PythonVersion("1.0a1");
            assertThat(alpha.preRelease()).isNotNull();
            assertThat(alpha.preRelease().type()).isEqualTo(PythonVersion.PreRelease.Type.ALPHA);
            assertThat(alpha.preRelease().number()).isEqualTo(1);

            final PythonVersion beta = new PythonVersion("1.0b2");
            assertThat(beta.preRelease()).isNotNull();
            assertThat(beta.preRelease().type()).isEqualTo(PythonVersion.PreRelease.Type.BETA);
            assertThat(beta.preRelease().number()).isEqualTo(2);

            final PythonVersion rc = new PythonVersion("1.0rc3");
            assertThat(rc.preRelease()).isNotNull();
            assertThat(rc.preRelease().type()).isEqualTo(PythonVersion.PreRelease.Type.RC);
            assertThat(rc.preRelease().number()).isEqualTo(3);
        }

        @Test
        void shouldParsePostRelease() {
            final PythonVersion version = new PythonVersion("1.0.post5");
            assertThat(version.postRelease()).isEqualTo(5);
        }

        @Test
        void shouldParseDevRelease() {
            final PythonVersion version = new PythonVersion("1.0.dev7");
            assertThat(version.devRelease()).isEqualTo(7);
        }

        @Test
        void shouldParseLocalVersion() {
            final PythonVersion version = new PythonVersion("1.0+local.version");
            assertThat(version.local()).isEqualTo("local.version");
        }

        @Test
        void shouldParseComplexVersion() {
            final PythonVersion version = new PythonVersion("1!1.2.3a4.post5.dev6+local");
            assertThat(version.epoch()).isEqualTo(1);
            assertThat(version.release()).containsExactly(1, 2, 3);
            assertThat(version.preRelease()).isNotNull();
            assertThat(version.preRelease().type()).isEqualTo(PythonVersion.PreRelease.Type.ALPHA);
            assertThat(version.preRelease().number()).isEqualTo(4);
            assertThat(version.postRelease()).isEqualTo(5);
            assertThat(version.devRelease()).isEqualTo(6);
            assertThat(version.local()).isEqualTo("local");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "abc",
                "1.2.3.a.b.c",
                "1.2-",
                ".1.2",
                "1..2"
        })
        void shouldThrowOnInvalidVersion(final String invalidVersion) {
            assertThatThrownBy(() -> new PythonVersion(invalidVersion))
                    .isInstanceOf(InvalidVersionException.class);
        }

    }

    @Nested
    class NormalizationTest {

        @Test
        void shouldStripLeadingV() {
            final PythonVersion v1 = new PythonVersion("v1.0");
            final PythonVersion v2 = new PythonVersion("1.0");
            assertThat(v1).isEqualByComparingTo(v2);
        }

        @Test
        void shouldNormalizeAlphaSpelling() {
            final PythonVersion v1 = new PythonVersion("1.0alpha1");
            final PythonVersion v2 = new PythonVersion("1.0a1");
            assertThat(v1).isEqualByComparingTo(v2);
        }

        @Test
        void shouldNormalizeBetaSpelling() {
            final PythonVersion v1 = new PythonVersion("1.0beta1");
            final PythonVersion v2 = new PythonVersion("1.0b1");
            assertThat(v1).isEqualByComparingTo(v2);
        }

        @Test
        void shouldNormalizeRcSpelling() {
            final PythonVersion v1 = new PythonVersion("1.0c1");
            final PythonVersion v2 = new PythonVersion("1.0rc1");
            assertThat(v1).isEqualByComparingTo(v2);
        }

        @Test
        void shouldNormalizePostSpelling() {
            final PythonVersion v1 = new PythonVersion("1.0-1");
            final PythonVersion v2 = new PythonVersion("1.0.post1");
            assertThat(v1).isEqualByComparingTo(v2);
        }

    }

    @Nested
    class IsStableTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "1.0",
                "1.2.3",
                "1.2.3.4.5",
                "1.0.post1",
                "2.1"
        })
        void shouldReturnTrueForFinalReleases(final String version) {
            assertThat(new PythonVersion(version).isStable()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "1.0a1",
                "1.0b1",
                "1.0rc1",
                "1.0.dev1",
                "1.0+local",
                "1.0a1.dev1"
        })
        void shouldReturnFalseForPreDevAndLocalReleases(final String version) {
            assertThat(new PythonVersion(version).isStable()).isFalse();
        }

    }

    @Test
    void testToString() {
        assertThat(new PythonVersion("1.2.3").toString()).isEqualTo("1.2.3");
    }

}
