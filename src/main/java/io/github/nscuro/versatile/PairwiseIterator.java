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

import java.util.Iterator;
import java.util.NoSuchElementException;

class PairwiseIterator<T> implements Iterator<PairwiseIterator.Pair<T>> {

    record Pair<T>(T left, T right) {
    }

    private final Iterator<T> delegate;
    private T current;
    private T next;

    PairwiseIterator(final Iterable<T> iterable) {
        this.delegate = iterable.iterator();
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public Pair<T> next() {
        if (!delegate.hasNext()) {
            throw new NoSuchElementException();
        }

        if (current == null) {
            current = delegate.next();
        }
        if (delegate.hasNext()) {
            next = delegate.next();
        }

        final var item = new Pair<>(current, next);
        current = next;
        return item;
    }

}
