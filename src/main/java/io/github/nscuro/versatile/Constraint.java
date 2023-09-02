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

import io.github.nscuro.versatile.version.Version;
import io.github.nscuro.versatile.version.VersioningScheme;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Constraint implements Comparable<Constraint> {

    private final VersioningScheme scheme;
    private final Comparator comparator;
    private final Version version;

    Constraint(final VersioningScheme scheme, final Comparator comparator, final Version version) {
        if (scheme == null) {
            throw new VersException("scheme must not be null");
        }
        if (comparator == null) {
            throw new VersException("comparator must not be null");
        }
        if (comparator == Comparator.WILDCARD && version != null) {
            throw new VersException("comparator %s is not allowed with version".formatted(comparator));
        } else if (comparator != Comparator.WILDCARD && version == null) {
            throw new VersException("comparator %s is not allowed without version".formatted(comparator));
        }
        this.scheme = scheme;
        this.comparator = comparator;
        this.version = version;
    }

    static Constraint parse(final VersioningScheme scheme, final String constraintStr) {
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

        final String versionStr = constraintStr.replaceFirst("^" + Pattern.quote(comparator.operator()), "").trim();
        if (versionStr.isBlank()) {
            throw new VersException("comparator %s is not allowed without version".formatted(comparator));
        }

        final Version version = Version.forScheme(scheme, maybeUrlDecode(versionStr));

        return new Constraint(scheme, comparator, version);
    }

    boolean matches(final Version version) {
        if (version == null) {
            throw new VersException("version must not be null");
        }
        if (this.scheme != version.scheme()) {
            throw new VersException("cannot evaluate constraint of scheme %s against version of scheme %s"
                    .formatted(this.scheme, version.scheme()));
        }

        final int comparisonResult = this.version.compareTo(version);
        return switch (comparator) {
            case LESS_THAN -> comparisonResult < 0;
            case LESS_THAN_OR_EQUAL -> comparisonResult <= 0;
            case GREATER_THAN_OR_EQUAL -> comparisonResult >= 0;
            case GREATER_THAN -> comparisonResult > 0;
            case EQUAL -> comparisonResult == 0;
            case NOT_EQUAL -> comparisonResult != 0;
            case WILDCARD -> true;
        };
    }

    private static String maybeUrlDecode(final String version) {
        if (version.contains("%")) {
            return URLDecoder.decode(version, StandardCharsets.UTF_8);
        }

        return version;
    }

    @Override
    public int compareTo(final Constraint other) {
        return this.version.compareTo(other.version);
    }

    public VersioningScheme scheme() {
        return scheme;
    }

    public Comparator comparator() {
        return comparator;
    }

    public Version version() {
        return version;
    }

    @Override
    public String toString() {
        if (comparator == Comparator.WILDCARD) {
            // Wildcard cannot have a version.
            return Comparator.WILDCARD.operator();
        }

        if (comparator == Comparator.EQUAL) {
            // Operator is omitted for equality.
            return URLEncoder.encode(version().toString(), StandardCharsets.UTF_8);
        }

        return comparator.operator() + URLEncoder.encode(version.toString(), StandardCharsets.UTF_8);
    }

}
