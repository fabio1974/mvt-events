package com.mvt.mvt_events.tenant;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(TenantFilterAspect.class);

    @Autowired
    private EntityManager entityManager;

    /**
     * Ativa o filtro antes de qualquer método de repository ser executado
     * EXCETO UserRepository (para evitar ciclo infinito no TenantFilter)
     * 
     * TEMPORARIAMENTE DESABILITADO PARA TESTE
     */
    // @Before("execution(* com.mvt.mvt_events.repository..*(..)) && !execution(*
    // com.mvt.mvt_events.repository.UserRepository.*(..))")
    public void enableTenantFilter() {
        Long tenantId = TenantContext.getCurrentTenantId();

        logger.info("🔧 TenantFilterAspect - Tenant ID from context: {}", tenantId);

        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);

            // Verifica se o filtro já está ativo para evitar duplicação
            if (session.getEnabledFilter("organizationFilter") == null) {
                org.hibernate.Filter filter = session.enableFilter("organizationFilter");
                filter.setParameter("organizationId", tenantId);
                logger.info("✅ TenantFilterAspect - Filter enabled with organizationId: {}", tenantId);
            } else {
                logger.info("ℹ️ TenantFilterAspect - Filter already enabled");
            }
        } else {
            logger.warn("⚠️ TenantFilterAspect - No tenant ID in context!");
        }
    }
}