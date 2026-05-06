package com.aurora.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Virtual Thread vs Platform Thread concurrency benchmark.
 *
 * <p>Simulates 1000 concurrent LLM API calls, each with 200ms
 * of simulated network I/O (Thread.sleep). Compares:
 * <ul>
 *   <li>{@code Executors.newVirtualThreadPerTaskExecutor()} — Java 21+ virtual threads</li>
 *   <li>{@code Executors.newFixedThreadPool(200)} — traditional platform threads</li>
 *   <li>{@code Executors.newFixedThreadPool(16)} — constrained platform pool</li>
 *   <li>{@code Executors.newCachedThreadPool()} — unbounded platform threads</li>
 * </ul>
 *
 * <p>Expected results (virtual threads should dominate):
 * <pre>
 *   VirtualThread:    < 500ms  (all 1000 IO sleeps overlap)
 *   FixedPool(200):   ~1000ms (5 waves of 200)
 *   FixedPool(16):    ~13000ms (63 waves of 16)
 *   CachedPool:       ~1000ms (creates 1000 platform threads)
 * </pre>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 2, time = 3)
@Measurement(iterations = 3, time = 5)
public class VirtualThreadVsPlatformThreadBenchmark {

    private static final int TASK_COUNT = 1000;
    private static final long SIMULATED_IO_MS = 200;

    @Param({"virtual", "fixed200", "fixed16", "cached"})
    public String threadType;

    private ExecutorService executor;

    @Setup(Level.Iteration)
    public void setup() {
        executor = switch (threadType) {
            case "virtual" -> Executors.newVirtualThreadPerTaskExecutor();
            case "fixed200" -> Executors.newFixedThreadPool(200, Thread.ofPlatform().factory());
            case "fixed16" -> Executors.newFixedThreadPool(16, Thread.ofPlatform().factory());
            case "cached" -> Executors.newCachedThreadPool(Thread.ofPlatform().factory());
            default -> throw new IllegalArgumentException("Unknown threadType: " + threadType);
        };
    }

    @TearDown(Level.Iteration)
    public void teardown() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Simulate 1000 concurrent I/O-bound tasks.
     *
     * <p>Each task sleeps for 200ms to simulate a network call
     * (LLM API invocation), then records a trivial computation result.
     */
    @Benchmark
    public void concurrentIOTasks(Blackhole bh) throws InterruptedException, ExecutionException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        // Submit all tasks
        for (int i = 0; i < TASK_COUNT; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(SIMULATED_IO_MS);
                    counter.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all tasks to complete
        latch.await();

        bh.consume(counter.get());
    }

}
