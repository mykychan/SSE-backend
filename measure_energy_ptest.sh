#!/usr/bin/env bash

# This script automatically:
# 1. Starts EnergiBridge and the backend
# 2. Waits for backend startup
# 3. Sends HTTP requests with curl while EnergiBridge measures energy
# 4. Saves everything in the same output directory
# Usage:
#   ./measure_energy_ptest.sh /path/to/EnergiBridge [seconds] /path/to/output-dir [concurrency] [url]

# Check EnergiBridge directory argument
if [ -z "$1" ]; then
  echo "Error: You must specify the path to EnergiBridge."
  echo "Usage: ./measure_energy_ptest.sh /path/to/EnergiBridge [seconds] /path/to/output-dir [concurrency] [url]"
  exit 1
fi

ENERGIBRIDGE_DIR="$1"

# Argument handling for measurement time and output directory
if [ -z "$2" ]; then
  echo "Error: You must specify the output directory."
  echo "Usage: ./measure_energy_ptest.sh /path/to/EnergiBridge [seconds] /path/to/output-dir [concurrency] [url]"
  exit 1
elif [[ "$2" =~ ^[0-9]+$ ]]; then
  MEASURE_TIME="$2"
  OUTPUT_DIR="$3"
  CONCURRENCY="${4:-5}"
  URL="${5:-http://localhost:4000/api/annuncio/visualizza-annuncio?id=1}"
  if [ -z "$OUTPUT_DIR" ]; then
    echo "Error: You must specify the output directory."
    echo "Usage: ./measure_energy_ptest.sh /path/to/EnergiBridge [seconds] /path/to/output-dir [concurrency] [url]"
    exit 1
  fi
else
  MEASURE_TIME=15
  OUTPUT_DIR="$2"
  CONCURRENCY="${3:-5}"
  URL="${4:-http://localhost:4000/api/annuncio/visualizza-annuncio?id=1}"
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"

BACKEND_LOG="$OUTPUT_DIR/backend.log"
CURL_STATUS_LOG="$OUTPUT_DIR/curl-status.log"
CURL_ERRORS_LOG="$OUTPUT_DIR/curl-errors.log"
META_FILE="$OUTPUT_DIR/metadata.txt"

# Save metadata about the measurement
{
  echo "EnergiBridge measurement metadata"
  echo "--------------------------------"
  echo "Date: $(date)"
  echo "Command: $0 $*"
  echo ""
  echo "EnergiBridge directory: $ENERGIBRIDGE_DIR"
  echo "Duration (seconds): $MEASURE_TIME"
  echo "Output directory: $OUTPUT_DIR"
  echo "Concurrency (parallel curl calls): $CONCURRENCY"
  echo "Endpoint tested: $URL"
} > "$META_FILE"

echo "---------------------------------------------"
echo "Output directory:   $OUTPUT_DIR"
echo "Measurement time:   $MEASURE_TIME seconds"
echo "Concurrency:        $CONCURRENCY"
echo "Target URL:         $URL"
echo "---------------------------------------------"
echo ""

echo "[1/3] Starting EnergiBridge + backend..."
./measure_energy.sh "$ENERGIBRIDGE_DIR" "$MEASURE_TIME" "$OUTPUT_DIR" >"$BACKEND_LOG" 2>&1 &
ENERGY_PID=$!

echo "[2/3] Waiting for backend to start..."
sleep 5

echo "[3/3] Sending HTTP requests with curl..."

echo "" > "$CURL_STATUS_LOG"
echo "" > "$CURL_ERRORS_LOG"

echo ""
echo "Curl load started, will stop when EnergiBridge finishes."
echo ""

while kill -0 "$ENERGY_PID" 2>/dev/null; do
  for ((i=0; i<CONCURRENCY; i++)); do
    (
      CODE=$(curl -s -o /dev/null -w "%{http_code}" "$URL" || echo "000")
      if [ "$CODE" -ge 200 ] && [ "$CODE" -lt 400 ]; then
        echo "$CODE" >> "$CURL_STATUS_LOG"
      else
        echo "$CODE" >> "$CURL_ERRORS_LOG"
      fi
    ) &
  done
  wait
  sleep 0.1
done

echo ""
echo "Curl load finished (EnergiBridge completed)."

echo ""
echo "----------------------------------------"
echo " EnergiBridge + curl test completed!"
echo " Energy CSVs:      $OUTPUT_DIR/energy-*.csv"
echo " Backend logs:     $BACKEND_LOG"
echo " Curl status log:  $CURL_STATUS_LOG"
echo " Curl errors log:  $CURL_ERRORS_LOG"
echo " Metadata file:    $META_FILE"
echo "----------------------------------------"

