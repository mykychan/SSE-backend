#!/usr/bin/env bash

# Run a JMH benchmark and generate metadata.
# Usage:
#   ./run_jmh.sh <benchmark_class> <base-output-dir>

set -e

# Resolve paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
JMH_JAR="$PROJECT_ROOT/target/benchmarks.jar"

# Check benchmark class
if [ -z "$1" ]; then
  echo "Error: You must specify the JMH benchmark class."
  echo "Usage: ./run_jmh.sh <benchmark_class> <base-output-dir>"
  exit 1
fi
BENCHMARK_CLASS="$1"

BENCH_NAME="$(basename "$BENCHMARK_CLASS")"

# Check base output directory
if [ -z "$2" ]; then
  echo "Error: You must specify the base output directory."
  echo "Usage: ./run_jmh.sh <benchmark_class> <base-output-dir>"
  exit 1
fi
BASE_OUTPUT_DIR="$2"

OUTPUT_DIR="$BASE_OUTPUT_DIR/$BENCH_NAME"
mkdir -p "$OUTPUT_DIR"

# Verify JMH JAR existence
if [ ! -f "$JMH_JAR" ]; then
  echo "Error: JMH benchmark JAR not found at:"
  echo "  $JMH_JAR"
  exit 1
fi

# Metadata file
META_FILE="$OUTPUT_DIR/metadata.txt"
touch "$META_FILE"

# Gather system information
CPU_MODEL=$(lscpu | grep "Model name" | sed 's/Model name:\s*//')
CPU_CORES=$(nproc)
TOTAL_MEM=$(free -h | awk '/Mem:/ {print $2}')
OS_INFO=$(uname -a)
JAVA_VERSION=$(java -version 2>&1 | head -n 1)

# Output files
RESULTS_FILE="$OUTPUT_DIR/results.json"
STDOUT_FILE="$OUTPUT_DIR/jmh.log"

# Run JMH
START_TIME=$(date +"%Y-%m-%d %H:%M:%S")

java \
  -cp "$JMH_JAR" \
  org.openjdk.jmh.Main \
  "$BENCHMARK_CLASS" \
  -rf json \
  -rff "$RESULTS_FILE" \
  | tee "$STDOUT_FILE"

END_TIME=$(date +"%Y-%m-%d %H:%M:%S")

# Append metadata
{
  echo ""
  echo "System Metadata"
  echo "-------------------------"
  echo "CPU Model:      $CPU_MODEL"
  echo "CPU Cores:      $CPU_CORES"
  echo "Total Memory:   $TOTAL_MEM"
  echo "OS:             $OS_INFO"
  echo "Java Version:   $JAVA_VERSION"

  echo ""
  echo "JMH Benchmark Metadata"
  echo "-------------------------"
  echo "Benchmark class: $BENCHMARK_CLASS"
  echo "Output folder:   $OUTPUT_DIR"
  echo "JMH JAR:         $JMH_JAR"
  echo "Start time:      $START_TIME"
  echo "End time:        $END_TIME"
  echo "Results file:    $RESULTS_FILE"
  echo "Log file:        $STDOUT_FILE"

} >> "$META_FILE"

echo "JMH benchmark completed."
echo "Results saved in: $OUTPUT_DIR"

