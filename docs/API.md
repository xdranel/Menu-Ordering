# API Documentation

REST API reference for the Menu Ordering Application (ChopChop Restaurant).

## Table of Contents

- [Base URL](#base-url)
- [Authentication](#authentication)
- [Response Format](#response-format)
- [Customer API](#customer-api)
- [Cart API](#cart-api)
- [Cashier API](#cashier-api)
- [Reports API](#reports-api)
- [WebSocket](#websocket)
- [Error Codes](#error-codes)

---

## Base URL

```
http://localhost:8080
```

---

## Authentication

The application supports two auth mechanisms that both populate the same Spring Security context — controllers do not distinguish between them.

### Browser (form login)
Spring Security intercepts `POST /auth/login` and sets a session cookie on success.

### API / Flutter (JWT)

**Login**
```http
POST /auth/api/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "sessionToken": "<jwt>",
    "cashier": {
      "id": 1,
      "username": "admin",
      "displayName": "Administrator",
      "role": "ADMIN",
      "isActive": true
    },
    "message": "Login successful"
  }
}
```

Use the returned JWT in subsequent requests:
```
Authorization: Bearer <jwt>
```

**Validate token**
```http
GET /auth/api/validate
Authorization: Bearer <jwt>
```

**Logout**
```http
POST /auth/api/logout
Authorization: Bearer <jwt>
```

---

## Response Format

All API responses use the `ApiResponse<T>` envelope:

```json
{
  "success": true,
  "message": "string",
  "data": {},
  "timestamp": "2025-01-01T12:00:00"
}
```

On error:
```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2025-01-01T12:00:00"
}
```

---

## Customer API

No authentication required.

### Get Available Menus
```http
GET /customer/api/menus
```

**Query parameters:**
| Param | Type | Description |
|-------|------|-------------|
| `categoryId` | Long | Filter by category ID |
| `search` | String | Search by name or description |

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Nasi Goreng Spesial",
      "description": "Nasi goreng dengan ayam dan sayuran",
      "price": 35000,
      "currentPrice": 35000,
      "imageUrl": "/images/menu/nasi-goreng.jpg",
      "category": { "id": 4, "name": "MAKANAN UTAMA" },
      "available": true,
      "isPromo": false,
      "promoPrice": null
    }
  ]
}
```

> `currentPrice` returns `promoPrice` when `isPromo` is `true`, otherwise `price`.

---

### Create Order
```http
POST /customer/api/orders
Content-Type: application/json

{
  "customerName": "John Doe",
  "items": [
    { "menuId": 1, "quantity": 2 }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderNumber": "ORD-A1B2C3D4",
    "customerName": "John Doe",
    "total": 70000,
    "status": "PENDING",
    "paymentStatus": "PENDING",
    "orderType": "CUSTOMER_SELF",
    "createdAt": "2025-01-01T12:00:00"
  }
}
```

> `total` is always pre-tax. Tax (10%) is added only on the invoice.

---

### Get Order by Number
```http
GET /customer/api/orders/{orderNumber}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderNumber": "ORD-A1B2C3D4",
    "customerName": "John Doe",
    "total": 70000,
    "status": "PENDING",
    "paymentStatus": "PENDING",
    "orderItems": [
      {
        "id": 1,
        "menuName": "Nasi Goreng Spesial",
        "quantity": 2,
        "price": 35000,
        "subtotal": 70000
      }
    ]
  }
}
```

---

### Add Item to Order
```http
POST /customer/api/orders/{orderId}/items
Content-Type: application/json

{
  "menuId": 2,
  "quantity": 1
}
```

### Update Item Quantity
```http
PUT /customer/api/orders/{orderId}/items/{itemId}?quantity=3
```

### Remove Item from Order
```http
DELETE /customer/api/orders/{orderId}/items/{itemId}
```

---

### Get QR Code for Order
```http
GET /customer/api/orders/{orderNumber}/qr-code
```

**Response:**
```json
{
  "success": true,
  "data": {
    "orderNumber": "ORD-A1B2C3D4",
    "qrCodeImage": "data:image/png;base64,iVBORw0KG...",
    "message": "QR code generated successfully"
  }
}
```

---

### Process Payment (Customer)
```http
POST /customer/api/payments
Content-Type: application/json

{
  "orderNumber": "ORD-A1B2C3D4",
  "paymentMethod": "CASH",
  "cashAmount": 100000
}
```

For QR:
```json
{
  "orderNumber": "ORD-A1B2C3D4",
  "paymentMethod": "QR_CODE",
  "qrData": "payment_data_string"
}
```

**Payment methods:** `CASH`, `QR_CODE`

**Response:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "orderNumber": "ORD-A1B2C3D4",
    "message": "Payment successful"
  }
}
```

> Invoice is auto-generated on successful payment.

---

### Simulate Payment (Test only)
```http
POST /customer/api/orders/{orderNumber}/simulate-payment
```

Only available when `SIMULATE_PAYMENT_ENABLED=true` (default in dev). Disable in production via `.env`.

---

## Cart API

Session-based. Cart is stored server-side in `HttpSession`; data is lost when the session expires. For Flutter, maintain cart client-side and submit items directly to `POST /customer/api/orders`.

### Get Cart
```http
GET /customer/api/cart
```

**Response:**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "menuId": 1,
        "menuName": "Nasi Goreng Spesial",
        "price": 35000,
        "quantity": 2,
        "subtotal": 70000,
        "imageUrl": "/images/menu/nasi-goreng.jpg"
      }
    ],
    "subtotal": 70000,
    "taxAmount": 7000,
    "total": 77000,
    "totalItems": 2
  }
}
```

### Get Cart Item Count
```http
GET /customer/api/cart/count
```

### Add Item to Cart
```http
POST /customer/api/cart/add
Content-Type: application/json

