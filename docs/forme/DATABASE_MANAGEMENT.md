# Database Management - ChopChop Restaurant

Panduan lengkap untuk manage database: reset, backup, restore, dan maintenance.

---

## Table of Contents

1. [Reset Database (Fresh Start)](#reset-database-fresh-start)
2. [Backup Database](#backup-database)
3. [Restore Database](#restore-database)
4. [Database Maintenance](#database-maintenance)
5. [Troubleshooting](#troubleshooting)

---

## Reset Database (Fresh Start)

**Kapan perlu reset database:**
- Database sudah messy dengan test data
- Ingin kembali ke state awal (default menu & cashiers)
- Ada error migration yang corrupt database
- Testing fresh installation
- Mau mulai dari awal untuk presentasi

### Otomatis dengan Script

```bash
# SSH ke VPS (atau di local)
cd /opt/menu-ordering-app

# Jalankan reset script
bash scripts/reset_database.sh
```

**Script akan:**
1. ✅ Backup database sebelum reset (safety)
2. ✅ Stop aplikasi
3. ✅ Drop database lama
4. ✅ Create database baru
5. ✅ Restart aplikasi (Flyway auto-run migrations)
6. ✅ Default data akan ter-insert otomatis

**Konfirmasi:**
- Script akan minta konfirmasi 2x (safety)
- Type `YES` untuk konfirmasi pertama
- Type nama database untuk konfirmasi kedua

**Hasil:**
- Database fresh dengan:
  - Menu items default (dari migrations)
  - Default cashiers (admin, kasir1, kasir2)
  - Semua tables baru
  - No orders, no old data

---

### Manual Reset (Tanpa Script)

Jika ingin reset manual:

#### 1. Backup dulu (safety)

```bash
mysqldump -u chopchop_user -p restaurant_db > backup_$(date +%Y%m%d_%H%M%S).sql
```

#### 2. Stop aplikasi

```bash
# Di VPS
sudo systemctl stop chopchop

# Di local (Ctrl+C untuk stop)
```

#### 3. Drop dan create ulang database

```bash
# Login ke MySQL
mysql -u chopchop_user -p

# Atau sebagai root
mysql -u root -p
```

Di MySQL console:

```sql
-- Drop database
DROP DATABASE restaurant_db;

-- Create fresh database
CREATE DATABASE restaurant_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Exit
EXIT;
```

#### 4. Start aplikasi (Flyway akan run migrations)

```bash
# Di VPS
sudo systemctl start chopchop

# Di local
mvn spring-boot:run
# atau
java -jar target/menu-ordering-app-0.0.1-SNAPSHOT.jar
```

Flyway akan otomatis:
- Create semua tables
- Insert default menu items
- Insert default cashiers

#### 5. Verify

```bash
# Login ke database
mysql -u chopchop_user -p restaurant_db

# Check tables
SHOW TABLES;

# Check data
SELECT COUNT(*) FROM menu_items;
SELECT * FROM cashiers;

EXIT;
```

---

## Backup Database

### Auto Backup dengan Script

```bash
cd /opt/menu-ordering-app
bash scripts/backup_database.sh
```

**Hasil:**
- Backup di: `/opt/menu-ordering-app/backups/`
- Format: `backup_restaurant_db_YYYYMMDD_HHMMSS.sql`
- Auto cleanup (keep last 7 backups)

### Manual Backup

```bash
# Full backup
mysqldump -u chopchop_user -p restaurant_db > backup_$(date +%Y%m%d_%H%M%S).sql

# Backup specific table
mysqldump -u chopchop_user -p restaurant_db orders > orders_backup.sql

# Backup with compression (save space)
mysqldump -u chopchop_user -p restaurant_db | gzip > backup_$(date +%Y%m%d_%H%M%S).sql.gz
```

### Scheduled Auto Backup (Cron)

Setup automatic daily backup:

```bash
# Edit crontab
crontab -e

# Add this line (backup every day at 3 AM)
0 3 * * * /opt/menu-ordering-app/scripts/backup_database.sh >> /var/log/chopchop-backup.log 2>&1

# Save and exit
```

Verify cron:

```bash
# List crontab
crontab -l

# Check if cron service running
sudo systemctl status cron
```

### Download Backup ke Local

```bash
# Di komputer local
scp chopchop@YOUR_VPS_IP:/opt/menu-ordering-app/backups/backup_*.sql ./
```

---

## Restore Database

### Restore dari Backup

#### 1. Stop aplikasi dulu

```bash
sudo systemctl stop chopchop
```

#### 2. Choose backup file

```bash
# List available backups
ls -lht /opt/menu-ordering-app/backups/

# Pilih file backup yang ingin restore
```

#### 3. Restore

```bash
# Drop existing database
mysql -u chopchop_user -p -e "DROP DATABASE restaurant_db;"

# Create fresh database
mysql -u chopchop_user -p -e "CREATE DATABASE restaurant_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Restore from backup
mysql -u chopchop_user -p restaurant_db < /opt/menu-ordering-app/backups/backup_20241124_120000.sql
```

#### 4. Start aplikasi

```bash
sudo systemctl start chopchop
```

### Restore Specific Table

Jika hanya ingin restore 1 table:

```bash
# Backup table yang ada sekarang dulu
mysqldump -u chopchop_user -p restaurant_db menu_items > menu_items_current.sql

# Drop table
mysql -u chopchop_user -p restaurant_db -e "DROP TABLE menu_items;"

# Restore table dari backup
mysql -u chopchop_user -p restaurant_db < menu_items_backup.sql
```

---

## Database Maintenance

### Check Database Size

```bash
mysql -u chopchop_user -p -e "
SELECT
    table_schema AS 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'restaurant_db'
GROUP BY table_schema;
"
```

### Check Table Sizes

```bash
mysql -u chopchop_user -p restaurant_db -e "
SELECT
    table_name AS 'Table',
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)',
    table_rows AS 'Rows'
FROM information_schema.TABLES
WHERE table_schema = 'restaurant_db'
ORDER BY (data_length + index_length) DESC;
"
```

### Optimize Tables

Setelah banyak insert/delete, optimize tables untuk performance:

```bash
mysql -u chopchop_user -p restaurant_db -e "
OPTIMIZE TABLE orders;
OPTIMIZE TABLE order_items;
OPTIMIZE TABLE menu_items;
"
```

### Clear Old Orders (Cleanup)

Jika database sudah penuh dengan orders lama:

```bash
mysql -u chopchop_user -p restaurant_db
```

```sql
-- Backup dulu sebelum delete!
-- DELETE orders older than 30 days
DELETE FROM order_items WHERE order_id IN (
    SELECT id FROM orders WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)
);

DELETE FROM orders WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);

-- Check how many deleted
SELECT COUNT(*) FROM orders;

EXIT;
```

### Verify Database Integrity

```bash
mysql -u chopchop_user -p restaurant_db -e "CHECK TABLE menu_items;"
mysql -u chopchop_user -p restaurant_db -e "CHECK TABLE orders;"
mysql -u chopchop_user -p restaurant_db -e "CHECK TABLE order_items;"
```

---

## Database Operations Cheat Sheet

### Quick Reference

```bash
# Login to database
mysql -u chopchop_user -p restaurant_db

# List all tables
SHOW TABLES;

# View table structure
DESCRIBE menu_items;

# Count records
SELECT COUNT(*) FROM orders;

# View recent orders
SELECT * FROM orders ORDER BY created_at DESC LIMIT 10;

# View all cashiers
SELECT id, username, full_name, role FROM cashiers;

# Check Flyway migration history
SELECT * FROM flyway_schema_history;
```

### Common Queries

```sql
-- Total revenue
SELECT SUM(total_price) as total_revenue FROM orders WHERE status = 'COMPLETED';

-- Orders today
SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE();

-- Popular menu items
SELECT
    m.name,
    COUNT(oi.id) as order_count,
    SUM(oi.quantity) as total_quantity
FROM order_items oi
JOIN menu_items m ON oi.menu_item_id = m.id
GROUP BY m.id, m.name
ORDER BY order_count DESC
LIMIT 10;

-- Cashier performance
SELECT
    c.full_name,
    COUNT(o.id) as orders_processed,
    SUM(o.total_price) as total_sales
FROM orders o
JOIN cashiers c ON o.cashier_id = c.id
WHERE o.status = 'COMPLETED'
GROUP BY c.id, c.full_name
ORDER BY total_sales DESC;
```

---

## Troubleshooting

### Database Connection Failed

```bash
# Check MySQL running
sudo systemctl status mysql

# Restart MySQL
sudo systemctl restart mysql

# Check .env configuration
cat /opt/menu-ordering-app/.env

# Test connection
mysql -u chopchop_user -p restaurant_db
```

### Flyway Migration Failed

```bash
# Check migration history
mysql -u chopchop_user -p restaurant_db -e "SELECT * FROM flyway_schema_history;"

# If stuck with failed migration, repair:
# 1. Stop application
sudo systemctl stop chopchop

# 2. Fix the issue in migration file or manually in DB

# 3. Mark migration as success (if you fixed manually)
mysql -u chopchop_user -p restaurant_db -e "
    UPDATE flyway_schema_history
    SET success = 1
    WHERE version = 'VERSION_NUMBER';
"

# 4. Restart application
sudo systemctl start chopchop
```

### Database Locked / Too Many Connections

```bash
# Check active connections
mysql -u root -p -e "SHOW PROCESSLIST;"

# Kill specific connection
mysql -u root -p -e "KILL CONNECTION_ID;"

# Restart application
sudo systemctl restart chopchop
```

### Disk Full

```bash
# Check disk space
df -h

# Check database size
du -sh /var/lib/mysql/restaurant_db/

# Cleanup old backups
cd /opt/menu-ordering-app/backups
ls -lht
rm backup_OLD_DATE_*.sql

# Cleanup old orders (if applicable)
# See "Clear Old Orders" section above
```

---

## Best Practices

### 1. Regular Backups

```bash
# Setup cron for daily backup (recommended)
crontab -e

# Add:
0 3 * * * /opt/menu-ordering-app/scripts/backup_database.sh >> /var/log/chopchop-backup.log 2>&1
```

### 2. Before Major Changes

```bash
# Always backup before:
# - Updating application
# - Running manual SQL
# - Changing database schema
# - Testing new features

bash scripts/backup_database.sh
```

### 3. Test Restore

Periodically test restore process:

```bash
# 1. Create test database
mysql -u root -p -e "CREATE DATABASE restaurant_db_test;"

# 2. Restore to test database
mysql -u chopchop_user -p restaurant_db_test < backups/latest_backup.sql

# 3. Verify data
mysql -u chopchop_user -p restaurant_db_test -e "SELECT COUNT(*) FROM orders;"

# 4. Cleanup
mysql -u root -p -e "DROP DATABASE restaurant_db_test;"
```

### 4. Monitor Database Size

```bash
# Create monitoring script
nano /opt/menu-ordering-app/scripts/check_db_size.sh
```

```bash
#!/bin/bash
DB_SIZE=$(mysql -u chopchop_user -p$DB_PASSWORD -se "
    SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 2)
    FROM information_schema.tables
    WHERE table_schema = 'restaurant_db';
")

echo "Database size: ${DB_SIZE} MB"

# Alert if > 1GB
if (( $(echo "$DB_SIZE > 1024" | bc -l) )); then
    echo "WARNING: Database size exceeds 1GB!"
fi
```

### 5. Security

```bash
# Strong database password (check .env)
cat /opt/menu-ordering-app/.env

# Restrict .env permissions
chmod 600 /opt/menu-ordering-app/.env

# Backup permissions (only owner can read)
chmod 600 /opt/menu-ordering-app/backups/*.sql
```

---

## Emergency Recovery

Jika database corrupt atau ada masalah serius:

### 1. Stop Everything

```bash
sudo systemctl stop chopchop
sudo systemctl stop mysql
```

### 2. Backup Current State (Even if Corrupt)

```bash
sudo cp -r /var/lib/mysql/restaurant_db /var/lib/mysql/restaurant_db_corrupt_backup
```

### 3. Try MySQL Repair

```bash
sudo systemctl start mysql
mysql -u root -p

# In MySQL
USE restaurant_db;
REPAIR TABLE menu_items;
REPAIR TABLE orders;
REPAIR TABLE order_items;
REPAIR TABLE cashiers;
```

### 4. If Repair Fails - Full Reset

```bash
# Use reset script
cd /opt/menu-ordering-app
bash scripts/reset_database.sh
```

### 5. Restore from Last Good Backup

```bash
# Find latest backup
ls -lht backups/

# Restore
mysql -u chopchop_user -p restaurant_db < backups/backup_LATEST.sql
```

---

## Summary Commands

```bash
# Reset database (fresh start)
bash scripts/reset_database.sh

# Backup database
bash scripts/backup_database.sh

# Restore from backup
mysql -u chopchop_user -p restaurant_db < backups/backup_FILE.sql

# Check database size
mysql -u chopchop_user -p -e "
    SELECT table_schema, ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
    FROM information_schema.tables
    WHERE table_schema = 'restaurant_db';
"

# Optimize all tables
mysql -u chopchop_user -p restaurant_db -e "
    OPTIMIZE TABLE menu_items, orders, order_items, cashiers;
"
```

---

**Database management made easy!** 🗄️

Untuk pertanyaan lebih lanjut, refer to MySQL documentation atau tanya di Stack Overflow.
