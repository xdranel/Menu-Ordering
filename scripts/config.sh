#!/bin/bash

# ============================================
# Application Configuration
# ============================================
# IMPORTANT: Edit this file after cloning the repository
# to match your VPS setup and application details.
# ============================================

# --------------------------------------------
# Application Metadata
# --------------------------------------------
# The internal name used for service, user, and system files (lowercase, no spaces)
APP_NAME="chopchop"

# The display name shown in scripts and logs
APP_DISPLAY_NAME="ChopChop Restaurant"

# The name of the GitHub repository or project folder
REPO_NAME="Menu-Ordering"

# --------------------------------------------
# System Users
# --------------------------------------------
# Main application user (will be created during setup)
APP_USER="chopchop"

# Webhook/CI-CD user for automated deployments (optional)
WEBHOOK_USER="webhook"

# --------------------------------------------
# Directory Paths
# --------------------------------------------
# Main application directory (where code will be deployed)
APP_DIR="/opt/Menu-Ordering"

# Backup directory for database backups
BACKUP_DIR="/opt/Menu-Ordering/backups"

# Log directory for deployment logs
LOG_DIR="/var/log"

# --------------------------------------------
# Application Binary
# --------------------------------------------
# JAR file name (adjust version as needed)
JAR_NAME="menu-ordering-app-0.0.1-SNAPSHOT.jar"

# Full path to JAR file
JAR_PATH="${APP_DIR}/target/${JAR_NAME}"

# --------------------------------------------
# Database Configuration (Defaults)
# --------------------------------------------
# These are default values used during setup.
# The actual values will be stored in .env file.

# Default database name
DEFAULT_DB_NAME="restaurant_db"

# Default database user
DEFAULT_DB_USER="chopchop_user"

# MySQL character set
DB_CHARSET="utf8mb4"
DB_COLLATION="utf8mb4_unicode_ci"

# --------------------------------------------
# Server Configuration
# --------------------------------------------
# Application server port
SERVER_PORT="8080"

# Timezone for database connections
SERVER_TIMEZONE="Asia/Jakarta"

# --------------------------------------------
# Java/JVM Configuration
# --------------------------------------------
# Minimum heap size (adjust based on VPS RAM)
JVM_XMS="512m"

# Maximum heap size (adjust based on VPS RAM)
# Examples: 1g for 2GB VPS, 2g for 4GB VPS, 4g for 8GB+ VPS
JVM_XMX="2g"

# --------------------------------------------
# Git Configuration
# --------------------------------------------
# Default branch to deploy from
GIT_BRANCH="main"

# GitHub repository URL (used for webhook setup documentation)
# Example: https://github.com/YOUR_USERNAME/Menu-Ordering.git
GITHUB_REPO_URL="https://github.com/XDX1O1/Menu-Ordering.git"

# --------------------------------------------
# Systemd Service Configuration
# --------------------------------------------
# Service file name (usually same as APP_NAME)
SERVICE_FILE="${APP_NAME}.service"

# Service description
SERVICE_DESCRIPTION="${APP_DISPLAY_NAME} Application"

# --------------------------------------------
# Firewall Ports
# --------------------------------------------
# Ports to open in UFW firewall
FIREWALL_PORTS=(
    "22/tcp"    # SSH (CRITICAL - don't remove!)
    "80/tcp"    # HTTP
    "443/tcp"   # HTTPS
    "8080/tcp"  # Application port (adjust if SERVER_PORT changes)
)

# Webhook listener port (optional, for CI/CD)
WEBHOOK_PORT="9000"

# --------------------------------------------
# Backup Configuration
# --------------------------------------------
# Number of backups to keep (older ones will be deleted)
BACKUP_RETENTION=7

# --------------------------------------------
# VPS Provider Notes
# --------------------------------------------
# Add any provider-specific notes or configurations here.
# Examples:
# - DigitalOcean: Usually Ubuntu 22.04 LTS works out of the box
# - AWS EC2: May need to adjust security groups for ports
# - Linode: Similar to DigitalOcean
# - Hostinger: Check if Java 21 is available in your plan

# --------------------------------------------
# Environment File
# --------------------------------------------
ENV_FILE="${APP_DIR}/.env"

# --------------------------------------------
# Validation
# --------------------------------------------
# This function checks if required variables are set
validate_config() {
    local required_vars=(
        "APP_NAME"
        "APP_DISPLAY_NAME"
        "APP_USER"
        "APP_DIR"
        "JAR_NAME"
        "DEFAULT_DB_NAME"
        "DEFAULT_DB_USER"
        "SERVER_PORT"
    )
    
    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            echo "ERROR: Required variable $var is not set in config.sh"
            return 1
        fi
    done
    
    return 0
}

# Export all variables so they're available to scripts that source this file
export APP_NAME APP_DISPLAY_NAME REPO_NAME
export APP_USER WEBHOOK_USER
export APP_DIR BACKUP_DIR LOG_DIR
export JAR_NAME JAR_PATH
export DEFAULT_DB_NAME DEFAULT_DB_USER DB_CHARSET DB_COLLATION
export SERVER_PORT SERVER_TIMEZONE
export JVM_XMS JVM_XMX
export GIT_BRANCH GITHUB_REPO_URL
export SERVICE_FILE SERVICE_DESCRIPTION
export FIREWALL_PORTS WEBHOOK_PORT
export BACKUP_RETENTION
export ENV_FILE
