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

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_RPM;
import static io.github.nscuro.versatile.version.VersionUtils.isAsciiNumeric;

import io.github.nscuro.versatile.spi.InvalidVersionException;
import io.github.nscuro.versatile.spi.Version;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link Version} implementation for the {@code rpm} versioning scheme.
 *
 * @see <a href="https://rpm-software-management.github.io/rpm/manual/dependencies.html">RPM versioning docs</a>
 * @see <a href="https://github.com/rpm-software-management/rpm/blob/rpm-4.19.0-rc1/rpmio/rpmvercmp.c">RPM version comparison logic</a>
 */
public class RpmVersion extends Version {

    /**
     * @since 0.8.0
     */
    public static class Provider extends AbstractBuiltinVersionProvider {

        public Provider() {
            super(Set.of(SCHEME_RPM), (scheme, versionStr) -> new RpmVersion(versionStr));
        }
    }

    private static final Pattern VERSION_PATTERN = Pattern.compile("""
            ^((?<epoch>\\d+):)?(?<version>[^-]+?)(-(?<release>[^-]+))?$\
            """);
    private static final Pattern VERSION_SEGMENT_PATTERN = Pattern.compile("[a-zA-Z]+|[0-9]+|~|\\^");

    private final int epoch;
    private final String version;
    private final String release;
    private final String[] versionSegments;
    private final String[] releaseSegments;

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

        this.epoch = Optional.ofNullable(versionMatcher.group("epoch"))
                .map(Integer::parseInt)
                .orElse(0);
        this.version = versionMatcher.group("version");
        this.release = Optional.ofNullable(versionMatcher.group("release")).orElse("0");

        if (this.version == null) {
            throw new InvalidVersionException(versionStr, """
                    Provided version "%s" does not contain the mandatory version part, \
                    according to the RPM version format [epoch:]version[-release]\
                    """.formatted(versionStr));
        }

        this.versionSegments = splitSegments(this.version);
        this.releaseSegments = splitSegments(this.release);
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

            comparisonResult = !this.version.equals(otherVersion.version)
                    ? rpmVerCmp(this.versionSegments, otherVersion.versionSegments)
                    : 0;
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            return !this.release.equals(otherVersion.release)
                    ? rpmVerCmp(this.releaseSegments, otherVersion.releaseSegments)
                    : 0;
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

    private static String[] splitSegments(String value) {
        final var segments = new ArrayList<String>();
        final Matcher matcher = VERSION_SEGMENT_PATTERN.matcher(value);
        while (matcher.find()) {
            segments.add(matcher.group());
        }

        return segments.toArray(new String[0]);
    }

    private static int rpmVerCmp(String[] segmentsA, String[] segmentsB) {
        // Loop through each version segment of a and b, and compare them.
        for (int i = 0; i < Math.max(segmentsA.length, segmentsB.length); i++) {
            String segmentA = i < segmentsA.length ? segmentsA[i] : "";
            String segmentB = i < segmentsB.length ? segmentsB[i] : "";

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

            if (isAsciiNumeric(segmentA)) {
                // Numeric segments are always newer than alpha segments.
                if (!isAsciiNumeric(segmentB)) {
                    return 1;
                }

                // Compare numerically, ignoring any leading zeroes.
                final int numComparisonResult = compareNumericSegments(segmentA, segmentB);
                if (numComparisonResult != 0) {
                    return numComparisonResult;
                }
            } else if (isAsciiNumeric(segmentB)) {
                return -1;
            } else {
                // Compare both segments as strings. Don't return if they are equal
                // because there might be more segments to compare.
                final int strComparisonResult = segmentA.compareTo(segmentB);
                if (strComparisonResult != 0) {
                    return strComparisonResult;
                }
            }
        }

        // This catches the case where all numeric and alpha segments have
        // compared identically but the segment separating characters were
        // different.
        return Integer.compare(segmentsA.length, segmentsB.length);
    }

    private static int compareNumericSegments(final String a, final String b) {
        // Indices of the first non-zero digit (i.e. position after leading zeroes).
        final int startA = leadingZeroEnd(a);
        final int startB = leadingZeroEnd(b);
        final int lenA = a.length() - startA;
        final int lenB = b.length() - startB;

        // Whichever number has more significant digits wins.
        if (lenA != lenB) {
            return lenA > lenB ? 1 : -1;
        }

        for (int i = 0; i < lenA; i++) {
            final char ca = a.charAt(startA + i);
            final char cb = b.charAt(startB + i);
            if (ca != cb) {
                return ca < cb ? -1 : 1;
            }
        }

        return 0;
    }

    private static int leadingZeroEnd(String segment) {
        int i = 0;
        while (i < segment.length() && segment.charAt(i) == '0') {
            i++;
        }

        return i;
    }
}
