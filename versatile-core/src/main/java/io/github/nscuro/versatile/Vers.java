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

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

import io.github.nscuro.versatile.spi.Version;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A version range as defined in the vers specification.
 *
 * @param scheme      The versioning scheme of this version range
 * @param constraints The {@link Constraint}s composing this version range
 * @see <a href="https://github.com/package-url/vers-spec">vers specification</a>
 */
public record Vers(String scheme, List<Constraint> constraints) {

    public Vers {
        requireNonNull(scheme, "scheme must not be null");
        requireNonNull(constraints, "constraints must not be null");
        if (constraints.isEmpty()) {
            throw new VersException("constraints must not be empty");
        }
    }

    /**
     * Parses a canonical {@code vers} string.
     * <p>
     * Per the vers specification, a {@code vers} string shall already be in canonical form.
     * {@code versString} values with the following properties are rejected:
     * <ul>
     *   <li>whitespace</li>
     *   <li>leading, trailing, or consecutive pipes</li>
     *   <li>unsorted constraints</li>
     *   <li>duplicate constraints</li>
     *   <li>invalid comparator sequences</li>
     * </ul>
     * <p>
     * Use {@link #parseLenient(String)} if inputs may be non-canonical.
     *
     * @throws VersException if the provided value is not a valid {@code vers} range.
     * @see #parseLenient(String)
     * @since 0.20.0
     */
    public static Vers parse(String versString) {
        return parse(versString, /* strict */ true);
    }

    /**
     * Parses a possibly non-canonical {@code vers} string, normalizing it instead of rejecting it
     * by sorting constraints, stripping leading and trailing pipes, and trimming whitespace.
     * <p>
     * Unlike {@link #parse(String)}, this does not enforce the spec's canonical-form rules.
     * It is meant for flexible parsing of externally-sourced ranges that may not be normalized.
     *
     * @throws VersException if the provided value is not a valid {@code vers} range.
     * @see <a href="https://github.com/package-url/vers-spec/issues/69">vers-spec#69</a>
     * @since 0.20.0
     */
    public static Vers parseLenient(String versString) {
        return parse(versString, /* strict */ false);
    }

    private static Vers parse(String versString, boolean strict) {
        requireNonNull(versString, "versString must not be null");
        if (versString.isBlank()) {
            throw new VersException("vers string must not be null or blank");
        }
        if (strict && containsWhitespace(versString)) {
            throw new VersException("vers string must not contain whitespace: \"%s\"".formatted(versString));
        }

        String[] parts = versString.split(":", 2);
        if (parts.length != 2) {
            throw new VersException(
                    "vers string does not contain a URI scheme separator: \"%s\"".formatted(versString));
        }

        if (!"vers".equals(parts[0])) {
            throw new VersException(
                    "URI scheme must be \"vers\", but is \"%s\" in \"%s\"".formatted(parts[0], versString));
        }

        parts = parts[1].split("/", 2);
        if (parts.length != 2) {
            throw new VersException(
                    "vers string does not contain a versioning scheme separator: \"%s\"".formatted(versString));
        }

        final String scheme = parts[0];
        if (scheme.isBlank()) {
            throw new VersException("scheme must not be blank in \"%s\"".formatted(versString));
        }

        String constraintsString = parts[1];
        if ("*".equals(constraintsString)) {
            return new Vers(scheme, List.of(new Constraint(scheme, Comparator.WILDCARD, null)));
        }

        if (strict) {
            if (constraintsString.startsWith("|")) {
                throw new VersException("constraints must not start with a pipe in \"%s\"".formatted(versString));
            }
            if (constraintsString.endsWith("|")) {
                throw new VersException("constraints must not end with a pipe in \"%s\"".formatted(versString));
            }
            if (constraintsString.contains("||")) {
                throw new VersException(
                        "constraints must not contain consecutive pipes in \"%s\"".formatted(versString));
            }
        } else {
            constraintsString = constraintsString.replaceAll("^\\|+", "").replaceAll("\\|+$", "");
        }

        parts = constraintsString.split("\\|");
        if (parts.length == 0) {
            throw new VersException("vers string contains no constraints: \"%s\"".formatted(versString));
        }

        Stream<Constraint> constraintStream = Arrays.stream(parts).map(part -> Constraint.parse(scheme, part));
        if (!strict) {
            constraintStream = constraintStream.sorted();
        }
        final List<Constraint> constraints = constraintStream.toList();

        if (!strict) {
            return new Vers(scheme, constraints);
        }

        for (int i = 0; i + 1 < constraints.size(); i++) {
            final Constraint curr = constraints.get(i);
            final Constraint next = constraints.get(i + 1);
            final int cmp = curr.compareTo(next);
            if (cmp > 0) {
                throw new VersException("constraints must be sorted by version, but \"%s\" precedes \"%s\" in \"%s\""
                        .formatted(curr, next, versString));
            }
            if (cmp == 0) {
                throw new VersException(
                        "version \"%s\" must occur only once, but is used by both \"%s\" and \"%s\" in \"%s\""
                                .formatted(curr.version(), curr, next, versString));
            }
        }

        return new Vers(scheme, constraints).validate();
    }

