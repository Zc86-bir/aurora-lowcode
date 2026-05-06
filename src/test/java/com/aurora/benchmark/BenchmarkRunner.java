package com.aurora.benchmark;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH Benchmark Runner for Aurora LowCode Platform.
 *
 * <p>Runs the VirtualThread vs PlatformThread concurrency benchmark
 * and outputs results to the console.
 *
 * <p>Usage: Run this class as a Java application or via:
 * <pre>{@code
 *   mvn test-compile exec:java -Dexec.mainClass="com.aurora.benchmark.BenchmarkRunner"
 * }</pre>
 */
public class BenchmarkRunner {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(VirtualThreadVsPlatformThreadBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(2)
                .measurementIterations(3)
                .resultFormat(ResultFormatType.JSON)
                .result("target/benchmark-results.json")
                .build();

        new Runner(opt).run();
    }
}
