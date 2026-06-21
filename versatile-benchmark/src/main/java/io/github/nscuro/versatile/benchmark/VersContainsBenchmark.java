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

import io.github.nscuro.versatile.Vers;
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

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class VersContainsBenchmark {

    @Param({"SINGLE", "RANGE"})
    private String shape;

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

    private Vers vers;
    private String testedVersion;

    @Setup
    public void setup() {
        final String lower = "1.0.0";
        final String upper = "2.0.0";
        this.testedVersion = "1.5.0";

        final String constraints = "SINGLE".equals(shape)
                ? ">=" + lower
                : ">=" + lower + "|<" + upper;
        this.vers = Vers.parse("vers:" + scheme + "/" + constraints);
    }

    @Benchmark
    public boolean contains() {
        return vers.contains(testedVersion);
    }

}
