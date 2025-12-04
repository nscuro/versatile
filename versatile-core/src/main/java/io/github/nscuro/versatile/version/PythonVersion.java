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
import io.github.nscuro.versatile.spi.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_PYPI;

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

    public record PreRelease(Type type, int number) {

        public enum Type {
            ALPHA,
            BETA,
            RC
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
                    $""",
            Pattern.CASE_INSENSITIVE);

    private final int epoch;
    private final List<Integer> release;
    private final PreRelease preRelease;
    private final Integer postRelease;
    private final Integer devRelease;
    private final String local;

    PythonVersion(final String versionStr) {
        super(SCHEME_PYPI, normalize(versionStr));

        final Matcher matcher = VERSION_PATTERN.matcher(versionStr.strip());
        if (!matcher.matches()) {
            throw new InvalidVersionException(versionStr, """
                    Provided version "%s" does not match PEP 440 format: \
                    [N!]N(.N)*[{a|b|rc}N][.postN][.devN][+local]\
                    """.formatted(versionStr));
        }

        this.epoch = parseEpoch(matcher.group("epoch"));
        this.release = parseRelease(matcher.group("release"));
        this.preRelease = parsePreRelease(
                matcher.group("pre"),
                matcher.group("preType"),
                matcher.group("preNum"));
        this.postRelease = parsePostRelease(
                matcher.group("post"),
                matcher.group("postNum1"),
                matcher.group("postNum2"));
        this.devRelease = parseDevRelease(
                matcher.group("dev"),
                matcher.group("devNum"));
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

        int result = Integer.compare(this.epoch, otherVersion.epoch);
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

    public int epoch() {
        return epoch;
    }

    public List<Integer> release() {
        return List.copyOf(release);
    }

    public PreRelease preRelease() {
        return preRelease;
    }

    public Integer postRelease() {
        return postRelease;
    }

    public Integer devRelease() {
        return devRelease;
    }

    public String local() {
        return local;
    }

    private static String normalize(final String versionStr) {
        // Normalize the version according to PEP 440:
        // https://peps.python.org/pep-0440/#normalization

        // https://peps.python.org/pep-0440/#leading-and-trailing-whitespace
        String normalized = versionStr.strip();

        // https://peps.python.org/pep-0440/#preceding-v-character
        normalized = normalized.replaceFirst("^[vV]", "");

        // https://peps.python.org/pep-0440/#integer-normalization
        normalized = normalized.replaceAll("^([0-9]+)!", "$1!");

        // https://peps.python.org/pep-0440/#pre-release-separators
        // https://peps.python.org/pep-0440/#pre-release-spelling
        normalized = normalized.replaceAll("[-_.]?(alpha)", "a");
        normalized = normalized.replaceAll("[-_.]?(beta)", "b");
        normalized = normalized.replaceAll("[-_.]?(c|pre|preview)", "rc");
        normalized = normalized.replaceAll("(a|b|rc)[-_.]?([0-9]+)", "$1$2");
        normalized = normalized.replaceAll("(a|b|rc)(?![0-9])", "$10");

        // https://peps.python.org/pep-0440/#post-release-separators
        // https://peps.python.org/pep-0440/#post-release-spelling
        normalized = normalized.replaceAll("[-_.]?(post|rev|r)[-_.]?([0-9]+)", ".post$2");
        normalized = normalized.replaceAll("-([0-9]+)(?!.*post)", ".post$1");

        // https://peps.python.org/pep-0440/#development-release-separators
        normalized = normalized.replaceAll("[-_.]?dev[-_.]?([0-9]+)", ".dev$1");
        normalized = normalized.replaceAll("[-_.]?dev(?![0-9])", ".dev0");

        // https://peps.python.org/pep-0440/#local-version-segments
        normalized = normalized.replaceAll("\\+([a-zA-Z0-9]+)[-_.]", "+$1.");

        // https://peps.python.org/pep-0440/#case-sensitivity
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static int parseEpoch(final String epochStr) {
        if (epochStr == null || epochStr.isBlank()) {
            return 0;
        }

        return Integer.parseInt(epochStr);
    }

    private static List<Integer> parseRelease(final String releaseStr) {
        if (releaseStr == null || releaseStr.isBlank()) {
            throw new InvalidVersionException(releaseStr, "Release segment is required");
        }

        final String[] parts = releaseStr.split("\\.");
        final var release = new ArrayList<Integer>(parts.length);

        for (final String part : parts) {
            release.add(Integer.parseInt(part));
        }

        return release;
    }

    private static PreRelease parsePreRelease(final String preStr, final String preType, final String preNum) {
        if (preStr == null || preType == null) {
            return null;
        }

        final PreRelease.Type type = switch (preType.toLowerCase(Locale.ROOT)) {
            case "a", "alpha" -> PreRelease.Type.ALPHA;
            case "b", "beta" -> PreRelease.Type.BETA;
            case "c", "rc", "pre", "preview" -> PreRelease.Type.RC;
            default -> throw new InvalidVersionException(preType, "Unknown pre-release type: " + preType);
        };

        final int num = (preNum == null || preNum.isBlank()) ? 0 : Integer.parseInt(preNum);
        return new PreRelease(type, num);
    }

    private static Integer parsePostRelease(
            final String postStr,
            final String postNum1,
            final String postNum2) {
        if (postStr == null) {
            return null;
        }

        if (postNum1 != null && !postNum1.isBlank()) {
            return Integer.parseInt(postNum1);
        }

        if (postNum2 != null && !postNum2.isBlank()) {
            return Integer.parseInt(postNum2);
        }

        return 0;
    }

    private static Integer parseDevRelease(final String devStr, final String devNum) {
        if (devStr == null) {
            return null;
        }

        return (devNum == null || devNum.isBlank()) ? 0 : Integer.parseInt(devNum);
    }

    private static String parseLocal(final String localStr) {
        if (localStr == null || localStr.isBlank()) {
            return null;
        }

        return localStr.toLowerCase(Locale.ROOT).replaceAll("[-_]", ".");
    }

    private static int compareRelease(final List<Integer> release1, final List<Integer> release2) {
        final int maxLen = Math.max(release1.size(), release2.size());

        for (int i = 0; i < maxLen; i++) {
            final int v1 = i < release1.size() ? release1.get(i) : 0;
            final int v2 = i < release2.size() ? release2.get(i) : 0;

            final int result = Integer.compare(v1, v2);
            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

    private static int comparePreReleaseWithDev(
            final PreRelease pre1,
            final Integer dev1,
            final PreRelease pre2,
            final Integer dev2,
            final Integer post1,
            final Integer post2) {
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

    private static int comparePreRelease(final PreRelease pre1, final PreRelease pre2) {
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

        return Integer.compare(pre1.number, pre2.number);
    }

    private static int compareOptionalInt(final Integer int1, final Integer int2) {
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

        return Integer.compare(int1, int2);
    }

    private static int compareDevRelease(final Integer dev1, final Integer dev2) {
        if (dev1 == null && dev2 == null) {
            return 0;
        }
        if (dev1 == null) {
            return 1;
        }
        if (dev2 == null) {
            return -1;
        }

        return Integer.compare(dev1, dev2);
    }

    private static int compareLocal(final String local1, final String local2) {
        if (local1 == null && local2 == null) {
            return 0;
        }
        if (local1 == null) {
            return -1;
        }
        if (local2 == null) {
            return 1;
        }

        final String[] parts1 = local1.split("\\.");
        final String[] parts2 = local2.split("\\.");
        final int maxLen = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLen; i++) {
            final String p1 = i < parts1.length ? parts1[i] : "";
            final String p2 = i < parts2.length ? parts2[i] : "";

            final int result;
            if (p1.matches("\\d+") && p2.matches("\\d+")) {
                result = Integer.compare(Integer.parseInt(p1), Integer.parseInt(p2));
            } else {
                result = p1.compareTo(p2);
            }

            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

}
