package com.mvt.mvt_events.tenant;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Interceptor do Hibernate que ativa filtros de tenant automaticamente
 * em todas as sessões do EntityManager.
 * 
 * Aplica o filtro "organizationFilter" em todas as queries para garantir
 * que usuários só vejam dados da sua própria organização.
 */
@Component
public class TenantInterceptor {

    @Autowired
    private EntityManager entityManager;

    /**
     * Ativa o filtro de tenant na sessão do Hibernate
     */
    public void enableTenantFilter() {
        Long tenantId = TenantContext.getCurrentTenantId();

        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);

            // Ativa o filtro definido nas entidades
            org.hibernate.Filter filter = session.enableFilter("organizationFilter");
            filter.setParameter("organizationId", tenantId);
        }
    }

    /**
     * Desativa o filtro de tenant (usado em casos especiais, como admin global)
     */
    public void disableTenantFilter() {
        Session session = entityManager.unwrap(Session.class);
        session.disableFilter("organizationFilter");
    }
}
