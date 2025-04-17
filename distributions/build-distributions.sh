#!/bin/bash

# Load variables from .env file if it exists
if [[ -f .env ]]; then
  while IFS='=' read -r key value; do
    [[ "$key" =~ ^#.*$ || -z "$key" ]] && continue
    export "$key"="$value"
  done < .env
fi

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$(dirname "$0")/.."

LAUNCH4J_HOME=tools/launch4j
# This is how you would rewrite path variables after moving to project root
[[ "$LAUNCH4J_HOME" != /* ]] && LAUNCH4J_JAR="$ROOT_DIR/$LAUNCH4J_HOME"
echo "LAUNCH4J_HOME: $LAUNCH4J_HOME"
set -e

JAR="target/tyberian-installer.jar"
INSTALLER="distributions/tyberian-installer.sh"
MAIN_CLASS="com.mordore.install.InstallerMain"
APP_NAME="tyberian-installer"
OUTPUT_DIR="target/dist"
ICON="src/main/resources"
JRE="distributions/jre-swing"

ALL_TYPES=("linux" "mac" "mac_command" "win")

if [[ -z "$LAUNCH4J_HOME" ]]; then
  echo "ERROR: LAUNCH4J_HOME not set"
  exit 1
fi

TYPES=()
EXTRA_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    win)
      TYPES+=("win")
      ;;
    mac)
      TYPES+=("mac" "mac_command")
      ;;
    linux)
      TYPES+=("linux")
      ;;
    -v|--verbose)
      EXTRA_ARGS+=("--verbose")
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
  shift
done

if [[ ${#TYPES[@]} -eq 0 ]]; then
  TYPES=("${ALL_TYPES[@]}")
fi

if [ -d "$OUTPUT_DIR" ]; then
  echo "==> Cleaning existing $OUTPUT_DIR..."
  rm -rf "$OUTPUT_DIR"/*
else
  echo "==> Creating $OUTPUT_DIR..."
  mkdir -p "$OUTPUT_DIR"
fi

for TYPE in "${TYPES[@]}"; do
  echo "==> Building [$TYPE] package ..."

  case "$TYPE" in
    linux)
      cat $INSTALLER $JAR > $OUTPUT_DIR/${APP_NAME}.run
      chmod +x $OUTPUT_DIR/${APP_NAME}.run
      ;;
    mac_command)
      cat $INSTALLER $JAR > $OUTPUT_DIR/${APP_NAME}.command
      chmod +x $OUTPUT_DIR/${APP_NAME}.command
      ;;
    mac)
      if [[ "$(uname)" == "Darwin" ]]; then
        bash distributions/create_mac_app_dmg.sh \
          --name "$APP_NAME" \
          --jar "$(basename "$JAR")" \
          --class "$MAIN_CLASS" \
          --icon "$ICON" \
          --dest "$OUTPUT_DIR"
      else
        bash distributions/create_mac_app_zip.sh \
          --name "$APP_NAME" \
          --jar "$(basename "$JAR")" \
          --class "$MAIN_CLASS" \
          --icon "$ICON" \
          --dest "$OUTPUT_DIR"
      fi
      ;;
    win)
      bash distributions/create_win_exe.sh \
        --jar "$JAR" \
        --exe "$OUTPUT_DIR/${APP_NAME}.exe" \
        --icon "$ICON" \
        --launch4j "$LAUNCH4J_HOME"
      WIN_RESULT="${APP_NAME}_win"
      mkdir "$OUTPUT_DIR/$WIN_RESULT"
      mv "$OUTPUT_DIR/${APP_NAME}.exe" "$OUTPUT_DIR/$WIN_RESULT"
      cp -r $JRE "$OUTPUT_DIR/$WIN_RESULT"
      zip -r "$OUTPUT_DIR/${WIN_RESULT}.zip" "$OUTPUT_DIR/$WIN_RESULT"
      rm -rf "$OUTPUT_DIR/$WIN_RESULT"
      ;;
    *)
      echo "Skipping unsupported type: $TYPE"
      ;;
  esac
done

echo "✅ All done. Distributables are in $OUTPUT_DIR"
