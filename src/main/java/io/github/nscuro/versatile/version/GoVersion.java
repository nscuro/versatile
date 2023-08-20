package io.github.nscuro.versatile.version;

import java.util.Map;

/**
 * @see <a href="https://github.com/golang/mod/blob/v0.12.0/semver/semver.go">Go Modules semantic version implementation</a>
 */
public class GoVersion extends Version {

    private final String major;
    private final String minor;
    private final String patch;
    private final String shortV;
    private final String prerelease;
    private final String build;

    GoVersion(final String versionStr) {
        super(VersioningScheme.GOLANG, versionStr);

        if (!versionStr.startsWith("v")) {
            throw new InvalidVersionException("");
        }

        Map.Entry<String, String> partAndRest = parseInt(versionStr.substring(1));
        if (partAndRest == null) {
            throw new InvalidVersionException("");
        }

        this.major = partAndRest.getKey();
        if (partAndRest.getValue().isEmpty()) {
            this.minor = "0";
            this.patch = "0";
            this.shortV = ".0.0";
            this.prerelease = null;
            this.build = null;
            return;
        }

        if (partAndRest.getValue().charAt(0) != '.') {
            throw new InvalidVersionException("");
        }

        partAndRest = parseInt(partAndRest.getValue().substring(1));
        if (partAndRest == null) {
            throw new InvalidVersionException("");
        }

        this.minor = partAndRest.getKey();
        if (partAndRest.getValue().isEmpty()) {
            this.patch = "0";
            this.shortV = ".0";
            this.prerelease = null;
            this.build = null;
            return;
        }

        partAndRest = parseInt(partAndRest.getValue().substring(1));
        if (partAndRest == null) {
            throw new InvalidVersionException("");
        }

        this.patch = partAndRest.getKey();
        if (!partAndRest.getValue().isEmpty() && partAndRest.getValue().charAt(0) == '-') {
            partAndRest = parsePrerelease(partAndRest.getValue());
            if (partAndRest == null) {
                throw new InvalidVersionException("");
            }

            this.prerelease = partAndRest.getKey();
        } else {
            this.prerelease = null;
        }

        if (!partAndRest.getValue().isEmpty() && partAndRest.getValue().charAt(0) == '+') {
            partAndRest = parsePrerelease(partAndRest.getValue());
            if (partAndRest == null) {
                throw new InvalidVersionException("");
            }

            this.build = partAndRest.getKey();
        } else {
            this.build = null;
        }

        if (!partAndRest.getValue().isEmpty()) {
            throw new InvalidVersionException("");
        }

        this.shortV = null;
    }

    @Override
    public int compareTo(final Version other) {
        if (other instanceof final GoVersion otherVersion) {
            int comparisonResult = Integer.compare(Integer.parseInt(this.major), Integer.parseInt(otherVersion.major));
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            comparisonResult = Integer.compare(Integer.parseInt(this.minor), Integer.parseInt(otherVersion.minor));
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            return Integer.compare(Integer.parseInt(this.patch), Integer.parseInt(otherVersion.patch));
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(GenericVersion.class.getSimpleName(), other.getClass().getSimpleName()));
    }

    private static Map.Entry<String, String> parseInt(final String version) {
        if (!Character.isDigit(version.charAt(0))) {
            return null;
        }

        int i = 1;
        while (i < version.length() && Character.isDigit(version.charAt(i))) {
            i++;
        }

        if (version.charAt(0) == '0' && i != 1) {
            return null;
        }

        return Map.entry(version.substring(0, i), version.substring(i));
    }

    private static Map.Entry<String, String> parsePrerelease(final String version) {
        // TODO
        return null;
    }

    private static Map.Entry<String, String> parseBuild(final String version) {
        // TODO
        return null;
    }

    private int comparePrerelease(final String x, final String y) {
        // TODO
        return 0;
    }

}
