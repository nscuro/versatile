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

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_GEM;
import static io.github.nscuro.versatile.version.VersionUtils.isAsciiDigit;

import io.github.nscuro.versatile.spi.InvalidVersionException;
import io.github.nscuro.versatile.spi.Version;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see <a href="https://github.com/rubygems/rubygems/blob/master/lib/rubygems/version.rb">Gem::Version implementation</a>
 * @see <a href="https://github.com/rubygems/rubygems/blob/master/test/rubygems/test_gem_version.rb">Gem::Version test suite</a>
 * @see <a href="https://guides.rubygems.org/patterns/#semantic-versioning">RubyGems versioning guide</a>
 * @since 0.19.0
 */
public class GemVersion extends Version {

    public static class Provider extends AbstractBuiltinVersionProvider {

        public Provider() {
            super(Set.of(SCHEME_GEM), (scheme, versionStr) -> new GemVersion(versionStr));
        }
    }

    // Gem::Version::ANCHORED_VERSION_PATTERN. An empty (or blank) version is valid and equal to "0".
    private static final Pattern ANCHORED_VERSION_PATTERN =
            Pattern.compile("\\A\\s*([0-9]+(?:\\.[0-9a-zA-Z]+)*(-[0-9A-Za-z-]+(\\.[0-9A-Za-z-]+)*)?)?\\s*\\z");

    // Gem::Version#partition_segments: runs of digits or runs of letters.
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("\\d+|[a-zA-Z]+");

    // Gem::Version#canonical_segments cleanup passes.
    private static final Pattern TRAILING_ZEROS_PATTERN = Pattern.compile("(?<=[a-zA-Z.])[.0]+\\z");
    private static final Pattern LEADING_ZEROS_PATTERN = Pattern.compile("(?<=\\.|\\A)[0.]+(?=[a-zA-Z])");

    // Each element is either a BigInteger (numeric segment) or a String (letter segment).
    private final List<Object> canonicalSegments;
    private final boolean prerelease;

    GemVersion(String versionStr) {
        super(SCHEME_GEM, versionStr);

        if (!ANCHORED_VERSION_PATTERN.matcher(versionStr).matches()) {
            throw new InvalidVersionException(versionStr, "Malformed gem version: " + versionStr);
        }

        // Gem::Version#initialize
        final String normalized = versionStr.strip().replace("-", ".pre.");
        this.prerelease = containsLetter(normalized);
        this.canonicalSegments = canonicalSegments(normalized, prerelease);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStable() {
        return !prerelease;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Version other) {
        if (other instanceof final GemVersion otherVersion) {
            return compareSegments(this.canonicalSegments, otherVersion.canonicalSegments);
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(this.getClass().getName(), other.getClass().getName()));
    }

    // Gem::Version#canonical_segments.
    private static List<Object> canonicalSegments(String normalized, boolean prerelease) {
        String canonical = TRAILING_ZEROS_PATTERN.matcher(normalized).replaceFirst("");
        if (prerelease) {
            canonical = LEADING_ZEROS_PATTERN.matcher(canonical).replaceFirst("");
        }

        final List<Object> segments = new ArrayList<>();
        final Matcher matcher = SEGMENT_PATTERN.matcher(canonical);
        while (matcher.find()) {
            final String segment = matcher.group();
            if (isAsciiDigit(segment.charAt(0))) {
                segments.add(new BigInteger(segment));
            } else {
                segments.add(segment);
            }
        }

        return segments;
    }

    // Gem::Version#<=>.
    private static int compareSegments(List<Object> lhs, List<Object> rhs) {
        final int limit = Math.min(lhs.size(), rhs.size());

        int i = 0;
        for (; i < limit; i++) {
            final Object l = lhs.get(i);
            final Object r = rhs.get(i);
            if (l.equals(r)) {
                continue;
            }

            final boolean lNumeric = l instanceof BigInteger;
            final boolean rNumeric = r instanceof BigInteger;
            if (lNumeric != rNumeric) {
                return lNumeric ? 1 : -1;
            }
            if (lNumeric) {
                return ((BigInteger) l).compareTo((BigInteger) r);
            }

            return ((String) l).compareTo((String) r);
        }

        if (i < lhs.size()) {
            return compareTail(lhs, i, 1);
        }
        if (i < rhs.size()) {
            return compareTail(rhs, i, -1);
        }

        return 0;
    }

    private static int compareTail(List<Object> segments, int from, int sign) {
        for (int i = from; i < segments.size(); i++) {
            final Object segment = segments.get(i);
            if (segment instanceof String) {
                return -sign;
            }
            if (((BigInteger) segment).signum() != 0) {
                return sign;
            }
        }

        return 0;
    }

    private static boolean containsLetter(String value) {
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                return true;
            }
        }

        return false;
    }
}
