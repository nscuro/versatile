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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MavenVersionTest {

    @ParameterizedTest
    @CsvSource(
            value = {
                "1, true",
                "1.0, true",
                "1.0.0, true",
                "1.0.0.0, true",
                "1.2.3-4, true",
                "1.0.0-Final, true",
                "1.0.0.Final, true",
                "1.0-RELEASE, true",
                "1.0-ga, true",
                "1.0-sp1, true",
                "foobar, true",
                "1.0-SNAPSHOT, false",
                "1.0.0-SNAPSHOT, false",
                "1.0.0-snapshot, false",
                "1.0-rc, false",
                "1.0-rc1, false",
                "1.0-rc.1, false",
                "1.0-rc-1, false",
                "1.0-RC1, false",
                "1.0-cr, false",
                "1.0-cr1, false",
                "1.0-cr.1, false",
                "1.0-cr-1, false",
                "1.0-CR1, false",
                "1.0-alpha, false",
                "1.0-alpha1, false",
                "1.0-alpha.1, false",
                "1.0-alpha-1, false",
                "1.0-ALPHA, false",
                "1.0-beta, false",
                "1.0-beta2, false",
                "1.0-beta.2, false",
                "1.0-beta-2, false",
                "1.0-m1, false",
                "1.0-m.1, false",
                "1.0-m-1, false",
                "1.0-milestone.1, false",
            })
    void testIsStable(String input, boolean expected) {
        assertThat(new MavenVersion(input).isStable()).isEqualTo(expected);
    }
}
