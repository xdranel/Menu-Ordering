package menuorderingapp.project.controller;

import menuorderingapp.project.model.Cashier;
import menuorderingapp.project.model.dto.*;
import menuorderingapp.project.service.AuthService;
import menuorderingapp.project.service.CashierService;
import menuorderingapp.project.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class AuthController extends BaseController {

    private final AuthService authService;
    private final CashierService cashierService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, CashierService cashierService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.cashierService = cashierService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    @RequestMapping("/access-denied")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> accessDenied() {
        return error(org.springframework.http.HttpStatus.FORBIDDEN,
            "Access denied: CSRF token validation failed. Please refresh the page.");
    }

    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute LoginRequest loginRequest,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please fill in all required fields");
            return "auth/login";
        }
        // Spring Security's UsernamePasswordAuthenticationFilter intercepts this URL first.
        model.addAttribute("error", "Invalid username or password");
        model.addAttribute("loginRequest", loginRequest);
        return "auth/login";
    }

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<ApiResponse<LoginResponse>> apiLogin(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            String token = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
            Cashier cashier = authService.getCashierFromSession(token);

            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setSessionToken(token);
            loginResponse.setCashier(convertToCashierDto(cashier));
            loginResponse.setMessage("Login successful");

            return success(loginResponse);

        } catch (Exception e) {
            return unauthorized("Invalid credentials");
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login";
    }

    @PostMapping("/api/logout")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> apiLogout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return success("Logged out successfully", null);
    }

    @GetMapping("/api/validate")
    @ResponseBody
    public ResponseEntity<ApiResponse<CashierDto>> validateSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized("No token provided");
        }

        String token = authHeader.substring(7);
        try {
            Cashier cashier = authService.getCashierFromSession(token);
            return success(convertToCashierDto(cashier));
        } catch (Exception e) {
            return unauthorized("Invalid or expired token");
        }
    }

    private CashierDto convertToCashierDto(Cashier cashier) {
        CashierDto dto = new CashierDto();
        dto.setId(cashier.getId());
        dto.setUsername(cashier.getUsername());
        dto.setDisplayName(cashier.getDisplayName());
        dto.setRole(cashier.getRole());
        dto.setIsActive(cashier.getIsActive());
        dto.setLastLogin(cashier.getLastLogin());
        dto.setCreatedAt(cashier.getCreatedAt());
        return dto;
    }
}
