package com.mvt.mvt_events.tenant;

import com.mvt.mvt_events.jpa.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro que intercepta todas as requisições HTTP e define o tenant ID
 * baseado na organização do usuário logado.
 * 
 * Este filtro roda ANTES das queries do Hibernate, permitindo que
 * o TenantInterceptor saiba qual organização filtrar.
 */
@Component
@Order(1) // Executa antes de outros filtros
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // Pega o usuário autenticado do Spring Security
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof User) {

                User user = (User) authentication.getPrincipal();

                // Define o tenant ID se o usuário tiver uma organização
                if (user.getOrganization() != null) {
                    Long organizationId = user.getOrganization().getId();
                    TenantContext.setCurrentTenantId(organizationId);
                }
            }

            // Continua a cadeia de filtros
            chain.doFilter(request, response);

        } finally {
            // IMPORTANTE: Limpa o ThreadLocal para evitar memory leaks
            TenantContext.clear();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Inicialização se necessário
    }

    @Override
    public void destroy() {
        // Cleanup se necessário
    }
}
