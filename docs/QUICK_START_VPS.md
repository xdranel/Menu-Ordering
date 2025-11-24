# Quick Start - Deploy ke Hostinger VPS

Panduan singkat untuk deploy aplikasi ke VPS Hostinger untuk presentasi.

## Ringkasan Cepat

1. **Beli VPS Hostinger** (VPS KVM 1, ~50-70rb/bulan)
2. **Setup VPS** dengan script otomatis
3. **Upload & Build aplikasi**
4. **Akses dengan 2 layar** untuk presentasi

Total waktu: ~30-60 menit

---

## Step 1: Beli VPS (5 menit)

1. Buka: https://www.hostinger.co.id/vps-hosting
2. Pilih: **VPS KVM 1** (4GB RAM, 2 vCPU, 50GB)
3. OS: **Ubuntu 22.04 LTS**
4. Proses pembayaran
5. Catat:
   - IP Address VPS
   - Username: root
   - Password awal

---

## Step 2: Upload Script Setup (2 menit)

Di komputer lokal Anda:

```bash
# Upload script ke VPS (ganti YOUR_VPS_IP)
scp scripts/setup_vps.sh root@YOUR_VPS_IP:/root/
```

Password: password yang dikirim Hostinger via email

---

## Step 3: Run Setup Script (10-15 menit)

```bash
# SSH ke VPS
ssh root@YOUR_VPS_IP

# Jalankan setup (install Java, MySQL, dll)
bash setup_vps.sh
```

Script akan install:
- Java 21
- MySQL 8.0
- Maven, Git, Nginx
- Firewall
- Database

**CATAT password database** yang Anda buat saat ditanya!

---

## Step 4: Upload Aplikasi (5 menit)

### Opsi A: Via Git (Recommended)

```bash
# SSH sebagai user chopchop
ssh chopchop@YOUR_VPS_IP

# Clone repository
cd /opt
sudo git clone https://github.com/YOUR_USERNAME/menu-ordering-app.git
sudo chown -R chopchop:chopchop menu-ordering-app
cd menu-ordering-app
```

### Opsi B: Upload JAR Manual

```bash
# Di komputer lokal, build dulu
mvn clean package -DskipTests

# Upload ke VPS
scp target/menu-ordering-app-0.0.1-SNAPSHOT.jar chopchop@YOUR_VPS_IP:/opt/menu-ordering-app/target/
```

---

## Step 5: Build & Deploy (10 menit)

```bash
# SSH ke VPS sebagai chopchop
ssh chopchop@YOUR_VPS_IP
cd /opt/menu-ordering-app

# Build aplikasi (jika clone dari git)
mvn clean package -DskipTests

# Buat systemd service
sudo bash scripts/create_service.sh

# Start aplikasi
sudo systemctl start chopchop

# Cek status
sudo systemctl status chopchop
```

**Tunggu sampai** status: `active (running)`

---

## Step 6: Test Akses

Buka browser:

**Customer page**:
```
http://YOUR_VPS_IP:8080/
```

**Cashier dashboard**:
```
http://YOUR_VPS_IP:8080/cashier/login
```

**Login credentials**:
- Username: `admin`
- Password: `password123`

Jika bisa diakses = SUKSES!

---

## Untuk Presentasi dengan 2 Layar

### Laptop/Layar 1 (Customer View):
```
http://YOUR_VPS_IP:8080/
```
- Browse menu
- Add to cart
- Place order

### Laptop/Layar 2 (Cashier Dashboard):
```
http://YOUR_VPS_IP:8080/cashier/login
```
- Login sebagai kasir
- Lihat order masuk real-time
- Process payment

**Real-time sync via WebSocket!** Order di layar 1 langsung muncul di layar 2.

---

## Troubleshooting

### Aplikasi tidak start?

```bash
# Cek logs
sudo journalctl -u chopchop -n 100

# Restart
sudo systemctl restart chopchop
```

### Tidak bisa akses dari browser?

```bash
# Cek firewall
sudo ufw status

# Pastikan port 8080 open
sudo ufw allow 8080/tcp

# Cek aplikasi running
sudo systemctl status chopchop
```

### Database error?

```bash
# Test koneksi
mysql -u chopchop_user -p restaurant_db

# Cek .env
cat /opt/menu-ordering-app/.env
```

---

## Maintenance

### Update aplikasi (setelah ada perubahan code)

```bash
cd /opt/menu-ordering-app
bash scripts/deploy.sh
```

### Backup database

```bash
bash scripts/backup_database.sh
```

### View logs

```bash
sudo journalctl -u chopchop -f
```

### Restart aplikasi

```bash
sudo systemctl restart chopchop
```

---

## Biaya Total

- **VPS**: Rp 60.000/bulan
- **Domain** (optional): Gratis (.my.id dari Pandi)

**Total untuk 1 bulan presentasi**: ~Rp 60.000

---

## Next Steps

Setelah presentasi:
1. Ganti password default (admin, kasir)
2. Setup SSL/HTTPS jika ingin production
3. Regular backup database
4. Monitor resource usage

---

## Dokumentasi Lengkap

- **Panduan lengkap**: `HOSTINGER_DEPLOYMENT.md`
- **Script documentation**: `scripts/README.md`
- **General deployment**: `DEPLOYMENT.md`

---

**Selamat! Aplikasi siap untuk presentasi! 🎉**

Jika ada masalah, cek file `HOSTINGER_DEPLOYMENT.md` untuk troubleshooting lengkap.
