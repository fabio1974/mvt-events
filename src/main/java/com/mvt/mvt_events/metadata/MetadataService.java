package com.mvt.mvt_events.metadata;

import com.mvt.mvt_events.jpa.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetadataService {

    @Autowired
    private JpaMetadataExtractor jpaExtractor;

    private static final Map<String, EntityConfig> ENTITIES = new HashMap<>();

    static {
        ENTITIES.put("event", new EntityConfig(Event.class, "Eventos", "/api/events"));
        ENTITIES.put("registration", new EntityConfig(Registration.class, "Inscrições", "/api/registrations"));
        ENTITIES.put("organization", new EntityConfig(Organization.class, "Organizações", "/api/organizations"));
        ENTITIES.put("user", new EntityConfig(User.class, "Usuários", "/api/users"));
        ENTITIES.put("payment", new EntityConfig(Payment.class, "Pagamentos", "/api/payments"));
        ENTITIES.put("eventCategory", new EntityConfig(EventCategory.class, "Categorias", "/api/event-categories"));
    }

    public Map<String, EntityMetadata> getAllEntitiesMetadata() {
        Map<String, EntityMetadata> all = new HashMap<>();
        for (Map.Entry<String, EntityConfig> entry : ENTITIES.entrySet()) {
            all.put(entry.getKey(), getEntityMetadata(entry.getKey()));
        }
        return all;
    }

    public EntityMetadata getEntityMetadata(String entityName) {
        EntityConfig config = ENTITIES.get(entityName);
        if (config == null)
            throw new IllegalArgumentException("Entity not found: " + entityName);

        EntityMetadata metadata = new EntityMetadata(entityName, config.label, config.endpoint);
        List<FieldMetadata> fields = jpaExtractor.extractFields(config.entityClass);

        // Criar tableFields e aplicar visibilidade baseada em @Visible(table)
        List<FieldMetadata> tableFields = fields.stream()
                .filter(f -> !isSystemField(f.getName()))
                .map(f -> {
                    FieldMetadata copy = copyField(f);
                    // Se @Visible(table = false), marca como não visível
                    if (f.getHiddenFromTable() != null && f.getHiddenFromTable()) {
                        copy.setVisible(false);
                        copy.setSortable(false);
                    }
                    return copy;
                })
                .toList();

        // Criar formFields e aplicar visibilidade baseada em @Visible(form)
        List<FieldMetadata> formFields = fields.stream()
                .filter(f -> !isSystemField(f.getName()))
                .map(f -> {
                    FieldMetadata copy = copyField(f);
                    // Se @Visible(form = false), marca como não visível
                    if (f.getHiddenFromForm() != null && f.getHiddenFromForm()) {
                        System.out.println("DEBUG: Campo '" + f.getName() + "' - hiddenFromForm=" + f.getHiddenFromForm() + ", setando visible=false");
                        copy.setVisible(false);
                    }
                    return copy;
                })
                .toList();

        metadata.setTableFields(tableFields);
        metadata.setFormFields(formFields);

        // Extrair filtros e remover apenas os marcados com @Visible(filter = false)
        List<FilterMetadata> filters = jpaExtractor.extractFilters(config.entityClass).stream()
                .filter(f -> !isFieldHiddenFromFilter(fields, f.getName()))
                .toList();
        metadata.setFilters(filters);

        metadata.setPagination(null);

        return metadata;
    }

    /**
     * Cria uma cópia do FieldMetadata para poder modificar a propriedade visible
     * sem afetar outras listas
     */
    private FieldMetadata copyField(FieldMetadata source) {
        FieldMetadata copy = new FieldMetadata();
        copy.setName(source.getName());
        copy.setLabel(source.getLabel());
        copy.setType(source.getType());
        copy.setRequired(source.getRequired());
        copy.setSortable(source.isSortable());
        copy.setSearchable(source.isSearchable());
        copy.setVisible(source.isVisible());
        copy.setWidth(source.getWidth());
        copy.setOptions(source.getOptions());
        copy.setRelationship(source.getRelationship());
        copy.setMaxLength(source.getMaxLength());
        copy.setMinLength(source.getMinLength());
        copy.setMax(source.getMax());
        copy.setMin(source.getMin());
        copy.setDefaultValue(source.getDefaultValue());
        copy.setHiddenFromTable(source.getHiddenFromTable());
        copy.setHiddenFromForm(source.getHiddenFromForm());
        copy.setHiddenFromFilter(source.getHiddenFromFilter());
        return copy;
    }

    private boolean isSystemField(String fieldName) {
        return "id".equals(fieldName) || "createdAt".equals(fieldName) ||
                "updatedAt".equals(fieldName) || "tenantId".equals(fieldName);
    }

    /**
     * Verifica se um filtro corresponde a um campo que está oculto de filtros
     * via @Visible(filter = false)
     */
    private boolean isFieldHiddenFromFilter(List<FieldMetadata> fields, String filterName) {
        return fields.stream()
                .filter(f -> f.getName().equals(filterName))
                .anyMatch(f -> f.getHiddenFromFilter() != null && f.getHiddenFromFilter());
    }

    private static class EntityConfig {
        final Class<?> entityClass;
        final String label;
        final String endpoint;

        EntityConfig(Class<?> entityClass, String label, String endpoint) {
            this.entityClass = entityClass;
            this.label = label;
            this.endpoint = endpoint;
        }
    }
}
