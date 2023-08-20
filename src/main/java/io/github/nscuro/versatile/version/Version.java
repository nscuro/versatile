package io.github.nscuro.versatile.version;

import java.util.Objects;

public abstract class Version implements Comparable<Version> {

    private final VersioningScheme scheme;
    final String versionStr;

    Version(final VersioningScheme scheme, final String versionStr) {
        this.scheme = scheme;
        this.versionStr = versionStr;
    }

    public static Version forScheme(final VersioningScheme scheme, final String versionStr) {
        // TODO: Would be nice to offer some sort of registry that library users can hook their
        //   own Version implementations into, and even override default implementations.
        return switch (scheme) {
            case DEB -> new DebianVersion(versionStr);
            case GOLANG -> new GoVersion(versionStr);
            case MAVEN -> new MavenVersion(versionStr);
            case NPM -> new NpmVersion(versionStr);
            default -> new GenericVersion(versionStr);
        };
    }

    /**
     * Determines whether the {@link Version} is considered stable.
     *
     * @return {@code true} when stable, otherwise {@code false}
     */
    public abstract boolean isStable();

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
