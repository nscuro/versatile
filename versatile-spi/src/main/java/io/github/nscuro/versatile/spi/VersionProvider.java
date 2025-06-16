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

/**
 * @since 0.8.0
 */
public interface VersionProvider {

    int PRIORITY_LOWEST = 0;
    int PRIORITY_BUILTIN = 50;
    int PRIORITY_HIGHEST = Integer.MAX_VALUE;

    /**
     * @return The priority of the provider.
     */
    int priority();

    /**
     * @param scheme The versioning scheme to check
     * @return {@code true} when {@code scheme} is supported, otherwise {@code false}
     */
    boolean supportsScheme(final String scheme);

    /**
     * @param scheme     The versioning scheme to create a {@link Version} for
     * @param versionStr The version string to create {@link Version} from
     * @return A {@link Version} instance
     * @throws InvalidVersionException When {@code versionStr} could not be used to construct a {@link Version}
     */
    Version getVersion(final String scheme, final String versionStr);

}
