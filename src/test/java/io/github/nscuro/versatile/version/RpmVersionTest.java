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

class RpmVersionTest extends AbstractVersionTest {

    @Test
    void testParse() {
        final var version = new RpmVersion("9:5.00502-3");
        assertThat(version.epoch()).isEqualTo(9);
        assertThat(version.version()).isEqualTo("5.00502");
        assertThat(version.release()).isEqualTo("3");
    }

    // https://github.com/rpm-software-management/rpm/blob/rpm-4.19.0-rc1/tests/rpmvercmp.at
    @ParameterizedTest
    @CsvSource(value = {
            "1.0, 1.0, EQUAL",
            "1.0, 2.0, LOWER",
            "2.0, 1.0, HIGHER",
            "2.0.1, 2.0.1, EQUAL",
            "2.0, 2.0.1, LOWER",
            "2.0.1, 2.0, HIGHER",
            "2.0.1a, 2.0.1a, EQUAL",
            "2.0.1a, 2.0.1, HIGHER",
            "2.0.1, 2.0.1a, LOWER",
            "5.5p1, 5.5p1, EQUAL",
            "5.5p1, 5.5p2, LOWER",
            "5.5p2, 5.5p1, HIGHER",
            "10xyz, 10.1xyz, LOWER",
            "10.1xyz, 10xyz, HIGHER",
            "xyz10, xyz10, EQUAL",
            "xyz10, xyz10.1, LOWER",
            "xyz10.1, xyz10, HIGHER",
            "xyz.4, xyz.4, EQUAL",
            "xyz.4, 8, LOWER",
            "8, xyz.4, HIGHER",
            "xyz.4, 2, LOWER",
            "2, xyz.4, HIGHER",
            "5.5p2, 5.6p1, LOWER",
            "5.6p1, 5.5p2, HIGHER",
            "5.6p1, 6.5p1, LOWER",
            "6.5p1, 5.6p1, HIGHER",
            "6.0.rc1, 6.0, HIGHER",
            "6.0, 6.0.rc1, LOWER",
            "10b2, 10a1, HIGHER",
            "10a2, 10b2, LOWER",
            "1.0aa, 1.0aa, EQUAL",
            "1.0a, 1.0aa, LOWER",
            "1.0aa, 1.0a, HIGHER",
            "10.0001, 10.0001, EQUAL",
            "10.0001, 10.1, EQUAL",
            "10.1, 10.0001, EQUAL",
            "10.0001, 10.0039, LOWER",
            "10.0039, 10.0001, HIGHER",
            "4.999.9, 5.0, LOWER",
            "5.0, 4.999.9, HIGHER",
            "20101121, 20101121, EQUAL",
            "20101121, 20101122, LOWER",
            "20101122, 20101121, HIGHER",
            "2_0, 2_0, EQUAL",
            "2.0, 2_0, EQUAL",
            "2_0, 2.0, EQUAL",
            "a, a, EQUAL",
            "a+, a+, EQUAL",
            "a+, a_, EQUAL",
            "a_, a+, EQUAL",
            "+a, +a, EQUAL",
            "+a, _a, EQUAL",
            "_a, +a, EQUAL",
            "+_, +_, EQUAL",
            "_+, +_, EQUAL",
            "_+, _+, EQUAL",
            "+, _, EQUAL",
            "_, +, EQUAL",
            // Basic testcases for tilde sorting.
            "1.0~rc1, 1.0~rc1, EQUAL",
            "1.0~rc1, 1.0, LOWER",
            "1.0, 1.0~rc1, HIGHER",
            "1.0~rc1, 1.0~rc2, LOWER",
            "1.0~rc2, 1.0~rc1, HIGHER",
            "1.0~rc1~git123, 1.0~rc1~git123, EQUAL",
            "1.0~rc1~git123, 1.0~rc1, LOWER",
            "1.0~rc1, 1.0~rc1~git123, HIGHER",
            // Basic testcases for caret sorting.
            "1.0^, 1.0^, EQUAL",
            "1.0^, 1.0, HIGHER",
            "1.0, 1.0^, LOWER",
            "1.0^git1, 1.0^git1, EQUAL",
            "1.0^git1, 1.0, HIGHER",
            "1.0, 1.0^git1, LOWER",
            "1.0^git1, 1.0^git2, LOWER",
            "1.0^git2, 1.0^git1, HIGHER",
            "1.0^git1, 1.01, LOWER",
            "1.01, 1.0^git1, HIGHER",
            "1.0^20160101, 1.0^20160101, EQUAL",
            "1.0^20160101, 1.0.1, LOWER",
            "1.0.1, 1.0^20160101, HIGHER",
            "1.0^20160101^git1, 1.0^20160101^git1, EQUAL",
            "1.0^20160102, 1.0^20160101^git1, HIGHER",
            "1.0^20160101^git1, 1.0^20160102, LOWER",
            // Basic testcases for tilde and caret sorting.
            "1.0~rc1^git1, 1.0~rc1^git1, EQUAL",
            "1.0~rc1^git1, 1.0~rc1, HIGHER",
            "1.0~rc1, 1.0~rc1^git1, LOWER",
            "1.0^git1~pre, 1.0^git1~pre, EQUAL",
            "1.0^git1, 1.0^git1~pre, HIGHER",
            "1.0^git1~pre, 1.0^git1, LOWER",
            // These are included here to document current, arguably buggy behaviors
            // for reference purposes and for easy checking against unintended
            // behavior changes.
            "1b.fc17, 1b.fc17, EQUAL",
            "1b.fc17, 1.fc17, LOWER",
            "1.fc17, 1b.fc17, HIGHER",
            "1g.fc17, 1g.fc17, EQUAL",
            "1g.fc17, 1.fc17, HIGHER",
            "1.fc17, 1g.fc17, LOWER",
            // Non-ascii characters are considered equal so these are all the same...
            "1.1.α, 1.1.α, EQUAL",
            "1.1.α, 1.1.β, EQUAL",
            "1.1.β, 1.1.α, EQUAL",
            "1.1.αα, 1.1.α, EQUAL",
            "1.1.α, 1.1.ββ, EQUAL",
            "1.1.ββ, 1.1.αα, EQUAL",
    })
    void testCompareTo(final String versionA, final String versionB, final ComparisonExpectation expectation) {
        expectation.evaluate(new RpmVersion(versionA), new RpmVersion(versionB));
    }

}