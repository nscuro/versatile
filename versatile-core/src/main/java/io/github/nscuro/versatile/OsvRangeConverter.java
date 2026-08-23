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

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

import io.github.nscuro.versatile.spi.InvalidVersionException;
import io.github.nscuro.versatile.spi.Version;
import io.github.nscuro.versatile.version.KnownVersioningSchemes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

final class OsvRangeConverter {

    private static final Set<String> UNFIXED_SENTINEL_VERSIONS = Set.of("<end-of-life>", "<unfixed>");
    private static final String RANGE_EVENT_FIXED = "fixed";
    private static final String RANGE_EVENT_INTRODUCED = "introduced";
    private static final String RANGE_EVENT_LAST_AFFECTED = "last_affected";
    private static final String RANGE_EVENT_LIMIT = "limit";

    private OsvRangeConverter() {}

    static List<Vers> convert(
            String type,
            String ecosystem,
            List<Map.Entry<String, String>> events,
            @Nullable Map<String, Object> databaseSpecific) {
        if (!"ecosystem".equalsIgnoreCase(type) && !"semver".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("Range type \"%s\" is not supported".formatted(type));
        }

        // The suffix is not part of the ecosystem name, and thus must not end up in the scheme.
        final var scheme =
                VersUtils.schemeFromOsvEcosystem(ecosystem).orElseGet(() -> VersUtils.osvEcosystemNameOf(ecosystem));
        final List<Map.Entry<String, String>> versionEvents = withoutNonVersionEvents(events);

        try {
            return versIntervalsOf(versionEvents, scheme, databaseSpecific);
        } catch (InvalidVersionException e) {
            // Comparing versions as opaque strings is less precise than an ecosystem's
            // own rules, but losing the range entirely is worse.
            if (KnownVersioningSchemes.SCHEME_GENERIC.equals(scheme)) {
                throw e;
            }

            return versIntervalsOf(versionEvents, KnownVersioningSchemes.SCHEME_GENERIC, databaseSpecific);
        }
    }

    private static List<Vers> versIntervalsOf(
            List<Map.Entry<String, String>> versionEvents,
            String scheme,
            @Nullable Map<String, Object> databaseSpecific) {
        // Events are not guaranteed to be sorted, the OSV spec merely *recommends* it.
        // For reliable conversion, we need to sort ourselves.
        final List<Map.Entry<String, String>> sortedVersionEvents = sortByVersion(versionEvents, scheme);

        final List<Interval> intervals = intervalsOf(sortedVersionEvents, scheme);

        final var versList = new ArrayList<Vers>(intervals.size());
        for (int i = 0; i < intervals.size(); i++) {
            final Interval interval = intervals.get(i);
            final String lastKnownAffectedRange = i == intervals.size() - 1 && interval.upperBound() == null
                    ? lastKnownAffectedRangeOf(databaseSpecific)
                    : null;

            versList.add(versFromInterval(interval, scheme, lastKnownAffectedRange));
        }

        return versList;
    }

    private record Interval(Map.Entry<String, String> introduced, Map.@Nullable Entry<String, String> upperBound) {}

    private static List<Interval> intervalsOf(List<Map.Entry<String, String>> events, String scheme) {
        int highestLimitIndex = -1;
        for (int i = events.size() - 1; i >= 0; i--) {
            if (RANGE_EVENT_LIMIT.equals(events.get(i).getKey())) {
                highestLimitIndex = i;
                break;
            }
        }

        final var intervals = new ArrayList<Interval>();
        Map.Entry<String, String> introduced = null;

        for (int i = 0; i < events.size(); i++) {
            final Map.Entry<String, String> event = events.get(i);
            final String eventType = event.getKey();

            if (!isUpperBoundEvent(eventType)) {
                // An introduced event cannot widen an interval that already started lower.
                if (introduced == null) {
                    introduced = event;
                }
                continue;
            }

            // Only the highest limit closes the range.
            // All others are satisfied by any version below it.
            if (RANGE_EVENT_LIMIT.equals(eventType) && i != highestLimitIndex) {
                continue;
            }

            // Nothing is affected below the first introduced event.
            if (introduced != null) {
                // Skip empty intervals such as >=3|<3, they're meaningless.
                if (!isEmptyInterval(introduced, event, scheme)) {
                    intervals.add(new Interval(introduced, event));
                }
                introduced = null;
            }

            // Nothing at or above the highest limit is affected.
            if (i == highestLimitIndex) {
                return intervals;
            }
        }

        if (introduced != null) {
            intervals.add(new Interval(introduced, null));
        }

        return intervals;
    }

