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
package io.github.nscuro.versatile.benchmark;

import io.github.nscuro.versatile.VersionFactory;
import io.github.nscuro.versatile.spi.Version;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class VersionParsingBenchmark {

    private static final Map<String, String> SIMPLE_BY_SCHEME =
            Map.ofEntries(
                    Map.entry("apk", "1.2.3"),
                    Map.entry("cargo", "1.2.3"),
                    Map.entry("composer", "1.2.3"),
                    Map.entry("deb", "1.2.3"),
                    Map.entry("gem", "1.2.3"),
                    Map.entry("generic", "1.2.3"),
                    Map.entry("golang", "v1.2.3"),
                    Map.entry("maven", "1.2.3"),
                    Map.entry("npm", "1.2.3"),
                    Map.entry("nuget", "1.2.3"),
                    Map.entry("pypi", "1.2.3"),
                    Map.entry("rpm", "1.2.3"));
    private static final Map<String, String> COMPLEX_BY_SCHEME =
            Map.ofEntries(
                    Map.entry("apk", "1.2.3_alpha1-r1"),
                    Map.entry("cargo", "1.2.3-beta.1+build.5"),
                    Map.entry("composer", "1.2.3-beta1"),
                    Map.entry("deb", "1:1.2.3-1ubuntu0.1"),
                    Map.entry("gem", "1.2.3.beta.1"),
                    Map.entry("generic", "1.2.3-beta1"),
                    Map.entry("golang", "v1.2.3-beta.1+build.5"),
                    Map.entry("maven", "1.2.3-rc.1-SNAPSHOT"),
                    Map.entry("npm", "1.2.3-beta.1"),
                    Map.entry("nuget", "1.2.3-beta.1+meta"),
                    Map.entry("pypi", "1.2.3a1.dev2+local.1"),
                    Map.entry("rpm", "1:1.2.3-1.el8"));

    @Param({
            "apk",
            "cargo",
            "composer",
            "deb",
            "gem",
            "generic",
            "golang",
            "maven",
            "npm",
            "nuget",
            "pypi",
            "rpm",
    })
    private String scheme;

    @Param({"SIMPLE", "COMPLEX"})
    private String complexity;

    private String versionStr;

    @Setup
    public void setup() {
        this.versionStr = "SIMPLE".equals(complexity)
                ? SIMPLE_BY_SCHEME.get(scheme)
                : COMPLEX_BY_SCHEME.get(scheme);
    }

    @Benchmark
    public Version parse() {
        return VersionFactory.forScheme(scheme, versionStr);
    }

}
