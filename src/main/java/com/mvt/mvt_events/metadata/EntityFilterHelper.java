package com.mvt.mvt_events.metadata;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

/**
 * Helper para criar configurações de filtros de entidades relacionadas.
 * Detecta automaticamente relacionamentos e cria EntityFilterConfig.
 */
@Slf4j
public class EntityFilterHelper {

    private static final String[] ENTITY_PACKAGES = {
            "com.mvt.mvt_events.jpa",
            "com.mvt.mvt_events.model",
            "com.mvt.mvt_events.entity",
            "com.mvt.mvt_events.domain"
    };

    // Mapeamento de nomes especiais (campo -> entidade)
    private static final Map<String, String> SPECIAL_MAPPINGS = Map.of(
            "category", "EventCategory");

    // Mapeamento de endpoints especiais (entityName -> endpoint)
    private static final Map<String, String> ENDPOINT_MAPPINGS = Map.of(
            "category", "/api/event-categories");

    /**
     * Configuração de tipo de renderização por entidade.
     * - "select": Dropdown tradicional (para poucas opções, < 50 registros)
     * - "typeahead": Autocomplete com busca dinâmica (para muitas opções, >= 50
     * registros)
     */
    private static final Map<String, String> RENDER_TYPE_CONFIG = Map.of(
            "user", "typeahead", // Usuários: muitos registros
            "event", "select", // Eventos: poucos registros
            "organization", "select", // Organizações: poucos registros
            "category", "select", // Categorias: poucos registros
            "eventCategory", "select", // Categorias de evento: poucos registros
            "payment", "select", // Pagamentos: contexto específico
            "registration", "select" // Inscrições: contexto específico
    );

    /**
     * Mapeamento de nomes humanizados para placeholders de busca.
     */
    private static final Map<String, String> HUMAN_NAMES = Map.of(
            "user", "usuário",
            "event", "evento",
            "organization", "organização",
            "category", "categoria",
            "eventCategory", "categoria de evento",
            "payment", "pagamento",
            "registration", "inscrição");

    /**
     * Cria EntityFilterConfig para um filtro de relacionamento.
     * 
     * @param filterName Nome do filtro (ex: "eventId")
     * @param filterType Tipo do campo (ex: Long.class)
     * @return EntityFilterConfig ou null se não for relacionamento
     */
    public static EntityFilterConfig createEntityFilterConfig(String filterName, Class<?> filterType) {
        // Verifica se termina com "Id"
        if (!filterName.endsWith("Id")) {
            return null;
        }

        // Verifica se é tipo de ID
        if (!isIdType(filterType)) {
            return null;
        }

        // Remove "Id" do final para obter nome da entidade
        String entityName = filterName.substring(0, filterName.length() - 2);

        // Encontra a classe da entidade
        Class<?> entityClass = findEntityClass(entityName);
        if (entityClass == null) {
            log.debug("Entity class not found for filter: {}", filterName);
            return null;
        }

        // Busca campo com @DisplayLabel
        String labelField = findDisplayLabelField(entityClass);
        if (labelField == null) {
            labelField = "id"; // Fallback para ID se não tiver @DisplayLabel
            log.debug("Using 'id' as label field for entity: {}", entityName);
        }

        // Busca endpoint especial ou usa padrão
        String endpoint = ENDPOINT_MAPPINGS.getOrDefault(entityName, "/api/" + pluralize(entityName) + "s");

        // Determina tipo de renderização (select vs typeahead)
        String renderAs = RENDER_TYPE_CONFIG.getOrDefault(entityName, "select");

        // Define placeholder de busca humanizado
        String searchPlaceholder = "Buscar " + humanize(entityName) + "...";

        return EntityFilterConfig.builder()
                .entityName(entityName)
                .endpoint(endpoint)
                .labelField(labelField)
                .valueField("id")
                .renderAs(renderAs)
                .searchable(true)
                .searchPlaceholder(searchPlaceholder)
                .build();
    }

    /**
     * Verifica se o tipo é um ID válido.
     */
    private static boolean isIdType(Class<?> type) {
        return type == Long.class ||
                type == Integer.class ||
                type == UUID.class ||
                type.getName().equals("java.util.UUID");
    }

    /**
     * Encontra a classe da entidade pelo nome.
     */
    private static Class<?> findEntityClass(String entityName) {
        try {
            // Checa mapeamentos especiais primeiro
            String className = SPECIAL_MAPPINGS.getOrDefault(entityName, null);

            if (className == null) {
                // Capitaliza primeira letra
                className = entityName.substring(0, 1).toUpperCase() +
                        entityName.substring(1);
            }

            // Tenta encontrar nos packages conhecidos
            for (String pkg : ENTITY_PACKAGES) {
                try {
                    return Class.forName(pkg + "." + className);
                } catch (ClassNotFoundException e) {
                    // Tenta próximo package
                }
            }
        } catch (Exception e) {
            log.error("Error finding entity class for: {}", entityName, e);
        }
        return null;
    }

    /**
     * Encontra campo anotado com @DisplayLabel.
     */
    private static String findDisplayLabelField(Class<?> entityClass) {
        try {
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(DisplayLabel.class)) {
                    return field.getName();
                }
            }
        } catch (Exception e) {
            log.error("Error finding display label field for: {}", entityClass.getName(), e);
        }
        return null;
    }

    /**
     * Pluraliza nome de entidade (simples).
     */
    private static String pluralize(String word) {
        if (word.endsWith("y")) {
            return word.substring(0, word.length() - 1) + "ie";
        }
        if (word.endsWith("s") || word.endsWith("x") || word.endsWith("z") ||
                word.endsWith("ch") || word.endsWith("sh")) {
            return word + "e";
        }
        return word;
    }

    /**
     * Converte nome da entidade para formato humanizado (português).
     * Exemplo: "user" → "usuário", "eventCategory" → "categoria de evento"
     */
    private static String humanize(String entityName) {
        return HUMAN_NAMES.getOrDefault(entityName, entityName);
    }
}
