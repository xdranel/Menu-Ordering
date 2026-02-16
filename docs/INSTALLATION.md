# Installation Guide

Complete setup guide for installing and running the Restaurant Menu Ordering Application.

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
git clone https://github.com/xdranel/Menu-Ordering
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

> **IMPORTANT - Flyway Configuration:**
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

## Configuration Reference

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

---

## Troubleshooting

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
3. but if the user and password exist it might because the hashes error so you can generate it by 2 options
- if there is error while trying to login you generate new hash password using PasswordHashGenerator.java inside util/dev/ or
   ```bash
   mvn exec:java -Dexec.mainClass="menuorderingapp.project.util.dev.PasswordHashGenerator"
   ```
- after getting the new hash try to apply/update it on your database

---

## Starting Fresh

To completely reset your local installation:

```bash
# 1. Stop application (Ctrl+C if running)

# 2. Drop and recreate database
mysql -u root -p -e "DROP DATABASE IF EXISTS restaurant_db; CREATE DATABASE restaurant_db;"

# 3. Clean Maven build
mvn clean

# 4. Ensure Flyway is enabled
Edit application.properties: spring.flyway.enabled=true

# 5. Run again
mvn spring-boot:run
```

---

## Post-Installation Security

### Local Development

1. Keep default credentials for ease of development
2. Never commit `application.properties` with real passwords to Git

---

- See [API.md](API.md) for API documentation
