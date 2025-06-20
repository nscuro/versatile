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
import io.github.nscuro.versatile.version.GenericVersion;
import io.github.nscuro.versatile.version.GoVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionFactoryTest {

    @Test
    void shouldReturnMatchingVersion() {
        final Version version = VersionFactory.forScheme("golang", "v1.2.4");
        assertThat(version).isInstanceOf(GoVersion.class);
        assertThat(version.scheme()).isEqualTo("golang");
    }

    @Test
    void shouldFallbackToGenericVersionAndRetainOriginalScheme() {
        final Version version = VersionFactory.forScheme("foobar", "v1.2.4");
        assertThat(version).isInstanceOf(GenericVersion.class);
        assertThat(version.scheme()).isEqualTo("foobar");
    }

}