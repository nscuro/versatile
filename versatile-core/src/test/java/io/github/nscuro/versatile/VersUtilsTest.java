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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.github.nscuro.versatile.VersUtils.schemeFromGhsaEcosystem;
import static io.github.nscuro.versatile.VersUtils.schemeFromOsvEcosystem;
import static io.github.nscuro.versatile.VersUtils.versFromGhsaRange;
import static io.github.nscuro.versatile.VersUtils.versFromNvdRange;
import static io.github.nscuro.versatile.VersUtils.versFromOsvRange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class VersUtilsTest {

    @ParameterizedTest
    @CsvSource(value = {
            "> 1.2.3, vers:other/>1.2.3",
            ">= 1.2.3, vers:other/>=1.2.3",
            "= 1.2.3, vers:other/1.2.3",
            "'> 1.2.3, <= 3.2.1', vers:other/>1.2.3|<=3.2.1",
            "'<= 3.2.1, > 1.2.3', vers:other/>1.2.3|<=3.2.1",
            "<= 3.2.1, vers:other/<=3.2.1",
            "< 3.2.1, vers:other/<3.2.1",

    })
    void testVersFromGhsaRange(final String ghsaRange, final String expectedVers) {
        assertThat(versFromGhsaRange("other", ghsaRange)).hasToString(expectedVers);
    }

    private static Stream<Arguments> testVersFromOsvRangeArguments() {
        return Stream.of(
                arguments(
                        List.of(Map.entry("introduced", "1.2.3")),
                        null,
                        "vers:other/>=1.2.3"
                ),
                arguments(
                        List.of(Map.entry("introduced", "1.2.3"), Map.entry("fixed", "3.2.1")),
                        null,
                        "vers:other/>=1.2.3|<3.2.1"
                ),
                arguments(
                        List.of(Map.entry("introduced", "1.2.3"), Map.entry("last_affected", "3.2.1")),
                        null,
                        "vers:other/>=1.2.3|<=3.2.1"
                ),
                arguments(
                        List.of(Map.entry("last_affected", "3.2.1"), Map.entry("introduced", "1.2.3")),
                        Map.of("foo", "bar"),
                        "vers:other/>=1.2.3|<=3.2.1"
                ),
                arguments(
                        List.of(Map.entry("introduced", "1.2.3"), Map.entry("limit", "3.2.1")),
                        Map.of("last_known_affected_version_range", Map.of("foo", "bar")),
                        "vers:other/>=1.2.3|<3.2.1"
                ),
                arguments(
                        List.of(Map.entry("introduced", "4.5.6")),
                        Map.of("last_known_affected_version_range", "<7.8.9"),
                        "vers:other/>=4.5.6|<7.8.9"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testVersFromOsvRangeArguments")
    void testVersFromOsvRange(final List<Map.Entry<String, String>> events, final Map<String, Object> databaseSpecific, final String expectedVers) {
        assertThat(versFromOsvRange("ecosystem", "other", events, databaseSpecific)).hasToString(expectedVers);
    }

    @Test
    void testVersFromOsvRangeWithInvalidRangeType() {
        final List<Map.Entry<String, String>> events = List.of(Map.entry("introduced", "0"));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> versFromOsvRange(null, "other", events, null));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> versFromOsvRange("", "other", events, null));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> versFromOsvRange("git", "other", events, null));
        assertThatNoException().isThrownBy(() -> versFromOsvRange("ecosystem", "other", events, null));
        assertThatNoException().isThrownBy(() -> versFromOsvRange("semver", "other", events, null));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "actions, ",
            "composer, ",
            "erlang, ",
            "go, golang",
            "maven, maven",
            "npm, npm",
            "nuget, nuget",
            "other, ",
            "pip, pypi",
            "pub, ",
            "rubygems, gem",
            "rust, ",
            "foo, ",
    })
    void testSchemeFromGhsaEcosystem(final String ecosystem, final String expectedScheme) {
        if (expectedScheme == null) {
            assertThat(schemeFromGhsaEcosystem(ecosystem)).isEmpty();
        } else {
            assertThat(schemeFromGhsaEcosystem(ecosystem)).contains(expectedScheme);
        }
    }

    @ParameterizedTest
    @CsvSource(value = {
            "AlmaLinux, rpm",
            "Alpine, apk",
            "Android, ",
            "Bioconductor, ",
            "Bitnami, ",
            "CRAN, ",
            "ConanCenter, ",
            "Debian, deb",
            "GHC, ",
            "GitHub Actions, ",
            "Go, golang",
            "Hackage, ",
            "Hex, ",
            "Linux, ",
            "Mageia, rpm",
            "Maven, maven",
            "OSS-Fuzz, ",
            "Packagist, ",
            "Photon OS, rpm",
            "Pub, ",
            "PyPI, pypi",
            "Rocky Linux, rpm",
            "RubyGems, gem",
            "SwiftURL, ",
            "crates.io, ",
            "npm, npm",
    })
    void testSchemeFromOsvEcosystem(final String ecosystem, final String expectedScheme) {
        if (expectedScheme == null) {
            assertThat(schemeFromOsvEcosystem(ecosystem)).isEmpty();
        } else {
            assertThat(schemeFromOsvEcosystem(ecosystem)).contains(expectedScheme);
        }
    }

    private static Stream<Arguments> testVersFromNvdRangeArguments() {
        return Stream.of(
                arguments(
                        null, "2.2.0", null, "2.2.13", "*",
                        "vers:generic/>=2.2.0|<=2.2.13"
                ),
                arguments(
                        null, null, null, null, "6.0.7",
                        "vers:generic/6.0.7"
                ),
                arguments(
                        null, null, null, null, "*",
                        "vers:generic/*"
                ),
                arguments(
                        null, "2.2.0", null, null, "6.0.7",
                        "vers:generic/>=2.2.0"
                ),
                arguments(
                        null, null, null, null, "-",
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testVersFromNvdRangeArguments")
    void testVersFromNvdRange(final String versionStartExcluding, final String versionStartIncluding,
                              final String versionEndExcluding, final String versionEndIncluding,
                              final String exactVersion, final String expectedVers) {
        final Optional<Vers> optionalVers = versFromNvdRange(versionStartExcluding, versionStartIncluding, versionEndExcluding, versionEndIncluding, exactVersion);
        if (expectedVers == null) {
            assertThat(optionalVers).isNotPresent();
        } else {
            assertThat(optionalVers).map(Vers::toString).contains(expectedVers);
        }
    }
}