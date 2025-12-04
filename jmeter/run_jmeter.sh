#!/usr/bin/env bash

# Run a JMeter test and generate metadata for the report.
# Usage:
#   ./run_jmeter.sh <test.jmx> <jmeter-output-dir>

# Check JMX file
if [ -z "$1" ]; then
  echo "Error: You must specify the JMX file."
  echo "Usage: ./run_jmeter.sh <test.jmx> <jmeter-output-dir>"
  exit 1
fi
JMX_FILE="$1"

# Check output directory
if [ -z "$2" ]; then
  echo "Error: You must specify the JMeter output directory."
  echo "Usage: ./run_jmeter.sh <test.jmx> <jmeter-output-dir>"
  exit 1
fi
OUTPUT_DIR="$2"

# Create output directory if missing
mkdir -p "$OUTPUT_DIR"

# Set metadata file path
META_FILE="$OUTPUT_DIR/metadata.txt"

# Create metadata file if missing
if [ ! -f "$META_FILE" ]; then
  echo "Creating metadata.txt at $META_FILE"
  touch "$META_FILE"
fi

# Gather system information
CPU_MODEL=$(lscpu | grep "Model name" | sed 's/Model name:\s*//')
CPU_CORES=$(nproc)
TOTAL_MEM=$(free -h | awk '/Mem:/ {print $2}')
OS_INFO=$(uname -a)

# Extract ThreadGroup parameters
THREADS=$(grep -oP '(?<=<intProp name="ThreadGroup.num_threads">)\d+' "$JMX_FILE")
RAMP_UP=$(grep -oP '(?<=<intProp name="ThreadGroup.ramp_time">)\d+' "$JMX_FILE")
DURATION=$(grep -oP '(?<=<longProp name="ThreadGroup.duration">)\d+' "$JMX_FILE")
DELAY=$(grep -oP '(?<=<longProp name="ThreadGroup.delay">)\d+' "$JMX_FILE")

# Extract endpoints
ENDPOINTS=$(grep -oP '(?<=<stringProp name="HTTPSampler.path">).*(?=</stringProp>)' "$JMX_FILE")

# Run JMeter
START_TIME=$(date +"%Y-%m-%d %H:%M:%S")
jmeter -n -t "$JMX_FILE" -l "$OUTPUT_DIR/results.jtl"
END_TIME=$(date +"%Y-%m-%d %H:%M:%S")

# Generate HTML report
jmeter -g "$OUTPUT_DIR/results.jtl" -o "$OUTPUT_DIR/report"

# Append metadata
{
  echo ""
  echo "System Metadata"
  echo "-------------------------"
  echo "CPU Model:      $CPU_MODEL"
  echo "CPU Cores:      $CPU_CORES"
  echo "Total Memory:   $TOTAL_MEM"
  echo "OS:             $OS_INFO"

  echo ""
  echo "JMeter Load Test Metadata"
  echo "-------------------------"
  echo "Start time:  $START_TIME"
  echo "End time:    $END_TIME"
  echo "JMX file:    $JMX_FILE"
  echo ""
  echo "Thread count: $THREADS"
  echo "Ramp-up:      $RAMP_UP"
  echo "Duration:     $DURATION"
  echo "Delay:        $DELAY"
  echo ""
  echo "Endpoints used:"
  echo "$ENDPOINTS"
} >> "$META_FILE"

echo "JMeter test completed."
echo "Results saved in: $OUTPUT_DIR"
echo "Metadata appended to: $META_FILE"

