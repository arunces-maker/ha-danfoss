#!/usr/bin/with-contenv bashio
set -euo pipefail

echo "Starting Danfoss Icon multi-house app..."

M4_FIX_ENABLED="$(bashio::config 'm4FixEnabled')"

if [[ "$M4_FIX_ENABLED" == "true" ]]; then
    export JAVA_TOOL_OPTIONS="-XX:UseSVE=0"
fi

CONFIG_DIR="/share/danfoss-icon"
OLD_CONFIG="$CONFIG_DIR/danfoss_config.json"
CONFIG_1A="$CONFIG_DIR/danfoss_config_1a.json"
CONFIG_2A="$CONFIG_DIR/danfoss_config_2a.json"

mkdir -p "$CONFIG_DIR"

# Preserve the existing paired first-floor controller.
if [[ -f "$OLD_CONFIG" && ! -f "$CONFIG_1A" ]]; then
    echo "Migrating existing Danfoss configuration to house 1A..."
    cp "$OLD_CONFIG" "$CONFIG_1A"
fi

echo "Starting house 1A on port 9199..."
java --enable-preview \
    -DDANFOSS_CONFIG_FILE="$CONFIG_1A" \
    -DDANFOSS_PORT=9199 \
    -jar /app.jar &
PID_1A=$!

echo "Starting house 2A on port 9200..."
java --enable-preview \
    -DDANFOSS_CONFIG_FILE="$CONFIG_2A" \
    -DDANFOSS_PORT=9200 \
    -jar /app.jar &
PID_2A=$!

cleanup() {
    echo "Stopping Danfoss Icon processes..."
    kill "$PID_1A" "$PID_2A" 2>/dev/null || true
    wait "$PID_1A" "$PID_2A" 2>/dev/null || true
}

trap cleanup TERM INT

# If either process crashes, stop the other and let Supervisor restart both.
set +e
wait -n "$PID_1A" "$PID_2A"
STATUS=$?
set -e

cleanup
exit "$STATUS"
