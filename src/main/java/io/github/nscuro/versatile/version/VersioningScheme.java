package io.github.nscuro.versatile.version;

import com.github.packageurl.PackageURL;

/**
 * @see <a href="https://github.com/package-url/purl-spec/blob/version-range-spec/VERSION-RANGE-SPEC.rst#some-of-the-known-versioning-schemes">Known versioning schemes</a>
 */
public enum VersioningScheme {

    ALPINE,

    CPAN,

    DEB,

    GEM,

    GENERIC,

    GENTOO,

    GOLANG,

    MAVEN,

    NPM,

    NUGET,

    PYPI,

    RPM;

    public static VersioningScheme fromPurlType(final String purlType) {
        return switch (purlType) {
            case PackageURL.StandardTypes.DEBIAN -> DEB;
            case PackageURL.StandardTypes.GEM -> GEM;
            case PackageURL.StandardTypes.GOLANG -> GOLANG;
            case PackageURL.StandardTypes.MAVEN -> MAVEN;
            case PackageURL.StandardTypes.NPM -> NPM;
            case PackageURL.StandardTypes.NUGET -> NUGET;
            case PackageURL.StandardTypes.PYPI -> PYPI;
            case PackageURL.StandardTypes.RPM -> RPM;
            default -> GENERIC;
        };
    }

}
