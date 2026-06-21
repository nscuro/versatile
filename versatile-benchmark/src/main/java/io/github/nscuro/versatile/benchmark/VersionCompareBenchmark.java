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
public class VersionCompareBenchmark {

    private static final Map<String, String[]> COMPLEX_PAIR_BY_SCHEME =
            Map.ofEntries(
                    Map.entry("apk", new String[]{"1.2.3_alpha1-r1", "1.2.3_alpha2-r1"}),
                    Map.entry("cargo", new String[]{"1.2.3-beta.1", "1.2.3-beta.2"}),
                    Map.entry("composer", new String[]{"1.2.3-beta1", "1.2.3-beta2"}),
                    Map.entry("deb", new String[]{"1:1.2.3-1ubuntu0.1", "1:1.2.3-1ubuntu0.2"}),
                    Map.entry("gem", new String[]{"1.2.3.beta.1", "1.2.3.beta.2"}),
                    Map.entry("generic", new String[]{"1.2.3-beta1", "1.2.3-beta2"}),
                    Map.entry("golang", new String[]{"v1.2.3-beta.1", "v1.2.3-beta.2"}),
                    Map.entry("maven", new String[]{"1.2.3-rc1", "1.2.3-rc2"}),
                    Map.entry("npm", new String[]{"1.2.3-beta.1", "1.2.3-beta.2"}),
                    Map.entry("nuget", new String[]{"1.2.3-beta.1", "1.2.3-beta.2"}),
                    Map.entry("pypi", new String[]{"1.2.3a1.dev2", "1.2.3a1.dev3"}),
                    Map.entry("rpm", new String[]{"1:1.2.3-1.el8", "1:1.2.3-2.el8"}));

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

    private Version left;
    private Version right;

    @Setup
    public void setup() {
        final String leftStr;
        final String rightStr;

        if ("SIMPLE".equals(complexity)) {
            leftStr = "1.2.3";
            rightStr = "1.2.4";
        } else {
            final String[] pair = COMPLEX_PAIR_BY_SCHEME.get(scheme);
            leftStr = pair[0];
            rightStr = pair[1];
        }

        this.left = VersionFactory.forScheme(scheme, leftStr);
        this.right = VersionFactory.forScheme(scheme, rightStr);
    }

    @Benchmark
    public int compare() {
        return left.compareTo(right);
    }

}
