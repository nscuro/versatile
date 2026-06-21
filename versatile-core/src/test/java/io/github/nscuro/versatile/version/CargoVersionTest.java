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

class CargoVersionTest extends AbstractVersionTest {

    // https://github.com/dtolnay/semver/blob/master/tests/test_version.rs.
    @ParameterizedTest
    @CsvSource(
            value = {
                "1.0.0-alpha, IS_LOWER_THAN, 1.0.0-alpha.1",
                "1.0.0-alpha.1, IS_LOWER_THAN, 1.0.0-alpha.beta",
                "1.0.0-alpha.beta, IS_LOWER_THAN, 1.0.0-beta",
                "1.0.0-beta, IS_LOWER_THAN, 1.0.0-beta.2",
                "1.0.0-beta.2, IS_LOWER_THAN, 1.0.0-beta.11",
                "1.0.0-beta.11, IS_LOWER_THAN, 1.0.0-rc.1",
                "1.0.0-rc.1, IS_LOWER_THAN, 1.0.0",
                "1.0.0-1, IS_LOWER_THAN, 1.0.0-alpha",
                "1.0.0-pre.8, IS_LOWER_THAN, 1.0.0-pre.12",
                "0.0.0, IS_LOWER_THAN, 1.2.3-alpha2",
                "1.2.3-alpha1, IS_LOWER_THAN, 1.2.3",
                "1.2.3, IS_LOWER_THAN, 1.2.4",
                "1.2.3, IS_LOWER_THAN, 1.3.0",
                "1.2.3, IS_LOWER_THAN, 2.0.0",
                "1.2.3, IS_EQUAL_TO, 1.2.3",
                "1.2.3+23, IS_EQUAL_TO, 1.2.3+42",
                "1.2.3, IS_EQUAL_TO, 1.2.3+build",
                "1.2.3-alpha+1, IS_EQUAL_TO, 1.2.3-alpha+2",
                "0, IS_EQUAL_TO, 0.0.0",
                "0.16, IS_EQUAL_TO, 0.16.0",
                "1, IS_LOWER_THAN, 1.0.1",
                "0, IS_LOWER_THAN, 0.3.24"
            })
    void testCompareTo(String versionA, ComparisonExpectation expectation, String versionB) {
        expectation.evaluate(new CargoVersion(versionA), new CargoVersion(versionB));
    }

    @ParameterizedTest
    @CsvSource(value = {"1.2.3, true", "1.2.3+build, true", "1.2.3-alpha, false", "1.0.0-rc.1, false"})
    void testIsStable(String version, boolean stable) {
        assertThat(new CargoVersion(version).isStable()).isEqualTo(stable);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "1.2.3-",
                "a.b.c",
                "1.2.3-01",
                "1.2.3++",
                "07",
                "v1.2.3",
                "1.2.3 abc",
                "1.2.3.4",
                "111111111111111111111.0.0"
            })
    void testFailingVersions(String version) {
        assertThatThrownBy(() -> new CargoVersion(version)).isInstanceOf(InvalidVersionException.class);
    }

    @Test
    void testFailingEmptyVersion() {
        assertThatThrownBy(() -> new CargoVersion("")).isInstanceOf(InvalidVersionException.class);
    }
}
