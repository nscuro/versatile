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

import com.github.packageurl.PackageURL;

/**
 * @see <a href="https://github.com/package-url/purl-spec/blob/version-range-spec/VERSION-RANGE-SPEC.rst#some-of-the-known-versioning-schemes">Known versioning schemes</a>
 */
public enum VersioningScheme {

    ALPINE,

    CPAN,

    DEB,

    GEM,

    GENERIC,

    GENTOO,

    GOLANG,

    MAVEN,

    NPM,

    NUGET,

    PYPI,

    RPM;

    public static VersioningScheme fromPurlType(final String purlType) {
        return switch (purlType) {
            case PackageURL.StandardTypes.DEBIAN -> DEB;
            case PackageURL.StandardTypes.GEM -> GEM;
            case PackageURL.StandardTypes.GOLANG -> GOLANG;
            case PackageURL.StandardTypes.MAVEN -> MAVEN;
            case PackageURL.StandardTypes.NPM -> NPM;
            case PackageURL.StandardTypes.NUGET -> NUGET;
            case PackageURL.StandardTypes.PYPI -> PYPI;
            case PackageURL.StandardTypes.RPM -> RPM;
            default -> GENERIC;
        };
    }

}
