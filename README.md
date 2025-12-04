# versatile

[![CI](https://github.com/nscuro/versatile/actions/workflows/ci.yml/badge.svg)](https://github.com/nscuro/versatile/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.nscuro/versatile-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.nscuro/versatile-core)
[![javadoc](https://javadoc.io/badge2/io.github.nscuro/versatile-core/javadoc.svg)](https://javadoc.io/doc/io.github.nscuro/versatile-core)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Java implementation of [vers](https://github.com/package-url/purl-spec/blob/version-range-spec/VERSION-RANGE-SPEC.rst),
*a mostly universal version range specifier*

## Introduction

### Supported Versioning Schemes

| Scheme   | Supported |
|:---------|:---------:|
| Alpine   |     ✅     |
| CPAN     |     ❌     |
| Debian   |     ✅     |
| Ruby Gem |     ❌     |
| Generic  |     ✅     |
| Gentoo   |     ❌     |
| Go       |     ✅     |
| Maven    |     ✅     |
| NPM      |     ✅     |
| Nuget    |     ❌     |
| PyPI     |     ✅     |
| RPM      |     ✅     |

> [!NOTE]
> Support for new schemes can be added, and default implementations overwritten, by [extending *versatile*](#extending-versatile)!
> Versions for which the appropriate scheme is not currently supported will fall back to `generic`.

## Usage

### Installation

```xml
<dependency>
    <groupId>io.github.nscuro</groupId>
    <artifactId>versatile-core</artifactId>
    <version>${versatile.version}</version>
</dependency>
```

> [!NOTE]
> *versatile* requires Java >= 21.

### Constructing `vers` Ranges

Ranges are constructed using a builder. Builders must be initialized with a versioning scheme.
`Constraint`s may be provided in structured, or freeform format. Versions used in constraints must
be valid according to the chosen versioning scheme. When `VersBuilder#build` is called, constraints are sorted
by version, and the built `Vers` is validated. If the range turns out to be invalid, an `VersException` is thrown.

```java
import io.github.nscuro.versatile.Comparator;
import io.github.nscuro.versatile.Vers;

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_GOLANG;

class ConstructVers {

    void shouldConstructVers() {
        Vers vers = Vers.builder(SCHEME_GOLANG)
                .withConstraint(Comparator.GREATER_THAN, "v1.2.3")
                .withConstraint(Comparator.LESS_THAN_OR_EQUAL, "v3.2.1")
                .withConstraint("!= v2.1.3")
                .build();

        assert "vers:golang/>v1.2.3|!=v2.1.3|<=v3.2.1".equals(vers.toString());
    }

}
```

### Parsing `vers` Ranges

`vers` ranges may
be [parsed](https://github.com/package-url/purl-spec/blob/version-range-spec/VERSION-RANGE-SPEC.rst#parsing-and-validating-version-range-specifiers)
using the `Vers#parse` method. If the range turns out to be invalid, a `VersException` is thrown.

```java
import io.github.nscuro.versatile.Vers;

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_GOLANG;

class ParseVers {

    void shouldParseVers() {
        Vers vers = Vers.parse("vers:golang/>v1.2.3|!=v2.1.3|<=v3.2.1");

        assert SCHEME_GOLANG.equals(vers.scheme());
        assert vers.constraints().size() == 3;
    }

}
```

### Simplifying `vers` Ranges

The `vers` specification defines an algorithm
to [simplify](https://github.com/package-url/purl-spec/blob/version-range-spec/VERSION-RANGE-SPEC.rst#version-constraints-simplification)
constraints in a range. This mechanism is exposed through the `Vers#simplify` method.

```java
import io.github.nscuro.versatile.Vers;

class SimplifyVers {

    void shouldSimplify() {
        Vers vers = Vers.parse("vers:golang/>v0.0.0|>=v0.0.1|v0.0.2|<v0.0.3|v0.0.4|<v0.0.5|>=v0.0.6");

        assert "vers:golang/>v0.0.0|<v0.0.5|>=v0.0.6".equals(vers.simplify().toString());
    }

}
```

> **Note**  
> *versatile* will never simplify ranges on its own. If simplification is desired,
> `simplify` must be called explicitly.

### Checking if a Range Contains a Version

To check whether a given `vers`
range [contains](https://github.com/package-url/purl-spec/blob/version-range-spec/VERSION-RANGE-SPEC.rst#checking-if-a-version-is-contained-within-a-range)
a specific version, the `Vers#contains` method may be used. The provided version must be valid according
to the range's versioning scheme.

```java
import io.github.nscuro.versatile.Vers;

class VersContains {

    Vers vers = Vers.parse("vers:golang/>v1.2.3|!=v2.1.3|<=v3.2.1");

    void shouldContainVersion() {
        assert vers.contains("v1.2.4");
        assert vers.contains("v2.0.2");
        assert vers.contains("v3.2.1");
    }

    void shouldNotContainVersion() {
        assert !vers.contains("v1.2.3");
        assert !vers.contains("v2.1.3");
        assert !vers.contains("v3.2.2");
    }

}
```

### Working With Versions

Versions can be used directly, outside the context of a `vers` range. To acquire a `Version` object,
`VersionFactory` may be used:

```java
import io.github.nscuro.versatile.VersionFactory;
import io.github.nscuro.versatile.version.GoVersion;
import io.github.nscuro.versatile.version.Version;

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_GOLANG;

class VersContains {

    void shouldReturnGoVersion() {
        Version version = VersionFactory.getVersion(SCHEME_GOLANG, "v1.2.4");
        assert version instanceof GoVersion;
        assert "golang".equals(version.scheme());
    }

    void shouldFallbackToGenericVersion() {
        Version version = VersionFactory.getVersion("foobar", "v1.2.4");
        assert version instanceof GenericVersion;
        assert "foobar".equals(version.scheme());
    }

}
```

As shown above, if *versatile* doesn't recognize the provided versioning scheme, it will fall back
to `GenericVersion`. Support for additional schemes can be added by [extending *versatile*](#extending-versatile).

### Extending *versatile*

#### Versioning Schemes

*versatile* ships with support for a few versioning schemes (see [Supported Versioning Schemes](#supported-versioning-schemes)).

While contributions to add support for more schemes is highly appreciated, it's not always feasible to wait for changes
to be released. It should be possible to leverage *versatile*'s `vers` functionality, without being reliant on
how fast support for new schemes is added upstream.

On the other hand, the default implementations may not always align with the desired behavior. Perhaps they are too strict,
and a more lax parsing logic is required. In that case, the default will need to be overwritten.

To address these concerns, *versatile* exposes an [SPI](https://docs.oracle.com/javase%2Ftutorial%2F/sound/SPI-intro.html) 
for versioning scheme support.

To add support for a new scheme, let's say `alpine`, the following steps may be performed:

1. Add either `versatile-core`, or `versatile-spi` as dependency to your project
2. Create a class `AlpineVersion` that extends `io.github.nscuro.versatile.spi.Version`
3. Implement the version parsing logic as desired
    * Be sure to overwrite `compareTo`, `equals`, `hashCode`, and `toString`
4. Create a class `AlpineVersionProvider` that implements `io.github.nscuro.versatile.spi.VersionProvider`
    * Implement `#supportsScheme(String scheme)` to return `true` for the `alpine` scheme
    * Implement `#getVersion(String scheme, String verstionStr)` to return an instance of `AlpineVersion`
    * Implement `#priority()` to return a value between `0` (lowest), and `Integer.MAX_VALUE` (highest)
        * Built-in providers have a priority of `50` (`VersionProvider#PRIORITY_BUILTIN`)
        * By defining a priority higher than `50`, built-in providers can effectively be overwritten
5. Create a file `src/main/resources/META-INF/services/io.github.nscuro.versatile.spi.VersionProvider`
6. List the fully qualified package name of all custom `VersionProvider` implementations, one per line
    * e.g. `com.acme.AlpineVersionProvider`

That's it! Now, whenever *versatile* encounters a `vers` range with the scheme `alpine`, it will use your `AlpineVersion`!