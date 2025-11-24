package menuorderingapp.project.controller;

import menuorderingapp.project.model.Cashier;
import menuorderingapp.project.model.dto.*;
import menuorderingapp.project.service.AuthService;
import menuorderingapp.project.service.CashierService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class AuthController extends BaseController {

    private final AuthService authService;
    private final CashierService cashierService;

    public AuthController(AuthService authService, CashierService cashierService) {
        this.authService = authService;
        this.cashierService = cashierService;
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
                               Model model,
                               HttpSession session) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please fill in all required fields");
            return "auth/login";
        }

        try {
            String sessionToken = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
            Cashier cashier = authService.getCashierFromSession(sessionToken);

            session.setAttribute("sessionToken", sessionToken);
            session.setAttribute("cashier", convertToCashierDto(cashier));
            session.setAttribute("cashierId", cashier.getId());

            return "redirect:/cashier/dashboard";

        } catch (Exception e) {
            model.addAttribute("error", "Invalid username or password");
            model.addAttribute("loginRequest", loginRequest);
            return "auth/login";
        }
    }

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<ApiResponse<LoginResponse>> apiLogin(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            String sessionToken = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
            Cashier cashier = authService.getCashierFromSession(sessionToken);

            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setSessionToken(sessionToken);
            loginResponse.setCashier(convertToCashierDto(cashier));
            loginResponse.setMessage("Login successful");

            return success(loginResponse);

        } catch (Exception e) {
            return unauthorized("Invalid credentials");
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        String sessionToken = (String) session.getAttribute("sessionToken");
        if (sessionToken != null) {
            authService.logout(sessionToken);
        }
        session.invalidate();
        return "redirect:/auth/login";
    }

    @PostMapping("/api/logout")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> apiLogout(HttpServletRequest request) {
        String sessionToken = request.getHeader("X-Session-Token");
        if (sessionToken != null) {
            authService.logout(sessionToken);
        }
        return success("Logged out successfully", null);
    }

    @GetMapping("/api/validate")
    @ResponseBody
    public ResponseEntity<ApiResponse<CashierDto>> validateSession(HttpServletRequest request) {
        String sessionToken = request.getHeader("X-Session-Token");
        if (sessionToken == null) {
            return unauthorized("No session token");
        }

        try {
            Cashier cashier = authService.getCashierFromSession(sessionToken);
            return success(convertToCashierDto(cashier));
        } catch (Exception e) {
            return unauthorized("Invalid session");
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
