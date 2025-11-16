package menuorderingapp.project.model.dto;

import java.math.BigDecimal;
import java.util.List;

public class CartResponse {

    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal total;
    private int totalItems;

    // Constructors
    public CartResponse() {
    }

    // Getters and Setters
    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }
}
