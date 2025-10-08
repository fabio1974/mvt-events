package com.mvt.mvt_events.metadata;

import lombok.*;

/**
 * Configuração de filtros que referenciam outras entidades.
 * Usado para criar selects dinâmicos no frontend com busca de dados do backend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityFilterConfig {

    /**
     * Nome da entidade (ex: "event", "user")
     */
    private String entityName;

    /**
     * Endpoint para buscar dados da entidade (ex: "/api/events")
     */
    private String endpoint;

    /**
     * Campo usado como label no select (ex: "name")
     */
    private String labelField;

    /**
     * Campo usado como value no select (ex: "id")
     */
    private String valueField;

    /**
     * Tipo de componente para renderização no frontend.
     * - "select": Dropdown tradicional (para poucas opções, carrega todas de uma
     * vez)
     * - "typeahead": Autocomplete com busca dinâmica (para muitas opções, busca sob
     * demanda)
     */
    private String renderAs;

    /**
     * Se o select permite busca/filtro
     */
    private Boolean searchable;

    /**
     * Placeholder para o campo de busca
     */
    private String searchPlaceholder;

    /**
     * Campos adicionais para exibição (opcional)
     */
    private String[] additionalFields;
}
