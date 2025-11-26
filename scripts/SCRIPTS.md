# Deployment Scripts

Collection of scripts to facilitate ChopChop application deployment to VPS.

## Scripts Available

### 1. setup_vps.sh
**Function**: Initial VPS setup (install all dependencies)

**Usage**:
```bash
# Upload script to VPS
scp setup_vps.sh root@YOUR_VPS_IP:/root/

# SSH to VPS
ssh root@YOUR_VPS_IP

# Run script
sudo bash setup_vps.sh
```

**What gets installed**:
- Java 21
- MySQL 8.0
- Maven
- Git
- Nginx
- UFW Firewall
- Creates user `chopchop`
- Setup database
- Creates directory `/opt/Menu-Ordering`

### 2. create_service.sh
**Function**: Create systemd service for application auto-start

**Usage**:
```bash
cd /opt/Menu-Ordering
sudo bash scripts/create_service.sh
```

**Result**:
- Service file at `/etc/systemd/system/chopchop.service`
- Enable auto-start on boot
- Support auto-restart if crash

**Commands after service creation**:
```bash
sudo systemctl start chopchop    # Start
sudo systemctl stop chopchop     # Stop
sudo systemctl restart chopchop  # Restart
sudo systemctl status chopchop   # Status
sudo journalctl -u chopchop -f   # Logs
```

### 3. deploy.sh
**Function**: Deploy/update application (pull, build, restart)

**Usage**:
```bash
cd /opt/Menu-Ordering
bash scripts/deploy.sh
```

**What it does**:
1. Pull latest code from Git
2. Build JAR with Maven
3. Stop service
4. Start service with new JAR
5. Verify service running

**When to use**:
- After code updates
- After bug fixes
- Deploy new version

### 4. backup_database.sh
**Function**: Backup MySQL database

**Usage**:
```bash
bash scripts/backup_database.sh
```

**Result**:
- Backup in `/opt/Menu-Ordedring/backups/`
- Format: `backup_restaurant_db_YYYYMMDD_HHMMSS.sql`
- Auto cleanup, keep last 7 backups

**Restore backup**:
```bash
mysql -u chopchop_user -p restaurant_db < backups/backup_restaurant_db_20241124_120000.sql
```

### 5. reset_database.sh
**Function**: Reset database to initial state (fresh start)

**WARNING**: This script will DELETE ALL DATA!

**Usage**:
```bash
bash scripts/reset_database.sh
```

**What it does**:
1. Backup database before reset (safety)
2. Stop application
3. Drop old database
4. Create new database
5. Restart application (Flyway auto-run migrations)
6. Default data inserted automatically

**When to use**:
- Database is messy with test data
- Want to return to default menu & cashiers
- Migration error that corrupted database
- Testing fresh installation
- Presentation preparation (clean state)

**Result**:
- Fresh database with default data
- Default menu items (from migrations)
- Default cashiers: admin, kasir1, kasir2
- Password: password123 (change after presentation!)
- Backup at `/opt/Menu-Ordering/backups/backup_before_reset_*.sql`

### 6. webhook_deploy.sh
**Function**: Automatic deployment triggered by GitHub webhook

**Usage**:
```bash
# Usually triggered automatically by GitHub webhook
# Can also be run manually:
sudo bash scripts/webhook_deploy.sh
```

**What it does**:
1. Create backup of current JAR
2. Pull latest code from Git (force update to origin/main)
3. Build application with Maven
4. Restart chopchop service
5. Wait and verify service is running
6. Cleanup old backups (keeps last 5)

**When to use**:
- Automatic deployment via GitHub webhook
- Manual deployment with automatic rollback on failure
- CI/CD pipeline integration

**Features**:
- Logs all output to `/var/log/chopchop-deploy.log`
- Automatic rollback if build fails
- Verification of service health after deployment
- Auto-cleanup of old backup JARs

**Check deployment logs**:
```bash
tail -f /var/log/chopchop-deploy.log
```

**Rollback on failure**:
- If build fails, automatically restores from backup
- If service fails to start, check logs and manually restore if needed

## Webhook Setup (Optional - CI/CD)

