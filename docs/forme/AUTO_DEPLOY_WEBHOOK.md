# Auto-Deploy dengan Git Webhook

Panduan setup auto-deploy menggunakan Git Webhook, sehingga setiap kali push ke GitHub, VPS otomatis pull dan rebuild aplikasi.

---

## Cara Kerja

```
1. Push code ke GitHub
   ↓
2. GitHub kirim webhook ke VPS
   ↓
3. VPS terima webhook
   ↓
4. VPS jalankan script deploy otomatis
   ↓
5. Pull → Build → Restart
```

**Hasilnya**: Push code → 2-3 menit kemudian aplikasi di VPS sudah update!

---

## Kelebihan & Kekurangan

### Kelebihan ✅
- Auto-deploy seperti Railway/Render
- Push code langsung live
- Tidak perlu SSH ke VPS setiap update
- Cocok untuk development yang sering update

### Kekurangan ❌
- Setup lebih kompleks
- Butuh port tambahan exposed (webhook receiver)
- Risk: jika ada bug, langsung deploy ke production
- Tidak ada staging/approval step

---

## Prerequisites

- VPS sudah setup (sudah jalankan `setup_vps.sh`)
- Aplikasi sudah deploy dengan `deploy.sh`
- Repository di GitHub (public/private OK)
- GitHub account dengan access token

---

## Langkah 1: Install Webhook Receiver di VPS

### 1.1 Install webhook package

```bash
# SSH ke VPS
ssh chopchop@YOUR_VPS_IP

# Install webhook (lightweight webhook receiver)
sudo apt update
sudo apt install -y webhook
```

### 1.2 Buat deploy script untuk webhook

```bash
cd /opt/menu-ordering-app
sudo nano scripts/webhook_deploy.sh
```

Copy script ini:

```bash
#!/bin/bash

# ChopChop - Webhook Auto Deploy Script
# Triggered by GitHub webhook

set -e

# Logging
LOG_FILE="/var/log/chopchop-deploy.log"
exec > >(tee -a "$LOG_FILE") 2>&1

echo "=========================================="
echo "Auto-Deploy triggered at $(date)"
echo "=========================================="

# Navigate to app directory
cd /opt/menu-ordering-app

# Backup current version (just in case)
echo "[1/5] Creating backup..."
cp target/menu-ordering-app-0.0.1-SNAPSHOT.jar target/menu-ordering-app-backup-$(date +%Y%m%d_%H%M%S).jar || true

# Pull latest code
echo "[2/5] Pulling latest code..."
sudo -u chopchop git fetch origin
sudo -u chopchop git reset --hard origin/main  # Force update to latest

# Build application
echo "[3/5] Building application..."
sudo -u chopchop mvn clean package -DskipTests

# Check if build successful
if [ ! -f "target/menu-ordering-app-0.0.1-SNAPSHOT.jar" ]; then
    echo "ERROR: Build failed! JAR not found."
    echo "Restoring from backup..."
    cp target/menu-ordering-app-backup-*.jar target/menu-ordering-app-0.0.1-SNAPSHOT.jar
    exit 1
fi

# Restart service
echo "[4/5] Restarting service..."
sudo systemctl restart chopchop

# Wait for service to start
sleep 5

# Verify service is running
echo "[5/5] Verifying service..."
if systemctl is-active --quiet chopchop; then
    echo "SUCCESS: Application deployed and running!"

    # Cleanup old backups (keep last 5)
    cd target
    ls -t menu-ordering-app-backup-*.jar 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true

    exit 0
else
    echo "ERROR: Service failed to start!"
    echo "Check logs: sudo journalctl -u chopchop -n 50"
    exit 1
fi
```

**Simpan**: Ctrl+O, Enter, Ctrl+X

```bash
# Make executable
sudo chmod +x scripts/webhook_deploy.sh

# Test run (manual)
sudo bash scripts/webhook_deploy.sh
```

### 1.3 Buat webhook configuration

```bash
sudo mkdir -p /etc/webhook
sudo nano /etc/webhook/hooks.json
```

Copy konfigurasi ini:

