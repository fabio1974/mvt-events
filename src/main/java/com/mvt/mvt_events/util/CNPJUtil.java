package com.mvt.mvt_events.util;

/**
 * Utilitário para validação e formatação de CNPJ brasileiro
 */
public class CNPJUtil {

    /**
     * Remove caracteres não numéricos do CNPJ
     */
    public static String clean(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        return cnpj.replaceAll("[^0-9]", "");
    }

    /**
     * Valida um CNPJ brasileiro
     * 
     * @param cnpj CNPJ a ser validado (com ou sem formatação)
     * @return true se o CNPJ é válido, false caso contrário
     */
    public static boolean isValid(String cnpj) {
        if (cnpj == null) {
            return false;
        }

        // Remove caracteres não numéricos
        String cleanCnpj = clean(cnpj);

        // CNPJ deve ter exatamente 14 dígitos
        if (cleanCnpj.length() != 14) {
            return false;
        }

        // Verifica se todos os dígitos são iguais (ex: 00000000000000)
        if (cleanCnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        // Validação dos dígitos verificadores
        try {
            // Calcula primeiro dígito verificador
            int sum = 0;
            int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            for (int i = 0; i < 12; i++) {
                sum += Character.getNumericValue(cleanCnpj.charAt(i)) * weights1[i];
            }
            int firstDigit = sum % 11 < 2 ? 0 : 11 - (sum % 11);

            // Verifica primeiro dígito
            if (firstDigit != Character.getNumericValue(cleanCnpj.charAt(12))) {
                return false;
            }

            // Calcula segundo dígito verificador
            sum = 0;
            int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            for (int i = 0; i < 13; i++) {
                sum += Character.getNumericValue(cleanCnpj.charAt(i)) * weights2[i];
            }
            int secondDigit = sum % 11 < 2 ? 0 : 11 - (sum % 11);

            // Verifica segundo dígito
            return secondDigit == Character.getNumericValue(cleanCnpj.charAt(13));

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Formata um CNPJ para o padrão XX.XXX.XXX/XXXX-XX
     * 
     * @param cnpj CNPJ a ser formatado (apenas números)
     * @return CNPJ formatado ou o valor original se não for possível formatar
     */
    public static String format(String cnpj) {
        String cleanCnpj = clean(cnpj);
        if (cleanCnpj == null || cleanCnpj.length() != 14) {
            return cnpj;
        }
        return String.format("%s.%s.%s/%s-%s",
                cleanCnpj.substring(0, 2),
                cleanCnpj.substring(2, 5),
                cleanCnpj.substring(5, 8),
                cleanCnpj.substring(8, 12),
                cleanCnpj.substring(12, 14));
    }
}
