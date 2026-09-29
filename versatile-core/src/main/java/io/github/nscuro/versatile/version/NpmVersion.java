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

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_NPM;

import io.github.nscuro.versatile.spi.Version;
import java.util.Set;

public class NpmVersion extends Version {

    /**
     * @since 0.8.0
     */
    public static class Provider extends AbstractBuiltinVersionProvider {

        public Provider() {
            super(Set.of(SCHEME_NPM), (scheme, versionStr) -> new NpmVersion(versionStr));
        }
    }

    private final SemVer delegate;

    NpmVersion(String versionStr) {
        super(SCHEME_NPM, versionStr);
        final String stripped = versionStr.strip();
        this.delegate =
                new SemVer((stripped.startsWith("v") || stripped.startsWith("V")) ? stripped.substring(1) : stripped);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStable() {
        return delegate.major() > 0 && !delegate.isPrerelease();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Version other) {
        if (other instanceof final NpmVersion otherVersion) {
            return this.delegate.compareTo(otherVersion.delegate);
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(this.getClass().getName(), other.getClass().getName()));
    }
}
