# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run application
mvn spring-boot:run

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=CategoryTestCase

# Build production JAR
mvn clean package

# Generate BCrypt password hashes (for seeding cashier accounts)
mvn exec:java -Dexec.mainClass="menuorderingapp.project.util.dev.PasswordHashGenerator"
```

App runs at `http://localhost:8080`. Customer side at `/customer/menu`, cashier login at `/auth/login`.

Database migrations run automatically via Flyway on startup (`src/main/resources/db/migration/`). Requires a `.env` file (copy from `.env.example`) with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Architecture

**Monolith** — Spring Boot 3.5.6, Java 21, MySQL 8, Thymeleaf (server-rendered HTML), WebSocket/STOMP for real-time updates, Flyway for schema migrations.

### Two distinct user sides

| Side | Entry | Auth |
|------|-------|------|
| Customer | `/customer/menu` | None (anonymous) |
| Cashier | `/auth/login` → `/cashier/dashboard` | Spring Security session + BCrypt |

### Request flow

```
Browser/JS → @Controller (Thymeleaf page) or @ResponseBody (JSON API)
           → Service (impl/) → Repository (JPA) → MySQL
```

Most controllers extend `BaseController` which provides typed `ResponseEntity<ApiResponse<T>>` helpers (`success()`, `error()`, `created()`, `notFound()`, `unauthorized()`). `BaseController` also holds the three shared converter methods: `convertToMenuResponse()`, `convertToOrderResponse()`, `convertToOrderItemResponse()`. All JSON APIs return the same `ApiResponse<T>` envelope.

### Key design decisions

**Dual auth systems (browser session + JWT):** Spring Security handles browser login (form login → `SecurityFilterChain` → session cookie). For Flutter/API clients, `POST /auth/api/login` returns a signed JWT (HS256, `JwtUtil`). `JwtAuthenticationFilter` (registered before `UsernamePasswordAuthenticationFilter`) reads `Authorization: Bearer <token>` and populates `SecurityContextHolder` if valid. Both paths end up with the same Security context — controllers don't need to know which was used. `SecurityUtils.getCurrentCashier()` is the authoritative check in controllers. `cashier_sessions` table is kept in DB but no longer populated (JWT is stateless). CSRF is disabled for all `/api/**` paths since API clients can't send CSRF tokens.

**Cart is server-side session, Order is database:** `CartController` stores `ShoppingCart` (with inner class `CartItem`) in `HttpSession`. When the customer checks out, the cart is converted to an `Order` + `OrderItems` persisted to DB. Cart data is lost if the session expires. `CartResponse` includes `subtotal`, `taxAmount`, and `total` (post-tax) — frontends should read these from the server rather than recalculating.

**Tax is NOT stored on Order — only on Invoice:** `Order.total` is always pre-tax (sum of `OrderItem.subtotal`). Tax (10%) is added only when `InvoiceServiceImpl.generateInvoice()` creates the invoice. `Constants.TAX_RATE = 0.10` is the canonical rate. Anywhere a post-tax amount is needed, compute `order.total * (1 + Constants.TAX_RATE)` in Java or `order.total * (1 + TAX_RATE)` in JS.

**Promo pricing:** `Menu.getCurrentPrice()` returns `promoPrice` if `isPromo == true`, else `price`. `OrderItem` snapshots this price at creation time (`price = menu.getCurrentPrice()`), so order history is price-safe.

**WebSocket:** `OrderWebSocketController` broadcasts to `/topic/orders` (order updates) and `/topic/dashboard` (stat refresh triggers). Cashier frontend subscribes via STOMP. Customer frontend does not use WebSocket.

**Order/invoice numbers:** Generated via UUID-based 8-char hex in `@PrePersist` hooks on `Order` and `Invoice` models (e.g., `ORD-A1B2C3D4`). Never use `System.currentTimeMillis()` for IDs.

### Package layout

```
config/         SecurityConfig, WebSocketConfig, WebConfig, SessionConfig, JacksonConfig
controller/     AuthController, CustomerController, CashierController, CartController,
                ReportController (@RestController), OrderWebSocketController, BaseController
model/          Order, OrderItem, Menu, Category, Invoice, Cashier, CashierSession, MenuAuditLog
model/dto/      Request/Response DTOs; ApiResponse<T> envelope; CartResponse (has subtotal+taxAmount+total)
service/impl/   Business logic — OrderServiceImpl, PaymentServiceImpl, InvoiceServiceImpl,
                MenuServiceImpl, AuthServiceImpl, CashierServiceImpl, ReportServiceImpl,
                MenuAuditServiceImpl
security/       CashierUserDetails, CashierUserDetailsService (Spring Security integration)
util/           Constants (TAX_RATE, currency, prefixes), SecurityUtils
```

### Frontend JS files

| File | Purpose |
|------|---------|
| `cashier-app.js` | All cashier dashboard logic (orders, payments, menus, reports). `const TAX_RATE = 0.10` at top. |
| `customer-app.js` | Customer cart sidebar + payment page flow; initializes `window.customerApp` |
| `menu-filter.js` | Menu page filtering, search, add-to-cart (canonical add-to-cart handler) |
| `cart-page.js` | Cart page-specific logic. `const TAX_RATE = 0.10` at top. |
| `websocket-client.js` | STOMP/SockJS connection wrapper used by cashier |
| `auth-app.js` | Login form handling (password toggle, empty field check only) |
| `timezone-utils.js` | Date/time display helpers for Asia/Jakarta (WIB, UTC+7) |

### Known architectural quirks

