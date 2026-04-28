const TAX_RATE = 0.10;

class CustomerApp {
    constructor() {
        this.currentOrder = null;
        this.init();
    }

    init() {
        this.loadCartFromSession();
        this.setupEventListeners();

        if (window.location.pathname.includes('/payment')) {
            this.loadPaymentPage();
        }
    }

    loadCartFromSession() {
        fetch('/customer/api/cart/count')
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    this.updateCartSidebar();
                }
            })
            .catch(error => console.error('Error loading cart:', error));
    }

    setupEventListeners() {
        const searchInput = document.getElementById('searchInput');
        const searchButton = document.getElementById('searchButton');

        if (searchInput && searchButton) {
            searchButton.addEventListener('click', () => this.performSearch());
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') this.performSearch();
            });
        }

        document.querySelectorAll('.category-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                this.filterByCategory(btn.dataset.category);
            });
        });

        document.addEventListener('click', (e) => {
            if (e.target.closest('.btn-increase')) {
                this.increaseQuantity(e.target.closest('.btn-increase'));
            }
            if (e.target.closest('.btn-decrease')) {
                this.decreaseQuantity(e.target.closest('.btn-decrease'));
            }
        });
    }

    loadPaymentPage() {
        this.setupPaymentEventListeners();
        this.loadPaymentOrderDetails();
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

    filterByCategory(category) {
        document.querySelectorAll('.category-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        event.target.classList.add('active');

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

    updateCartSidebar() {
        const cartItemsDisplay = document.getElementById('cartItemsDisplay');
        const cartSubtotal = document.getElementById('cartSubtotal');

        if (!cartItemsDisplay) return;

        fetch('/customer/api/cart')
            .then(response => response.json())
            .then(data => {
                if (!data.success || !data.data || data.data.items.length === 0) {
                    cartItemsDisplay.innerHTML = `
                        <div class="empty-cart">
                            <i class="fas fa-shopping-cart"></i>
                            <p>Keranjang kosong</p>
                        </div>
                    `;
                    if (cartSubtotal) cartSubtotal.textContent = 'Rp.0';

                    this.updateMobileCartSummary(0, 0);
                    return;
                }

                let html = '';
                const cart = data.data;

                cart.items.forEach(item => {
                    const itemTotal = item.price * item.quantity;

                    html += `
                        <div class="cart-item">
                            <div class="cart-item-name">${item.menuName}</div>
                            <div class="cart-item-details">
                                <span>${item.quantity} x Rp ${item.price.toLocaleString('id-ID')}</span>
                                <span class="fw-bold">Rp ${itemTotal.toLocaleString('id-ID')}</span>
                            </div>
                        </div>
                    `;
                });

                cartItemsDisplay.innerHTML = html;
                if (cartSubtotal) {
                    cartSubtotal.textContent = `Rp.${cart.total.toLocaleString('id-ID')}`;
                }

                this.updateMobileCartSummary(cart.totalItems, cart.total);
                this.setupCartButtons();
            })
            .catch(error => {
                console.error('Error loading cart:', error);
            });
    }

    updateMobileCartSummary(itemCount, total) {
        const mobileCartSummary = document.getElementById('mobileCartSummary');
        const mobileCartCount = document.getElementById('mobileCartCount');
        const mobileCartTotal = document.getElementById('mobileCartTotal');

        if (mobileCartCount) {
            mobileCartCount.textContent = itemCount;
        }

        if (mobileCartTotal) {
            mobileCartTotal.textContent = `Rp ${total.toLocaleString('id-ID')}`;
        }

        if (mobileCartSummary) {
            if (itemCount > 0 && window.innerWidth <= 992) {
                mobileCartSummary.style.display = 'block';
            } else {
                mobileCartSummary.style.display = 'none';
            }
        }
    }

    setupCartButtons() {
        const checkoutBtn = document.getElementById('checkoutBtn');
        const clearCartBtn = document.getElementById('clearCartBtn');
        const mobileCheckoutBtn = document.getElementById('mobileCheckoutBtn');
        const mobileCleanCartBtn = document.getElementById('mobileCleanCartBtn');

        if (checkoutBtn) {
            checkoutBtn.onclick = () => {
                window.location.href = '/customer/cart';
            };
        }

        if (clearCartBtn) {
            clearCartBtn.onclick = () => {
                this.clearCart();
            };
        }

        if (mobileCheckoutBtn) {
            mobileCheckoutBtn.onclick = () => {
                window.location.href = '/customer/cart';
            };
        }

        if (mobileCleanCartBtn) {
            mobileCleanCartBtn.onclick = () => {
                this.clearCart();
            };
        }
    }

    removeFromCart(menuId) {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

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
                    this.showToast('Item berhasil dihapus dari keranjang', 'info');
                    this.loadCartFromSession();

                    if (window.location.pathname.includes('/cart')) {
                        setTimeout(() => {
                            window.location.reload();
                        }, 300);
                    }
                } else {
                    this.showToast('Gagal menghapus item', 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                this.showToast('Gagal menghapus item', 'error');
            });
    }

    clearCart() {
        if (!confirm('Yakin ingin mengosongkan keranjang?')) {
            return;
        }

        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

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
                    this.showToast('Keranjang berhasil dikosongkan', 'success');
                    this.loadCartFromSession();

                    if (window.location.pathname.includes('/cart')) {
                        setTimeout(() => {
                            window.location.reload();
                        }, 500);
                    }
                } else {
                    this.showToast('Gagal mengosongkan keranjang', 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                this.showToast('Gagal mengosongkan keranjang', 'error');
            });
    }

    showToast(message, type = 'info') {
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

    setupPaymentEventListeners() {
        const checkPaymentButton = document.querySelector('[onclick="checkPaymentStatus()"]');
        if (checkPaymentButton) {
            checkPaymentButton.removeAttribute('onclick');
            checkPaymentButton.addEventListener('click', () => this.checkPaymentStatus());
        }
    }

    async fetchOrderDetails(orderNumber) {
        try {
            const response = await fetch(`/customer/api/orders/${orderNumber}`);
            const data = await response.json();

            if (data.success) {
                const order = data.data;
                document.getElementById('paymentAmount').textContent =
                    'Rp ' + (order.total * (1 + TAX_RATE)).toLocaleString('id-ID');

                await this.generateQRCode(orderNumber);
                this.displayOrderDetails(order);

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
window.customerApp = customerApp;

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
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

        const headers = {
            'Content-Type': 'application/json'
        };

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
