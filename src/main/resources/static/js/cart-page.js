// Cart Page Script - Loads cart from backend session
// Location: src/main/resources/static/js/cart-page.js

document.addEventListener('DOMContentLoaded', function() {
    // Get CSRF token for security
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    // Load cart on page load
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
                <div class="card cart-item mb-3" data-menu-id="${item.menuId}">
                    <div class="card-body">
                        <div class="row align-items-center">
                            ${item.imageUrl ? `
                            <div class="col-md-2">
                                <img src="${item.imageUrl}" alt="${item.menuName}"
                                     class="img-fluid rounded" style="max-height: 80px;">
                            </div>
                            ` : ''}
                            <div class="col-md-${item.imageUrl ? '4' : '6'}">
                                <h5 class="card-title mb-1">${item.menuName}</h5>
                                <p class="text-muted mb-0">Rp ${item.price.toLocaleString('id-ID')} / item</p>
                            </div>
                            <div class="col-md-3">
                                <div class="input-group input-group-sm">
                                    <button class="btn btn-outline-secondary btn-qty-decrease"
                                            data-menu-id="${item.menuId}">
                                        <i class="fas fa-minus"></i>
                                    </button>
                                    <input type="number" class="form-control text-center"
                                           value="${item.quantity}" min="1" max="99" readonly>
                                    <button class="btn btn-outline-secondary btn-qty-increase"
                                            data-menu-id="${item.menuId}">
                                        <i class="fas fa-plus"></i>
                                    </button>
                                </div>
                            </div>
                            <div class="col-md-2 text-end">
                                <strong class="h5 mb-0">Rp ${item.subtotal.toLocaleString('id-ID')}</strong>
                            </div>
                            <div class="col-md-1 text-end">
                                <button class="btn btn-outline-danger btn-sm btn-remove"
                                        data-menu-id="${item.menuId}">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </div>
                        </div>
                    </div>
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
                const card = this.closest('.cart-item');
                const input = card.querySelector('input[type="number"]');
                const newQuantity = parseInt(input.value) + 1;
                updateQuantity(menuId, newQuantity);
            });
        });

        // Decrease quantity buttons
        document.querySelectorAll('.btn-qty-decrease').forEach(button => {
            button.addEventListener('click', function() {
                const menuId = parseInt(this.getAttribute('data-menu-id'));
                const card = this.closest('.cart-item');
                const input = card.querySelector('input[type="number"]');
                const currentQuantity = parseInt(input.value);

                if (currentQuantity > 1) {
                    updateQuantity(menuId, currentQuantity - 1);
                } else {
                    removeItem(menuId);
                }
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
