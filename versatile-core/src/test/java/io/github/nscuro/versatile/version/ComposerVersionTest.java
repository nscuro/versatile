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

/**
 * @see <a href="https://github.com/composer/semver/blob/main/tests/VersionParserTest.php">VersionParserTest.php</a>
 */
class ComposerVersionTest {

    /**
     * @see <a href="https://github.com/composer/semver/blob/38ccbbfd0098b205e4d947f18e3f1f321803b067/tests/VersionParserTest.php#L94">successfulNormalizedVersions</a>
     */
    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(value = {
            "1.0.0, 1.0.0.0",
            "1.2.3.4, 1.2.3.4",
            "v1.0.0, 1.0.0.0",
            "0, 0.0.0.0",
            "99999, 99999.0.0.0",
            "1.0.0RC1dev, 1.0.0.0-RC1-dev",
            "1.0.0-rC15-dev, 1.0.0.0-RC15-dev",
            "1.0.0.RC.15-dev, 1.0.0.0-RC15-dev",
            "1.0.0-rc1, 1.0.0.0-RC1",
            "1.0.0.pl3-dev, 1.0.0.0-patch3-dev",
            "1.0-dev, 1.0.0.0-dev",
            "10.4.13-beta, 10.4.13.0-beta",
            "10.4.13beta2, 10.4.13.0-beta2",
            "10.4.13beta.2, 10.4.13.0-beta2",
            "v1.13.11-beta.0, 1.13.11.0-beta0",
            "1.13.11.0-beta0, 1.13.11.0-beta0",
            "10.4.13-b, 10.4.13.0-beta",
            "10.4.13-b5, 10.4.13.0-beta5",
            "2010.01, 2010.01.0.0",
            "2010.01.02, 2010.01.02.0",
            "2010.1.555, 2010.1.555.0",
            "2010.10.200, 2010.10.200.0",
            "2012.06.07, 2012.06.07.0",
            "20230131.0.0, 20230131.0.0",
            "202301310000.0.0, 202301310000.0.0",
            "v20100102, 20100102",
            "20100102, 20100102",
            "20100102.0, 20100102.0",
            "20100102.1.0, 20100102.1.0",
            "20100102.0.3, 20100102.0.3",
            "100000, 100000",
            "2010-01-02-10-20-30.0.3, 2010.01.02.10.20.30.0.3",
            "2010-01-02-10-20-30.5, 2010.01.02.10.20.30.5",
            "2010-01-02, 2010.01.02",
            "2010-01-02.5, 2010.01.02.5",
            "20100102-203040, 20100102.203040",
            "20100102203040-10, 20100102203040.10",
            "20100102-203040-p1, 20100102.203040-patch1",
            "201903.0, 201903.0",
            "201903.0-p2, 201903.0-patch2",
            "20100102.x-dev, 20100102.9999999.9999999.9999999-dev",
            "20100102.203040.x-dev, 20100102.203040.9999999.9999999-dev",
            "1.x-dev, 1.9999999.9999999.9999999-dev",
            "201903.x-dev, 201903.9999999.9999999.9999999-dev",
            "041.x-dev, 041.9999999.9999999.9999999-dev",
            "dev-master, dev-master",
            "master, dev-master",
            "dev-trunk, dev-trunk",
            "dev-feature-foo, dev-feature-foo",
            "DEV-FOOBAR, dev-FOOBAR",
            "dev-feature/foo, dev-feature/foo",
            "dev-feature+issue-1, dev-feature+issue-1",
            "dev-1.0.0-dev<1.0.5-dev, dev-1.0.0-dev<1.0.5-dev",
            "dev-foo bar, dev-foo bar",
            "dev-master as 1.0.0, dev-master",
            "dev-load-varnish-only-when-used as ^2.0, dev-load-varnish-only-when-used",
            "dev-load-varnish-only-when-used@stable, dev-load-varnish-only-when-used",
            "1.0.0+foo@dev, 1.0.0.0",
            "1.0.0@dev, 1.0.0.0",
            "dev-load-varnish-only-when-used@dev as ^2.0@dev, dev-load-varnish-only-when-used",
            "1.0.0-beta.5+foo, 1.0.0.0-beta5",
            "1.0.0+foo, 1.0.0.0",
            "1.0.0-alpha.3.1+foo, 1.0.0.0-alpha3.1",
            "1.0.0-alpha2.1+foo, 1.0.0.0-alpha2.1",
            "1.0.0-alpha-2.1-3+foo, 1.0.0.0-alpha2.1-3",
            "1.0.0+foo as 2.0, 1.0.0.0",
            "00.01.03.04, 00.01.03.04",
            "000.001.003.004, 000.001.003.004",
            "0.000.103.204, 0.000.103.204",
            "0700, 0700.0.0.0",
            "dev-041.003, dev-041.003",
            "1.0.0-stable, 1.0.0.0",
            "1.0.0-stable1, 1.0.0.0",
            "1.0.0-stable-dev, 1.0.0.0",
    })
    void testSuccessfulNormalizedVersions(String input, String expected) {
        assertThat(new ComposerVersion(input).toString()).isEqualTo(expected);
    }

    @Test
    void testNormalizeTrimsWhitespace() {
        assertThat(new ComposerVersion(" 1.0.0").toString()).isEqualTo("1.0.0.0");
        assertThat(new ComposerVersion("1.0.0 ").toString()).isEqualTo("1.0.0.0");
    }

