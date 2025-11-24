#!/bin/bash

# ChopChop Restaurant - Create Systemd Service
# Usage: sudo bash create_service.sh

set -e

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

print_info "Creating ChopChop systemd service..."

# Create service file
cat > /etc/systemd/system/chopchop.service << 'EOF'
[Unit]
Description=ChopChop Restaurant Application
After=syslog.target network.target mysql.service

[Service]
User=chopchop
Group=chopchop
WorkingDirectory=/opt/menu-ordering-app

# Load environment variables from .env
EnvironmentFile=/opt/menu-ordering-app/.env

# Java execution with memory settings
# Adjust -Xmx based on your VPS RAM (2g for 4GB VPS, 1g for 2GB VPS)
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/menu-ordering-app/target/menu-ordering-app-0.0.1-SNAPSHOT.jar

# Restart policy
Restart=always
RestartSec=10
SuccessExitStatus=143

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=chopchop

[Install]
WantedBy=multi-user.target
EOF

print_success "Service file created at /etc/systemd/system/chopchop.service"

# Reload systemd
systemctl daemon-reload
print_success "Systemd reloaded"

# Enable service
systemctl enable chopchop
print_success "Service enabled (will auto-start on boot)"

print_info "Service created successfully!"
echo ""
print_info "Available commands:"
echo "  sudo systemctl start chopchop    - Start the application"
echo "  sudo systemctl stop chopchop     - Stop the application"
echo "  sudo systemctl restart chopchop  - Restart the application"
echo "  sudo systemctl status chopchop   - Check status"
echo "  sudo journalctl -u chopchop -f   - View logs (real-time)"
echo ""
print_info "To start the application now, run:"
echo "  sudo systemctl start chopchop"
