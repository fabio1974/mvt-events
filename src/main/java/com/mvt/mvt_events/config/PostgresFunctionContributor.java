package com.mvt.mvt_events.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * Registra funções SQL customizadas do PostgreSQL no Hibernate.
 * Isso permite usar essas funções em queries JPQL via function('nome', ...)
 */
public class PostgresFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        // Registra a função immutable_unaccent para busca insensível a acentos
        functionContributions.getFunctionRegistry().registerPattern(
                "immutable_unaccent",
                "immutable_unaccent(?1)",
                functionContributions.getTypeConfiguration()
                        .getBasicTypeRegistry()
                        .resolve(StandardBasicTypes.STRING)
        );
    }
}
