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

import io.github.nscuro.versatile.PairwiseIterator.Pair;
import io.github.nscuro.versatile.version.Version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

/**
 * A version range as defined in the vers specification.
 *
 * @param scheme      The versioning scheme of this version range
 * @param constraints The {@link Constraint}s composing this version range
 * @see <a href="https://github.com/package-url/purl-spec/tree/version-range-spec">vers specification</a>
 */
public record Vers(String scheme, List<Constraint> constraints) {

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

        final String scheme = parts[0];
        if (scheme.isBlank()) {
            throw new VersException("scheme must not be blank");
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

        return new Vers(scheme, constraints);
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
        if (constraintPair.size() > 0) {
            versList.add(new Vers(scheme, constraintPair));
        }
        return versList;
    }

    public static Builder builder(final String versioningScheme) {
        return new Builder(versioningScheme);
    }

    public String scheme() {
        return scheme;
    }

    public List<Constraint> constraints() {
        return List.copyOf(constraints);
    }

    public boolean isWildcard() {
        return constraints.size() == 1 && constraints.get(0).comparator() == Comparator.WILDCARD;
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
            return constraints.get(0).matches(testedVersion);
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
            return remainingConstraints.get(0).matches(testedVersion);
        }

        // Iterate over the current and next contiguous constraints pairs
        // (aka. pairwise) in the second list.
        var firstIteration = true;
        final PairwiseIterator<Constraint> constraintIter = new PairwiseIterator<>(remainingConstraints);
        Constraint currConstraint, nextConstraint = null;
        while (constraintIter.hasNext()) {
            final Pair<Constraint> constraintPair = constraintIter.next();
            currConstraint = constraintPair.left();
            nextConstraint = constraintPair.right();

            // If this is the first iteration and current comparator is "<" or <=" and
            // the "tested version" is less than the current version then the "tested version"
            // is IN the range. Check is finished.
            if (firstIteration) {
                if (isUpperBoundConstraint(currConstraint)
                        && testedVersion.compareTo(currConstraint.version()) < 0) {
                    return true;
                }

                firstIteration = false;
            }

            // If current comparator is ">" or >=" and next comparator is "<" or <=" and the "tested version"
            // is greater than the current version and the "tested version" is less than the next version then
            // the "tested version" is IN the range. Check is finished.
            if (isLowerBoundConstraint(currConstraint) && isUpperBoundConstraint(nextConstraint)) {
                if (testedVersion.compareTo(currConstraint.version()) > 0
                        && testedVersion.compareTo(nextConstraint.version()) < 0) {
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

        // If this is the last iteration and next comparator is ">" or >=" and the "tested version"
        // is greater than the next version then the "tested version" is IN the range. Check is finished.
        if (isLowerBoundConstraint(nextConstraint)
                && testedVersion.compareTo(nextConstraint.version()) > 0) {
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
            if (Set.of(Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(currComparator)
                    && Set.of(Comparator.EQUAL, Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(nextComparator)) {
                remainderConstraints.remove(nextIndex);
            }

            // If current comparator is "=", "<" or "<=" and next comparator is <" or <=", discard current constraint.
            if (Set.of(Comparator.EQUAL, Comparator.LESS_THAN, Comparator.LESS_THAN_OR_EQUAL).contains(currComparator)
                    && Set.of(Comparator.LESS_THAN, Comparator.LESS_THAN_OR_EQUAL).contains(nextComparator)) {
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
                if (Set.of(Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(prevComparator)
                        && Set.of(Comparator.EQUAL, Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(currComparator)) {
                    remainderConstraints.remove(currIndex);
                }

                // If previous comparator is "=", "<" or "<=" and current comparator is <" or <=",
                // discard previous constraint.
                if (Set.of(Comparator.EQUAL, Comparator.LESS_THAN, Comparator.LESS_THAN_OR_EQUAL).contains(prevComparator)
                        && Set.of(Comparator.LESS_THAN, Comparator.LESS_THAN_OR_EQUAL).contains(currComparator)) {
                    remainderConstraints.remove(prevConstraint);
                }
            }

            currIndex++;
        }

        // Concatenate the "unequal constraints" list and the filtered "constraints" list.
        final List<Constraint> simplifiedConstraints = Stream.concat(remainderConstraints.stream(), unequalConstraints.stream())
                .distinct()
                .sorted()
                .toList();

        return new Vers(scheme, simplifiedConstraints);
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
            throw new VersException("Invalid range %s: wildcard is only allowed with a single constraint".formatted(this));
        }

        // Ignoring all constraints with "!=" comparators...
        List<Constraint> tmpConstraints = constraints.stream()
                .filter(constraint -> constraint.comparator() != Comparator.NOT_EQUAL)
                .toList();
        if (tmpConstraints.size() < 2) {
            // No, or only one constraint remaining; Nothing to validate anymore.
            return this;
        }

        // A "=" constraint must be followed only by a constraint with one of "=", ">", ">=" as comparator (or no constraint).
        var constraintIter = new PairwiseIterator<>(tmpConstraints);
        while (constraintIter.hasNext()) {
            final Pair<Constraint> constraintPair = constraintIter.next();
            final Constraint currConstraint = constraintPair.left();
            final Constraint nextConstraint = constraintPair.right();

            if (currConstraint.comparator() == Comparator.EQUAL
                    && !Set.of(Comparator.EQUAL, Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(nextConstraint.comparator())) {
                throw new VersException("Invalid range %s: A = comparator must only be followed by a > or >= operator, but got: %s"
                        .formatted(this, nextConstraint.comparator().operator()));
            }
        }

        // And ignoring all constraints with "=" or "!=" comparators...
        tmpConstraints = tmpConstraints.stream()
                .filter(constraint -> constraint.comparator() != Comparator.EQUAL)
                .toList();
        if (tmpConstraints.size() < 2) {
            // No, or only one constraint remaining; Nothing to validate anymore.
            return this;
        }

        // ... the sequence of constraint comparators must be an alternation of greater and lesser comparators:
        //   * "<" and "<=" must be followed by one of ">", ">=" (or no constraint).
        //   * ">" and ">=" must be followed by one of "<", "<=" (or no constraint).
        constraintIter = new PairwiseIterator<>(tmpConstraints);
        while (constraintIter.hasNext()) {
            final Pair<Constraint> constraintPair = constraintIter.next();
            final Constraint currConstraint = constraintPair.left();
            final Constraint nextConstraint = constraintPair.right();

            if (Set.of(Comparator.LESS_THAN, Comparator.LESS_THAN_OR_EQUAL).contains(currConstraint.comparator())
                    && !Set.of(Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(nextConstraint.comparator())) {
                throw new VersException("Invalid range %s: A < or <= comparator must only be followed by a > or >= comparator, but got: %s"
                        .formatted(this, nextConstraint.comparator().operator()));
            }
            if (Set.of(Comparator.GREATER_THAN, Comparator.GREATER_THAN_OR_EQUAL).contains(currConstraint.comparator())
                    && !Set.of(Comparator.LESS_THAN, Comparator.LESS_THAN_OR_EQUAL).contains(nextConstraint.comparator())) {
                throw new VersException("Invalid range %s: A > or >= comparator must only be followed by a < or <= comparator, but got: %s"
                        .formatted(this, nextConstraint.comparator().operator()));
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
        if (!this.constraints().get(0).scheme().equals(vers.constraints().get(0).scheme())) {
            throw new VersException(
                "Vers ranges with different schemes cannot be checked for an overlap");
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
        var const1 =
            this.constraints().stream()
                .filter(
                    c ->
                        !c.comparator().equals(Comparator.EQUAL)
                            && !c.comparator().equals(Comparator.NOT_EQUAL))
                .toList();
        var const2 =
            vers.constraints().stream()
                .filter(
                    c ->
                        !c.comparator().equals(Comparator.EQUAL)
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
                    vers1.constraints().get(i).version(),
                    vers1.constraints().get(i + 1).version(),
                    vers2.constraints().get(j).version(),
                    vers2.constraints().get(j + 1).version())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        final String schemeStr = scheme().toLowerCase();
        final String constraintsStr = constraints.stream()
                .map(Constraint::toString)
                .collect(Collectors.joining("|"));
        return "vers:%s/%s".formatted(schemeStr, constraintsStr);
    }

    private static boolean isLowerBoundConstraint(final Constraint constraint) {
        return constraint != null && (constraint.comparator() == Comparator.GREATER_THAN
                || constraint.comparator() == Comparator.GREATER_THAN_OR_EQUAL);
    }

    private static boolean isUpperBoundConstraint(final Constraint constraint) {
        return constraint != null && (constraint.comparator() == Comparator.LESS_THAN
                || constraint.comparator() == Comparator.LESS_THAN_OR_EQUAL);
    }


    private static boolean isPartOf(Vers vers1, Vers vers2) {
        // get all equal constraints
        var equalityConstraints =
            vers1.constraints().stream()
                .filter(
                    c ->
                        c.comparator().equals(Comparator.EQUAL)
                            || c.comparator().equals(Comparator.GREATER_THAN_OR_EQUAL)
                            || c.comparator().equals(Comparator.LESS_THAN_OR_EQUAL))
                .toList();

        for (var constraint : equalityConstraints) {
            if (vers2.contains(constraint.version().toString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlapsWithUnboundedConstraint(Vers vers1, Vers vers2) {
        // if first constraint is < or last constraint is > than this is an unbound constraint
        var first = vers1.constraints().get(0);
        if (isUpperBoundConstraint(first)) {
            // this is an upper bound without limit such as "vers:generic/<1.2.3" should overlap with
            // "vers:generic/<1.2.1|>1.3.0"
            // if the version is larger than any of the versions of vers2, than we have an overlap
            for (var constraint2 : vers2.constraints()) {
                if (first.version().compareTo(constraint2.version()) > 0) {
                    return true;
                }
            }
        }

        var last = vers1.constraints().get(vers1.constraints().size() - 1);
        if (isLowerBoundConstraint(last)) {
            // this is a lower bound without limit such as "vers:generic/>1.2.3",
            // "vers:generic/>1.2.9|<1.3.0", "vers:generic/<1.3.0"
            // if the version is smaller than any of the versions of vers2, than we have an overlap
            for (var constraint2 : vers2.constraints()) {
                if (last.version().compareTo(constraint2.version()) < 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean boundsOverlap(
        Version lower1, Version upper1, Version lower2, Version upper2) {

        // Check if the ranges overlap
        return lower1.compareTo(upper2) < 0 && lower2.compareTo(upper1) < 0;
    }

    public static List<Constraint> removeUnboundedConstraints(Vers vers) {

        var constraints = vers.constraints();

        if (constraints.isEmpty()) {
            return constraints;
        }

        var first = constraints.get(0);
        if (isUpperBoundConstraint(first)) {
            constraints = constraints.stream().filter(c -> !c.equals(first)).toList();
        }

        if (constraints.isEmpty()) {
            return constraints;
        }

        var last = constraints.get(constraints.size() - 1);
        if (isLowerBoundConstraint(last)) {
            constraints = constraints.stream().filter(c -> !c.equals(last)).toList();
        }
        return constraints;
    }

    public static class Builder {

        private final String scheme;
        private final List<Constraint> constraints = new ArrayList<>();

        private Builder(final String scheme) {
            this.scheme = scheme;
        }

        public Constraint createConstraint(final Comparator comparator, final String version) {
            return new Constraint(scheme, comparator, VersionFactory.forScheme(scheme, version));
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
                constraints.add(new Constraint(scheme, comparator, VersionFactory.forScheme(scheme, versionStr)));
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
