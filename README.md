# Restaurant Menu Ordering Application

A complete restaurant ordering system built with Spring Boot, featuring real-time order management, cashier dashboard, and customer self-service ordering.

## ✨ Features

### Customer Features
- Browse menu by categories
- Search and filter menu items
- Shopping cart with real-time price calculation
- Multiple payment methods (Cash, QR Code)
- Responsive design for mobile and desktop
- Order tracking with order numbers

### Cashier Features
- Real-time dashboard with statistics (revenue, orders, pending count)
- Order management with status filtering
- Real-time order updates via WebSocket
- Process payments and generate invoices
- Sales reports with date range filtering
- Role-based access control (Admin/Cashier)

### Technical Features
- Real-time updates using WebSocket/STOMP
- Secure authentication with BCrypt password hashing
- Database migrations with Flyway
- Comprehensive audit logging for menu changes
- RESTful API architecture

## 🛠 Tech Stack

**Backend:** Spring Boot 3.5.6, Java 21, Spring Security, MySQL 8.0, Hibernate, Flyway  
**Frontend:** Thymeleaf, JavaScript, CSS3, STOMP.js  
**Tools:** Maven, ZXing (QR codes), BCrypt

## 📋 Prerequisites

- Java 21+
- MySQL 8.0+
- Maven 3.6+

## 🚀 Quick Start

Choose your setup type:

### Option 1: Local Development

Perfect for development, testing, and making changes to the code.

1. **Clone and navigate**
   ```bash
   git clone <your-repo-url>
   cd Menu-Ordering
   ```

2. **Create database**
   ```bash
   mysql -u root -p -e "CREATE DATABASE restaurant_db;"
   ```

3. **Configure database**
   
   Edit `src/main/resources/application.properties`:
   ```properties
   spring.datasource.password=your_mysql_password
   spring.flyway.enabled=true
   ```
   
   > **Important:** Set `spring.flyway.enabled=true` for the **first run** to create tables and insert sample data. After the first successful run, you can set it to `false` if you don't want migrations to run on every startup.

4. **Run application**
   ```bash
   mvn spring-boot:run
   ```

5. **Access application**
   - Customer: http://localhost:8080
   - Cashier: http://localhost:8080/cashier/login

See [docs/INSTALLATION.md](docs/INSTALLATION.md) for detailed local setup.

### Option 2: VPS Deployment

Perfect for production deployment on any VPS provider (DigitalOcean, AWS, Linode, Hostinger, etc.).

1. **Configure deployment scripts**
   
   Edit `scripts/config.sh` to customize for your application:
   ```bash
   nano scripts/config.sh
   ```
   
   Update at minimum:
   - `APP_NAME` - Your service name
   - `APP_USER` - System user to run the app
   - `APP_DIR` - Installation directory
   - `JAR_NAME` - Your JAR filename
   - Database settings, ports, etc.

2. **Upload and run setup script**
   ```bash
   scp scripts/setup_vps.sh scripts/config.sh root@YOUR_VPS_IP:/root/
   ssh root@YOUR_VPS_IP
   sudo bash setup_vps.sh
   ```

3. **Follow the prompts**
   
   The script will:
   - Install Java, MySQL, Maven, Git, Nginx
   - Create users and directories
   - Configure firewall
   - Set up database

4. **Deploy your application**
   ```bash
   cd /opt/<YOUR_APP_DIR>
   git clone <your-repo-url> .
   bash scripts/deploy.sh
   ```

See [scripts/SCRIPTS.md](scripts/SCRIPTS.md) for complete VPS deployment guide.

## 🔑 Default Credentials

> **⚠️ Security Warning:** Change these passwords immediately in production!

## 📁 Project Structure

```
Menu-Ordering/
├── scripts/                   # VPS deployment scripts
│   ├── config.sh             # Configuration (edit this first!)
│   ├── setup_vps.sh          # Initial VPS setup
│   ├── deploy.sh             # Deploy/update app
│   ├── create_service.sh     # Create systemd service
│   ├── backup_database.sh    # Database backup
│   ├── reset_database.sh     # Reset database
│   └── SCRIPTS.md            # Deployment documentation
├── src/main/
│   ├── java/menuorderingapp/project/
│   │   ├── config/           # Spring configuration
│   │   ├── controller/       # REST controllers
│   │   ├── model/           # Entity models & DTOs
│   │   ├── repository/      # JPA repositories
│   │   ├── security/        # Security components
│   │   ├── service/         # Business logic
│   │   └── util/            # Utility classes
│   └── resources/
│       ├── db/migration/    # Flyway SQL migrations
│       ├── static/          # CSS, JS, images
│       ├── templates/       # Thymeleaf templates
│       └── application.properties
├── docs/                    # Documentation
│   ├── INSTALLATION.md      # Detailed setup guide
│   └── API.md              # API reference
└── pom.xml                 # Maven configuration
```

## 📖 Documentation

- **[INSTALLATION.md](docs/INSTALLATION.md)** - Detailed local installation guide
- **[scripts/SCRIPTS.md](scripts/SCRIPTS.md)** - VPS deployment guide
- **[API.md](docs/API.md)** - REST API reference

## 🔒 Security Features

- BCrypt password hashing (strength: 10)
- Session-based authentication (8-hour timeout)
- CSRF protection enabled
- Role-based access control (RBAC)
- SQL injection prevention via JPA
- XSS protection via Thymeleaf
- Secure cookies (HttpOnly, Secure, SameSite)
- Environment-based configuration for sensitive data

## 🐛 Troubleshooting

**Port already in use:**
```bash
lsof -ti:8080 | xargs kill -9
# Or change port in application.properties:
# server.port=9090
```

**Database connection error:**
- Verify MySQL is running: `systemctl status mysql`
- Check credentials in `application.properties`

**Flyway migration error:**
```bash
mysql -u root -p -e "DROP DATABASE restaurant_db; CREATE DATABASE restaurant_db;"
mvn spring-boot:run
```

See [docs/INSTALLATION.md](docs/INSTALLATION.md) for more troubleshooting.

## 🛠 Development

**Generate password hashes:**
```bash
mvn exec:java -Dexec.mainClass="menuorderingapp.project.util.dev.PasswordHashGenerator"
```

**Build production JAR:**
```bash
mvn clean package
java -jar target/menu-ordering-app-0.0.1-SNAPSHOT.jar
```

## 📄 License

[LICENSE](LICENSE)

## 👥 Authors

Team Hola Holo Don't Even Know What We're Doing
- [Gendhi Ramona P](https://github.com/XDX1O1)
- [Anak Agung Bramasta Jaya](https://github.com/BramastaJaya)
- [Haidar Fulca Kurniawan](https://github.com/sijuki09)
- [Arka Dwi Indrastata](https://github.com/Arkkop12)
- Hehe