package com.mvt.mvt_events.tenant;

/**
 * ThreadLocal para armazenar o ID da organização (tenant) do usuário logado.
 * Usado para filtrar automaticamente todas as queries por organização.
 */
public class TenantContext {

    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();

    /**
     * Define o tenant ID para a thread atual
     */
    public static void setCurrentTenantId(Long tenantId) {
        currentTenantId.set(tenantId);
    }

    /**
     * Obtém o tenant ID da thread atual
     */
    public static Long getCurrentTenantId() {
        return currentTenantId.get();
    }

    /**
     * Verifica se há um tenant definido
     */
    public static boolean hasTenant() {
        return currentTenantId.get() != null;
    }

    /**
     * Limpa o tenant da thread atual (IMPORTANTE para evitar memory leaks)
     */
    public static void clear() {
        currentTenantId.remove();
    }
}
