package io.github.nscuro.versatile;

import io.github.nscuro.versatile.version.Version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

/**
 * A version range as defined in the vers specification.
 *
 * @param scheme      The {@link VersioningScheme} of this version range
 * @param constraints The {@link Constraint}s composing this version range
 * @see <a href="https://github.com/package-url/purl-spec/tree/version-range-spec">vers specification</a>
 */
public record Vers(VersioningScheme scheme, List<Constraint> constraints) {

    public Vers {
        if (scheme == null) {
            throw new VersException("versioning scheme must not be null");
        }
        if (constraints == null || constraints.isEmpty()) {
            throw new VersException("constraints must not be null or empty");
        }
    }

    public static Vers parse(final String versString) {
        if (versString == null || versString.isBlank()) {
            throw new VersException("vers string must not be null or blank");
        }

        String[] parts = versString.split(":", 2);
        if (parts.length != 2) {
            throw new VersException("vers string does not contain a URI scheme separator");
        }

        if (!"vers".equals(parts[0])) {
            throw new VersException("URI scheme must be \"vers\", but is \"%s\"".formatted(parts[0]));
        }

        parts = parts[1].split("/", 2);
        if (parts.length != 2) {
            throw new VersException("vers string does not contain a versioning scheme separator");
        }

        final VersioningScheme scheme;
        try {
            scheme = VersioningScheme.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new VersException("vers string contains an invalid versioning scheme", e);
        }

        final String constraintsString = parts[1].replaceAll("^\\|+", "").replaceAll("\\|+$", "");
        if ("*".equals(constraintsString)) {
            return new Vers(scheme, List.of(new Constraint(scheme, Comparator.WILDCARD, null)));
        }

        parts = constraintsString.split("\\|");
        if (parts.length == 0) {
            throw new VersException("vers string contains no constraints");
        }

        final List<Constraint> constraints = Arrays.stream(parts)
                .map(part -> Constraint.parse(scheme, part))
                .toList();

        return new Vers(scheme, constraints).validate();
    }

    public static Builder builder(final VersioningScheme versioningScheme) {
        return new Builder(versioningScheme);
    }

    public VersioningScheme scheme() {
        return scheme;
    }

    public List<Constraint> constraints() {
        return List.copyOf(constraints);
    }

    public boolean isWildcard() {
        return constraints.size() == 1 && constraints.get(0).comparator() == Comparator.WILDCARD;
    }

