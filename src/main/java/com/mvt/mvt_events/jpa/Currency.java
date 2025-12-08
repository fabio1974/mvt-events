package com.mvt.mvt_events.jpa;

/**
 * Moedas suportadas pelo sistema de pagamento
 */
public enum Currency {
    /**
     * Real Brasileiro
     */
    BRL("Real Brasileiro", "R$", "BRA"),
    
    /**
     * Dólar Americano
     */
    USD("US Dollar", "$", "USA"),
    
    /**
     * Euro
     */
    EUR("Euro", "€", "EUR");

    private final String displayName;
    private final String symbol;
    private final String countryCode;

    Currency(String displayName, String symbol, String countryCode) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.countryCode = countryCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Retorna código ISO 4217 (3 letras)
     */
    public String getCode() {
        return this.name();
    }
}
