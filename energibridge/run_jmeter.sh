#!/usr/bin/env bash

# This script runs a JMeter load test and appends test metadata
# to the metadata.txt file created by measure_energy.sh.
# Usage:
#   ./run_jmeter.sh <test.jmx> <energibridge-output-dir>

# Check JMX file
if [ -z "$1" ]; then
  echo "Error: You must specify the JMX file."
  echo "Usage: ./run_jmeter.sh <test.jmx> <energibridge-output-dir>"
  exit 1
fi
JMX_FILE="$1"

# Check output directory
if [ -z "$2" ]; then
  echo "Error: You must specify the EnergiBridge output directory."
  echo "Usage: ./run_jmeter.sh <test.jmx> <energibridge-output-dir>"
  exit 1
fi
OUTPUT_DIR="$2"

META_FILE="$OUTPUT_DIR/metadata.txt"

# Check metadata file
if [ ! -f "$META_FILE" ]; then
  echo "Error: metadata.txt not found in:"
  echo "  $META_FILE"
  exit 1
fi

# Gather system information
CPU_MODEL=$(lscpu | grep "Model name" | sed 's/Model name:\s*//')
CPU_CORES=$(nproc)
TOTAL_MEM=$(free -h | awk '/Mem:/ {print $2}')
OS_INFO=$(uname -a)

# Extract multiple ThreadGroup parameters
THREADS_LIST=($(grep -oP '(?<=<intProp name="ThreadGroup.num_threads">)\d+' "$JMX_FILE"))
RAMPUP_LIST=($(grep -oP '(?<=<intProp name="ThreadGroup.ramp_time">)\d+' "$JMX_FILE"))
DURATION_LIST=($(grep -oP '(?<=<longProp name="ThreadGroup.duration">)\d+' "$JMX_FILE"))
DELAY_LIST=($(grep -oP '(?<=<longProp name="ThreadGroup.delay">)\d+' "$JMX_FILE"))

# Extract unique endpoints
ENDPOINTS=$(grep -oP '(?<=<stringProp name="HTTPSampler.path">).*(?=</stringProp>)' "$JMX_FILE" | sort -u)

# Run JMeter
START_TIME=$(date +"%Y-%m-%d %H:%M:%S")
jmeter -n -t "$JMX_FILE"
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

  echo ""
  echo "JMeter Test Metadata"
  echo "-------------------------"
  echo "Start time:  $START_TIME"
  echo "End time:    $END_TIME"
  echo "JMX file:    $JMX_FILE"
  echo ""

  echo "Thread Groups:"
  echo "-------------------------"
  for i in "${!THREADS_LIST[@]}"; do
    echo "TG$((i+1)) -> threads: ${THREADS_LIST[$i]}, ramp-up: ${RAMPUP_LIST[$i]}, duration: ${DURATION_LIST[$i]}, delay: ${DELAY_LIST[$i]}"
  done

  echo ""
  echo "Endpoints used:"
  echo "-------------------------"
  echo "$ENDPOINTS"

} >> "$META_FILE"

echo "JMeter metadata appended to: $META_FILE"

