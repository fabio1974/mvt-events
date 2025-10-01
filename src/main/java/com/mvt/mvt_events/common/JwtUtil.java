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
            claims.put("email", user.getUsername()); // username is the email

            // Add personal information
            if (user.getDateOfBirth() != null) {
                claims.put("dateOfBirth", user.getDateOfBirth().toString());
            }
            if (user.getGender() != null) {
                claims.put("gender", user.getGender().name());
            }
            if (user.getCity() != null) {
                claims.put("city", user.getCity());
            }
            if (user.getState() != null) {
                claims.put("state", user.getState());
            }
            if (user.getDocumentNumber() != null) {
                claims.put("cpf", user.getDocumentNumber());
            }
            if (user.getPhone() != null) {
                claims.put("phone", user.getPhone());
            }
            if (user.getAddress() != null) {
                claims.put("address", user.getAddress());
            }
            if (user.getCountry() != null) {
                claims.put("country", user.getCountry());
            }

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

    // Extract email from token
    public String getEmailFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    // Extract date of birth from token
    public String getDateOfBirthFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("dateOfBirth", String.class);
    }

    // Extract gender from token
    public String getGenderFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("gender", String.class);
    }

    // Extract city from token
    public String getCityFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("city", String.class);
    }

    // Extract state from token
    public String getStateFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("state", String.class);
    }

    // Extract CPF from token
    public String getCpfFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("cpf", String.class);
    }

    // Extract phone from token
    public String getPhoneFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("phone", String.class);
    }

    // Extract address from token
    public String getAddressFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("address", String.class);
    }

    // Extract country from token
    public String getCountryFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("country", String.class);
    }

    // Get all user data from token as a Map
    public Map<String, Object> getUserDataFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Map<String, Object> userData = new HashMap<>();

        userData.put("userId", claims.get("userId"));
        userData.put("username", claims.getSubject());
        userData.put("email", claims.get("email"));
        userData.put("name", claims.get("name"));
        userData.put("role", claims.get("role"));
        userData.put("dateOfBirth", claims.get("dateOfBirth"));
        userData.put("gender", claims.get("gender"));
        userData.put("city", claims.get("city"));
        userData.put("state", claims.get("state"));
        userData.put("cpf", claims.get("cpf"));
        userData.put("phone", claims.get("phone"));
        userData.put("address", claims.get("address"));
        userData.put("country", claims.get("country"));
        userData.put("organizationId", claims.get("organizationId"));

        return userData;
    }
}