    public List<Vers> split() {
        var versSimplified = this.simplify();
        List<Vers> versList = new ArrayList<>();
        ArrayList<Constraint> constraintPair = new ArrayList<>();
        for (var constraint : versSimplified.constraints) {
            if (constraint.comparator().equals(Comparator.EQUAL)
                    || constraint.comparator().equals(Comparator.NOT_EQUAL)) {
                versList.add(Vers.builder(scheme).withConstraint(constraint).build());
            } else {
                constraintPair.add(constraint);
            }
            if (constraintPair.size() == 2) {
                versList.add(new Vers(scheme, List.copyOf(constraintPair)));
                constraintPair.clear();
            }
        }
        if (!constraintPair.isEmpty()) {
            versList.add(new Vers(scheme, constraintPair));
        }
        return versList;
    }

    public static Builder builder(String versioningScheme) {
        return new Builder(versioningScheme);
    }

    public String scheme() {
        return scheme;
    }

    public List<Constraint> constraints() {
        return List.copyOf(constraints);
    }

    public boolean isWildcard() {
        return constraints.size() == 1 && constraints.getFirst().comparator() == Comparator.WILDCARD;
    }

    public boolean contains(final String versionStr) {
        // Select the version equality and comparison procedures suitable for this
        // versioning scheme and use these for all version comparisons performed below.
        final Version testedVersion = VersionFactory.forScheme(scheme, versionStr);

        // If the constraint list contains only one item and the comparator is "*",
        // then the "tested version" is IN the range. Check is finished.
        //
        // If the constraint list contains only one item and the "tested version"
        // satisfies the comparator then the "tested version" is IN the range.
        // Check is finished.
        if (constraints.size() == 1) {
            return constraints.getFirst().matches(testedVersion);
        }

        boolean equalityMatches = false;
        boolean invertedEqualityMatches = false;
        final var remainingConstraints = new ArrayList<Constraint>(constraints.size());

        for (final Constraint constraint : constraints) {
            final Comparator comparator = constraint.comparator();

            // If the "tested version" is equal to any of the constraint version where the
            // constraint comparator is for equality (any of "=", "<=", or ">=") then the
            // "tested version" is in the range. Check is finished.
            if (comparator == Comparator.EQUAL
                    || comparator == Comparator.LESS_THAN_OR_EQUAL
                    || comparator == Comparator.GREATER_THAN_OR_EQUAL) {
                if (testedVersion.equals(requireNonNull(constraint.version()))) {
                    equalityMatches = true;
                }
            }
            // If the "tested version" is equal to any of the constraint version where
            // the constraint comparator is "!=" then the "tested version" is NOT in the range.
            // Check is finished.
            else if (comparator == Comparator.NOT_EQUAL && testedVersion.equals(requireNonNull(constraint.version()))) {
                invertedEqualityMatches = true;
            }

            // Everything except "=" and "!=" is a bound constraint ("<", "<=", ">", ">=")
            // and participates in the pairwise pass below. Note that "<=" and ">=" take
            // part in both the equality check above and the pairwise pass.
            if (comparator != Comparator.EQUAL && comparator != Comparator.NOT_EQUAL) {
                remainingConstraints.add(constraint);
            }
        }

        // If the "tested version" is equal to any of the constraint versions where the
        // constraint comparator is for equality (any of "=", "<=", or ">="), then the
        // "tested version" is IN the range. This takes precedence over inverted equality,
        // matching the order in which the spec evaluates these checks.
        if (equalityMatches) {
            return true;
        }

        // If the "tested version" is equal to any of the constraint versions where
        // the constraint comparator is "!=" then the "tested version" is NOT in the range.
        if (invertedEqualityMatches) {
            return false;
        }

        if (remainingConstraints.isEmpty()) {
            return false;
        }

        // If the remaining constraint list contains only one item and the "tested version"
        // satisfies the comparator then the "tested version" is IN the range.
        // Check is finished.
        //
        // NB: This step is not mentioned in the specification, but necessary because
        // the pairwise iteration below does not even start when fewer than two constraints
        // are left.
        if (remainingConstraints.size() == 1) {
            return remainingConstraints.getFirst().matches(testedVersion);
        }

        // Iterate over the current and next contiguous constraint pairs
        // (aka. pairwise) in the remaining list.
        for (int i = 0; i + 1 < remainingConstraints.size(); i++) {
            final Constraint currConstraint = remainingConstraints.get(i);
            final Constraint nextConstraint = remainingConstraints.get(i + 1);

            // If this is the first iteration and current comparator is "<" or <=" and
            // the "tested version" is less than the current version then the "tested version"
            // is IN the range. Check is finished.
            if (i == 0
                    && isUpperBoundConstraint(currConstraint)
                    && testedVersion.compareTo(requireNonNull(currConstraint.version())) < 0) {
                return true;
            }

            // If current comparator is ">" or >=" and next comparator is "<" or <=" and the "tested version"
            // is greater than the current version and the "tested version" is less than the next version then
            // the "tested version" is IN the range. Check is finished.
            if (isLowerBoundConstraint(currConstraint) && isUpperBoundConstraint(nextConstraint)) {
                if (testedVersion.compareTo(requireNonNull(currConstraint.version())) > 0
                        && testedVersion.compareTo(requireNonNull(nextConstraint.version())) < 0) {
                    return true;
                }
            }

            // If current comparator is "<" or <=" and next comparator is ">" or >=" then these versions are
            // out the range. Continue to the next iteration.
            else if (isUpperBoundConstraint(currConstraint) && isLowerBoundConstraint(nextConstraint)) {
                //noinspection UnnecessaryContinue
                continue;
            }

            // The algorithm does not cover this case, which indicates that it should
            // not happen. If it happens, then likely the order of constraints is invalid.
            else {
                throw new VersException("Constraints are in an invalid order");
            }
        }

        // If the last constraint's comparator is ">" or >=" and the "tested version"
        // is greater than its version then the "tested version" is IN the range.
        final Constraint lastConstraint = remainingConstraints.getLast();
        if (isLowerBoundConstraint(lastConstraint)
                && testedVersion.compareTo(requireNonNull(lastConstraint.version())) > 0) {
            return true;
        }

        // Reaching here without having finished the check before means that
        // the "tested version" is NOT in the range.
        return false;
    }

