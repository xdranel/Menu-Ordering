# ChopChop Restaurant - Menu Ordering Application

A restaurant ordering system built with Spring Boot, featuring real-time order management, cashier dashboard, and customer self-service ordering.

## Features

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

## Tech Stack

**Backend:** Spring Boot 3.5.6, Java 21, Spring Security, MySQL 8.0, Hibernate, Flyway

**Frontend:** Thymeleaf, JavaScript, CSS3, STOMP.js

**Tools:** Maven, ZXing (QR codes), BCrypt

## Quick Start

### Prerequisites
- Java 21+
- MySQL 8.0+
- Maven 3.6+

### Installation

1. **Create database**
   ```bash
   mysql -u root -p -e "CREATE DATABASE restaurant_db;"
   ```

2. **Configure database**

   For local development, you can edit `src/main/resources/application.properties`:
   ```properties
   spring.datasource.password=your_mysql_password
   spring.flyway.enabled=true
   ```

   Or use environment variables (recommended):
   ```bash
   export DB_PASSWORD=your_mysql_password
   export HIBERNATE_DDL_AUTO=update
   export THYMELEAF_CACHE=false
   ```

3. **Run application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access**
   - Customer: http://localhost:8080
   - Cashier: http://localhost:8080/cashier/login

See [INSTALLATION.md](docs/INSTALLATION.md) for detailed setup instructions.

## Default Credentials

| Username | Password     | Role    |
|----------|--------------|---------|
| admin    | password123  | ADMIN   |
| kasir1   | password123  | CASHIER |
| kasir2   | password123  | CASHIER |

**Warning:** Change these passwords in production.

## Project Structure

```
menu-ordering-app/
├── src/main/
│   ├── java/menuorderingapp/project/
│   │   ├── config/              # Spring configuration
│   │   ├── controller/          # REST controllers
│   │   ├── model/              # Entity models & DTOs
│   │   ├── repository/         # JPA repositories
│   │   ├── security/           # Security components
│   │   ├── service/            # Business logic
│   │   └── util/               # Utility classes
│   └── resources/
│       ├── db/migration/       # Flyway SQL migrations
│       ├── static/             # CSS, JS, images
│       └── templates/          # Thymeleaf templates
├── docs/                       # Documentation
└── pom.xml                    # Maven configuration
```

## Documentation

- [INSTALLATION.md](docs/INSTALLATION.md) - Detailed installation guide
- [docs/API.md](docs/API.md) - REST API reference

## Key Endpoints

### Customer
- `GET /` - Menu page
- `GET /cart` - Shopping cart
- `POST /api/cart/add` - Add to cart
- `POST /api/orders` - Create order

### Cashier
- `GET /cashier/dashboard` - Dashboard
- `GET /cashier/orders` - Order management
- `GET /cashier/reports` - Sales reports
- `POST /api/auth/login` - Login

See [docs/API.md](docs/API.md) for complete API documentation.

## Security

- BCrypt password hashing (strength: 10)
- Session-based authentication (8-hour timeout)
- CSRF protection enabled
- Role-based access control (RBAC)
- SQL injection prevention via JPA
- XSS protection via Thymeleaf
- Secure cookies (HttpOnly, Secure, SameSite)
- HTTPS/TLS support
- Security headers (HSTS, CSP, X-Frame-Options)
- Environment-based configuration for sensitive data

## Troubleshooting

**Port 8080 in use:**
```bash
lsof -ti:8080 | xargs kill -9
```

**Database connection error:**
- Check MySQL is running
- Verify credentials in `application.properties`

**Flyway migration error:**
```bash
mysql -u root -p -e "DROP DATABASE restaurant_db; CREATE DATABASE restaurant_db;"
mvn spring-boot:run
```

## Development

### Generate Password Hashes
```bash
mvn exec:java -Dexec.mainClass="menuorderingapp.project.util.dev.PasswordHashGenerator"
```

### Build JAR
```bash
mvn clean package
java -jar target/menu-ordering-app-0.0.1-SNAPSHOT.jar
```

## License

[LICENSE](LICENSE)

## Authors

Team Hola Holo Don't Even Know What We're Doing
- [Gendhi Ramona P](https://github.com/XDX1O1)
- [Anak Agung Bramasta Jaya](https://github.com/BramastaJaya)
- [Haidar Fulca Kurniawan](https://github.com/sijuki09)
- - [Haidar Fulca Kurniawan](https://github.com/Arkkop12)