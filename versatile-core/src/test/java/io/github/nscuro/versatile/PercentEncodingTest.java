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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PercentEncodingTest {

    @ParameterizedTest
    @CsvSource(
            value = {
                "1.2.3;1.2.3",
                "2024-01-01T00:00:00Z;2024-01-01T00:00:00Z",
                "1.0%2F0;1.0%252F0",
                "1.0.0-a b;1.0.0-a%20b",
                "1<2>3=4!5*6|7;1%3C2%3E3%3D4%215%2A6%7C7",
                "1.0.0-ä;1.0.0-%C3%A4",
            },
            delimiter = ';')
    void shouldEncode(String version, String expected) {
        assertThat(PercentEncoding.encode(version)).isEqualTo(expected);
        assertThat(PercentEncoding.decode(expected, /* strict */ true)).isEqualTo(version);
    }

    @Nested
    class DecodeTest {
        @ParameterizedTest
        @ValueSource(
                strings = {
                    "1.0%2G0",
                    "1.0%2",
                    "2024-01-01T00%3A00%3A00Z",
                    "1.0%2f0",
                    "1.0%FF",
                })
        void shouldRejectNonCanonicalWhenStrict(String version) {
            assertThatExceptionOfType(VersException.class)
                    .isThrownBy(() -> PercentEncoding.decode(version, /* strict */ true));
        }

        @Test
        void shouldDecodeNonCanonicalWhenLenient() {
            assertThat(PercentEncoding.decode("2024-01-01T00%3a00%3a00Z", /* strict */ false))
                    .isEqualTo("2024-01-01T00:00:00Z");
        }

        @Test
        void shouldRejectInvalidTripletWhenLenient() {
            assertThatExceptionOfType(VersException.class).isThrownBy(() -> PercentEncoding.decode("1.0%2G0", false));
        }
    }
}
