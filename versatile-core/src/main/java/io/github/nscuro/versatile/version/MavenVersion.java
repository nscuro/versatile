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

import io.github.nscuro.versatile.spi.Version;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.Set;
import java.util.regex.Pattern;

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_MAVEN;

public class MavenVersion extends Version {

    /**
     * @since 0.8.0
     */
    public static class Provider extends AbstractBuiltinVersionProvider {

        public Provider() {
            super(Set.of(SCHEME_MAVEN), (scheme, versionStr) -> new MavenVersion(versionStr));
        }

    }

    private static final Pattern UNSTABLE_QUALIFIER_PATTERN = Pattern.compile("""
            ^(snapshot|rc\\d*|alpha\\.?\\d*|beta\\.?\\d*|m\\.?\\d*|milestone\\.\\d*)$
            """, Pattern.CASE_INSENSITIVE);

    private final ArtifactVersion delegate;

    MavenVersion(final String versionStr) {
        super(SCHEME_MAVEN, versionStr);
        this.delegate = new DefaultArtifactVersion(versionStr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStable() {
        if (delegate.getQualifier() == null || delegate.getQualifier().equals(this.versionStr)) {
            return true;
        }

        return UNSTABLE_QUALIFIER_PATTERN.matcher(delegate.getQualifier()).matches();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Version other) {
        if (other instanceof final MavenVersion otherVersion) {
            return this.delegate.compareTo(otherVersion.delegate);
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(this.getClass().getName(), other.getClass().getName()));
    }

}
