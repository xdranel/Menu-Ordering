package menuorderingapp.project.controller;

import menuorderingapp.project.model.*;
import menuorderingapp.project.model.dto.*;
import menuorderingapp.project.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import menuorderingapp.project.util.Constants;
import menuorderingapp.project.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/cashier")
public class CashierController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(CashierController.class);

    private final OrderService orderService;
    private final MenuService menuService;
    private final PaymentService paymentService;
    private final ReportService reportService;
    private final InvoiceService invoiceService;
    private final AuthService authService;
    private final OrderWebSocketController webSocketController;
    private final MenuAuditService menuAuditService;
    private final ObjectMapper objectMapper;
    private final CashierService cashierService;

    public CashierController(OrderService orderService, MenuService menuService,
                             PaymentService paymentService, ReportService reportService,
                             InvoiceService invoiceService, AuthService authService,
                             OrderWebSocketController webSocketController,
                             MenuAuditService menuAuditService, ObjectMapper objectMapper,
                             CashierService cashierService) {
        this.orderService = orderService;
        this.menuService = menuService;
        this.paymentService = paymentService;
        this.reportService = reportService;
        this.invoiceService = invoiceService;
        this.authService = authService;
        this.webSocketController = webSocketController;
        this.menuAuditService = menuAuditService;
        this.objectMapper = objectMapper;
        this.cashierService = cashierService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpSession session) {
        if (!isAuthenticatedCashier()) {
            return "redirect:/auth/login";
        }

        long pendingOrders = orderService.getPendingOrdersCount();
        double todayRevenue = orderService.getTotalRevenueToday();
        List<Order> recentOrders = orderService.getTodayOrders();

        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("todayRevenue", todayRevenue);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("cashier", session.getAttribute("cashier"));
        model.addAttribute("currentPath", "/cashier/dashboard");

        return "cashier/dashboard";
    }

    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(HttpSession session) {
        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            long pendingOrders = orderService.getPendingOrdersCount();
            double todayRevenue = orderService.getTotalRevenueToday();
            List<Order> recentOrders = orderService.getTodayOrders();
            long availableMenus = menuService.getAvailableMenus().size();
            long todayOrdersCount = (long) recentOrders.size();

            List<OrderResponse> orderResponses = recentOrders.stream()
                    .map(this::convertToOrderResponse)
                    .collect(Collectors.toList());

            DashboardStatsResponse stats = new DashboardStatsResponse(
                    todayRevenue,
                    todayOrdersCount,
                    pendingOrders,
                    availableMenus,
                    orderResponses
            );

            return success(stats);

        } catch (Exception e) {
            log.error("Failed to fetch dashboard stats: {}", e.getMessage(), e);
            return error("Failed to fetch dashboard stats");
        }
    }

    // Orders Management Page
    @GetMapping("/orders")
    public String showOrdersPage(Model model, HttpSession session) {
        if (!isAuthenticatedCashier()) {
            return "redirect:/auth/login";
        }

        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);
        model.addAttribute("cashier", session.getAttribute("cashier"));
        model.addAttribute("currentPath", "/cashier/orders");

        return "cashier/orders";
    }

    // Get All Orders API
    @GetMapping("/api/orders/all")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(HttpSession session) {
        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            List<Order> orders = orderService.getAllOrders();


            List<OrderResponse> orderResponses = orders.stream()
                    .map(this::convertToOrderResponse)
                    .collect(Collectors.toList());

            return success(orderResponses);

        } catch (Exception e) {
            log.error("Failed to fetch orders: {}", e.getMessage(), e);
            return error("Failed to fetch orders");
        }
    }

    // Get Today's Orders API
    @GetMapping("/api/orders/today")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getTodayOrders(HttpSession session) {
        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            List<Order> orders = orderService.getTodayOrders();

            List<OrderResponse> orderResponses = orders.stream()
                    .map(this::convertToOrderResponse)
                    .collect(Collectors.toList());

            return success(orderResponses);

        } catch (Exception e) {
            log.error("Failed to fetch today's orders: {}", e.getMessage(), e);
            return error("Failed to fetch today's orders");
        }
    }

    // Get Orders by Date Range API
    @GetMapping("/api/orders/by-date")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {
        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

            List<Order> orders = orderService.getOrdersByDateRange(startOfDay, endOfDay);

            List<OrderResponse> orderResponses = orders.stream()
                    .map(this::convertToOrderResponse)
                    .collect(Collectors.toList());

            return success(orderResponses);

        } catch (Exception e) {
            log.error("Failed to fetch orders by date: {}", e.getMessage(), e);
            return error("Failed to fetch orders by date");
        }
    }

    // Create New Order (Cashier Assisted)
    @PostMapping("/api/orders")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> createCashierOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            HttpSession session) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            Long cashierId = SecurityUtils.getCurrentCashierId();
            if (cashierId == null) {
                return error("Cashier ID not found in security context");
            }

            Optional<Cashier> cashierOpt = cashierService.getCashierById(cashierId);
            if (cashierOpt.isEmpty()) {
                return error("Cashier not found with id: " + cashierId);
            }

            Order order = new Order();
            order.setOrderType(Order.OrderType.CASHIER_ASSISTED);
            order.setCustomerName(orderRequest.getCustomerName());
            order.setStatus(Order.OrderStatus.PENDING);
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
            order.setCashier(cashierOpt.get());

            // Add items to order
            for (OrderItemRequest itemRequest : orderRequest.getItems()) {
                Optional<Menu> menu = menuService.getMenuById(itemRequest.getMenuId());
                if (menu.isPresent() && menu.get().getAvailable()) {
                    OrderItem orderItem = new OrderItem(menu.get(), itemRequest.getQuantity());
                    order.addOrderItem(orderItem);
                }
            }

            Order savedOrder = orderService.createOrder(order);
            OrderResponse orderResponse = convertToOrderResponse(savedOrder);

            // Broadcast order creation via WebSocket
            webSocketController.broadcastOrderUpdate(orderResponse);
            webSocketController.broadcastDashboardUpdate();

            return created(orderResponse);

        } catch (Exception e) {
            log.error("Failed to create order: {}", e.getMessage(), e);
            return error("Failed to create order");
        }
    }

    // Update Order Status
    @PutMapping("/api/orders/{orderId}/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) Map<String, String> body) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            String statusStr = status != null ? status : (body != null ? body.get("status") : null);
            if (statusStr == null || statusStr.isBlank()) {
                return error("Status field is required");
            }
            Order.OrderStatus orderStatus;
            try {
                orderStatus = Order.OrderStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return error("Invalid status value: " + statusStr);
            }

            Order updatedOrder = orderService.updateOrderStatus(orderId, orderStatus);
            OrderResponse orderResponse = convertToOrderResponse(updatedOrder);

            webSocketController.broadcastOrderUpdate(orderResponse);
            webSocketController.broadcastDashboardUpdate();

            return success("Order status updated", orderResponse);

        } catch (Exception e) {
            log.error("Failed to update order status for order {}: {}", orderId, e.getMessage(), e);
            return error("Failed to update order status");
        }
    }

    // Process Payment (Cashier)
    @PostMapping("/api/payments")
    @ResponseBody
    public ResponseEntity<ApiResponse<PaymentResponse>> processCashierPayment(
            @Valid @RequestBody PaymentRequest paymentRequest,
            HttpSession session) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            boolean paymentSuccess = false;

            if (paymentRequest.getPaymentMethod() == Order.PaymentMethod.QR_CODE) {
                paymentSuccess = paymentService.processQRPayment(
                        paymentRequest.getOrderNumber(),
                        paymentRequest.getQrData()
                );
            } else if (paymentRequest.getPaymentMethod() == Order.PaymentMethod.CASH) {
                paymentSuccess = paymentService.processCashPayment(
                        paymentRequest.getOrderNumber(),
                        paymentRequest.getCashAmount()
                );
            }

            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setSuccess(paymentSuccess);
            paymentResponse.setOrderNumber(paymentRequest.getOrderNumber());
            paymentResponse.setMessage(paymentSuccess ? "Payment successful" : "Payment failed");

            // Calculate change for cash payments (cashAmount - rounded post-tax total)
            if (paymentSuccess && paymentRequest.getPaymentMethod() == Order.PaymentMethod.CASH) {
                Optional<Order> orderOpt = orderService.getOrderByNumber(paymentRequest.getOrderNumber());
                if (orderOpt.isPresent()) {
                    double subtotal = orderOpt.get().getTotal().doubleValue();
                    double exactAmount = subtotal * (1 + Constants.TAX_RATE);
                    double roundedAmount = Math.ceil(exactAmount / Constants.CASH_ROUNDING_UNIT) * Constants.CASH_ROUNDING_UNIT;
                    double change = paymentRequest.getCashAmount() - roundedAmount;
                    paymentResponse.setChange(change > 0 ? change : 0);
                }
            }

            if (paymentSuccess) {
                // Generate invoice
                var currentCashier = SecurityUtils.getCurrentCashier();
                Optional<Order> orderOpt = orderService.getOrderByNumber(paymentRequest.getOrderNumber());
                if (orderOpt.isPresent() && currentCashier != null) {
                    invoiceService.generateInvoice(orderOpt.get(), currentCashier.getCashierId());

                    // Broadcast payment update via WebSocket
                    OrderResponse orderResponse = convertToOrderResponse(orderOpt.get());
                    webSocketController.broadcastOrderUpdate(orderResponse);
                    webSocketController.broadcastDashboardUpdate();
                }

                return success(paymentResponse);
            } else {
                return error("Payment processing failed");
            }

        } catch (Exception e) {
            log.error("Payment error for order {}: {}", paymentRequest.getOrderNumber(), e.getMessage(), e);
            return error("Payment failed");
        }
    }

    @GetMapping("/reports")
    public String showReportsPage(Model model, HttpSession session) {
        if (!isAuthenticatedCashier()) {
            return "redirect:/auth/login";
        }

        model.addAttribute("cashier", session.getAttribute("cashier"));
        model.addAttribute("currentPath", "/cashier/reports");
        return "cashier/reports";
    }

    @GetMapping("/api/reports/sales")
    @ResponseBody
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now();
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

            java.util.Map<String, Object> reportData = reportService.getSalesReport(
                    start.atStartOfDay(), end.atTime(LocalTime.MAX));

            SalesReportResponse response = new SalesReportResponse();
            response.setStartDate(start.atStartOfDay());
            response.setEndDate(end.atTime(LocalTime.MAX));
            response.setTotalRevenue((Double) reportData.get("totalRevenue"));
            response.setTotalOrders((Long) reportData.get("totalOrders"));
            response.setAverageOrderValue((Double) reportData.get("averageOrderValue"));

            @SuppressWarnings("unchecked")
            java.util.Map<Order.PaymentMethod, Double> rawMap =
                    (java.util.Map<Order.PaymentMethod, Double>) reportData.get("revenueByPaymentMethod");
            if (rawMap != null) {
                java.util.Map<String, Double> stringKeyMap = new java.util.HashMap<>();
                rawMap.forEach((k, v) -> stringKeyMap.put(k.name(), v));
                response.setRevenueByPaymentMethod(stringKeyMap);
            }

            response.setGeneratedAt(LocalDateTime.now().toString());
            return success(response);
        } catch (Exception e) {
            log.error("Failed to generate report: {}", e.getMessage(), e);
            return error("Failed to generate report");
        }
    }

    // Generate Missing Invoices for Paid Orders
    @PostMapping("/api/invoices/generate-missing")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateMissingInvoices(HttpSession session) {
        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            // Get cashier from SecurityContext
            var currentCashier = SecurityUtils.getCurrentCashier();
            if (currentCashier == null) {
                return error("Cashier not found in security context");
            }

            Long cashierId = currentCashier.getCashierId();
            log.info("Generating missing invoices for cashier ID: {}", cashierId);

            List<Order> allOrders = orderService.getAllOrders();
            List<Order> paidOrders = allOrders.stream()
                    .filter(order -> order.getPaymentStatus() == Order.PaymentStatus.PAID)
                    .collect(Collectors.toList());

            int createdCount = 0;
            int skippedCount = 0;

            for (Order order : paidOrders) {
                Optional<Invoice> existingInvoice = invoiceService.getInvoiceByOrder(order);
                if (existingInvoice.isEmpty()) {
                    invoiceService.generateInvoice(order, cashierId);
                    createdCount++;
                } else {
                    skippedCount++;
                }
            }

            log.info("Invoice generation complete — created: {}, skipped: {}", createdCount, skippedCount);

            Map<String, Object> result = new HashMap<>();
            result.put("totalPaidOrders", paidOrders.size());
            result.put("invoicesCreated", createdCount);
            result.put("invoicesSkipped", skippedCount);

            return success("Successfully generated missing invoices", result);
        } catch (Exception e) {
            log.error("Failed to generate missing invoices: {}", e.getMessage(), e);
            return error("Failed to generate missing invoices");
        }
    }

    // Get Invoices by Date Range API
    @GetMapping("/api/invoices/by-date")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByDateRange(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            HttpSession session) {

        log.debug("Fetching invoices by date range: {} to {}", startDate, endDate);

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            List<Invoice> invoices = invoiceService.getInvoicesByDateRange(startDate, endDate);
            List<InvoiceResponse> invoiceResponses = invoices.stream()
                    .map(this::convertToInvoiceResponse)
                    .collect(Collectors.toList());
            return success(invoiceResponses);
        } catch (Exception e) {
            log.error("Failed to fetch invoices for range {} to {}: {}", startDate, endDate, e.getMessage(), e);
            return error("Failed to fetch invoices");
        }
    }

    @GetMapping("/settings")
    public String showSettingsPage(Model model, HttpSession session) {
        if (!isAuthenticatedCashier()) {
            return "redirect:/auth/login";
        }

        List<Menu> menus = menuService.getAllMenus();
        List<Category> categories = menuService.getAllCategories();

        model.addAttribute("menus", menus);
        model.addAttribute("categories", categories);
        model.addAttribute("menuRequest", new MenuRequest());
        model.addAttribute("categoryRequest", new CategoryRequest());
        model.addAttribute("cashier", session.getAttribute("cashier"));
        model.addAttribute("currentPath", "/cashier/settings");

        return "cashier/settings";
    }

    // Update Menu Availability
    @PutMapping("/api/menus/{menuId}/availability")
    @ResponseBody
    public ResponseEntity<ApiResponse<MenuResponse>> toggleMenuAvailability(
            @PathVariable Long menuId,
            HttpSession session) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            var currentCashierDetails = SecurityUtils.getCurrentCashier();
            if (currentCashierDetails == null) {
                return error("Cashier not found in security context");
            }
            Cashier currentCashier = currentCashierDetails.getCashier();

            // Get current menu state before toggle
            Optional<Menu> menuOpt = menuService.getMenuById(menuId);
            if (menuOpt.isEmpty()) {
                return error("Menu not found");
            }
            Menu menu = menuOpt.get();
            boolean oldAvailability = menu.getAvailable();

            // Toggle availability
            Menu updatedMenu = menuService.toggleMenuAvailability(menuId);

            // Log availability change
            menuAuditService.logAvailabilityChange(updatedMenu, currentCashier, oldAvailability, updatedMenu.getAvailable());

            MenuResponse response = convertToMenuResponse(updatedMenu);
            return success("Menu availability updated", response);

        } catch (Exception e) {
            log.error("Failed to toggle availability for menu {}: {}", menuId, e.getMessage(), e);
            return error("Failed to update menu availability");
        }
    }

    // Create New Menu
    @PostMapping("/api/menus")
    @ResponseBody
    public ResponseEntity<ApiResponse<MenuResponse>> createMenu(
            @Valid @RequestBody MenuRequest menuRequest,
            HttpSession session) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            var currentCashierDetails = SecurityUtils.getCurrentCashier();
            if (currentCashierDetails == null) {
                return error("Cashier not found in security context");
            }
            Cashier currentCashier = currentCashierDetails.getCashier();

            // Get category
            Optional<Category> categoryOpt = menuService.getCategoryById(menuRequest.getCategoryId());
            if (categoryOpt.isEmpty()) {
                return error("Category not found");
            }

            // Create new menu
            Menu newMenu = new Menu();
            newMenu.setName(menuRequest.getName());
            newMenu.setDescription(menuRequest.getDescription());
            newMenu.setPrice(menuRequest.getPrice());
            newMenu.setCategory(categoryOpt.get());
            newMenu.setAvailable(menuRequest.getAvailable());
            newMenu.setImageUrl(menuRequest.getImageUrl());
            newMenu.setIsPromo(menuRequest.getIsPromo());
            newMenu.setPromoPrice(menuRequest.getIsPromo() ? menuRequest.getPromoPrice() : null);

            Menu savedMenu = menuService.saveMenu(newMenu);

            // Log the creation in audit log
            menuAuditService.logMenuCreate(savedMenu, currentCashier);

            MenuResponse response = convertToMenuResponse(savedMenu);
            return created(response);

        } catch (Exception e) {
            log.error("Failed to create menu: {}", e.getMessage(), e);
            return error("Failed to create menu");
        }
    }

    // Update Menu
    @PutMapping("/api/menus/{menuId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<MenuResponse>> updateMenu(
            @PathVariable Long menuId,
            @Valid @RequestBody MenuRequest menuRequest,
            HttpSession session) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            var currentCashierDetails = SecurityUtils.getCurrentCashier();
            if (currentCashierDetails == null) {
                return error("Cashier not found in security context");
            }
            Cashier currentCashier = currentCashierDetails.getCashier();

            // Get the existing menu for audit logging
            Optional<Menu> existingMenuOpt = menuService.getMenuById(menuId);
            if (existingMenuOpt.isEmpty()) {
                return error("Menu not found");
            }
            Menu existingMenu = existingMenuOpt.get();

            // Store old values for audit
            String oldValues = menuToJson(existingMenu);
            BigDecimal oldPrice = existingMenu.getPrice();

            // Build updated menu details
            Menu menuDetails = new Menu();
            menuDetails.setName(menuRequest.getName());
            menuDetails.setDescription(menuRequest.getDescription());
            menuDetails.setImageUrl(menuRequest.getImageUrl());
            menuDetails.setPrice(menuRequest.getPrice());
            menuDetails.setAvailable(menuRequest.getAvailable());
            menuDetails.setIsPromo(menuRequest.getIsPromo());
            menuDetails.setPromoPrice(menuRequest.getPromoPrice());

            // Set category if provided
            if (menuRequest.getCategoryId() != null) {
                Optional<Category> category = menuService.getCategoryById(menuRequest.getCategoryId());
                category.ifPresent(menuDetails::setCategory);
            }

            // Update the menu
            Menu updatedMenu = menuService.updateMenu(menuId, menuDetails);

            // Store new values for audit
            String newValues = menuToJson(updatedMenu);

            // Log the update
            menuAuditService.logMenuUpdate(updatedMenu, currentCashier, oldValues, newValues);

            // Log price change separately if price changed
            if (oldPrice.compareTo(updatedMenu.getPrice()) != 0) {
                menuAuditService.logPriceChange(updatedMenu, currentCashier, oldPrice.doubleValue(), updatedMenu.getPrice().doubleValue());
            }

            MenuResponse response = convertToMenuResponse(updatedMenu);
            return success("Menu updated successfully", response);

        } catch (Exception e) {
            log.error("Failed to update menu {}: {}", menuId, e.getMessage(), e);
            return error("Failed to update menu");
        }
    }

    // Delete Menu
    @DeleteMapping("/api/menus/{menuId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> deleteMenu(
            @PathVariable Long menuId,
            HttpSession session) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            var currentCashierDetails = SecurityUtils.getCurrentCashier();
            if (currentCashierDetails == null) {
                return error("Cashier not found in security context");
            }

            Cashier currentCashier = currentCashierDetails.getCashier();

            // Get menu before deleting for audit log
            Optional<Menu> menuOpt = menuService.getMenuById(menuId);
            if (menuOpt.isEmpty()) {
                return error("Menu not found");
            }

            Menu menu = menuOpt.get();

            // Log the deletion to audit
            menuAuditService.logMenuDelete(menu, currentCashier);

            // Delete the menu
            menuService.deleteMenu(menuId);

            return success("Menu berhasil dihapus", "Deleted");

        } catch (Exception e) {
            log.error("Failed to delete menu {}: {}", menuId, e.getMessage(), e);
            return error("Failed to delete menu");
        }
    }

    private boolean isAuthenticatedCashier() {
        return SecurityUtils.getCurrentCashier() != null;
    }
    // Get All Categories
    @GetMapping("/api/categories")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        try {
            List<Category> categories = menuService.getAllCategories();
            List<CategoryResponse> categoryResponses = categories.stream()
                    .map(cat -> {
                        CategoryResponse response = new CategoryResponse();
                        response.setId(cat.getId());
                        response.setName(cat.getName());
                        response.setDisplayOrder(cat.getDisplayOrder());
                        return response;
                    })
                    .collect(Collectors.toList());

            return success(categoryResponses);

        } catch (Exception e) {
            log.error("Failed to fetch categories: {}", e.getMessage(), e);
            return error("Failed to fetch categories");
        }
    }

    // Create New Category
    @PostMapping("/api/categories")
    public String createCategory(
            @Valid @ModelAttribute CategoryRequest categoryRequest,
            HttpSession session) {

        if (!isAuthenticatedCashier()) {
            return "redirect:/auth/login";
        }

        try {
            // Create new category
            Category newCategory = new Category();
            newCategory.setName(categoryRequest.getName());
            newCategory.setDisplayOrder(categoryRequest.getDisplayOrder());

            // Save category
            menuService.saveCategory(newCategory);

            // Redirect back to settings page
            return "redirect:/cashier/settings";

        } catch (Exception e) {
            log.error("Failed to create category: {}", e.getMessage(), e);
            return "redirect:/cashier/settings?error=Failed+to+create+category";
        }
    }

    // Create Category (JSON — for Flutter/API clients)
    @PostMapping(value = "/api/categories", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategoryJson(
            @Valid @RequestBody CategoryRequest categoryRequest) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            Category newCategory = new Category();
            newCategory.setName(categoryRequest.getName());
            newCategory.setDisplayOrder(categoryRequest.getDisplayOrder());
            menuService.saveCategory(newCategory);

            CategoryResponse response = new CategoryResponse();
            response.setId(newCategory.getId());
            response.setName(newCategory.getName());
            response.setDisplayOrder(newCategory.getDisplayOrder());
            return created(response);

        } catch (Exception e) {
            log.error("Failed to create category: {}", e.getMessage(), e);
            return error("Failed to create category");
        }
    }

    // Delete Category
    @DeleteMapping("/api/categories/{categoryId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long categoryId,
            HttpSession session) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            menuService.deleteCategory(categoryId);
            return success("Category deleted successfully", null);

        } catch (Exception e) {
            log.error("Failed to delete category {}: {}", categoryId, e.getMessage(), e);
            return error("Failed to delete category");
        }
    }

    // Get Invoice by Order Number
    @GetMapping("/api/invoices/order/{orderNumber}")
    @ResponseBody
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByOrderNumber(
            @PathVariable String orderNumber) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            Optional<Order> orderOpt = orderService.getOrderByNumber(orderNumber);
            if (orderOpt.isEmpty()) {
                return error("Order not found");
            }

            Optional<Invoice> invoiceOpt = invoiceService.getInvoiceByOrder(orderOpt.get());
            if (invoiceOpt.isEmpty()) {
                return error("Invoice not found for this order");
            }

            return success(convertToInvoiceResponse(invoiceOpt.get()));

        } catch (Exception e) {
            log.error("Failed to retrieve invoice for order {}: {}", orderNumber, e.getMessage(), e);
            return error("Failed to retrieve invoice");
        }
    }

    // Download Invoice PDF
    @GetMapping("/api/invoices/{invoiceId}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable Long invoiceId,
            HttpSession session) {

        if (!isAuthenticatedCashier()) {
            return ResponseEntity.status(401).build();
        }

        try {
            byte[] pdfBytes = invoiceService.generateInvoicePdf(invoiceId);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=invoice-" + invoiceId + ".pdf")
                    .body(pdfBytes);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // Helper method to convert Menu to JSON for audit logging
    private String menuToJson(Menu menu) {
        try {
            Map<String, Object> menuData = new HashMap<>();
            menuData.put("name", menu.getName());
            menuData.put("description", menu.getDescription());
            menuData.put("price", menu.getPrice());
            menuData.put("category", menu.getCategory() != null ? menu.getCategory().getName() : null);
            menuData.put("available", menu.getAvailable());
            menuData.put("isPromo", menu.getIsPromo());
            menuData.put("promoPrice", menu.getPromoPrice());
            menuData.put("imageUrl", menu.getImageUrl());
            return objectMapper.writeValueAsString(menuData);
        } catch (Exception e) {
            return "{}";
        }
    }
}
