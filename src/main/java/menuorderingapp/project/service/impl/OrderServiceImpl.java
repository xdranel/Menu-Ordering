package menuorderingapp.project.service.impl;

import menuorderingapp.project.model.*;
import menuorderingapp.project.repository.CashierRepository;
import menuorderingapp.project.repository.MenuRepository;
import menuorderingapp.project.repository.OrderItemRepository;
import menuorderingapp.project.repository.OrderRepository;
import menuorderingapp.project.service.OrderService;
import menuorderingapp.project.util.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuRepository menuRepository;
    private final CashierRepository cashierRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            MenuRepository menuRepository,
                            CashierRepository cashierRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuRepository = menuRepository;
        this.cashierRepository = cashierRepository;
    }

    @Override
    public Order createOrder(Order order) {
        order.calculateTotal();
        return orderRepository.save(order);
    }

    @Override
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByCashier(Long cashierId) {
        Cashier cashier = cashierRepository.findById(cashierId)
                .orElseThrow(() -> new RuntimeException("Cashier not found with id: " + cashierId));
        return orderRepository.findByCashierOrderByCreatedAtDesc(cashier);
    }

    @Override
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Override
    public Order addItemToOrder(Long orderId, Long menuId, Integer quantity) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new RuntimeException("Menu not found with id: " + menuId));

        if (!menu.getAvailable()) {
            throw new RuntimeException("Menu item is not available: " + menu.getName());
        }

        Optional<OrderItem> existingItem = order.getOrderItems().stream()
                .filter(item -> item.getMenu().getId().equals(menuId))
                .findFirst();

        if (existingItem.isPresent()) {
            OrderItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            orderItemRepository.save(item);
        } else {
            OrderItem newItem = new OrderItem(order, menu, quantity);
            order.addOrderItem(newItem);
            orderItemRepository.save(newItem);
        }

        order.calculateTotal();
        return orderRepository.save(order);
    }

    @Override
    public Order removeItemFromOrder(Long orderId, Long orderItemId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("Order item not found with id: " + orderItemId));

        if (!orderItem.getOrder().getId().equals(orderId)) {
            throw new RuntimeException("Order item does not belong to this order");
        }

        order.removeOrderItem(orderItem);
        orderItemRepository.delete(orderItem);
        order.calculateTotal();

        return orderRepository.save(order);
    }

    @Override
    public Order updateItemQuantity(Long orderId, Long orderItemId, Integer quantity) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("Order item not found with id: " + orderItemId));

        if (!orderItem.getOrder().getId().equals(orderId)) {
            throw new RuntimeException("Order item does not belong to this order");
        }

        if (quantity <= 0) {
            return removeItemFromOrder(orderId, orderItemId);
        }

        orderItem.setQuantity(quantity);
        orderItemRepository.save(orderItem);
        order.calculateTotal();

        return orderRepository.save(order);
    }

    @Override
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed order");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getTodayOrders() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return orderRepository.findOrdersByDateRange(startOfDay, endOfDay);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByDateRange(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findOrdersByDateRange(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalRevenueToday() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        Double revenue = orderRepository.getTotalRevenueBetween(startOfDay, endOfDay);
        return revenue != null ? revenue * (1 + Constants.TAX_RATE) : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public long getPendingOrdersCount() {
        return orderRepository.countByStatus(Order.OrderStatus.PENDING);
    }
}
