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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.nscuro.versatile.spi.InvalidVersionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GemVersionTest extends AbstractVersionTest {

    // https://github.com/rubygems/rubygems/blob/master/test/rubygems/test_gem_version.rb
    @ParameterizedTest
    @CsvSource(
            value = {
                "1.0, IS_EQUAL_TO, 1.0.0",
                "0.beta.1, IS_EQUAL_TO, 0.0.beta.1",
                "1.2.b1, IS_EQUAL_TO, 1.2.b.1",
                "1.0, IS_HIGHER_THAN, 1.0.a",
                "1.8.2, IS_HIGHER_THAN, 0.0.0",
                "1.8.2, IS_HIGHER_THAN, 1.8.2.a",
                "1.8.2.b, IS_HIGHER_THAN, 1.8.2.a",
                "1.8.2.a, IS_LOWER_THAN, 1.8.2",
                "1.8.2.a10, IS_HIGHER_THAN, 1.8.2.a9",
                "0.0.beta, IS_LOWER_THAN, 0.0.beta.1",
                "0.0.beta, IS_LOWER_THAN, 0.beta.1",
                "5.a, IS_LOWER_THAN, 5.0.0.rc2",
                "5.x, IS_HIGHER_THAN, 5.0.0.rc2",
                "1.9.3, IS_HIGHER_THAN, 1.9.2.99",
                "1.9.3, IS_LOWER_THAN, 1.9.3.1"
            })
    void testCompareTo(String versionA, ComparisonExpectation expectation, String versionB) {
        expectation.evaluate(new GemVersion(versionA), new GemVersion(versionB));
    }

    @Test
    void testEmptyVersionEqualsZero() {
        assertThat(new GemVersion("")).isEqualByComparingTo(new GemVersion("0"));
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                "1.2.0.a, false",
                "2.9.b, false",
                "1.A, false",
                "1-1, false",
                "1-a, false",
                "1.2.0, true",
                "2.9, true",
                "22.1.50.0, true"
            })
    void testIsStable(String version, boolean stable) {
        assertThat(new GemVersion(version).isStable()).isEqualTo(stable);
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1.2.3", "1..2", "abc", "1.2.3 4", "1_0"})
    void testFailingVersions(String version) {
        assertThatThrownBy(() -> new GemVersion(version)).isInstanceOf(InvalidVersionException.class);
    }
}
