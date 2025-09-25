package com.mvt.mvt_events.common;

import com.mvt.mvt_events.jpa.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:mvt-events-secret-key-for-jwt-authentication-very-long-secret-key-256-bits}")
    private String jwtSecret;

    @Value("${jwt.expiration:18000}") // 5 hours in seconds
    private int jwtTokenValidity;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Retrieve username from jwt token
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // Retrieve expiration date from jwt token
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // For retrieving any information from token we will need the secret key
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Check if the token has expired
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // Generate token for user
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Add authorities/roles to the token
        List<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .toList();
        claims.put("authorities", authorities);

        // Add additional user information if userDetails is a User instance
        if (userDetails instanceof User user) {
            claims.put("userId", user.getId().toString());
            claims.put("name", user.getName());
            claims.put("role", user.getRole().name());

            // Add organization_id for ORGANIZER users
            if (user.getOrganization() != null) {
                claims.put("organizationId", user.getOrganization().getId());
            }
        }

        return createToken(claims, userDetails.getUsername());
    }

    // While creating the token -
    // 1. Define claims of the token, like Issuer, Expiration, Subject, and the ID
    // 2. Sign the JWT using the HS512 algorithm and secret key.
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtTokenValidity * 1000L))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // Validate token
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Extract authorities from token
    @SuppressWarnings("unchecked")
    public List<String> getAuthoritiesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("authorities", List.class);
    }

    // Extract user ID from token
    public String getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", String.class);
    }

    // Extract name from token
    public String getNameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("name", String.class);
    }

    // Extract role from token
    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    // Extract organization ID from token
    public Long getOrganizationIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object orgId = claims.get("organizationId");
        return orgId != null ? Long.valueOf(orgId.toString()) : null;
    }

    // Check if user has organization
    public boolean hasOrganization(String token) {
        return getOrganizationIdFromToken(token) != null;
    }
}