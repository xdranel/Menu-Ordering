package menuorderingapp.project.model.dto;

import java.util.List;

public class DashboardStatsResponse {

    private Double todayRevenue;
    private Long todayOrders;
    private Long pendingOrders;
    private Long availableMenus;
    private List<OrderResponse> recentOrders;

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(Double todayRevenue, Long todayOrders, Long pendingOrders,
                                  Long availableMenus, List<OrderResponse> recentOrders) {
        this.todayRevenue = todayRevenue;
        this.todayOrders = todayOrders;
        this.pendingOrders = pendingOrders;
        this.availableMenus = availableMenus;
        this.recentOrders = recentOrders;
    }

    // Getters and Setters
    public Double getTodayRevenue() {
        return todayRevenue;
    }

    public void setTodayRevenue(Double todayRevenue) {
        this.todayRevenue = todayRevenue;
    }

    public Long getTodayOrders() {
        return todayOrders;
    }

    public void setTodayOrders(Long todayOrders) {
        this.todayOrders = todayOrders;
    }

    public Long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(Long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public Long getAvailableMenus() {
        return availableMenus;
    }

    public void setAvailableMenus(Long availableMenus) {
        this.availableMenus = availableMenus;
    }

    public List<OrderResponse> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<OrderResponse> recentOrders) {
        this.recentOrders = recentOrders;
    }
}
