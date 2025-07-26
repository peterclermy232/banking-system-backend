package com.sacco.banking.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationInMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())  // Note: using .subject() instead of .setSubject()
                .issuedAt(new Date())                   // Note: using .issuedAt() instead of .setIssuedAt()
                .expiration(expiryDate)                 // Note: using .expiration() instead of .setExpiration()
                .signWith(getSigningKey())
                .compact();
    }

    public String getMemberNumberFromToken(String token) {
        Claims claims = Jwts.parser()              // Note: JJWT 0.12.x uses parser(), not parserBuilder()
                .verifyWith(getSigningKey())       // Note: using .verifyWith() instead of .setSigningKey()
                .build()
                .parseSignedClaims(token)          // Note: using .parseSignedClaims() instead of .parseClaimsJws()
                .getPayload();                     // Note: using .getPayload() instead of .getBody()

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
}