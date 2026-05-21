package com.scau.campusstudyroomreservationmanagementsystem.config;

import com.scau.campusstudyroomreservationmanagementsystem.support.CurrentUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expireHours;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expire-hours}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireHours = expireHours;
    }

    public String createToken(CurrentUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.username())
                .claims(Map.of(
                        "userId", user.id(),
                        "role", user.role(),
                        "displayName", user.displayName()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireHours * 3600)))
                .signWith(key)
                .compact();
    }

    public CurrentUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Long userId = ((Number) claims.get("userId")).longValue();
        String role = String.valueOf(claims.get("role"));
        String displayName = String.valueOf(claims.get("displayName"));
        return new CurrentUser(userId, claims.getSubject(), role, displayName);
    }
}