{
  "menuId": 1,
  "quantity": 2
}
```

### Update Cart Item Quantity
```http
PUT /customer/api/cart/update/{menuId}?quantity=3
```

### Remove Item from Cart
```http
DELETE /customer/api/cart/remove/{menuId}
```

### Clear Cart
```http
DELETE /customer/api/cart/clear
```

---

## Cashier API

All cashier endpoints require authentication (browser session cookie or `Authorization: Bearer <jwt>`).

### Dashboard Stats
```http
GET /cashier/api/dashboard/stats
```

**Response:**
```json
{
  "success": true,
  "data": {
    "todayRevenue": 1500000,
    "todayOrdersCount": 45,
    "pendingOrders": 8,
    "availableMenus": 24,
    "recentOrders": [ ... ]
  }
}
```

> `todayRevenue` is pre-tax (sum of `Order.total`).

---

### Orders

**Get all orders:**
```http
GET /cashier/api/orders/all
```

**Get today's orders:**
```http
GET /cashier/api/orders/today
```

**Get orders by date:**
```http
GET /cashier/api/orders/by-date?date=2025-01-01
```

**Create cashier-assisted order:**
```http
POST /cashier/api/orders
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "customerName": "John Doe",
  "items": [
    { "menuId": 1, "quantity": 2 }
  ]
}
```

**Update order status:**
```http
PUT /cashier/api/orders/{orderId}/status
Content-Type: application/json

{
  "status": "CONFIRMED"
}
```

Valid status values: `PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `COMPLETED`, `CANCELLED`

---

### Payments

```http
POST /cashier/api/payments
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "orderNumber": "ORD-A1B2C3D4",
  "paymentMethod": "CASH",
  "cashAmount": 100000
}
```

**Response (cash payment):**
```json
{
  "success": true,
  "data": {
    "success": true,
    "orderNumber": "ORD-A1B2C3D4",
    "message": "Payment successful",
    "change": 13500
  }
}
```

> Cash change is calculated as: `cashAmount - ceil((order.total * 1.1) / 500) * 500`
>
> Invoice is auto-generated on success.

---

### Invoices

