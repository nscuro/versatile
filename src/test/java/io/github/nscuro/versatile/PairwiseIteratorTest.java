package io.github.nscuro.versatile;

import io.github.nscuro.versatile.PairwiseIterator.Pair;
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
        assertThat(iterator.next()).isEqualTo(new Pair<>("A", null));
    }

    @Test
    void testWithNoItem() {
        final Iterator<Pair<String>> iterator = new PairwiseIterator<>(Collections.emptyList());
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(iterator::next);
    }

}