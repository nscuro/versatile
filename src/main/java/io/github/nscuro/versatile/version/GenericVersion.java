package io.github.nscuro.versatile.version;

public class GenericVersion extends Version {

    private final ComponentVersion delegate;

    public GenericVersion(final String versionStr) {
        super(VersioningScheme.GENERIC, versionStr);
        this.delegate = new ComponentVersion(versionStr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStable() {
        // TODO: Looks for some common pre-release qualifiers.
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Version other) {
        if (other instanceof final GenericVersion otherVersion) {
            return this.delegate.compareTo(otherVersion.delegate);
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(GenericVersion.class.getSimpleName(), other.getClass().getSimpleName()));
    }

}
