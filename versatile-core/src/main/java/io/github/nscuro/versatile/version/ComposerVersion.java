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

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_COMPOSER;

/**
 * @see <a href="https://github.com/composer/semver/blob/main/src/VersionParser.php">Composer VersionParser</a>
 * @see <a href="https://getcomposer.org/doc/articles/versions.md">Composer Versions</a>
 */
public class ComposerVersion extends Version {

    public static class Provider extends AbstractBuiltinVersionProvider {

        public Provider() {
            super(Set.of(SCHEME_COMPOSER), (scheme, versionStr) -> new ComposerVersion(versionStr));
        }

    }

    private enum Stability {

        DEV("dev"),
        ALPHA("alpha"),
        BETA("beta"),
        RC("RC"),
        STABLE("stable"),
        PATCH("patch");

        private final String label;

        Stability(String label) {
            this.label = label;
        }

        private static Stability of(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "alpha", "a" -> ALPHA;
                case "beta", "b" -> BETA;
                case "rc" -> RC;
                case "stable" -> STABLE;
                case "patch", "p", "pl" -> PATCH;
                default -> throw new IllegalArgumentException("Unknown stability: " + value);
            };
        }

    }

    private static final String MODIFIER_REGEX =
            "[._-]?(?:(stable|beta|b|RC|alpha|a|patch|pl|p)((?:[.-]?\\d+)*+)?)?([.-]?dev)?";
    private static final Pattern CLASSICAL_PATTERN = Pattern.compile(
            "^[vV]?(\\d{1,5}(?:\\.\\d+){0,3})" + MODIFIER_REGEX + "$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "^[vV]?(\\d{4}(?:[.:-]?\\d{2}){1,6}(?:[.:-]?\\d{1,3}){0,2})" + MODIFIER_REGEX + "$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BRANCH_NUM_PATTERN = Pattern.compile(
            "^[vV]?(\\d+(?:\\.\\d+)*\\.[xX])[.-]?dev$");

    private static final Pattern ALIAS_PATTERN = Pattern.compile(
            "^([^,\\s]++) +as +[^,\\s]++$");
    private static final Pattern STABILITY_FLAG_PATTERN = Pattern.compile(
            "@(?:stable|RC|beta|alpha|dev)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern METADATA_PATTERN = Pattern.compile(
            "^([^,\\s+]++)\\+\\S++$");

    private final String branchName;
    private final String[] originalComponents;
    private final long[] numericComponents;
    private final Stability stability;
    private final String stabilityNumDisplay;
    private final int[] stabilityNumbers;
    private final boolean isDev;
    private final String normalizedString;

    ComposerVersion(final String versionStr) {
        super(SCHEME_COMPOSER, versionStr);

        if (versionStr == null || versionStr.isEmpty()) {
            throw new InvalidVersionException(versionStr, "Version must not be empty");
        }

        String version = versionStr.trim();

        if (version.isEmpty()) {
            throw new InvalidVersionException(versionStr, "Version must not be empty");
        }

        final Matcher aliasMatcher = ALIAS_PATTERN.matcher(version);
        if (aliasMatcher.matches()) {
            version = aliasMatcher.group(1);
        }

        final Matcher stabilityFlagMatcher = STABILITY_FLAG_PATTERN.matcher(version);
        if (stabilityFlagMatcher.find()) {
            version = version.substring(0, stabilityFlagMatcher.start());
        }

        final String lower = version.toLowerCase(Locale.ROOT);
        if ("master".equals(lower) || "trunk".equals(lower) || "default".equals(lower)) {
            this.branchName = lower;
            this.originalComponents = new String[0];
            this.numericComponents = new long[0];
            this.stability = Stability.DEV;
            this.stabilityNumbers = new int[0];
            this.stabilityNumDisplay = null;
            this.isDev = false;
            this.normalizedString = "dev-" + this.branchName;
            return;
        }

        if (lower.startsWith("dev-")) {
            final String name = version.substring(4);
            if (name.isEmpty()) {
                throw new InvalidVersionException(versionStr, "Branch name must not be empty");
            }
            this.branchName = name;
            this.originalComponents = new String[0];
            this.numericComponents = new long[0];
            this.stability = Stability.DEV;
            this.stabilityNumbers = new int[0];
            this.stabilityNumDisplay = null;
            this.isDev = false;
            this.normalizedString = "dev-" + this.branchName;
            return;
        }

        this.branchName = null;

        final Matcher metadataMatcher = METADATA_PATTERN.matcher(version);
        if (metadataMatcher.matches()) {
            version = metadataMatcher.group(1);
        }

        final Matcher branchMatcher = BRANCH_NUM_PATTERN.matcher(version);
        if (branchMatcher.matches()) {
            final String[] parts = branchMatcher.group(1).split("\\.");
            this.originalComponents = new String[4];
            this.numericComponents = new long[4];
            for (int i = 0; i < 4; i++) {
                if (i < parts.length && !parts[i].equalsIgnoreCase("x")) {
                    this.originalComponents[i] = parts[i];
                    this.numericComponents[i] = Long.parseLong(parts[i]);
                } else {
                    this.originalComponents[i] = "9999999";
                    this.numericComponents[i] = 9999999;
                }
            }
            this.stability = Stability.DEV;
            this.stabilityNumbers = new int[0];
            this.stabilityNumDisplay = null;
            this.isDev = false;
            this.normalizedString = buildNormalizedString();
            return;
        }

        Matcher matcher = CLASSICAL_PATTERN.matcher(version);
        boolean isDate = false;
        if (!matcher.matches()) {
            matcher = DATE_PATTERN.matcher(version);
            if (!matcher.matches()) {
                throw new InvalidVersionException(versionStr, "Invalid Composer version format");
            }
            isDate = true;
        }

        String numericStr = matcher.group(1);
        if (isDate) {
            numericStr = numericStr.replaceAll("[:-]", ".");
        }
        final String[] rawParts = numericStr.split("\\.");

        if (!isDate) {
            this.originalComponents = new String[4];
            for (int i = 0; i < 4; i++) {
                this.originalComponents[i] = i < rawParts.length ? rawParts[i] : "0";
            }
        } else {
            this.originalComponents = rawParts;
        }

        this.numericComponents = new long[this.originalComponents.length];
        for (int i = 0; i < this.originalComponents.length; i++) {
            this.numericComponents[i] = Long.parseLong(this.originalComponents[i]);
        }

        final String stabilityStr = matcher.group(2);
        final String rawStabilityNum = matcher.group(3);
        final String devSuffix = matcher.group(4);

        if (stabilityStr != null) {
            final Stability parsedStability = Stability.of(stabilityStr);
            if (parsedStability == Stability.STABLE) {
                this.stability = Stability.STABLE;
                this.isDev = false;
                this.stabilityNumDisplay = null;
                this.stabilityNumbers = new int[0];
            } else {
                this.stability = parsedStability;
                this.isDev = devSuffix != null;
                if (rawStabilityNum != null && !rawStabilityNum.isEmpty()) {
                    this.stabilityNumDisplay = rawStabilityNum.replaceAll("^[.-]+", "");
                    final String[] numParts = this.stabilityNumDisplay.split("[.-]");
                    this.stabilityNumbers = new int[numParts.length];
                    for (int i = 0; i < numParts.length; i++) {
                        this.stabilityNumbers[i] = Integer.parseInt(numParts[i]);
                    }
                } else {
                    this.stabilityNumDisplay = null;
                    this.stabilityNumbers = new int[0];
                }
            }
        } else if (devSuffix != null) {
            this.stability = Stability.DEV;
            this.isDev = false;
            this.stabilityNumDisplay = null;
            this.stabilityNumbers = new int[0];
        } else {
            this.stability = Stability.STABLE;
            this.isDev = false;
            this.stabilityNumDisplay = null;
            this.stabilityNumbers = new int[0];
        }

        this.normalizedString = buildNormalizedString();
    }

    private String buildNormalizedString() {
        if (branchName != null) {
            return "dev-" + branchName;
        }

        final var sb = new StringBuilder();
        for (int i = 0; i < originalComponents.length; i++) {
            if (i > 0) sb.append('.');
            sb.append(originalComponents[i]);
        }

        if (stability != Stability.STABLE) {
            sb.append('-').append(stability.label);
            if (stabilityNumDisplay != null) {
                sb.append(stabilityNumDisplay);
            }
        }

        if (isDev) {
            sb.append("-dev");
        }

        return sb.toString();
    }

    @Override
    public boolean isStable() {
        return branchName == null
                && (stability == Stability.STABLE || stability == Stability.PATCH)
                && !isDev;
    }

    @Override
    public int compareTo(Version other) {
        if (other instanceof final ComposerVersion o) {
            if (this.branchName != null && o.branchName != null) {
                return this.branchName.compareToIgnoreCase(o.branchName);
            }
            if (this.branchName != null) {
                return -1;
            }
            if (o.branchName != null) {
                return 1;
            }

            final int maxLen = Math.max(this.numericComponents.length, o.numericComponents.length);
            for (int i = 0; i < maxLen; i++) {
                final long a = i < this.numericComponents.length ? this.numericComponents[i] : 0;
                final long b = i < o.numericComponents.length ? o.numericComponents[i] : 0;
                final int cmp = Long.compare(a, b);
                if (cmp != 0) {
                    return cmp;
                }
            }

            int cmp = this.stability.compareTo(o.stability);
            if (cmp != 0) {
                return cmp;
            }

            final int maxStabLen = Math.max(this.stabilityNumbers.length, o.stabilityNumbers.length);
            for (int i = 0; i < maxStabLen; i++) {
                final int a = i < this.stabilityNumbers.length ? this.stabilityNumbers[i] : 0;
                final int b = i < o.stabilityNumbers.length ? o.stabilityNumbers[i] : 0;
                cmp = Integer.compare(a, b);
                if (cmp != 0) {
                    return cmp;
                }
            }

            return Boolean.compare(o.isDev, this.isDev);
        }

        throw new IllegalArgumentException(
                "%s can only be compared with its own type, but got %s".formatted(
                        this.getClass().getName(), other.getClass().getName()));
    }

    @Override
    public String toString() {
        return normalizedString;
    }

}