    /**
     * @see <a href="https://github.com/composer/semver/blob/38ccbbfd0098b205e4d947f18e3f1f321803b067/tests/VersionParserTest.php#L189">failingNormalizedVersions</a>
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "a",
            "1.0.0-meh",
            "1.0.0.0.0",
            "feature-foo",
            "1.0.0+foo bar",
            "1.0.1-SNAPSHOT",
            "1.0.0<1.0.5-dev",
            "1.0.0-dev<1.0.5-dev",
            "foo bar-dev",
            "dev-",
            "20100102.0.3.4",
            "100000.0.0.0",
            "2023013.0.0",
            "202301311.0.0",
            "20230131000.0.0",
            "2023013100000.0.0",
    })
    void testFailingNormalizedVersions(String input) {
        assertThatThrownBy(() -> new ComposerVersion(input))
                .isInstanceOf(InvalidVersionException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1.0.0, true",
            "1.0.0-patch1, true",
            "1.0.0-stable, true",
            "1.0.0-stable-dev, true",
            "1.0.0-beta1, false",
            "1.0.0-alpha1, false",
            "1.0.0-RC1, false",
            "1.0.0-dev, false",
            "1.0.0-beta1-dev, false",
            "dev-master, false",
            "master, false",
            "1.x-dev, false",
    })
    void testIsStable(String input, boolean expected) {
        assertThat(new ComposerVersion(input).isStable()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "0.0.0, IS_LOWER_THAN, 1.0.0",
            "1.0.0, IS_LOWER_THAN, 1.1.0",
            "1.0.0, IS_LOWER_THAN, 1.0.1",
            "1.999.9999, IS_LOWER_THAN, 2.1.1",
            "1.0.0.0, IS_LOWER_THAN, 1.0.0.1",
            "1.0.0.9, IS_LOWER_THAN, 1.0.1.0",
            "1.0.0-dev, IS_LOWER_THAN, 1.0.0-alpha1",
            "1.0.0-alpha1, IS_LOWER_THAN, 1.0.0-beta1",
            "1.0.0-beta1, IS_LOWER_THAN, 1.0.0-RC1",
            "1.0.0-RC1, IS_LOWER_THAN, 1.0.0",
            "1.0.0, IS_LOWER_THAN, 1.0.0-patch1",
            "1.0.0-alpha1, IS_LOWER_THAN, 1.0.0-alpha2",
            "1.0.0-beta1, IS_LOWER_THAN, 1.0.0-beta2",
            "1.0.0-RC1, IS_LOWER_THAN, 1.0.0-RC2",
            "1.0.0-alpha.3.1, IS_LOWER_THAN, 1.0.0-alpha.3.2",
            "1.0.0-alpha.3, IS_LOWER_THAN, 1.0.0-alpha.3.1",
            "1.0.0-alpha3, IS_EQUAL_TO, 1.0.0-alpha.3",
            "1.0.0-beta0, IS_EQUAL_TO, 1.0.0-beta",
            "1.0.0-beta1-dev, IS_LOWER_THAN, 1.0.0-beta1",
            "1.0.0-alpha1-dev, IS_LOWER_THAN, 1.0.0-alpha1",
            "1.0.0-a1, IS_EQUAL_TO, 1.0.0-alpha1",
            "1.0.0-b2, IS_EQUAL_TO, 1.0.0-beta2",
            "1.0.0-p1, IS_EQUAL_TO, 1.0.0-patch1",
            "1.0.0-pl1, IS_EQUAL_TO, 1.0.0-patch1",
            "v1.0.0, IS_EQUAL_TO, 1.0.0",
            "1.0, IS_EQUAL_TO, 1.0.0.0",
            "1, IS_EQUAL_TO, 1.0.0.0",
            "00.01, IS_EQUAL_TO, 0.1",
            "1.0.0+build1, IS_EQUAL_TO, 1.0.0+build2",
            "1.0.0+meta, IS_EQUAL_TO, 1.0.0",
            "1.0.0-stable, IS_EQUAL_TO, 1.0.0",
            "1.0.0-stable1, IS_EQUAL_TO, 1.0.0-stable2",
            "1.0.0-stable-dev, IS_EQUAL_TO, 1.0.0",
            "1.0.0-BETA1, IS_EQUAL_TO, 1.0.0-beta1",
            "1.0.0-rc1, IS_EQUAL_TO, 1.0.0-RC1",
            "1.0.0@dev, IS_EQUAL_TO, 1.0.0",
            "dev-master, IS_LOWER_THAN, 0.0.1",
            "dev-feature, IS_LOWER_THAN, dev-master",
            "master, IS_EQUAL_TO, dev-master",
            "trunk, IS_EQUAL_TO, dev-trunk",
            "1.x-dev, IS_HIGHER_THAN, 1.0.0",
            "1.x-dev, IS_LOWER_THAN, 2.0.0",
            "1.0.x-dev, IS_HIGHER_THAN, 1.0.0",
            "1.0.x-dev, IS_LOWER_THAN, 1.1.0",
    })
    void testCompareTo(String versionA, ComparisonExpectation expectation, String versionB) {
        expectation.evaluate(new ComposerVersion(versionA), new ComposerVersion(versionB));
    }

}
