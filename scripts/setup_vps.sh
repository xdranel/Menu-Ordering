#!/bin/bash

# ChopChop Restaurant - VPS Setup Script
# Untuk Ubuntu 22.04 LTS
# Author: Team Hola Holo
# Usage: sudo bash setup_vps.sh

set -e  # Exit on error

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
print_info "ChopChop Restaurant - VPS Setup Script"
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

# Allow SSH first (IMPORTANT!)
ufw allow 22/tcp
print_success "SSH port 22 allowed"

# Allow HTTP/HTTPS
ufw allow 80/tcp
ufw allow 443/tcp
print_success "HTTP/HTTPS allowed"

# Allow Spring Boot port
ufw allow 8080/tcp
print_success "Port 8080 allowed"

# Enable firewall
echo "y" | ufw enable
print_success "Firewall configured"
echo ""

# Step 8: Create application user
print_info "Step 8: Setting up application user..."
if id "chopchop" &>/dev/null; then
    print_success "User 'chopchop' already exists"
else
    read -p "Create user 'chopchop'? (y/n): " -r
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        adduser --disabled-password --gecos "" chopchop
        usermod -aG sudo chopchop
        print_success "User 'chopchop' created"

        # Set password
        print_info "Please set password for user 'chopchop':"
        passwd chopchop
    fi
fi
echo ""

# Step 9: Setup MySQL database
print_info "Step 9: Setting up database..."
echo ""
print_info "Please enter database details:"
read -p "Database name [restaurant_db]: " DB_NAME
DB_NAME=${DB_NAME:-restaurant_db}

read -p "Database user [chopchop_user]: " DB_USER
DB_USER=${DB_USER:-chopchop_user}

read -sp "Database password: " DB_PASSWORD
echo ""

if [ -z "$DB_PASSWORD" ]; then
    print_error "Password cannot be empty!"
    exit 1
fi

# Create database and user
print_info "Creating database and user..."
mysql -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true
mysql -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';" 2>/dev/null || true
mysql -e "GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';" 2>/dev/null || true
mysql -e "FLUSH PRIVILEGES;" 2>/dev/null || true
print_success "Database configured"
echo ""

# Step 10: Create application directory
print_info "Step 10: Setting up application directory..."
mkdir -p /opt/menu-ordering-app
chown -R chopchop:chopchop /opt/menu-ordering-app
print_success "Directory /opt/menu-ordering-app created"
echo ""

# Step 11: Create .env template
print_info "Step 11: Creating .env configuration..."
cat > /opt/menu-ordering-app/.envjust << EOF
# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/${DB_NAME}?useSSL=false&serverTimezone=Asia/Jakarta&allowPublicKeyRetrieval=true
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
SERVER_PORT=8080
EOF

chmod 600 /opt/menu-ordering-app/.envjust
chown chopchop:chopchop /opt/menu-ordering-app/.envjust
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
echo "1. Clone or upload your application to /opt/menu-ordering-app"
echo "2. Build the application: cd /opt/menu-ordering-app && mvn clean package -DskipTests"
echo "3. Create systemd service (see HOSTINGER_DEPLOYMENT.md)"
echo "4. Start the application: sudo systemctl start chopchop"
echo ""
print_info "Database Information:"
echo "  - Database: ${DB_NAME}"
echo "  - User: ${DB_USER}"
echo "  - Password: [saved in /opt/menu-ordering-app/.env]"
echo ""
print_info "Access your application:"
echo "  - Customer: http://YOUR_VPS_IP:8080/"
echo "  - Cashier: http://YOUR_VPS_IP:8080/cashier/login"
echo ""
print_info "To view this again, check: /opt/menu-ordering-app/.env"
echo ""
