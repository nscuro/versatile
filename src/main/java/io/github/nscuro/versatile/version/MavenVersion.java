package io.github.nscuro.versatile.version;

import io.github.nscuro.versatile.VersioningScheme;
import org.apache.maven.artifact.versioning.ComparableVersion;

class MavenVersion extends Version {

    private final ComparableVersion delegate;

    MavenVersion(final String versionStr) {
        super(VersioningScheme.MAVEN, versionStr);
        this.delegate = new ComparableVersion(versionStr);
    }

    @Override
    public int compareTo(final Version other) {
        if (other instanceof final MavenVersion otherVersion) {
            return this.delegate.compareTo(otherVersion.delegate);
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(GenericVersion.class.getSimpleName(), other.getClass().getSimpleName()));
    }

}