```json
[
  {
    "id": "chopchop-deploy",
    "execute-command": "/opt/menu-ordering-app/scripts/webhook_deploy.sh",
    "command-working-directory": "/opt/menu-ordering-app",
    "response-message": "Deployment triggered successfully",
    "trigger-rule": {
      "and": [
        {
          "match": {
            "type": "payload-hash-sha256",
            "secret": "YOUR_WEBHOOK_SECRET_HERE",
            "parameter": {
              "source": "header",
              "name": "X-Hub-Signature-256"
            }
          }
        },
        {
          "match": {
            "type": "value",
            "value": "refs/heads/main",
            "parameter": {
              "source": "payload",
              "name": "ref"
            }
          }
        }
      ]
    }
  }
]
```

**PENTING**: Ganti `YOUR_WEBHOOK_SECRET_HERE` dengan secret random string yang kuat!

Generate secret:
```bash
# Generate random secret
openssl rand -hex 32
```

Copy output dan ganti di `hooks.json`.

**Simpan**: Ctrl+O, Enter, Ctrl+X

### 1.4 Buat systemd service untuk webhook

```bash
sudo nano /etc/systemd/system/webhook.service
```

Copy ini:

```ini
[Unit]
Description=Webhook receiver for GitHub
After=network.target

[Service]
ExecStart=/usr/bin/webhook -hooks /etc/webhook/hooks.json -ip "0.0.0.0" -port 9000 -verbose
Restart=always
RestartSec=10
User=root

[Install]
WantedBy=multi-user.target
```

**Simpan**: Ctrl+O, Enter, Ctrl+X

```bash
# Enable and start webhook service
sudo systemctl daemon-reload
sudo systemctl enable webhook
sudo systemctl start webhook

# Check status
sudo systemctl status webhook
```

Output harus: `active (running)`

### 1.5 Open firewall port

```bash
# Allow webhook port
sudo ufw allow 9000/tcp

# Check
sudo ufw status
```

---

## Langkah 2: Setup Webhook di GitHub

### 2.1 Get VPS IP

```bash
# Di VPS, cek IP public
curl ifconfig.me
```

Copy IP address.

### 2.2 Configure webhook di GitHub

1. **Buka repository** di GitHub
2. **Settings → Webhooks → Add webhook**
3. **Payload URL**:
   ```
   http://YOUR_VPS_IP:9000/hooks/chopchop-deploy
   ```
4. **Content type**: `application/json`
5. **Secret**: Paste secret yang sama dengan di `hooks.json`
6. **Events**: Pilih `Just the push event`
7. **Active**: ✅ Checklist
8. **Add webhook**

### 2.3 Test webhook

Di GitHub webhook settings:
- Scroll ke bawah ke "Recent Deliveries"
- Klik **Redeliver** untuk test
- Response harus: `200 OK` dan message: "Deployment triggered successfully"

---

## Langkah 3: Test End-to-End

### 3.1 Make a change

Di local machine:

```bash
# Edit file apapun (contoh: README.md)
echo "Test auto-deploy" >> README.md

# Commit and push
git add README.md
git commit -m "Test webhook auto-deploy"
git push origin main
```

### 3.2 Monitor deployment

Di VPS:

```bash
# Watch deployment logs real-time
sudo tail -f /var/log/chopchop-deploy.log
```

Atau:

```bash
# Watch webhook logs
sudo journalctl -u webhook -f
```

### 3.3 Verify

Dalam ~2-3 menit, aplikasi akan auto-restart dengan code terbaru.

```bash
# Check application logs
sudo journalctl -u chopchop -f

# Check if changes are reflected
cd /opt/menu-ordering-app
cat README.md  # Should see "Test auto-deploy"
```

---

## Monitoring & Troubleshooting

### View deployment logs

```bash
# View all deployment logs
sudo cat /var/log/chopchop-deploy.log

# View recent deployments
sudo tail -n 100 /var/log/chopchop-deploy.log

# Watch live
sudo tail -f /var/log/chopchop-deploy.log
```

### View webhook receiver logs

```bash
# Real-time
sudo journalctl -u webhook -f

# Last 50 lines
sudo journalctl -u webhook -n 50
```

### Check webhook service status

```bash
sudo systemctl status webhook
```

### Test webhook manually

```bash
# From local machine (test webhook endpoint)
curl -X POST http://YOUR_VPS_IP:9000/hooks/chopchop-deploy \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: sha256=test" \
  -d '{"ref":"refs/heads/main"}'
```

