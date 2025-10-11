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
                    logger.info("🔍 TenantFilter - User Organization: {}", user.getOrganization());
                    logger.info("🔍 TenantFilter - User Role: {}", user.getRole());

                    // Verifica se é ADMIN
                    boolean isAdmin = user.getRole() != null && "ADMIN".equals(user.getRole().name());
                    TenantContext.setIsAdmin(isAdmin);

                    if (isAdmin) {
                        logger.info("👑 TenantFilter - User is ADMIN - NO tenant filter will be applied");
                    } else {
                        // Define o tenant ID se o usuário tiver uma organização
                        if (user.getOrganization() != null) {
                            Long organizationId = user.getOrganization().getId();
                            TenantContext.setCurrentTenantId(organizationId);
                            logger.info("✅ TenantFilter - Tenant ID set to: {}", organizationId);
                        } else {
                            logger.warn("⚠️ TenantFilter - User has no organization");
                        }
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
