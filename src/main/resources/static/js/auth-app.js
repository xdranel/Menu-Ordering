class AuthApp {
    constructor() {
        this.init();
    }

    init() {
        this.setupEventListeners();
    }

    setupEventListeners() {
        const loginForm = document.getElementById('loginForm');
        if (loginForm) {
            // Let the form submit normally via Spring Security form login.
            // auth-app.js only handles UI concerns: password toggle, validation feedback.
            loginForm.addEventListener('submit', (e) => this.handleSubmit(e));
        }

        const togglePassword = document.getElementById('togglePassword');
        if (togglePassword) {
            togglePassword.addEventListener('click', () => this.togglePasswordVisibility());
        }
    }

    handleSubmit(event) {
        const form = event.target;
        const username = form.querySelector('[name="username"]')?.value?.trim();
        const password = form.querySelector('[name="password"]')?.value;

        if (!username || !password) {
            event.preventDefault();
            this.showError('Please enter both username and password');
        }
        // Otherwise let the form submit normally — Spring Security handles auth
    }

    togglePasswordVisibility() {
        const passwordInput = document.getElementById('password');
        const toggleIcon = document.getElementById('togglePassword');

        if (passwordInput.type === 'password') {
            passwordInput.type = 'text';
            toggleIcon.className = 'fas fa-eye-slash';
        } else {
            passwordInput.type = 'password';
            toggleIcon.className = 'fas fa-eye';
        }
    }

    showError(message) {
        this.showMessage(message, 'error');
    }

    showMessage(message, type) {
        document.querySelectorAll('.alert-dismissible').forEach(alert => alert.remove());

        const alert = document.createElement('div');
        alert.className = `alert alert-${type === 'error' ? 'danger' : 'success'} alert-dismissible fade show`;
        alert.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        const form = document.getElementById('loginForm');
        if (form) {
            form.insertBefore(alert, form.firstChild);
        }

        setTimeout(() => {
            if (alert.parentNode) alert.remove();
        }, 5000);
    }
}

const authApp = new AuthApp();
