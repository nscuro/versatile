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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DebianVersionTest extends AbstractVersionTest {

    @ParameterizedTest
    @CsvSource(value = {
            "0, IS_LOWER_THAN, 1",
            "114.0.5735.106-1~deb11u1, IS_LOWER_THAN, 114.0.5735.133-1~deb12u1",
            "114.0.5735.133-1, IS_HIGHER_THAN, 114.0.5735.133-1~deb12u1"
    })
    void testCompareTo(final String versionA, final ComparisonExpectation expectation, final String versionB) {
        expectation.evaluate(new DebianVersion(versionA), new DebianVersion(versionB));
    }

}