    /**
     * @param versionStr
     * @return
     * @see <a href="https://github.com/package-url/purl-spec/blob/version-range-spec/VERSION-RANGE-SPEC.rst#checking-if-a-version-is-contained-within-a-range">Algorithm description</a>
     */
    public boolean contains(final String versionStr) {
        // Select the version equality and comparison procedures suitable for this
        // versioning scheme and use these for all version comparisons performed below.
        final Version testedVersion = Version.forScheme(scheme, versionStr);

        // If the constraint list contains only one item and the comparator is "*",
        // then the "tested version" is IN the range. Check is finished.
        //
        // If the constraint list contains only one item and the "tested version"
        // satisfies the comparator then the "tested version" is IN the range.
        // Check is finished.
        if (constraints.size() == 1 && constraints.get(0).matches(testedVersion)) {
            return true;
        }

        // If the "tested version" is equal to any of the constraint version where the
        // constraint comparator is for equality (any of "=", "<=", or ">=") then the
        // "tested version" is in the range. Check is finished.
        final boolean equalityMatches = constraints.stream()
                .filter(constraint -> constraint.comparator() == Comparator.LESS_THAN_OR_EQUAL
                        || constraint.comparator() == Comparator.GREATER_THAN_OR_EQUAL
                        || constraint.comparator() == Comparator.EQUAL)
                .map(Constraint::version)
                .anyMatch(Predicate.isEqual(testedVersion));
        if (equalityMatches) {
            return true;
        }

        // If the "tested version" is equal to any of the constraint version where
        // the constraint comparator is "!=" then the "tested version" is NOT in the range.
        // Check is finished.
        final boolean invertedEqualityMatches = constraints.stream()
                .filter(constraint -> constraint.comparator() == Comparator.NOT_EQUAL)
                .map(Constraint::version)
                .anyMatch(Predicate.isEqual(testedVersion));
        if (invertedEqualityMatches) {
            return false;
        }

        // Split the constraint list in two sub lists:
        //   * a first list where the comparator is "=" or "!="
        //   * a second list where the comparator is neither "=" nor "!="
        final List<Constraint> remainingConstraints = constraints.stream()
                .filter(constraint -> constraint.comparator() != Comparator.EQUAL)
                .filter(constraint -> constraint.comparator() != Comparator.NOT_EQUAL)
                .toList();
        if (constraints.isEmpty()) {
            return false;
        }

        // Iterate over the current and next contiguous constraints pairs
        // (aka. pairwise) in the second list.
        final PairwiseIterator<Constraint> constraintIter = new PairwiseIterator<>(remainingConstraints);
        while (constraintIter.hasNext()) {
            final Map.Entry<Constraint, Constraint> constraintPair = constraintIter.next();
            final Constraint currConstraint = constraintPair.getKey();
            final Constraint nextConstraint = constraintPair.getValue();

            // If this is the first iteration and current comparator is "<" or <=" and
            // the "tested version" is less than the current version then the "tested version"
            // is IN the range. Check is finished.
            if (nextConstraint == null
                    && Set.of(Comparator.LESS_THAN, Comparator.LESS_THAN_OR_EQUAL).contains(currConstraint.comparator())
                    && testedVersion.compareTo(currConstraint.version()) < 0) {
                return true;
            }

            // If this is the last iteration and next comparator is ">" or >=" and the "tested version"
            // is greater than the next version then the "tested version" is IN the range. Check is finished.
            if (nextConstraint == null
                    && Set.of(Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(nextConstraint.comparator())
                    && testedVersion.compareTo(nextConstraint.version()) > 0) {
                return true;
            }

            // If current comparator is ">" or >=" and next comparator is "<" or <=" and the "tested version"
            // is greater than the current version and the "tested version" is less than the next version then
            // the "tested version" is IN the range. Check is finished.
            if (Set.of(Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(currConstraint.comparator())
                    && Set.of(Comparator.LESS_THAN, Comparator.LESS_THAN_OR_EQUAL).contains(nextConstraint.comparator())) {
                if (testedVersion.compareTo(currConstraint.version()) > 0
                        && testedVersion.compareTo(nextConstraint.version()) < 0) {
                    return true;
                }
            }

            // If current comparator is "<" or <=" and next comparator is ">" or >=" then these versions are
            // out the range. Continue to the next iteration.
            else if (Set.of(Comparator.LESS_THAN, Comparator.LESS_THAN_OR_EQUAL).contains(currConstraint.comparator())
                    && Set.of(Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(nextConstraint.comparator())) {
                continue;
            }

            // The algorithm does not cover this case, which indicates that it should
            // not happen. If it happens, then likely the order of constraints is invalid.
            else {
                throw new VersException("Constraints are in an invalid order");
            }
        }

        // Reaching here without having finished the check before means that
        // the "tested version" is NOT in the range.
        return false;
    }

    public Vers validate() {
        // The special star "*" comparator matches any version.
        // It must be used alone exclusive of any other constraint and must not be followed by a version.
        // For example "vers:deb/*" represent all the versions of a Debian package.
        // This includes past, current and possible future versions.
        // https://github.com/package-url/purl-spec/blob/version-range-spec/VERSION-RANGE-SPEC.rst#version-constraint
        final boolean containsWildcard = constraints.stream()
                .map(Constraint::comparator)
                .anyMatch(Comparator.WILDCARD::equals);
        if (containsWildcard && constraints.size() > 1) {
            throw new VersException("comparator %s is only allowed with a single constraint");
        }

        // Ignoring all constraints with "!=" comparators...
        List<Constraint> tmpConstraints = constraints.stream()
                .filter(constraint -> constraint.comparator() != Comparator.NOT_EQUAL)
                .toList();
        if (tmpConstraints.isEmpty()) {
            return this;
        }

        // A "=" constraint must be followed only by a constraint with one of "=", ">", ">=" as comparator (or no constraint).
        var constraintIter = new PairwiseIterator<>(tmpConstraints);
        while (constraintIter.hasNext()) {
            final Map.Entry<Constraint, Constraint> constraintPair = constraintIter.next();
            final Constraint currConstraint = constraintPair.getKey();
            final Constraint nextConstraint = constraintPair.getValue();

            if (currConstraint.comparator() == Comparator.EQUAL
                    && !Set.of(Comparator.EQUAL, Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(nextConstraint.comparator())) {
                throw new VersException("A = comparator must only be followed by a > or >= operator, but got: %s".formatted(nextConstraint.comparator().operator()));
            }
        }

        // And ignoring all constraints with "=" or "!=" comparators...
        tmpConstraints = tmpConstraints.stream()
                .filter(constraint -> constraint.comparator() != Comparator.EQUAL)
                .toList();
        if (tmpConstraints.isEmpty()) {
            return this;
        }

        // ... the sequence of constraint comparators must be an alternation of greater and lesser comparators:
        //   * "<" and "<=" must be followed by one of ">", ">=" (or no constraint).
        //   * ">" and ">=" must be followed by one of "<", "<=" (or no constraint).
        constraintIter = new PairwiseIterator<>(tmpConstraints);
        while (constraintIter.hasNext()) {
            final Map.Entry<Constraint, Constraint> constraintPair = constraintIter.next();
            final Constraint currConstraint = constraintPair.getKey();
            final Constraint nextConstraint = constraintPair.getValue();

            if (Set.of(Comparator.LESS_THAN, Comparator.LESS_THAN_OR_EQUAL).contains(currConstraint.comparator())
                    && !Set.of(Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(nextConstraint.comparator())) {
                throw new VersException("A < or <= comparator must only be followed by a > or >= comparator, but got: %s"
                        .formatted(nextConstraint.comparator().operator()));
            }
            if (Set.of(Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(currConstraint.comparator())
                    && !Set.of(Comparator.LESS_THAN, Comparator.LESS_THAN_OR_EQUAL).contains(nextConstraint.comparator())) {
                throw new VersException("A > or >= comparator must only be followed by a < or <= comparator, but got: %s"
                        .formatted(nextConstraint.comparator().operator()));
            }
        }

        return this;
    }

    @Override
    public String toString() {
        final String schemeStr = scheme().name().toLowerCase();
        final String constraintsStr = constraints.stream()
                .map(Constraint::toString)
                .collect(Collectors.joining("|"));
        return "vers:%s/%s".formatted(schemeStr, constraintsStr);
    }

    public static class Builder {

        private final VersioningScheme scheme;
        private final List<Constraint> constraints = new ArrayList<>();

        private Builder(final VersioningScheme scheme) {
            this.scheme = scheme;
        }

        public Constraint createConstraint(final Comparator comparator, final String version) {
            return new Constraint(scheme, comparator, Version.forScheme(scheme, version));
        }

        public Constraint parseConstraint(final String constraintStr) {
            return Constraint.parse(scheme, constraintStr);
        }

        public Builder withConstraint(final Constraint constraint) {
            if (constraint == null) {
                throw new VersException("constraint must not be null");
            }
            constraints.add(constraint);
            return this;
        }

        public Builder withConstraint(final Comparator comparator, final String versionStr) {
            if (versionStr == null) {
                constraints.add(new Constraint(scheme, comparator, null));
            } else {
                constraints.add(new Constraint(scheme, comparator, Version.forScheme(scheme, versionStr)));
            }

            return this;
        }

        public Builder withConstraint(final String constraintStr) {
            constraints.add(parseConstraint(constraintStr));
            return this;
        }

        public boolean hasConstraints() {
            return !constraints.isEmpty();
        }

        public Vers build() {
            if (scheme == null) {
                throw new VersException("scheme must not be null");
            }
            if (constraints.isEmpty()) {
                throw new VersException("constraints must not be empty");
            }
            if (constraints.stream().map(Constraint::scheme).anyMatch(not(scheme::equals))) {
                throw new VersException("constraints must have identical versioning schemes (%s)".formatted(scheme));
            }

            constraints.sort(Constraint::compareTo);

            return new Vers(scheme, constraints).validate();
        }

        public Optional<Vers> maybeBuild() {
            try {
                return Optional.of(build());
            } catch (VersException e) {
                return Optional.empty();
            }
        }

    }

}
