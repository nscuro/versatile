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
package io.github.nscuro.versatile;

import io.github.nscuro.versatile.spi.Version;
import io.github.nscuro.versatile.spi.VersionProvider;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

/**
 * @since 0.8.0
 */
public class VersionFactory {

    private static final Map<String, VersionProvider> PROVIDER_BY_SCHEME = new HashMap<>();

    private VersionFactory() {
    }

    public static Version forScheme(final String scheme, final String versionStr) {
        VersionProvider provider = PROVIDER_BY_SCHEME.computeIfAbsent(scheme, VersionFactory::findProviderForScheme);
        if (provider != null) {
            return provider.getVersion(scheme, versionStr);
        }

        if (!"generic".equals(scheme)) {
            provider = PROVIDER_BY_SCHEME.computeIfAbsent("generic", VersionFactory::findProviderForScheme);
            if (provider != null) {
                return provider.getVersion(scheme, versionStr);
            }
        }

        throw new NoSuchElementException("No provider found for scheme: %s".formatted(scheme));
    }

    private static VersionProvider findProviderForScheme(final String scheme) {
        return ServiceLoader.load(VersionProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(provider -> provider.supportsScheme(scheme))
                .max(Comparator.comparingInt(VersionProvider::priority))
                .orElse(null);
    }

}
