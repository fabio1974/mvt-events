package com.mvt.mvt_events.jpa;

/**
 * Provedores de pagamento suportados
 */
public enum PaymentProvider {
    /**
     * Iugu - Gateway de pagamento brasileiro
     */
    IUGU("Iugu", "https://iugu.com"),
    
    /**
     * Stripe - Gateway de pagamento internacional
     */
    STRIPE("Stripe", "https://stripe.com"),
    
    /**
     * MercadoPago - Gateway de pagamento latino-americano
     */
    MERCADO_PAGO("MercadoPago", "https://mercadopago.com"),
    
    /**
     * PayPal - Gateway de pagamento internacional
     */
    PAYPAL("PayPal", "https://paypal.com"),
    
    /**
     * PagSeguro - Gateway de pagamento brasileiro
     */
    PAGSEGURO("PagSeguro", "https://pagseguro.uol.com.br"),
    
    /**
     * Outro provedor não listado
     */
    OTHER("Outro", null);

    private final String displayName;
    private final String website;

    PaymentProvider(String displayName, String website) {
        this.displayName = displayName;
        this.website = website;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getWebsite() {
        return website;
    }

    /**
     * Retorna código do provedor em lowercase
     */
    public String getCode() {
        return this.name().toLowerCase().replace("_", "-");
    }
}
