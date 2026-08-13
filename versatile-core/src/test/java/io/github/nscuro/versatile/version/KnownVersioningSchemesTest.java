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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class KnownVersioningSchemesTest {

    @Nested
    class FromPurlTypeTest {

        @ParameterizedTest
        @CsvSource({
            "apk, apk",
            "cargo, cargo",
            "cpan, cpan",
            "maven, maven",
            "gradle, maven",
            "maven, maven",
            "deb, deb",
            "gem, gem",
            "generic, generic",
            "golang, golang",
            "npm, npm",
            "nuget, nuget",
            "pypi, pypi",
            "rpm, rpm"
        })
        void shouldReturnMatchingScheme(String purl, String expectedScheme) {
            assertThat(KnownVersioningSchemes.fromPurlType(purl)).contains(expectedScheme);
        }

        @Test
        void shouldReturnEmptyOptionalForUnknown() {
            assertThat(KnownVersioningSchemes.fromPurlType("pkg:foobar/baz@1.2.3"))
                    .isEmpty();
        }

        @Test
        void shouldReturnEmptyOptionalForNull() {
            assertThat(KnownVersioningSchemes.fromPurlType(null)).isEmpty();
        }
    }
}
