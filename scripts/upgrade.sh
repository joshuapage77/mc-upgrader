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

UPGRADE_CLASS="java/Upgrader.class"

if [[ ! -f "$UPGRADE_CLASS" ]]; then
   echo "[INFO] Compiling Java sources..."
   "$JAVA_PATH"c -d java java/*.java || {
      echo "[ERROR] Compilation failed"
      exit 1
   }
fi

#"$JAVA_PATH" -cp java Upgrader
"$JAVA_PATH" -cp java ModCompatibilityResolver