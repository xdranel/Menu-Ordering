class CustomerApp {
    constructor() {
        this.cart = JSON.parse(localStorage.getItem('restaurantCart')) || [];
        this.currentOrder = null;
        this.init();
    }

    init() {
        this.updateCartCount();
        this.setupEventListeners();
        this.loadMenuData();

        // Check if we're on specific pages
        if (window.location.pathname.includes('/cart')) {
            this.loadCartPage();
        } else if (window.location.pathname.includes('/payment')) {
            this.loadPaymentPage();
        }
    }

    setupEventListeners() {
        // Search functionality
        const searchInput = document.getElementById('searchInput');
        const searchButton = document.getElementById('searchButton');

        if (searchInput && searchButton) {
            searchButton.addEventListener('click', () => this.performSearch());
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') this.performSearch();
            });
        }

        // Category filtering
        document.querySelectorAll('.category-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                this.filterByCategory(btn.dataset.category);
            });
        });

        // Quantity controls
        document.addEventListener('click', (e) => {
            if (e.target.closest('.btn-increase')) {
                this.increaseQuantity(e.target.closest('.btn-increase'));
            }
            if (e.target.closest('.btn-decrease')) {
                this.decreaseQuantity(e.target.closest('.btn-decrease'));
            }
        });
    }

    loadCartPage() {
        this.loadCartItems();
        this.setupCartEventListeners();
    }

    loadCartItems() {
        const cartItemsList = document.getElementById('cartItemsList');
        const emptyCart = document.getElementById('emptyCart');

        if (!cartItemsList || !emptyCart) return;

        if (this.cart.length === 0) {
            emptyCart.style.display = 'block';
            cartItemsList.innerHTML = '';
            cartItemsList.appendChild(emptyCart);
            return;
        }

        emptyCart.style.display = 'none';
        this.renderCartItems();
        this.updateOrderSummary();
    }

    loadPaymentPage() {
        this.setupPaymentEventListeners();
        this.loadPaymentOrderDetails();
    }

    renderCartItems() {
        const cartItemsList = document.getElementById('cartItemsList');
        if (!cartItemsList) return;

        // Show loading state
        cartItemsList.innerHTML = `
        <div class="alert alert-info">
            Memuat item keranjang...
        </div>
    `;

        // In a real implementation, you would fetch menu data and render items
        // For now, we'll show a simplified version
        let html = '';
        this.cart.forEach(item => {
            html += `
            <div class="card cart-item mb-3">
                <div class="card-body">
                    <div class="row align-items-center">
                        <div class="col-md-6">
                            <h6 class="card-title">${item.name || 'Menu Item'}</h6>
                            <p class="text-muted mb-0">Quantity: ${item.quantity}</p>
                        </div>
                        <div class="col-md-4 text-end">
                            <strong>Rp ${((item.price || 0) * item.quantity).toLocaleString('id-ID')}</strong>
                        </div>
                        <div class="col-md-2 text-end">
                            <button class="btn btn-outline-danger btn-sm" 
                                    onclick="customerApp.removeFromCart(${item.menuId})">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        });

        cartItemsList.innerHTML = html;
    }

    loadMenuData() {
        // Load menu data from server
        fetch('/customer/api/menus')
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    window.menuData = data.data;
                    this.setupMenuItems();
                }
            })
            .catch(error => console.error('Error loading menu data:', error));
    }

    loadPaymentOrderDetails() {
        const orderNumber = new URLSearchParams(window.location.search).get('order');
        if (!orderNumber) {
            this.showToast('Nomor pesanan tidak valid', 'error');
            window.location.href = '/customer/menu';
            return;
        }

        this.currentOrder = orderNumber;
        document.getElementById('orderNumber').textContent = orderNumber;

        this.fetchOrderDetails(orderNumber);
    }

    setupMenuItems() {
        // Setup menu item interactions
        document.querySelectorAll('.menu-card').forEach(card => {
            const menuId = card.dataset.menuId;
            const addButton = card.querySelector('.btn-add-to-cart');

            if (addButton) {
                addButton.addEventListener('click', () => {
                    const quantity = parseInt(card.querySelector('.quantity-input').value) || 1;
                    this.addToCart(parseInt(menuId), quantity);
                });
            }
        });
    }

    updateOrderSummary() {
        const subtotal = this.cart.reduce((sum, item) => {
            return sum + ((item.price || 0) * item.quantity);
        }, 0);

        const tax = subtotal * 0.10; // 10% tax
        const total = subtotal + tax;

        const subtotalElement = document.getElementById('orderSubtotal');
        const taxElement = document.getElementById('orderTax');
        const totalElement = document.getElementById('orderTotal');

        if (subtotalElement) subtotalElement.textContent = this.formatCurrency(subtotal);
        if (taxElement) taxElement.textContent = this.formatCurrency(tax);
        if (totalElement) totalElement.textContent = this.formatCurrency(total);
    }

    filterByCategory(category) {
        // Update active category button
        document.querySelectorAll('.category-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        event.target.classList.add('active');

        // Filter menu items
        const menuCards = document.querySelectorAll('.menu-card');
        menuCards.forEach(card => {
            if (category === 'SEMUA' || card.dataset.category === category) {
                card.style.display = 'block';
            } else {
                card.style.display = 'none';
            }
        });
    }

    performSearch() {
        const searchTerm = document.getElementById('searchInput').value.toLowerCase().trim();
        if (!searchTerm) return;

        const menuCards = document.querySelectorAll('.menu-card');
        menuCards.forEach(card => {
            const menuName = card.querySelector('.card-title').textContent.toLowerCase();
            const menuDescription = card.querySelector('.card-text').textContent.toLowerCase();

            if (menuName.includes(searchTerm) || menuDescription.includes(searchTerm)) {
                card.style.display = 'block';
            } else {
                card.style.display = 'none';
            }
        });
    }

    increaseQuantity(button) {
        const input = button.parentElement.querySelector('.quantity-input');
        input.value = parseInt(input.value) + 1;
    }

    decreaseQuantity(button) {
        const input = button.parentElement.querySelector('.quantity-input');
        if (parseInt(input.value) > 1) {
            input.value = parseInt(input.value) - 1;
        }
    }

    addToCart(menuId, quantity = 1) {
        const menuItem = window.menuData?.find(item => item.id === menuId);
        if (!menuItem) {
            this.showToast('Menu item not found', 'error');
            return;
        }

        if (!menuItem.available) {
            this.showToast('This item is currently unavailable', 'warning');
            return;
        }

        const existingItem = this.cart.find(item => item.menuId === menuId);
        if (existingItem) {
            existingItem.quantity += quantity;
        } else {
            this.cart.push({
                menuId: menuId,
                quantity: quantity,
                name: menuItem.name,
                price: menuItem.currentPrice,
                image: menuItem.imageUrl
            });
        }

        this.saveCart();
        this.updateCartCount();
        this.showToast(`${quantity} x ${menuItem.name} added to cart`, 'success');

        // Update cart dropdown if it exists
        this.updateCartDropdown();
    }

    updateCartCount() {
        const cartCount = document.getElementById('cartCount');
        if (cartCount) {
            const totalItems = this.cart.reduce((sum, item) => sum + item.quantity, 0);
            cartCount.textContent = totalItems;
        }
    }

    updateCartDropdown() {
        const cartDropdown = document.getElementById('cartDropdownContent');
        if (!cartDropdown) return;

        if (this.cart.length === 0) {
            cartDropdown.innerHTML = '<div class="text-muted p-2">Cart is empty</div>';
            return;
        }

        let html = '';
        let subtotal = 0;

        this.cart.forEach(item => {
            const itemTotal = item.price * item.quantity;
            subtotal += itemTotal;

            html += `
                <div class="cart-dropdown-item d-flex justify-content-between align-items-center p-2 border-bottom">
                    <div>
                        <h6 class="mb-0">${item.name}</h6>
                        <small class="text-muted">${item.quantity} x Rp ${item.price.toLocaleString('id-ID')}</small>
                    </div>
                    <div>
                        <strong>Rp ${itemTotal.toLocaleString('id-ID')}</strong>
                        <button class="btn btn-sm btn-outline-danger ms-2" onclick="customerApp.removeFromCart(${item.menuId})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            `;
        });

        html += `
            <div class="p-2">
                <div class="d-flex justify-content-between">
                    <strong>Subtotal:</strong>
                    <strong>Rp ${subtotal.toLocaleString('id-ID')}</strong>
                </div>
                <div class="d-grid gap-2 mt-2">
                    <a href="/customer/cart" class="btn btn-primary btn-sm">View Cart</a>
                    <button class="btn btn-outline-danger btn-sm" onclick="customerApp.clearCart()">Clear Cart</button>
                </div>
            </div>
        `;

        cartDropdown.innerHTML = html;
    }

    removeFromCart(menuId) {
        this.cart = this.cart.filter(item => item.menuId !== menuId);
        this.saveCart();
        this.updateCartCount();
        this.updateCartDropdown();
        this.showToast('Item removed from cart', 'info');
    }

    clearCart() {
        this.cart = [];
        this.saveCart();
        this.updateCartCount();
        this.updateCartDropdown();
        this.showToast('Cart cleared', 'info');
    }

    saveCart() {
        localStorage.setItem('restaurantCart', JSON.stringify(this.cart));
    }

    showToast(message, type = 'info') {
        // Implementation of toast notification
        const toast = document.createElement('div');
        toast.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
        toast.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        toast.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        document.body.appendChild(toast);

        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 3000);
    }

    formatCurrency(amount) {
        return 'Rp ' + amount.toLocaleString('id-ID');
    }

    setupCartEventListeners() {
        const clearCartButton = document.querySelector('[onclick="clearCart()"]');
        const createOrderButton = document.querySelector('[onclick="createOrder()"]');

        if (clearCartButton) {
            clearCartButton.removeAttribute('onclick');
            clearCartButton.addEventListener('click', () => this.clearCart());
        }

        if (createOrderButton) {
            createOrderButton.removeAttribute('onclick');
            createOrderButton.addEventListener('click', () => this.createOrderFromCart());
        }
    }

    setupPaymentEventListeners() {
        const checkPaymentButton = document.querySelector('[onclick="checkPaymentStatus()"]');
        if (checkPaymentButton) {
            checkPaymentButton.removeAttribute('onclick');
            checkPaymentButton.addEventListener('click', () => this.checkPaymentStatus());
        }
    }

    createOrderFromCart() {
        const customerNameInput = document.getElementById('customerName');
        if (!customerNameInput) return;

        const customerName = customerNameInput.value.trim();

        if (!customerName) {
            this.showToast('Silakan masukkan nama pemesan', 'warning');
            return;
        }

        if (this.cart.length === 0) {
            this.showToast('Keranjang kosong. Silakan tambahkan item terlebih dahulu.', 'warning');
            return;
        }

        this.createOrder(customerName);
    }

    // Checkout and payment methods
    async createOrder(customerName) {
        if (!customerName || customerName.trim() === '') {
            this.showToast('Please enter your name', 'warning');
            return;
        }

        if (this.cart.length === 0) {
            this.showToast('Your cart is empty', 'warning');
            return;
        }

        const orderData = {
            orderType: 'CUSTOMER_SELF',
            customerName: customerName.trim(),
            items: this.cart.map(item => ({
                menuId: item.menuId,
                quantity: item.quantity
            }))
        };

        try {
            const response = await fetch('/customer/api/orders', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(orderData)
            });

            const data = await response.json();

            if (data.success) {
                this.currentOrder = data.data;
                this.clearCart();
                window.location.href = `/customer/payment?order=${this.currentOrder.orderNumber}`;
            } else {
                this.showToast('Failed to create order: ' + data.message, 'error');
            }
        } catch (error) {
            console.error('Error creating order:', error);
            this.showToast('An error occurred while creating your order', 'error');
        }
    }

    async checkPaymentStatus(orderNumber) {
        try {
            const response = await fetch(`/customer/api/orders/${orderNumber}`);
            const data = await response.json();

            if (data.success) {
                return data.data;
            }
            return null;
        } catch (error) {
            console.error('Error checking payment status:', error);
            return null;
        }
    }

    async fetchOrderDetails(orderNumber) {
        try {
            const response = await fetch(`/customer/api/orders/${orderNumber}`);
            const data = await response.json();

            if (data.success) {
                const order = data.data;
                document.getElementById('paymentAmount').textContent =
                    'Rp ' + order.total.toLocaleString('id-ID');

                await this.generateQRCode(orderNumber);
                this.displayOrderDetails(order);

                // Start auto-checking payment status
                this.startPaymentStatusChecker();
            } else {
                this.showToast('Gagal memuat detail pesanan', 'error');
            }
        } catch (error) {
            console.error('Error fetching order details:', error);
            this.showToast('Terjadi kesalahan saat memuat detail pesanan', 'error');
        }
    }

    async generateQRCode(orderNumber) {
        try {
            const response = await fetch(`/customer/api/orders/${orderNumber}/qr-code`);
            const data = await response.json();

            if (data.success) {
                document.getElementById('qrCodeImage').src = data.data.qrCodeImage;
            }
        } catch (error) {
            console.error('Error generating QR code:', error);
        }
    }

    displayOrderDetails(order) {
        const orderDetails = document.getElementById('orderDetails');
        if (!orderDetails) return;

        let html = `
        <div class="row">
            <div class="col-md-6">
                <strong>Status Pesanan:</strong> <span class="badge bg-info">${order.status}</span>
            </div>
            <div class="col-md-6">
                <strong>Status Pembayaran:</strong> <span class="badge bg-warning">${order.paymentStatus}</span>
            </div>
        </div>
        <hr>
        <h6>Items:</h6>
        <ul class="list-group list-group-flush">
    `;

        order.items.forEach(item => {
            html += `
            <li class="list-group-item d-flex justify-content-between align-items-center">
                ${item.menu.name}
                <span>${item.quantity} x Rp ${item.price.toLocaleString('id-ID')}</span>
            </li>
        `;
        });

        html += `</ul>`;
        orderDetails.innerHTML = html;
    }

    async checkPaymentStatus() {
        if (!this.currentOrder) return;

        try {
            // Fetch order from API
            const response = await fetch(`/customer/api/orders/${this.currentOrder}`);
            const data = await response.json();

            if (data.success && data.data) {
                const order = data.data;
                if (order.paymentStatus === 'PAID') {
                    this.stopPaymentStatusChecker();
                    this.showToast('Pembayaran berhasil! Pesanan Anda sedang diproses.', 'success');
                    setTimeout(() => {
                        window.location.href = '/customer/menu?payment=success';
                    }, 2000);
                } else {
                    this.showToast('Pembayaran belum diterima. Silakan coba lagi dalam beberapa saat.', 'info');
                }
            } else {
                this.showToast('Gagal memeriksa status pembayaran.', 'error');
            }
        } catch (error) {
            console.error('Error checking payment status:', error);
            this.showToast('Terjadi kesalahan saat memeriksa status pembayaran', 'error');
        }
    }

    startPaymentStatusChecker() {
        this.paymentCheckInterval = setInterval(() => {
            this.checkPaymentStatus();
        }, 10000);
    }

    stopPaymentStatusChecker() {
        if (this.paymentCheckInterval) {
            clearInterval(this.paymentCheckInterval);
            this.paymentCheckInterval = null;
        }
    }
}

