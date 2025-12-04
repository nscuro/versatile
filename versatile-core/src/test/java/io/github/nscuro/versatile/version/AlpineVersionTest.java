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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AlpineVersionTest extends AbstractVersionTest {

    @ParameterizedTest
    @CsvSource(value = {
            // Basic numeric comparisons
            "0, IS_LOWER_THAN, 1",
            "1, IS_EQUAL_TO, 1",
            "1.0, IS_LOWER_THAN, 1.1",
            "1.2, IS_HIGHER_THAN, 1.1",
            "1.9, IS_LOWER_THAN, 1.10",
            // Real Alpine package versions
            "5.0.0-r0, IS_EQUAL_TO, 5.0.0-r0",
            "5.0.0-r0, IS_LOWER_THAN, 5.0.0-r1",
            "5.0.0-r0, IS_LOWER_THAN, 5.0.1-r0",
            "4.3.46-r5, IS_LOWER_THAN, 5.0.0-r0",
            "3.2.3-r4, IS_LOWER_THAN, 7.78.0-r0",
            "2.12.6-r0, IS_LOWER_THAN, 2.12.6-r1",
            "11.9-r0, IS_LOWER_THAN, 12.4-r0",
            // Multi-digit version components
            "1.2.3, IS_LOWER_THAN, 1.2.10",
            "1.2.3, IS_LOWER_THAN, 1.10.3",
            "9.1.1566-r0, IS_HIGHER_THAN, 9.1.100-r0",
            // Pre-release suffixes (alpha < beta < pre < rc < stable)
            "1.0_alpha, IS_LOWER_THAN, 1.0_beta",
            "1.0_beta, IS_LOWER_THAN, 1.0_pre",
            "1.0_pre, IS_LOWER_THAN, 1.0_rc",
            "1.0_rc, IS_LOWER_THAN, 1.0",
            "1.0_alpha, IS_LOWER_THAN, 1.0",
            "1.0_beta, IS_LOWER_THAN, 1.0",
            "1.0_pre, IS_LOWER_THAN, 1.0",
            // Pre-release with numbers
            "1.0_alpha1, IS_EQUAL_TO, 1.0_alpha1",
            "1.0_alpha1, IS_LOWER_THAN, 1.0_alpha2",
            "1.0_rc1, IS_LOWER_THAN, 1.0_rc2",
            "1.0_rc2, IS_LOWER_THAN, 1.0",
            // Version control suffixes
            "1.0_cvs, IS_HIGHER_THAN, 1.0",
            "1.0_svn, IS_HIGHER_THAN, 1.0",
            "1.0_git, IS_HIGHER_THAN, 1.0",
            "1.0_hg, IS_HIGHER_THAN, 1.0",
            // Letters in versions
            "1.0a, IS_LOWER_THAN, 1.0b",
            "1.0b, IS_LOWER_THAN, 1.0c",
            "1.0z, IS_LOWER_THAN, 1.1",
            // Commit hashes
            "1.0~abc123, IS_EQUAL_TO, 1.0~abc123",
            "1.0~abc123, IS_LOWER_THAN, 1.0~def456",
            "1.0, IS_LOWER_THAN, 1.0~abc123",
            // Revisions
            "1.0-r0, IS_LOWER_THAN, 1.0-r1",
            "1.0-r1, IS_LOWER_THAN, 1.0-r10",
            "1.0-r9, IS_LOWER_THAN, 1.0-r10",
            // Complex version strings
            "1.2.3_rc1-r0, IS_LOWER_THAN, 1.2.3-r0",
            "1.2.3-r0, IS_LOWER_THAN, 1.2.4_alpha-r0",
            "1.0_alpha~git123-r0, IS_LOWER_THAN, 1.0-r0",
            // Leading zeros (lexicographic comparison as per Gentoo spec)
            "01, IS_HIGHER_THAN, 001",
            "1.01, IS_HIGHER_THAN, 1.001",
            // Edge cases
            "0.0.0, IS_LOWER_THAN, 0.0.1",
            "0.0.0-r0, IS_LOWER_THAN, 0.0.0-r1",
            "0.1, IS_LOWER_THAN, 1.0"
    })
    void testCompareTo(final String versionA, final ComparisonExpectation expectation, final String versionB) {
        expectation.evaluate(new AlpineVersion(versionA), new AlpineVersion(versionB));
    }

    @ParameterizedTest
    @CsvSource({
            "1.0.0-r0, true",
            "1.0.0, true",
            "1.0_alpha, false",
            "1.0_beta, false",
            "1.0_pre, false",
            "1.0_rc, false",
            "1.0_rc1-r0, false",
            "1.0_git, true",
            "1.0_svn, true"
    })
    void testIsStable(final String version, final boolean expected) {
        assertThat(new AlpineVersion(version).isStable()).isEqualTo(expected);
    }

    @Test
    void testToString() {
        final var version = new AlpineVersion("1.2.3-r4");
        assertThat(version.toString()).isEqualTo("1.2.3-r4");
    }

}
