package com.mvt.mvt_events.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação para validação de CPF ou CNPJ brasileiro
 */
@Documented
@Constraint(validatedBy = DocumentValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Document {

    String message() default "CPF ou CNPJ inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    boolean required() default true;
}