    private static boolean isEmptyInterval(
            Map.Entry<String, String> introduced, Map.Entry<String, String> upperBound, String scheme) {
        // last_affected is inclusive, so its own version stays affected.
        if (RANGE_EVENT_LAST_AFFECTED.equals(upperBound.getKey())) {
            return false;
        }

        final Version introducedVersion = versionOf(introduced, scheme);
        return introducedVersion != null
                && introducedVersion.compareTo(VersionFactory.forScheme(scheme, upperBound.getValue())) == 0;
    }

    private static Vers versFromInterval(Interval interval, String scheme, @Nullable String lastKnownAffectedRange) {
        final var versBuilder = Vers.builder(scheme);

        if (!isIntroducedZeroEvent(interval.introduced())) {
            // introduced=0 is OSV's special value for "before all versions",
            // see https://ossf.github.io/osv-schema/#special-values
            versBuilder.withConstraint(
                    Comparator.GREATER_THAN_OR_EQUAL, interval.introduced().getValue());
        }

        final Map.Entry<String, String> upperBound = interval.upperBound();
        if (upperBound != null) {
            versBuilder.withConstraint(
                    RANGE_EVENT_LAST_AFFECTED.equals(upperBound.getKey())
                            ? Comparator.LESS_THAN_OR_EQUAL
                            : Comparator.LESS_THAN,
                    upperBound.getValue());
        } else if (lastKnownAffectedRange != null) {
            if (lastKnownAffectedRange.startsWith("<=")) {
                versBuilder.withConstraint(
                        Comparator.LESS_THAN_OR_EQUAL,
                        lastKnownAffectedRange.replaceFirst("<=", "").trim());
            } else if (lastKnownAffectedRange.startsWith("<")) {
                versBuilder.withConstraint(
                        Comparator.LESS_THAN,
                        lastKnownAffectedRange.replaceFirst("<", "").trim());
            }
        }

        if (!versBuilder.hasConstraints()) {
            versBuilder.withConstraint(Comparator.WILDCARD, null);
        }

        return versBuilder.build();
    }

    private static List<Map.Entry<String, String>> withoutNonVersionEvents(List<Map.Entry<String, String>> events) {
        // A limit of "*" means infinity. Because a version only has to be below one limit
        // to pass the gate, it leaves every other limit in the range without effect.
        final boolean hasWildcardLimit = events.stream()
                .anyMatch(event -> RANGE_EVENT_LIMIT.equals(event.getKey()) && "*".equals(event.getValue()));

        final var retainedEvents = new ArrayList<Map.Entry<String, String>>(events.size());

        for (int i = 0; i < events.size(); i++) {
            final Map.Entry<String, String> event = events.get(i);
            final String eventType = event.getKey();
            final String eventVersion = event.getValue();

            if (!RANGE_EVENT_INTRODUCED.equals(eventType) && !isUpperBoundEvent(eventType)) {
                throw new IllegalArgumentException("Invalid event \"%s\" at position %d".formatted(eventType, i));
            }

            if (hasWildcardLimit && RANGE_EVENT_LIMIT.equals(eventType)) {
                continue;
            }

            // Debian ranges use these to signal that no fix is available, leaving the interval
            // open. They are not versions in any ecosystem, so the scheme does not matter.
            if (isUpperBoundEvent(eventType) && UNFIXED_SENTINEL_VERSIONS.contains(eventVersion)) {
                continue;
            }

            retainedEvents.add(event);
        }

        return retainedEvents;
    }

    private static List<Map.Entry<String, String>> sortByVersion(
            List<Map.Entry<String, String>> events, String scheme) {
        final var sortedEvents = new ArrayList<>(events);
        sortedEvents.sort(comparing(event -> versionOf(event, scheme), nullsFirst(naturalOrder())));
        return sortedEvents;
    }

    private static @Nullable Version versionOf(Map.Entry<String, String> event, String scheme) {
        // introduced=0 sorts before every other version,
        // and may not even be a valid version for the scheme.
        return isIntroducedZeroEvent(event) ? null : VersionFactory.forScheme(scheme, event.getValue());
    }

    private static boolean isUpperBoundEvent(String eventKey) {
        return RANGE_EVENT_FIXED.equals(eventKey)
                || RANGE_EVENT_LIMIT.equals(eventKey)
                || RANGE_EVENT_LAST_AFFECTED.equals(eventKey);
    }

    private static boolean isIntroducedZeroEvent(Map.Entry<String, String> event) {
        return RANGE_EVENT_INTRODUCED.equals(event.getKey()) && "0".equals(event.getValue());
    }

    private static @Nullable String lastKnownAffectedRangeOf(@Nullable Map<String, Object> databaseSpecific) {
        if (databaseSpecific != null
                && databaseSpecific.get("last_known_affected_version_range")
                        instanceof final String lastKnownAffectedRange) {
            return lastKnownAffectedRange;
        }

        return null;
    }
}