    public Vers simplify() {
        // Start from a list of constraints of comparator and version, sorted by
        // version and where each version occurs only once in any constraint.

        // If the constraints list contains a single constraint (star, equal or anything)
        // return this list and simplification is finished.
        if (constraints.size() < 2) {
            return this;
        }

        // Split the constraints list in two sub lists:
        //   * a list of "unequal constraints" where the comparator is "!="
        //   * a remainder list of "constraints" where the comparator is not "!="
        final List<Constraint> unequalConstraints = constraints.stream()
                .filter(constraint -> constraint.comparator() == Comparator.NOT_EQUAL)
                .toList();
        final List<Constraint> remainderConstraints = constraints.stream()
                .filter(constraint -> constraint.comparator() != Comparator.NOT_EQUAL)
                .collect(Collectors.toList()); // This list must be mutable

        // If the remainder list of "constraints" is empty, return the "unequal constraints"
        // list and simplification is finished.
        if (remainderConstraints.isEmpty()) {
            return new Vers(scheme, unequalConstraints);
        }

        // Iterate over the constraints list, considering the current and next contiguous constraints,
        // and the previous constraint (e.g., before current) if it exists:
        int currIndex = 0, nextIndex = 0;
        while (currIndex < remainderConstraints.size() - 1) {
            nextIndex = currIndex + 1;

            final Constraint currConstraint = remainderConstraints.get(currIndex);
            final Constraint nextConstraint = remainderConstraints.get(nextIndex);
            final Comparator currComparator = currConstraint.comparator();
            final Comparator nextComparator = nextConstraint.comparator();

            // If current comparator is ">" or ">=" and next comparator is "=", ">" or ">=", discard next constraint.
            if (isLowerBoundComparator(currComparator)
                    && (nextComparator == Comparator.EQUAL || isLowerBoundComparator(nextComparator))) {
                remainderConstraints.remove(nextIndex);
            }

            // If current comparator is "=", "<" or "<=" and next comparator is <" or <=", discard current constraint.
            if ((currComparator == Comparator.EQUAL || isUpperBoundComparator(currComparator))
                    && isUpperBoundComparator(nextComparator)) {
                remainderConstraints.remove(currIndex);

                // Previous constraint becomes current if it exists.
                if (currIndex > 0) {
                    currIndex -= 1;
                }
            }

            // If there is a previous constraint:
            if (currIndex > 0) {
                final Constraint prevConstraint = remainderConstraints.get(currIndex - 1);
                final Comparator prevComparator = prevConstraint.comparator();

                // If previous comparator is ">" or ">=" and current comparator is "=", ">" or ">=",
                // discard current constraint.
                if (isLowerBoundComparator(prevComparator)
                        && (currComparator == Comparator.EQUAL || isLowerBoundComparator(currComparator))) {
                    remainderConstraints.remove(currIndex);
                }

                // If previous comparator is "=", "<" or "<=" and current comparator is <" or <=",
                // discard previous constraint.
                if ((prevComparator == Comparator.EQUAL || isUpperBoundComparator(prevComparator))
                        && isUpperBoundComparator(currComparator)) {
                    remainderConstraints.remove(prevConstraint);
                }
            }

            currIndex++;
        }

        // Concatenate the "unequal constraints" list and the filtered "constraints" list.
        final List<Constraint> simplifiedConstraints = Stream.concat(
                        remainderConstraints.stream(), unequalConstraints.stream())
                .distinct()
                .sorted()
                .toList();

        return new Vers(scheme, simplifiedConstraints);
    }