### Common Issues

#### 1. Webhook returns 403/401
**Problem**: Secret tidak match

**Solution**:
```bash
# Regenerate secret
openssl rand -hex 32

# Update di /etc/webhook/hooks.json
sudo nano /etc/webhook/hooks.json

# Update di GitHub webhook settings

# Restart webhook service
sudo systemctl restart webhook
```

#### 2. Deployment triggered but build fails
**Problem**: Build error (missing dependencies, syntax error, dll)

**Solution**:
```bash
# Check deployment logs
sudo tail -n 200 /var/log/chopchop-deploy.log

# Manual test build
cd /opt/menu-ordering-app
mvn clean package -DskipTests
```

#### 3. Port 9000 tidak bisa diakses
**Problem**: Firewall atau port blocked

**Solution**:
```bash
# Check UFW
sudo ufw status | grep 9000

# Allow port
sudo ufw allow 9000/tcp

# Check if webhook listening
sudo netstat -tulpn | grep 9000
```

#### 4. Build berhasil tapi service tidak restart
**Problem**: Permission issue

**Solution**:
```bash
# Give webhook script sudo access without password for systemctl
sudo visudo

# Add this line at the end:
# chopchop ALL=(ALL) NOPASSWD: /bin/systemctl restart chopchop
```

---

## Security Considerations

### 1. Use HTTPS (Recommended untuk Production)

Webhook saat ini pakai HTTP. Untuk production, gunakan HTTPS:

```bash
# Install certbot for SSL
sudo apt install -y certbot

# Get SSL certificate (requires domain)
sudo certbot certonly --standalone -d yourdomain.com

# Configure webhook to use SSL
sudo nano /etc/systemd/system/webhook.service

# Update ExecStart:
# ExecStart=/usr/bin/webhook -hooks /etc/webhook/hooks.json -ip "0.0.0.0" -port 9000 -cert /etc/letsencrypt/live/yourdomain.com/fullchain.pem -key /etc/letsencrypt/live/yourdomain.com/privkey.pem -verbose
```

### 2. Restrict IP (Optional)

Hanya allow request dari GitHub IPs:

```bash
# Get GitHub webhook IPs
curl https://api.github.com/meta | jq .hooks

# Add UFW rules (example)
sudo ufw allow from 140.82.112.0/20 to any port 9000
sudo ufw allow from 143.55.64.0/20 to any port 9000
```

### 3. Use Strong Secret

```bash
# Generate strong secret (32 bytes)
openssl rand -hex 32
```

### 4. Limit Deploy Frequency

Untuk prevent spam deploy, edit `hooks.json`:

```json
"trigger-rule-mismatch-http-response-code": 400,
"pass-arguments-to-command": [],
"pass-environment-to-command": [],
"response-headers": [
  {
    "name": "X-Rate-Limit",
    "value": "1 request per minute"
  }
]
```

---

## Rollback Strategy

Jika auto-deploy gagal dan perlu rollback:

### Quick Rollback

```bash
# SSH ke VPS
ssh chopchop@YOUR_VPS_IP
cd /opt/menu-ordering-app

# List available backups
ls -lht target/menu-ordering-app-backup-*.jar

# Stop service
sudo systemctl stop chopchop

# Restore from backup (pilih timestamp yang benar)
cp target/menu-ordering-app-backup-20241124_120000.jar target/menu-ordering-app-0.0.1-SNAPSHOT.jar

# Start service
sudo systemctl start chopchop

# Verify
sudo systemctl status chopchop
```

### Git Rollback

```bash
cd /opt/menu-ordering-app

# See recent commits
git log --oneline -10

# Rollback to specific commit
git reset --hard COMMIT_HASH

# Rebuild and restart
bash scripts/deploy.sh
```

---

## Advanced: Staging Environment

Setup staging branch untuk test sebelum deploy ke production:

### 1. Create staging branch

```bash
git checkout -b staging
git push origin staging
```

### 2. Add staging webhook config

```bash
sudo nano /etc/webhook/hooks.json
```

Add second webhook:

