package menuorderingapp.project.controller;

import menuorderingapp.project.model.*;
import menuorderingapp.project.model.dto.*;
import menuorderingapp.project.security.CashierUserDetails;
import menuorderingapp.project.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import menuorderingapp.project.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cashier")
public class CashierController extends BaseController{

    private final OrderService orderService;
    private final MenuService menuService;
    private final PaymentService paymentService;
    private final ReportService reportService;
    private final InvoiceService invoiceService;
    private final AuthService authService;

    public CashierController(OrderService orderService, MenuService menuService,
                             PaymentService paymentService, ReportService reportService,
                             InvoiceService invoiceService, AuthService authService) {
        this.orderService = orderService;
        this.menuService = menuService;
        this.paymentService = paymentService;
        this.reportService = reportService;
        this.invoiceService = invoiceService;
        this.authService = authService;
    }

    // Dashboard - Kasir Interface
    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpSession session) {
        CashierUserDetails currentCashier = SecurityUtils.getCurrentCashier();
        if (currentCashier == null) {
            return "redirect:/auth/login";
        }

        // Dashboard statistics
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

    // Dashboard Stats API
    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(HttpSession session) {
        CashierUserDetails currentCashier = SecurityUtils.getCurrentCashier();
        if (currentCashier == null) {
            return unauthorized("Not authenticated");
        }

        try {
            // Get dashboard statistics
            long pendingOrders = orderService.getPendingOrdersCount();
            double todayRevenue = orderService.getTotalRevenueToday();
            List<Order> recentOrders = orderService.getTodayOrders();
            long availableMenus = menuService.getAvailableMenus().size();
            long todayOrdersCount = (long) recentOrders.size();

            // Convert orders to OrderResponse DTOs
            List<OrderResponse> orderResponses = recentOrders.stream()
                    .map(this::convertToOrderResponse)
                    .collect(Collectors.toList());

            // Build response
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
        CashierUserDetails currentCashier = SecurityUtils.getCurrentCashier();
        if (currentCashier == null) {
            return "redirect:/auth/login";
        }

        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);
        model.addAttribute("cashier", session.getAttribute("cashier"));
        model.addAttribute("currentPath", "/cashier/orders");

        return "cashier/orders";
    }

    // Create New Order (Cashier Assisted)
    @PostMapping("/api/orders")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> createCashierOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            HttpSession session) {

        CashierUserDetails currentCashier = SecurityUtils.getCurrentCashier();
        if (currentCashier == null) {
            return unauthorized("Not authenticated");
        }

        try {
            Long cashierId = (Long) session.getAttribute("cashierId");

            Order order = new Order();
            order.setOrderType(Order.OrderType.CASHIER_ASSISTED);
            order.setCustomerName(orderRequest.getCustomerName());
            order.setStatus(Order.OrderStatus.PENDING);
            order.setPaymentStatus(Order.PaymentStatus.PENDING);

            // Set cashier if available
            if (cashierId != null) {
                // In a real app, you'd fetch the cashier entity
                // For now, we'll just store the ID
            }

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

        CashierUserDetails currentCashier = SecurityUtils.getCurrentCashier();
        if (currentCashier == null) {
            return unauthorized("Not authenticated");
        }

        try {
            Order updatedOrder = orderService.updateOrderStatus(orderId, status);
            OrderResponse orderResponse = convertToOrderResponse(updatedOrder);
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

        CashierUserDetails currentCashier = SecurityUtils.getCurrentCashier();
        if (currentCashier == null) {
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
                Long cashierId = (Long) session.getAttribute("cashierId");
                Optional<Order> orderOpt = orderService.getOrderByNumber(paymentRequest.getOrderNumber());
                if (orderOpt.isPresent() && cashierId != null) {
                    invoiceService.generateInvoice(orderOpt.get(), cashierId);
                }

                return success(paymentResponse);
            } else {
                return error("Payment processing failed");
            }

        } catch (Exception e) {
            return error("Payment error: " + e.getMessage());
        }
    }

    // Reports Page - Laporan
    @GetMapping("/reports")
    public String showReportsPage(Model model, HttpSession session) {
        CashierUserDetails currentCashier = SecurityUtils.getCurrentCashier();
        if (currentCashier == null) {
            return "redirect:/auth/login";
        }

        // Default to today's report
        LocalDate today = LocalDate.now();
        var salesReport = reportService.getDailySalesReport(today);

        model.addAttribute("salesReport", salesReport);
        model.addAttribute("cashier", session.getAttribute("cashier"));
        model.addAttribute("currentPath", "/cashier/reports");

        return "cashier/reports";
    }

    // Generate Sales Report
    @GetMapping("/api/reports/sales")
    @ResponseBody
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesReport(
            @RequestParam String startDate,
            @RequestParam String endDate,
            HttpSession session) {

        CashierUserDetails currentCashier = SecurityUtils.getCurrentCashier();
        if (currentCashier == null) {
            return unauthorized("Not authenticated");
        }

        try {
            var report = reportService.getSalesReport(
                    LocalDate.parse(startDate).atStartOfDay(),
                    LocalDate.parse(endDate).atTime(23, 59, 59)
            );

            SalesReportResponse response = new SalesReportResponse();
            // Convert report data to response DTO
            // This would involve mapping the report map to the response object

            return success(response);

        } catch (Exception e) {
            return error("Failed to generate report: " + e.getMessage());
        }
    }

    // Settings Page - Pengaturan
    @GetMapping("/settings")
    public String showSettingsPage(Model model, HttpSession session) {
        CashierUserDetails currentCashier = SecurityUtils.getCurrentCashier();
        if (currentCashier == null) {
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

        CashierUserDetails currentCashier = SecurityUtils.getCurrentCashier();
        if (currentCashier == null) {
            return unauthorized("Not authenticated");
        }

        try {
            Menu updatedMenu = menuService.toggleMenuAvailability(menuId);
            MenuResponse response = convertToMenuResponse(updatedMenu);
            return success("Menu availability updated", response);

        } catch (Exception e) {
            return error("Failed to update menu: " + e.getMessage());
        }
    }

    // Update Menu
    @PutMapping("/api/menus/{menuId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<MenuResponse>> updateMenu(
            @PathVariable Long menuId,
            @Valid @RequestBody MenuRequest menuRequest,
            HttpSession session) {

        CashierUserDetails currentCashier = SecurityUtils.getCurrentCashier();
        if (currentCashier == null) {
            return unauthorized("Not authenticated");
        }

        try {
            // Convert request to entity and update
            Menu menuDetails = new Menu();
            menuDetails.setName(menuRequest.getName());
            menuDetails.setDescription(menuRequest.getDescription());
            menuDetails.setPrice(menuRequest.getPrice());
            menuDetails.setAvailable(menuRequest.getAvailable());
            menuDetails.setIsPromo(menuRequest.getIsPromo());
            menuDetails.setPromoPrice(menuRequest.getPromoPrice());

            // Set category if provided
            if (menuRequest.getCategoryId() != null) {
                Optional<Category> category = menuService.getCategoryById(menuRequest.getCategoryId());
                category.ifPresent(menuDetails::setCategory);
            }

            Menu updatedMenu = menuService.updateMenu(menuId, menuDetails);
            MenuResponse response = convertToMenuResponse(updatedMenu);
            return success("Menu updated successfully", response);

        } catch (Exception e) {
            return error("Failed to update menu: " + e.getMessage());
        }
    }

    // Helper methods
//    private boolean isAuthenticated(HttpSession session) {
//        return SecurityUtils.isAuthenticated();
//    }

    // Conversion methods (similar to CustomerController)
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
}
