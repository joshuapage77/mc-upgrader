#!/bin/bash
# package_mac_app - Creates a standalone .app from a Java JAR using jpackage on macOS
cd "$(dirname "$0")/.."
set -e

# --- Defaults ---
QUIET=0

# --- Logging ---
log() {
   [ "$QUIET" -eq 0 ] && echo "[INFO] $*"
}

# --- Help message ---
show_help() {
   cat <<EOF
Usage: $0 [OPTIONS]

Options:
  --name <app-name>         Name of the app (without .app)
  --dest <output-path>      Output directory for the .app bundle
  --jar <main-jar>          Name of the main JAR file
  --class <main-class>      Main class name
  --icon <icon-path>        Path to .icns or .png icon
  --quiet                   Suppress logging
  --help                    Show this help message
EOF
   exit 0
}

# --- Parse arguments ---
while [[ $# -gt 0 ]]; do
   case "$1" in
      --name) APP_NAME="$2"; shift 2 ;;
      --dest) OUTPUT_PATH="$2"; shift 2 ;;
      --jar) MAIN_JAR="$2"; shift 2 ;;
      --class) MAIN_CLASS="$2"; shift 2 ;;
      --icon) ICON_PATH="$2"; shift 2 ;;
      --quiet) QUIET=1; shift ;;
      --help) show_help ;;
      *) echo "Unknown option: $1" >&2; show_help ;;
   esac
done

# --- Validate required args ---
if [ -z "$APP_NAME" ] || [ -z "$OUTPUT_PATH" ] || [ -z "$MAIN_JAR" ] || [ -z "$MAIN_CLASS" ] || [ -z "$ICON_PATH" ]; then
   echo "[ERROR] Missing required argument(s)." >&2
   show_help
fi

# --- Constants ---
INPUT_PATH=target/jpackage-input

# --- Clean output ---
APP_DIR="$OUTPUT_PATH/$APP_NAME"
rm -rf "$APP_DIR" "$APP_DIR.dmg"

rm -rf $INPUT_PATH
mkdir $INPUT_PATH
cp target/$MAIN_JAR $INPUT_PATH
# --- Run jpackage ---
log "Packaging $APP_NAME..."
jpackage \
  --type dmg \
  --name "$APP_NAME" \
  --input "$INPUT_PATH" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --icon "$ICON_PATH" \
  --java-options "-Xmx1G" \
  --dest "$OUTPUT_PATH"

log "Created: $APP_DIR.dmg"
