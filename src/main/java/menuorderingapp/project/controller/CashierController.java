package menuorderingapp.project.controller;

import menuorderingapp.project.model.*;
import menuorderingapp.project.model.dto.*;
import menuorderingapp.project.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import menuorderingapp.project.util.SecurityUtils;
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
public class CashierController extends BaseController{

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
            return error("Failed to fetch dashboard stats: " + e.getMessage());
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
            return error("Failed to fetch orders: " + e.getMessage());
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
            return error("Failed to fetch today's orders: " + e.getMessage());
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
            return error("Failed to fetch orders by date: " + e.getMessage());
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
            return error("Failed to create order: " + e.getMessage());
        }
    }

    // Update Order Status
    @PutMapping("/api/orders/{orderId}/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam Order.OrderStatus status,
            HttpSession session) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            Order updatedOrder = orderService.updateOrderStatus(orderId, status);
            OrderResponse orderResponse = convertToOrderResponse(updatedOrder);

            // Broadcast order update via WebSocket
            webSocketController.broadcastOrderUpdate(orderResponse);
            webSocketController.broadcastDashboardUpdate();

            return success("Order status updated", orderResponse);

        } catch (Exception e) {
            return error("Failed to update order status: " + e.getMessage());
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

            // Calculate change for cash payments
            if (paymentSuccess && paymentRequest.getPaymentMethod() == Order.PaymentMethod.CASH) {
                Optional<Order> orderOpt = orderService.getOrderByNumber(paymentRequest.getOrderNumber());
                if (orderOpt.isPresent()) {
                    double change = paymentRequest.getCashAmount() - orderOpt.get().getTotal().doubleValue();
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
            return error("Payment error: " + e.getMessage());
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
            @RequestParam(required = false) String endDate,
            HttpSession session) {

        if (!isAuthenticatedCashier()) {
            return unauthorized("Not authenticated");
        }

        try {
            SalesReportResponse response = new SalesReportResponse();
            return success(response);
        } catch (Exception e) {
            return error("Failed to generate report: " + e.getMessage());
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
            System.out.println("Generating missing invoices for cashier ID: " + cashierId);

            // Get all PAID orders
            List<Order> allOrders = orderService.getAllOrders();
            List<Order> paidOrders = allOrders.stream()
                    .filter(order -> order.getPaymentStatus() == Order.PaymentStatus.PAID)
                    .collect(Collectors.toList());

            System.out.println("Found " + paidOrders.size() + " paid orders");

            int createdCount = 0;
            int skippedCount = 0;

            for (Order order : paidOrders) {
                // Check if invoice already exists
                Optional<Invoice> existingInvoice = invoiceService.getInvoiceByOrder(order);
                if (existingInvoice.isEmpty()) {
                    invoiceService.generateInvoice(order, cashierId);
                    createdCount++;
                } else {
                    skippedCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalPaidOrders", paidOrders.size());
            result.put("invoicesCreated", createdCount);
            result.put("invoicesSkipped", skippedCount);

            System.out.println("Created " + createdCount + " invoices, skipped " + skippedCount);

            return success("Successfully generated missing invoices", result);
        } catch (Exception e) {
            System.err.println("ERROR generating missing invoices: " + e.getMessage());
            e.printStackTrace();
            return error("Failed to generate missing invoices: " + e.getMessage());
        }
    }

    // Get Invoices by Date Range API
    @GetMapping("/api/invoices/by-date")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByDateRange(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            HttpSession session) {

        System.out.println("=== GET INVOICES BY DATE RANGE ===");
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);

        if (!isAuthenticatedCashier()) {
            System.out.println("ERROR: Not authenticated");
            return unauthorized("Not authenticated");
        }

        try {
            List<Invoice> invoices = invoiceService.getInvoicesByDateRange(startDate, endDate);
            System.out.println("Found " + invoices.size() + " invoices");

            List<InvoiceResponse> invoiceResponses = invoices.stream()
                    .map(this::convertToInvoiceResponse)
                    .collect(Collectors.toList());
            System.out.println("Converted to " + invoiceResponses.size() + " responses");

            return success(invoiceResponses);
        } catch (Exception e) {
            System.err.println("ERROR fetching invoices: " + e.getMessage());
            e.printStackTrace();
            return error("Failed to fetch invoices: " + e.getMessage());
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
            return error("Failed to update menu: " + e.getMessage());
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
            return error("Failed to create menu: " + e.getMessage());
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
            return error("Failed to update menu: " + e.getMessage());
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
            return error("Failed to delete menu: " + e.getMessage());
        }
    }

    private boolean isAuthenticatedCashier() {
        return SecurityUtils.getCurrentCashier() != null;
    }
    private MenuResponse convertToMenuResponse(Menu menu) {
        MenuResponse response = new MenuResponse();
        response.setId(menu.getId());
        response.setName(menu.getName());
        response.setDescription(menu.getDescription());
        response.setPrice(menu.getPrice());
        response.setImageUrl(menu.getImageUrl());
        response.setAvailable(menu.getAvailable());
        response.setIsPromo(menu.getIsPromo());
        response.setPromoPrice(menu.getPromoPrice());
        response.setCurrentPrice(menu.getCurrentPrice());

        if (menu.getCategory() != null) {
            CategoryResponse categoryResponse = new CategoryResponse();
            categoryResponse.setId(menu.getCategory().getId());
            categoryResponse.setName(menu.getCategory().getName());
            response.setCategory(categoryResponse);
        }

        return response;
    }

    private OrderResponse convertToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setTotal(order.getTotal());
        response.setStatus(order.getStatus());
        response.setOrderType(order.getOrderType());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setCustomerName(order.getCustomerName());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        // Convert order items
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(this::convertToOrderItemResponse)
                .collect(Collectors.toList());
        response.setItems(itemResponses);

        return response;
    }

    private OrderItemResponse convertToOrderItemResponse(OrderItem orderItem) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(orderItem.getId());
        response.setQuantity(orderItem.getQuantity());
        response.setPrice(orderItem.getPrice());
        response.setSubtotal(orderItem.getSubtotal());

        if (orderItem.getMenu() != null) {
            response.setMenu(convertToMenuResponse(orderItem.getMenu()));
        }

        return response;
    }

    private InvoiceResponse convertToInvoiceResponse(Invoice invoice) {
        InvoiceResponse response = new InvoiceResponse();
        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setTaxAmount(invoice.getTaxAmount());
        response.setFinalAmount(invoice.getFinalAmount());
        response.setPaymentMethod(invoice.getPaymentMethod());
        response.setCreatedAt(invoice.getCreatedAt());

        // Convert order
        if (invoice.getOrder() != null) {
            response.setOrder(convertToOrderResponse(invoice.getOrder()));
        }

        // Convert cashier
        if (invoice.getCashier() != null) {
            CashierDto cashierDto = new CashierDto();
            cashierDto.setId(invoice.getCashier().getId());
            cashierDto.setUsername(invoice.getCashier().getUsername());
            cashierDto.setDisplayName(invoice.getCashier().getDisplayName());
            response.setCashier(cashierDto);
        }

        return response;
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
            return error("Failed to fetch categories: " + e.getMessage());
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
            return error("Failed to delete category: " + e.getMessage());
        }
    }

    // Get Invoice by Order Number
    @GetMapping("/api/invoices/order/{orderNumber}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Invoice>> getInvoiceByOrderNumber(
            @PathVariable String orderNumber,
            HttpSession session) {

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

            return success(invoiceOpt.get());

        } catch (Exception e) {
            return error("Failed to retrieve invoice: " + e.getMessage());
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
