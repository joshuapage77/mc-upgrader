#! /bin/bash
# -------------------------------
# Tyberian Installer Launcher
# -------------------------------

quiet=0
JAVA_PATH=""
best_version=0
best_path=""

log() {
   [ "$quiet" -eq 0 ] && echo "[INFO] $*"
}

err() {
   echo "[ERROR] $*" >&2
   exit 1
}

# Detect platform-specific Minecraft paths
MC_DIRS=("$HOME/.minecraft" "$HOME/Library/Application Support/minecraft")

# Scan for best bundled Java
for base in "${MC_DIRS[@]}"; do
   log "checking base: $base"
   if [ ! -d "$base" ]; then
      continue
   fi

   log "Checking $base for Java runtimes..."
   for runtime_dir in "$base"/runtime/java-runtime-*; do
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
done

JAVA_PATH="$best_path"

# Fallback to system Java
if [ -z "$JAVA_PATH" ]; then
   JAVA_PATH=$(which java 2>/dev/null)
   log "Using system Java: $JAVA_PATH"
fi

if [ -z "$JAVA_PATH" ]; then
   err "No Java found."
fi

# Launch embedded JAR
exec "$JAVA_PATH" -jar "$0" "$@"
