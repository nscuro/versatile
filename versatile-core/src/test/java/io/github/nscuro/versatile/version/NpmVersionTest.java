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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.nscuro.versatile.spi.InvalidVersionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/*
 * Rows marked "node-semver" are the strict (non-loose) cases from
 * https://github.com/npm/node-semver/tree/9c8692ae05416e9dbe88d95ffe2b80e6964550fe/test/fixtures
 * (comparisons.js, equality.js, valid-versions.js, invalid-versions.js), used under the ISC License:
 *
 * Copyright (c) Isaac Z. Schlueter and Contributors
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
class NpmVersionTest extends AbstractVersionTest {

    @ParameterizedTest
    @CsvSource(
            value = {
                // node-semver comparisons.js
                "0.0.0, IS_HIGHER_THAN, 0.0.0-foo",
                "0.0.1, IS_HIGHER_THAN, 0.0.0",
                "1.0.0, IS_HIGHER_THAN, 0.9.9",
                "0.10.0, IS_HIGHER_THAN, 0.9.0",
                "0.99.0, IS_HIGHER_THAN, 0.10.0",
                "2.0.0, IS_HIGHER_THAN, 1.2.3",
                "1.2.3, IS_HIGHER_THAN, 1.2.3-asdf",
                "1.2.3, IS_HIGHER_THAN, 1.2.3-4",
                "1.2.3, IS_HIGHER_THAN, 1.2.3-4-foo",
                "1.2.3-5-foo, IS_HIGHER_THAN, 1.2.3-5",
                "1.2.3-5, IS_HIGHER_THAN, 1.2.3-4",
                "1.2.3-5-foo, IS_HIGHER_THAN, 1.2.3-5-Foo",
                "3.0.0, IS_HIGHER_THAN, 2.7.2+asdf",
                "1.2.3-a.10, IS_HIGHER_THAN, 1.2.3-a.5",
                "1.2.3-a.b, IS_HIGHER_THAN, 1.2.3-a.5",
                "1.2.3-a.b, IS_HIGHER_THAN, 1.2.3-a",
                "1.2.3-a.b.c.10.d.5, IS_HIGHER_THAN, 1.2.3-a.b.c.5.d.100",
                "1.2.3-r2, IS_HIGHER_THAN, 1.2.3-r100",
                "1.2.3-r100, IS_HIGHER_THAN, 1.2.3-R2",
                // node-semver equality.js
                "1.2.3-beta+build, IS_EQUAL_TO, 1.2.3-beta+otherbuild",
                "1.2.3+build, IS_EQUAL_TO, 1.2.3+otherbuild",
                "'  v1.2.3+build', IS_EQUAL_TO, 1.2.3+otherbuild",
                // Partial versions are padded with zeros, not treated as X-ranges.
                "0, IS_LOWER_THAN, 0.0.1-security",
                "0.0.1-security, IS_HIGHER_THAN, 0",
                "0, IS_HIGHER_THAN, 0.0.0-security",
                "0.0.0-security, IS_LOWER_THAN, 0",
                "1, IS_HIGHER_THAN, 1.0.0-beta",
                "1.0.0-beta, IS_LOWER_THAN, 1",
                "1+build1, IS_EQUAL_TO, 1.0.0",
                "128.0.0, IS_EQUAL_TO, 128.0.0",
                "v1.2.3, IS_EQUAL_TO, 1.2.3",
                "V1.2.3, IS_EQUAL_TO, 1.2.3",
                "' v1.2.3 ', IS_EQUAL_TO, 1.2.3",
            })
    void testCompareTo(String versionA, ComparisonExpectation expectation, String versionB) {
        expectation.evaluate(new NpmVersion(versionA), new NpmVersion(versionB));
    }

    @ParameterizedTest
    @CsvSource(value = {"1.2.3, true", "1.2.3+build, true", "1.2.3-alpha, false", "0.1.0, false"})
    void testIsStable(String version, boolean stable) {
        assertThat(new NpmVersion(version).isStable()).isEqualTo(stable);
    }

    // node-semver valid-versions.js
    @ParameterizedTest
    @ValueSource(
            strings = {
                "1.0.0",
                "2.1.0",
                "3.2.1",
                "v1.2.3",
                "1.2.3-0",
                "1.2.3-123",
                "1.2.3-1.2.3",
                "1.2.3-1a",
                "1.2.3-a1",
                "1.2.3-alpha",
                "1.2.3-alpha.1",
                "1.2.3-alpha-1",
                "1.2.3-alpha-.-beta",
                "1.2.3+456",
                "1.2.3+build",
                "1.2.3+new-build",
                "1.2.3+build.1",
                "1.2.3+build.1a",
                "1.2.3+build.a1",
                "1.2.3+build.alpha",
                "1.2.3+build.alpha.beta",
                "1.2.3-alpha+build"
            })
    void testValidVersions(String version) {
        assertThatNoException().isThrownBy(() -> new NpmVersion(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello, world", "1.x", "131.0.6778.85", "01.2.3", "1.2.3-01"})
    void testFailingVersions(String version) {
        assertThatThrownBy(() -> new NpmVersion(version)).isInstanceOf(InvalidVersionException.class);
    }

    // node-semver invalid-versions.js "too long"
    @Test
    void testFailingTooLongVersion() {
        assertThatThrownBy(() -> new NpmVersion("1".repeat(255) + ".0.0")).isInstanceOf(InvalidVersionException.class);
    }
}
