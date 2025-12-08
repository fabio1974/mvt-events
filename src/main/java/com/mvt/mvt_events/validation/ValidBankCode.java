package com.mvt.mvt_events.validation;

import com.mvt.mvt_events.util.BrazilianBanks;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação customizada para validar códigos de bancos brasileiros.
 * Verifica se o código tem 3 dígitos E existe no cadastro do Banco Central.
 * 
 * Exemplo de uso:
 * <pre>
 * {@code
 * @ValidBankCode
 * private String bankCode;
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidBankCode.BankCodeValidator.class)
@Documented
public @interface ValidBankCode {

    String message() default "Código de banco inválido. Deve ter 3 dígitos e existir no cadastro do Banco Central.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Se true, valida apenas o formato (3 dígitos).
     * Se false (padrão), valida formato E existência no cadastro.
     */
    boolean formatOnly() default false;

    /**
     * Implementação do validador
     */
    class BankCodeValidator implements ConstraintValidator<ValidBankCode, String> {

        private boolean formatOnly;

        @Override
        public void initialize(ValidBankCode annotation) {
            this.formatOnly = annotation.formatOnly();
        }

        @Override
        public boolean isValid(String code, ConstraintValidatorContext context) {
            // Null ou vazio é tratado por @NotNull/@NotBlank
            if (code == null || code.isBlank()) {
                return true; // Deixa @NotNull/@NotBlank cuidar disso
            }

            // Validação apenas de formato (3 dígitos)
            if (formatOnly) {
                return BrazilianBanks.hasValidFormat(code);
            }

            // Validação completa: formato + existência
            boolean isValid = BrazilianBanks.isValid(code);

            if (!isValid) {
                // Customiza mensagem de erro
                context.disableDefaultConstraintViolation();
                
                if (!BrazilianBanks.hasValidFormat(code)) {
                    context.buildConstraintViolationWithTemplate(
                        "Código de banco inválido. Deve ter exatamente 3 dígitos numéricos."
                    ).addConstraintViolation();
                } else {
                    context.buildConstraintViolationWithTemplate(
                        "Código de banco '" + code + "' não encontrado no cadastro do Banco Central. " +
                        "Bancos válidos: 001=BB, 033=Santander, 104=Caixa, 237=Bradesco, 341=Itaú, 260=Nubank, etc."
                    ).addConstraintViolation();
                }
            }

            return isValid;
        }
    }
}
