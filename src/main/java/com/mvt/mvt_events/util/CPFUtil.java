package com.mvt.mvt_events.util;

/**
 * Utilitário para manipulação e validação de CPF
 */
public class CPFUtil {

    /**
     * Remove formatação do CPF (mantém apenas números)
     */
    public static String clean(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("[^0-9]", "");
    }

    /**
     * Formata CPF no padrão XXX.XXX.XXX-XX
     */
    public static String format(String cpf) {
        String clean = clean(cpf);
        if (clean == null || clean.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
                clean.substring(0, 3),
                clean.substring(3, 6),
                clean.substring(6, 9),
                clean.substring(9, 11));
    }

    /**
     * Valida CPF usando o algoritmo de dígitos verificadores
     */
    public static boolean isValid(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return false;
        }

        // Remove formatação
        cpf = clean(cpf);

        // CPF deve ter exatamente 11 dígitos
        if (cpf.length() != 11) {
            return false;
        }

        // Rejeita CPFs com todos os dígitos iguais
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            // Calcula o primeiro dígito verificador
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
            }
            int firstDigit = 11 - (sum % 11);
            if (firstDigit >= 10) {
                firstDigit = 0;
            }

            // Verifica o primeiro dígito
            if (firstDigit != Character.getNumericValue(cpf.charAt(9))) {
                return false;
            }

            // Calcula o segundo dígito verificador
            sum = 0;
            for (int i = 0; i < 10; i++) {
                sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
            }
            int secondDigit = 11 - (sum % 11);
            if (secondDigit >= 10) {
                secondDigit = 0;
            }

            // Verifica o segundo dígito
            return secondDigit == Character.getNumericValue(cpf.charAt(10));

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Mascara o CPF parcialmente (XXX.XXX.XXX-XX -> XXX.***.**X-XX)
     */
    public static String mask(String cpf) {
        String formatted = format(cpf);
        if (formatted == null || formatted.length() < 14) {
            return cpf;
        }
        return formatted.substring(0, 3) + ".***.**" + formatted.substring(11);
    }
}