    /**
     * Validates itself against the {@code vers} spec's rules.
     * <p>
     * <strong>Note:</strong> since version 0.20.0, validation is performed implicitly
     * by {@link #parse(String)}, {@link #parseLenient(String)}, and {@link Builder#build()}.
     * Calling it separately is not necessary.
     *
     * @return the validated {@link Vers}.
     */
    public Vers validate() {
        // The special star "*" comparator matches any version.
        // It must be used alone exclusive of any other constraint and must not be followed by a version.
        // For example "vers:deb/*" represent all the versions of a Debian package.
        // This includes past, current and possible future versions.
        // https://github.com/package-url/vers-spec/blob/main/docs/standard/specification.md#version-constraints
        boolean containsWildcard = false;
        for (final Constraint constraint : constraints) {
            if (constraint.comparator() == Comparator.WILDCARD) {
                containsWildcard = true;
                break;
            }
        }
        if (containsWildcard && constraints.size() > 1) {
            throw new VersException("""
                    Invalid range %s: wildcard is only allowed \
                    with a single constraint""".formatted(this));
        }

        // Ignoring all constraints with "!=" comparators...
        final List<Constraint> tmpConstraints = new ArrayList<>(constraints.size());
        for (final Constraint constraint : constraints) {
            if (constraint.comparator() != Comparator.NOT_EQUAL) {
                tmpConstraints.add(constraint);
            }
        }
        if (tmpConstraints.size() < 2) {
            // No, or only one constraint remaining; Nothing to validate anymore.
            return this;
        }

        // A "=" constraint must be followed only by a constraint with one of "=", ">", ">=" as comparator (or no
        // constraint).
        for (int i = 0; i + 1 < tmpConstraints.size(); i++) {
            final Comparator currComparator = tmpConstraints.get(i).comparator();
            final Comparator nextComparator = tmpConstraints.get(i + 1).comparator();

            if (currComparator == Comparator.EQUAL
                    && nextComparator != Comparator.EQUAL
                    && !isLowerBoundComparator(nextComparator)) {
                throw new VersException("""
                        Invalid range %s: A = comparator must only be \
                        followed by a > or >= operator, but got: %s""".formatted(this, nextComparator.operator()));
            }
        }

        // ... the sequence of constraint comparators must be an alternation of greater and lesser comparators:
        //   * "<" and "<=" must be followed by one of ">", ">=" (or no constraint).
        //   * ">" and ">=" must be followed by one of "<", "<=" (or no constraint).
        tmpConstraints.removeIf(constraint -> constraint.comparator() == Comparator.EQUAL);
        if (tmpConstraints.size() < 2) {
            // No, or only one constraint remaining; Nothing to validate anymore.
            return this;
        }

        for (int i = 0; i + 1 < tmpConstraints.size(); i++) {
            final Comparator currComparator = tmpConstraints.get(i).comparator();
            final Comparator nextComparator = tmpConstraints.get(i + 1).comparator();

            if (isUpperBoundComparator(currComparator) && !isLowerBoundComparator(nextComparator)) {
                throw new VersException("""
                        Invalid range %s: A < or <= comparator must only be \
                        followed by a > or >= comparator, but got: %s""".formatted(this, nextComparator.operator()));
            }
            if (isLowerBoundComparator(currComparator) && !isUpperBoundComparator(nextComparator)) {
                throw new VersException("""
                        Invalid range %s: A > or >= comparator must only be \
                        followed by a < or <= comparator, but got: %s""".formatted(this, nextComparator.operator()));
            }
        }

        return this;
    }

