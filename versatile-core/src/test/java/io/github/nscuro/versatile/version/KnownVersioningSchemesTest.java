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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class KnownVersioningSchemesTest {

    @Nested
    class FromPurlTest {

        @ParameterizedTest
        @CsvSource({
                "pkg:apk/alpine/curl@7.83.0-r0, apk",
                "pkg:cpan/DROLSKY/DateTime@1.55, cpan",
                "pkg:maven/org.apache.xmlgraphics/batik-anim@1.9.1, maven",
                "pkg:gradle/org.apache.xmlgraphics/batik-anim@1.9.1, maven",
                "pkg:maven/org.apache.xmlgraphics/batik-anim@1.9.1, maven",
                "pkg:deb/debian/curl@7.50.3-1, deb",
                "pkg:gem/ruby-advisory-db-check@0.12.4, gem",
                "pkg:generic/openssl@1.1.10g, generic",
                "pkg:golang/github.com/gorilla/context@234fd47e07d1004f0aed9c, golang",
                "pkg:npm/%40angular/animation@12.3.1, npm",
                "pkg:nuget/EnterpriseLibrary.Common@6.0.1304, nuget",
                "pkg:pypi/django-allauth@12.23, pypi",
                "pkg:rpm/fedora/curl@7.50.3-1.fc25, rpm"
        })
        void shouldReturnMatchingScheme(final String purl, final String expectedScheme) {
            assertThat(KnownVersioningSchemes.fromPurl(purl)).contains(expectedScheme);
        }

        @Test
        void shouldReturnEmptyOptionalForUnknown() {
            assertThat(KnownVersioningSchemes.fromPurl("pkg:foobar/baz@1.2.3")).isEmpty();
        }

    }

}