package io.github.nscuro.versatile;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PairwiseIteratorTest {

    @Test
    void test() {
        final Iterator<Map.Entry<String, String>> iterator = new PairwiseIterator<>(List.of("A", "B", "C", "D", "E"));
        assertThat(iterator.next()).isEqualTo(Pair.of("A", "B"));
        assertThat(iterator.next()).isEqualTo(Pair.of("B", "C"));
        assertThat(iterator.next()).isEqualTo(Pair.of("C", "D"));
        assertThat(iterator.next()).isEqualTo(Pair.of("D", "E"));
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(iterator::next);
    }

}