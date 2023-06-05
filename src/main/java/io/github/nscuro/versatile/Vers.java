package io.github.nscuro.versatile;

import java.util.Arrays;
import java.util.List;

public record Vers(Scheme scheme, List<Constraint> constraints) {

    /**
     * @param versString
     * @return
     * @see <a href="https://github.com/package-url/purl-spec/blob/version-range-spec/VERSION-RANGE-SPEC.rst#parsing-and-validating-version-range-specifiers">Parsing and validating version range specifiers</a>
     */
    public static Vers parse(final String versString) {
        if (versString == null || versString.isBlank()) {
            throw new VersException();
        }

        String[] parts = versString.split(":", 2);
        if (parts.length != 2) {
            throw new VersException();
        }

        if (!"vers".equals(parts[0])) {
            throw new VersException();
        }

        parts = parts[1].split("/", 2);
        if (parts.length != 2) {
            throw new VersException();
        }

        final Scheme scheme;
        try {
            scheme = Scheme.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new VersException(e);
        }

        final String constraintsString = parts[1].replaceAll("^\\|+", "").replaceAll("\\|+$", "");
        if ("*".equals(constraintsString)) {
            return new Vers(scheme, List.of(new Constraint(Comparator.WILDCARD, null)));
        }

        parts = constraintsString.split("\\|");
        if (parts.length == 0) {
            throw new VersException();
        }

        final List<Constraint> constraints = Arrays.stream(parts)
                .map(Constraint::parse)
                .toList();

        return new Vers(scheme, constraints);
    }

    public Scheme scheme() {
        return scheme;
    }

    public List<Constraint> constraints() {
        return List.copyOf(constraints);
    }

    /**
     * @param version
     * @return
     * @see <a href="https://github.com/package-url/purl-spec/blob/version-range-spec/VERSION-RANGE-SPEC.rst#checking-if-a-version-is-contained-within-a-range">Checking if a version is contained within a range</a>
     */
    public boolean contains(final String version) {
        if (constraints != null && constraints.size() == 1
                && constraints.get(0).comparator() == Comparator.WILDCARD) {
            return true;
        }

        return false;
    }

}
