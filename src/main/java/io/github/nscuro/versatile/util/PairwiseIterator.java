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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;

public class PairwiseIterator<T> implements Iterator<PairwiseIterator.Pair<T>> {

    public record Pair<T>(T left, T right) {
    }

    private final Iterator<T> delegate;
    private final long delegateSize;
    private T current;
    private T next;

    public PairwiseIterator(final Iterable<T> iterable) {
        if (!iterable.spliterator().hasCharacteristics(Spliterator.SIZED)) {
            throw new IllegalArgumentException("Unable to determine size of the provided iterable");
        }

        this.delegate = iterable.iterator();
        this.delegateSize = iterable.spliterator().getExactSizeIfKnown();
    }

    @Override
    public boolean hasNext() {
        return delegateSize >= 2 && delegate.hasNext();
    }

    @Override
    public Pair<T> next() {
        if (delegateSize < 2 || !delegate.hasNext()) {
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
