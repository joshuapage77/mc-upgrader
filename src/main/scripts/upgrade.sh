#!/usr/bin/env bash
set -e

ENV_FILE=".env"

if [[ -f "$ENV_FILE" ]]; then
   source "$ENV_FILE"
else
   JAVA_PATH=$(./find_latest_java.sh -q) || {
      echo "[ERROR] Failed to detect Java runtime"
      exit 1
   }
   echo "JAVA_PATH=$JAVA_PATH" > "$ENV_FILE"
   echo "[INFO] Created .env with JAVA_PATH=$JAVA_PATH"
fi

echo "[INFO] Using Java: $JAVA_PATH"
"$JAVA_PATH" -version

"$JAVA_PATH" -jar mc-upgrader.jar "$@"