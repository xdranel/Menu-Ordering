#!/bin/bash

# Database Backup Script
# Usage: bash backup_database.sh

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
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_info() { echo -e "${YELLOW}→ $1${NC}"; }

# Load environment variables
if [ -f "$ENV_FILE" ]; then
    source "$ENV_FILE"
else
    print_error ".env file not found at $ENV_FILE"
    exit 1
fi

# Extract database name from DB_URL
DB_NAME=$(echo $DB_URL | sed 's/.*\/\([^?]*\).*/\1/')

print_info "=========================================="
print_info "Database Backup"
print_info "=========================================="
echo ""
print_info "Database: $DB_NAME"
print_info "User: $DB_USERNAME"
echo ""

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Generate backup filename with timestamp
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/backup_${DB_NAME}_${TIMESTAMP}.sql"

print_info "Creating backup..."
print_info "Location: $BACKUP_FILE"

# Create backup
mysqldump -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME > $BACKUP_FILE

# Check if backup was successful
if [ -f "$BACKUP_FILE" ]; then
    BACKUP_SIZE=$(du -h $BACKUP_FILE | cut -f1)
    print_success "Backup created successfully!"
    print_info "Size: $BACKUP_SIZE"
    echo ""

    # List recent backups
    print_info "Recent backups:"
    ls -lht $BACKUP_DIR | head -6
    echo ""

    # Count total backups
    BACKUP_COUNT=$(ls -1 $BACKUP_DIR/*.sql 2>/dev/null | wc -l)
    print_info "Total backups: $BACKUP_COUNT"

    # Cleanup old backups (keep last N)
    if [ $BACKUP_COUNT -gt $BACKUP_RETENTION ]; then
        print_info "Cleaning up old backups (keeping last $BACKUP_RETENTION)..."
        cd $BACKUP_DIR
        ls -t *.sql | tail -n +$(($BACKUP_RETENTION + 1)) | xargs rm -f
        print_success "Old backups removed"
    fi

    echo ""
    print_success "=========================================="
    print_info "To restore this backup, run:"
    echo "  mysql -u $DB_USERNAME -p $DB_NAME < $BACKUP_FILE"
    echo ""
else
    print_error "Backup failed!"
    exit 1
fi
