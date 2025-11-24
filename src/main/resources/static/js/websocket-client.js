    class WebSocketClient {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectInterval = 3000; // 3 seconds
    }

    connect() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect({}, (frame) => {
            console.log('Connected: ' + frame);
            this.connected = true;
            this.reconnectAttempts = 0;

            this.subscribeToOrders();
            this.onConnected();
        }, (error) => {
            console.log('WebSocket connection error: ', error);
            this.connected = false;
            this.handleDisconnect();
        });
    }

    subscribeToOrders() {
        if (this.stompClient && this.connected) {
            this.stompClient.subscribe('/topic/orders', (message) => {
                const orderUpdate = JSON.parse(message.body);
                this.handleOrderUpdate(orderUpdate);
            });

            this.stompClient.subscribe('/topic/dashboard', (message) => {
                console.log('Dashboard update received:', message.body);
                this.handleDashboardUpdate();
            });

            this.stompClient.subscribe('/topic/notifications', (message) => {
                const notification = JSON.parse(message.body);
                this.handleNotification(notification);
            });
        }
    }

    handleDashboardUpdate() {
        if (window.cashierApp && typeof window.cashierApp.loadDashboardData === 'function') {
            console.log('Refreshing dashboard data...');
            window.cashierApp.loadDashboardData();
        }

        if (window.cashierApp && typeof window.cashierApp.loadOrdersPage === 'function') {
            const ordersTable = document.getElementById('ordersTableBody');
            if (ordersTable) {
                console.log('Refreshing orders data...');
                window.cashierApp.loadOrdersPage();
            }
        }
    }

    handleOrderUpdate(orderUpdate) {
        if (typeof window.orderUpdateHandler === 'function') {
            window.orderUpdateHandler(orderUpdate);
        }

        this.showNotification(`Order ${orderUpdate.orderNumber} updated: ${orderUpdate.status}`);
    }

    handleNotification(notification) {
        this.showNotification(notification.message, notification.type);
    }

    showNotification(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
        toast.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        toast.innerHTML = `
            <strong>${type.toUpperCase()}</strong><br>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        document.body.appendChild(toast);

        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 5000);
    }

    handleDisconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

            setTimeout(() => {
                this.connect();
            }, this.reconnectInterval);
        } else {
            console.error('Max reconnection attempts reached');
            this.showNotification('Connection lost. Please refresh the page.', 'danger');
        }
    }

    onConnected() {
        console.log('WebSocket connected successfully');
    }

    disconnect() {
        if (this.stompClient !== null) {
            this.stompClient.disconnect();
        }
        this.connected = false;
        console.log("WebSocket disconnected");
    }

    sendOrderUpdate(order) {
        if (this.stompClient && this.connected) {
            this.stompClient.send("/app/order/update", {}, JSON.stringify(order));
        }
    }

    sendNotification(message, type = 'info') {
        if (this.stompClient && this.connected) {
            const notification = {
                message: message,
                type: type,
                timestamp: new Date().toISOString()
            };
            this.stompClient.send("/app/notification", {}, JSON.stringify(notification));
        }
    }
}

// Initialize WebSocket client
const webSocketClient = new WebSocketClient();

// Connect when page loads
document.addEventListener('DOMContentLoaded', function() {
    webSocketClient.connect();
});

// Disconnect when page unloads
window.addEventListener('beforeunload', function() {
    webSocketClient.disconnect();
});