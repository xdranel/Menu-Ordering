#!/bin/bash

# VPS Setup Script
# For Ubuntu 22.04 LTS
# Usage: sudo bash setup_vps.sh
# Run this once on a brand new VPS

set -e  # Exit on error

# Load configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/config.sh" ]; then
    source "$SCRIPT_DIR/config.sh"
else
    echo "ERROR: config.sh not found!"
    echo "Please make sure config.sh exists in the same directory as this script."
    exit 1
fi

# Validate configuration
if ! validate_config; then
    echo "Please check your config.sh file and ensure all required variables are set."
    exit 1
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}→ $1${NC}"
}

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    print_error "Please run as root: sudo bash setup_vps.sh"
    exit 1
fi

print_info "=========================================="
print_info "${APP_DISPLAY_NAME} - VPS Setup Script"
print_info "=========================================="
echo ""

# Step 1: Update system
print_info "Step 1: Updating system packages..."
apt update && apt upgrade -y
print_success "System updated"
echo ""

# Step 2: Install Java 21
print_info "Step 2: Installing Java 21..."
if java -version 2>&1 | grep -q "21"; then
    print_success "Java 21 already installed"
else
    apt install -y openjdk-21-jdk
    print_success "Java 21 installed"
fi
java -version
echo ""

# Step 3: Install MySQL
print_info "Step 3: Installing MySQL Server..."
if systemctl is-active --quiet mysql; then
    print_success "MySQL already running"
else
    apt install -y mysql-server
    systemctl start mysql
    systemctl enable mysql
    print_success "MySQL installed and started"
fi
echo ""

# Step 4: Install Maven
print_info "Step 4: Installing Maven..."
if command -v mvn &> /dev/null; then
    print_success "Maven already installed"
else
    apt install -y maven
    print_success "Maven installed"
fi
mvn -version
echo ""

# Step 5: Install Git
print_info "Step 5: Installing Git..."
if command -v git &> /dev/null; then
    print_success "Git already installed"
else
    apt install -y git
    print_success "Git installed"
fi
echo ""

# Step 6: Install Nginx (Optional)
print_info "Step 6: Installing Nginx..."
if command -v nginx &> /dev/null; then
    print_success "Nginx already installed"
else
    apt install -y nginx
    systemctl enable nginx
    print_success "Nginx installed"
fi
echo ""

# Step 7: Setup Firewall
print_info "Step 7: Configuring UFW Firewall..."
apt install -y ufw

# Configure firewall ports from config
for port in "${FIREWALL_PORTS[@]}"; do
    ufw allow "$port"
done
print_success "Firewall ports configured"

# Enable firewall
echo "y" | ufw enable
print_success "Firewall configured"
echo ""

# Step 8: Create application user
print_info "Step 8: Setting up application user..."
if id "$APP_USER" &>/dev/null; then
    print_success "User '$APP_USER' already exists"
else
    read -p "Create user '$APP_USER'? (y/n): " -r
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        adduser --disabled-password --gecos "" "$APP_USER"
        usermod -aG sudo "$APP_USER"
        print_success "User '$APP_USER' created"

        # Set password
        print_info "Please set password for user '$APP_USER':"
        passwd "$APP_USER"
    fi
fi
echo ""

# Step 8b: Create webhook user (for CI/CD)
print_info "Step 8b: Setting up webhook user for deployments..."
if id "$WEBHOOK_USER" &>/dev/null; then
    print_success "User '$WEBHOOK_USER' already exists"
else
    read -p "Create user '$WEBHOOK_USER' for automated deployments? (y/n): " -r
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        adduser --disabled-password --gecos "" "$WEBHOOK_USER"
        print_success "User '$WEBHOOK_USER' created"

        # Add webhook user to app group for file access
        usermod -aG "$APP_USER" "$WEBHOOK_USER"

        # Give webhook user sudo permission for specific deployment commands
        echo "$WEBHOOK_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart $APP_NAME" >> /etc/sudoers.d/"$WEBHOOK_USER"
        echo "$WEBHOOK_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl stop $APP_NAME" >> /etc/sudoers.d/"$WEBHOOK_USER"
        echo "$WEBHOOK_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl start $APP_NAME" >> /etc/sudoers.d/"$WEBHOOK_USER"
        echo "$WEBHOOK_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl status $APP_NAME" >> /etc/sudoers.d/"$WEBHOOK_USER"
        chmod 0440 /etc/sudoers.d/"$WEBHOOK_USER"
        print_success "Webhook user configured with limited sudo permissions"
    fi
