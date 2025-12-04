import io.github.nscuro.versatile.version.DebianVersion;
import io.github.nscuro.versatile.version.GenericVersion;
import io.github.nscuro.versatile.version.GoVersion;
import io.github.nscuro.versatile.version.MavenVersion;
import io.github.nscuro.versatile.version.NpmVersion;
import io.github.nscuro.versatile.version.PythonVersion;
import io.github.nscuro.versatile.version.RpmVersion;

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
module io.github.nscuro.versatile.core {
    exports io.github.nscuro.versatile.version;
    exports io.github.nscuro.versatile;

    requires io.github.nscuro.versatile.spi;
    requires maven.artifact;
    requires packageurl.java;
    requires semver4j;

    provides io.github.nscuro.versatile.spi.VersionProvider with
            DebianVersion.Provider,
            GenericVersion.Provider,
            GoVersion.Provider,
            MavenVersion.Provider,
            NpmVersion.Provider,
            PythonVersion.Provider,
            RpmVersion.Provider;

    uses io.github.nscuro.versatile.spi.VersionProvider;
}