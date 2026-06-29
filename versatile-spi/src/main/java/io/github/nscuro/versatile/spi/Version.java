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
package io.github.nscuro.versatile.spi;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public abstract class Version implements Comparable<Version> {

    protected final String scheme;
    protected final String versionStr;

    protected Version(String scheme, String versionStr) {
        this.scheme = requireNonNull(scheme, "scheme must not be null");
        this.versionStr = requireNonNull(versionStr, "versionStr must not be null");
    }

    public abstract boolean isStable();

    public String scheme() {
        return scheme;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, versionStr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final Version version)) {
            return false;
        }

        return Objects.equals(scheme, version.scheme) && this.compareTo(version) == 0;
    }

    @Override
    public String toString() {
        return versionStr;
    }
}
