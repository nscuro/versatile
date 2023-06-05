package io.github.nscuro.versatile;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public record Constraint(Comparator comparator, String version) {

    public static Constraint parse(final String constraintStr) {
        final Comparator comparator;
        if (constraintStr.startsWith("<=")) {
            comparator = Comparator.LESS_THAN_OR_EQUAL;
        } else if (constraintStr.startsWith(">=")) {
            comparator = Comparator.GREATER_THAN_OR_EQUAL;
        } else if (constraintStr.startsWith("!=")) {
            comparator = Comparator.NOT_EQUAL;
        } else if (constraintStr.startsWith("<")) {
            comparator = Comparator.LESS_THAN;
        } else if (constraintStr.startsWith(">")) {
            comparator = Comparator.GREATER_THAN;
        } else {
            comparator = Comparator.EQUAL;
        }

        final String version = constraintStr.replaceFirst("^" + Pattern.quote(comparator.operator()), "");
        if (version.isBlank()) {
            throw new VersException();
        }

        return new Constraint(comparator, maybeUrlDecode(version));
    }

    private static String maybeUrlDecode(final String version) {
        if (version.contains("%")) {
            return URLDecoder.decode(version, StandardCharsets.UTF_8);
        }

        return version;
    }

}
