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

import io.github.nscuro.versatile.version.NpmVersion;
import io.github.nscuro.versatile.version.VersioningScheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class VersTest {

    @Test
    void testParse() {
        final Vers vers = Vers.parse("vers:npm/1.2.3|>=2.0.0|<5.0.0");
        assertThat(vers).isNotNull();
        assertThat(vers.scheme()).isEqualTo(VersioningScheme.NPM);
        assertThat(vers.constraints()).satisfiesExactly(
                constraint -> {
                    assertThat(constraint).isNotNull();
                    assertThat(constraint.comparator()).isEqualTo(Comparator.EQUAL);
                    assertThat(constraint.version()).isInstanceOf(NpmVersion.class);
                    assertThat(constraint.version()).hasToString("1.2.3");
                },
                constraint -> {
                    assertThat(constraint).isNotNull();
                    assertThat(constraint.comparator()).isEqualTo(Comparator.GREATER_THAN_OR_EQUAL);
                    assertThat(constraint.version()).isInstanceOf(NpmVersion.class);
                    assertThat(constraint.version()).hasToString("2.0.0");
                },
                constraint -> {
                    assertThat(constraint).isNotNull();
                    assertThat(constraint.comparator()).isEqualTo(Comparator.LESS_THAN);
                    assertThat(constraint.version()).isInstanceOf(NpmVersion.class);
                    assertThat(constraint.version()).hasToString("5.0.0");
                }
        );
    }

    @Test
    void testBuild() {
        final Vers vers = Vers.builder(VersioningScheme.MAVEN)
                .withConstraint(Comparator.LESS_THAN, "6.6.6")
                .withConstraint(Comparator.GREATER_THAN_OR_EQUAL, "1.2.3")
                .withConstraint(Comparator.NOT_EQUAL, "3.2.1")
                .withConstraint("< 0.5.1")
                .build();

        assertThat(vers).hasToString("vers:maven/<0.5.1|>=1.2.3|!=3.2.1|<6.6.6");
        assertThat(vers.contains("1.2.2")).isFalse();
        assertThat(vers.contains("2.3.4")).isTrue();
        assertThat(vers.contains("3.2.1")).isFalse();
        assertThat(vers.contains("3.2.2")).isTrue();
        assertThat(vers.contains("6.6.5")).isTrue();
        assertThat(vers.contains("6.6.6")).isFalse();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "vers:pypi/>0.0.0|>=0.0.1|0.0.2|<0.0.3|0.0.4|<0.0.5|>=0.0.6,vers:pypi/>0.0.0|<0.0.5|>=0.0.6",
            "vers:pypi/>0.0.0|>=0.0.1|0.0.2|0.0.3|0.0.4|<0.0.5|>=0.0.6|!=0.8,vers:pypi/>0.0.0|<0.0.5|>=0.0.6|!=0.8",
            "vers:pypi/>0.0.0|>=0.0.1|>=0.0.1|0.0.2|0.0.3|0.0.4|<0.0.5|<=0.0.6|!=0.7|8.0|>12|<15.3,vers:pypi/>0.0.0|<=0.0.6|!=0.7|8.0|>12|<15.3"
    })
    void testSimplify(final String before, final String after) {
        final Vers vers = Vers.parse(before).simplify();
        assertThat(vers).hasToString(after);
        assertThatNoException().isThrownBy(vers::validate);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "vers:generic/*, CONTAINS, 1.0.0",
            "vers:generic/>1.0.0, CONTAINS, 1.0.1",
            "vers:generic/>1.0.0, NOT_CONTAINS, 1.0.0",
            "vers:generic/>=1.0.0, CONTAINS, 1.0.0",
            "vers:generic/>=1.0.0, CONTAINS, 1.0.1",
            "vers:generic/>=1.0.0, NOT_CONTAINS, 0.9.9",
            "vers:generic/1.0.0, CONTAINS, 1.0.0",
            "vers:generic/1.0.0, NOT_CONTAINS, 1.0.1",
            "vers:generic/1.0.0, NOT_CONTAINS, 0.9.9",
            "vers:generic/<=1.0.0, CONTAINS, 1.0.0",
            "vers:generic/<=1.0.0, CONTAINS, 0.9.9",
            "vers:generic/<=1.0.0, NOT_CONTAINS, 1.0.1",
            "vers:generic/<1.0.0, CONTAINS, 0.9.9",
            "vers:generic/<1.0.0, NOT_CONTAINS, 1.0.0",
            "vers:generic/<1.0.0, NOT_CONTAINS, 1.0.1",
            "vers:generic/>1.0.0|<2.0.0, CONTAINS, 1.0.1",
            "vers:generic/>1.0.0|<2.0.0, CONTAINS, 1.9.9",
            "vers:generic/>1.0.0|<2.0.0, NOT_CONTAINS, 1.0.0",
            "vers:generic/>1.0.0|<2.0.0, NOT_CONTAINS, 2.0.0",
            "vers:generic/>1.0.0|<2.0.0|>3.0.0|<4.0.0, CONTAINS, 3.1.0",
            "vers:generic/>1.0.0|<2.0.0|>3.0.0|<4.0.0, NOT_CONTAINS, 2.1.0",
            "vers:generic/>0|!=6.6.6, CONTAINS, 1.0.0",
            "vers:generic/>0|!=6.6.6, NOT_CONTAINS, 6.6.6",
            "vers:generic/>1.0.0|<2.0.0|>3.0.0, NOT_CONTAINS, 0.5.0",
            "vers:generic/>1.0.0|<2.0.0|>3.0.0, CONTAINS, 1.5.0",
            "vers:generic/>1.0.0|<2.0.0|>3.0.0, NOT_CONTAINS, 2.5.0",
            "vers:generic/>1.0.0|<2.0.0|>3.0.0, NOT_CONTAINS, 3.0.0",
            "vers:generic/>1.0.0|<2.0.0|>3.0.0, CONTAINS, 3.5.0",
            "vers:generic/>1.0.0|<2.0.0|>=3.0.0, CONTAINS, 3.0.0",
            "vers:generic/>1.0.0|<2.0.0|>=3.0.0, CONTAINS, 3.5.0"
    })
    void testContains(final String range, final ContainsExpectation expectation, final String version) {
        if (expectation == ContainsExpectation.CONTAINS) {
            assertThat(Vers.parse(range).contains(version)).isTrue();
        } else {
            assertThat(Vers.parse(range).contains(version)).isFalse();
        }
    }

    enum ContainsExpectation {

        CONTAINS,

        NOT_CONTAINS

    }

}