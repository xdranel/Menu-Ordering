# Installation Guide

Setup guide for the Restaurant Menu Ordering Application.

---

## Local Development Setup

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
- **Windows:** Download MySQL Installer from the official website

---

#### 2. Clone Repository

```bash
git clone https://github.com/xdranel/Menu-Ordering
cd Menu-Ordering
```

---

#### 3. Create MySQL Database

```bash
mysql -u root -p -e "CREATE DATABASE restaurant_db;"
```

**Optional: Create a dedicated database user (recommended)**
```sql
CREATE USER 'restaurant_user'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON restaurant_db.* TO 'restaurant_user'@'localhost';
FLUSH PRIVILEGES;
```

---

#### 4. Configure Environment

Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

Edit `.env` with your values:

```
# Database
DB_URL=jdbc:mysql://localhost:3306/restaurant_db?useSSL=false&serverTimezone=Asia/Jakarta&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# JWT (required — generate a random secret for production)
JWT_SECRET=your-random-256-bit-secret-here
JWT_EXPIRATION_HOURS=8

# Flyway (keep true for first run; migrations run automatically)
SPRING_FLYWAY_ENABLED=true

# CORS (use * for dev, restrict to specific origins in production)
CORS_ALLOWED_ORIGINS=*

# Payment simulation (disable in production)
SIMULATE_PAYMENT_ENABLED=true
```

> **JWT_SECRET:** Generate a secure value with `openssl rand -base64 64`. The default dev value in `application.properties` must be changed in production.

---

#### 5. Run Application

**Development:**
```bash
mvn spring-boot:run
```

**Production JAR:**
```bash
mvn clean package
java -jar target/menu-ordering-app-0.0.1-SNAPSHOT.jar
```

**From IDE:**
Import as Maven project and run `MenuOrderingAppApplication.java`.

---

#### 6. Verify Installation

**Check logs for:**
```
Started MenuOrderingAppApplication
Flyway: Successfully applied X migrations
```

**Access the application:**
- Customer interface: http://localhost:8080/customer/menu
- Cashier login: http://localhost:8080/auth/login

**Default login credentials:**

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

## Configuration Reference

### Development Settings

Set these in `.env` for local development:

```
HIBERNATE_SHOW_SQL=true
THYMELEAF_CACHE=false
SPRING_FLYWAY_ENABLED=true
HIBERNATE_DDL_AUTO=update
```

### JWT Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | (weak dev default) | HS256 signing secret — change in production |
| `JWT_EXPIRATION_HOURS` | `8` | Token lifetime in hours |

### CORS Configuration

```
CORS_ALLOWED_ORIGINS=https://your-flutter-app.com,https://your-dashboard.com
```

---

## Troubleshooting

**Port 8080 already in use:**
```bash
lsof -ti:8080 | xargs kill -9
# Or set SERVER_PORT=9090 in .env
```

**Database connection refused:**
- Verify MySQL is running: `systemctl status mysql` (Linux) or `brew services list` (macOS)
- Check credentials in `.env`
- Test manually: `mysql -u root -p`

**Access denied for MySQL user:**
```bash
sudo mysql
ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';
FLUSH PRIVILEGES;
```

**Unknown database 'restaurant_db':**
```bash
mysql -u root -p -e "CREATE DATABASE restaurant_db;"
```

**Flyway migration failed:**
```bash
mysql -u root -p -e "DROP DATABASE IF EXISTS restaurant_db; CREATE DATABASE restaurant_db;"
mvn clean
mvn spring-boot:run
```

**Dependencies not downloading:**
```bash
mvn dependency:purge-local-repository
mvn clean install -U
```

**Cannot login — Invalid credentials:**
1. Verify Flyway created the default users:
   ```bash
   mysql -u root -p restaurant_db -e "SELECT username, role FROM cashiers;"
   ```
2. If users don't exist, ensure `SPRING_FLYWAY_ENABLED=true` in `.env` and restart.
3. If users exist but login still fails, the password hash may be corrupt. Generate a fresh hash:
   ```bash
   mvn exec:java -Dexec.mainClass="menuorderingapp.project.util.dev.PasswordHashGenerator"
   ```
   Then update the `password_hash` column in the `cashiers` table with the new hash.

---

## Starting Fresh

```bash
# 1. Stop application (Ctrl+C)

# 2. Drop and recreate database
mysql -u root -p -e "DROP DATABASE IF EXISTS restaurant_db; CREATE DATABASE restaurant_db;"

# 3. Clean Maven build
mvn clean

# 4. Ensure Flyway is enabled in .env
#    SPRING_FLYWAY_ENABLED=true

# 5. Run again
mvn spring-boot:run
```

---

## Post-Installation Security

1. Set a strong `JWT_SECRET` (at least 256 bits): `openssl rand -base64 64`
2. Change default cashier passwords via the cashier settings page
3. Set `SIMULATE_PAYMENT_ENABLED=false` in `.env`
4. Restrict `CORS_ALLOWED_ORIGINS` to your actual client origins
5. Never commit `.env` to Git

---

See [API.md](API.md) for API documentation.
