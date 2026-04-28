package menuorderingapp.project.repository;

import menuorderingapp.project.model.Menu;
import menuorderingapp.project.model.Order;
import menuorderingapp.project.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);

    List<OrderItem> findByMenu(Menu menu);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.menu WHERE oi.order = :order")
    List<OrderItem> findByOrderWithMenu(@Param("order") Order order);

    @Query("SELECT oi.menu, SUM(oi.quantity) as totalQuantity, SUM(oi.price * oi.quantity) as totalRevenue " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.paymentStatus = 'PAID' AND o.createdAt BETWEEN :start AND :end " +
            "GROUP BY oi.menu " +
            "ORDER BY totalQuantity DESC")
    List<Object[]> findTopSellingMenusBetween(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.menu = :menu")
    Long getTotalQuantitySoldByMenu(@Param("menu") Menu menu);

    void deleteByOrder(Order order);
}
