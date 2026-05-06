@echo off
REM =============================================================
REM Aurora LowCode — JMH Performance Benchmark Runner (Windows)
REM =============================================================
REM Runs Virtual Thread vs Platform Thread concurrency benchmarks.
REM Requires: Java 25+ and Maven.
REM
REM Usage:
REM   scripts\run-benchmark.bat          # Run all benchmarks
REM   scripts\run-benchmark.bat quick    # Quick mode (fewer iterations)
REM =============================================================
setlocal

echo ============================================
echo  Aurora LowCode — JMH Benchmark Suite
java --version 2>&1 | findstr /C:"openjdk" /C:"version"
echo ============================================
echo.

REM Compile test sources
echo [1/2] Compiling benchmarks...
call mvn test-compile -DskipSpringdoc=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true -q
if errorlevel 1 (
    echo ERROR: Compilation failed
    exit /b 1
)

REM Run JMH benchmarks
echo [2/2] Running JMH benchmarks...
echo.

call mvn exec:java ^
    -Dexec.mainClass="com.aurora.benchmark.BenchmarkRunner" ^
    -Dexec.classpathScope=test ^
    -DskipSpringdoc=true ^
    -Dmaven.javadoc.skip=true ^
    -Dmaven.source.skip=true

echo.
echo ============================================
echo  Benchmark results saved to:
echo    target\benchmark-results.json
echo ============================================
endlocal
