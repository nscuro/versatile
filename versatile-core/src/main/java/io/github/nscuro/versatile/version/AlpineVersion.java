/*
 * This file is part of versatile.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Niklas Düster. All Rights Reserved.
 */
package io.github.nscuro.versatile.version;

import io.github.nscuro.versatile.spi.InvalidVersionException;
import io.github.nscuro.versatile.spi.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.nscuro.versatile.version.KnownVersioningSchemes.SCHEME_ALPINE;

/**
 * @see <a href="https://github.com/alpinelinux/apk-tools/blob/master/src/version.c">Alpine version comparison implementation</a>
 * @since 0.14.0
 */
public class AlpineVersion extends Version {

    public static class Provider extends AbstractBuiltinVersionProvider {

        public Provider() {
            super(Set.of(SCHEME_ALPINE), (scheme, versionStr) -> new AlpineVersion(versionStr));
        }

    }

    private record Token(Type type, String value) {

        private enum Type {

            COMMIT_HASH,
            DIGIT,
            LETTER,
            REVISION,
            SUFFIX_ALPHA,
            SUFFIX_BETA,
            SUFFIX_CVS,
            SUFFIX_GIT,
            SUFFIX_HG,
            SUFFIX_P,
            SUFFIX_PRE,
            SUFFIX_RC,
            SUFFIX_SVN;

            static Type ofSuffix(final String suffix) {
                return switch (suffix) {
                    case "alpha" -> Type.SUFFIX_ALPHA;
                    case "beta" -> Type.SUFFIX_BETA;
                    case "cvs" -> Type.SUFFIX_CVS;
                    case "git" -> Type.SUFFIX_GIT;
                    case "hg" -> Type.SUFFIX_HG;
                    case "p" -> Type.SUFFIX_P;
                    case "pre" -> Type.SUFFIX_PRE;
                    case "rc" -> Type.SUFFIX_RC;
                    case "svn" -> Type.SUFFIX_SVN;
                    default -> throw new IllegalArgumentException("Unknown suffix: " + suffix);
                };
            }

            boolean isPreReleaseSuffix() {
                return equals(SUFFIX_ALPHA)
                        || equals(SUFFIX_BETA)
                        || equals(SUFFIX_PRE)
                        || equals(SUFFIX_RC);
            }

        }

        boolean hasLeadingZero() {
            return value.length() > 1 && value.startsWith("0");
        }

    }

    private static final Pattern TOKEN_PATTERN = Pattern.compile("""
            (?<digit>\\d+)|\
            (?<letter>[a-z])|\
            (?<suffix>_(?:alpha|beta|pre|rc|cvs|svn|git|hg|p))|\
            (?<commit>~[0-9a-f]+)|\
            (?<revision>-r\\d+)|\
            (?<dot>\\.)|\
            (?<other>.)""");

    private final List<Token> tokens;

    AlpineVersion(final String versionStr) {
        super(SCHEME_ALPINE, versionStr);

        this.tokens = parseVersion(versionStr);
        if (this.tokens.isEmpty()) {
            throw new InvalidVersionException(versionStr, "Failed to parse Alpine version: " + versionStr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStable() {
        return tokens.stream().map(Token::type).noneMatch(Token.Type::isPreReleaseSuffix);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Version other) {
        if (other instanceof final AlpineVersion otherVersion) {
            return compareTokens(this.tokens, otherVersion.tokens);
        }

        throw new IllegalArgumentException("%s can only be compared with its own type, but got %s"
                .formatted(this.getClass().getName(), other.getClass().getName()));
    }

    private static List<Token> parseVersion(final String versionStr) {
        final var tokens = new ArrayList<Token>();
        final Matcher matcher = TOKEN_PATTERN.matcher(versionStr);

        while (matcher.find()) {
            if (matcher.group("digit") != null) {
                tokens.add(new Token(Token.Type.DIGIT, matcher.group("digit")));
            } else if (matcher.group("letter") != null) {
                tokens.add(new Token(Token.Type.LETTER, matcher.group("letter")));
            } else if (matcher.group("suffix") != null) {
                final String suffix = matcher.group("suffix").substring(1); // Remove leading underscore.
                tokens.add(new Token(Token.Type.ofSuffix(suffix), suffix));
            } else if (matcher.group("commit") != null) {
                tokens.add(new Token(Token.Type.COMMIT_HASH, matcher.group("commit").substring(1)));
            } else if (matcher.group("revision") != null) {
                final String revision = matcher.group("revision").substring(2); // Remove "-r" prefix.
                tokens.add(new Token(Token.Type.REVISION, revision));
            }
        }

        return tokens;
    }

    private static int compareTokens(final List<Token> tokensA, final List<Token> tokensB) {
        final int maxLength = Math.max(tokensA.size(), tokensB.size());

        for (int i = 0; i < maxLength; i++) {
            if (i >= tokensA.size() && i >= tokensB.size()) {
                return 0;
            }

            if (i >= tokensA.size()) {
                final Token tokenB = tokensB.get(i);
                if (tokenB.type().isPreReleaseSuffix()) {
                    return 1;
                }

                return -1;
            }

            if (i >= tokensB.size()) {
                final Token tokenA = tokensA.get(i);
                if (tokenA.type().isPreReleaseSuffix()) {
                    return -1;
                }

                return 1;
            }

            final Token tokenA = tokensA.get(i);
            final Token tokenB = tokensB.get(i);

            if (tokenA.type() != tokenB.type()) {
                if (tokenA.type().isPreReleaseSuffix() && !tokenB.type().isPreReleaseSuffix()) {
                    return -1;
                }
                if (tokenB.type().isPreReleaseSuffix() && !tokenA.type().isPreReleaseSuffix()) {
                    return 1;
                }

                return Integer.compare(tokenA.type().ordinal(), tokenB.type().ordinal());
            }

            final int comparisonResult = compareTokenValues(tokenA, tokenB);
            if (comparisonResult != 0) {
                return comparisonResult;
            }
        }

        return 0;
    }

    // https://github.com/alpinelinux/apk-tools/blob/982c9961ad9e71b4068911329c9d8121cedfd9f7/src/version.c#L100
    private static int compareTokenValues(final Token tokenA, final Token tokenB) {
        return switch (tokenA.type()) {
            case COMMIT_HASH -> tokenA.value().compareTo(tokenB.value());
            case DIGIT -> {
                if (tokenA.hasLeadingZero() || tokenB.hasLeadingZero()) {
                    yield tokenA.value().compareTo(tokenB.value());
                }

                yield Integer.compare(
                        Integer.parseInt(tokenA.value()),
                        Integer.parseInt(tokenB.value()));
            }
            case LETTER -> Character.compare(tokenA.value().charAt(0), tokenB.value().charAt(0));
            case REVISION -> Integer.compare(
                    Integer.parseInt(tokenA.value()),
                    Integer.parseInt(tokenB.value()));
            case SUFFIX_ALPHA,
                 SUFFIX_BETA,
                 SUFFIX_CVS,
                 SUFFIX_GIT,
                 SUFFIX_HG,
                 SUFFIX_P,
                 SUFFIX_PRE,
                 SUFFIX_RC,
                 SUFFIX_SVN -> 0;
        };
    }

}
