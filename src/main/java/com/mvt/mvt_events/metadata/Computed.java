package com.mvt.mvt_events.metadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um campo como computado automaticamente pelo frontend.
 * 
 * Campos computados:
 * - São calculados em tempo real com base em outros campos (dependências)
 * - São sempre readonly no frontend
 * - O backend aceita o valor calculado, mas não precisa recalculá-lo
 * 
 * Exemplo:
 * 
 * <pre>
 * {@literal @}Computed(function = "categoryName", dependencies = {"distance", "gender", "minAge", "maxAge"})
 * private String name;
 * </pre>
 * 
 * O frontend irá:
 * 1. Observar mudanças nos campos dependentes
 * 2. Recalcular automaticamente o valor usando a função especificada
 * 3. Renderizar o campo como readonly
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Computed {

    /**
     * Nome da função de cálculo a ser executada pelo frontend.
     * 
     * Funções disponíveis:
     * - "categoryName": Gera nome de categoria (distância + gênero + faixa etária)
     * 
     * Exemplo: "5KM - Masculino - 30 a 39 anos"
     * 
     * @return nome da função de cálculo
     */
    String function();

    /**
     * Lista de campos que, quando mudarem, disparam o recálculo.
     * 
     * Deve conter os nomes exatos dos campos (nome do atributo Java).
     * 
     * Exemplo: {"distance", "gender", "minAge", "maxAge"}
     * 
     * @return array com nomes dos campos dependentes
     */
    String[] dependencies();
}
