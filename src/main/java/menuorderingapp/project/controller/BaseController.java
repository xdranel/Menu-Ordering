package menuorderingapp.project.controller;

import menuorderingapp.project.model.Invoice;
import menuorderingapp.project.model.Menu;
import menuorderingapp.project.model.Order;
import menuorderingapp.project.model.OrderItem;
import menuorderingapp.project.model.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Collectors;

public class BaseController {

    protected <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    protected <T> ResponseEntity<ApiResponse<T>> success(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Created successfully", data));
    }

    protected <T> ResponseEntity<ApiResponse<T>> error(String message) {
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    protected <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }

    protected <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    protected <T> ResponseEntity<ApiResponse<T>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }

    protected MenuResponse convertToMenuResponse(Menu menu) {
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

    protected OrderResponse convertToOrderResponse(Order order) {
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

    protected OrderItemResponse convertToOrderItemResponse(OrderItem orderItem) {
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

    protected InvoiceResponse convertToInvoiceResponse(Invoice invoice) {
        InvoiceResponse response = new InvoiceResponse();
        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setTaxAmount(invoice.getTaxAmount());
        response.setFinalAmount(invoice.getFinalAmount());
        response.setPaymentMethod(invoice.getPaymentMethod());
        response.setCreatedAt(invoice.getCreatedAt());

        if (invoice.getOrder() != null) {
            response.setOrder(convertToOrderResponse(invoice.getOrder()));
        }

        if (invoice.getCashier() != null) {
            CashierDto cashierDto = new CashierDto();
            cashierDto.setId(invoice.getCashier().getId());
            cashierDto.setUsername(invoice.getCashier().getUsername());
            cashierDto.setDisplayName(invoice.getCashier().getDisplayName());
            response.setCashier(cashierDto);
        }

        return response;
    }
}
