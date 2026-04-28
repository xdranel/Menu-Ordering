package menuorderingapp.project.service;

import menuorderingapp.project.model.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order createOrder(Order order);

    Order saveOrder(Order order);

    Optional<Order> getOrderById(Long id);

    Optional<Order> getOrderByNumber(String orderNumber);

    List<Order> getAllOrders();

    List<Order> getOrdersByStatus(Order.OrderStatus status);

    List<Order> getOrdersByCashier(Long cashierId);

    Order updateOrderStatus(Long orderId, Order.OrderStatus status);

    Order addItemToOrder(Long orderId, Long menuId, Integer quantity);

    Order removeItemFromOrder(Long orderId, Long orderItemId);

    Order updateItemQuantity(Long orderId, Long orderItemId, Integer quantity);

    void cancelOrder(Long orderId);

    List<Order> getTodayOrders();

    List<Order> getOrdersByDateRange(LocalDateTime start, LocalDateTime end);

    Double getTotalRevenueToday();

    long getPendingOrdersCount();
}
