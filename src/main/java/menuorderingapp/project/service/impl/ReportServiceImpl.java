package menuorderingapp.project.service.impl;

import menuorderingapp.project.model.*;
import menuorderingapp.project.repository.InvoiceRepository;
import menuorderingapp.project.repository.OrderItemRepository;
import menuorderingapp.project.repository.OrderRepository;
import menuorderingapp.project.service.ReportService;
import menuorderingapp.project.util.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InvoiceRepository invoiceRepository;

    public ReportServiceImpl(OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             InvoiceRepository invoiceRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public Map<String, Object> getSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findPaidOrdersBetween(startDate, endDate);

        double totalRevenue = orders.stream()
                .mapToDouble(order -> order.getTotal().doubleValue() * (1 + Constants.TAX_RATE))
                .sum();

        long totalOrders = orders.size();
        double averageOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;

        Map<Order.PaymentMethod, Double> revenueByPaymentMethod = orders.stream()
                .collect(Collectors.groupingBy(
                        Order::getPaymentMethod,
                        Collectors.summingDouble(order -> order.getTotal().doubleValue() * (1 + Constants.TAX_RATE))
                ));

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalRevenue", totalRevenue);
        report.put("totalOrders", totalOrders);
        report.put("averageOrderValue", averageOrderValue);
        report.put("revenueByPaymentMethod", revenueByPaymentMethod);
        report.put("orders", orders);

        return report;
    }

    @Override
    public Map<String, Object> getDailySalesReport(LocalDate date) {
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.atTime(LocalTime.MAX);
        return getSalesReport(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getTopSellingItems(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = orderItemRepository.findTopSellingMenusBetween(startDate, endDate);

        return results.stream()
                .map(result -> {
                    Menu menu = (Menu) result[0];
                    Long quantity = (Long) result[1];
                    double revenue = ((java.math.BigDecimal) result[2]).doubleValue();

                    Map<String, Object> item = new HashMap<>();
                    item.put("menu", menu);
                    item.put("quantity", quantity);
                    item.put("revenue", revenue);
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getCashierPerformanceReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findPaidOrdersBetween(startDate, endDate);

        Map<Cashier, List<Order>> ordersByCashier = orders.stream()
                .filter(order -> order.getCashier() != null)
                .collect(Collectors.groupingBy(Order::getCashier));

        Map<Cashier, Double> revenueByCashier = ordersByCashier.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .mapToDouble(order -> order.getTotal().doubleValue() * (1 + Constants.TAX_RATE))
                                .sum()
                ));

        Map<Cashier, Long> orderCountByCashier = ordersByCashier.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (long) entry.getValue().size()
                ));

        Map<String, Object> report = new HashMap<>();
        report.put("revenueByCashier", revenueByCashier);
        report.put("orderCountByCashier", orderCountByCashier);
        report.put("startDate", startDate);
        report.put("endDate", endDate);

        return report;
    }

    @Override
    public List<Order> getOrdersForReport(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findOrdersByDateRange(startDate, endDate);
    }
}
