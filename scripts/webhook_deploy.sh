#!/bin/bash

# Webhook Auto Deploy Script
# Triggered by GitHub webhook

set -e

# Load configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/config.sh" ]; then
    source "$SCRIPT_DIR/config.sh"
else
    echo "ERROR: config.sh not found!"
    exit 1
fi

# Logging
LOG_FILE="${LOG_DIR}/${APP_NAME}-deploy.log"
exec > >(tee -a "$LOG_FILE") 2>&1

echo "=========================================="
echo "Auto-Deploy triggered at $(date)"
echo "=========================================="

echo "Ensuring correct permissions for $APP_USER..."

# 1. Ensure the directory is owned by the deployment user
chown -R "$APP_USER:$APP_USER" "$APP_DIR"

# 2. Add the safe directory exception specifically for the deployment user
sudo -u "$APP_USER" git config --global --add safe.directory "$APP_DIR" 2>/dev/null || true

# 3. Clean up any stale lock files that cause "Permission Denied"
rm -f "$APP_DIR/.git/index.lock"
rm -f "$APP_DIR/.git/FETCH_HEAD"

# Navigate to app directory
cd "$APP_DIR"

# Backup current version (just in case)
echo "[1/5] Creating backup..."
cp "$JAR_PATH" "${APP_DIR}/target/${JAR_NAME%.jar}-backup-$(date +%Y%m%d_%H%M%S).jar" || true

# Pull latest code
echo "[2/5] Pulling latest code..."
sudo -u "$APP_USER" git fetch origin
sudo -u "$APP_USER" git reset --hard origin/"$GIT_BRANCH"  # Force update to latest

# Build application
echo "[3/5] Building application..."
sudo -u "$APP_USER" mvn clean package -DskipTests

# Check if build successful
if [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: Build failed! JAR not found."
    echo "Restoring from backup..."
    cp "${APP_DIR}/target/${JAR_NAME%.jar}-backup-"*.jar "$JAR_PATH"
    exit 1
fi

# Restart service
echo "[4/5] Restarting service..."
sudo systemctl restart "$APP_NAME"

# Wait for service to start
sleep 5

# Verify service is running
echo "[5/5] Verifying service..."
if systemctl is-active --quiet "$APP_NAME"; then
    echo "SUCCESS: Application deployed and running!"

    # Cleanup old backups (keep last 5)
    cd "${APP_DIR}/target"
    ls -t "${JAR_NAME%.jar}"-backup-*.jar 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true

    exit 0
else
    echo "ERROR: Service failed to start!"
    echo "Check logs: sudo journalctl -u $APP_NAME -n 50"
    exit 1
fi
