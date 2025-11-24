# Deployment Scripts

Kumpulan script untuk memudahkan deployment aplikasi ChopChop ke VPS.

## Scripts Available

### 1. setup_vps.sh
**Fungsi**: Setup awal VPS (install semua dependencies)

**Usage**:
```bash
# Upload script ke VPS
scp setup_vps.sh root@YOUR_VPS_IP:/root/

# SSH ke VPS
ssh root@YOUR_VPS_IP

# Jalankan script
sudo bash setup_vps.sh
```

**Yang diinstall**:
- Java 21
- MySQL 8.0
- Maven
- Git
- Nginx
- UFW Firewall
- Membuat user `chopchop`
- Setup database
- Membuat direktori `/opt/menu-ordering-app`

### 2. create_service.sh
**Fungsi**: Membuat systemd service untuk auto-start aplikasi

**Usage**:
```bash
cd /opt/menu-ordering-app
sudo bash scripts/create_service.sh
```

**Hasil**:
- Service file di `/etc/systemd/system/chopchop.service`
- Enable auto-start on boot
- Support auto-restart jika crash

**Commands setelah service dibuat**:
```bash
sudo systemctl start chopchop    # Start
sudo systemctl stop chopchop     # Stop
sudo systemctl restart chopchop  # Restart
sudo systemctl status chopchop   # Status
sudo journalctl -u chopchop -f   # Logs
```

### 3. deploy.sh
**Fungsi**: Deploy/update aplikasi (pull, build, restart)

**Usage**:
```bash
cd /opt/menu-ordering-app
bash scripts/deploy.sh
```

**Yang dilakukan**:
1. Pull latest code dari Git
2. Build JAR dengan Maven
3. Stop service
4. Start service dengan JAR baru
5. Verify service running

**Kapan digunakan**:
- Setelah ada update code
- Setelah fix bug
- Deploy versi baru

### 4. backup_database.sh
**Fungsi**: Backup database MySQL

**Usage**:
```bash
bash scripts/backup_database.sh
```

**Hasil**:
- Backup di `/opt/menu-ordering-app/backups/`
- Format: `backup_restaurant_db_YYYYMMDD_HHMMSS.sql`
- Auto cleanup, keep last 7 backups

**Restore backup**:
```bash
mysql -u chopchop_user -p restaurant_db < backups/backup_restaurant_db_20241124_120000.sql
```

## Quick Start Guide

### First Time Setup

1. **Upload setup script ke VPS**:
```bash
scp scripts/setup_vps.sh root@YOUR_VPS_IP:/root/
```

2. **SSH ke VPS dan run setup**:
```bash
ssh root@YOUR_VPS_IP
sudo bash setup_vps.sh
```

3. **Clone aplikasi**:
```bash
cd /opt
sudo git clone https://github.com/YOUR_USERNAME/menu-ordering-app.git
sudo chown -R chopchop:chopchop menu-ordering-app
```

4. **Build aplikasi**:
```bash
cd /opt/menu-ordering-app
mvn clean package -DskipTests
```

5. **Create service**:
```bash
sudo bash scripts/create_service.sh
```

6. **Start aplikasi**:
```bash
sudo systemctl start chopchop
```

7. **Verify**:
```bash
sudo systemctl status chopchop
```

### Update Aplikasi

```bash
# SSH ke VPS
ssh chopchop@YOUR_VPS_IP

# Run deploy script
cd /opt/menu-ordering-app
bash scripts/deploy.sh
```

### Daily Maintenance

**Backup database (recommended: daily)**:
```bash
bash scripts/backup_database.sh
```

**Check logs**:
```bash
sudo journalctl -u chopchop -f
```

**Check status**:
```bash
sudo systemctl status chopchop
```

**Restart if needed**:
```bash
sudo systemctl restart chopchop
```

## Automated Backups (Cron)

Setup automatic daily backups:

```bash
# Edit crontab
crontab -e

# Add this line (backup every day at 2 AM)
0 2 * * * /opt/menu-ordering-app/scripts/backup_database.sh >> /var/log/chopchop-backup.log 2>&1
```

## Troubleshooting

### Script Permission Denied
```bash
chmod +x scripts/*.sh
```

### Service Won't Start
```bash
# Check logs
sudo journalctl -u chopchop -n 100

# Check JAR exists
ls -lh target/menu-ordering-app-0.0.1-SNAPSHOT.jar

# Rebuild
mvn clean package -DskipTests
```

### Database Connection Error
```bash
# Test connection
mysql -u chopchop_user -p restaurant_db

# Check .env file
cat /opt/menu-ordering-app/.env

# Restart MySQL
sudo systemctl restart mysql
```

## File Permissions

Correct permissions:
```bash
# Scripts
chmod +x scripts/*.sh

# .env file (sensitive)
chmod 600 .env

# Application directory
chown -R chopchop:chopchop /opt/menu-ordering-app
```

## Security Notes

1. **Never commit .env to Git** - contains passwords
2. **Use strong passwords** for database
3. **Regular backups** - run daily
4. **Keep system updated**: `sudo apt update && sudo apt upgrade`
5. **Monitor logs** for suspicious activity
6. **Change default app passwords** after deployment

## Support

Jika ada error:
1. Check logs: `sudo journalctl -u chopchop -n 100`
2. Check service status: `sudo systemctl status chopchop`
3. Check database: `mysql -u chopchop_user -p restaurant_db`
4. Refer to: `/opt/menu-ordering-app/HOSTINGER_DEPLOYMENT.md`
