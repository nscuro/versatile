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

import io.github.nscuro.versatile.version.InvalidVersionException;
import io.github.nscuro.versatile.version.VersioningScheme;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.trimToNull;

public final class VersUtils {

    private VersUtils() {
    }

    /**
     * Convert an ecosystem and version range as used by GitHub Security Advisories to a {@link Vers} range.
     * <p>
     * Ranges are composed of one or more constraints, separated by commas. Valid comparators for constraints
     * are {@code =}, {@code >=}, {@code >}, {@code <}, and {@code <=}. For example, a valid range is {@code >= 1.2.3, < 5.0.1}.
     *
     * @param ecosystem The ecosystem of the affected package
     * @param rangeExpr The affected version range expression
     * @return The resulting {@link Vers}
     * @throws IllegalArgumentException When the provided range expression is invalid
     * @throws VersException            When the produced {@link Vers} is invalid
     * @throws InvalidVersionException  When any version in the range is invalid according to the inferred {@link VersioningScheme}
     * @see <a href="https://docs.github.com/en/rest/security-advisories/global-advisories?apiVersion=2022-11-28#get-a-global-security-advisory">GitHub Security Advisories API documentation</a>
     */
    public static Vers versFromGhsaRange(final String ecosystem, final String rangeExpr) {
        final var versBuilder = Vers.builder(schemeFromGhsaEcosystem(ecosystem));

        final String[] constraintExprs = rangeExpr.split(",");

        for (int i = 0; i < constraintExprs.length; i++) {
            final String constraintExpr = constraintExprs[i].trim();

            if (constraintExpr.startsWith("<=")) {
                versBuilder.withConstraint(Comparator.LESS_THAN_OR_EQUAL, constraintExpr.replaceFirst("<=", "").trim());
            } else if (constraintExpr.startsWith("<")) {
                versBuilder.withConstraint(Comparator.LESS_THAN, constraintExpr.replaceFirst("<", "").trim());
            } else if (constraintExpr.startsWith(">=")) {
                versBuilder.withConstraint(Comparator.GREATER_THAN_OR_EQUAL, constraintExpr.replaceFirst(">=", "").trim());
            } else if (constraintExpr.startsWith(">")) {
                versBuilder.withConstraint(Comparator.GREATER_THAN, constraintExpr.replaceFirst(">", "").trim());
            } else if (constraintExpr.startsWith("=")) {
                versBuilder.withConstraint(Comparator.EQUAL, constraintExpr.replaceFirst("=", "").trim());
            } else {
                throw new IllegalArgumentException("Invalid constraint \"%s\" at position %d".formatted(constraintExpr, i));
            }
        }

        return versBuilder.build();
    }

    /**
     * Convert a range type, ecosystem, and range events as used by OSV to a {@link Vers} range.
     *
     * @param type      The type of the range, must be either {@code ECOSYSTEM} or {@code SEMVER}
     * @param ecosystem The ecosystem of the affected package
     * @param events    The events in the range
     * @return The resulting {@link Vers}
     * @throws IllegalArgumentException When the provided range type is not support supported,
     *                                  or the provided {@code events} contains an invalid event
     * @throws VersException            When the produced {@link Vers} is invalid
     * @throws InvalidVersionException  When any version in the range is invalid according to the inferred {@link VersioningScheme}
     */
    public static Vers versFromOsvRange(final String type, final String ecosystem,
                                        final List<Map.Entry<String, String>> events, final String databaseSpecificLastKnownAffected) {
        if (!"ecosystem".equalsIgnoreCase(type) && !"semver".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("Range type \"%s\" is not supported".formatted(type));
        }

        final var scheme = schemeFromOsvEcosystem(ecosystem);
        final var versBuilder = Vers.builder(scheme);

        for (int i = 0; i < events.size(); i++) {
            final Map.Entry<String, String> event = events.get(i);

            final Comparator comparator = switch (event.getKey()) {
                case "introduced" -> Comparator.GREATER_THAN_OR_EQUAL;
                case "fixed", "limit" -> Comparator.LESS_THAN;
                case "last_affected" -> Comparator.LESS_THAN_OR_EQUAL;
                default -> throw new IllegalArgumentException("Invalid event \"%s\" at position %d"
                        .formatted(event.getKey(), i));
            };

            if (scheme == VersioningScheme.DEB
                    && (comparator == Comparator.LESS_THAN || comparator == Comparator.LESS_THAN_OR_EQUAL)
                    && Set.of("<end-of-life>", "<unfixed>").contains(event.getValue())) {
                // Some ranges in the Debian ecosystem use these special values for their upper bound,
                // to signal that all versions are affected. As they are not valid versions, we skip them.
                //
                // introduced=0, fixed=<unfixed> is equivalent to >=0.
                continue;
            }

            versBuilder.withConstraint(comparator, event.getValue());
        }

        if (databaseSpecificLastKnownAffected != null) {
            if (databaseSpecificLastKnownAffected.startsWith("<=")) {
                versBuilder.withConstraint(Comparator.LESS_THAN_OR_EQUAL, databaseSpecificLastKnownAffected.replaceFirst("<=", "").trim());
            } else if (databaseSpecificLastKnownAffected.startsWith("<")) {
                versBuilder.withConstraint(Comparator.LESS_THAN, databaseSpecificLastKnownAffected.replaceFirst("<", "").trim());
            }
        }

        return versBuilder.build();
    }

