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

import com.vdurmont.semver4j.Semver;

public class NpmVersion extends Version {

    private final Semver delegate;

    public NpmVersion(final String versionStr) {
        super(VersioningScheme.NPM, versionStr);
        this.delegate = new Semver(versionStr, Semver.SemverType.NPM);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStable() {
        return delegate.isStable();
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
                .formatted(GenericVersion.class.getSimpleName(), other.getClass().getSimpleName()));
    }

}