fi
echo ""

# Step 9: Setup MySQL database
print_info "Step 9: Setting up database..."
echo ""
print_info "Please enter database details:"
read -p "Database name [$DEFAULT_DB_NAME]: " DB_NAME
DB_NAME=${DB_NAME:-$DEFAULT_DB_NAME}

read -p "Database user [$DEFAULT_DB_USER]: " DB_USER
DB_USER=${DB_USER:-$DEFAULT_DB_USER}

read -sp "Database password: " DB_PASSWORD
echo ""

if [ -z "$DB_PASSWORD" ]; then
    print_error "Password cannot be empty!"
    exit 1
fi

# Create database and user
print_info "Creating database and user..."
mysql -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET ${DB_CHARSET} COLLATE ${DB_COLLATION};" 2>/dev/null || true
mysql -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';" 2>/dev/null || true
mysql -e "GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';" 2>/dev/null || true
mysql -e "FLUSH PRIVILEGES;" 2>/dev/null || true
print_success "Database configured"
echo ""

# Step 10: Create application directory
print_info "Step 10: Setting up application directory..."
mkdir -p "$APP_DIR"
chown -R "$APP_USER:$APP_USER" "$APP_DIR"
print_success "Directory $APP_DIR created"
echo ""

# Step 11: Create .env template
print_info "Step 11: Creating .env configuration..."
cat > "$ENV_FILE" << EOF
# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/${DB_NAME}?useSSL=false&serverTimezone=${SERVER_TIMEZONE}&allowPublicKeyRetrieval=true
DB_USERNAME=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}

# Hibernate Configuration
HIBERNATE_DDL_AUTO=update
HIBERNATE_SHOW_SQL=false

# Thymeleaf Configuration
THYMELEAF_CACHE=true

# Logging Configuration
LOG_LEVEL_SECURITY=INFO
LOG_LEVEL_APP=INFO

# Flyway Configuration
SPRING_FLYWAY_ENABLED=true
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true

# Server Configuration
SERVER_PORT=${SERVER_PORT}
EOF

chmod 600 "$ENV_FILE"
chown "$APP_USER:$APP_USER" "$ENV_FILE"
print_success ".env file created"
echo ""

# Step 12: Install additional utilities
print_info "Step 12: Installing utilities..."
apt install -y curl wget htop net-tools
print_success "Utilities installed"
echo ""

print_success "=========================================="
print_success "VPS Setup Complete!"
print_success "=========================================="
echo ""
print_info "Next steps:"
echo "1. Clone or upload your application to $APP_DIR"
echo "2. Build the application: cd $APP_DIR && mvn clean package -DskipTests"
echo "3. Create systemd service: sudo bash scripts/create_service.sh"
echo "4. Start the application: sudo systemctl start $APP_NAME"
echo ""
print_info "Database Information:"
echo "  - Database: ${DB_NAME}"
echo "  - User: ${DB_USER}"
echo "  - Password: [saved in $ENV_FILE]"
echo ""
print_info "Access your application:"
echo "  - URL: http://YOUR_VPS_IP:${SERVER_PORT}/"
echo ""
print_info "Users created:"
echo "  - $APP_USER: Application owner (can SSH and manage app)"
echo "  - $WEBHOOK_USER: CI/CD automation (limited permissions)"
echo ""
print_info "Optional: Setup GitHub webhook for auto-deployment"
echo "  - See scripts/SCRIPTS.md 'Webhook Setup' section"
echo ""
print_info "To view this again, check: $ENV_FILE"
echo ""
