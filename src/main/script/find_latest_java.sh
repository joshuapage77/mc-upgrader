#!/usr/bin/env bash

# File: scripts/find_latest_java.sh

set -e

quiet=0
runtime_base_dir="../../runtime"
selected_java=""
log() {
   if [[ $quiet -eq 0 ]]; then
      echo "[INFO] $*"
   fi
}

err() {
   echo "[ERROR] $*" >&2
   exit 1
}

# Parse flags
while [[ $# -gt 0 ]]; do
   case "$1" in
      -q|--quiet)
         quiet=1
         shift
         ;;
      *)
         err "Unknown option: $1"
         ;;
   esac
done

log "Scanning runtime directory: $runtime_base_dir"

[[ -d "$runtime_base_dir" ]] || err "Runtime directory not found: $runtime_base_dir"

best_version=0
best_path=""

for runtime_dir in "$runtime_base_dir"/java-runtime-*; do
      log "checking: $runtime_dir"
      if [ ! -d "$runtime_dir" ]; then
         continue
      fi

      log "searching for jpackage under $runtime_dir"
      jpackage_path=$(find "$runtime_dir" -type f -name "jpackage" 2>/dev/null | head -n1)
      if [ -z "$jpackage_path" ]; then
         continue
      fi

      java_bin=$(dirname "$jpackage_path")/java
      if [ ! -x "$java_bin" ]; then
         continue
      fi

      version_str=$("$java_bin" -version 2>&1 | grep 'version' | awk '{print $3}' | tr -d '"')
      major=$(echo "$version_str" | cut -d. -f1 | sed 's/^1$//')
      echo "$major" | grep -Eq '^[0-9]+$'
      if [ $? -ne 0 ]; then
         continue
      fi

      log "Found Java $major at $java_bin"

      if [ "$major" -gt "$best_version" ]; then
         best_version=$major
         best_path=$java_bin
      fi
   done

[[ -n "$best_path" ]] || err "No valid Java runtimes found."

log "Selected Java version: $best_version at $best_path"

echo "$best_path"
