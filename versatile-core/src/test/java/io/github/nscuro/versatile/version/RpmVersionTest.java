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
            "1.0, IS_EQUAL_TO, 1.0",
            "1.0, IS_LOWER_THAN, 2.0",
            "2.0, IS_HIGHER_THAN, 1.0",
            "2.0.1, IS_EQUAL_TO, 2.0.1",
            "2.0, IS_LOWER_THAN, 2.0.1",
            "2.0.1, IS_HIGHER_THAN, 2.0",
            "2.0.1a, IS_EQUAL_TO, 2.0.1a",
            "2.0.1a, IS_HIGHER_THAN, 2.0.1",
            "2.0.1, IS_LOWER_THAN, 2.0.1a",
            "5.5p1, IS_EQUAL_TO, 5.5p1",
            "5.5p1, IS_LOWER_THAN, 5.5p2",
            "5.5p2, IS_HIGHER_THAN, 5.5p1",
            "10xyz, IS_LOWER_THAN, 10.1xyz",
            "10.1xyz, IS_HIGHER_THAN, 10xyz",
            "xyz10, IS_EQUAL_TO, xyz10",
            "xyz10, IS_LOWER_THAN, xyz10.1",
            "xyz10.1, IS_HIGHER_THAN, xyz10",
            "xyz.4, IS_EQUAL_TO, xyz.4",
            "xyz.4, IS_LOWER_THAN, 8",
            "8, IS_HIGHER_THAN, xyz.4",
            "xyz.4, IS_LOWER_THAN, 2",
            "2, IS_HIGHER_THAN, xyz.4",
            "5.5p2, IS_LOWER_THAN, 5.6p1",
            "5.6p1, IS_HIGHER_THAN, 5.5p2",
            "5.6p1, IS_LOWER_THAN, 6.5p1",
            "6.5p1, IS_HIGHER_THAN, 5.6p1",
            "6.0.rc1, IS_HIGHER_THAN, 6.0",
            "6.0, IS_LOWER_THAN, 6.0.rc1",
            "10b2, IS_HIGHER_THAN, 10a1",
            "10a2, IS_LOWER_THAN, 10b2",
            "1.0aa, IS_EQUAL_TO, 1.0aa",
            "1.0a, IS_LOWER_THAN, 1.0aa",
            "1.0aa, IS_HIGHER_THAN, 1.0a",
            "10.0001, IS_EQUAL_TO, 10.0001",
            "10.0001, IS_EQUAL_TO, 10.1",
            "10.1, IS_EQUAL_TO, 10.0001",
            "10.0001, IS_LOWER_THAN, 10.0039",
            "10.0039, IS_HIGHER_THAN, 10.0001",
            "4.999.9, IS_LOWER_THAN, 5.0",
            "5.0, IS_HIGHER_THAN, 4.999.9",
            "20101121, IS_EQUAL_TO, 20101121",
            "20101121, IS_LOWER_THAN, 20101122",
            "20101122, IS_HIGHER_THAN, 20101121",
            "2_0, IS_EQUAL_TO, 2_0",
            "2.0, IS_EQUAL_TO, 2_0",
            "2_0, IS_EQUAL_TO, 2.0",
            "a, IS_EQUAL_TO, a",
            "a+, IS_EQUAL_TO, a+",
            "a+, IS_EQUAL_TO, a_",
            "a_, IS_EQUAL_TO, a+",
            "+a, IS_EQUAL_TO, +a",
            "+a, IS_EQUAL_TO, _a",
            "_a, IS_EQUAL_TO, +a",
            "+_, IS_EQUAL_TO, +_",
            "_+, IS_EQUAL_TO, +_",
            "_+, IS_EQUAL_TO, _+",
            "+, IS_EQUAL_TO, _",
            "_, IS_EQUAL_TO, +",
            // Basic testcases for tilde sorting.
            "1.0~rc1, IS_EQUAL_TO, 1.0~rc1",
            "1.0~rc1, IS_LOWER_THAN, 1.0",
            "1.0, IS_HIGHER_THAN, 1.0~rc1",
            "1.0~rc1, IS_LOWER_THAN, 1.0~rc2",
            "1.0~rc2, IS_HIGHER_THAN, 1.0~rc1",
            "1.0~rc1~git123, IS_EQUAL_TO, 1.0~rc1~git123",
            "1.0~rc1~git123, IS_LOWER_THAN, 1.0~rc1",
            "1.0~rc1, IS_HIGHER_THAN, 1.0~rc1~git123",
            // Basic testcases for caret sorting.
            "1.0^, IS_EQUAL_TO, 1.0^",
            "1.0^, IS_HIGHER_THAN, 1.0",
            "1.0, IS_LOWER_THAN, 1.0^",
            "1.0^git1, IS_EQUAL_TO, 1.0^git1",
            "1.0^git1, IS_HIGHER_THAN, 1.0",
            "1.0, IS_LOWER_THAN, 1.0^git1",
            "1.0^git1, IS_LOWER_THAN, 1.0^git2",
            "1.0^git2, IS_HIGHER_THAN, 1.0^git1",
            "1.0^git1, IS_LOWER_THAN, 1.01",
            "1.01, IS_HIGHER_THAN, 1.0^git1",
            "1.0^20160101, IS_EQUAL_TO, 1.0^20160101",
            "1.0^20160101, IS_LOWER_THAN, 1.0.1",
            "1.0.1, IS_HIGHER_THAN, 1.0^20160101",
            "1.0^20160101^git1, IS_EQUAL_TO, 1.0^20160101^git1",
            "1.0^20160102, IS_HIGHER_THAN, 1.0^20160101^git1",
            "1.0^20160101^git1, IS_LOWER_THAN, 1.0^20160102",
            // Basic testcases for tilde and caret sorting.
            "1.0~rc1^git1, IS_EQUAL_TO, 1.0~rc1^git1",
            "1.0~rc1^git1, IS_HIGHER_THAN, 1.0~rc1",
            "1.0~rc1, IS_LOWER_THAN, 1.0~rc1^git1",
            "1.0^git1~pre, IS_EQUAL_TO, 1.0^git1~pre",
            "1.0^git1, IS_HIGHER_THAN, 1.0^git1~pre",
            "1.0^git1~pre, IS_LOWER_THAN, 1.0^git1",
            // These are included here to document current, arguably buggy behaviors
            // for reference purposes and for easy checking against unintended
            // behavior changes.
            "1b.fc17, IS_EQUAL_TO, 1b.fc17",
            "1b.fc17, IS_LOWER_THAN, 1.fc17",
            "1.fc17, IS_HIGHER_THAN, 1b.fc17",
            "1g.fc17, IS_EQUAL_TO, 1g.fc17",
            "1g.fc17, IS_HIGHER_THAN, 1.fc17",
            "1.fc17, IS_LOWER_THAN, 1g.fc17",
            // Non-ascii characters are considered equal so these are all the same...
            "1.1.α, IS_EQUAL_TO, 1.1.α",
            "1.1.α, IS_EQUAL_TO, 1.1.β",
            "1.1.β, IS_EQUAL_TO, 1.1.α",
            "1.1.αα, IS_EQUAL_TO, 1.1.α",
            "1.1.α, IS_EQUAL_TO, 1.1.ββ",
            "1.1.ββ, IS_EQUAL_TO, 1.1.αα",
            // Custom test cases
            "1.0-1, IS_HIGHER_THAN, 1.0",
            "1.0, IS_LOWER_THAN, 1.0-1"
    })
    void testCompareTo(final String versionA, final ComparisonExpectation expectation, final String versionB) {
        expectation.evaluate(new RpmVersion(versionA), new RpmVersion(versionB));
    }

}