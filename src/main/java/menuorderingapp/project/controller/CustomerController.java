package menuorderingapp.project.controller;

import menuorderingapp.project.model.*;
import menuorderingapp.project.model.dto.*;
import menuorderingapp.project.service.InvoiceService;
import menuorderingapp.project.service.MenuService;
import menuorderingapp.project.service.OrderService;
import menuorderingapp.project.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customer")
public class CustomerController extends BaseController {

    private final MenuService menuService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;

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
            OrderResponse orderResponse = convertToOrderResponse(savedOrder);

            return created(orderResponse);

        } catch (Exception e) {
            return error("Failed to create order: " + e.getMessage());
        }
    }


    @PostMapping("/api/orders/{orderId}/items")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> addItemToOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemRequest itemRequest) {

        try {
            Order updatedOrder = orderService.addItemToOrder(orderId, itemRequest.getMenuId(), itemRequest.getQuantity());
            OrderResponse orderResponse = convertToOrderResponse(updatedOrder);
            return success("Item added to order", orderResponse);

        } catch (Exception e) {
            return error("Failed to add item: " + e.getMessage());
        }
    }


    @DeleteMapping("/api/orders/{orderId}/items/{itemId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> removeItemFromOrder(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {

        try {
            Order updatedOrder = orderService.removeItemFromOrder(orderId, itemId);
            OrderResponse orderResponse = convertToOrderResponse(updatedOrder);
            return success("Item removed from order", orderResponse);

        } catch (Exception e) {
            return error("Failed to remove item: " + e.getMessage());
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
            OrderResponse orderResponse = convertToOrderResponse(updatedOrder);
            return success("Quantity updated", orderResponse);

        } catch (Exception e) {
            return error("Failed to update quantity: " + e.getMessage());
        }
    }


    @GetMapping("/api/orders/{orderNumber}")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String orderNumber) {
        try {
            Optional<Order> orderOpt = orderService.getOrderByNumber(orderNumber);
            if (orderOpt.isPresent()) {
                OrderResponse orderResponse = convertToOrderResponse(orderOpt.get());
                return success(orderResponse);
            } else {
                return notFound("Order not found");
            }
        } catch (Exception e) {
            return error("Failed to get order: " + e.getMessage());
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

            Order order = orderOpt.get();
            String qrCode = paymentService.generatePaymentQRCode(order);

            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setSuccess(true);
            paymentResponse.setOrderNumber(orderNumber);
            paymentResponse.setQrCodeImage(qrCode);
            paymentResponse.setMessage("QR code generated successfully");

            return success(paymentResponse);

        } catch (Exception e) {
            return error("Failed to generate QR code: " + e.getMessage());
        }
    }


    @PostMapping("/api/orders/{orderNumber}/simulate-payment")
    @ResponseBody
    public ResponseEntity<ApiResponse<PaymentResponse>> simulatePayment(@PathVariable String orderNumber) {
        try {
            Optional<Order> orderOpt = orderService.getOrderByNumber(orderNumber);
            if (orderOpt.isEmpty()) {
                return notFound("Order not found");
            }

            Order order = orderOpt.get();

            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setPaymentMethod(Order.PaymentMethod.QR_CODE);
            order.setStatus(Order.OrderStatus.CONFIRMED);

            Order updatedOrder = orderService.createOrder(order);

            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setSuccess(true);
            paymentResponse.setOrderNumber(orderNumber);
            paymentResponse.setMessage("Payment simulated successfully (TEST MODE)");

            return success(paymentResponse);

        } catch (Exception e) {
            return error("Failed to simulate payment: " + e.getMessage());
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
                // Generate invoice for customer order (cashier is null for customer_self orders)
                Optional<Order> orderOpt = orderService.getOrderByNumber(paymentRequest.getOrderNumber());
                if (orderOpt.isPresent()) {
                    invoiceService.generateInvoice(orderOpt.get(), null);
                }

                return success(paymentResponse);
            } else {
                return error("Payment processing failed");
            }

        } catch (Exception e) {
            return error("Payment error: " + e.getMessage());
        }
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
