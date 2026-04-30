---
name: Fix plan — bugs, security, consistency
description: Comprehensive list of all identified issues (security, bugs, consistency, dead code) to fix before adding Flutter REST layer
type: project
---

A full audit was done (2026-04-28). 24 tracked tasks were created covering:

**Security (S1–S7):** CORS wildcard on BaseController; unauthenticated /simulate-payment endpoint (critical — anyone can mark orders paid); ReportController using session attribute instead of Spring Security; exception messages leaking to API responses; passwordHash in Cashier.toString(); System.out.println instead of SLF4J Logger; session token stored in localStorage.

**Bugs/Consistency (B1–B8):** Constants.TAX_RATE defined but never used (hardcoded 0.10 in 4+ places); QR payment doesn't apply/validate tax but cash payment does; cashier change calculation uses pre-tax order total; CartResponse.total == CartResponse.subtotal (no tax); top-selling items revenue uses current price not historical order price; invoice timestamp set to order creation time instead of payment time; simulate-payment calls createOrder() on existing order; order/invoice number collision risk (System.currentTimeMillis()).

**Duplicate code (D1–D2):** convertToOrderResponse/MenuResponse/OrderItemResponse methods are identical in CustomerController and CashierController; window.clearCart defined in both menu-filter.js and cart-page.js.

**Dead code (U1–U7):** QRCodeGenerator.java unused; HomeController redundant with WebConfig; dead CustomerApp methods (this.cart never initialized); unused OrderService.processPayment(); broken /cashier/api/reports/sales endpoint; ReportUtils.java unused; manual session attributes in AuthController.processLogin() only needed by ReportController's broken auth check.

**Why:** These need to be fixed BEFORE adding a Flutter REST/JWT layer to keep the codebase clean and avoid propagating bugs into the new API surface.

**How to apply:** Check the task list (tasks #1–#24) for current status before starting any fix work.