```json
[
  {
    "id": "chopchop-deploy-production",
    "execute-command": "/opt/menu-ordering-app/scripts/webhook_deploy.sh",
    ...
    "trigger-rule": {
      "match": {
        "value": "refs/heads/main",
        ...
      }
    }
  },
  {
    "id": "chopchop-deploy-staging",
    "execute-command": "/opt/menu-ordering-app/scripts/webhook_deploy_staging.sh",
    ...
    "trigger-rule": {
      "match": {
        "value": "refs/heads/staging",
        ...
      }
    }
  }
]
```

### 3. Workflow

```
1. Develop → push ke staging branch
2. Test di staging environment
3. Jika OK → merge staging ke main
4. Auto-deploy ke production
```

---

## Comparison: Manual vs Auto-Deploy

| Aspect | Manual (deploy.sh) | Auto (Webhook) |
|--------|-------------------|----------------|
| **Setup** | Mudah ✅ | Kompleks ⚠️ |
| **Deployment** | SSH + run script | Push code aja ✅ |
| **Speed** | ~2-3 menit | ~2-3 menit |
| **Control** | Full control ✅ | Less control ⚠️ |
| **Risk** | Low (manual check) | Higher (auto) |
| **Best for** | Production, careful deployment | Development, rapid iteration |

---

## Rekomendasi

### Gunakan Auto-Deploy (Webhook) jika:
- ✅ Masih development/testing phase
- ✅ Sering update code (multiple times per day)
- ✅ Ingin workflow seperti Railway/Vercel
- ✅ Team collaboration (multiple developers)

### Gunakan Manual Deploy (deploy.sh) jika:
- ✅ Production environment
- ✅ Jarang update (1-2x per minggu)
- ✅ Perlu review sebelum deploy
- ✅ Solo developer / small team

---

## Uninstall Webhook (Kembali ke Manual)

Jika ingin kembali ke manual deploy:

```bash
# Stop webhook service
sudo systemctl stop webhook
sudo systemctl disable webhook

# Remove webhook config
sudo rm -rf /etc/webhook

# Close firewall port
sudo ufw delete allow 9000/tcp

# Remove webhook from GitHub repository settings
```

Aplikasi tetap jalan normal, tinggal pakai `deploy.sh` untuk update.

---

## Monitoring Dashboard (Bonus)

Setup simple monitoring untuk track deployments:

```bash
# Install nginx untuk serve logs
sudo apt install -y nginx

# Create log viewer
sudo nano /var/www/html/deploy-logs.html
```

Copy HTML ini:

```html
<!DOCTYPE html>
<html>
<head>
    <title>ChopChop Deployment Logs</title>
    <meta http-equiv="refresh" content="10">
</head>
<body>
    <h1>Recent Deployments</h1>
    <pre id="logs">Loading...</pre>
    <script>
        fetch('/deploy-logs.txt')
            .then(response => response.text())
            .then(data => document.getElementById('logs').textContent = data);
    </script>
</body>
</html>
```

```bash
# Create symlink to logs
sudo ln -s /var/log/chopchop-deploy.log /var/www/html/deploy-logs.txt

# Access via browser:
# http://YOUR_VPS_IP/deploy-logs.html
```

---

## FAQ

**Q: Apakah webhook aman?**
A: Ya, jika menggunakan secret dan ideally HTTPS. GitHub sign semua request dengan secret.

**Q: Berapa lama deployment?**
A: ~2-3 menit dari push sampai live. Sama seperti manual deploy.

**Q: Apa yang terjadi jika build gagal?**
A: Script akan restore dari backup JAR terakhir yang sukses. Aplikasi tetap jalan dengan versi lama.

**Q: Bisa deploy branch selain main?**
A: Bisa, tinggal ubah di `hooks.json` bagian `refs/heads/main` ke branch yang diinginkan.

**Q: Webhook tidak trigger?**
A: Cek GitHub webhook "Recent Deliveries" untuk error message. Biasanya masalah secret atau port blocked.

---

## Resources

- Webhook package: https://github.com/adnanh/webhook
- GitHub Webhooks docs: https://docs.github.com/en/webhooks
- Securing webhooks: https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries

---

**Setup ini optional.** Deploy manual dengan `deploy.sh` sudah sangat baik untuk production. Auto-deploy bagus untuk development yang rapid iteration.

Pilih yang sesuai dengan workflow Anda! 🚀
