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

import static io.github.nscuro.versatile.VersUtils.schemeFromGhsaEcosystem;
import static io.github.nscuro.versatile.VersUtils.schemeFromOsvEcosystem;
import static io.github.nscuro.versatile.VersUtils.versFromGhsaRange;
import static io.github.nscuro.versatile.VersUtils.versFromNvdRange;
import static io.github.nscuro.versatile.VersUtils.versFromOsvRange;
import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_PYPI;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.nscuro.versatile.spi.Version;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class VersUtilsTest {

    @ParameterizedTest
    @CsvSource(
            value = {
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

    @ParameterizedTest
    @CsvSource(
            value = {
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

    @Test
    void testSchemeFromOsvEcosystemDoesNotInferSchemeFromSuffix() {
        assertThat(schemeFromOsvEcosystem("Alpine:npm")).contains("apk");
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                "AlmaLinux, rpm",
                "Alpaquita:23, apk",
                "Alpine, apk",
                "Android, ",
                "Azure Linux:2, rpm",
                "BellSoft Hardened Containers:stream, apk",
                "Bioconductor, ",
                "Bitnami, ",
                "CRAN, ",
                "Chainguard, apk",
                "CleanStart, apk",
                "ConanCenter, ",
                "Debian, deb",
                "Debian:12, deb",
                "Echo, deb",
                "Echo:Maven, maven",
                "Echo:PyPi, pypi",
                "GHC, ",
                "GitHub Actions, ",
                "Go, golang",
                "Hackage, ",
                "Hex, ",
                "Linux, ",
                "Mageia, rpm",
                "Maven, maven",
                "Maven:https://maven.google.com, maven",
                "MinimOS, apk",
                "OSS-Fuzz, ",
                "Packagist, composer",
                "Packagist:https://packages.drupal.org/8, composer",
                "Photon OS, rpm",
                "Pub, ",
                "PyPI, pypi",
                "Red Hat:enterprise_linux:7::server, rpm",
                "Rocky Linux, rpm",
                "Root:Alpine:3.18, apk",
                "Root:Composer, composer",
                "Root:Go, golang",
                "Root:Ruby, gem",
                "Root:Ubuntu:22.04, deb",
                "Root:npm, npm",
                "RubyGems, gem",
                "SUSE:Linux Enterprise Module for Public Cloud 15 SP4, rpm",
                "SwiftURL, ",
                "TuxCare:CentOS-Stream:8, rpm",
                "TuxCare:CentOS:8.4, rpm",
                "TuxCare:Maven, maven",
                "TuxCare:OracleLinux:6, rpm",
                "TuxCare:Packagist, composer",
                "TuxCare:RHEL:7, rpm",
                "TuxCare:Ubuntu:16.04, deb",
                "Ubuntu, deb",
                "Ubuntu:Pro:18.04:LTS, deb",
                "VSCode:https://open-vsx.org, ",
                "Wolfi, apk",
                "crates.io, cargo",
                "npm, npm",
                "openEuler:20.03-LTS-SP1, rpm",
                "openSUSE:Leap 15.3, rpm",
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
                arguments(null, "2.2.0", null, "2.2.13", "*", "vers:generic/>=2.2.0|<=2.2.13"),
                arguments(null, null, null, null, "6.0.7", "vers:generic/6.0.7"),
                arguments(null, null, null, null, "*", "vers:generic/*"),
                arguments(null, "2.2.0", null, null, "6.0.7", "vers:generic/>=2.2.0"),
                arguments(null, null, null, null, "-", null));
    }

    @ParameterizedTest
    @MethodSource("testVersFromNvdRangeArguments")
    void testVersFromNvdRange(
            final String versionStartExcluding,
            final String versionStartIncluding,
            final String versionEndExcluding,
            final String versionEndIncluding,
            final String exactVersion,
            final String expectedVers) {
        final Optional<Vers> optionalVers = versFromNvdRange(
                versionStartExcluding, versionStartIncluding, versionEndExcluding, versionEndIncluding, exactVersion);
        if (expectedVers == null) {
            assertThat(optionalVers).isNotPresent();
        } else {
            assertThat(optionalVers).map(Vers::toString).contains(expectedVers);
        }
    }

    @Nested
    class VersFromOsvRangeTest {

        private static Stream<Arguments> versFromOsvRangeArguments() {
            return Stream.of(
                    arguments(
                            List.of(
                                    Map.entry("introduced", "0"),
                                    Map.entry("fixed", "1.11.27"),
                                    Map.entry("introduced", "2.2"),
                                    Map.entry("fixed", "2.2.9")),
                            List.of("vers:pypi/<1.11.27", "vers:pypi/>=2.2|<2.2.9")),
                    arguments(
                            List.of(
                                    Map.entry("introduced", "1"),
                                    Map.entry("fixed", "2"),
                                    Map.entry("introduced", "3"),
                                    Map.entry("fixed", "4")),
                            List.of("vers:pypi/>=1|<2", "vers:pypi/>=3|<4")),
                    arguments(
                            List.of(
                                    Map.entry("fixed", "2"),
                                    Map.entry("introduced", "3"),
                                    Map.entry("introduced", "4"),
                                    Map.entry("fixed", "5")),
                            List.of("vers:pypi/>=3|<5")),
                    arguments(
                            List.of(
                                    Map.entry("introduced", "0"),
                                    Map.entry("last_affected", "1.2.1"),
                                    Map.entry("last_affected", "1.2.0")),
                            List.of("vers:pypi/<=1.2.0")),
                    arguments(
                            List.of(
                                    Map.entry("introduced", "2.0"),
                                    Map.entry("introduced", "1.0"),
                                    Map.entry("fixed", "2.5")),
                            List.of("vers:pypi/>=1.0|<2.5")),
                    arguments(
                            List.of(
                                    Map.entry("introduced", "5.0.0"),
                                    Map.entry("fixed", "5.0.12"),
                                    Map.entry("introduced", "6.2.0"),
                                    Map.entry("fixed", "6.2.2"),
                                    Map.entry("fixed", "6.2.6")),
                            List.of("vers:pypi/>=5.0.0|<5.0.12", "vers:pypi/>=6.2.0|<6.2.2")),
                    arguments(
                            List.of(Map.entry("introduced", "1.0.0"), Map.entry("limit", "*")),
                            List.of("vers:pypi/>=1.0.0")),
                    arguments(
                            List.of(Map.entry("introduced", "0"), Map.entry("limit", "*"), Map.entry("limit", "5")),
                            List.of("vers:pypi/*")),
                    arguments(
                            List.of(Map.entry("introduced", "1.0.0"), Map.entry("limit", "5"), Map.entry("limit", "*")),
                            List.of("vers:pypi/>=1.0.0")),
                    arguments(
                            List.of(
                                    Map.entry("introduced", "0"),
                                    Map.entry("introduced", "1.0"),
                                    Map.entry("fixed", "1.1")),
                            List.of("vers:pypi/<1.1")),
                    arguments(
                            List.of(
                                    Map.entry("introduced", "0"),
                                    Map.entry("limit", "5"),
                                    Map.entry("introduced", "6"),
                                    Map.entry("fixed", "7")),
                            List.of("vers:pypi/<5")),
                    arguments(
                            List.of(
                                    Map.entry("introduced", "0"),
                                    Map.entry("fixed", "1.0.0"),
                                    Map.entry("introduced", "2.0.0"),
                                    Map.entry("limit", "3.0.0")),
                            List.of("vers:pypi/<1.0.0", "vers:pypi/>=2.0.0|<3.0.0")),
                    arguments(
                            List.of(
                                    Map.entry("introduced", "1.0.0"),
                                    Map.entry("fixed", "1.2.0"),
                                    Map.entry("introduced", "2.0.0")),
                            List.of("vers:pypi/>=1.0.0|<1.2.0", "vers:pypi/>=2.0.0")),
                    arguments(List.of(Map.entry("introduced", "0")), List.of("vers:pypi/*")),
                    arguments(List.of(Map.entry("fixed", "1.2.0")), List.of()),
                    arguments(List.of(Map.entry("introduced", "3"), Map.entry("fixed", "3")), List.of()),
                    arguments(List.of(Map.entry("introduced", "3"), Map.entry("limit", "3")), List.of()),
                    arguments(
                            List.of(Map.entry("introduced", "3"), Map.entry("last_affected", "3")),
                            List.of("vers:pypi/>=3|<=3")),
                    arguments(
                            List.of(
                                    Map.entry("introduced", "0.23.0"),
                                    Map.entry("fixed", "0.23.0"),
                                    Map.entry("introduced", "0.23.13"),
                                    Map.entry("fixed", "0.23.18")),
                            List.of("vers:pypi/>=0.23.13|<0.23.18")));
        }

        @ParameterizedTest
        @MethodSource("versFromOsvRangeArguments")
        void versFromOsvRangeShouldReturnOneRangePerAffectedInterval(
                final List<Map.Entry<String, String>> events, final List<String> expectedVers) {
            assertThat(versFromOsvRange("ECOSYSTEM", "PyPI", events, null))
                    .extracting(Vers::toString)
                    .containsExactlyElementsOf(expectedVers);
        }

        @Test
        void versFromOsvRangeShouldIgnoreDebianUnfixedSentinels() {
            assertThat(versFromOsvRange(
                            "ECOSYSTEM",
                            "Debian:12",
                            List.of(Map.entry("introduced", "0"), Map.entry("fixed", "<unfixed>")),
                            null))
                    .extracting(Vers::toString)
                    .containsExactly("vers:deb/*");
        }

        @Test
        void versFromOsvRangeShouldRetainGoPseudoVersionUpperBound() {
            final List<Vers> versList = versFromOsvRange(
                    "SEMVER",
                    "Go",
                    List.of(Map.entry("introduced", "0"), Map.entry("fixed", "0.0.0-20220412211240-33da011f77ad")),
                    null);

            assertThat(versList)
                    .extracting(Vers::toString)
                    .containsExactly("vers:golang/<0.0.0-20220412211240-33da011f77ad");
            assertThat(versList.getFirst().contains("v0.47.0")).isFalse();
            assertThat(versList.getFirst().contains("v0.0.0-20220227234510-4e6760a101f9"))
                    .isTrue();
        }

        @Test
        void versFromOsvRangeShouldMapSuffixedEcosystemToScheme() {
            List<Map.Entry<String, String>> events =
                    List.of(Map.entry("introduced", "0"), Map.entry("fixed", "1.35.0"));

            assertThat(versFromOsvRange("ecosystem", "Packagist:https://packages.drupal.org/8", events, null))
                    .extracting(Vers::toString)
                    .containsExactly("vers:composer/<1.35.0.0");
            assertThat(versFromOsvRange("ecosystem", "VSCode:https://open-vsx.org", events, null))
                    .extracting(Vers::toString)
                    .containsExactly("vers:vscode/<1.35.0");
            assertThat(versFromOsvRange("ecosystem", "TuxCare:Ubuntu:16.04", events, null))
                    .extracting(Vers::toString)
                    .containsExactly("vers:deb/<1.35.0");
        }

        @Test
        void versFromOsvRangeShouldApplyLastKnownAffectedVersionRangeToOpenLastInterval() {
            assertThat(versFromOsvRange(
                            "ECOSYSTEM",
                            "PyPI",
                            List.of(
                                    Map.entry("introduced", "1.0.0"),
                                    Map.entry("fixed", "1.2.0"),
                                    Map.entry("introduced", "2.0.0")),
                            Map.of("last_known_affected_version_range", "<= 2.5.0")))
                    .extracting(Vers::toString)
                    .containsExactly("vers:pypi/>=1.0.0|<1.2.0", "vers:pypi/>=2.0.0|<=2.5.0");
        }

        @Test
        void versFromOsvRangeShouldIgnoreLastKnownAffectedVersionRangeWhenLastIntervalIsBounded() {
            assertThat(versFromOsvRange(
                            "ECOSYSTEM",
                            "PyPI",
                            List.of(Map.entry("introduced", "1.0.0"), Map.entry("fixed", "1.2.0")),
                            Map.of("last_known_affected_version_range", "<2.5.0")))
                    .extracting(Vers::toString)
                    .containsExactly("vers:pypi/>=1.0.0|<1.2.0");
        }

        @Test
        void versFromOsvRangeShouldFallBackToGenericSchemeForInvalidVersions() {
            assertThat(versFromOsvRange(
                            "ECOSYSTEM",
                            "PyPI",
                            List.of(Map.entry("introduced", "0"), Map.entry("fixed", "4.6.0.stable11")),
                            null))
                    .extracting(Vers::toString)
                    .containsExactly("vers:generic/<4.6.0.stable11");
        }

        @Test
        void versFromOsvRangeShouldRetainIntervalsWhenFallingBackToGenericScheme() {
            assertThat(versFromOsvRange(
                            "ECOSYSTEM",
                            "PyPI",
                            List.of(
                                    Map.entry("introduced", "1.0"),
                                    Map.entry("fixed", "2.0.x"),
                                    Map.entry("introduced", "3.0"),
                                    Map.entry("fixed", "4.0")),
                            null))
                    .extracting(Vers::toString)
                    .containsExactly("vers:generic/>=1.0|<2.0.x", "vers:generic/>=3.0|<4.0");
        }

        @Test
        void versFromOsvRangeShouldAcceptAnyVersionUnderGenericScheme() {
            assertThat(versFromOsvRange(
                            "ECOSYSTEM",
                            "generic",
                            List.of(Map.entry("introduced", "0"), Map.entry("fixed", "2.0.x")),
                            null))
                    .extracting(Vers::toString)
                    .containsExactly("vers:generic/<2.0.x");
        }

        @Test
        void versFromOsvRangeShouldIgnoreUnfixedSentinelsRegardlessOfScheme() {
            assertThat(versFromOsvRange(
                            "ECOSYSTEM",
                            "PyPI",
                            List.of(Map.entry("introduced", "1.0"), Map.entry("fixed", "<unfixed>")),
                            null))
                    .extracting(Vers::toString)
                    .containsExactly("vers:pypi/>=1.0");
        }

        @Test
        void versFromOsvRangeShouldThrowForInvalidRangeType() {
            final List<Map.Entry<String, String>> events = List.of(Map.entry("introduced", "0"));
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> versFromOsvRange(null, "other", events, null));
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> versFromOsvRange("", "other", events, null));
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> versFromOsvRange("git", "other", events, null));
            assertThatNoException().isThrownBy(() -> versFromOsvRange("ecosystem", "other", events, null));
            assertThatNoException().isThrownBy(() -> versFromOsvRange("semver", "other", events, null));
        }

        @Test
        void versFromOsvRangeShouldTreatOnlyExactWildcardLimitAsInfinity() {
            assertThat(versFromOsvRange(
                            "ECOSYSTEM",
                            "PyPI",
                            List.of(Map.entry("introduced", "0"), Map.entry("limit", "1.*")),
                            null))
                    .extracting(Vers::toString)
                    .containsExactly("vers:generic/<1.%2A");
        }

        @Test
        void versFromOsvRangeShouldIgnoreNonStringLastKnownAffectedVersionRange() {
            assertThat(versFromOsvRange(
                            "ECOSYSTEM",
                            "PyPI",
                            List.of(Map.entry("introduced", "1.0.0")),
                            Map.of("last_known_affected_version_range", Map.of("foo", "bar"))))
                    .extracting(Vers::toString)
                    .containsExactly("vers:pypi/>=1.0.0");
        }

        @Test
        void versFromOsvRangeShouldThrowForInvalidEvent() {
            final List<Map.Entry<String, String>> events =
                    List.of(Map.entry("introduced", "0"), Map.entry("foo", "1.2.3"));
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> versFromOsvRange("ecosystem", "other", events, null))
                    .withMessage("Invalid event \"foo\" at position 1");
        }

        /**
         * Cross-checks the conversion against a transcription of OSV's evaluation algorithm,
         * over randomly generated ranges.
         *
         * @see <a href="https://ossf.github.io/osv-schema/#evaluation">OSV evaluation algorithm</a>
         */
        @Nested
        class OsvEvaluationCrossCheck {

            private static final List<String> VERSIONS = List.of("0", "1", "2", "3", "4", "5", "6");
            private static final List<String> EVENT_KEYS = List.of("introduced", "fixed", "last_affected", "limit");

            @Test
            void versFromOsvRangeShouldMatchOsvEvaluationAlgorithm() {
                final var random = new Random(666L);

                for (int i = 0; i < 2_000; i++) {
                    final List<Map.Entry<String, String>> events = randomEvents(random);
                    final List<Vers> versList =
                            versFromOsvRange("ECOSYSTEM", "PyPI", events, /* databaseSpecific */ null);

                    for (final String version : VERSIONS) {
                        final boolean expected = includedInRange(version, events);

                        assertThat(versList.stream().anyMatch(vers -> vers.contains(version)))
                                .withFailMessage(
                                        "events=%s, version=%s, ranges=%s, expected=%s",
                                        events, version, versList, expected)
                                .isEqualTo(expected);
                    }
                }
            }

            private static List<Map.Entry<String, String>> randomEvents(Random random) {
                final var versions = new ArrayList<>(VERSIONS);
                Collections.shuffle(versions, random);

                final var events = new ArrayList<Map.Entry<String, String>>();
                for (final String version : versions.subList(0, 1 + random.nextInt(VERSIONS.size()))) {
                    events.add(Map.entry(EVENT_KEYS.get(random.nextInt(EVENT_KEYS.size())), version));
                }

                return events;
            }

            private static boolean includedInRange(String v, List<Map.Entry<String, String>> range) {
                if (beforeLimits(v, range)) {
                    final Version version = VersionFactory.forScheme(SCHEME_PYPI, v);
                    final var sortedEvents = new ArrayList<>(range);
                    sortedEvents.sort(comparing((Map.Entry<String, String> event) ->
                            VersionFactory.forScheme(SCHEME_PYPI, event.getValue())));

                    boolean vulnerable = false;
                    for (final Map.Entry<String, String> event : sortedEvents) {
                        final Version eventVersion = VersionFactory.forScheme(SCHEME_PYPI, event.getValue());

                        if ("introduced".equals(event.getKey()) && version.compareTo(eventVersion) >= 0) {
                            vulnerable = true;
                        } else if ("fixed".equals(event.getKey()) && version.compareTo(eventVersion) >= 0) {
                            vulnerable = false;
                        } else if ("last_affected".equals(event.getKey()) && version.compareTo(eventVersion) > 0) {
                            vulnerable = false;
                        }
                    }

                    return vulnerable;
                }

                return false;
            }

            private static boolean beforeLimits(String v, List<Map.Entry<String, String>> range) {
                final Version version = VersionFactory.forScheme(SCHEME_PYPI, v);
                boolean hasLimit = false;

                for (final Map.Entry<String, String> event : range) {
                    if (!"limit".equals(event.getKey())) {
                        continue;
                    }
                    hasLimit = true;
                    if (version.compareTo(VersionFactory.forScheme(SCHEME_PYPI, event.getValue())) < 0) {
                        return true;
                    }
                }

                // No limit events means an implicit { "limit": "*" }.
                return !hasLimit;
            }
        }
    }
}
