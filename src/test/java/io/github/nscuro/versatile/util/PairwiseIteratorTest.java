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
package io.github.nscuro.versatile.util;

import io.github.nscuro.versatile.util.PairwiseIterator.Pair;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PairwiseIteratorTest {

    @Test
    void test() {
        final Iterator<Pair<String>> iterator = new PairwiseIterator<>(List.of("A", "B", "C", "D", "E"));
        assertThat(iterator.next()).isEqualTo(new Pair<>("A", "B"));
        assertThat(iterator.next()).isEqualTo(new Pair<>("B", "C"));
        assertThat(iterator.next()).isEqualTo(new Pair<>("C", "D"));
        assertThat(iterator.next()).isEqualTo(new Pair<>("D", "E"));
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(iterator::next);
    }

    @Test
    void testWithSingleItem() {
        final Iterator<Pair<String>> iterator = new PairwiseIterator<>(List.of("A"));
        assertThat(iterator.hasNext()).isFalse();
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(iterator::next);
    }

    @Test
    void testWithNoItem() {
        final Iterator<Pair<String>> iterator = new PairwiseIterator<>(Collections.emptyList());
        assertThat(iterator.hasNext()).isFalse();
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(iterator::next);
    }

}