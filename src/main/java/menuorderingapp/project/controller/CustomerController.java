package menuorderingapp.project.controller;

import menuorderingapp.project.model.*;
import menuorderingapp.project.model.dto.*;
import menuorderingapp.project.service.InvoiceService;
import menuorderingapp.project.service.MenuService;
import menuorderingapp.project.service.OrderService;
import menuorderingapp.project.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customer")
public class CustomerController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final MenuService menuService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;

    @Value("${app.simulate-payment.enabled:true}")
    private boolean simulatePaymentEnabled;

    public CustomerController(MenuService menuService, OrderService orderService, PaymentService paymentService, InvoiceService invoiceService) {
        this.menuService = menuService;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
    }

    @GetMapping("/menu")
    public String showMenuPage(Model model,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) String search) {

        List<Category> categories = menuService.getAllCategories();
        List<Menu> menus;

        if (search != null && !search.trim().isEmpty()) {
            menus = menuService.searchMenus(search);
            model.addAttribute("searchTerm", search);
        } else if (category != null && !category.equals("SEMUA")) {
            Optional<Category> selectedCategory = categories.stream()
                    .filter(c -> c.getName().equals(category))
                    .findFirst();
            if (selectedCategory.isPresent()) {
                menus = menuService.getMenusByCategory(selectedCategory.get().getId());
            } else {
                menus = menuService.getAvailableMenus();
            }
            model.addAttribute("selectedCategory", category);
        } else {
            menus = menuService.getAvailableMenus();
        }

        model.addAttribute("categories", categories);
        model.addAttribute("menus", menus);
        model.addAttribute("cartSummary", new CartSummary());

        return "customer/menu";
    }

    @GetMapping("/payment")
    public String showPaymentPage(Model model, @RequestParam(required = false) String order) {
        if (order != null) {
            model.addAttribute("orderNumber", order);
        }
        return "customer/payment";
    }

    @GetMapping("/api/menus")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getAvailableMenus(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search) {

        List<Menu> menus;
        if (search != null && !search.trim().isEmpty()) {
            menus = menuService.searchMenus(search);
        } else if (categoryId != null) {
            menus = menuService.getMenusByCategory(categoryId);
        } else {
            menus = menuService.getAvailableMenus();
        }

        List<MenuResponse> menuResponses = menus.stream()
                .map(this::convertToMenuResponse)
                .collect(Collectors.toList());

        return success(menuResponses);
    }

    @PostMapping("/api/orders")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        try {
            Order order = new Order();
            order.setOrderType(Order.OrderType.CUSTOMER_SELF);
            order.setCustomerName(orderRequest.getCustomerName() != null ?
                    orderRequest.getCustomerName() : "Customer");
            order.setStatus(Order.OrderStatus.PENDING);
            order.setPaymentStatus(Order.PaymentStatus.PENDING);

            for (OrderItemRequest itemRequest : orderRequest.getItems()) {
                Optional<Menu> menu = menuService.getMenuById(itemRequest.getMenuId());
                if (menu.isPresent() && menu.get().getAvailable()) {
                    OrderItem orderItem = new OrderItem(menu.get(), itemRequest.getQuantity());
                    order.addOrderItem(orderItem);
                }
            }

            Order savedOrder = orderService.createOrder(order);
            return created(convertToOrderResponse(savedOrder));

        } catch (Exception e) {
            log.error("Failed to create order: {}", e.getMessage(), e);
            return error("Failed to create order");
        }
    }

    @PostMapping("/api/orders/{orderId}/items")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> addItemToOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemRequest itemRequest) {

        try {
            Order updatedOrder = orderService.addItemToOrder(orderId, itemRequest.getMenuId(), itemRequest.getQuantity());
            return success("Item added to order", convertToOrderResponse(updatedOrder));
        } catch (Exception e) {
            log.error("Failed to add item to order {}: {}", orderId, e.getMessage(), e);
            return error("Failed to add item");
        }
    }

    @DeleteMapping("/api/orders/{orderId}/items/{itemId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> removeItemFromOrder(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {

        try {
            Order updatedOrder = orderService.removeItemFromOrder(orderId, itemId);
            return success("Item removed from order", convertToOrderResponse(updatedOrder));
        } catch (Exception e) {
            log.error("Failed to remove item {} from order {}: {}", itemId, orderId, e.getMessage(), e);
            return error("Failed to remove item");
        }
    }

    @PutMapping("/api/orders/{orderId}/items/{itemId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> updateItemQuantity(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam Integer quantity) {

        try {
            Order updatedOrder = orderService.updateItemQuantity(orderId, itemId, quantity);
            return success("Quantity updated", convertToOrderResponse(updatedOrder));
        } catch (Exception e) {
            log.error("Failed to update quantity for item {} in order {}: {}", itemId, orderId, e.getMessage(), e);
            return error("Failed to update quantity");
        }
    }

    @GetMapping("/api/orders/{orderNumber}")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String orderNumber) {
        try {
            Optional<Order> orderOpt = orderService.getOrderByNumber(orderNumber);
            if (orderOpt.isPresent()) {
                return success(convertToOrderResponse(orderOpt.get()));
            } else {
                return notFound("Order not found");
            }
        } catch (Exception e) {
            log.error("Failed to get order {}: {}", orderNumber, e.getMessage(), e);
            return error("Failed to get order");
        }
    }

    @GetMapping("/api/orders/{orderNumber}/qr-code")
    @ResponseBody
    public ResponseEntity<ApiResponse<PaymentResponse>> generateQRCode(@PathVariable String orderNumber) {
        try {
            Optional<Order> orderOpt = orderService.getOrderByNumber(orderNumber);
            if (orderOpt.isEmpty()) {
                return notFound("Order not found");
            }

            String qrCode = paymentService.generatePaymentQRCode(orderOpt.get());

            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setSuccess(true);
            paymentResponse.setOrderNumber(orderNumber);
            paymentResponse.setQrCodeImage(qrCode);
            paymentResponse.setMessage("QR code generated successfully");

            return success(paymentResponse);
        } catch (Exception e) {
            log.error("Failed to generate QR code for order {}: {}", orderNumber, e.getMessage(), e);
            return error("Failed to generate QR code");
        }
    }

    // FOR TESTING ONLY — disable in production via app.simulate-payment.enabled=false
    @PostMapping("/api/orders/{orderNumber}/simulate-payment")
    @ResponseBody
    public ResponseEntity<ApiResponse<PaymentResponse>> simulatePayment(@PathVariable String orderNumber) {
        if (!simulatePaymentEnabled) {
            return error("Payment simulation is disabled");
        }

        try {
            Optional<Order> orderOpt = orderService.getOrderByNumber(orderNumber);
            if (orderOpt.isEmpty()) {
                return notFound("Order not found");
            }

            Order order = orderOpt.get();

            if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                return error("Order has already been paid");
            }

            if (order.getStatus() == Order.OrderStatus.CANCELLED) {
                return error("Cannot pay a cancelled order");
            }

            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setPaymentMethod(Order.PaymentMethod.QR_CODE);
            order.setStatus(Order.OrderStatus.CONFIRMED);

            Order updatedOrder = orderService.saveOrder(order);

            Long cashierId = updatedOrder.getCashier() != null ? updatedOrder.getCashier().getId() : null;
            invoiceService.generateInvoice(updatedOrder, cashierId);

            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setSuccess(true);
            paymentResponse.setOrderNumber(orderNumber);
            paymentResponse.setMessage("Payment simulated successfully (TEST MODE)");

            return success(paymentResponse);
        } catch (Exception e) {
            log.error("Payment simulation failed for order {}: {}", orderNumber, e.getMessage(), e);
            return error("Payment simulation failed");
        }
    }

    @PostMapping("/api/payments")
    @ResponseBody
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@Valid @RequestBody PaymentRequest paymentRequest) {
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

            if (paymentSuccess) {
                Optional<Order> orderOpt = orderService.getOrderByNumber(paymentRequest.getOrderNumber());
                if (orderOpt.isPresent()) {
                    invoiceService.generateInvoice(orderOpt.get(), null);
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

}
