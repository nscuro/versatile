package io.github.nscuro.versatile.version;

import io.github.nscuro.versatile.ComponentVersion;
import io.github.nscuro.versatile.VersioningScheme;

class GenericVersion extends Version {

    private final ComponentVersion delegate;

    GenericVersion(final String versionStr) {
        super(VersioningScheme.GENERIC, versionStr);
        this.delegate = new ComponentVersion(versionStr);
    }

    @Override
    public int compareTo(final Version other) {
        if (other instanceof final GenericVersion otherVersion) {
            return this.delegate.compareTo(otherVersion.delegate);
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(GenericVersion.class.getSimpleName(), other.getClass().getSimpleName()));
    }

}