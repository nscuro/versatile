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

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_NUGET;

/**
 * @see <a href="https://github.com/NuGet/NuGet.Client/blob/dev/src/NuGet.Core/NuGet.Versioning/VersionComparer.cs">NuGet VersionComparer</a>
 * @see <a href="https://learn.microsoft.com/en-us/nuget/concepts/package-versioning">NuGet Package Versioning</a>
 */
public class NugetVersion extends Version {

    public static class Provider extends AbstractBuiltinVersionProvider {

        public Provider() {
            super(Set.of(SCHEME_NUGET), (scheme, versionStr) -> new NugetVersion(versionStr));
        }

    }

    private final int major;
    private final int minor;
    private final int patch;
    private final int revision;
    private final String[] releaseLabels;
    private final String metadata;
    private final String normalizedString;

    NugetVersion(final String versionStr) {
        super(SCHEME_NUGET, versionStr);

        if (versionStr == null || versionStr.isEmpty()) {
            throw new InvalidVersionException(versionStr, "Version must not be empty");
        }

        String remaining = versionStr;

        if (remaining.charAt(0) == 'v' || remaining.charAt(0) == 'V') {
            remaining = remaining.substring(1);
            if (remaining.isEmpty()) {
                throw new InvalidVersionException(versionStr, "Version must not be empty");
            }
        }

        final int metaIndex = remaining.indexOf('+');
        if (metaIndex >= 0) {
            this.metadata = remaining.substring(metaIndex + 1);
            remaining = remaining.substring(0, metaIndex);
        } else {
            this.metadata = null;
        }

        final int preReleaseIndex = remaining.indexOf('-');
        String prerelease = null;
        if (preReleaseIndex >= 0) {
            prerelease = remaining.substring(preReleaseIndex + 1);
            if (prerelease.isEmpty()) {
                throw new InvalidVersionException(versionStr, "Pre-release label must not be empty");
            }
            remaining = remaining.substring(0, preReleaseIndex);
        }

        final String[] parts = remaining.split("\\.", -1);
        if (parts.length < 1 || parts.length > 4) {
            throw new InvalidVersionException(versionStr, "Version must have 1 to 4 numeric components");
        }

        this.major = parseComponent(versionStr, parts[0], "major");
        this.minor = parts.length > 1
                ? parseComponent(versionStr, parts[1], "minor")
                : 0;
        this.patch = parts.length > 2
                ? parseComponent(versionStr, parts[2], "patch")
                : 0;
        this.revision = parts.length > 3
                ? parseComponent(versionStr, parts[3], "revision")
                : 0;

        if (prerelease != null) {
            this.releaseLabels = prerelease.split("\\.", -1);
            for (final String label : this.releaseLabels) {
                if (label.isEmpty()) {
                    throw new InvalidVersionException(versionStr, "Pre-release label segment must not be empty");
                }
            }
        } else {
            this.releaseLabels = null;
        }

        this.normalizedString = normalize(prerelease);
    }

    private String normalize(String prerelease) {
        final var sb = new StringBuilder()
                .append(this.major)
                .append('.').append(this.minor)
                .append('.').append(this.patch);
        if (this.revision != 0) {
            sb.append('.').append(this.revision);
        }
        if (prerelease != null) {
            sb.append('-').append(prerelease.toLowerCase(Locale.ROOT));
        }
        if (this.metadata != null) {
            sb.append('+').append(this.metadata);
        }
        return sb.toString();
    }

    private static int parseComponent(String versionStr, String part, String name) {
        if (part.isEmpty()) {
            throw new InvalidVersionException(versionStr, "Empty " + name + " version component");
        }

        try {
            return Integer.parseInt(part);
        } catch (final NumberFormatException e) {
            throw new InvalidVersionException(versionStr, "Invalid " + name + " version component: " + part);
        }
    }

    @Override
    public boolean isStable() {
        return releaseLabels == null;
    }

    @Override
    public int compareTo(Version other) {
        if (other instanceof final NugetVersion o) {
            int cmp = Integer.compare(this.major, o.major);
            if (cmp != 0) {
                return cmp;
            }

            cmp = Integer.compare(this.minor, o.minor);
            if (cmp != 0) {
                return cmp;
            }

            cmp = Integer.compare(this.patch, o.patch);
            if (cmp != 0) {
                return cmp;
            }

            cmp = Integer.compare(this.revision, o.revision);
            if (cmp != 0) {
                return cmp;
            }

            if (this.releaseLabels != null && o.releaseLabels == null) {
                return -1;
            }
            if (this.releaseLabels == null && o.releaseLabels != null) {
                return 1;
            }
            if (this.releaseLabels == null) {
                return 0;
            }

            return compareReleaseLabels(this.releaseLabels, o.releaseLabels);
        }

        throw new IllegalArgumentException(
                "%s can only be compared with its own type, but got %s".formatted(
                        this.getClass().getName(), other.getClass().getName()));
    }

    private static int compareReleaseLabels(final String[] a, final String[] b) {
        final int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            if (i >= a.length) {
                return -1;
            }
            if (i >= b.length) {
                return 1;
            }

            final int cmp = compareRelease(a[i], b[i]);
            if (cmp != 0) {
                return cmp;
            }
        }

        return 0;
    }

    private static int compareRelease(String a, String b) {
        final boolean aIsNum = isNumeric(a);
        final boolean bIsNum = isNumeric(b);

        if (aIsNum && bIsNum) {
            return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
        }

        if (aIsNum) {
            return -1;
        }
        if (bIsNum) {
            return 1;
        }

        return a.compareToIgnoreCase(b);
    }

    private static boolean isNumeric(String s) {
        if (s.isEmpty()) {
            return false;
        }

        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return normalizedString;
    }

}
