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

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_GOLANG;
import static io.github.nscuro.versatile.version.VersionUtils.isAlphaNumeric;
import static io.github.nscuro.versatile.version.VersionUtils.isNumeric;

/**
 * @see <a href="https://github.com/golang/mod/blob/v0.12.0/semver/semver.go">Go Modules semantic version implementation</a>
 * @see <a href="https://github.com/golang/mod/blob/v0.12.0/module/pseudo.go">Go Modules pseudo version implementation</a>
 */
public class GoVersion extends Version {

    /**
     * @since 0.8.0
     */
    public static class Provider extends AbstractBuiltinVersionProvider {

        public Provider() {
            super(Set.of(SCHEME_GOLANG), (scheme, versionStr) -> new GoVersion(versionStr));
        }

    }

    private final String major;
    private final String minor;
    private final String patch;
    private final String shortV;
    private final String prerelease;
    private final String build;

    // https://github.com/golang/mod/blob/baa5c2d058db25484c20d76985ba394e73176132/semver/semver.go#L172-L225
    GoVersion(final String versionStr) {
        super(SCHEME_GOLANG, versionStr);

        // Note: Go's implementation requires the `v` prefix to be present.
        // Version data in databases external to the Go ecosystem, like OSV,
        // do however not always include this prefix. To handle such cases
        // more gracefully, we simply ignore it if it's there.
        int versionStart = 0;
        if (versionStr.startsWith("v")) {
            versionStart = 1;
        }

        Map.Entry<String, String> partAndRest = parseInt(versionStr.substring(versionStart));
        if (partAndRest == null) {
            throw new InvalidVersionException(versionStr, "Invalid major version");
        }

        this.major = partAndRest.getKey();
        if (partAndRest.getValue().isEmpty()) {
            this.minor = "0";
            this.patch = "0";
            this.shortV = ".0.0";
            this.prerelease = null;
            this.build = null;
            return;
        }

        if (partAndRest.getValue().charAt(0) != '.') {
            throw new InvalidVersionException(versionStr, "Major version must be followed by \".\"");
        }

        partAndRest = parseInt(partAndRest.getValue().substring(1));
        if (partAndRest == null) {
            throw new InvalidVersionException(versionStr, "Invalid minor version");
        }

        this.minor = partAndRest.getKey();
        if (partAndRest.getValue().isEmpty()) {
            this.patch = "0";
            this.shortV = ".0";
            this.prerelease = null;
            this.build = null;
            return;
        }

        partAndRest = parseInt(partAndRest.getValue().substring(1));
        if (partAndRest == null) {
            throw new InvalidVersionException(versionStr, "Invalid patch version");
        }

        this.patch = partAndRest.getKey();
        if (!partAndRest.getValue().isEmpty() && partAndRest.getValue().charAt(0) == '-') {
            partAndRest = parsePrerelease(partAndRest.getValue());
            if (partAndRest == null) {
                throw new InvalidVersionException(versionStr, "Invalid pre-release version");
            }

            this.prerelease = partAndRest.getKey();
        } else {
            this.prerelease = null;
        }

        if (!partAndRest.getValue().isEmpty() && partAndRest.getValue().charAt(0) == '+') {
            partAndRest = parseBuild(partAndRest.getValue());
            if (partAndRest == null) {
                throw new InvalidVersionException(versionStr, "Invalid build version");
            }

            this.build = partAndRest.getKey();
        } else {
            this.build = null;
        }

        if (!partAndRest.getValue().isEmpty()) {
            throw new InvalidVersionException(versionStr, "Unexpected remainder after parsing: \"%s\"".formatted(partAndRest.getValue()));
        }

        this.shortV = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStable() {
        return prerelease == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // https://github.com/golang/mod/blob/baa5c2d058db25484c20d76985ba394e73176132/semver/semver.go#L116-L138
    public int compareTo(final Version other) {
        if (other instanceof final GoVersion otherVersion) {
            int comparisonResult = Integer.compare(Integer.parseInt(this.major), Integer.parseInt(otherVersion.major));
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            comparisonResult = Integer.compare(Integer.parseInt(this.minor), Integer.parseInt(otherVersion.minor));
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            comparisonResult = Integer.compare(Integer.parseInt(this.patch), Integer.parseInt(otherVersion.patch));
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            return comparePrerelease(this.prerelease, otherVersion.prerelease);
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(this.getClass().getName(), other.getClass().getName()));
    }

    public String major() {
        return major;
    }

    public String minor() {
        return minor;
    }

    public String patch() {
        return patch;
    }

    public String prerelease() {
        return prerelease;
    }

    public String build() {
        return build;
    }

    // https://github.com/golang/mod/blob/baa5c2d058db25484c20d76985ba394e73176132/semver/semver.go#L227-L242
    private static Map.Entry<String, String> parseInt(final String version) {
        if (!Character.isDigit(version.charAt(0))) {
            return null;
        }

        int i = 1;
        while (i < version.length() && Character.isDigit(version.charAt(i))) {
            i++;
        }

        if (version.charAt(0) == '0' && i != 1) {
            return null;
        }

        return Map.entry(version.substring(0, i), version.substring(i));
    }

    // https://github.com/golang/mod/blob/baa5c2d058db25484c20d76985ba394e73176132/semver/semver.go#L244-L270
    private static Map.Entry<String, String> parsePrerelease(final String version) {
        if (!version.startsWith("-")) {
            return null;
        }

        int i = 1, start = 1;
        for (/* i */; i < version.length() && version.charAt(i) != '+'; i++) {
            final char currChar = version.charAt(i);
            if (!isIdentChar(currChar) && currChar != '.') {
                return null;
            }

            if (currChar == '.') {
                if (start == i || isBadNum(version.substring(start, i))) {
                    return null;
                }

                start = i + 1;
            }
        }

        if (start == i || isBadNum(version.substring(start, i))) {
            return null;
        }

        return Map.entry(version.substring(0, i), version.substring(i));
    }

    // https://github.com/golang/mod/blob/baa5c2d058db25484c20d76985ba394e73176132/semver/semver.go#L272-L294
    private static Map.Entry<String, String> parseBuild(final String version) {
        if (!version.startsWith("+")) {
            return null;
        }

        int i = 1, start = 1;
        for (/* i */; i < version.length() && version.charAt(i) != '+'; i++) {
            final char currChar = version.charAt(i);
            if (!isIdentChar(currChar) && currChar != '.') {
                return null;
            }

            if (currChar == '.') {
                if (start == i) {
                    return null;
                }

                start = i + 1;
            }
        }

        if (start == i) {
            return null;
        }

        return Map.entry(version.substring(0, i), version.substring(i));
    }

    // https://github.com/golang/mod/blob/baa5c2d058db25484c20d76985ba394e73176132/semver/semver.go#L333-L393
    private int comparePrerelease(String x, String y) {
        if (Objects.equals(x, y)) {
            return 0;
        }
        if (x == null || x.isEmpty()) {
            return 1;
        }
        if (y == null || y.isEmpty()) {
            return -1;
        }

        while (!x.isEmpty() && !y.isEmpty()) {
            x = x.substring(1);
            y = y.substring(1);

            final Map.Entry<String, String> dxAndRest = nextIdent(x);
            final Map.Entry<String, String> yxIdentAndRest = nextIdent(y);
            x = dxAndRest.getValue();
            y = yxIdentAndRest.getValue();
            String dx = dxAndRest.getKey();
            String dy = yxIdentAndRest.getKey();

            if (!Objects.equals(dx, dy)) {
                boolean ix = isNum(dx);
                boolean iy = isNum(dy);
                if (ix != iy) {
                    if (ix) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
                if (ix) {
                    if (dx.length() < dy.length()) {
                        return -1;
                    }
                    if (dx.length() > dy.length()) {
                        return 1;
                    }
                }
                if (dx.compareTo(dy) < 0) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }

        if (x.isEmpty()) {
            return -1;
        }

        return 1;
    }

    // https://github.com/golang/mod/blob/baa5c2d058db25484c20d76985ba394e73176132/semver/semver.go#L296-L298
    private static boolean isIdentChar(final char versionChar) {
        return isAlphaNumeric(String.valueOf(versionChar));
    }

    // https://github.com/golang/mod/blob/baa5c2d058db25484c20d76985ba394e73176132/semver/semver.go#L308-L314
    private static boolean isNum(final String v) {
        return isNumeric(v);
    }

    // https://github.com/golang/mod/blob/baa5c2d058db25484c20d76985ba394e73176132/semver/semver.go#L300-L306
    private static boolean isBadNum(final String version) {
        int i = 0;
        for (/* i */; i < version.length() && '0' <= version.charAt(i) && version.charAt(i) <= '9'; i++) ;
        return i == version.length() && i > 1 && version.startsWith("0");
    }

    // https://github.com/golang/mod/blob/baa5c2d058db25484c20d76985ba394e73176132/semver/semver.go#L395-L401
    private static Map.Entry<String, String> nextIdent(final String x) {
        int i = 0;
        for (/* i */; i < x.length() && x.charAt(i) != '.'; i++) ;
        return Map.entry(x.substring(0, i), x.substring(i));
    }

}
