#!/bin/bash
set -e

while [[ $# -gt 0 ]]; do
  case "$1" in
    --jar) JAR="${2#target/}"; shift 2 ;;
    --exe) EXE="${2#target/}"; shift 2 ;;
    --icon) ICON="$2"; shift 2 ;;
    --launch4j) LAUNCH4J_HOME="$2"; shift 2 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

LAUNCH4J_HOME="distributions/tools/launch4j"

# Ensure binaries are executable (first-time git clone might strip perms)
chmod +x "$LAUNCH4J_HOME/bin/"*

# Add local bin dir to PATH
export PATH="$LAUNCH4J_HOME/bin:$PATH"
convert $ICON/icon.png target/classes/icon.ico

# Create temporary config.xml
CONFIG="target/launch4j-config.xml"

cat > "$CONFIG" <<EOF
<launch4jConfig>
  <dontWrapJar>false</dontWrapJar>
  <headerType>gui</headerType>
  <jar>$JAR</jar>
  <outfile>$EXE</outfile>
  <errTitle>Installer Error</errTitle>
  <chdir>.</chdir>
  <icon>classes/icon.ico</icon>
  <jre>
    <path>jre-swing</path>
      <requires64Bit>true</requires64Bit>
      <requiresJdk>false</requiresJdk>
  </jre>
</launch4jConfig>
EOF

# Run Launch4j with generated config
java -cp "$LAUNCH4J_HOME/launch4j.jar:$LAUNCH4J_HOME/xstream.jar" net.sf.launch4j.Main "$CONFIG"

# Cleanup
rm "$CONFIG"
