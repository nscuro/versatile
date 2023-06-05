package io.github.nscuro.versatile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersTest {

    @Test
    void testParse() {
        final Vers vers = Vers.parse("vers:npm/1.2.3|>=2.0.0|<5.0.0");
        assertThat(vers).isNotNull();
        assertThat(vers.scheme()).isEqualTo(Scheme.NPM);
        assertThat(vers.constraints()).satisfiesExactly(
                constraint -> {
                    assertThat(constraint).isNotNull();
                    assertThat(constraint.comparator()).isEqualTo(Comparator.EQUAL);
                    assertThat(constraint.version()).isEqualTo("1.2.3");
                },
                constraint -> {
                    assertThat(constraint).isNotNull();
                    assertThat(constraint.comparator()).isEqualTo(Comparator.GREATER_THAN_OR_EQUAL);
                    assertThat(constraint.version()).isEqualTo("2.0.0");
                },
                constraint -> {
                    assertThat(constraint).isNotNull();
                    assertThat(constraint.comparator()).isEqualTo(Comparator.LESS_THAN);
                    assertThat(constraint.version()).isEqualTo("5.0.0");
                }
        );
    }

}