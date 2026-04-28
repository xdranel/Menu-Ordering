package menuorderingapp.project.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import menuorderingapp.project.model.Cashier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-hours:8}")
    private int expirationHours;

    public String generateToken(Cashier cashier) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (long) expirationHours * 3600 * 1000);

        return Jwts.builder()
                .subject(cashier.getUsername())
                .claim("cashierId", cashier.getId())
                .claim("displayName", cashier.getDisplayName())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
