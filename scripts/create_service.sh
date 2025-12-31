#!/bin/bash

# Create Systemd Service
# Usage: sudo bash create_service.sh

set -e

# Load configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/config.sh" ]; then
    source "$SCRIPT_DIR/config.sh"
else
    echo "ERROR: config.sh not found!"
    exit 1
fi

# Validate configuration
if ! validate_config; then
    echo "Please check your config.sh file."
    exit 1
fi

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_info() { echo -e "${YELLOW}→ $1${NC}"; }

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    print_error "Please run as root: sudo bash create_service.sh"
    exit 1
fi

print_info "Creating ${APP_DISPLAY_NAME} systemd service..."

# Create service file
cat > /etc/systemd/system/${SERVICE_FILE} << EOF
[Unit]
Description=${SERVICE_DESCRIPTION}
After=syslog.target network.target mysql.service

[Service]
User=${APP_USER}
Group=${APP_USER}
WorkingDirectory=${APP_DIR}

# Load environment variables from .env
EnvironmentFile=${ENV_FILE}

# Java execution with memory settings
# Adjust -Xmx based on your VPS RAM (2g for 4GB VPS, 1g for 2GB VPS)
ExecStart=/usr/bin/java -Xms${JVM_XMS} -Xmx${JVM_XMX} -jar ${JAR_PATH}

# Restart policy
Restart=always
RestartSec=10
SuccessExitStatus=143

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=${APP_NAME}

[Install]
WantedBy=multi-user.target
EOF

print_success "Service file created at /etc/systemd/system/${SERVICE_FILE}"

# Reload systemd
systemctl daemon-reload
print_success "Systemd reloaded"

# Enable service
systemctl enable "$APP_NAME"
print_success "Service enabled (will auto-start on boot)"

print_info "Service created successfully!"
echo ""
print_info "Available commands:"
echo "  sudo systemctl start $APP_NAME    - Start the application"
echo "  sudo systemctl stop $APP_NAME     - Stop the application"
echo "  sudo systemctl restart $APP_NAME  - Restart the application"
echo "  sudo systemctl status $APP_NAME   - Check status"
echo "  sudo journalctl -u $APP_NAME -f   - View logs (real-time)"
echo ""
print_info "To start the application now, run:"
echo "  sudo systemctl start $APP_NAME"
