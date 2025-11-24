// Cart page - loads cart from backend session

document.addEventListener('DOMContentLoaded', function() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    loadCartItems();

    // ========== LOAD CART ITEMS FROM BACKEND ==========
    function loadCartItems() {
        fetch('/customer/api/cart')
            .then(response => response.json())
            .then(data => {
                if (data.success && data.data) {
                    renderCartPage(data.data);
                } else {
                    showEmptyCart();
                }
            })
            .catch(error => {
                console.error('Error loading cart:', error);
                showNotification('Gagal memuat keranjang', 'error');
            });
    }

    // ========== RENDER CART PAGE ==========
    function renderCartPage(cartData) {
        const cartItemsList = document.getElementById('cartItemsList');
        const emptyCart = document.getElementById('emptyCart');

        if (!cartData.items || cartData.items.length === 0) {
            showEmptyCart();
            return;
        }

        // Hide empty cart message
        if (emptyCart) {
            emptyCart.style.display = 'none';
        }

        // Build cart items HTML
        let html = '';
        cartData.items.forEach(item => {
            html += `
                <div class="cart-item-card" data-menu-id="${item.menuId}">
                    ${item.imageUrl ? `
                        <img src="${item.imageUrl}" alt="${item.menuName}" class="cart-item-image">
                    ` : ''}
                    <div class="cart-item-details">
                        <div class="cart-item-name">${item.menuName}</div>
                        <div class="cart-item-price">Rp ${item.price.toLocaleString('id-ID')} / item</div>
                    </div>
                    <div class="cart-item-quantity">
                        <button class="qty-btn-cart btn-qty-decrease" data-menu-id="${item.menuId}">
                            <i class="fas fa-minus"></i>
                        </button>
                        <input type="number" class="qty-input-cart" value="${item.quantity}" min="1" max="99">
                        <button class="qty-btn-cart btn-qty-increase" data-menu-id="${item.menuId}">
                            <i class="fas fa-plus"></i>
                        </button>
                    </div>
                    <div class="cart-item-total">
                        Rp ${item.subtotal.toLocaleString('id-ID')}
                    </div>
                    <button class="btn-remove-item btn-remove" data-menu-id="${item.menuId}">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            `;
        });

        cartItemsList.innerHTML = html;

        // Update order summary
        updateOrderSummary(cartData);

        // Setup event listeners for quantity and remove buttons
        setupCartItemListeners();
    }

    // ========== SHOW EMPTY CART ==========
    function showEmptyCart() {
        const cartItemsList = document.getElementById('cartItemsList');
        const emptyCart = document.getElementById('emptyCart');

        if (cartItemsList) {
            cartItemsList.innerHTML = '';
        }
        if (emptyCart) {
            emptyCart.style.display = 'block';
        }

        // Reset order summary
        updateOrderSummary({ subtotal: 0, total: 0, totalItems: 0 });
    }

    // ========== UPDATE ORDER SUMMARY ==========
    function updateOrderSummary(cartData) {
        const subtotalElement = document.getElementById('orderSubtotal');
        const taxElement = document.getElementById('orderTax');
        const totalElement = document.getElementById('orderTotal');

        const subtotal = cartData.subtotal || 0;
        const tax = subtotal * 0.10; // 10% tax
        const total = subtotal + tax;

        if (subtotalElement) {
            subtotalElement.textContent = 'Rp ' + subtotal.toLocaleString('id-ID');
        }
        if (taxElement) {
            taxElement.textContent = 'Rp ' + tax.toLocaleString('id-ID');
        }
        if (totalElement) {
            totalElement.textContent = 'Rp ' + total.toLocaleString('id-ID');
        }
    }

    // ========== SETUP CART ITEM LISTENERS ==========
    function setupCartItemListeners() {
        // Increase quantity buttons
        document.querySelectorAll('.btn-qty-increase').forEach(button => {
            button.addEventListener('click', function() {
                const menuId = parseInt(this.getAttribute('data-menu-id'));
                const card = this.closest('.cart-item-card');
                const input = card.querySelector('.qty-input-cart');
                const newQuantity = parseInt(input.value) + 1;

                if (newQuantity <= 99) {
                    updateQuantity(menuId, newQuantity);
                }
            });
        });

        // Decrease quantity buttons
        document.querySelectorAll('.btn-qty-decrease').forEach(button => {
            button.addEventListener('click', function() {
                const menuId = parseInt(this.getAttribute('data-menu-id'));
                const card = this.closest('.cart-item-card');
                const input = card.querySelector('.qty-input-cart');
                const currentQuantity = parseInt(input.value);

                if (currentQuantity > 1) {
                    updateQuantity(menuId, currentQuantity - 1);
                } else {
                    removeItem(menuId);
                }
            });
        });

        // Quantity input change
        document.querySelectorAll('.qty-input-cart').forEach(input => {
            input.addEventListener('change', function() {
                const menuId = parseInt(this.closest('.cart-item-card').getAttribute('data-menu-id'));
                let newQuantity = parseInt(this.value);

                // Validate quantity
                if (isNaN(newQuantity) || newQuantity < 1) {
                    newQuantity = 1;
                } else if (newQuantity > 99) {
                    newQuantity = 99;
                }

                this.value = newQuantity;
                updateQuantity(menuId, newQuantity);
            });
        });

        // Remove buttons
        document.querySelectorAll('.btn-remove').forEach(button => {
            button.addEventListener('click', function() {
                const menuId = parseInt(this.getAttribute('data-menu-id'));
                removeItem(menuId);
            });
        });
    }

    // ========== UPDATE QUANTITY ==========
    function updateQuantity(menuId, quantity) {
        const headers = {
            'Content-Type': 'application/json'
        };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        fetch(`/customer/api/cart/update/${menuId}?quantity=${quantity}`, {
            method: 'PUT',
            headers: headers
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    loadCartItems(); // Reload cart
                    showNotification('Quantity berhasil diupdate', 'success');
                } else {
                    showNotification(data.message || 'Gagal update quantity', 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showNotification('Gagal update quantity', 'error');
            });
    }

    // ========== REMOVE ITEM ==========
    function removeItem(menuId) {
        if (!confirm('Hapus item dari keranjang?')) {
            return;
        }

        const headers = {
            'Content-Type': 'application/json'
        };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        fetch(`/customer/api/cart/remove/${menuId}`, {
            method: 'DELETE',
            headers: headers
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    loadCartItems(); // Reload cart
                    showNotification('Item berhasil dihapus', 'success');

                    // Update header cart count
                    if (window.updateCartCount) {
                        window.updateCartCount();
                    }
                } else {
                    showNotification(data.message || 'Gagal menghapus item', 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showNotification('Gagal menghapus item', 'error');
            });
    }

    // ========== CLEAR CART FUNCTION ==========
    window.clearCart = function() {
        if (!confirm('Apakah Anda yakin ingin mengosongkan keranjang?')) {
            return;
        }

        const headers = {
            'Content-Type': 'application/json'
        };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        fetch('/customer/api/cart/clear', {
            method: 'DELETE',
            headers: headers
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showNotification('Keranjang berhasil dikosongkan', 'success');
                    // Reload cart items
                    loadCartItems();
                } else {
                    showNotification('Gagal mengosongkan keranjang', 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showNotification('Gagal mengosongkan keranjang', 'error');
            });
    };

    // ========== CREATE ORDER FUNCTION ==========
    window.createOrder = function() {
        const customerNameInput = document.getElementById('customerName');
        if (!customerNameInput) return;

        const customerName = customerNameInput.value.trim();

        if (!customerName) {
            showNotification('Silakan masukkan nama pemesan', 'warning');
            customerNameInput.focus();
            return;
        }

        // Get current cart
        fetch('/customer/api/cart')
            .then(response => response.json())
            .then(cartResponse => {
                if (!cartResponse.success || !cartResponse.data || cartResponse.data.totalItems === 0) {
                    showNotification('Keranjang kosong. Silakan tambahkan item terlebih dahulu.', 'warning');
                    return;
                }

                // Build order data
                const orderData = {
                    orderType: 'CUSTOMER_SELF',
                    customerName: customerName,
                    items: cartResponse.data.items.map(item => ({
                        menuId: item.menuId,
                        quantity: item.quantity
                    }))
                };

                // Get CSRF headers
                const headers = {
                    'Content-Type': 'application/json'
                };
                if (csrfToken && csrfHeader) {
                    headers[csrfHeader] = csrfToken;
                }

                // Create order
                return fetch('/customer/api/orders', {
                    method: 'POST',
                    headers: headers,
                    body: JSON.stringify(orderData)
                });
            })
            .then(response => {
                if (!response) return; // Cart was empty
                return response.json();
            })
            .then(orderResponse => {
                if (!orderResponse) return; // Cart was empty

                if (orderResponse.success) {
                    showNotification('Pesanan berhasil dibuat!', 'success');
                    // Clear cart on backend
                    return fetch('/customer/api/cart/clear', {
                        method: 'DELETE',
                        headers: {
                            'Content-Type': 'application/json',
                            ...(csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {})
                        }
                    }).then(() => {
                        // Redirect to payment page
                        setTimeout(() => {
                            window.location.href = `/customer/payment?order=${orderResponse.data.orderNumber}`;
                        }, 500);
                    });
                } else {
                    showNotification(orderResponse.message || 'Gagal membuat pesanan', 'error');
                }
            })
            .catch(error => {
                console.error('Error creating order:', error);
                showNotification('Terjadi kesalahan saat membuat pesanan', 'error');
            });
    };

    // ========== NOTIFICATION HELPER ==========
    function showNotification(message, type) {
        // Remove existing notifications
        document.querySelectorAll('.notification-toast').forEach(n => n.remove());

        const notification = document.createElement('div');
        notification.className = `alert alert-${type === 'success' ? 'success' : type === 'warning' ? 'warning' : 'danger'} alert-dismissible fade show position-fixed notification-toast`;
        notification.style.cssText = 'top: 80px; right: 20px; z-index: 9999; min-width: 300px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);';
        notification.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'warning' ? 'exclamation-triangle' : 'exclamation-circle'} me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        document.body.appendChild(notification);

        setTimeout(() => {
            notification.classList.remove('show');
            setTimeout(() => notification.remove(), 150);
        }, 3000);
    }
});
