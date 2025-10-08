package com.mvt.mvt_events.validation;

import com.mvt.mvt_events.util.CPFUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador de CPF brasileiro
 * Reutiliza a lógica de validação do CPFUtil
 */
public class CPFValidator implements ConstraintValidator<CPF, String> {

    private boolean required;

    @Override
    public void initialize(CPF constraintAnnotation) {
        this.required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        // Se não é obrigatório e está vazio/null, é válido
        if (!required && (cpf == null || cpf.trim().isEmpty())) {
            return true;
        }

        // Se é obrigatório e está vazio/null, é inválido
        if (cpf == null || cpf.trim().isEmpty()) {
            return false;
        }

        // Delega a validação para o CPFUtil (reutiliza código)
        return CPFUtil.isValid(cpf);
    }
}
