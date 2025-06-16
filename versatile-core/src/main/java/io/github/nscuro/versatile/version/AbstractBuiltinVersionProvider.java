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
import io.github.nscuro.versatile.spi.VersionProvider;

import java.util.Set;
import java.util.function.BiFunction;

/**
 * @since 0.8.0
 */
abstract class AbstractBuiltinVersionProvider implements VersionProvider {

    private final Set<String> supportedSchemes;
    private final BiFunction<String, String, Version> constructor;

    AbstractBuiltinVersionProvider(final Set<String> supportedSchemes, final BiFunction<String, String, Version> constructor) {
        this.supportedSchemes = supportedSchemes;
        this.constructor = constructor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int priority() {
        return PRIORITY_BUILTIN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsScheme(final String scheme) {
        return supportedSchemes.contains(scheme);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Version getVersion(final String scheme, final String versionStr) {
        return constructor.apply(scheme, versionStr);
    }

}
