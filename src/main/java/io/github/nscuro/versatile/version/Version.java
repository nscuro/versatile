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

import java.util.Objects;

public abstract class Version implements Comparable<Version> {

    private final VersioningScheme scheme;
    final String versionStr;

    Version(final VersioningScheme scheme, final String versionStr) {
        this.scheme = scheme;
        this.versionStr = versionStr;
    }

    public static Version forScheme(final VersioningScheme scheme, final String versionStr) {
        // TODO: Would be nice to offer some sort of registry that library users can hook their
        //   own Version implementations into, and even override default implementations.
        return switch (scheme) {
            case DEB -> new DebianVersion(versionStr);
            case GOLANG -> new GoVersion(versionStr);
            case MAVEN -> new MavenVersion(versionStr);
            case NPM -> new NpmVersion(versionStr);
            default -> new GenericVersion(versionStr);
        };
    }

    /**
     * Determines whether the {@link Version} is considered stable.
     *
     * @return {@code true} when stable, otherwise {@code false}
     */
    public abstract boolean isStable();

    @Override
    public boolean equals(final Object obj) {
        return Objects.equals(obj, versionStr);
    }

    public VersioningScheme scheme() {
        return scheme;
    }

    @Override
    public String toString() {
        return versionStr;
    }

}
