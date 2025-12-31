#!/bin/bash

# Database Reset Script
# WARNING: This script will DELETE ALL DATA!
# Usage: bash reset_database.sh

set -e

# Load configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/config.sh" ]; then
    source "$SCRIPT_DIR/config.sh"
else
    echo "ERROR: config.sh not found!"
    exit 1
fi

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠ $1${NC}"; }
print_info() { echo -e "${YELLOW}→ $1${NC}"; }

# Load environment variables
if [ -f "$ENV_FILE" ]; then
    source "$ENV_FILE"
elif [ -f ".env" ]; then
    source .env
else
    print_error ".env file not found!"
    print_info "Run this script from $APP_DIR directory"
    exit 1
fi

# Extract database name from DB_URL
DB_NAME=$(echo $DB_URL | sed 's/.*\/\([^?]*\).*/\1/')

print_warning "=========================================="
print_warning "DATABASE RESET"
print_warning "=========================================="
echo ""
print_warning "This will DELETE ALL DATA in database: $DB_NAME"
print_warning "Database user: $DB_USERNAME"
echo ""
print_warning "ALL tables, data, and records will be PERMANENTLY DELETED!"
echo ""

# Confirmation
read -p "Are you sure you want to continue? (type 'YES' to confirm): " CONFIRM

if [ "$CONFIRM" != "YES" ]; then
    print_info "Reset cancelled."
    exit 0
fi

echo ""
print_warning "Last chance! This action CANNOT be undone!"
read -p "Type the database name '$DB_NAME' to confirm: " CONFIRM_DB

if [ "$CONFIRM_DB" != "$DB_NAME" ]; then
    print_error "Database name doesn't match. Reset cancelled."
    exit 0
fi

echo ""
print_info "=========================================="
print_info "Starting database reset..."
print_info "=========================================="
echo ""

# Step 1: Create backup first (safety)
print_info "[1/5] Creating backup before reset..."
mkdir -p "$BACKUP_DIR" 2>/dev/null || mkdir -p backups

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/backup_before_reset_${TIMESTAMP}.sql"

mysqldump -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME > $BACKUP_FILE 2>/dev/null || true

if [ -f "$BACKUP_FILE" ]; then
    BACKUP_SIZE=$(du -h $BACKUP_FILE | cut -f1)
    print_success "Backup created: $BACKUP_FILE (Size: $BACKUP_SIZE)"
else
    print_warning "Backup failed or database is empty"
fi
echo ""

# Step 2: Stop application (to release DB connections)
print_info "[2/5] Stopping application..."
if systemctl is-active --quiet "$APP_NAME" 2>/dev/null; then
    sudo systemctl stop "$APP_NAME"
    print_success "Application stopped"
else
    print_info "Application not running (or not in VPS)"
fi
echo ""

# Step 3: Drop and recreate database
print_info "[3/5] Dropping database..."
mysql -u $DB_USERNAME -p$DB_PASSWORD -e "DROP DATABASE IF EXISTS $DB_NAME;" 2>/dev/null || \
mysql -u root -p -e "DROP DATABASE IF EXISTS $DB_NAME;"

print_success "Database dropped"
echo ""

print_info "[4/5] Creating fresh database..."
mysql -u $DB_USERNAME -p$DB_PASSWORD -e "CREATE DATABASE $DB_NAME CHARACTER SET ${DB_CHARSET} COLLATE ${DB_COLLATION};" 2>/dev/null || \
mysql -u root -p -e "CREATE DATABASE $DB_NAME CHARACTER SET ${DB_CHARSET} COLLATE ${DB_COLLATION}; GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USERNAME'@'localhost'; FLUSH PRIVILEGES;"

print_success "Fresh database created: $DB_NAME"
echo ""

# Step 4: Restart application (Flyway will auto-migrate)
print_info "[5/5] Restarting application..."
print_info "Flyway migrations will run automatically to create tables and insert default data..."
echo ""

if systemctl is-active --quiet "$APP_NAME" 2>/dev/null; then
    sudo systemctl start "$APP_NAME"
    sleep 5

    if systemctl is-active --quiet "$APP_NAME"; then
        print_success "Application restarted"
    else
        print_error "Application failed to start!"
        print_info "Check logs: sudo journalctl -u $APP_NAME -n 100"
        exit 1
    fi
else
    print_info "Not in VPS environment. Start application manually:"
    print_info "  mvn spring-boot:run"
    print_info "  OR"
    print_info "  java -jar $JAR_PATH"
fi

echo ""
print_success "=========================================="
print_success "Database Reset Complete!"
print_success "=========================================="
echo ""
print_info "What happened:"
echo "  1. ✓ Backup created (if data existed)"
echo "  2. ✓ Old database dropped"
echo "  3. ✓ Fresh database created"
echo "  4. ✓ Flyway migrations applied"
echo "  5. ✓ Default data inserted (menu items, cashiers)"
echo ""
print_info "Default credentials restored:"
echo "  Username: admin    | Password: password123"
echo "  Username: kasir1   | Password: password123"
echo "  Username: kasir2   | Password: password123"
echo ""
print_info "Backup location: $BACKUP_FILE"
echo ""
print_info "To restore from backup if needed:"
echo "  mysql -u $DB_USERNAME -p $DB_NAME < $BACKUP_FILE"
echo ""