- `ReportController` uses `SecurityUtils.getCurrentCashier()` (consistent with other controllers).
- `PaymentServiceImpl.processCashPayment()` validates against `roundedAmount = Math.ceil((subtotal * (1 + TAX_RATE)) / CASH_ROUNDING_UNIT) * CASH_ROUNDING_UNIT`; QR payments use exact post-tax amount. These are intentionally different.
- `ReportServiceImpl` multiplies all revenue figures by `(1 + Constants.TAX_RATE)` in the service layer, since `Order.total` is always pre-tax.
- `cashier-app.js` stores both `dataset.exactTotal` and `dataset.roundedTotal` on the payment total element; `togglePaymentSections()` switches which value is displayed based on selected payment method.
- `Constants.APP_NAME` is used for the QR code merchant field (with spaces stripped).
- `BaseController` now holds all shared converters: `convertToMenuResponse()`, `convertToOrderResponse()`, `convertToOrderItemResponse()`, `convertToInvoiceResponse()`.

---

## Flutter Migration Plan

The goal is to expose the existing backend as a REST + JWT API so a Flutter app can replace the Thymeleaf frontend. The monolith stays — no microservices needed.

### What already works for Flutter

- All business logic is in `service/impl/` — no changes needed there
- `CustomerController` and `CashierController` already have `@ResponseBody` JSON API endpoints (`/customer/api/*`, `/cashier/api/*`)
- `ApiResponse<T>` envelope is consistent across all endpoints
- `CartResponse` (with `subtotal`, `taxAmount`, `total`), `OrderResponse`, `MenuResponse`, `InvoiceResponse` DTOs are already well-formed
- `/auth/api/login` already exists and returns a custom session token — needs JWT upgrade

### Flutter backend changes — completed

| # | Task | Status | Details |
|---|------|--------|---------|
| F1 | **JWT auth** | Done | `JwtUtil` in `util/`, `JwtAuthenticationFilter` in `security/`. `AuthServiceImpl.login()` now returns a signed JWT (HS256). Filter reads `Authorization: Bearer <token>` on every request and populates `SecurityContextHolder`. `POST /auth/api/login` → `{ sessionToken: "<jwt>", cashier: {...} }`. Validate via `GET /auth/api/validate` with same `Authorization: Bearer` header. JWT secret from `JWT_SECRET` env var; expiry from `JWT_EXPIRATION_HOURS` (default 8h). |
| F2 | **CORS** | Done | `CorsConfigurationSource` bean in `SecurityConfig`. Controlled by `CORS_ALLOWED_ORIGINS` env var (default `*` for dev). CSRF disabled for `/auth/api/**`, `/cashier/api/**`, `/customer/api/**`, `/api/**` — Flutter clients don't send CSRF tokens. |
| F3 | **Cart for mobile** | Decision | Flutter should maintain cart client-side and call `POST /customer/api/orders` with all items at once. No backend change needed. |
| F4 | **Category creation JSON** | Done | Added `POST /cashier/api/categories` with `consumes = "application/json"` returning `ApiResponse<CategoryResponse>`. The form-based endpoint (returns HTML redirect) still works for the browser UI. |
| F5 | **WebSocket (Flutter)** | No backend change | Backend already ready. Flutter cashier app uses `stomp_dart_client`. Update `CORS_ALLOWED_ORIGINS` to include Flutter's origin. |
| F6 | **Reports** | Already worked | `GET /cashier/api/reports/sales`, `GET /api/reports/top-items` all functional. |
| F7 | **Disable simulate-payment** | Config only | Set `SIMULATE_PAYMENT_ENABLED=false` in `.env` for production. |

### Existing API surface (already usable by Flutter with no backend changes)

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/customer/api/menus` | None | Available menus (category/search filter) |
| POST | `/customer/api/orders` | None | Create order from items list |
| GET | `/customer/api/orders/{number}` | None | Order status + items |
| GET | `/customer/api/orders/{number}/qr-code` | None | QR code as base64 data URL |
| POST | `/cashier/api/payments` | Session (→JWT after F1) | Process cash or QR payment; returns change |
| GET | `/cashier/api/orders/all` | Session (→JWT after F1) | All orders |
| GET | `/cashier/api/orders/today` | Session (→JWT after F1) | Today's orders |
| PUT | `/cashier/api/orders/{id}/status` | Session (→JWT after F1) | Update order status |
| GET | `/cashier/api/dashboard/stats` | Session (→JWT after F1) | Revenue, pending count, recent orders |
| GET | `/cashier/api/reports/sales` | Session (→JWT after F1) | Sales report with revenue breakdown |
| GET | `/cashier/api/menus` | Session (→JWT after F1) | All menus for management |
| POST | `/cashier/api/menus` | Session (→JWT after F1) | Create menu item |
| PUT | `/cashier/api/menus/{id}` | Session (→JWT after F1) | Update menu item |
| DELETE | `/cashier/api/menus/{id}` | Session (→JWT after F1) | Delete menu item |
| GET | `/cashier/api/invoices/by-date` | Session (→JWT after F1) | Invoices in date range |

### Recommended Flutter project structure

```
flutter_app/
  lib/
    api/          Dio HTTP client, JWT interceptor, ApiResponse<T> deserializer
    models/       MenuResponse, OrderResponse, CartResponse, InvoiceResponse, etc.
    screens/
      customer/   MenuScreen, CartScreen (local state), PaymentScreen
      cashier/    LoginScreen, DashboardScreen, OrdersScreen, PaymentScreen, ReportsScreen
    services/     AuthService (JWT storage), MenuService, OrderService, PaymentService
    websocket/    StompService (cashier only, using stomp_dart_client)
```

**All backend Flutter tasks are complete.** The backend is now fully ready for a Flutter client.
