# Restaurant Menu Ordering Application

A restaurant ordering system built with Spring Boot, featuring real-time order management, cashier dashboard, and customer self-service ordering.

## Features

### Customer Features
- Browse menu by categories with search and filter
- Shopping cart with real-time price calculation (subtotal, tax, total)
- Multiple payment methods: Cash or QR Code
- Order tracking with order numbers

### Cashier Features
- Real-time dashboard with statistics (revenue, orders, pending count)
- Order management with status updates
- Real-time order updates via WebSocket/STOMP
- Process payments and auto-generate invoices
- Sales reports with date range filtering
- Menu and category management with audit logging

### Technical Features
- Dual auth: browser session (Spring Security) + JWT for API/Flutter clients
- Real-time updates using WebSocket/STOMP
- BCrypt password hashing
- Database migrations with Flyway
- Audit logging for menu changes
- CORS support for Flutter/API clients

## Tech Stack

**Backend:** Spring Boot 3.5.6, Java 21, Spring Security, MySQL 8.0, Hibernate, Flyway  
**Frontend:** Thymeleaf, JavaScript, CSS3, STOMP.js  
**Auth:** BCrypt, Spring Security sessions, JWT (HS256)  
**Tools:** Maven, ZXing (QR codes)

## Prerequisites

- Java 21+
- MySQL 8.0+
- Maven 3.6+

## Quick Start

1. **Clone and navigate**
   ```bash
   git clone https://github.com/xdranel/Menu-Ordering
   cd Menu-Ordering
   ```

2. **Create database**
   ```bash
   mysql -u root -p -e "CREATE DATABASE restaurant_db;"
   ```

3. **Configure environment**

   Copy `.env.example` to `.env` and fill in your values:
   ```bash
   cp .env.example .env
   ```

   Minimum required changes in `.env`:
   ```
   DB_USERNAME=root
   DB_PASSWORD=your_mysql_password
   JWT_SECRET=your-random-256-bit-secret
   ```

4. **Run application**
   ```bash
   mvn spring-boot:run
   ```

5. **Access application**
   - Customer: http://localhost:8080/customer/menu
   - Cashier login: http://localhost:8080/auth/login

See [docs/INSTALLATION.md](docs/INSTALLATION.md) for detailed setup including Docker and troubleshooting.

## Project Structure

```
Menu-Ordering/
├── src/main/
│   ├── java/menuorderingapp/project/
│   │   ├── config/           # Spring configuration (Security, WebSocket, CORS, etc.)
│   │   ├── controller/       # Controllers (Customer, Cashier, Auth, Cart, Report)
│   │   ├── model/            # Entity models & DTOs
│   │   ├── repository/       # JPA repositories
│   │   ├── security/         # Spring Security + JWT filter
│   │   ├── service/          # Business logic
│   │   └── util/             # Constants, SecurityUtils, JwtUtil
│   └── resources/
│       ├── db/migration/    # Flyway SQL migrations
│       ├── static/          # CSS, JS, images
│       ├── templates/       # Thymeleaf templates
│       └── application.properties
├── docs/
│   ├── INSTALLATION.md      # Detailed setup guide
│   └── API.md               # API reference
└── pom.xml
```

## Documentation

- **[INSTALLATION.md](docs/INSTALLATION.md)** - Detailed installation guide
- **[API.md](docs/API.md)** - REST API reference

## Security

- BCrypt password hashing (strength: 10)
- Session-based auth for browser (8-hour timeout, HttpOnly/SameSite cookies)
- JWT (HS256) for API/Flutter clients — secret from `JWT_SECRET` env var
- CSRF enabled for browser routes; disabled for `/api/**` paths
- Role-based access control (ADMIN / CASHIER)
- SQL injection prevention via JPA
- XSS protection via Thymeleaf

## Troubleshooting

**Port already in use:**
```bash
lsof -ti:8080 | xargs kill -9
# Or set SERVER_PORT=9090 in .env
```

**Database connection error:**
- Verify MySQL is running: `systemctl status mysql`
- Check `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` in `.env`

**Flyway migration error:**
```bash
mysql -u root -p -e "DROP DATABASE restaurant_db; CREATE DATABASE restaurant_db;"
mvn spring-boot:run
```

See [docs/INSTALLATION.md](docs/INSTALLATION.md) for more troubleshooting.

## Development

**Generate password hashes:**
```bash
mvn exec:java -Dexec.mainClass="menuorderingapp.project.util.dev.PasswordHashGenerator"
```

**Build production JAR:**
```bash
mvn clean package
java -jar target/menu-ordering-app-0.0.1-SNAPSHOT.jar
```

## License

[LICENSE](LICENSE)

## Authors

- [Gendhi Ramona P](https://github.com/XDX1O1)
- [Anak Agung Bramasta Jaya](https://github.com/BramastaJaya)
- [Haidar Fulca Kurniawan](https://github.com/sijuki09)
- [Arka Dwi Indrastata](https://github.com/Arkkop12)
