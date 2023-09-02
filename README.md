# versatile

[![CI](https://github.com/nscuro/versatile/actions/workflows/ci.yml/badge.svg)](https://github.com/nscuro/versatile/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.nscuro/versatile/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.nscuro/versatile)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Java implementation of [vers](https://github.com/package-url/purl-spec/blob/version-range-spec/VERSION-RANGE-SPEC.rst),
*a mostly universal version range specifier*

## Introduction

### Supported Versioning Schemes

| Scheme   | Supported |
|:---------|:---------:|
| Alpine   |     ❌     |
| CPAN     |     ❌     |
| Debian   |     ✅     |
| Ruby Gem |     ❌     |
| Generic  |     ✅     |
| Gentoo   |     ❌     |
| Go       |     ❌     |
| Maven    |     ✅     |
| NPM      |     ✅     |
| Nuget    |     ❌     |
| PyPI     |     ❌     |
| RPM      |     ❌     |

> **Note**  
> Versions for which the appropriate scheme is not currently supported will fall back to `generic`

## Usage

### Installation

```xml

<dependency>
    <groupId>io.github.nscuro</groupId>
    <artifactId>versatile</artifactId>
    <version>${versatile.version}</version>
</dependency>
```

### Constructing `vers` Ranges

Ranges are constructed using a builder. Builders must be initialized with a `VersioningScheme`.
`Constraint`s may be provided in structured, or freeform format. Versions used in constraints must
be valid according to the chosen `VersioningScheme`. When `VersBuilder#build` is called, constraints are sorted
by version, and the built `Vers` is validated. If the range turns out to be invalid, a `VersException` is thrown.

```java
import io.github.nscuro.versatile.Comparator;
import io.github.nscuro.versatile.Vers;
import io.github.nscuro.versatile.version.VersioningScheme;

class ConstructVers {

    void shouldConstructVers() {
        Vers vers = Vers.builder(VersioningScheme.GOLANG)
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
import io.github.nscuro.versatile.version.VersioningScheme;

class ParseVers {

    void shouldParseVers() {
        Vers vers = Vers.parse("vers:golang/>v1.2.3|!=v2.1.3|<=v3.2.1");

        assert vers.scheme() == VersioningScheme.GOLANG;
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
to the range's `VersioningScheme`.

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
