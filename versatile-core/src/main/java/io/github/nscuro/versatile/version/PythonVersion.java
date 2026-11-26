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

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_PYPI;
import static io.github.nscuro.versatile.version.VersionUtils.isAsciiNumeric;

import io.github.nscuro.versatile.spi.InvalidVersionException;
import io.github.nscuro.versatile.spi.Version;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * @see <a href="https://peps.python.org/pep-0440/">PEP 440 – Version Identification and Dependency Specification</a>
 * @see <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/">Python Packaging User Guide - Version Specifiers</a>
 * @since 0.14.0
 */
public class PythonVersion extends Version {

    public static final class Provider extends AbstractBuiltinVersionProvider {

        public Provider() {
            super(Set.of(SCHEME_PYPI), (scheme, versionStr) -> new PythonVersion(versionStr));
        }
    }

    public record PreRelease(Type type, BigInteger number) {

        public enum Type {
            ALPHA,
            BETA,
            RC;

            static Type of(String spelling) {
                return switch (spelling.toLowerCase(Locale.ROOT)) {
                    case "a", "alpha" -> ALPHA;
                    case "b", "beta" -> BETA;
                    default -> RC;
                };
            }

            String normalized() {
                return switch (this) {
                    case ALPHA -> "a";
                    case BETA -> "b";
                    case RC -> "rc";
                };
            }
        }
    }

