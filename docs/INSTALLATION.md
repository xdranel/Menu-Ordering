# Installation Guide

Complete setup guide for installing and running the ChopChop Restaurant application.

## Prerequisites

- Java Development Kit (JDK) 21 or higher
- Apache Maven 3.6+
- MySQL 8.0+

## Quick Installation

### Step 1: Clone Repository

```bash
git clone <repository-url>
cd menu-ordering-app
```

### Step 2: Create MySQL Database

```bash
mysql -u root -p -e "CREATE DATABASE restaurant_db;"
```

Or using MySQL Workbench:
```sql
CREATE DATABASE restaurant_db;
```

### Step 3: Configure Database

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=your_mysql_password
spring.flyway.enabled=true
```

### Step 4: Run Application

```bash
mvn spring-boot:run
```

Flyway will automatically create tables and insert sample data.

### Step 5: Access Application

- Customer Interface: http://localhost:8080
- Cashier Login: http://localhost:8080/cashier/login

### Step 6: Login

| Username | Password     | Role    |
|----------|--------------|---------|
| admin    | password123  | ADMIN   |
| kasir1   | password123  | CASHIER |
| kasir2   | password123  | CASHIER |

## Detailed Installation

### Install Java

Verify installation:
```bash
java -version
```

Expected output: `java version "21.0.x"`

**If not installed:**
- Windows/macOS: Download from [Oracle](https://www.oracle.com/java/technologies/downloads/)
- macOS: `brew install openjdk@21`
- Ubuntu: `sudo apt install openjdk-21-jdk`

### Install Maven

Verify installation:
```bash
mvn -version
```

**If not installed:**
- Windows/macOS: Download from [Maven](https://maven.apache.org/download.cgi)
- macOS: `brew install maven`
- Ubuntu: `sudo apt install maven`

### Install MySQL

**macOS:**
```bash
brew install mysql
brew services start mysql
```

**Ubuntu:**
```bash
sudo apt install mysql-server
sudo systemctl start mysql
sudo mysql_secure_installation
```

**Windows:**
Download and install MySQL Installer from official website.

### Create Database User (Optional)

For better security:

```bash
mysql -u root -p
```

```sql
CREATE USER 'restaurant_user'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON restaurant_db.* TO 'restaurant_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

Then update `application.properties`:
```properties
spring.datasource.username=restaurant_user
spring.datasource.password=secure_password
```

## Configuration

### Main Configuration File

Location: `src/main/resources/application.properties`

```properties
# Application
spring.application.name=menu-ordering-app
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/restaurant_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Session
server.servlet.session.timeout=8h
```

### Development vs Production

**Development:**
- `spring.jpa.show-sql=true` - See SQL queries
- `spring.thymeleaf.cache=false` - Hot reload templates

**Production:**
- `spring.jpa.show-sql=false`
- `spring.thymeleaf.cache=true`
- `spring.flyway.enabled=false` (after first run)
- Use strong passwords

## Build and Run

### Option 1: Maven Spring Boot Plugin
```bash
mvn spring-boot:run
```

### Option 2: Build JAR
```bash
mvn clean package
java -jar target/menu-ordering-app-0.0.1-SNAPSHOT.jar
```

### Option 3: Run from IDE
1. Import as Maven project
2. Run `MenuOrderingAppApplication.java`

## Verify Installation

### Check Application Logs
Look for:
```
Started MenuOrderingAppApplication
Flyway: Successfully applied 2 migrations
```

### Test Endpoints
```bash
curl http://localhost:8080
curl http://localhost:8080/cashier/login
```

### Verify Database
```bash
mysql -u root -p restaurant_db -e "SHOW TABLES;"
```

Expected tables:
- cashier_sessions
- cashiers
- categories
- invoices
- menus
- menu_audit_log
- orders
- order_items

## Troubleshooting

### Port 8080 Already in Use

```bash
# Find and kill process
lsof -ti:8080 | xargs kill -9

# Or change port in application.properties
server.port=9090
```

### Access Denied for MySQL User

**Cause:** Wrong password

**Solution:**
1. Verify password in `application.properties`
2. Test connection: `mysql -u root -p`
3. Reset password if needed:
   ```bash
   sudo mysql
   ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';
   FLUSH PRIVILEGES;
   ```

### Unknown Database Error

**Cause:** Database not created

**Solution:**
```bash
mysql -u root -p -e "CREATE DATABASE restaurant_db;"
```

### Flyway Migration Failed

**Cause:** Database state inconsistent or tables already exist

**Solution:**
```bash
# Drop and recreate database
mysql -u root -p -e "DROP DATABASE IF EXISTS restaurant_db; CREATE DATABASE restaurant_db;"

# Run application again
mvn spring-boot:run
```

### Dependencies Not Downloading

```bash
# Clear Maven cache
mvn dependency:purge-local-repository

# Force update
mvn clean install -U
```

### Cannot Login - Invalid Credentials

**Cause:** Password hash mismatch

**Solution:**
1. Generate new hash using `PasswordHashGenerator`
2. Update database:
   ```sql
   UPDATE cashiers SET password_hash = '<new_hash>' WHERE username = 'admin';
   ```

## Starting Fresh

To completely reset:

```bash
# Stop application (Ctrl+C)

# Drop and recreate database
mysql -u root -p -e "DROP DATABASE IF EXISTS restaurant_db; CREATE DATABASE restaurant_db;"

# Clean build
mvn clean

# Run again
mvn spring-boot:run
```

## What Gets Installed

### Database Tables
- cashiers - User accounts
- cashier_sessions - Active sessions
- categories - Menu categories
- menus - Menu items
- orders - Customer orders
- order_items - Order details
- invoices - Payment records
- menu_audit_log - Change history

### Sample Data
- 9 Categories (Semua, Promo, Paket Kombo, etc.)
- 20+ Menu Items
- 3 Cashier Accounts (admin, kasir1, kasir2)

## Post-Installation

1. **Change Default Passwords**
   - Use `PasswordHashGenerator` utility
   - Update database with new hashes

2. **Configure Production Settings**
   - Disable Flyway after first run
   - Enable template caching
   - Configure HTTPS

3. **Test All Features**
   - Customer ordering
   - Cashier dashboard
   - Payment processing
   - Reports
