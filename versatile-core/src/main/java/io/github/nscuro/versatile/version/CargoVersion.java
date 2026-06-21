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
import java.util.Set;

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_CARGO;
import static io.github.nscuro.versatile.version.VersionUtils.isAsciiAlphaNumeric;
import static io.github.nscuro.versatile.version.VersionUtils.isAsciiDigit;
import static io.github.nscuro.versatile.version.VersionUtils.isAsciiNumeric;

/**
 * @see <a href="https://github.com/dtolnay/semver/blob/master/src/impls.rs">semver crate ordering implementation</a>
 * @see <a href="https://github.com/dtolnay/semver/blob/master/tests/test_version.rs">semver crate test suite</a>
 * @see <a href="https://semver.org/#spec-item-11">Semantic Versioning 2.0.0, &sect;11 (precedence)</a>
 * @see <a href="https://doc.rust-lang.org/cargo/reference/semver.html">Cargo SemVer compatibility reference</a>
 * @since 0.19.0
 */
public class CargoVersion extends Version {

    public static class Provider extends AbstractBuiltinVersionProvider {

        public Provider() {
            super(Set.of(SCHEME_CARGO), (scheme, versionStr) -> new CargoVersion(versionStr));
        }

    }

    private static final String[] NO_PRERELEASE = new String[0];

    private final long major;
    private final long minor;
    private final long patch;
    private final String[] prerelease;

    CargoVersion(String versionStr) {
        super(SCHEME_CARGO, versionStr);

        final int[] cursor = {0};
        this.major = parseNumericField(versionStr, cursor);
        expect(versionStr, cursor, '.');
        this.minor = parseNumericField(versionStr, cursor);
        expect(versionStr, cursor, '.');
        this.patch = parseNumericField(versionStr, cursor);

        String[] pre = NO_PRERELEASE;
        if (peek(versionStr, cursor) == '-') {
            cursor[0]++;
            pre = parseIdentifiers(versionStr, cursor, true);
        }
        if (peek(versionStr, cursor) == '+') {
            cursor[0]++;
            parseIdentifiers(versionStr, cursor, false);
        }
        if (cursor[0] != versionStr.length()) {
            throw new InvalidVersionException(
                    versionStr,
                    "Unexpected character at position %d: %s".formatted(cursor[0], versionStr));
        }

        this.prerelease = pre;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStable() {
        return prerelease.length == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Version other) {
        if (other instanceof final CargoVersion otherVersion) {
            int result = Long.compareUnsigned(this.major, otherVersion.major);
            if (result != 0) {
                return result;
            }
            result = Long.compareUnsigned(this.minor, otherVersion.minor);
            if (result != 0) {
                return result;
            }
            result = Long.compareUnsigned(this.patch, otherVersion.patch);
            if (result != 0) {
                return result;
            }

            return comparePrerelease(this.prerelease, otherVersion.prerelease);
        }

        throw new IllegalArgumentException(
                "%s can only be compared with its own type, but got %s".formatted(
                        this.getClass().getName(),
                        other.getClass().getName()));
    }

    private static long parseNumericField(String versionStr, int[] cursor) {
        final int start = cursor[0];
        int i = start;
        while (i < versionStr.length() && isAsciiDigit(versionStr.charAt(i))) {
            i++;
        }
        if (i == start) {
            throw new InvalidVersionException(
                    versionStr,
                    "Expected a number at position " + start);
        }

        final String number = versionStr.substring(start, i);
        if (number.length() > 1 && number.charAt(0) == '0') {
            throw new InvalidVersionException(
                    versionStr,
                    "Leading zero in numeric component: " + number);
        }

        cursor[0] = i;
        try {
            return Long.parseUnsignedLong(number);
        } catch (NumberFormatException e) {
            throw new InvalidVersionException(
                    versionStr,
                    "Numeric component exceeds 64-bit range: " + number,
                    e);
        }
    }

    private static String[] parseIdentifiers(String versionStr, int[] cursor, boolean prerelease) {
        final List<String> identifiers = new ArrayList<>();

        while (true) {
            final int start = cursor[0];
            int i = start;
            while (i < versionStr.length() && isAsciiAlphaNumeric(versionStr.charAt(i))) {
                i++;
            }
            if (i == start) {
                throw new InvalidVersionException(
                        versionStr,
                        "Empty identifier at position " + start);
            }

            final String identifier = versionStr.substring(start, i);
            cursor[0] = i;

            if (prerelease && isAsciiNumeric(identifier)
                    && identifier.length() > 1
                    && identifier.charAt(0) == '0') {
                throw new InvalidVersionException(
                        versionStr,
                        "Leading zero in numeric pre-release identifier: " + identifier);
            }

            identifiers.add(identifier);

            if (peek(versionStr, cursor) == '.') {
                cursor[0]++;
                continue;
            }

            return identifiers.toArray(new String[0]);
        }
    }

    private static int comparePrerelease(String[] lhs, String[] rhs) {
        if (lhs.length == 0 && rhs.length == 0) {
            return 0;
        }
        if (lhs.length == 0) {
            return 1;
        }
        if (rhs.length == 0) {
            return -1;
        }

        final int limit = Math.min(lhs.length, rhs.length);
        for (int i = 0; i < limit; i++) {
            final int result = compareIdentifier(lhs[i], rhs[i]);
            if (result != 0) {
                return result;
            }
        }

        return Integer.compare(lhs.length, rhs.length);
    }

    private static int compareIdentifier(String lhs, String rhs) {
        final boolean lhsNumeric = isAsciiNumeric(lhs);
        final boolean rhsNumeric = isAsciiNumeric(rhs);

        if (lhsNumeric && rhsNumeric) {
            if (lhs.length() != rhs.length()) {
                return Integer.compare(lhs.length(), rhs.length());
            }
            return lhs.compareTo(rhs);
        }

        if (lhsNumeric != rhsNumeric) {
            return lhsNumeric ? -1 : 1;
        }

        return lhs.compareTo(rhs);
    }

    private static char peek(String versionStr, int[] cursor) {
        return cursor[0] < versionStr.length()
                ? versionStr.charAt(cursor[0])
                : '\0';
    }

    private static void expect(String versionStr, int[] cursor, char expected) {
        if (peek(versionStr, cursor) != expected) {
            throw new InvalidVersionException(
                    versionStr,
                    "Expected '%c' at position %d".formatted(expected, cursor[0]));
        }

        cursor[0]++;
    }

}