    // https://peps.python.org/pep-0440/#appendix-b-parsing-version-strings-with-regular-expressions
    private static final Pattern VERSION_PATTERN = Pattern.compile("""
            ^\
            v?\
            (?:(?<epoch>[0-9]+)!)?\
            (?<release>[0-9]+(?:\\.[0-9]+)*)\
            (?<pre>[-_.]?(?<preType>a|alpha|b|beta|c|rc|pre|preview)[-_.]?(?<preNum>[0-9]+)?)?\
            (?<post>-(?<postNum1>[0-9]+)|[-_.]?(?<postType>post|rev|r)[-_.]?(?<postNum2>[0-9]+))?\
            (?<dev>[-_.]?dev[-_.]?(?<devNum>[0-9]+)?)?\
            (?:\\+(?<local>[a-zA-Z0-9]+(?:[-_.][a-zA-Z0-9]+)*))?\
            $""", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEGMENT_SEPARATOR_PATTERN = Pattern.compile("\\.");
    private static final Pattern LOCAL_NORMALIZE_PATTERN = Pattern.compile("[-_]");

    // CPython's default int_max_str_digits. Also caps the superlinear cost of parsing BigInteger.
    private static final int MAX_NUMBER_DIGITS = 4300;
    private static final Pattern OVERSIZED_NUMBER_PATTERN = Pattern.compile("[0-9]{" + (MAX_NUMBER_DIGITS + 1) + "}");

    private final BigInteger epoch;
    private final List<BigInteger> release;
    private final @Nullable PreRelease preRelease;
    private final @Nullable BigInteger postRelease;
    private final @Nullable BigInteger devRelease;
    private final @Nullable String local;

    PythonVersion(String versionStr) {
        this(requireMatch(versionStr));
    }

    private PythonVersion(Matcher matcher) {
        super(SCHEME_PYPI, normalize(matcher));

        this.epoch = parseEpoch(matcher.group("epoch"));
        this.release = parseRelease(matcher.group("release"));
        this.preRelease = parsePreRelease(matcher.group("preType"), matcher.group("preNum"));
        this.postRelease =
                parsePostRelease(matcher.group("post"), matcher.group("postNum1"), matcher.group("postNum2"));
        this.devRelease = parseDevRelease(matcher.group("dev"), matcher.group("devNum"));
        this.local = parseLocal(matcher.group("local"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStable() {
        return preRelease == null && devRelease == null && local == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Version other) {
        if (!(other instanceof final PythonVersion otherVersion)) {
            throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                    .formatted(this.getClass().getName(), other.getClass().getName()));
        }

        int result = this.epoch.compareTo(otherVersion.epoch);
        if (result != 0) {
            return result;
        }

        result = compareRelease(this.release, otherVersion.release);
        if (result != 0) {
            return result;
        }

        // X.Y.devN < X.YaN.devM < X.YaN < X.YaN.postP < X.YbN < ... < X.Y < X.Y.postN

        result = comparePreReleaseWithDev(
                this.preRelease,
                this.devRelease,
                otherVersion.preRelease,
                otherVersion.devRelease,
                this.postRelease,
                otherVersion.postRelease);
        if (result != 0) {
            return result;
        }

        result = compareOptionalInt(this.postRelease, otherVersion.postRelease);
        if (result != 0) {
            return result;
        }

        result = compareDevRelease(this.devRelease, otherVersion.devRelease);
        if (result != 0) {
            return result;
        }

        return compareLocal(this.local, otherVersion.local);
    }

    public BigInteger epoch() {
        return epoch;
    }

    public List<BigInteger> release() {
        return List.copyOf(release);
    }

    public @Nullable PreRelease preRelease() {
        return preRelease;
    }

    public @Nullable BigInteger postRelease() {
        return postRelease;
    }

    public @Nullable BigInteger devRelease() {
        return devRelease;
    }

    public @Nullable String local() {
        return local;
    }

    private static Matcher requireMatch(String versionStr) {
        final Matcher matcher = VERSION_PATTERN.matcher(versionStr.strip());
        if (!matcher.matches()) {
            throw new InvalidVersionException(versionStr, """
                    Provided version "%s" does not match PEP 440 format: \
                    [N!]N(.N)*[{a|b|rc}N][.postN][.devN][+local]\
                    """.formatted(versionStr));
        }
        if (OVERSIZED_NUMBER_PATTERN.matcher(versionStr).find()) {
            throw new InvalidVersionException(
                    versionStr, "Numbers must not exceed %d digits".formatted(MAX_NUMBER_DIGITS));
        }

        return matcher;
    }

    private static String normalize(Matcher matcher) {
        final var normalized = new StringBuilder();

        final BigInteger epoch = parseEpoch(matcher.group("epoch"));
        if (epoch.signum() != 0) {
            normalized.append(epoch).append('!');
        }

        final List<BigInteger> release = parseRelease(matcher.group("release"));
        for (int i = 0; i < release.size(); i++) {
            if (i > 0) {
                normalized.append('.');
            }
            normalized.append(release.get(i));
        }

        final PreRelease preRelease = parsePreRelease(matcher.group("preType"), matcher.group("preNum"));
        if (preRelease != null) {
            normalized.append(preRelease.type().normalized()).append(preRelease.number());
        }

        final BigInteger postRelease =
                parsePostRelease(matcher.group("post"), matcher.group("postNum1"), matcher.group("postNum2"));
        if (postRelease != null) {
            normalized.append(".post").append(postRelease);
        }

        final BigInteger devRelease = parseDevRelease(matcher.group("dev"), matcher.group("devNum"));
        if (devRelease != null) {
            normalized.append(".dev").append(devRelease);
        }

        final String local = parseLocal(matcher.group("local"));
        if (local != null) {
            normalized.append('+').append(local);
        }

        return normalized.toString();
    }

    private static BigInteger parseEpoch(@Nullable String epochStr) {
        if (epochStr == null || epochStr.isBlank()) {
            return BigInteger.ZERO;
        }

        return new BigInteger(epochStr);
    }

    private static List<BigInteger> parseRelease(String releaseStr) {
        final String[] parts = SEGMENT_SEPARATOR_PATTERN.split(releaseStr);
        final var release = new ArrayList<BigInteger>(parts.length);

        for (final String part : parts) {
            release.add(new BigInteger(part));
        }

        return release;
    }

    private static @Nullable PreRelease parsePreRelease(@Nullable String preType, @Nullable String preNum) {
        if (preType == null) {
            return null;
        }

        final PreRelease.Type type = PreRelease.Type.of(preType);
        final BigInteger num = (preNum == null || preNum.isBlank()) ? BigInteger.ZERO : new BigInteger(preNum);
        return new PreRelease(type, num);
    }

    private static @Nullable BigInteger parsePostRelease(
            @Nullable String postStr, @Nullable String postNum1, @Nullable String postNum2) {
        if (postStr == null) {
            return null;
        }

        if (postNum1 != null && !postNum1.isBlank()) {
            return new BigInteger(postNum1);
        }

        if (postNum2 != null && !postNum2.isBlank()) {
            return new BigInteger(postNum2);
        }

        return BigInteger.ZERO;
    }

    private static @Nullable BigInteger parseDevRelease(@Nullable String devStr, @Nullable String devNum) {
        if (devStr == null) {
            return null;
        }

        return (devNum == null || devNum.isBlank()) ? BigInteger.ZERO : new BigInteger(devNum);
    }

    private static @Nullable String parseLocal(@Nullable String localStr) {
        if (localStr == null || localStr.isBlank()) {
            return null;
        }

        return LOCAL_NORMALIZE_PATTERN
                .matcher(localStr.toLowerCase(Locale.ROOT))
                .replaceAll(".");
    }

    private static int compareRelease(List<BigInteger> release1, List<BigInteger> release2) {
        final int maxLen = Math.max(release1.size(), release2.size());

        for (int i = 0; i < maxLen; i++) {
            final BigInteger v1 = i < release1.size() ? release1.get(i) : BigInteger.ZERO;
            final BigInteger v2 = i < release2.size() ? release2.get(i) : BigInteger.ZERO;

            final int result = v1.compareTo(v2);
            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

    private static int comparePreReleaseWithDev(
            @Nullable PreRelease pre1,
            @Nullable BigInteger dev1,
            @Nullable PreRelease pre2,
            @Nullable BigInteger dev2,
            @Nullable BigInteger post1,
            @Nullable BigInteger post2) {
        final boolean isDev1OfFinal = pre1 == null && post1 == null && dev1 != null;
        final boolean isDev2OfFinal = pre2 == null && post2 == null && dev2 != null;

        if (isDev1OfFinal && !isDev2OfFinal) {
            return -1;
        }
        if (!isDev1OfFinal && isDev2OfFinal) {
            return 1;
        }
        if (isDev1OfFinal && isDev2OfFinal) {
            return 0;
        }

        return comparePreRelease(pre1, pre2);
    }

    private static int comparePreRelease(@Nullable PreRelease pre1, @Nullable PreRelease pre2) {
        if (pre1 == null && pre2 == null) {
            return 0;
        }
        if (pre1 == null) {
            return 1;
        }
        if (pre2 == null) {
            return -1;
        }

        int result = pre1.type.compareTo(pre2.type);
        if (result != 0) {
            return result;
        }

        return pre1.number.compareTo(pre2.number);
    }

    private static int compareOptionalInt(@Nullable BigInteger int1, @Nullable BigInteger int2) {
        // null is treated as less than any value (for post-releases).
        if (int1 == null && int2 == null) {
            return 0;
        }

        if (int1 == null) {
            return -1;
        }
        if (int2 == null) {
            return 1;
        }

        return int1.compareTo(int2);
    }

    private static int compareDevRelease(@Nullable BigInteger dev1, @Nullable BigInteger dev2) {
        if (dev1 == null && dev2 == null) {
            return 0;
        }
        if (dev1 == null) {
            return 1;
        }
        if (dev2 == null) {
            return -1;
        }

        return dev1.compareTo(dev2);
    }

    private static int compareLocal(@Nullable String local1, @Nullable String local2) {
        if (local1 == null && local2 == null) {
            return 0;
        }
        if (local1 == null) {
            return -1;
        }
        if (local2 == null) {
            return 1;
        }

        final String[] parts1 = SEGMENT_SEPARATOR_PATTERN.split(local1);
        final String[] parts2 = SEGMENT_SEPARATOR_PATTERN.split(local2);
        final int minLen = Math.min(parts1.length, parts2.length);

        for (int i = 0; i < minLen; i++) {
            final int result = compareLocalSegment(parts1[i], parts2[i]);
            if (result != 0) {
                return result;
            }
        }

        return Integer.compare(parts1.length, parts2.length);
    }

    private static int compareLocalSegment(String segment1, String segment2) {
        final boolean numeric1 = isAsciiNumeric(segment1);
        final boolean numeric2 = isAsciiNumeric(segment2);

        if (numeric1 && numeric2) {
            return new BigInteger(segment1).compareTo(new BigInteger(segment2));
        }
        if (numeric1) {
            return 1;
        }
        if (numeric2) {
            return -1;
        }

        return segment1.compareTo(segment2);
    }
}
