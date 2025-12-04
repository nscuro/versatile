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

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @see <a href="https://github.com/package-url/purl-spec/blob/version-range-spec/VERSION-RANGE-SPEC.rst#some-of-the-known-versioning-schemes">List of known schemes</a>
 * @since 0.8.0
 */
public final class KnownVersioningSchemes {

    public static final String SCHEME_APK = "apk";
    public static final String SCHEME_CPAN = "cpan";
    public static final String SCHEME_DEBIAN = "deb";
    public static final String SCHEME_GEM = "gem";
    public static final String SCHEME_GENERIC = "generic";
    public static final String SCHEME_GENTOO = "gentoo";
    public static final String SCHEME_GOLANG = "golang";
    public static final String SCHEME_MAVEN = "maven";
    public static final String SCHEME_NPM = "npm";
    public static final String SCHEME_NUGET = "nuget";
    public static final String SCHEME_PYPI = "pypi";
    public static final String SCHEME_RPM = "rpm";

    private KnownVersioningSchemes() {
    }

    /**
     * Attempt to match a given Package URL with any of the versioning schemes known to versatile.
     *
     * @param purl The Package URL to match
     * @return An {@link Optional} containing the matched scheme, otherwise an empty {@link Optional}
     * @throws IllegalArgumentException When the provided {@code purl} is invalid
     * @see #fromPurl(PackageURL)
     * @since 0.9.0
     */
    public static Optional<String> fromPurl(final String purl) {
        try {
            return fromPurl(new PackageURL(purl));
        } catch (MalformedPackageURLException e) {
            throw new IllegalArgumentException("The provided purl is invalid: " + purl, e);
        }
    }

    /**
     * Attempt to match a given {@link PackageURL} with any of the versioning schemes known to versatile.
     *
     * @param purl The {@link PackageURL} to match
     * @return An {@link Optional} containing the matched scheme, otherwise an empty {@link Optional}
     * @see <a href="https://github.com/package-url/purl-spec/blob/master/PURL-TYPES.rst">PURL Types</a>
     * @since 0.9.0
     */
    public static Optional<String> fromPurl(final PackageURL purl) {
        requireNonNull(purl, "purl must not be null");

        // NB: It may be necessary to inspect more than just the type to
        // determine the versioning scheme.

        return switch (purl.getType()) {
            case "apk" -> Optional.of(SCHEME_APK);
            case "cpan" -> Optional.of(SCHEME_CPAN);
            case "clojars", "gradle", "maven" -> Optional.of(SCHEME_MAVEN);
            case "deb" -> Optional.of(SCHEME_DEBIAN);
            case "gem" -> Optional.of(SCHEME_GEM);
            case "generic" -> Optional.of(SCHEME_GENERIC);
            case "golang" -> Optional.of(SCHEME_GOLANG);
            case "npm" -> Optional.of(SCHEME_NPM);
            case "nuget" -> Optional.of(SCHEME_NUGET);
            case "pypi" -> Optional.of(SCHEME_PYPI);
            case "rpm" -> Optional.of(SCHEME_RPM);
            default -> Optional.empty();
        };
    }

}
