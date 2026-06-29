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

import io.github.nscuro.versatile.spi.Version;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public class Constraint implements Comparable<Constraint> {

    private final String scheme;
    private final Comparator comparator;
    private final @Nullable Version version;

    Constraint(String scheme, Comparator comparator, @Nullable Version version) {
        requireNonNull(scheme, "scheme must not be null");
        requireNonNull(comparator, "comparator must not be null");

        if (comparator == Comparator.WILDCARD && version != null) {
            throw new VersException("comparator %s is not allowed with version".formatted(comparator));
        } else if (comparator != Comparator.WILDCARD && version == null) {
            throw new VersException("comparator %s is not allowed without version".formatted(comparator));
        }
        this.scheme = scheme;
        this.comparator = comparator;
        this.version = version;
    }

    static Constraint parse(String scheme, String constraintStr) {
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

        final String versionStr = constraintStr
                .replaceFirst("^" + Pattern.quote(comparator.operator()), "")
                .trim();
        if (versionStr.isBlank()) {
            throw new VersException("comparator %s is not allowed without version".formatted(comparator));
        }

        final Version version = VersionFactory.forScheme(scheme, maybeUrlDecode(versionStr));

        return new Constraint(scheme, comparator, version);
    }

    boolean matches(Version version) {
        requireNonNull(version, "version must not be null");

        if (!Objects.equals(this.scheme, version.scheme())) {
            throw new VersException("cannot evaluate constraint of scheme %s against version of scheme %s"
                    .formatted(this.scheme, version.scheme()));
        }

        return switch (comparator) {
            case LESS_THAN -> requireNonNull(this.version).compareTo(version) > 0;
            case LESS_THAN_OR_EQUAL -> requireNonNull(this.version).compareTo(version) >= 0;
            case GREATER_THAN_OR_EQUAL -> requireNonNull(this.version).compareTo(version) <= 0;
            case GREATER_THAN -> requireNonNull(this.version).compareTo(version) < 0;
            case EQUAL -> requireNonNull(this.version).compareTo(version) == 0;
            case NOT_EQUAL -> requireNonNull(this.version).compareTo(version) != 0;
            case WILDCARD -> true;
        };
    }

    /**
     * Inverts the comparator of the constraint e.g. {@code < 1.3} becomes {@code >= 1.3}
     *
     * @return a new inverted constraint and null if current comparator is a wildcard: *
     */
    @Nullable
    Constraint invert() {
        return switch (comparator) {
            case LESS_THAN -> new Constraint(scheme, Comparator.GREATER_THAN_OR_EQUAL, version);
            case LESS_THAN_OR_EQUAL -> new Constraint(scheme, Comparator.GREATER_THAN, version);
            case GREATER_THAN_OR_EQUAL -> new Constraint(scheme, Comparator.LESS_THAN, version);
            case GREATER_THAN -> new Constraint(scheme, Comparator.LESS_THAN_OR_EQUAL, version);
            case EQUAL -> new Constraint(scheme, Comparator.NOT_EQUAL, version);
            case NOT_EQUAL -> new Constraint(scheme, Comparator.EQUAL, version);
            case WILDCARD -> null;
        };
    }

    private static String maybeUrlDecode(String version) {
        if (version.contains("%")) {
            return URLDecoder.decode(version, StandardCharsets.UTF_8);
        }

        return version;
    }

    @Override
    public int compareTo(Constraint other) {
        // NB: Only a wildcard constraint has no version.
        // A wildcard is valid only as the sole constraint of a vers range,
        // so this branch is never reached for a valid vers.
        if (this.version == null || other.version == null) {
            return Boolean.compare(this.version != null, other.version != null);
        }

        return this.version.compareTo(other.version);
    }

    public String scheme() {
        return scheme;
    }

    public Comparator comparator() {
        return comparator;
    }

    public @Nullable Version version() {
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
            return URLEncoder.encode(requireNonNull(version).toString(), StandardCharsets.UTF_8);
        }

        return comparator.operator() + URLEncoder.encode(requireNonNull(version).toString(), StandardCharsets.UTF_8);
    }
}
