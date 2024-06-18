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

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_RPM;
import static io.github.nscuro.versatile.version.VersionUtils.isNumeric;

/**
 * A {@link Version} implementation for the {@code rpm} versioning scheme.
 *
 * @see <a href="https://rpm-software-management.github.io/rpm/manual/dependencies.html">RPM versioning docs</a>
 * @see <a href="https://github.com/rpm-software-management/rpm/blob/rpm-4.19.0-rc1/rpmio/rpmvercmp.c">RPM version comparison logic</a>
 */
public class RpmVersion extends Version {

    private static final Pattern VERSION_PATTERN = Pattern.compile("""
            ^((?<epoch>\\d+):)?(?<version>[^-]+?)(-(?<release>[^-]+))?$\
            """);
    private static final Pattern VERSION_SEGMENT_PATTERN = Pattern.compile("[a-zA-Z]+|[0-9]+|~|\\^");

    private final int epoch;
    private final String version;
    private final String release;

    /**
     * Create a new {@link RpmVersion}.
     *
     * @param versionStr The raw RPM version
     * @throws InvalidVersionException When the provided {@code versionStr} is not a valid RPM version
     */
    RpmVersion(final String versionStr) {
        super(SCHEME_RPM, versionStr);

        final Matcher versionMatcher = VERSION_PATTERN.matcher(versionStr);
        if (!versionMatcher.find()) {
            throw new InvalidVersionException(versionStr, """
                    Provided version "%s" does not match the RPM version format \
                    [epoch:]version[-release]\
                    """.formatted(versionStr));
        }

        this.epoch = Optional.ofNullable(versionMatcher.group("epoch")).map(Integer::parseInt).orElse(0);
        this.version = versionMatcher.group("version");
        this.release = versionMatcher.group("release");

        if (this.version == null) {
            throw new InvalidVersionException(versionStr, """
                    Provided version "%s" does not contain the mandatory version part, \
                    according to the RPM version format [epoch:]version[-release]\
                    """.formatted(versionStr));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Version other) {
        if (other instanceof final RpmVersion otherVersion) {
            if (this.epoch == otherVersion.epoch
                    && Objects.equals(this.version, otherVersion.version)
                    && Objects.equals(this.release, otherVersion.release)) {
                return 0;
            }

            int comparisonResult = Integer.compare(this.epoch, otherVersion.epoch);
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            comparisonResult = rpmVerCmp(this.version, otherVersion.version);
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            return rpmVerCmp(this.release, otherVersion.release);
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(this.getClass().getName(), other.getClass().getName()));
    }

    public int epoch() {
        return epoch;
    }

    public String version() {
        return version;
    }

    public String release() {
        return release;
    }

    private static int rpmVerCmp(final String a, final String b) {
        // Easy comparison to see if versions are identical.
        if (Objects.equals(a, b)) {
            return 0;
        }

        final var segmentsA = new ArrayList<String>();
        final var segmentsB = new ArrayList<String>();
        final Matcher matcherA = VERSION_SEGMENT_PATTERN.matcher(a);
        while (matcherA.find()) {
            segmentsA.add(matcherA.group());
        }
        final Matcher matcherB = VERSION_SEGMENT_PATTERN.matcher(b);
        while (matcherB.find()) {
            segmentsB.add(matcherB.group());
        }

        // Loop through each version segment of a and b, and compare them.
        for (int i = 0; i < Math.max(segmentsA.size(), segmentsB.size()); i++) {
            var segmentA = "";
            if (segmentsA.size() >= (i + 1)) {
                segmentA = segmentsA.get(i);
            }
            var segmentB = "";
            if (segmentsB.size() >= (i + 1)) {
                segmentB = segmentsB.get(i);
            }

            // Handle the tilde separator, it sorts before everything else.
            if (segmentA.equals("~") || segmentB.equals("~")) {
                if (!segmentA.equals("~")) {
                    return 1;
                }
                if (!segmentB.equals("~")) {
                    return -1;
                }
                continue;
            }

            // Handle caret separator. Concept is the same as tilde,
            // except that if one of the strings ends (base version),
            // the other is considered as higher version.
            if (segmentA.startsWith("^") || segmentB.startsWith("^")) {
                if (segmentA.isEmpty()) {
                    return -1;
                }
                if (segmentB.isEmpty()) {
                    return 1;
                }
                if (!segmentA.equals("^")) {
                    return 1;
                }
                if (!segmentB.equals("^")) {
                    return -1;
                }
                continue;
            }

            // If we ran to the end of either, we are finished with the loop.
            if (segmentA.isEmpty() || segmentB.isEmpty()) {
                break;
            }

            if (isNumeric(segmentA)) {
                // Numeric segments are always newer than alpha segments.
                if (!isNumeric(segmentB)) {
                    return 1;
                }

                // Throw away any leading zeroes - it's a number, right?
                segmentA = segmentA.replaceFirst("^0*", "");
                segmentB = segmentB.replaceFirst("^0*", "");

                // Whichever number has more digits wins.
                if (segmentA.length() > segmentB.length()) {
                    return 1;
                } else if (segmentB.length() > segmentA.length()) {
                    return -1;
                }
            } else if (isNumeric(segmentB)) {
                return -1;
            }

            // Compare both segments as strings. Don't return if they are equal
            // because there might be more segments to compare.
            int strComparisonResult = segmentA.compareTo(segmentB);
            if (strComparisonResult != 0) {
                return strComparisonResult;
            }
        }

        // This catches the case where all numeric and alpha segments have
        // compared identically but the segment separating characters were
        // different.
        return Integer.compare(segmentsA.size(), segmentsB.size());
    }

}
