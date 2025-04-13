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
   [[ -d "$runtime_dir" ]] || continue

   os_subdir=$(find "$runtime_dir" -mindepth 1 -maxdepth 1 -type d | head -n1)
   [[ -d "$os_subdir" ]] || continue

   java_home=$(find "$os_subdir" -type d -name "java-runtime-*" | head -n1)
   java_bin="$java_home/bin/java"

   [[ -x "$java_bin" ]] || continue

   version_str=$("$java_bin" -version 2>&1 | grep 'version' | awk '{print $3}' | tr -d '"')
   major=$(echo "$version_str" | cut -d. -f1 | sed 's/^1$//') # handle Java 1.8 style

   [[ "$major" =~ ^[0-9]+$ ]] || continue

   log "Found Java $major at $java_home"

   if (( major > best_version )); then
      best_version=$major
      best_path="$java_bin"
   fi
done

[[ -n "$best_path" ]] || err "No valid Java runtimes found."

log "Selected Java version: $best_version at $best_path"

echo "$best_path"
