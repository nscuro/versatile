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
package io.github.nscuro.versatile;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.util.HexFormat;

final class PercentEncoding {

    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

    private PercentEncoding() {}

    static String encode(String value) {
        final byte[] bytes = value.getBytes(UTF_8);

        final var sb = new StringBuilder(bytes.length);
        for (final byte rawByte : bytes) {
            final int b = rawByte & 0xFF;
            if (mustEncode(b)) {
                sb.append('%').append(HEX_FORMAT.toHighHexDigit(b)).append(HEX_FORMAT.toLowHexDigit(b));
            } else {
                sb.append((char) b);
            }
        }

        return sb.toString();
    }

    static String decode(String value, boolean strict) {
        final byte[] bytes = value.getBytes(UTF_8);

        final var out = new ByteArrayOutputStream(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            final int b = bytes[i] & 0xFF;
            if (b != '%') {
                if (strict && mustEncode(b)) {
                    throw new VersException("""
                            Value "%s" contains character "%s", which must be percent-encoded\
                            """.formatted(value, b));
                }
                out.write(b);
                continue;
            }

            final int hi = i + 1 < bytes.length ? hexDigit(bytes[i + 1], strict) : -1;
            final int lo = i + 2 < bytes.length ? hexDigit(bytes[i + 2], strict) : -1;
            if (hi < 0 || lo < 0) {
                throw new VersException("""
                        Invalid or non-canonical percent-encoded triplet at index %d of value "%s"\
                        """.formatted(i, value));
            }

            final int decoded = hi << 4 | lo;
            if (strict && !mustEncode(decoded)) {
                throw new VersException("""
                        Percent-encoded triplet at index %d of value "%s" is non-canonical, \
                        the encoded character does not require encoding""".formatted(i, value));
            }
            out.write(decoded);
            i += 2;
        }

        try {
            return UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(out.toByteArray()))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new VersException("Value \"%s\" does not percent-decode to valid UTF-8".formatted(value), e);
        }
    }

    private static int hexDigit(byte rawByte, boolean strict) {
        final char c = (char) (rawByte & 0xFF);
        if (strict && c >= 'a' && c <= 'f') {
            return -1; // Lowercase hex is valid, but not canonical.
        }

        return HexFormat.isHexDigit(c) ? HexFormat.fromHexDigit(c) : -1;
    }

    private static boolean mustEncode(int b) {
        return b < 0x21 || b > 0x7E || "%><=!*|".indexOf(b) >= 0;
    }
}
