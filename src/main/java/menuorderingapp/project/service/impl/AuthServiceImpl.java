package menuorderingapp.project.service.impl;

import menuorderingapp.project.model.Cashier;
import menuorderingapp.project.repository.CashierRepository;
import menuorderingapp.project.repository.CashierSessionRepository;
import menuorderingapp.project.security.CashierUserDetails;
import menuorderingapp.project.service.AuthService;
import menuorderingapp.project.service.CashierService;
import menuorderingapp.project.util.JwtUtil;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final CashierService cashierService;
    private final CashierRepository cashierRepository;
    private final CashierSessionRepository sessionRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(CashierService cashierService,
            CashierRepository cashierRepository,
            CashierSessionRepository sessionRepository,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil) {
        this.cashierService = cashierService;
        this.cashierRepository = cashierRepository;
        this.sessionRepository = sessionRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String login(String username, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            CashierUserDetails userDetails = (CashierUserDetails) authentication.getPrincipal();
            Cashier cashier = userDetails.getCashier();

            cashierService.updateLastLogin(cashier.getId());

            return jwtUtil.generateToken(cashier);

        } catch (Exception e) {
            throw new RuntimeException("Invalid username or password");
        }
    }

    @Override
    public void logout(String token) {
        SecurityContextHolder.clearContext();
        // JWT is stateless — client discards the token; nothing to invalidate server-side
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateSession(String token) {
        return jwtUtil.validateToken(token);
    }

    @Override
    @Transactional(readOnly = true)
    public Cashier getCashierFromSession(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }
        String username = jwtUtil.extractUsername(token);
        return cashierRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Cashier not found"));
    }

    @Override
    public void cleanupExpiredSessions() {
        sessionRepository.deleteExpiredSessions(LocalDateTime.now());
    }
}
