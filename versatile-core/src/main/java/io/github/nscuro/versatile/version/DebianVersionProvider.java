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

import java.util.Set;

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_DEBIAN;

/**
 * @since 0.8.0
 */
public class DebianVersionProvider extends AbstractBuiltinVersionProvider {

    public DebianVersionProvider() {
        super(Set.of(SCHEME_DEBIAN), (scheme, versionStr) -> new DebianVersion(versionStr));
    }

}
