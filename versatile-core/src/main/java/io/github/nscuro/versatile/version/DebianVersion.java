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

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_DEBIAN;
import static io.github.nscuro.versatile.version.VersionUtils.isAsciiNumeric;

import io.github.nscuro.versatile.spi.InvalidVersionException;
import io.github.nscuro.versatile.spi.Version;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see <a href="https://manpages.debian.org/stretch/dpkg-dev/deb-version.5.en.html">Debian version format and sorting algorithm</a>
 * @see <a href="https://github.com/romlok/python-debian/blob/be7a55c8415da239fb408ca4d22d5d3a52fbede1/lib/debian/debian_support.py#L177-L258">python-debian implementation</a>
 */
public class DebianVersion extends Version {

    /**
     * @since 0.8.0
     */
    public static class Provider extends AbstractBuiltinVersionProvider {

        public Provider() {
            super(Set.of(SCHEME_DEBIAN), (scheme, versionStr) -> new DebianVersion(versionStr));
        }
    }

    private static final Pattern VERSION_PATTERN = Pattern.compile("""
            ^((?<epoch>\\d+):)?(?<upstreamVersion>[A-Za-z0-9.+-:~]+?)(-(?<debianRevision>[A-Za-z0-9+.~]+))?$\
            """);
    private static final Pattern DIGIT_OR_NON_DIGIT_PATTERN = Pattern.compile("\\d+|\\D+");

    private final int epoch;
    private final String upstreamVersion;
    private final String debianRevision;
    private final String[] upstreamVersionSegments;
    private final String[] debianRevisionSegments;

    DebianVersion(final String versionStr) {
        super(SCHEME_DEBIAN, versionStr);

        final Matcher versionMatcher = VERSION_PATTERN.matcher(versionStr);
        if (!versionMatcher.find()) {
            throw new InvalidVersionException(versionStr, """
                    Provided version "%s" does not match the Debian version format \
                    [epoch:]upstream-version[-debian-revision]\
                    """.formatted(versionStr));
        }

        this.epoch = Optional.ofNullable(versionMatcher.group("epoch"))
                .map(Integer::parseInt)
                .orElse(0);
        this.upstreamVersion = versionMatcher.group("upstreamVersion");
        this.debianRevision =
                Optional.ofNullable(versionMatcher.group("debianRevision")).orElse("0");

        if (this.upstreamVersion == null) {
            throw new InvalidVersionException(versionStr, """
                    Provided version "%s" does not contain the mandatory upstream version, \
                    according to the Debian version format [epoch:]upstream-version[-debian-revision]\
                    """.formatted(versionStr));
        }

        this.upstreamVersionSegments = splitSegments(this.upstreamVersion);
        this.debianRevisionSegments = splitSegments(this.debianRevision);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStable() {
        // Stability in Debian is mostly dictated by the distro version and its repository.
        // TODO: Is there a convention for Debian package versions that we can rely on?
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Version other) {
        if (other instanceof final DebianVersion otherVersion) {
            int comparisonResult = Integer.compare(this.epoch, otherVersion.epoch);
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            comparisonResult = compareVersionPart(this.upstreamVersionSegments, otherVersion.upstreamVersionSegments);
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            return compareVersionPart(this.debianRevisionSegments, otherVersion.debianRevisionSegments);
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(this.getClass().getName(), other.getClass().getName()));
    }

    public int epoch() {
        return epoch;
    }

    public String upstreamVersion() {
        return upstreamVersion;
    }

    public String debianRevision() {
        return debianRevision;
    }

    private static String[] splitSegments(String versionPart) {
        final var segments = new ArrayList<String>();
        final Matcher matcher = DIGIT_OR_NON_DIGIT_PATTERN.matcher(versionPart);
        while (matcher.find()) {
            segments.add(matcher.group());
        }

        return segments.toArray(new String[0]);
    }

    private static int compareVersionPart(String[] segmentsA, String[] segmentsB) {
        final int max = Math.max(segmentsA.length, segmentsB.length);

        for (int i = 0; i < max; i++) {
            final String a = i < segmentsA.length ? segmentsA[i] : "0";
            final String b = i < segmentsB.length ? segmentsB[i] : "0";

            final int comparisonResult;
            if (isAsciiNumeric(a) && isAsciiNumeric(b)) {
                comparisonResult = Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
            } else {
                comparisonResult = compareString(a, b);
            }

            if (comparisonResult != 0) {
                return comparisonResult;
            }
        }

        return 0;
    }

    private static int compareString(final String versionPartA, final String versionPartB) {
        final int max = Math.max(versionPartA.length(), versionPartB.length());

        for (int i = 0; i < max; i++) {
            final int a = i < versionPartA.length() ? charOrder(versionPartA.charAt(i)) : 0;
            final int b = i < versionPartB.length() ? charOrder(versionPartB.charAt(i)) : 0;

            final int comparisonResult = Integer.compare(a, b);
            if (comparisonResult != 0) {
                return comparisonResult;
            }
        }

        return 0;
    }

    private static int charOrder(final char x) {
        if (x == '~') {
            return -1;
        } else if (Character.isDigit(x)) {
            return Character.getNumericValue(x) + 1;
        } else if (Character.isAlphabetic(x)) {
            return x;
        }

        return ((int) x) + 255;
    }
}
