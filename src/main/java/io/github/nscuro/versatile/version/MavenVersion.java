package io.github.nscuro.versatile.version;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class MavenVersion extends Version {

    private final ArtifactVersion delegate;

    public MavenVersion(final String versionStr) {
        super(VersioningScheme.MAVEN, versionStr);
        this.delegate = new DefaultArtifactVersion(versionStr);
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
