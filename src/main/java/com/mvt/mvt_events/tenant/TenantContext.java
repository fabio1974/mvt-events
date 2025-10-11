package com.mvt.mvt_events.tenant;

/**
 * ThreadLocal para armazenar o ID da organização (tenant) do usuário logado.
 * Usado para filtrar automaticamente todas as queries por organização.
 */
public class TenantContext {

    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> isAdmin = new ThreadLocal<>();

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
     * Define se o usuário atual é ADMIN
     */
    public static void setIsAdmin(boolean admin) {
        isAdmin.set(admin);
    }

    /**
     * Verifica se o usuário atual é ADMIN
     */
    public static boolean isAdmin() {
        Boolean admin = isAdmin.get();
        return admin != null && admin;
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
        isAdmin.remove();
    }
}
