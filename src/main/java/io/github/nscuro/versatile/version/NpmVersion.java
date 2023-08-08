package io.github.nscuro.versatile.version;

import com.vdurmont.semver4j.Semver;
import io.github.nscuro.versatile.VersioningScheme;

public class NpmVersion extends Version {

    private final Semver delegate;

    NpmVersion(final String versionStr) {
        super(VersioningScheme.NPM, versionStr);
        this.delegate = new Semver(versionStr, Semver.SemverType.NPM);
    }

    @Override
    public int compareTo(final Version other) {
        if (other instanceof final NpmVersion otherVersion) {
            return this.delegate.compareTo(otherVersion.delegate);
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(GenericVersion.class.getSimpleName(), other.getClass().getSimpleName()));
    }

}