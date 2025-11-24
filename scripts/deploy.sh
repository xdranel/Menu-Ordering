#!/bin/bash

# ChopChop Restaurant - Deployment Script
# Untuk update aplikasi di VPS
# Usage: bash deploy.sh

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_info() { echo -e "${YELLOW}→ $1${NC}"; }
print_step() { echo -e "${BLUE}[$1/${2}]${NC} $3"; }

APP_DIR="/opt/menu-ordering-app"
SERVICE_NAME="chopchop"

print_info "=========================================="
print_info "ChopChop Restaurant - Deployment"
print_info "=========================================="
echo ""

# Step 1: Check if we're in the right directory
print_step 1 6 "Checking application directory..."
if [ ! -d "$APP_DIR" ]; then
    print_error "Application directory not found: $APP_DIR"
    exit 1
fi
cd "$APP_DIR"
print_success "In directory: $APP_DIR"
echo ""

# Step 2: Check if .env exists
print_step 2 6 "Checking environment configuration..."
if [ ! -f "$APP_DIR/.env" ]; then
    print_error ".env file not found!"
    print_info "Please create .env file first. See .env.vps.example for template."
    exit 1
fi
print_success ".env file found"
echo ""

# Step 3: Pull latest code (if git repo)
print_step 3 6 "Checking for updates..."
if [ -d ".git" ]; then
    print_info "Pulling latest code from Git..."
    git pull origin main || git pull origin master
    print_success "Code updated"
else
    print_info "Not a git repository, skipping..."
fi
echo ""

# Step 4: Build application
print_step 4 6 "Building application..."
print_info "This may take a few minutes..."
mvn clean package -DskipTests
print_success "Build completed"

# Check if JAR exists
if [ ! -f "target/menu-ordering-app-0.0.1-SNAPSHOT.jar" ]; then
    print_error "JAR file not found after build!"
    exit 1
fi

JAR_SIZE=$(du -h target/menu-ordering-app-0.0.1-SNAPSHOT.jar | cut -f1)
print_info "JAR size: $JAR_SIZE"
echo ""

# Step 5: Stop service
print_step 5 6 "Stopping application..."
if systemctl is-active --quiet $SERVICE_NAME; then
    sudo systemctl stop $SERVICE_NAME
    print_success "Application stopped"
else
    print_info "Application was not running"
fi
echo ""

# Step 6: Start service
print_step 6 6 "Starting application..."
sudo systemctl start $SERVICE_NAME
sleep 5  # Wait for service to start

# Check if service started successfully
if systemctl is-active --quiet $SERVICE_NAME; then
    print_success "Application started successfully!"
else
    print_error "Failed to start application!"
    print_info "Check logs with: sudo journalctl -u $SERVICE_NAME -n 50"
    exit 1
fi
echo ""

print_success "=========================================="
print_success "Deployment Complete!"
print_success "=========================================="
echo ""
print_info "Application Status:"
sudo systemctl status $SERVICE_NAME --no-pager -l || true
echo ""
print_info "Useful commands:"
echo "  sudo journalctl -u $SERVICE_NAME -f          - View live logs"
echo "  sudo journalctl -u $SERVICE_NAME -n 100      - View last 100 log lines"
echo "  sudo systemctl restart $SERVICE_NAME         - Restart application"
echo "  sudo systemctl stop $SERVICE_NAME            - Stop application"
echo ""
print_info "Access your application:"
echo "  Customer: http://YOUR_VPS_IP:8080/"
echo "  Cashier:  http://YOUR_VPS_IP:8080/cashier/login"
echo ""
