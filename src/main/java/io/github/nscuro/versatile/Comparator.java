package io.github.nscuro.versatile;

public enum Comparator {

    LESS_THAN_OR_EQUAL("<="),

    LESS_THAN("<"),

    EQUAL("="),

    NOT_EQUAL("!="),

    GREATER_THAN(">"),

    GREATER_THAN_OR_EQUAL(">="),

    WILDCARD("*");

    private final String operator;

    Comparator(final String operator) {
        this.operator = operator;
    }

    String operator() {
        return operator;
    }

}
