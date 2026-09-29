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

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_CARGO;

import io.github.nscuro.versatile.spi.Version;
import java.util.Set;

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

    private final SemVer delegate;

    CargoVersion(String versionStr) {
        super(SCHEME_CARGO, versionStr);
        this.delegate = new SemVer(versionStr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStable() {
        return !delegate.isPrerelease();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Version other) {
        if (other instanceof final CargoVersion otherVersion) {
            return this.delegate.compareTo(otherVersion.delegate);
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(this.getClass().getName(), other.getClass().getName()));
    }
}
