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

import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractVersionTest {

    enum ComparisonExpectation {

        IS_LOWER_THAN((x, y) -> assertThat(x).isLessThan(y)),
        IS_EQUAL_TO((x, y) -> assertThat(x).isEqualByComparingTo(y)),
        IS_HIGHER_THAN((x, y) -> assertThat(x).isGreaterThan(y));

        private final BiConsumer<Version, Version> evaluator;

        ComparisonExpectation(final BiConsumer<Version, Version> evaluator) {
            this.evaluator = evaluator;
        }

        void evaluate(final Version x, final Version y) {
            evaluator.accept(x, y);
        }

    }

}