    /**
     * Checks if his vers has a potential version overlap with another vers
     *
     * @param vers the vers to check for overlap with
     * @return true if there is an overlap, false otherwise
     * @throws VersException if the compared verses have different schemes
     */
    public boolean overlapsWith(Vers vers) {
        // Ensure both Vers use the same scheme
        if (!this.constraints()
                .getFirst()
                .scheme()
                .equals(vers.constraints().getFirst().scheme())) {
            throw new VersException("Vers ranges with different schemes cannot be checked for an overlap");
        }
        // if one of the vers is empty, there can't be an overlap
        if (this.constraints().isEmpty() || vers.constraints().isEmpty()) {
            return false;
        }

        // there is always an overlap if one of the versions is a wildcard
        if (this.isWildcard() || vers.isWildcard()) {
            return true;
        }

        // check if any version denoted as equal, greaterOrEqual or smallerOrEqual constraint is part of the other range
        if (isPartOf(this, vers) || isPartOf(vers, this)) {
            return true;
        }

        // we checked all the equality parts, we can remove all equal parts and do upper / lower bounds
        // check on the rest
        var const1 = this.constraints().stream()
                .filter(c -> !c.comparator().equals(Comparator.EQUAL)
                        && !c.comparator().equals(Comparator.NOT_EQUAL))
                .toList();
        var const2 = vers.constraints().stream()
                .filter(c -> !c.comparator().equals(Comparator.EQUAL)
                        && !c.comparator().equals(Comparator.NOT_EQUAL))
                .toList();

        // if one of the vers doesn't contain ranges anymore, we are done
        if (const1.isEmpty() || const2.isEmpty()) {
            return false;
        }

        // This ensures we have pairs of < then > then < then > and so on
        //   without cases of < or <= being followed by < or <=
        // From this point on we do not care about the `orEqual` anymore,as it was checked before already
        var vers1 = new Vers(this.scheme(), const1).simplify();
        var vers2 = new Vers(vers.scheme(), const2).simplify();

        // unbounded constraints are < at the beginning or > at the end of a vers
        // this may be mergeable with the bounds checks happening later
        if (overlapsWithUnboundedConstraint(vers1, vers2) || overlapsWithUnboundedConstraint(vers2, vers1)) {
            return true;
        }

        // we can drop all unbounded constraints, they have no relevance for an overlap anymore
        const1 = removeUnboundedConstraints(vers1);
        const2 = removeUnboundedConstraints(vers2);

        if (const1.isEmpty() || const2.isEmpty()) {
            return false;
        }
        vers1 = new Vers(vers1.scheme(), const1);
        vers2 = new Vers(vers2.scheme(), const2);

        // at this point both vers contains pairs of boundaries starting with > < > < > <
        // it should always be an even number of boundaries. we can split those up into ranges
        if (vers1.constraints().size() % 2 != 0 || vers2.constraints().size() % 2 != 0) {
            throw new RuntimeException("Unexpected behavior detected! Please file a bug");
        }

        for (var i = 0; i < vers1.constraints().size(); i = i + 2) {
            for (var j = 0; j < vers2.constraints().size(); j = j + 2) {

                if (boundsOverlap(
                        requireNonNull(vers1.constraints().get(i).version()),
                        requireNonNull(vers1.constraints().get(i + 1).version()),
                        requireNonNull(vers2.constraints().get(j).version()),
                        requireNonNull(vers2.constraints().get(j + 1).version()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Inverts a given vers expression and returns a new simplified vers
     *
     * @throws VersException if the vers is a wildcard
     */
    public Vers invert() {
        if (isWildcard()) {
            throw new VersException("Can not invert wildcard vers");
        }
        final List<Constraint> inverted = constraints.stream()
                .map(Constraint::invert)
                .filter(Objects::nonNull)
                .toList();
        return new Vers(this.scheme(), inverted).simplify();
    }

    @Override
    public String toString() {
        final String schemeStr = scheme().toLowerCase();
        final String constraintsStr =
                constraints.stream().map(Constraint::toString).collect(Collectors.joining("|"));
        return "vers:%s/%s".formatted(schemeStr, constraintsStr);
    }

    private static boolean isLowerBoundConstraint(@Nullable Constraint constraint) {
        return constraint != null && isLowerBoundComparator(constraint.comparator());
    }

    private static boolean isUpperBoundConstraint(@Nullable Constraint constraint) {
        return constraint != null && isUpperBoundComparator(constraint.comparator());
    }

    private static boolean isLowerBoundComparator(Comparator comparator) {
        return comparator == Comparator.GREATER_THAN || comparator == Comparator.GREATER_THAN_OR_EQUAL;
    }

    private static boolean isUpperBoundComparator(Comparator comparator) {
        return comparator == Comparator.LESS_THAN || comparator == Comparator.LESS_THAN_OR_EQUAL;
    }

    private static boolean isPartOf(Vers vers1, Vers vers2) {
        // get all equal constraints
        var equalityConstraints = vers1.constraints().stream()
                .filter(c -> c.comparator().equals(Comparator.EQUAL)
                        || c.comparator().equals(Comparator.GREATER_THAN_OR_EQUAL)
                        || c.comparator().equals(Comparator.LESS_THAN_OR_EQUAL))
                .toList();

        for (var constraint : equalityConstraints) {
            if (vers2.contains(requireNonNull(constraint.version()).toString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlapsWithUnboundedConstraint(Vers vers1, Vers vers2) {
        // if first constraint is < or last constraint is > than this is an unbound constraint
        var first = vers1.constraints().getFirst();
        if (isUpperBoundConstraint(first)) {
            // this is an upper bound without limit such as "vers:generic/<1.2.3" should overlap with
            // "vers:generic/<1.2.1|>1.3.0"
            // if the version is larger than any of the versions of vers2, than we have an overlap
            for (var constraint2 : vers2.constraints()) {
                if (requireNonNull(first.version()).compareTo(requireNonNull(constraint2.version())) > 0) {
                    return true;
                }
            }
        }

        var last = vers1.constraints().getLast();
        if (isLowerBoundConstraint(last)) {
            // this is a lower bound without limit such as "vers:generic/>1.2.3",
            // "vers:generic/>1.2.9|<1.3.0", "vers:generic/<1.3.0"
            // if the version is smaller than any of the versions of vers2, than we have an overlap
            for (var constraint2 : vers2.constraints()) {
                if (requireNonNull(last.version()).compareTo(requireNonNull(constraint2.version())) < 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean boundsOverlap(Version lower1, Version upper1, Version lower2, Version upper2) {

        // Check if the ranges overlap
        return lower1.compareTo(upper2) < 0 && lower2.compareTo(upper1) < 0;
    }

    public static List<Constraint> removeUnboundedConstraints(Vers vers) {

        var constraints = vers.constraints();

        if (constraints.isEmpty()) {
            return constraints;
        }

        var first = constraints.getFirst();
        if (isUpperBoundConstraint(first)) {
            constraints = constraints.stream().filter(c -> !c.equals(first)).toList();
        }

        if (constraints.isEmpty()) {
            return constraints;
        }

        var last = constraints.getLast();
        if (isLowerBoundConstraint(last)) {
            constraints = constraints.stream().filter(c -> !c.equals(last)).toList();
        }
        return constraints;
    }

    private static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    public static class Builder {

        private final String scheme;
        private final List<Constraint> constraints = new ArrayList<>();

        private Builder(String scheme) {
            this.scheme = scheme;
        }

        public Constraint createConstraint(Comparator comparator, String version) {
            return new Constraint(scheme, comparator, VersionFactory.forScheme(scheme, version));
        }

        public Constraint parseConstraint(String constraintStr) {
            return Constraint.parse(scheme, constraintStr);
        }

        public Builder withConstraint(Constraint constraint) {
            constraints.add(requireNonNull(constraint, "constraint cannot be null"));
            return this;
        }

        public Builder withConstraint(Comparator comparator, @Nullable String versionStr) {
            if (versionStr == null) {
                constraints.add(new Constraint(scheme, comparator, null));
            } else {
                constraints.add(new Constraint(scheme, comparator, VersionFactory.forScheme(scheme, versionStr)));
            }

            return this;
        }

        public Builder withConstraint(String constraintStr) {
            constraints.add(parseConstraint(constraintStr));
            return this;
        }

        public boolean hasConstraints() {
            return !constraints.isEmpty();
        }

        public Vers build() {
            requireNonNull(scheme, "scheme must not be null");
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