This section explains how to set up automatic deployment when you push to GitHub.

### Why Separate `webhook` User?

**Security Best Practice**: The `webhook` user has limited permissions and can only:
- Execute the deployment script
- Restart the chopchop service
- Access deployment logs

This prevents potential security risks if the webhook endpoint is compromised.

### Prerequisites

- VPS setup completed (including `webhook` user creation)
- Application already deployed and running
- GitHub repository set up

### Step 1: Setup Webhook Endpoint

We'll use a simple webhook listener service. Install `webhook` package:

```bash
# SSH to VPS as root or sudo user
ssh root@YOUR_VPS_IP

# Install webhook package
apt install -y webhook

# Create webhook directory
mkdir -p /opt/webhooks
```

### Step 2: Create Webhook Configuration

```bash
# Create hooks.json file
cat > /opt/webhooks/hooks.json << 'EOF'
[
  {
    "id": "chopchop-deploy",
    "execute-command": "/opt/Menu-Ordering/scripts/webhook_deploy.sh",
    "command-working-directory": "/opt/Menu-Ordering",
    "response-message": "Deployment started",
    "trigger-rule": {
      "match": {
        "type": "payload-hash-sha256",
        "secret": "YOUR_WEBHOOK_SECRET_HERE",
        "parameter": {
          "source": "header",
          "name": "X-Hub-Signature-256"
        }
      }
    },
    "pass-arguments-to-command": [],
    "pass-environment-to-command": [],
    "run-as-user": "webhook"
  }
]
EOF

# Replace YOUR_WEBHOOK_SECRET_HERE with a secure random string
# Generate one with: openssl rand -hex 32
```

### Step 3: Create Webhook Service

```bash
# Create systemd service for webhook listener
cat > /etc/systemd/system/webhook-listener.service << 'EOF'
[Unit]
Description=Webhook Listener for ChopChop Deployments
After=network.target

[Service]
Type=simple
User=webhook
ExecStart=/usr/bin/webhook -hooks /opt/webhooks/hooks.json -port 9000 -verbose
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd and enable service
systemctl daemon-reload
systemctl enable webhook-listener
systemctl start webhook-listener
```

### Step 4: Configure Firewall

```bash
# Allow webhook port (9000)
ufw allow 9000/tcp
```

### Step 5: Setup GitHub Webhook

1. Go to your GitHub repository
2. Navigate to **Settings** → **Webhooks** → **Add webhook**
3. Configure:
   - **Payload URL**: `http://YOUR_VPS_IP:9000/hooks/chopchop-deploy`
   - **Content type**: `application/json`
   - **Secret**: Same secret you used in `hooks.json`
   - **Events**: Select "Just the push event"
   - **Active**: Check the box

4. Click **Add webhook**

### Step 6: Set Permissions

```bash
# Give webhook user access to application directory
chown -R chopchop:chopchop /opt/Menu-Ordering
chmod -R g+rw /opt/Menu-Ordering

# Add webhook user to chopchop group (if not already done)
usermod -aG chopchop webhook

# Make deployment script executable
chmod +x /opt/Menu-Ordering/scripts/webhook_deploy.sh

# Ensure webhook user can write to log
touch /var/log/chopchop-deploy.log
chown webhook:webhook /var/log/chopchop-deploy.log
```

### Step 7: Test the Setup

**Method 1: Push to GitHub**
```bash
# Make a small change and push
git add .
git commit -m "test: trigger webhook deployment"
git push origin main
```

**Method 2: Manual Trigger**
```bash
# SSH as webhook user
sudo -u webhook bash /opt/Menu-Ordering/scripts/webhook_deploy.sh
```

**Method 3: Test GitHub Webhook**
1. Go to GitHub repository → Settings → Webhooks
2. Click on your webhook
3. Scroll to "Recent Deliveries"
4. Click "Redeliver" on any delivery

### Monitoring Webhook Deployments

```bash
# Watch deployment logs in real-time
tail -f /var/log/chopchop-deploy.log

# Check webhook listener status
systemctl status webhook-listener

# Check webhook listener logs
journalctl -u webhook-listener -f

# Check application status after deployment
sudo systemctl status chopchop
```