    /**
     * Convert ranges or exact version as used by NVD to a {@link Vers} range.
     *
     * @param versionStartExcluding   The versionStartExcluding in the range
     * @param versionStartIncluding   The versionStartIncluding in the range
     * @param versionEndExcluding   The versionEndExcluding in the range
     * @param versionEndIncluding   The versionEndIncluding in the range
     * @param exactVersion  The exact version in CpeMatch
     * @return The resulting {@link Vers}
     * @throws IllegalArgumentException When the provided cpe match is invalid,
     *                                  or the provided {@code events} contains an invalid event
     * @throws VersException            When the produced {@link Vers} is invalid
     * @throws InvalidVersionException  When any version in the range is invalid according to the inferred {@link VersioningScheme}
     */
    public static Vers versFromNvdRange(final String versionStartExcluding, final String versionStartIncluding,
                                        final String versionEndExcluding, final String versionEndIncluding,
                                        final String exactVersion) {

        // Using 'generic' as versioning scheme for NVD due to lack of package data.
        final var versBuilder = Vers.builder(VersioningScheme.GENERIC);

        if (trimToNull(versionStartExcluding) != null) {
            versBuilder.withConstraint(Comparator.GREATER_THAN, versionStartExcluding);
        }
        if (trimToNull(versionStartIncluding) != null) {
            versBuilder.withConstraint(Comparator.GREATER_THAN_OR_EQUAL, versionStartIncluding);
        }
        if (trimToNull(versionEndExcluding) != null) {
            versBuilder.withConstraint(Comparator.LESS_THAN, versionEndExcluding);
        }
        if (trimToNull(versionEndIncluding) != null) {
            versBuilder.withConstraint(Comparator.LESS_THAN_OR_EQUAL, versionEndIncluding);
        }
        // If CpeMatch does not define a version range, but the CPE itself can
        // still give us the information we need. The version field can either be:
        //   * an exact version (e.g. "1.0.0")
        //   * a wildcard matching all versions ("*")
        //   * a "not applicable", matching no version at all ("-")
        if (!versBuilder.hasConstraints() && exactVersion != null) {
            if (!"*".equals(exactVersion) && !"-".equals(exactVersion)) {
                // If we have neither upper, nor lower bound, and the CPE version
                // is not a wildcard, only a specific version is vulnerable.
                versBuilder.withConstraint(Comparator.EQUAL, exactVersion);
            } else if ("*".equals(exactVersion)) {
                // If we have neither upper, nor lower bound, and the CPE version
                // is a wildcard, all versions are vulnerable, and we can safely use a vers wildcard.
                versBuilder.withConstraint(Comparator.WILDCARD, null);
            }
        }
        return versBuilder.build();
    }

    static VersioningScheme schemeFromGhsaEcosystem(final String ecosystem) {
        // Can be one of: actions, composer, erlang, go, maven, npm, nuget, other, pip, pub, rubygems, rust.
        return switch (ecosystem.toLowerCase()) {
            case "go" -> VersioningScheme.GOLANG;
            case "maven" -> VersioningScheme.MAVEN;
            case "npm" -> VersioningScheme.NPM;
            case "nuget" -> VersioningScheme.NUGET;
            case "pip" -> VersioningScheme.PYPI;
            case "rubygems" -> VersioningScheme.GEM;
            default -> VersioningScheme.GENERIC;
        };
    }

    static VersioningScheme schemeFromOsvEcosystem(final String ecosystem) {
        // https://github.com/ossf/osv-schema/blob/main/docs/schema.md#affectedpackage-field
        return switch (ecosystem.toLowerCase()) {
            case "alpine" -> VersioningScheme.ALPINE;
            case "debian" -> VersioningScheme.DEB;
            case "go" -> VersioningScheme.GOLANG;
            case "maven" -> VersioningScheme.MAVEN;
            case "npm" -> VersioningScheme.NPM;
            case "nuget" -> VersioningScheme.NUGET;
            case "pypi" -> VersioningScheme.PYPI;
            case "rubygems" -> VersioningScheme.GEM;
            default -> VersioningScheme.GENERIC;
        };
    }

}
