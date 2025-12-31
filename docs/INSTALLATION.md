# Installation Guide

Complete setup guide for installing and running the Restaurant Menu Ordering Application.

> **Choose your deployment type:** This guide covers both **local development** and **VPS production deployment**. Jump to the section that matches your needs.

---

## 🖥️ Local Development Setup

Perfect for development, testing, and making changes to the code.

### Prerequisites

- Java Development Kit (JDK) 21 or higher
- Apache Maven 3.6+
- MySQL 8.0+

### Step-by-Step Installation

#### 1. Install Prerequisites

**Verify Java:**
```bash
java -version
# Expected: java version "21.0.x"
```

**If not installed:**
- **Ubuntu/Debian:** `sudo apt install openjdk-21-jdk`
- **macOS:** `brew install openjdk@21`
- **Windows:** Download from [Oracle](https://www.oracle.com/java/technologies/downloads/)

**Verify Maven:**
```bash
mvn -version
```

**If not installed:**
- **Ubuntu/Debian:** `sudo apt install maven`
- **macOS:** `brew install maven`
- **Windows:** Download from [Maven](https://maven.apache.org/download.cgi)

**Verify MySQL:**
```bash
mysql --version
```

**If not installed:**
- **Ubuntu/Debian:**
  ```bash
  sudo apt install mysql-server
  sudo systemctl start mysql
  sudo mysql_secure_installation
  ```
- **macOS:**
  ```bash
  brew install mysql
  brew services start mysql
  ```
- **Windows:** Download MySQL Installer from official website

#### 2. Clone Repository

```bash
git clone <your-repository-url>
cd Menu-Ordering
```

#### 3. Create MySQL Database

```bash
mysql -u root -p -e "CREATE DATABASE restaurant_db;"
```

Or using MySQL Workbench:
```sql
CREATE DATABASE restaurant_db;
```

**Optional: Create dedicated database user (recommended for security)**
```bash
mysql -u root -p
```

```sql
CREATE USER 'restaurant_user'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON restaurant_db.* TO 'restaurant_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### 4. Configure Application

Edit `src/main/resources/application.properties`:

```properties
# Database Connection
spring.datasource.username=root
spring.datasource.password=your_mysql_password

# IMPORTANT: Set to true for FIRST RUN to create tables
spring.flyway.enabled=true
```

> **⚠️ IMPORTANT - Flyway Configuration:**
> - **First run:** Set `spring.flyway.enabled=true` to create all tables and insert sample data
> - **After first successful run:** You can set it to `false` to prevent migrations from running on every startup
> - If you make database changes, set it back to `true` to apply new migrations

**Alternative: Use environment variables (recommended for sensitive data)**
```bash
export DB_PASSWORD=your_mysql_password
export SPRING_FLYWAY_ENABLED=true
```

#### 5. Run Application

**Option 1: Maven Spring Boot Plugin (Development)**
```bash
mvn spring-boot:run
```

**Option 2: Build and run JAR (Production-like)**
```bash
mvn clean package
java -jar target/menu-ordering-app-0.0.1-SNAPSHOT.jar
```

**Option 3: Run from IDE**
1. Import project as Maven project
2. Run `MenuOrderingAppApplication.java`

#### 6. Verify Installation

**Check application logs for success messages:**
```
Started MenuOrderingAppApplication
Flyway: Successfully applied X migrations
```

**Access the application:**
- Customer interface: http://localhost:8080
- Cashier login: http://localhost:8080/cashier/login

**Login with default credentials:**

| Username | Password     | Role    |
|----------|--------------|---------|
| admin    | password123  | ADMIN   |
| kasir1   | password123  | CASHIER |
| kasir2   | password123  | CASHIER |

**Verify database tables:**
```bash
mysql -u root -p restaurant_db -e "SHOW TABLES;"
```

Expected tables: `cashiers`, `cashier_sessions`, `categories`, `menus`, `orders`, `order_items`, `invoices`, `menu_audit_log`

---

## 🌐 VPS Production Deployment

Perfect for deploying to production on any VPS provider (DigitalOcean, AWS EC2, Linode, Hostinger, etc.).

### Prerequisites

- A VPS with Ubuntu 22.04 LTS (or similar)
- Root or sudo access
- SSH access to your VPS

### Quick Deployment

#### 1. Configure Deployment Scripts

**Before uploading, edit `scripts/config.sh` on your local machine:**

```bash
nano scripts/config.sh
```

**Update these key variables:**
```bash
APP_NAME="myapp"                    # Your service name
APP_DISPLAY_NAME="My Restaurant"   # Display name
APP_USER="myappuser"               # System user to run app
APP_DIR="/opt/MyApp"               # Installation directory
JAR_NAME="myapp-1.0.0.jar"        # Your JAR filename
DEFAULT_DB_NAME="myapp_db"        # Database name
DEFAULT_DB_USER="myapp_user"      # Database user
SERVER_PORT="8080"                # Application port
JVM_XMX="2g"                      # Max heap (adjust for your VPS RAM)
```

See [scripts/SCRIPTS.md](../scripts/SCRIPTS.md) for all configuration options.

#### 2. Upload Setup Scripts

```bash
scp scripts/setup_vps.sh scripts/config.sh root@YOUR_VPS_IP:/root/
```

#### 3. Run VPS Setup

```bash
ssh root@YOUR_VPS_IP
sudo bash setup_vps.sh
```

The script will:
- Install Java 21, MySQL 8.0, Maven, Git, Nginx
- Create application user and webhook user
- Configure firewall (SSH, HTTP, HTTPS, app port)
- Create database with your specified name
- Set up directories and permissions
- Create `.env` file with database credentials

#### 4. Deploy Your Application

```bash
# Clone your repository
cd /opt
sudo git clone <your-repo-url> <YOUR_APP_DIR>
cd <YOUR_APP_DIR>

# Set ownership
sudo chown -R <APP_USER>:<APP_USER> .

# Build application
mvn clean package -DskipTests

# Create systemd service
sudo bash scripts/create_service.sh

# Start application
sudo systemctl start <APP_NAME>

# Verify it's running
sudo systemctl status <APP_NAME>
```

#### 5. Access Your Application

```
http://YOUR_VPS_IP:<SERVER_PORT>/
```

### Complete VPS Documentation

See **[scripts/SCRIPTS.md](../scripts/SCRIPTS.md)** for:
- Detailed deployment guide
- Update/redeploy procedures
- Database backup scripts
- Webhook setup for auto-deployment
- Troubleshooting

---

## 🔧 Configuration Reference

### Development Settings

Edit `application.properties` for local development:

```properties
# Show SQL queries in console
spring.jpa.show-sql=true

# Disable template caching for hot reload
spring.thymeleaf.cache=false

# Enable Flyway for first run
spring.flyway.enabled=true

# Use update for development (careful!)
spring.jpa.hibernate.ddl-auto=update
```

### Production Settings

For VPS deployment (automatically configured in `.env` by setup script):

```properties
# Hide SQL queries
spring.jpa.show-sql=false

# Enable template caching for performance
spring.thymeleaf.cache=true

# Disable Flyway after initial setup
spring.flyway.enabled=false

# Use validate in production
spring.jpa.hibernate.ddl-auto=validate
```

---

## 🐛 Troubleshooting

### Local Development Issues

**Port 8080 already in use:**
```bash
# Find and kill process using port 8080
lsof -ti:8080 | xargs kill -9

# Or change port in application.properties
server.port=9090
```

**Database connection refused:**
- Verify MySQL is running: `systemctl status mysql` (Linux) or `brew services list` (macOS)
- Check credentials in `application.properties`
- Try connecting manually: `mysql -u root -p`

**Access denied for MySQL user:**
```bash
# Reset MySQL root password
sudo mysql
ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';
FLUSH PRIVILEGES;
EXIT;
```

**Unknown database 'restaurant_db':**
```bash
mysql -u root -p -e "CREATE DATABASE restaurant_db;"
```

**Flyway migration failed:**
```bash
# Drop and recreate database
mysql -u root -p -e "DROP DATABASE IF EXISTS restaurant_db; CREATE DATABASE restaurant_db;"

# Clean build and restart
mvn clean
mvn spring-boot:run
```

**Dependencies not downloading:**
```bash
# Clear Maven cache
mvn dependency:purge-local-repository

# Force update
mvn clean install -U
```

**Cannot login - Invalid credentials:**
1. Verify Flyway created the default users:
   ```bash
   mysql -u root -p restaurant_db -e "SELECT username, role FROM cashiers;"
   ```
2. If users don't exist, ensure `spring.flyway.enabled=true` and restart
3. Generate new password hash if needed:
   ```bash
   mvn exec:java -Dexec.mainClass="menuorderingapp.project.util.dev.PasswordHashGenerator"
   ```

### VPS Deployment Issues

**Service won't start:**
```bash
# Check service status
sudo systemctl status <APP_NAME>

# View detailed logs
sudo journalctl -u <APP_NAME> -n 100

# Check if JAR exists
ls -lh <APP_DIR>/target/<JAR_NAME>
```

**Database connection error on VPS:**
```bash
# Verify MySQL is running
sudo systemctl status mysql

# Check .env file
cat <APP_DIR>/.env

# Test database connection
mysql -u <DB_USER> -p <DB_NAME>
```

For more VPS troubleshooting, see [scripts/SCRIPTS.md](../scripts/SCRIPTS.md).

---

## 🔄 Starting Fresh

To completely reset your local installation:

```bash
# 1. Stop application (Ctrl+C if running)

# 2. Drop and recreate database
mysql -u root -p -e "DROP DATABASE IF EXISTS restaurant_db; CREATE DATABASE restaurant_db;"

# 3. Clean Maven build
mvn clean

# 4. Ensure Flyway is enabled
# Edit application.properties: spring.flyway.enabled=true

# 5. Run again
mvn spring-boot:run
```

---

## ✅ Post-Installation Security

### For Local Development

1. Keep default credentials for ease of development
2. Never commit `application.properties` with real passwords to Git

### For VPS Production

1. **Change default passwords immediately:**
   ```bash
   mvn exec:java -Dexec.mainClass="menuorderingapp.project.util.dev.PasswordHashGenerator"
   # Update database with new hashes
   ```

2. **Secure the .env file:**
   ```bash
   chmod 600 <APP_DIR>/.env
   ```

3. **Set up SSL/HTTPS:** Use Let's Encrypt with Nginx

4. **Configure firewall properly:** Only open necessary ports

5. **Regular backups:**
   ```bash
   # Set up daily database backups
   crontab -e
   # Add: 0 2 * * * <APP_DIR>/scripts/backup_database.sh
   ```

---

## 📚 Next Steps

- **Local Development:** Start coding! See [API.md](API.md) for API documentation
- **VPS Deployment:** See [scripts/SCRIPTS.md](../scripts/SCRIPTS.md) for deployment automation
- **Questions?** Check the main [README.md](../README.md) for more information
