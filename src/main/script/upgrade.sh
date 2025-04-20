#!/usr/bin/env bash
set -e

PROPERTIES_FILE="properties.json"

if [[ ! -f "$PROPERTIES_FILE" ]]; then
   echo "[ERROR] Missing properties.json"
   exit 1
fi

JAVA_PATH=$(jq -r '.java' "$PROPERTIES_FILE")

if [[ -z "$JAVA_PATH" || "$JAVA_PATH" == "null" ]]; then
   echo "[ERROR] Java path not found in properties.json"
   exit 1
fi

echo "[INFO] Using Java: $JAVA_PATH"
"$JAVA_PATH" -version

"$JAVA_PATH" -jar mc-upgrader.jar "$@"
