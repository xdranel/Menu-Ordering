# API Documentation - ChopChop Restaurant

REST API endpoints documentation for the Menu Ordering Application.

## Table of Contents

- [Base URL](#base-url)
- [Authentication](#authentication)
- [Response Format](#response-format)
- [Customer API](#customer-api)
- [Cashier API](#cashier-api)
- [Cart API](#cart-api)
- [Order API](#order-api)
- [Payment API](#payment-api)
- [Menu Management API](#menu-management-api)
- [Reports API](#reports-api)
- [Error Codes](#error-codes)

## Base URL

```
http://localhost:8080
```

## Authentication

The application uses **session-based authentication** with Spring Security.

### Login
```http
POST /api/auth/login
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
    "cashier": {
      "id": 1,
      "username": "admin",
      "displayName": "Administrator",
      "role": "ADMIN"
    },
    "sessionToken": "uuid-session-token"
  }
}
```

### Logout
```http
POST /api/auth/logout
```

**Response:**
```json
{
  "success": true,
  "message": "Logout successful"
}
```

## Response Format

All API responses follow this standard format:

```json
{
  "success": boolean,
  "message": "string",
  "data": object|array,
  "timestamp": "ISO 8601 datetime"
}
```

### Success Response
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2025-01-01T12:00:00"
}
```

### Error Response
```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2025-01-01T12:00:00"
}
```

## Customer API

### Get All Categories
```http
GET /api/categories
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "SEMUA",
      "displayOrder": 1
    },
    {
      "id": 2,
      "name": "PROMO",
      "displayOrder": 2
    }
  ]
}
```

### Get Available Menus
```http
GET /api/menus
```

**Query Parameters:**
- `category` (optional): Filter by category ID

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
      "category": {
        "id": 4,
        "name": "MAKANAN UTAMA"
      },
      "available": true,
      "isPromo": false,
      "promoPrice": null
    }
  ]
}
```

### Search Menus
```http
GET /api/menus/search?q={searchTerm}
```

**Parameters:**
- `q`: Search term (name or description)

**Response:** Same as Get Available Menus

### Get Promo Items
```http
GET /api/menus/promo
```

**Response:** Same as Get Available Menus

## Cart API

### Get Cart
```http
GET /api/cart
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
        "subtotal": 70000
      }
    ],
    "total": 70000,
    "itemCount": 2
  }
}
```

### Add Item to Cart
```http
POST /api/cart/add
Content-Type: application/json

{
  "menuId": 1,
  "quantity": 2
}
```

**Response:**
```json
{
  "success": true,
  "message": "Item added to cart",
  "data": {
    "items": [...],
    "total": 70000,
    "itemCount": 2
  }
}
```

### Update Cart Item
```http
PUT /api/cart/update
Content-Type: application/json

{
  "menuId": 1,
  "quantity": 3
}
```

### Remove from Cart
```http
DELETE /api/cart/remove/{menuId}
```

### Clear Cart
```http
DELETE /api/cart/clear
```

## Order API

### Create Order
```http
POST /api/orders
Content-Type: application/json

{
  "customerName": "John Doe",
  "orderType": "CUSTOMER_SELF",
  "tableNumber": "5",
  "notes": "Extra spicy",
  "items": [
    {
      "menuId": 1,
      "quantity": 2
    }
  ]
}
```

**Order Types:**
- `CUSTOMER_SELF`
- `CASHIER_ASSISTED`

**Response:**
```json
{
  "success": true,
  "message": "Order created successfully",
  "data": {
    "id": 1,
    "orderNumber": "ORD-20250101-001",
    "customerName": "John Doe",
    "total": 70000,
    "status": "PENDING",
    "paymentStatus": "UNPAID",
    "orderType": "CUSTOMER_SELF",
    "createdAt": "2025-01-01T12:00:00"
  }
}
```

### Get Order by Number
```http
GET /api/orders/{orderNumber}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderNumber": "ORD-20250101-001",
    "customerName": "John Doe",
    "total": 70000,
    "status": "PENDING",
    "paymentStatus": "UNPAID",
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

## Payment API

### Process Cash Payment
```http
POST /api/payment/cash
Content-Type: application/json

{
  "orderNumber": "ORD-20250101-001",
  "amountTendered": 100000
}
```

**Response:**
```json
{
  "success": true,
  "message": "Payment processed successfully",
  "data": {
    "orderNumber": "ORD-20250101-001",
    "paymentMethod": "CASH",
    "total": 70000,
    "amountTendered": 100000,
    "change": 30000,
    "paymentStatus": "PAID"
  }
}
```

### Generate QR Code
```http
POST /api/payment/qr-code
Content-Type: application/json

{
  "orderNumber": "ORD-20250101-001"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "qrCode": "data:image/png;base64,iVBORw0KG...",
    "orderNumber": "ORD-20250101-001",
    "amount": 70000
  }
}
```

### Process QR Payment
```http
POST /api/payment/qr
Content-Type: application/json

{
  "orderNumber": "ORD-20250101-001",
  "qrData": "payment_data_string"
}
```

## Cashier API

****Authentication Required:** All cashier endpoints require valid cashier session.

### Get Dashboard Stats
```http
GET /api/cashier/dashboard/stats
```

**Response:**
```json
{
  "success": true,
  "data": {
    "todayRevenue": 1500000,
    "todayOrders": 45,
    "pendingOrders": 8,
    "activeCashiers": 3
  }
}
```

### Get All Orders
```http
GET /api/cashier/orders
```

**Query Parameters:**
- `status` (optional): PENDING, CONFIRMED, PREPARING, READY, COMPLETED, CANCELLED

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "orderNumber": "ORD-20250101-001",
      "customerName": "John Doe",
      "total": 70000,
      "status": "PENDING",
      "paymentStatus": "UNPAID",
      "orderType": "CUSTOMER_SELF",
      "tableNumber": "5",
      "createdAt": "2025-01-01T12:00:00"
    }
  ]
}
```

### Update Order Status
```http
PUT /api/cashier/orders/{orderId}/status
Content-Type: application/json

{
  "status": "CONFIRMED"
}
```

**Valid Status Transitions:**
- PENDING → CONFIRMED
- CONFIRMED → PREPARING
- PREPARING → READY
- READY → COMPLETED
- Any → CANCELLED

### Process Order Payment
```http
POST /api/cashier/orders/{orderId}/payment
Content-Type: application/json

{
  "paymentMethod": "CASH",
  "amountTendered": 100000
}
```

**Payment Methods:**
- `CASH`
- `QR_CODE`
- `CARD`

### Generate Invoice
```http
POST /api/cashier/orders/{orderId}/invoice
```

**Response:**
```json
{
  "success": true,
  "data": {
    "invoiceNumber": "INV-20250101-001",
    "orderNumber": "ORD-20250101-001",
    "totalAmount": 70000,
    "taxAmount": 7000,
    "finalAmount": 77000,
    "cashier": "Administrator",
    "createdAt": "2025-01-01T12:00:00"
  }
}
```

## Menu Management API

****Admin Access Required**

### Get All Menus (Admin)
```http
GET /api/cashier/menus
```

### Create Menu
```http
POST /api/cashier/menus
Content-Type: application/json

{
  "name": "New Dish",
  "description": "Description here",
  "price": 45000,
  "categoryId": 4,
  "imageUrl": "/images/menu/new-dish.jpg",
  "available": true,
  "isPromo": false
}
```

### Update Menu
```http
PUT /api/cashier/menus/{menuId}
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

### Toggle Menu Availability
```http
PUT /api/cashier/menus/{menuId}/toggle-availability
```

### Delete Menu
```http
DELETE /api/cashier/menus/{menuId}
```

### Get Menu Audit Logs
```http
GET /api/cashier/menus/{menuId}/audit-logs
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "menuName": "Nasi Goreng Spesial",
      "action": "PRICE_CHANGED",
      "cashier": "Administrator",
      "oldValues": "{\"price\":35000}",
      "newValues": "{\"price\":40000}",
      "createdAt": "2025-01-01T12:00:00"
    }
  ]
}
```

## Reports API

****Authentication Required**

### Generate Sales Report
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
    "startDate": "2025-01-01",
    "endDate": "2025-01-31",
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

### Get Daily Report
```http
GET /api/reports/daily?date=2025-01-01
```

### Get Top Selling Items
```http
GET /api/reports/top-items?startDate=2025-01-01&endDate=2025-01-31
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "menu": {
        "id": 1,
        "name": "Nasi Goreng Spesial"
      },
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

**Query Parameters:**
- `startDate`: Start date (YYYY-MM-DD)
- `endDate`: End date (YYYY-MM-DD)
- `format`: pdf (currently only PDF supported)

**Response:** PDF file download

## WebSocket API

### Connect to WebSocket
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);

    // Subscribe to order updates
    stompClient.subscribe('/topic/orders', function(message) {
        const order = JSON.parse(message.body);
        console.log('New order:', order);
    });

    // Subscribe to dashboard updates
    stompClient.subscribe('/topic/dashboard', function(message) {
        console.log('Dashboard update:', message.body);
    });
});
```

### WebSocket Topics
- `/topic/orders` - Real-time order updates
- `/topic/dashboard` - Dashboard statistics updates
- `/topic/pong` - Ping/pong for connection testing

### Send Message
```javascript
stompClient.send("/app/ping", {}, "test message");
```

## Error Codes

### HTTP Status Codes
- `200 OK` - Successful request
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

**Last Updated:** 2025-01-01
