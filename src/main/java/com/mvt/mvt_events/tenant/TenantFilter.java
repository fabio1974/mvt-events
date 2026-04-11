package com.mvt.mvt_events.tenant;

import com.mvt.mvt_events.jpa.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que intercepta todas as requisições HTTP e define o tenant ID
 * baseado na organização do usuário logado.
 * 
 * Este filtro roda DEPOIS do JwtAuthenticationFilter, garantindo que
 * o usuário já esteja autenticado e disponível no SecurityContext.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Skip tenant filtering for auth endpoints and public endpoints
        return path.startsWith("/api/auth/") ||
                path.startsWith("/api/metadata") ||
                path.startsWith("/api/webhooks/") ||
                path.startsWith("/webhooks/") || // Novo path sem /api
                path.startsWith("/api/payments/webhooks/") ||
                path.startsWith("/api/payments/methods") ||
                path.startsWith("/api/payments/calculate-fee") ||
                path.startsWith("/api/tracking/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response,
            jakarta.servlet.FilterChain chain)
            throws IOException, jakarta.servlet.ServletException {

        try {
            // Pega o usuário autenticado do Spring Security
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            logger.info("🔍 TenantFilter - Request: {} {}", request.getMethod(), request.getRequestURI());
            logger.info("🔍 TenantFilter - Authentication: {}",
                    authentication != null ? authentication.getClass().getSimpleName() : "null");

            if (authentication != null && authentication.isAuthenticated()) {
                logger.info("🔍 TenantFilter - Principal type: {}", authentication.getPrincipal().getClass().getName());
                logger.info("🔍 TenantFilter - Principal instanceof User: {}",
                        authentication.getPrincipal() instanceof User);

                if (authentication.getPrincipal() instanceof User) {
                    User user = (User) authentication.getPrincipal();
                    logger.info("🔍 TenantFilter - User: {}", user.getUsername());
                    // TODO: Implementar campos organization e role na entidade User
                    // logger.info("🔍 TenantFilter - User Organization: {}",
                    // user.getOrganization());
                    // logger.info("🔍 TenantFilter - User Role: {}", user.getRole());

                    // Por enquanto, assumir que não é admin
                    boolean isAdmin = false; // user.getRole() != null && "ADMIN".equals(user.getRole().name());
                    TenantContext.setIsAdmin(isAdmin);

                    if (isAdmin) {
                        logger.info("👑 TenantFilter - User is ADMIN - NO tenant filter will be applied");
                    } else {
                        // Por enquanto, usar o ID do usuário como tenant
                        // TODO: Implementar lógica correta de tenant baseada em organização
                        TenantContext.setCurrentTenantId(user.getId().hashCode() % 1000L);
                        logger.info("✅ TenantFilter - Tenant ID set to: {}", TenantContext.getCurrentTenantId());
                    }
                } else {
                    logger.warn("⚠️ TenantFilter - Principal is not a User instance");
                }
            } else {
                logger.warn("⚠️ TenantFilter - No authentication or not authenticated");
            }

            // Continua a cadeia de filtros
            chain.doFilter(request, response);

        } finally {
            // IMPORTANTE: Limpa o ThreadLocal SOMENTE após a resposta ser enviada
            // Comentado temporariamente para debug - o Aspect deve gerenciar isso
            // TenantContext.clear();
            logger.info("🔚 TenantFilter - Request completed");
        }
    }
}