### Troubleshooting Webhooks

**Webhook not triggering:**
```bash
# Check if webhook listener is running
systemctl status webhook-listener

# Check firewall
ufw status | grep 9000

# Test endpoint manually
curl -X POST http://YOUR_VPS_IP:9000/hooks/chopchop-deploy
```

**Permission errors:**
```bash
# Verify webhook user permissions
sudo -u webhook ls -la /opt/Menu-Ordering

# Check sudoers configuration
cat /etc/sudoers.d/webhook

# Re-apply permissions
chown -R chopchop:chopchop /opt/Menu-Ordering
chmod -R g+rw /opt/Menu-Ordering
```

**Deployment fails:**
```bash
# Check deployment logs
tail -n 100 /var/log/chopchop-deploy.log

# Check application logs
sudo journalctl -u chopchop -n 50

# Test script manually as webhook user
sudo -u webhook bash /opt/Menu-Ordering/scripts/webhook_deploy.sh
```

### Security Notes for Webhooks

1. **Use HTTPS in production**: Set up SSL/TLS with Let's Encrypt
2. **Keep webhook secret secure**: Never commit to Git
3. **Limit webhook user permissions**: Already configured in setup script
4. **Monitor webhook logs**: Regular checks for suspicious activity
5. **Use Nginx reverse proxy**: For additional security layer (optional)

### Optional: Nginx Reverse Proxy for Webhooks

For production, it's recommended to use Nginx as a reverse proxy with HTTPS:

```bash
# Create Nginx configuration
cat > /etc/nginx/sites-available/webhook << 'EOF'
server {
    listen 80;
    server_name YOUR_DOMAIN_OR_IP;

    location /hooks/ {
        proxy_pass http://localhost:9000/hooks/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
EOF

# Enable site
ln -s /etc/nginx/sites-available/webhook /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx

# Then use: http://YOUR_DOMAIN/hooks/chopchop-deploy in GitHub
```

## Quick Start Guide

### First Time Setup

1. **Upload setup script to VPS**:
```bash
scp scripts/setup_vps.sh root@YOUR_VPS_IP:/root/
```

2. **SSH to VPS and run setup**:
```bash
ssh root@YOUR_VPS_IP
sudo bash setup_vps.sh
```

3. **Clone application**:
```bash
cd /opt
sudo git clone https://github.com/YOUR_USERNAME/Menu-Ordering.git
sudo chown -R chopchop:chopchop Menu-Ordering
```

4. **Build application**:
```bash
cd /opt/Menu-Ordering
mvn clean package -DskipTests
```

5. **Create service**:
```bash
sudo bash scripts/create_service.sh
```

6. **Start application**:
```bash
sudo systemctl start chopchop
```

7. **Verify**:
```bash
sudo systemctl status chopchop
```

### Update Application

```bash
# SSH to VPS
ssh chopchop@YOUR_VPS_IP

# Run deploy script
cd /opt/Menu-Ordering
bash scripts/deploy.sh
```

### Daily Maintenance

**Backup database (recommended: daily)**:
```bash
bash scripts/backup_database.sh
```

**Reset database (if needed)**:
```bash
# WARNING: This deletes all data!
bash scripts/reset_database.sh
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
0 2 * * * /opt/Menu-Ordering/scripts/backup_database.sh >> /var/log/chopchop-backup.log 2>&1
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
cat /opt/Menu-Ordering/.env

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
chown -R chopchop:chopchop /opt/Menu-Ordering
```

## Security Notes

1. **Never commit .env to Git** - contains passwords
2. **Use strong passwords** for database
3. **Regular backups** - run daily
4. **Keep system updated**: `sudo apt update && sudo apt upgrade`
5. **Monitor logs** for suspicious activity
6. **Change default app passwords** after deployment

## Errors

If there are errors:
1. Check logs: `sudo journalctl -u chopchop -n 100`
2. Check service status: `sudo systemctl status chopchop`
3. Check database: `mysql -u chopchop_user -p restaurant_db`