**Get invoices by date range:**
```http
GET /cashier/api/invoices/by-date?startDate=2025-01-01&endDate=2025-01-31
```

**Get invoice by order number:**
```http
GET /cashier/api/invoices/order/{orderNumber}
```

**Download invoice PDF:**
```http
GET /cashier/api/invoices/{invoiceId}/pdf
```
Returns `application/pdf`.

**Generate missing invoices (for paid orders without one):**
```http
POST /cashier/api/invoices/generate-missing
```

---

### Menu Management

> To list menus as JSON, use `GET /customer/api/menus` (no auth required). The cashier settings page loads menus server-side via Thymeleaf.

**Create menu:**
```http
POST /cashier/api/menus
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "name": "New Dish",
  "description": "Description",
  "price": 45000,
  "categoryId": 4,
  "imageUrl": "/images/menu/new-dish.jpg",
  "available": true,
  "isPromo": false,
  "promoPrice": null
}
```

**Update menu:**
```http
PUT /cashier/api/menus/{menuId}
Content-Type: application/json

{
  "name": "Updated Name",
  "description": "Updated description",
  "price": 50000,
  "categoryId": 4,
  "available": true,
  "isPromo": true,
  "promoPrice": 45000
}
```

**Toggle menu availability:**
```http
PUT /cashier/api/menus/{menuId}/availability
```

**Delete menu:**
```http
DELETE /cashier/api/menus/{menuId}
```

---

### Category Management

**Get all categories:**
```http
GET /cashier/api/categories
```

**Response:**
```json
{
  "success": true,
  "data": [
    { "id": 1, "name": "SEMUA", "displayOrder": 1 },
    { "id": 2, "name": "PROMO", "displayOrder": 2 }
  ]
}
```

**Create category (JSON — for API/Flutter):**
```http
POST /cashier/api/categories
Content-Type: application/json

{
  "name": "MINUMAN",
  "displayOrder": 5
}
```

**Delete category:**
```http
DELETE /cashier/api/categories/{categoryId}
```

---

## Reports API

All require authentication.

### Sales Report
```http
POST /api/reports/sales
Content-Type: application/json

{
  "startDate": "2025-01-01",
  "endDate": "2025-01-31"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalRevenue": 15000000,
    "totalOrders": 450,
    "averageOrderValue": 33333,
    "revenueByPaymentMethod": {
      "CASH": 10000000,
      "QR_CODE": 5000000
    }
  }
}
```

> Revenue figures are post-tax (multiplied by 1.1 in `ReportServiceImpl`).

Also available via:
```http
GET /cashier/api/reports/sales?startDate=2025-01-01&endDate=2025-01-31
```

### Daily Report
```http
GET /api/reports/daily?date=2025-01-01
```

### Top Selling Items
```http
GET /api/reports/top-items?startDate=2025-01-01&endDate=2025-01-31
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "menu": { "id": 1, "name": "Nasi Goreng Spesial" },
      "quantity": 150,
      "revenue": 5250000
    }
  ]
}
```

### Export Report
```http
GET /api/reports/export?startDate=2025-01-01&endDate=2025-01-31&format=pdf
```

Returns a PDF file download.

---

## WebSocket

Used by the cashier frontend for real-time order updates.

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    stompClient.subscribe('/topic/orders', function(message) {
        const order = JSON.parse(message.body);
    });

    stompClient.subscribe('/topic/dashboard', function(message) {
        // Dashboard stats changed, refetch /cashier/api/dashboard/stats
    });
});
```

**Topics:**
| Topic | Payload |
|-------|---------|
| `/topic/orders` | `OrderResponse` — broadcast on order create, status change, or payment |
| `/topic/dashboard` | String trigger — broadcast after any order/payment change |

---

## Error Codes

| HTTP Status | Meaning |
|-------------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad request / validation error |
| 401 | Not authenticated |
| 403 | Access denied (CSRF or role) |
| 404 | Resource not found |
| 500 | Internal server error |

---

**Last Updated:** 2026-05-02
