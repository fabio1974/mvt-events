package com.mvt.mvt_events.validation;

import com.mvt.mvt_events.util.CPFUtil;
import com.mvt.mvt_events.util.CNPJUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador de CPF ou CNPJ brasileiro
 * Aceita tanto CPF (11 dígitos) quanto CNPJ (14 dígitos)
 */
public class DocumentValidator implements ConstraintValidator<Document, String> {

    private boolean required;

    @Override
    public void initialize(Document constraintAnnotation) {
        this.required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String document, ConstraintValidatorContext context) {
        // Se não é obrigatório e está vazio/null, é válido
        if (!required && (document == null || document.trim().isEmpty())) {
            return true;
        }

        // Se é obrigatório e está vazio/null, é inválido
        if (document == null || document.trim().isEmpty()) {
            return false;
        }

        // Remove formatação para contar dígitos
        String cleanDocument = document.replaceAll("[^0-9]", "");

        // Tenta validar como CPF (11 dígitos) ou CNPJ (14 dígitos)
        if (cleanDocument.length() == 11) {
            return CPFUtil.isValid(document);
        } else if (cleanDocument.length() == 14) {
            return CNPJUtil.isValid(document);
        }

        // Tamanho inválido (nem CPF nem CNPJ)
        return false;
    }
}
