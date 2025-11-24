package menuorderingapp.project.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import menuorderingapp.project.model.Menu;
import menuorderingapp.project.model.dto.ApiResponse;
import menuorderingapp.project.model.dto.CartItemRequest;
import menuorderingapp.project.model.dto.CartItemResponse;
import menuorderingapp.project.model.dto.CartResponse;
import menuorderingapp.project.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customer")
public class CartController extends BaseController {

    private final MenuService menuService;
    private static final String CART_SESSION_KEY = "shopping_cart";

    public CartController(MenuService menuService) {
        this.menuService = menuService;
    }

    // ========== API ENDPOINTS ==========

    // Add item to cart
    @PostMapping("/api/cart/add")
    @ResponseBody
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody CartItemRequest request,
            HttpSession session) {
        try {
            // Get or create cart from session
            ShoppingCart cart = getCartFromSession(session);

            // Get menu details
            Optional<Menu> menuOpt = menuService.getMenuById(request.getMenuId());
            if (menuOpt.isEmpty()) {
                return error("Menu tidak ditemukan");
            }

            Menu menu = menuOpt.get();
            if (!menu.getAvailable()) {
                return error("Menu tidak tersedia");
            }

            // Add or update cart item
            CartItem existingItem = cart.getItems().get(request.getMenuId());
            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            } else {
                CartItem newItem = new CartItem(
                        request.getMenuId(),
                        menu.getName(),
                        menu.getCurrentPrice(),
                        request.getQuantity(),
                        menu.getImageUrl()
                );
                cart.getItems().put(request.getMenuId(), newItem);
            }

            session.setAttribute(CART_SESSION_KEY, cart);

            CartResponse response = buildCartResponse(cart);
            return success("Item berhasil ditambahkan ke keranjang", response);

        } catch (Exception e) {
            return error("Gagal menambahkan item: " + e.getMessage());
        }
    }

    // Get cart contents
    @GetMapping("/api/cart")
    @ResponseBody
    public ResponseEntity<ApiResponse<CartResponse>> getCart(HttpSession session) {
        ShoppingCart cart = getCartFromSession(session);
        CartResponse response = buildCartResponse(cart);
        return success(response);
    }

    // Get cart item count
    @GetMapping("/api/cart/count")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getCartCount(HttpSession session) {
        ShoppingCart cart = getCartFromSession(session);
        int count = cart.getItems().values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        Map<String, Integer> response = new HashMap<>();
        response.put("count", count);
        return success(response);
    }

    // Update cart item quantity
    @PutMapping("/api/cart/update/{menuId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long menuId,
            @RequestParam Integer quantity,
            HttpSession session) {
        try {
            if (quantity < 1) {
                return error("Quantity harus minimal 1");
            }

            ShoppingCart cart = getCartFromSession(session);
            CartItem item = cart.getItems().get(menuId);

            if (item == null) {
                return error("Item tidak ditemukan di keranjang");
            }

            item.setQuantity(quantity);
            session.setAttribute(CART_SESSION_KEY, cart);

            CartResponse response = buildCartResponse(cart);
            return success("Quantity berhasil diupdate", response);

        } catch (Exception e) {
            return error("Gagal update quantity: " + e.getMessage());
        }
    }

    // Remove item from cart
    @DeleteMapping("/api/cart/remove/{menuId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable Long menuId,
            HttpSession session) {
        try {
            ShoppingCart cart = getCartFromSession(session);
            CartItem removedItem = cart.getItems().remove(menuId);

            if (removedItem == null) {
                return error("Item tidak ditemukan di keranjang");
            }

            session.setAttribute(CART_SESSION_KEY, cart);

            CartResponse response = buildCartResponse(cart);
            return success("Item berhasil dihapus dari keranjang", response);

        } catch (Exception e) {
            return error("Gagal menghapus item: " + e.getMessage());
        }
    }

    // Clear entire cart
    @DeleteMapping("/api/cart/clear")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, String>>> clearCart(HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Keranjang berhasil dikosongkan");
        return success(response);
    }

    // Show cart page
    @GetMapping("/cart")
    public String showCartPage(Model model, HttpSession session) {
        ShoppingCart cart = getCartFromSession(session);
        model.addAttribute("cart", buildCartResponse(cart));
        return "customer/cart";
    }

    // ========== HELPER METHODS ==========

    private ShoppingCart getCartFromSession(HttpSession session) {
        ShoppingCart cart = (ShoppingCart) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ShoppingCart();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    private CartResponse buildCartResponse(ShoppingCart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().values().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        BigDecimal subtotal = cart.getItems().values().stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = cart.getItems().values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        CartResponse response = new CartResponse();
        response.setItems(itemResponses);
        response.setSubtotal(subtotal);
        response.setTotal(subtotal);
        response.setTotalItems(totalItems);

        return response;
    }

    private CartItemResponse convertToResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setMenuId(item.getMenuId());
        response.setMenuName(item.getMenuName());
        response.setPrice(item.getPrice());
        response.setQuantity(item.getQuantity());
        response.setSubtotal(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        response.setImageUrl(item.getImageUrl());
        return response;
    }

    // ========== INNER CLASSES (Keep in same file) ==========

    /**
     * Shopping cart stored in HTTP session
     */
    public static class ShoppingCart implements Serializable {
        private static final long serialVersionUID = 1L;
        private Map<Long, CartItem> items = new HashMap<>();

        public Map<Long, CartItem> getItems() {
            return items;
        }

        public void setItems(Map<Long, CartItem> items) {
            this.items = items;
        }
    }

    /**
     * Individual cart item
     */
    public static class CartItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long menuId;
        private String menuName;
        private BigDecimal price;
        private Integer quantity;
        private String imageUrl;

        public CartItem() {}

        public CartItem(Long menuId, String menuName, BigDecimal price, Integer quantity, String imageUrl) {
            this.menuId = menuId;
            this.menuName = menuName;
            this.price = price;
            this.quantity = quantity;
            this.imageUrl = imageUrl;
        }

        // Getters and setters
        public Long getMenuId() { return menuId; }
        public void setMenuId(Long menuId) { this.menuId = menuId; }
        public String getMenuName() { return menuName; }
        public void setMenuName(String menuName) { this.menuName = menuName; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }
}