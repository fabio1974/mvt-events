package com.mvt.mvt_events.tenant;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Aspect que intercepta métodos de repositórios e ativa o filtro de tenant
 * automaticamente antes de cada query.
 * 
 * Isso garante que TODAS as queries sejam filtradas pela organização do
 * usuário,
 * sem necessidade de código adicional em cada repository.
 */
@Aspect
@Component
public class TenantFilterAspect {

    @Autowired
    private EntityManager entityManager;

    /**
     * Ativa o filtro antes de qualquer método de repository ser executado
     */
    @Before("execution(* com.mvt.mvt_events.repository..*(..))")
    public void enableTenantFilter() {
        Long tenantId = TenantContext.getCurrentTenantId();

        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);

            // Verifica se o filtro já está ativo para evitar duplicação
            if (session.getEnabledFilter("organizationFilter") == null) {
                org.hibernate.Filter filter = session.enableFilter("organizationFilter");
                filter.setParameter("organizationId", tenantId);
            }
        }
    }
}
