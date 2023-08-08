package io.github.nscuro.versatile.version;

import io.github.nscuro.versatile.VersioningScheme;

import java.util.Objects;

public abstract class Version implements Comparable<Version> {

    private final VersioningScheme scheme;
    private final String versionStr;

    Version(final VersioningScheme scheme, final String versionStr) {
        this.scheme = scheme;
        this.versionStr = versionStr;
    }

    public static Version forScheme(final VersioningScheme scheme, final String versionStr) {
        return switch (scheme) {
            case MAVEN -> new MavenVersion(versionStr);
            case NPM -> new NpmVersion(versionStr);
            default -> new GenericVersion(versionStr);
        };
    }

    @Override
    public boolean equals(final Object obj) {
        return Objects.equals(obj, versionStr);
    }

    public VersioningScheme scheme() {
        return scheme;
    }

    @Override
    public String toString() {
        return versionStr;
    }

}