// Initialize customer app
const customerApp = new CustomerApp();

// ========== SIMULATE PAYMENT (FOR TESTING ONLY) ==========
window.simulatePayment = async function() {
    const orderNumber = new URLSearchParams(window.location.search).get('order');

    if (!orderNumber) {
        alert('Nomor pesanan tidak ditemukan');
        return;
    }

    if (!confirm('Simulasi pembayaran untuk testing?\n\nDalam production, pembayaran akan diproses oleh payment gateway.')) {
        return;
    }

    const btn = document.getElementById('simulatePaymentBtn');
    const originalHTML = btn ? btn.innerHTML : '';

    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> Memproses...';
    }

    try {
        // Get CSRF token
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

        const headers = {
            'Content-Type': 'application/json'
        };

        // Add CSRF token if available
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch(`/customer/api/orders/${orderNumber}/simulate-payment`, {
            method: 'POST',
            headers: headers
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        if (data.success) {
            alert('✅ Pembayaran berhasil disimulasikan!\n\nStatus: PAID\nPesanan Anda sedang diproses.');

            // Reload page immediately to show updated status
            window.location.reload();
        } else {
            alert('❌ Gagal: ' + (data.message || 'Unknown error'));
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalHTML;
            }
        }
    } catch (error) {
        console.error('Error simulating payment:', error);
        alert('❌ Terjadi kesalahan: ' + error.message);
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = originalHTML;
        }
    }
};