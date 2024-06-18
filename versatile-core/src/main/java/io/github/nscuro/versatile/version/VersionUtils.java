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

import java.util.regex.Pattern;

/**
 * @since 0.9.0
 */
final class VersionUtils {

    private static final Pattern PATTERN_ALPHANUMERIC = Pattern.compile("^[a-zA-Z0-9\\-]+$");
    private static final Pattern PATTERN_NUMERIC = Pattern.compile("^\\d+$");

    private VersionUtils() {
    }

    static boolean isAlphaNumeric(final String str) {
        if (str == null) {
            return false;
        }

        return PATTERN_ALPHANUMERIC.matcher(str).matches();
    }

    static boolean isNumeric(final String str) {
        if (str == null) {
            return false;
        }

        return PATTERN_NUMERIC.matcher(str).matches();
    }

}
