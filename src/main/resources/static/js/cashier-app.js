class CashierApp {
    constructor() {
        this.currentCashier = null;
        this.csrfToken = null;
        this.csrfHeader = null;
        this.init();
    }

    init() {
        this.loadCsrfToken();
        this.setupEventListeners();
        this.setupDashboardEventListeners();
        this.setupOrderModalListener();
        this.loadDashboardData();
        this.loadOrdersPage();
        this.setupOrderFilterListeners(); // Setup once during initialization
        this.setupReportsPage();
        this.setupRealTimeUpdates();
        this.setupMenuFilters(); // Setup menu filters on settings page
        this.availableMenus = [];
        this.availableCategories = [];
        this.selectedOrderItems = [];
        this.currentMenuFilter = '';
        this.currentCategoryFilter = '';
        this.orderItemCounter = 0;
    }

    setupOrderModalListener() {
        const newOrderModal = document.getElementById('newOrderModal');
        if (newOrderModal) {
            newOrderModal.addEventListener('show.bs.modal', async () => {
                // Reset the modal
                this.selectedOrderItems = [];
                document.getElementById('customerName').value = '';

                // Load menus and categories
                if (this.availableMenus.length === 0) {
                    await this.loadAvailableMenus();
                }
                await this.loadCategories();

                // Populate the modal
                this.populateMenuGrid();
                this.populateCategoryFilter();
                this.updateSelectedItemsDisplay();
                this.setupMenuSearch();
                this.setupCategoryFilter();
            });

            newOrderModal.addEventListener('hidden.bs.modal', () => {
                // Clean up when modal closes
                this.selectedOrderItems = [];
                this.currentMenuFilter = '';
                this.currentCategoryFilter = '';
            });
        }

        // Setup payment modal listeners
        this.setupPaymentModalListeners();
    }

    setupPaymentModalListeners() {
        const paymentMethodSelect = document.getElementById('paymentMethod');
        const cashAmountInput = document.getElementById('cashAmount');
        const processPaymentBtn = document.getElementById('processPaymentBtn');

        if (paymentMethodSelect) {
            paymentMethodSelect.addEventListener('change', () => {
                this.togglePaymentSections(paymentMethodSelect.value);
            });
        }

        if (cashAmountInput) {
            cashAmountInput.addEventListener('input', () => {
                this.calculateChange();
            });
        }

        if (processPaymentBtn) {
            processPaymentBtn.addEventListener('click', () => {
                this.processPayment();
            });
        }
    }

    togglePaymentSections(paymentMethod) {
        const cashSection = document.getElementById('cashPaymentSection');
        const qrSection = document.getElementById('qrPaymentSection');

        if (paymentMethod === 'CASH') {
            cashSection.style.display = 'block';
            qrSection.style.display = 'none';
        } else if (paymentMethod === 'QR_CODE') {
            cashSection.style.display = 'none';
            qrSection.style.display = 'block';
        }
    }

    calculateChange() {
        const totalElement = document.getElementById('paymentTotal');
        const cashAmountInput = document.getElementById('cashAmount');
        const changeElement = document.getElementById('changeAmount');

        if (!totalElement || !cashAmountInput || !changeElement) return;

        const total = parseFloat(totalElement.dataset.total || 0);
        const cashAmount = parseFloat(cashAmountInput.value || 0);
        const change = cashAmount - total;

        if (change >= 0) {
            changeElement.value = `Rp ${change.toLocaleString('id-ID')}`;
            changeElement.classList.remove('text-danger');
            changeElement.classList.add('text-success');
        } else {
            changeElement.value = `Kurang Rp ${Math.abs(change).toLocaleString('id-ID')}`;
            changeElement.classList.remove('text-success');
            changeElement.classList.add('text-danger');
        }
    }

    loadCsrfToken() {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');

        if (tokenMeta && headerMeta) {
            this.csrfToken = tokenMeta.getAttribute('content');
            this.csrfHeader = headerMeta.getAttribute('content');
            console.log('CSRF Token loaded:', this.csrfHeader, '=', this.csrfToken ? this.csrfToken.substring(0, 20) + '...' : 'null');
        } else {
            console.error('CSRF Token not found in meta tags!');
        }
    }

    getCsrfHeaders() {
        if (this.csrfToken && this.csrfHeader) {
            return {
                [this.csrfHeader]: this.csrfToken
            };
        }
        return {};
    }

    setupEventListeners() {
        // Refresh buttons
        document.querySelectorAll('[data-action="refresh"]').forEach(btn => {
            btn.addEventListener('click', () => this.refreshData());
        });

        // Order status updates
        document.addEventListener('click', (e) => {
            if (e.target.closest('[data-order-action]')) {
                const button = e.target.closest('[data-order-action]');
                const action = button.dataset.orderAction;
                const orderId = button.dataset.orderId;
                this.handleOrderAction(orderId, action);
            }
        });

        // Payment processing
        document.addEventListener('click', (e) => {
            if (e.target.closest('[data-payment-action]')) {
                const button = e.target.closest('[data-payment-action]');
                const action = button.dataset.paymentAction;
                const orderNumber = button.dataset.orderNumber;
                this.handlePaymentAction(orderNumber, action);
            }
        });
    }

    async loadDashboardData() {
        try {
            const response = await fetch('/cashier/api/dashboard/stats');
            const data = await response.json();

            if (data.success) {
                this.updateDashboard(data.data);
            }
        } catch (error) {
            console.error('Error loading dashboard data:', error);
            this.showToast('Failed to load dashboard data', 'error');
        }
    }

    updateDashboard(stats) {
        /// Update revenue
        const revenueElement = document.getElementById('todayRevenue');
        if (revenueElement) {
            revenueElement.textContent = `Rp ${stats.todayRevenue?.toLocaleString('id-ID') || '0'}`;
        }

        // Update order counts
        const todayOrdersElement = document.getElementById('todayOrders');
        if (todayOrdersElement) {
            todayOrdersElement.textContent = stats.todayOrders || '0';
        }

        const pendingOrdersElement = document.getElementById('pendingOrders');
        if (pendingOrdersElement) {
            pendingOrdersElement.textContent = stats.pendingOrders || '0';
        }

        const availableMenusElement = document.getElementById('availableMenus');
        if (availableMenusElement) {
            availableMenusElement.textContent = stats.availableMenus || '0';
        }

        // Update recent orders table
        this.updateRecentOrders(stats.recentOrders);

        // Update last update time
        const lastUpdateElement = document.getElementById('lastUpdate');
        if (lastUpdateElement) {
            lastUpdateElement.textContent = TimezoneUtils.formatTime(TimezoneUtils.now());
        }
    }

    updateRecentOrders(orders) {
        const tbody = document.getElementById('recentOrdersBody');
        if (!tbody) return;

        if (!orders || orders.length === 0) {
            tbody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center text-muted py-3">
                    Tidak ada pesanan terbaru
                </td>
            </tr>
        `;
            return;
        }

        let html = '';
        orders.forEach(order => {
            // Calculate final amount with 10% tax
            const subtotal = order.total || 0;
            const finalAmount = subtotal * 1.10;

            html += `
            <tr>
                <td>${order.orderNumber}</td>
                <td>${order.customerName || 'Walk-in Customer'}</td>
                <td>Rp ${finalAmount.toLocaleString('id-ID')}</td>
                <td>
                    <span class="badge bg-${this.getStatusColor(order.status)}">
                        ${order.status}
                    </span>
                </td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" 
                            onclick="cashierApp.viewOrder('${order.orderNumber}')">
                        <i class="fas fa-eye"></i>
                    </button>
                </td>
            </tr>
        `;
        });

        tbody.innerHTML = html;
    }

    // Add refresh button handler
    setupDashboardEventListeners() {
        const refreshButton = document.querySelector('[onclick="refreshDashboard()"]');
        if (refreshButton) {
            refreshButton.removeAttribute('onclick');
            refreshButton.addEventListener('click', () => this.refreshDashboard());
        }
    }

    refreshDashboard() {
        this.loadDashboardData();
        this.showToast('Dashboard diperbarui', 'info');
    }

    getStatusColor(status) {
        const statusColors = {
            'PENDING': 'warning',
            'CONFIRMED': 'info',
            'PREPARING': 'primary',
            'READY': 'success',
            'COMPLETED': 'secondary',
            'CANCELLED': 'danger'
        };
        return statusColors[status] || 'secondary';
    }

    formatDateTime(dateString) {
        if (!dateString) return '-';
        try {
            // Handle array format [year, month, day, hour, minute, second]
            // This happens when Jackson serializes LocalDateTime as array
            if (Array.isArray(dateString)) {
                // Convert array to ISO string: [2025, 11, 26, 15, 9, 10] -> "2025-11-26T15:09:10"
                const [year, month, day, hour, minute, second] = dateString;
                const isoString = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}T${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`;
                return TimezoneUtils.formatDateTime(isoString);
            }

            // Try to parse the date
            const date = new Date(dateString);
            // Check if date is valid
            if (isNaN(date.getTime())) {
                console.error('Invalid date:', dateString);
                return '-';
            }
            return TimezoneUtils.formatDateTime(dateString);
        } catch (e) {
            console.error('Error formatting date:', dateString, e);
            return '-';
        }
    }

    async handleOrderAction(orderId, action) {
        try {
            let url = '';
            let method = 'PUT';

            switch (action) {
                case 'confirm':
                    url = `/cashier/api/orders/${orderId}/status?status=CONFIRMED`;
                    break;
                case 'preparing':
                    url = `/cashier/api/orders/${orderId}/status?status=PREPARING`;
                    break;
                case 'ready':
                    url = `/cashier/api/orders/${orderId}/status?status=READY`;
                    break;
                case 'complete':
                    url = `/cashier/api/orders/${orderId}/status?status=COMPLETED`;
                    break;
                case 'cancel':
                    url = `/cashier/api/orders/${orderId}/status?status=CANCELLED`;
                    break;
                default:
                    throw new Error('Unknown action');
            }

            console.log('Sending request with CSRF headers:', this.getCsrfHeaders());

            const response = await fetch(url, {
                method: method,
                headers: {
                    ...this.getCsrfHeaders()
                }
            });
            const data = await response.json();

            if (data.success) {
                this.showToast(`Pesanan berhasil diupdate!`, 'success');

                // Refresh data based on current page
                if (document.getElementById('ordersTableBody')) {
                    this.loadOrdersPage(); // Refresh orders
                }
                if (document.getElementById('recentOrdersBody')) {
                    this.loadDashboardData(); // Refresh dashboard
                }


                if (window.webSocketClient) {
                    window.webSocketClient.sendOrderUpdate(data.data);
                }
            } else {
                // Check if it's a CSRF error
                if (response.status === 403 && data.message && data.message.includes('CSRF')) {
                    this.showToast('Session expired. Please refresh the page.', 'error');
                    setTimeout(() => {
                        window.location.reload();
                    }, 2000);
                } else {
                    this.showToast(`Gagal update pesanan: ${data.message}`, 'error');
                }
            }
        } catch (error) {
            console.error('Error handling order action:', error);
            this.showToast('Terjadi kesalahan: ' + error.message, 'error');
        }
    }

    async handlePaymentAction(orderNumber, action) {
        try {
            let paymentData = {
                orderNumber: orderNumber,
                paymentMethod: action.toUpperCase()
            };

            if (action === 'cash') {
                const amount = prompt('Enter cash amount tendered:');
                if (!amount || isNaN(amount)) {
                    this.showToast('Invalid amount', 'error');
                    return;
                }
                paymentData.cashAmount = parseFloat(amount);
            }

            const response = await fetch('/cashier/api/payments', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...this.getCsrfHeaders()
                },
                body: JSON.stringify(paymentData)
            });

            const data = await response.json();

            if (data.success) {
                this.showToast('Payment processed successfully', 'success');
                this.loadDashboardData();

                if (window.webSocketClient) {
                    window.webSocketClient.sendOrderUpdate(data.data);
                }
            } else {
                this.showToast(`Payment failed: ${data.message}`, 'error');
            }
        } catch (error) {
            console.error('Error processing payment:', error);
            this.showToast('An error occurred while processing payment', 'error');
        }
    }

    viewOrder(orderNumber) {
        window.open(`/cashier/orders?order=${orderNumber}`, '_blank');
    }

    refreshData() {
        this.loadDashboardData();
        this.showToast('Data refreshed', 'info');
    }

    setupRealTimeUpdates() {
        // Setup WebSocket order update handler
        window.orderUpdateHandler = (orderUpdate) => {
            this.showToast(`Order ${orderUpdate.orderNumber} updated: ${orderUpdate.status}`, 'info');
            this.loadDashboardData(); // Refresh
        };
    }

    // Orders Page Functions
    async loadOrdersPage(filterType = 'today', customDate = null, statusFilter = 'all', paymentFilter = 'all') {
        const ordersTableBody = document.getElementById('ordersTableBody');
        if (!ordersTableBody) return;

        try {
            let apiUrl = '/cashier/api/orders/all';

            // Determine which API endpoint to use based on date filter
            if (filterType === 'today') {
                apiUrl = '/cashier/api/orders/today';
            } else if (filterType === 'custom' && customDate) {
                apiUrl = `/cashier/api/orders/by-date?date=${customDate}`;
            }

            console.log('Loading orders from:', apiUrl);
            console.log('Filter params:', {filterType, customDate, statusFilter, paymentFilter});

            const response = await fetch(apiUrl);
            const data = await response.json();

            console.log('Orders API response:', data);

            if (data.success && data.data) {
                console.log(`Received ${data.data.length} orders from API`);

                // Debug: Log the first order's structure
                if (data.data.length > 0) {
                    console.log('Sample order structure:', data.data[0]);
                }

                // Store all orders for filtering
                this.allOrders = data.data;

                // Apply status and payment filters
                let filteredOrders = this.applyOrderFilters(data.data, statusFilter, paymentFilter);
                console.log(`After filtering: ${filteredOrders.length} orders`);

                this.updateOrdersTable(filteredOrders);
                this.updateOrderCount(filteredOrders.length);
            } else {
                console.error('API returned no data or failed:', data);
            }
        } catch (error) {
            console.error('Error loading orders:', error);
        }

        await this.loadAvailableMenus();
    }

    applyOrderFilters(orders, statusFilter, paymentFilter) {
        let filtered = orders;

        // Apply status filter
        if (statusFilter !== 'all') {
            filtered = filtered.filter(order => order.status === statusFilter);
        }

        // Apply payment filter
        if (paymentFilter !== 'all') {
            if (paymentFilter === 'PAID' || paymentFilter === 'PENDING') {
                // Filter by payment status
                filtered = filtered.filter(order => order.paymentStatus === paymentFilter);
            } else if (paymentFilter === 'CASH' || paymentFilter === 'QR_CODE') {
                // Filter by payment method
                filtered = filtered.filter(order => order.paymentMethod === paymentFilter);
            }
        }

        return filtered;
    }

    setupOrderFilterListeners() {
        const dateFilterType = document.getElementById('dateFilterType');
        const customDate = document.getElementById('customDate');
        const statusFilter = document.getElementById('statusFilter');
        const paymentFilter = document.getElementById('paymentFilter');
        const applyButton = document.getElementById('applyFilters');

        if (!dateFilterType) return;

        // Show/hide custom date input based on selection
        dateFilterType.addEventListener('change', () => {
            if (dateFilterType.value === 'custom') {
                customDate.style.display = 'block';
                // Set default to today's date
                const today = TimezoneUtils.toDateString();
                customDate.value = today;
            } else {
                customDate.style.display = 'none';
            }
        });

        // Apply all filters button
        if (applyButton) {
            applyButton.addEventListener('click', () => {
                const dateType = dateFilterType.value;
                const selectedDate = customDate.value;
                const status = statusFilter ? statusFilter.value : 'all';
                const payment = paymentFilter ? paymentFilter.value : 'all';
                this.loadOrdersPage(dateType, selectedDate, status, payment);
            });
        }

        // Also apply filter when status or payment filter changes
        if (statusFilter) {
            statusFilter.addEventListener('change', () => {
                const dateType = dateFilterType.value;
                const selectedDate = customDate.value;
                const status = statusFilter.value;
                const payment = paymentFilter ? paymentFilter.value : 'all';

                // If we have loaded orders, just refilter them
                if (this.allOrders) {
                    const filtered = this.applyOrderFilters(this.allOrders, status, payment);
                    this.updateOrdersTable(filtered);
                    this.updateOrderCount(filtered.length);
                }
            });
        }

        if (paymentFilter) {
            paymentFilter.addEventListener('change', () => {
                const dateType = dateFilterType.value;
                const selectedDate = customDate.value;
                const status = statusFilter ? statusFilter.value : 'all';
                const payment = paymentFilter.value;

                // If we have loaded orders, just refilter them
                if (this.allOrders) {
                    const filtered = this.applyOrderFilters(this.allOrders, status, payment);
                    this.updateOrdersTable(filtered);
                    this.updateOrderCount(filtered.length);
                }
            });
        }
    }

    updateOrderCount(count) {
        const badge = document.getElementById('orderCountBadge');
        if (badge) {
            badge.textContent = `${count} Pesanan`;
        }
    }

    async loadAvailableMenus() {
        try {
            console.log('Loading available menus...');
            const response = await fetch('/customer/api/menus');
            const data = await response.json();

            console.log('Menus response:', data);

            if (data.success && data.data) {
                this.availableMenus = data.data;
                console.log(`Loaded ${this.availableMenus.length} menus`);
            } else {
                console.error('Failed to load menus:', data);
                this.availableMenus = [];
            }
        } catch (error) {
            console.error('Error loading menus:', error);
            this.availableMenus = [];
        }
    }

    updateOrdersTable(orders) {
        const tbody = document.getElementById('ordersTableBody');
        if (!tbody) return;

        if (!orders || orders.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center text-muted py-3">
                        Tidak ada pesanan
                    </td>
                </tr>
            `;
            return;
        }

        let html = '';
        orders.forEach(order => {
            // Calculate final amount with 10% tax
            const subtotal = order.total || 0;
            const finalAmount = subtotal * 1.10;

            html += `
                <tr>
                    <td>${order.orderNumber}</td>
                    <td>${order.customerName || 'Walk-in'}</td>
                    <td>Rp ${finalAmount.toLocaleString('id-ID')}</td>
                    <td>
                        <span class="badge bg-${this.getStatusColor(order.status)}">
                            ${order.status}
                        </span>
                    </td>
                    <td>
                        <span class="badge bg-${this.getPaymentStatusColor(order.paymentStatus)}">
                            ${order.paymentStatus}
                        </span>
                    </td>
                    <td>${this.formatDateTime(order.createdAt)}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary"
                                onclick="cashierApp.viewOrderDetails('${order.orderNumber}')">
                            <i class="fas fa-eye"></i>
                        </button>
                        ${this.getOrderActionButtons(order)}
                    </td>
                </tr>
            `;
        });

        tbody.innerHTML = html;
    }

    getPaymentStatusColor(status) {
        const colors = {
            'PENDING': 'warning',
            'PAID': 'success',
            'FAILED': 'danger'
        };
        return colors[status] || 'secondary';
    }

    getOrderActionButtons(order) {
        let buttons = '';

        // Status transition buttons based on current status
        switch (order.status) {
            case 'PENDING':
                buttons += `
                    <button class="btn btn-sm btn-success ms-1" title="Konfirmasi"
                            data-order-action="confirm" data-order-id="${order.id}">
                        <i class="fas fa-check"></i>
                    </button>
                    <button class="btn btn-sm btn-danger ms-1" title="Batalkan"
                            data-order-action="cancel" data-order-id="${order.id}">
                        <i class="fas fa-times"></i>
                    </button>
                `;
                break;

            case 'CONFIRMED':
                buttons += `
                    <button class="btn btn-sm btn-primary ms-1" title="Mulai Persiapan"
                            data-order-action="preparing" data-order-id="${order.id}">
                        <i class="fas fa-fire"></i>
                    </button>
                `;
                break;

            case 'PREPARING':
                buttons += `
                    <button class="btn btn-sm btn-info ms-1" title="Siap Diantar"
                            data-order-action="ready" data-order-id="${order.id}">
                        <i class="fas fa-clipboard-check"></i>
                    </button>
                `;
                break;

            case 'READY':
                
                if (order.paymentStatus === 'PENDING') {
                    buttons += `
                        <button class="btn btn-sm btn-warning ms-1" title="Proses Pembayaran"
                                onclick="cashierApp.showPaymentModal('${order.orderNumber}')">
                            <i class="fas fa-money-bill"></i>
                        </button>
                    `;
                } else {
                    buttons += `
                        <button class="btn btn-sm btn-success ms-1" title="Selesaikan Pesanan"
                                data-order-action="complete" data-order-id="${order.id}">
                            <i class="fas fa-check-double"></i>
                        </button>
                    `;
                }
                break;

            case 'COMPLETED':
                
                break;

            case 'CANCELLED':
                
                break;
        }

        return buttons;
    }

    async loadCategories() {
        try {
            const response = await fetch('/cashier/api/categories');
            const data = await response.json();
            if (data.success && data.data) {
                this.availableCategories = data.data;
            }
        } catch (error) {
            console.error('Error loading categories:', error);
        }
    }

    populateCategoryFilter() {
        const categoryFilter = document.getElementById('categoryFilter');
        if (!categoryFilter) return;

        // Filter out "SEMUA" category as it's a placeholder, not a real category
        const options = this.availableCategories
            .filter(cat => cat.name.toUpperCase() !== 'SEMUA')
            .map(cat => `<option value="${cat.name}">${cat.name}</option>`)
            .join('');

        categoryFilter.innerHTML = '<option value="">Semua Kategori</option>' + options;
    }

    populateMenuGrid() {
        const menuGrid = document.getElementById('menuGrid');
        if (!menuGrid) return;

        let filteredMenus = this.availableMenus.filter(menu => menu.available);

        // Apply search filter
        if (this.currentMenuFilter) {
            const search = this.currentMenuFilter.toLowerCase();
            filteredMenus = filteredMenus.filter(menu =>
                menu.name.toLowerCase().includes(search) ||
                (menu.description && menu.description.toLowerCase().includes(search))
            );
        }

        // Apply category filter
        if (this.currentCategoryFilter) {
            filteredMenus = filteredMenus.filter(menu =>
                menu.category && menu.category.name === this.currentCategoryFilter
            );
        }

        if (filteredMenus.length === 0) {
            menuGrid.innerHTML = `
                <div class="col-12 text-center text-muted py-4">
                    <i class="fas fa-search fa-2x mb-2"></i>
                    <p>Tidak ada menu ditemukan</p>
                </div>
            `;
            return;
        }

        menuGrid.innerHTML = filteredMenus.map(menu => `
            <div class="col-md-6">
                <div class="card menu-card-cashier" style="cursor: pointer;" data-menu-id="${menu.id}">
                    <div class="card-body p-2">
                        <div class="d-flex align-items-center">
                            <div class="flex-grow-1">
                                <h6 class="mb-1">${menu.name}</h6>
                                <p class="text-muted small mb-1">${menu.category ? menu.category.name : ''}</p>
                                <strong class="text-primary" style="font-size: 1.1rem;">Rp ${menu.currentPrice.toLocaleString('id-ID')}</strong>
                            </div>
                            <button type="button" class="btn btn-sm btn-primary add-menu-btn" data-menu-id="${menu.id}">
                                <i class="fas fa-plus"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `).join('');

        // Add click listeners
        menuGrid.querySelectorAll('.add-menu-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const menuId = parseInt(btn.dataset.menuId);
                this.addMenuToOrder(menuId);
            });
        });
    }

    setupMenuSearch() {
        const searchInput = document.getElementById('menuSearch');
        if (!searchInput) return;

        searchInput.addEventListener('input', (e) => {
            this.currentMenuFilter = e.target.value;
            this.populateMenuGrid();
        });
    }

    setupCategoryFilter() {
        const categoryFilter = document.getElementById('categoryFilter');
        if (!categoryFilter) return;

        categoryFilter.addEventListener('change', (e) => {
            this.currentCategoryFilter = e.target.value;
            this.populateMenuGrid();
        });
    }

    addMenuToOrder(menuId) {
        const menu = this.availableMenus.find(m => m.id === menuId);
        if (!menu) return;

        // Check if item already in order
        const existingItem = this.selectedOrderItems.find(item => item.menuId === menuId);
        if (existingItem) {
            existingItem.quantity++;
        } else {
            this.selectedOrderItems.push({
                menuId: menu.id,
                menuName: menu.name,
                price: menu.currentPrice,
                quantity: 1
            });
        }

        this.updateSelectedItemsDisplay();
        this.showToast(`${menu.name} ditambahkan`, 'success');
    }

    updateSelectedItemsDisplay() {
        const container = document.getElementById('selectedItemsContainer');
        const emptyMessage = document.getElementById('emptySelectedItems');
        const countBadge = document.getElementById('selectedItemsCount');
        const totalDisplay = document.getElementById('orderTotal');

        if (!container) return;

        if (countBadge) {
            countBadge.textContent = this.selectedOrderItems.length;
        }

        if (this.selectedOrderItems.length === 0) {
            if (emptyMessage) {
                emptyMessage.style.display = 'block';
            }
            container.innerHTML = '';
            if (totalDisplay) {
                totalDisplay.textContent = 'Rp 0';
            }
            return;
        }

        if (emptyMessage) {
            emptyMessage.style.display = 'none';
        }

        let total = 0;
        const itemsHTML = this.selectedOrderItems.map((item, index) => {
            const subtotal = item.price * item.quantity;
            total += subtotal;

            return `
                <div class="card mb-2">
                    <div class="card-body p-2">
                        <div class="d-flex justify-content-between align-items-start">
                            <div class="flex-grow-1">
                                <h6 class="mb-1">${item.menuName}</h6>
                                <div class="d-flex align-items-center gap-2">
                                    <div class="btn-group btn-group-sm" role="group">
                                        <button type="button" class="btn btn-outline-secondary" onclick="cashierApp.decreaseQuantity(${index})">
                                            <i class="fas fa-minus"></i>
                                        </button>
                                        <input type="number" class="form-control" style="width: 50px; text-align: center;"
                                               value="${item.quantity}" min="1" readonly>
                                        <button type="button" class="btn btn-outline-secondary" onclick="cashierApp.increaseQuantity(${index})">
                                            <i class="fas fa-plus"></i>
                                        </button>
                                    </div>
                                    <span class="text-muted">×</span>
                                    <strong style="font-size: 1rem;">Rp ${item.price.toLocaleString('id-ID')}</strong>
                                </div>
                            </div>
                            <div class="text-end">
                                <button type="button" class="btn btn-sm btn-danger mb-1" onclick="cashierApp.removeFromOrder(${index})">
                                    <i class="fas fa-trash"></i>
                                </button>
                                <div><strong class="text-primary">Rp ${subtotal.toLocaleString('id-ID')}</strong></div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        container.innerHTML = itemsHTML;
        if (totalDisplay) {
            totalDisplay.textContent = `Rp ${total.toLocaleString('id-ID')}`;
        }
    }

    increaseQuantity(index) {
        if (this.selectedOrderItems[index]) {
            this.selectedOrderItems[index].quantity++;
            this.updateSelectedItemsDisplay();
        }
    }

    decreaseQuantity(index) {
        if (this.selectedOrderItems[index] && this.selectedOrderItems[index].quantity > 1) {
            this.selectedOrderItems[index].quantity--;
            this.updateSelectedItemsDisplay();
        }
    }

    removeFromOrder(index) {
        this.selectedOrderItems.splice(index, 1);
        this.updateSelectedItemsDisplay();
        this.showToast('Item dihapus', 'info');
    }

    async createNewOrder() {
        const customerName = document.getElementById('customerName').value.trim();

        // Validate
        if (!customerName) {
            this.showToast('Nama customer harus diisi', 'error');
            return;
        }

        if (this.selectedOrderItems.length === 0) {
            this.showToast('Tambahkan minimal 1 item menu', 'error');
            return;
        }

        const orderItems = this.selectedOrderItems.map(item => ({
            menuId: item.menuId,
            quantity: item.quantity
        }));

        try {
            const requestBody = {
                customerName: customerName,
                orderType: 'CASHIER_ASSISTED',
                items: orderItems
            };

            console.log('Creating order with data:', requestBody);

            const response = await fetch('/cashier/api/orders', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...this.getCsrfHeaders()
                },
                body: JSON.stringify(requestBody)
            });

            const data = await response.json();
            console.log('Order creation response:', data);

            if (data.success) {
                this.showToast('Pesanan berhasil dibuat!', 'success');

                // Close modal
                const modal = bootstrap.Modal.getInstance(document.getElementById('newOrderModal'));
                if (modal) modal.hide();

                // Reset form
                document.getElementById('newOrderForm').reset();
                this.selectedOrderItems = [];
                this.currentMenuFilter = '';
                this.currentCategoryFilter = '';

                // Refresh orders
                this.loadOrdersPage();
            } else {
                this.showToast('Gagal membuat pesanan: ' + data.message, 'error');
            }
        } catch (error) {
            console.error('Error creating order:', error);
            this.showToast('Terjadi kesalahan saat membuat pesanan', 'error');
        }
    }

    viewOrderDetails(orderNumber) {
        // Find the order in the cached data
        const order = this.allOrders ? this.allOrders.find(o => o.orderNumber === orderNumber) : null;

        if (!order) {
            this.showToast('Pesanan tidak ditemukan', 'error');
            return;
        }

        // Generate order details HTML
        const formattedDate = TimezoneUtils.formatDateTime(order.createdAt, {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });

        const statusBadge = this.getStatusBadge(order.status);
        const paymentStatusBadge = this.getPaymentStatusBadge(order.paymentStatus);
        const paymentMethodBadge = order.paymentMethod ? this.getPaymentMethodBadge(order.paymentMethod) : '<span class="badge bg-secondary">-</span>';

        let itemsHtml = '';
        if (order.items && order.items.length > 0) {
            itemsHtml = order.items.map(item => {
                const itemTotal = item.quantity * item.price;
                const menuName = item.menu ? item.menu.name : 'Item';
                return `
                    <tr>
                        <td>${menuName}</td>
                        <td class="text-center">${item.quantity}</td>
                        <td class="text-end">Rp ${item.price.toLocaleString('id-ID')}</td>
                        <td class="text-end"><strong>Rp ${itemTotal.toLocaleString('id-ID')}</strong></td>
                    </tr>
                `;
            }).join('');
        } else {
            itemsHtml = '<tr><td colspan="4" class="text-center text-muted">Tidak ada item</td></tr>';
        }

        const detailsHtml = `
            <div class="order-details">
                <div class="row mb-3">
                    <div class="col-md-6">
                        <h6 class="text-muted mb-2">Informasi Pesanan</h6>
                        <table class="table table-sm table-borderless">
                            <tr>
                                <td width="140"><strong>Nomor Pesanan:</strong></td>
                                <td>${order.orderNumber}</td>
                            </tr>
                            <tr>
                                <td><strong>Nama Customer:</strong></td>
                                <td>${order.customerName || '-'}</td>
                            </tr>
                            <tr>
                                <td><strong>Tipe Pesanan:</strong></td>
                                <td><span class="badge bg-info">${order.orderType === 'CUSTOMER_SELF' ? 'CUSTOMER SELF' : 'CASHIER ASSISTED'}</span></td>
                            </tr>
                            <tr>
                                <td><strong>Tanggal:</strong></td>
                                <td>${formattedDate}</td>
                            </tr>
                        </table>
                    </div>
                    <div class="col-md-6">
                        <h6 class="text-muted mb-2">Status & Pembayaran</h6>
                        <table class="table table-sm table-borderless">
                            <tr>
                                <td width="140"><strong>Status Pesanan:</strong></td>
                                <td>${statusBadge}</td>
                            </tr>
                            <tr>
                                <td><strong>Status Pembayaran:</strong></td>
                                <td>${paymentStatusBadge}</td>
                            </tr>
                            <tr>
                                <td><strong>Metode Pembayaran:</strong></td>
                                <td>${paymentMethodBadge}</td>
                            </tr>
                        </table>
                    </div>
                </div>

                <hr>

                <h6 class="text-muted mb-3">Detail Item Pesanan</h6>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead class="table-light">
                            <tr>
                                <th>Item</th>
                                <th class="text-center">Qty</th>
                                <th class="text-end">Harga Satuan</th>
                                <th class="text-end">Subtotal</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${itemsHtml}
                        </tbody>
                        <tfoot>
                            <tr class="table-light">
                                <td colspan="3" class="text-end"><strong>Subtotal:</strong></td>
                                <td class="text-end">Rp ${order.total.toLocaleString('id-ID')}</td>
                            </tr>
                            <tr class="table-light">
                                <td colspan="3" class="text-end"><strong>Pajak (10%):</strong></td>
                                <td class="text-end">Rp ${(order.total * 0.10).toLocaleString('id-ID')}</td>
                            </tr>
                            <tr class="table-light border-top">
                                <td colspan="3" class="text-end"><strong>TOTAL:</strong></td>
                                <td class="text-end"><strong class="text-primary">Rp ${(order.total * 1.10).toLocaleString('id-ID')}</strong></td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        `;

        // Populate modal and show it
        const modalContent = document.getElementById('orderDetailsContent');
        if (modalContent) {
            modalContent.innerHTML = detailsHtml;
            const modal = new bootstrap.Modal(document.getElementById('orderDetailsModal'));
            modal.show();
        }
    }

    async showPaymentModal(orderNumber) {
        try {
            // Fetch order details
            const response = await fetch(`/cashier/api/orders/all`);
            const data = await response.json();

            if (data.success && data.data) {
                const order = data.data.find(o => o.orderNumber === orderNumber);

                if (order) {
                    // Calculate tax (10%) and final amount
                    const subtotal = order.total;
                    const tax = subtotal * 0.10;
                    const finalAmount = subtotal + tax;

                    // Populate modal
                    document.getElementById('paymentOrderNumber').value = order.orderNumber;
                    document.getElementById('paymentTotal').value = `Rp ${finalAmount.toLocaleString('id-ID')}`;
                    document.getElementById('paymentTotal').dataset.total = finalAmount;

                    
                    document.getElementById('paymentMethod').value = 'CASH';
                    document.getElementById('cashAmount').value = '';
                    document.getElementById('changeAmount').value = '';
                    document.getElementById('qrTransactionCode').value = '';

                    // Show cash section by default
                    this.togglePaymentSections('CASH');

                    // Show modal
                    const modal = new bootstrap.Modal(document.getElementById('paymentModal'));
                    modal.show();
                } else {
                    this.showToast('Pesanan tidak ditemukan', 'error');
                }
            }
        } catch (error) {
            console.error('Error loading order for payment:', error);
            this.showToast('Gagal memuat data pesanan', 'error');
        }
    }

    async processPayment() {
        const orderNumber = document.getElementById('paymentOrderNumber').value;
        const paymentMethod = document.getElementById('paymentMethod').value;
        const totalElement = document.getElementById('paymentTotal');
        const total = parseFloat(totalElement.dataset.total || 0);

        let paymentData = {
            orderNumber: orderNumber,
            paymentMethod: paymentMethod
        };

        // Validation based on payment method
        if (paymentMethod === 'CASH') {
            const cashAmount = parseFloat(document.getElementById('cashAmount').value || 0);

            if (cashAmount < total) {
                this.showToast('Jumlah uang tidak cukup!', 'error');
                return;
            }

            paymentData.cashAmount = cashAmount;
        } else if (paymentMethod === 'QR_CODE') {
            const qrCode = document.getElementById('qrTransactionCode').value.trim();

            if (!qrCode) {
                this.showToast('Kode transaksi QR harus diisi!', 'error');
                return;
            }

            paymentData.qrData = qrCode;
        }

        try {
            console.log('Processing payment:', paymentData);

            const response = await fetch('/cashier/api/payments', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...this.getCsrfHeaders()
                },
                body: JSON.stringify(paymentData)
            });

            const data = await response.json();
            console.log('Payment response:', data);

            if (data.success) {
                // Show change for cash payments
                if (paymentMethod === 'CASH' && data.data.change > 0) {
                    this.showToast(`Pembayaran berhasil! Kembalian: Rp ${data.data.change.toLocaleString('id-ID')}`, 'success');
                } else {
                    this.showToast('Pembayaran berhasil!', 'success');
                }

                
                const modal = bootstrap.Modal.getInstance(document.getElementById('paymentModal'));
                if (modal) modal.hide();

                // Refresh orders
                this.loadOrdersPage();
            } else {
                this.showToast(`Pembayaran gagal: ${data.message}`, 'error');
            }
        } catch (error) {
            console.error('Error processing payment:', error);
            this.showToast('Terjadi kesalahan saat memproses pembayaran', 'error');
        }
    }

    // Reports Page Functions
    setupReportsPage() {
        const startDateInput = document.getElementById('startDate');
        const endDateInput = document.getElementById('endDate');
        const generateBtn = document.getElementById('generateReportBtn');

        if (!startDateInput || !endDateInput) {
            console.log('Not on reports page');
            return; // Not on reports page
        }

        console.log('Setting up reports page (v2)...');

        // Set default dates (today)
        const today = TimezoneUtils.toDateString();
        startDateInput.value = today;
        endDateInput.value = today;

        console.log('Default dates set to:', today);

        // Attach event listener to Generate button
        if (generateBtn) {
            generateBtn.addEventListener('click', () => {
                console.log('Generate button clicked (v2)');
                this.generateReport();
            });
            console.log('Generate button listener attached (v2)');
        }

        // Load today's report
        this.loadReportFromOrders();
    }

    async generateReport() {
        // UPDATED: 2025-11-17 22:26 - Fixed report generation
        const startDate = document.getElementById('startDate').value;
        const endDate = document.getElementById('endDate').value;

        if (!startDate || !endDate) {
            this.showToast('Pilih tanggal mulai dan akhir', 'error');
            return;
        }

        console.log('*** GENERATING REPORT (NEW VERSION) for:', startDate, 'to', endDate);

        // Directly load report from orders
        this.loadReportFromOrders();
    }

    async loadReportFromOrders() {
        try {
            console.log('Loading report from orders...');
            const response = await fetch('/cashier/api/orders/all');
            const data = await response.json();

            console.log('Orders data received:', data);

            if (data.success && data.data) {
                const orders = data.data;
                console.log('Total orders in database:', orders.length);

                // Get date values from inputs
                const startDateStr = document.getElementById('startDate').value;
                const endDateStr = document.getElementById('endDate').value;

                console.log('Date range (strings):', startDateStr, 'to', endDateStr);

                // Filter orders by date range - compare only dates, not times
                const filteredOrders = orders.filter(order => {
                    // Extract date portion from order's createdAt
                    let orderDateStr;

                    if (Array.isArray(order.createdAt)) {
                        // Handle array format: [2025, 11, 26, 15, 9, 10] -> "2025-11-26"
                        const [year, month, day] = order.createdAt;
                        orderDateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                    } else if (typeof order.createdAt === 'string') {
                        // Handle string format: "2025-11-19T10:30:45" -> "2025-11-19"
                        orderDateStr = order.createdAt.split('T')[0];
                    } else {
                        return false; // Skip invalid dates
                    }

                    // Compare as strings (YYYY-MM-DD format compares correctly)
                    return orderDateStr >= startDateStr && orderDateStr <= endDateStr;
                });

                console.log('Filtered orders:', filteredOrders.length);

                // Calculate totals
                const totalOrders = filteredOrders.length;
                const completedOrders = filteredOrders.filter(o => o.status === 'COMPLETED');
                const cancelledOrders = filteredOrders.filter(o => o.status === 'CANCELLED');
                const paidOrders = filteredOrders.filter(o => o.paymentStatus === 'PAID');
                // Calculate revenue with 10% tax included
                const totalRevenue = paidOrders.reduce((sum, order) => sum + ((order.total || 0) * 1.10), 0);

                // Payment method breakdown
                const qrPayments = paidOrders.filter(o => o.paymentMethod === 'QR_CODE').length;
                const cashPayments = paidOrders.filter(o => o.paymentMethod === 'CASH').length;

                console.log('Completed orders:', completedOrders.length);
                console.log('Paid orders:', paidOrders.length);
                console.log('Cancelled orders:', cancelledOrders.length);
                console.log('QR payments:', qrPayments);
                console.log('Cash payments:', cashPayments);
                console.log('Total revenue:', totalRevenue);

                // Update UI - Main Stats
                const revenueElement = document.getElementById('totalRevenue');
                const ordersElement = document.getElementById('totalOrders');

                if (revenueElement) {
                    revenueElement.textContent = `Rp ${totalRevenue.toLocaleString('id-ID')}`;
                }

                if (ordersElement) {
                    ordersElement.textContent = totalOrders;
                }

                // Update UI - Payment Method Breakdown
                const qrCountElement = document.getElementById('qrPaymentCount');
                const cashCountElement = document.getElementById('cashPaymentCount');
                const cancelledElement = document.getElementById('cancelledOrders');

                if (qrCountElement) {
                    qrCountElement.textContent = qrPayments;
                }

                if (cashCountElement) {
                    cashCountElement.textContent = cashPayments;
                }

                if (cancelledElement) {
                    cancelledElement.textContent = cancelledOrders.length;
                }

                // Load invoices for the same date range
                await this.loadInvoices(startDateStr, endDateStr);

                this.showToast('Laporan berhasil di-generate', 'success');
            } else {
                console.error('Failed to load orders:', data);
            }
        } catch (error) {
            console.error('Error loading report:', error);
        }
    }

    async loadInvoices(startDate, endDate) {
        try {
            console.log('=== LOADING INVOICES ===');
            console.log('Date range:', startDate, 'to', endDate);

            const url = `/cashier/api/invoices/by-date?startDate=${startDate}&endDate=${endDate}`;
            console.log('Fetching from URL:', url);

            const response = await fetch(url);
            console.log('Response status:', response.status);

            if (!response.ok) {
                console.error('HTTP error:', response.status, response.statusText);
                this.updateInvoicesTable([]);
                this.showToast('Gagal memuat invoice: ' + response.statusText, 'error');
                return;
            }

            const data = await response.json();
            console.log('Invoices API response:', data);
            console.log('Success:', data.success);
            console.log('Data array:', data.data);
            console.log('Invoice count:', data.data ? data.data.length : 0);

            if (data.success && data.data) {
                console.log('Updating table with', data.data.length, 'invoices');
                // Store all invoices for filtering
                this.allInvoices = data.data;
                this.updateInvoicesTable(data.data);
                this.setupInvoiceFilter();
            } else {
                console.error('API returned success=false or no data:', data);
                this.allInvoices = [];
                this.updateInvoicesTable([]);
            }
        } catch (error) {
            console.error('Error loading invoices:', error);
            console.error('Error stack:', error.stack);
            this.allInvoices = [];
            this.updateInvoicesTable([]);
            this.showToast('Terjadi kesalahan saat memuat invoice', 'error');
        }
    }

    setupInvoiceFilter() {
        const filterButtons = document.querySelectorAll('input[name="invoiceFilter"]');
        filterButtons.forEach(button => {
            button.addEventListener('change', (e) => {
                const filterValue = e.target.value;
                this.filterInvoices(filterValue);
            });
        });
    }

    filterInvoices(filterType) {
        if (!this.allInvoices) {
            return;
        }

        let filteredInvoices = this.allInvoices;

        if (filterType === 'qr') {
            filteredInvoices = this.allInvoices.filter(inv => inv.paymentMethod === 'QR_CODE');
        } else if (filterType === 'cash') {
            filteredInvoices = this.allInvoices.filter(inv => inv.paymentMethod === 'CASH');
        }

        this.updateInvoicesTable(filteredInvoices);
    }

    updateInvoicesTable(invoices) {
        const tableBody = document.getElementById('invoicesTableBody');
        if (!tableBody) return;

        if (invoices.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center text-muted py-4">
                        <i class="fas fa-inbox fa-2x mb-2"></i>
                        <p class="mb-0">Tidak ada invoice untuk periode ini</p>
                    </td>
                </tr>
            `;
            return;
        }

        tableBody.innerHTML = invoices.map(invoice => {
            const formattedDate = TimezoneUtils.formatDateTime(invoice.createdAt, {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });

            const paymentBadge = this.getPaymentMethodBadge(invoice.paymentMethod);

            return `
                <tr>
                    <td><strong>${invoice.invoiceNumber}</strong></td>
                    <td>${invoice.order ? invoice.order.orderNumber : '-'}</td>
                    <td>${invoice.order ? invoice.order.customerName : '-'}</td>
                    <td>
                        <strong>Rp ${invoice.finalAmount.toLocaleString('id-ID')}</strong>
                        <br>
                        <small class="text-muted">${paymentBadge}</small>
                    </td>
                    <td>${formattedDate}</td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="cashierApp.viewInvoice(${invoice.id})">
                            <i class="fas fa-eye"></i> Lihat
                        </button>
                        <button class="btn btn-sm btn-success" onclick="cashierApp.downloadInvoicePdf(${invoice.id})">
                            <i class="fas fa-download"></i> PDF
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    }

    getPaymentMethodBadge(method) {
        switch(method) {
            case 'CASH':
                return '<span class="badge bg-success">Tunai</span>';
            case 'QR_CODE':
                return '<span class="badge bg-info">QR Code</span>';
            default:
                return '<span class="badge bg-secondary">' + method + '</span>';
        }
    }

    getStatusBadge(status) {
        const color = this.getStatusColor(status);
        return `<span class="badge bg-${color}">${status}</span>`;
    }

    getPaymentStatusBadge(status) {
        const color = this.getPaymentStatusColor(status);
        return `<span class="badge bg-${color}">${status}</span>`;
    }

    viewInvoice(invoiceId) {
        // Find the invoice in the cached data
        const invoice = this.allInvoices ? this.allInvoices.find(inv => inv.id === invoiceId) : null;

        if (!invoice) {
            this.showToast('Invoice tidak ditemukan', 'error');
            return;
        }

        // Generate invoice details HTML
        // Handle both array and string date formats
        let dateString = invoice.createdAt;
        if (Array.isArray(dateString)) {
            const [year, month, day, hour, minute, second] = dateString;
            dateString = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}T${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`;
        }
        const invoiceDate = new Date(dateString);
        const formattedDate = invoiceDate.toLocaleDateString('id-ID', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });

        const paymentMethodBadge = this.getPaymentMethodBadge(invoice.paymentMethod);

        let itemsHtml = '';
        if (invoice.order && invoice.order.items && invoice.order.items.length > 0) {
            itemsHtml = invoice.order.items.map(item => {
                const itemTotal = item.quantity * item.price;
                const menuName = item.menu ? item.menu.name : 'Item';
                return `
                    <tr>
                        <td>${menuName}</td>
                        <td class="text-center">${item.quantity}</td>
                        <td class="text-end">Rp ${item.price.toLocaleString('id-ID')}</td>
                        <td class="text-end"><strong>Rp ${itemTotal.toLocaleString('id-ID')}</strong></td>
                    </tr>
                `;
            }).join('');
        } else {
            itemsHtml = '<tr><td colspan="4" class="text-center text-muted">Tidak ada item</td></tr>';
        }

        const detailsHtml = `
            <div class="invoice-details">
                <!-- Invoice Header -->
                <div class="text-center mb-4">
                    <h4 class="text-primary mb-1">ChopChop Restaurant</h4>
                    <p class="text-muted mb-0">Jl. Kuliner No. 123, Jakarta</p>
                    <p class="text-muted mb-0">Telp: (021) 1234-5678</p>
                </div>

                <hr>

                <!-- Invoice Info -->
                <div class="row mb-4">
                    <div class="col-md-6">
                        <h6 class="text-muted mb-2">Informasi Invoice</h6>
                        <table class="table table-sm table-borderless">
                            <tr>
                                <td width="140"><strong>No. Invoice:</strong></td>
                                <td>${invoice.invoiceNumber}</td>
                            </tr>
                            <tr>
                                <td><strong>Tanggal:</strong></td>
                                <td>${formattedDate}</td>
                            </tr>
                            <tr>
                                <td><strong>Kasir:</strong></td>
                                <td>${invoice.cashier ? invoice.cashier.displayName : '-'}</td>
                            </tr>
                        </table>
                    </div>
                    <div class="col-md-6">
                        <h6 class="text-muted mb-2">Informasi Pesanan</h6>
                        <table class="table table-sm table-borderless">
                            <tr>
                                <td width="140"><strong>No. Pesanan:</strong></td>
                                <td>${invoice.order ? invoice.order.orderNumber : '-'}</td>
                            </tr>
                            <tr>
                                <td><strong>Customer:</strong></td>
                                <td>${invoice.order ? invoice.order.customerName : '-'}</td>
                            </tr>
                            <tr>
                                <td><strong>Pembayaran:</strong></td>
                                <td>${paymentMethodBadge}</td>
                            </tr>
                        </table>
                    </div>
                </div>

                <hr>

                <!-- Items Table -->
                <h6 class="text-muted mb-3">Detail Item</h6>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead class="table-light">
                            <tr>
                                <th>Item</th>
                                <th class="text-center">Qty</th>
                                <th class="text-end">Harga Satuan</th>
                                <th class="text-end">Subtotal</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${itemsHtml}
                        </tbody>
                    </table>
                </div>

                <hr>

                <!-- Totals -->
                <div class="row justify-content-end">
                    <div class="col-md-6">
                        <table class="table table-sm table-borderless">
                            <tr>
                                <td class="text-end"><strong>Subtotal:</strong></td>
                                <td class="text-end" width="150">Rp ${invoice.totalAmount.toLocaleString('id-ID')}</td>
                            </tr>
                            <tr>
                                <td class="text-end"><strong>Pajak (10%):</strong></td>
                                <td class="text-end">Rp ${invoice.taxAmount.toLocaleString('id-ID')}</td>
                            </tr>
                            <tr class="border-top">
                                <td class="text-end"><h5 class="mb-0"><strong>TOTAL:</strong></h5></td>
                                <td class="text-end"><h5 class="mb-0 text-primary"><strong>Rp ${invoice.finalAmount.toLocaleString('id-ID')}</strong></h5></td>
                            </tr>
                        </table>
                    </div>
                </div>

                <div class="text-center mt-4 text-muted">
                    <p class="mb-0"><small>Terima kasih atas kunjungan Anda!</small></p>
                </div>
            </div>
        `;

        // Populate modal and show it
        const modalContent = document.getElementById('invoiceDetailsContent');
        if (modalContent) {
            modalContent.innerHTML = detailsHtml;
            const modal = new bootstrap.Modal(document.getElementById('invoiceDetailsModal'));
            modal.show();

            // Setup print button
            const printBtn = document.getElementById('printInvoiceBtn');
            if (printBtn) {
                printBtn.onclick = () => {
                    this.downloadInvoicePdf(invoiceId);
                };
            }
        }
    }

    async downloadInvoicePdf(invoiceId) {
        try {
            // Fetch invoice data
            const invoice = this.allInvoices ? this.allInvoices.find(inv => inv.id === invoiceId) : null;

            if (!invoice) {
                this.showToast('Invoice tidak ditemukan', 'error');
                return;
            }

            // Generate invoice HTML for printing
            this.generateInvoicePdf(invoice);

        } catch (error) {
            console.error('Error downloading invoice PDF:', error);
            this.showToast('Terjadi kesalahan saat mengunduh invoice', 'error');
        }
    }

    generateInvoicePdf(invoice) {
        const printWindow = window.open('', '_blank');

        const formattedDate = TimezoneUtils.formatDateTime(invoice.createdAt, {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });

        const paymentMethod = invoice.paymentMethod === 'CASH' ? 'Tunai' : 'QR Code';

        const orderItems = invoice.order && invoice.order.items ? invoice.order.items.map((item, index) => `
            <tr>
                <td>${index + 1}</td>
                <td>${item.menu ? item.menu.name : 'N/A'}</td>
                <td style="text-align: center;">${item.quantity}</td>
                <td style="text-align: right;">Rp ${item.price.toLocaleString('id-ID')}</td>
                <td style="text-align: right;">Rp ${item.subtotal.toLocaleString('id-ID')}</td>
            </tr>
        `).join('') : '<tr><td colspan="5" style="text-align: center;">No items</td></tr>';

        const htmlContent = `
            <!DOCTYPE html>
            <html>
            <head>
                <title>Invoice - ${invoice.invoiceNumber}</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: Arial, sans-serif;
                        padding: 40px;
                        background: white;
                    }
                    .invoice-header {
                        text-align: center;
                        margin-bottom: 30px;
                        border-bottom: 3px solid #A63939;
                        padding-bottom: 20px;
                    }
                    .invoice-header h1 {
                        color: #A63939;
                        font-size: 28px;
                        margin-bottom: 5px;
                    }
                    .invoice-header h2 {
                        color: #666;
                        font-size: 18px;
                        font-weight: normal;
                    }
                    .invoice-number {
                        text-align: center;
                        font-size: 20px;
                        font-weight: bold;
                        color: #A63939;
                        margin-bottom: 20px;
                    }
                    .invoice-info {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin-bottom: 30px;
                    }
                    .info-section {
                        background: #f8f9fa;
                        padding: 15px;
                        border-radius: 8px;
                    }
                    .info-section h3 {
                        color: #A63939;
                        font-size: 14px;
                        margin-bottom: 10px;
                        text-transform: uppercase;
                    }
                    .info-section p {
                        margin: 5px 0;
                        font-size: 13px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 20px;
                    }
                    th {
                        background: #A63939;
                        color: white;
                        padding: 12px;
                        text-align: left;
                        font-weight: bold;
                        font-size: 12px;
                    }
                    td {
                        padding: 10px 12px;
                        border-bottom: 1px solid #ddd;
                        font-size: 12px;
                    }
                    .total-section {
                        margin-top: 30px;
                        text-align: right;
                    }
                    .total-row {
                        display: flex;
                        justify-content: flex-end;
                        margin: 10px 0;
                        font-size: 14px;
                    }
                    .total-row.grand {
                        font-size: 18px;
                        font-weight: bold;
                        color: #A63939;
                        border-top: 2px solid #A63939;
                        padding-top: 10px;
                        margin-top: 15px;
                    }
                    .total-label {
                        width: 150px;
                        text-align: right;
                        padding-right: 20px;
                    }
                    .total-value {
                        width: 200px;
                        text-align: right;
                    }
                    .footer {
                        margin-top: 40px;
                        padding-top: 20px;
                        border-top: 2px solid #ddd;
                        text-align: center;
                        color: #666;
                        font-size: 12px;
                    }
                    @media print {
                        body {
                            padding: 20px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="invoice-header">
                    <h1>ChopChop Restaurant</h1>
                    <h2>Invoice</h2>
                </div>

                <div class="invoice-number">
                    ${invoice.invoiceNumber}
                </div>

                <div class="invoice-info">
                    <div class="info-section">
                        <h3>Informasi Pesanan</h3>
                        <p><strong>No. Pesanan:</strong> ${invoice.order ? invoice.order.orderNumber : 'N/A'}</p>
                        <p><strong>Customer:</strong> ${invoice.order ? invoice.order.customerName : 'N/A'}</p>
                        <p><strong>Tanggal:</strong> ${formattedDate}</p>
                    </div>
                    <div class="info-section">
                        <h3>Pembayaran</h3>
                        <p><strong>Metode:</strong> ${paymentMethod}</p>
                        <p><strong>Status:</strong> LUNAS</p>
                        <p><strong>Kasir:</strong> ${invoice.cashier ? invoice.cashier.displayName : 'N/A'}</p>
                    </div>
                </div>

                <h3 style="color: #A63939; margin-bottom: 10px;">Detail Pesanan</h3>
                <table>
                    <thead>
                        <tr>
                            <th style="width: 40px;">No</th>
                            <th>Item</th>
                            <th style="width: 80px; text-align: center;">Qty</th>
                            <th style="width: 120px; text-align: right;">Harga</th>
                            <th style="width: 120px; text-align: right;">Subtotal</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${orderItems}
                    </tbody>
                </table>

                <div class="total-section">
                    <div class="total-row">
                        <div class="total-label">Subtotal:</div>
                        <div class="total-value">Rp ${invoice.totalAmount.toLocaleString('id-ID')}</div>
                    </div>
                    <div class="total-row">
                        <div class="total-label">Pajak:</div>
                        <div class="total-value">Rp ${invoice.taxAmount.toLocaleString('id-ID')}</div>
                    </div>
                    <div class="total-row grand">
                        <div class="total-label">Total:</div>
                        <div class="total-value">Rp ${invoice.finalAmount.toLocaleString('id-ID')}</div>
                    </div>
                </div>

                <div class="footer">
                    <p>Terima kasih atas kunjungan Anda!</p>
                    <p>&copy; ${new Date().getFullYear()} ChopChop Restaurant. All rights reserved.</p>
                </div>

                <script>
                    window.onload = function() {
                        window.print();
                    }
                </script>
            </body>
            </html>
        `;

        printWindow.document.write(htmlContent);
        printWindow.document.close();
    }

    async generateMissingInvoices() {
        if (!confirm('Generate invoices untuk semua pesanan yang sudah dibayar tapi belum memiliki invoice?\n\nIni akan membuat invoice untuk pesanan lama yang belum memiliki invoice.')) {
            return;
        }

        try {
            this.showToast('Menghasilkan invoice yang hilang...', 'info');

            const response = await fetch('/cashier/api/invoices/generate-missing', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [this.csrfHeader]: this.csrfToken
                }
            });

            const result = await response.json();

            if (result.success) {
                const data = result.data;
                this.showToast(
                    `Berhasil! ${data.invoicesCreated} invoice baru dibuat, ${data.invoicesSkipped} sudah ada. Total pesanan terbayar: ${data.totalPaidOrders}`,
                    'success'
                );

                // Reload the report to show new invoices
                this.generateReport();
            } else {
                this.showToast('Gagal generate invoice: ' + result.message, 'error');
            }
        } catch (error) {
            console.error('Error generating missing invoices:', error);
            this.showToast('Terjadi kesalahan saat generate invoice', 'error');
        }
    }

    async exportReport() {
        try {
            const startDate = document.getElementById('startDate').value;
            const endDate = document.getElementById('endDate').value;

            if (!startDate || !endDate) {
                this.showToast('Pilih tanggal mulai dan akhir terlebih dahulu', 'error');
                return;
            }

            this.showToast('Menghasilkan PDF laporan...', 'info');

            // Get current report data
            const totalRevenue = document.getElementById('totalRevenue')?.textContent || 'Rp 0';
            const totalOrders = document.getElementById('totalOrders')?.textContent || '0';

            // Fetch invoices for the date range
            const response = await fetch(`/cashier/api/invoices/by-date?startDate=${startDate}&endDate=${endDate}`);
            const data = await response.json();

            if (!data.success) {
                this.showToast('Gagal mengambil data invoice', 'error');
                return;
            }

            const invoices = data.data || [];

            // Generate PDF content
            this.generateReportPdf(startDate, endDate, totalRevenue, totalOrders, invoices);

        } catch (error) {
            console.error('Error exporting report:', error);
            this.showToast('Gagal export laporan', 'error');
        }
    }

    generateReportPdf(startDate, endDate, totalRevenue, totalOrders, invoices) {
        // Create a printable HTML version
        const printWindow = window.open('', '_blank');

        const invoiceRows = invoices.map((invoice, index) => {
            const formattedDate = TimezoneUtils.formatDateTime(invoice.createdAt, {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });

            const paymentMethod = invoice.paymentMethod === 'CASH' ? 'Tunai' : 'QR Code';

            return `
                <tr>
                    <td>${index + 1}</td>
                    <td>${invoice.invoiceNumber}</td>
                    <td>${invoice.order ? invoice.order.orderNumber : '-'}</td>
                    <td>${invoice.order ? invoice.order.customerName : '-'}</td>
                    <td>${paymentMethod}</td>
                    <td style="text-align: right;">Rp ${invoice.finalAmount.toLocaleString('id-ID')}</td>
                    <td>${formattedDate}</td>
                </tr>
            `;
        }).join('');

        const htmlContent = `
            <!DOCTYPE html>
            <html>
            <head>
                <title>Laporan Penjualan - ChopChop Restaurant</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: Arial, sans-serif;
                        padding: 40px;
                        background: white;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        border-bottom: 3px solid #A63939;
                        padding-bottom: 20px;
                    }
                    .header h1 {
                        color: #A63939;
                        font-size: 28px;
                        margin-bottom: 5px;
                    }
                    .header h2 {
                        color: #666;
                        font-size: 18px;
                        font-weight: normal;
                    }
                    .period {
                        text-align: center;
                        margin-bottom: 30px;
                        color: #333;
                        font-size: 14px;
                    }
                    .summary {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin-bottom: 40px;
                    }
                    .summary-card {
                        background: #f8f9fa;
                        border: 2px solid #A63939;
                        border-radius: 8px;
                        padding: 20px;
                        text-align: center;
                    }
                    .summary-card h3 {
                        color: #666;
                        font-size: 14px;
                        margin-bottom: 10px;
                        text-transform: uppercase;
                    }
                    .summary-card .value {
                        color: #A63939;
                        font-size: 24px;
                        font-weight: bold;
                    }
                    .invoice-section h3 {
                        color: #A63939;
                        margin-bottom: 15px;
                        font-size: 18px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 10px;
                    }
                    th {
                        background: #A63939;
                        color: white;
                        padding: 12px;
                        text-align: left;
                        font-weight: bold;
                        font-size: 12px;
                    }
                    td {
                        padding: 10px 12px;
                        border-bottom: 1px solid #ddd;
                        font-size: 11px;
                    }
                    tr:hover {
                        background: #f8f9fa;
                    }
                    .footer {
                        margin-top: 40px;
                        padding-top: 20px;
                        border-top: 2px solid #ddd;
                        text-align: center;
                        color: #666;
                        font-size: 12px;
                    }
                    .print-date {
                        text-align: right;
                        color: #666;
                        font-size: 11px;
                        margin-top: 20px;
                    }
                    @media print {
                        body {
                            padding: 20px;
                        }
                        .no-print {
                            display: none;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>ChopChop Restaurant</h1>
                    <h2>Laporan Penjualan</h2>
                </div>

                <div class="period">
                    <strong>Periode:</strong> ${startDate} sampai ${endDate}
                </div>

                <div class="summary">
                    <div class="summary-card">
                        <h3>Total Pendapatan</h3>
                        <div class="value">${totalRevenue}</div>
                    </div>
                    <div class="summary-card">
                        <h3>Total Pesanan</h3>
                        <div class="value">${totalOrders}</div>
                    </div>
                </div>

                <div class="invoice-section">
                    <h3>Daftar Invoice (${invoices.length} Invoice)</h3>
                    <table>
                        <thead>
                            <tr>
                                <th style="width: 40px;">No</th>
                                <th>No. Invoice</th>
                                <th>No. Pesanan</th>
                                <th>Customer</th>
                                <th>Pembayaran</th>
                                <th style="text-align: right;">Total</th>
                                <th>Tanggal</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${invoiceRows.length > 0 ? invoiceRows : '<tr><td colspan="7" style="text-align: center; padding: 20px;">Tidak ada invoice untuk periode ini</td></tr>'}
                        </tbody>
                    </table>
                </div>

                <div class="print-date">
                    Dicetak pada: ${TimezoneUtils.formatDateTime(TimezoneUtils.now())}
                </div>

                <div class="footer">
                    <p>&copy; ${new Date().getFullYear()} ChopChop Restaurant. All rights reserved.</p>
                </div>

                <script>
                    window.onload = function() {
                        window.print();
                    }
                </script>
            </body>
            </html>
        `;

        printWindow.document.write(htmlContent);
        printWindow.document.close();
    }

    showToast(message, type = 'info') {
        // Use the WebSocket client's notification system if available
        if (window.webSocketClient && window.webSocketClient.showNotification) {
            window.webSocketClient.showNotification(message, type);
            return;
        }

        // Create Bootstrap toast
        const toastContainer = this.getOrCreateToastContainer();

        const toastId = 'toast-' + Date.now();
        const bgClass = this.getToastBgClass(type);
        const icon = this.getToastIcon(type);

        const toastHTML = `
            <div id="${toastId}" class="toast align-items-center text-white ${bgClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        <i class="fas ${icon} me-2"></i>${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;

        toastContainer.insertAdjacentHTML('beforeend', toastHTML);

        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, {
            autohide: true,
            delay: 3000
        });

        toast.show();

        // Remove toast element after it's hidden
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }

    getOrCreateToastContainer() {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container position-fixed top-0 end-0 p-3';
            container.style.zIndex = '9999';
            document.body.appendChild(container);
        }
        return container;
    }

    getToastBgClass(type) {
        const bgClasses = {
            'success': 'bg-success',
            'error': 'bg-danger',
            'warning': 'bg-warning',
            'info': 'bg-info'
        };
        return bgClasses[type] || 'bg-info';
    }

    getToastIcon(type) {
        const icons = {
            'success': 'fa-check-circle',
            'error': 'fa-exclamation-circle',
            'warning': 'fa-exclamation-triangle',
            'info': 'fa-info-circle'
        };
        return icons[type] || 'fa-info-circle';
    }

    // Menu management functions
    async toggleMenuAvailability(menuId) {
        try {
            const response = await fetch(`/cashier/api/menus/${menuId}/availability`, {
                method: 'PUT',
                headers: {
                    ...this.getCsrfHeaders()
                }
            });
            const data = await response.json();

            if (data.success) {
                this.showToast('Status menu berhasil diupdate', 'success');
                // Reload the page to show updated status
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                this.showToast('Gagal update menu: ' + data.message, 'error');
            }
        } catch (error) {
            console.error('Error toggling menu availability:', error);
            this.showToast('Terjadi kesalahan', 'error');
        }
    }

    async editMenu(menuId) {
        try {
            // Fetch all menus and categories
            const [menusResponse, categoriesResponse] = await Promise.all([
                fetch('/customer/api/menus'),
                fetch('/cashier/api/categories')
            ]);

            const menusData = await menusResponse.json();
            const categoriesData = await categoriesResponse.json();

            if (menusData.success && menusData.data) {
                const menu = menusData.data.find(m => m.id === menuId);
                if (menu) {
                    const categories = categoriesData.success ? categoriesData.data : [];
                    this.showEditMenuModal(menu, categories);
                } else {
                    this.showToast('Menu tidak ditemukan', 'error');
                }
            }
        } catch (error) {
            console.error('Error loading menu:', error);
            this.showToast('Gagal memuat data menu', 'error');
        }
    }

    showEditMenuModal(menu, categories) {
        // Create modal dynamically if it doesn't exist
        let modal = document.getElementById('editMenuModal');
        if (!modal) {
            const modalHTML = `
                <div class="modal fade" id="editMenuModal" tabindex="-1">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">Edit Menu</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <input type="hidden" id="editMenuId">
                                <div class="mb-3">
                                    <label class="form-label">Nama Menu</label>
                                    <input type="text" class="form-control" id="editMenuName" required>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Kategori</label>
                                    <select class="form-select" id="editMenuCategoryId" required>
                                        <option value="">Pilih Kategori</option>
                                    </select>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Deskripsi</label>
                                    <textarea class="form-control" id="editMenuDescription" rows="3"></textarea>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">URL Gambar</label>
                                    <input type="text" class="form-control" id="editMenuImageUrl" placeholder="/images/menu/...">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Harga Normal</label>
                                    <input type="number" class="form-control" id="editMenuPrice" required>
                                </div>
                                <div class="mb-3">
                                    <div class="form-check">
                                        <input class="form-check-input" type="checkbox" id="editMenuIsPromo">
                                        <label class="form-check-label" for="editMenuIsPromo">
                                            Menu Promo
                                        </label>
                                    </div>
                                </div>
                                <div class="mb-3" id="editPromoSection" style="display: none;">
                                    <label class="form-label">Harga Promo</label>
                                    <input type="number" class="form-control" id="editMenuPromoPrice">
                                </div>
                                <div class="mb-3">
                                    <div class="form-check">
                                        <input class="form-check-input" type="checkbox" id="editMenuAvailable">
                                        <label class="form-check-label" for="editMenuAvailable">
                                            Tersedia
                                        </label>
                                    </div>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Batal</button>
                                <button type="button" class="btn btn-primary" id="saveMenuBtn">Simpan</button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            document.body.insertAdjacentHTML('beforeend', modalHTML);
            modal = document.getElementById('editMenuModal');

            // Add event listener for promo checkbox
            document.getElementById('editMenuIsPromo').addEventListener('change', (e) => {
                document.getElementById('editPromoSection').style.display = e.target.checked ? 'block' : 'none';
            });

            // Add event listener for save button
            document.getElementById('saveMenuBtn').addEventListener('click', () => {
                this.saveMenuChanges();
            });
        }

        // Populate category dropdown
        const categorySelect = document.getElementById('editMenuCategoryId');
        categorySelect.innerHTML = '<option value="">Pilih Kategori</option>';
        categories.forEach(cat => {
            const option = document.createElement('option');
            option.value = cat.id;
            option.textContent = cat.name;
            categorySelect.appendChild(option);
        });

        // Populate modal with menu data
        document.getElementById('editMenuId').value = menu.id;
        document.getElementById('editMenuName').value = menu.name;
        document.getElementById('editMenuCategoryId').value = menu.category ? menu.category.id : '';
        document.getElementById('editMenuDescription').value = menu.description || '';
        document.getElementById('editMenuImageUrl').value = menu.imageUrl || '';
        document.getElementById('editMenuPrice').value = menu.price;
        document.getElementById('editMenuIsPromo').checked = menu.isPromo || false;
        document.getElementById('editMenuPromoPrice').value = menu.promoPrice || '';
        document.getElementById('editMenuAvailable').checked = menu.available;

        // Show/hide promo section
        document.getElementById('editPromoSection').style.display = menu.isPromo ? 'block' : 'none';

        // Show modal
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();
    }

    async saveMenuChanges() {
        const menuId = document.getElementById('editMenuId').value;
        const categoryId = document.getElementById('editMenuCategoryId').value;

        if (!categoryId) {
            this.showToast('Pilih kategori terlebih dahulu', 'error');
            return;
        }

        const menuData = {
            name: document.getElementById('editMenuName').value,
            description: document.getElementById('editMenuDescription').value,
            imageUrl: document.getElementById('editMenuImageUrl').value || null,
            categoryId: parseInt(categoryId),
            price: parseFloat(document.getElementById('editMenuPrice').value),
            isPromo: document.getElementById('editMenuIsPromo').checked,
            promoPrice: document.getElementById('editMenuIsPromo').checked ?
                parseFloat(document.getElementById('editMenuPromoPrice').value || 0) : null,
            available: document.getElementById('editMenuAvailable').checked
        };

        try {
            const response = await fetch(`/cashier/api/menus/${menuId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    ...this.getCsrfHeaders()
                },
                body: JSON.stringify(menuData)
            });

            const data = await response.json();

            if (data.success) {
                this.showToast('Menu berhasil diupdate', 'success');

                
                const modal = bootstrap.Modal.getInstance(document.getElementById('editMenuModal'));
                if (modal) modal.hide();

                // Reload page
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                this.showToast('Gagal update menu: ' + data.message, 'error');
            }
        } catch (error) {
            console.error('Error updating menu:', error);
            this.showToast('Terjadi kesalahan saat update menu', 'error');
        }
    }

    async createNewMenu() {
        // Validate form
        const name = document.getElementById('addMenuName').value.trim();
        const categoryId = document.getElementById('addMenuCategory').value;
        const price = parseFloat(document.getElementById('addMenuPrice').value);

        if (!name || !categoryId || !price || price <= 0) {
            this.showToast('Mohon isi semua field yang wajib diisi', 'error');
            return;
        }

        const menuData = {
            name: name,
            description: document.getElementById('addMenuDescription').value.trim() || null,
            price: price,
            categoryId: parseInt(categoryId),
            imageUrl: document.getElementById('addMenuImageUrl').value.trim() || null,
            available: document.getElementById('addMenuAvailable').checked,
            isPromo: document.getElementById('addMenuIsPromo').checked,
            promoPrice: document.getElementById('addMenuIsPromo').checked ?
                parseFloat(document.getElementById('addMenuPromoPrice').value) || null : null
        };

        try {
            const response = await fetch('/cashier/api/menus', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...this.getCsrfHeaders()
                },
                body: JSON.stringify(menuData)
            });

            const data = await response.json();

            if (data.success) {
                this.showToast('Menu berhasil ditambahkan!', 'success');

                // Close modal
                const modal = bootstrap.Modal.getInstance(document.getElementById('addMenuModal'));
                modal.hide();

                // Reset form
                document.getElementById('addMenuForm').reset();

                // Reload page to show new menu
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                this.showToast('Gagal menambah menu: ' + data.message, 'error');
            }
        } catch (error) {
            console.error('Error creating menu:', error);
            this.showToast('Terjadi kesalahan saat menambah menu', 'error');
        }
    }

    async deleteMenu(menuId, menuName) {
        if (!confirm(`Apakah Anda yakin ingin menghapus menu "${menuName}"?\n\nPeringatan: Tindakan ini akan tercatat dalam audit log dan tidak dapat dibatalkan.`)) {
            return;
        }

        try {
            const response = await fetch(`/cashier/api/menus/${menuId}`, {
                method: 'DELETE',
                headers: {
                    ...this.getCsrfHeaders()
                }
            });

            const data = await response.json();

            if (data.success) {
                this.showToast('Menu berhasil dihapus', 'success');
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                this.showToast('Gagal menghapus menu: ' + data.message, 'error');
            }
        } catch (error) {
            console.error('Error deleting menu:', error);
            this.showToast('Terjadi kesalahan saat menghapus menu', 'error');
        }
    }

    setupMenuFilters() {
        const categoryFilter = document.getElementById('categoryFilterMenu');
        const statusFilter = document.getElementById('statusFilterMenu');
        const promoFilter = document.getElementById('promoFilterMenu');
        const resetBtn = document.getElementById('resetFiltersBtn');

        if (!categoryFilter) return; // Not on settings page

        const applyFilters = () => {
            const categoryValue = categoryFilter.value.toLowerCase();
            const statusValue = statusFilter.value;
            const promoValue = promoFilter.value;

            const rows = document.querySelectorAll('#menusTableBody tr');

            rows.forEach(row => {
                const category = row.dataset.category?.toLowerCase() || '';
                const available = row.dataset.available === 'true';
                const isPromo = row.dataset.promo === 'true';

                let showRow = true;

                // Apply category filter
                if (categoryValue && category !== categoryValue) {
                    showRow = false;
                }

                // Apply status filter
                if (statusValue === 'available' && !available) {
                    showRow = false;
                } else if (statusValue === 'unavailable' && available) {
                    showRow = false;
                }

                // Apply promo filter
                if (promoValue === 'promo' && !isPromo) {
                    showRow = false;
                } else if (promoValue === 'normal' && isPromo) {
                    showRow = false;
                }

                row.style.display = showRow ? '' : 'none';
            });
        };

        categoryFilter.addEventListener('change', applyFilters);
        statusFilter.addEventListener('change', applyFilters);
        promoFilter.addEventListener('change', applyFilters);

        if (resetBtn) {
            resetBtn.addEventListener('click', () => {
                categoryFilter.value = '';
                statusFilter.value = '';
                promoFilter.value = '';
                applyFilters();
            });
        }

        // Setup event delegation for menu action buttons
        document.addEventListener('click', (e) => {
            // Handle delete menu button
            const deleteBtn = e.target.closest('.delete-menu-btn');
            if (deleteBtn) {
                const menuId = parseInt(deleteBtn.dataset.menuId);
                const menuName = deleteBtn.dataset.menuName;
                this.deleteMenu(menuId, menuName);
                return;
            }

            // Handle edit menu button
            const editBtn = e.target.closest('.edit-menu-btn');
            if (editBtn) {
                const menuId = parseInt(editBtn.dataset.menuId);
                this.editMenu(menuId);
                return;
            }

            // Handle toggle availability button
            const toggleBtn = e.target.closest('.toggle-availability-btn');
            if (toggleBtn) {
                const menuId = parseInt(toggleBtn.dataset.menuId);
                this.toggleMenuAvailability(menuId);
                return;
            }
        });
    }

    async deleteCategory(categoryId) {
        if (!confirm('Apakah Anda yakin ingin menghapus kategori ini?')) {
            return;
        }

        try {
            const response = await fetch(`/cashier/api/categories/${categoryId}`, {
                method: 'DELETE',
                headers: {
                    ...this.getCsrfHeaders()
                }
            });

            const data = await response.json();

            if (data.success) {
                this.showToast('Kategori berhasil dihapus', 'success');
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                this.showToast('Gagal hapus kategori: ' + data.message, 'error');
            }
        } catch (error) {
            console.error('Error deleting category:', error);
            this.showToast('Terjadi kesalahan', 'error');
        }
    }

}

// Initialize cashier app
const cashierApp = new CashierApp();
