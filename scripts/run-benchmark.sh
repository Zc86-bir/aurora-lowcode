#!/bin/bash
# =============================================================
# Aurora LowCode — JMH Performance Benchmark Runner
# =============================================================
# Runs Virtual Thread vs Platform Thread concurrency benchmarks.
# Requires: Java 25+ and Maven.
#
# Usage:
#   ./scripts/run-benchmark.sh          # Run all benchmarks
#   ./scripts/run-benchmark.sh quick    # Quick mode (fewer iterations)
# =============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_DIR"

echo "============================================"
echo " Aurora LowCode — JMH Benchmark Suite"
echo " Java: $(java --version 2>&1 | head -1)"
echo "============================================"
echo ""

# Compile test sources (includes benchmarks)
echo "[1/2] Compiling benchmarks..."
mvn test-compile -DskipSpringdoc=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true -q

# Run JMH benchmarks
echo "[2/2] Running JMH benchmarks..."
echo ""

if [ "${1:-}" = "quick" ]; then
    echo ">>> Quick mode (1 warmup, 1 iteration)"
    mvn exec:java \
        -Dexec.mainClass="com.aurora.benchmark.BenchmarkRunner" \
        -Dexec.classpathScope=test \
        -DskipSpringdoc=true \
        -Dmaven.javadoc.skip=true \
        -Dmaven.source.skip=true \
        -Djmh.warmupIterations=1 \
        -Djmh.measurementIterations=1
else
    mvn exec:java \
        -Dexec.mainClass="com.aurora.benchmark.BenchmarkRunner" \
        -Dexec.classpathScope=test \
        -DskipSpringdoc=true \
        -Dmaven.javadoc.skip=true \
        -Dmaven.source.skip=true
fi

echo ""
echo "============================================"
echo " Benchmark results saved to:"
echo "   target/benchmark-results.json"
echo "============================================"
