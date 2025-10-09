package com.mvt.mvt_events.metadata;

import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.metadata.FilterMetadata.FilterOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetadataService {

    @Autowired
    private JpaMetadataExtractor jpaExtractor;

    // Mapa de classes das entidades
    private static final Map<String, Class<?>> ENTITY_CLASSES = new HashMap<>();

    static {
        ENTITY_CLASSES.put("event", Event.class);
        ENTITY_CLASSES.put("registration", Registration.class);
        ENTITY_CLASSES.put("organization", Organization.class);
        ENTITY_CLASSES.put("user", User.class);
        ENTITY_CLASSES.put("payment", Payment.class);
        ENTITY_CLASSES.put("eventCategory", EventCategory.class);
    }

    public Map<String, EntityMetadata> getAllEntitiesMetadata() {
        Map<String, EntityMetadata> metadata = new HashMap<>();

        metadata.put("event", getEventMetadata());
        metadata.put("registration", getRegistrationMetadata());
        metadata.put("organization", getOrganizationMetadata());
        metadata.put("user", getUserMetadata());
        metadata.put("payment", getPaymentMetadata());
        metadata.put("eventCategory", getEventCategoryMetadata());

        return metadata;
    }

    public EntityMetadata getEntityMetadata(String entityName) {
        return getAllEntitiesMetadata().get(entityName);
    }

    private EntityMetadata getEventMetadata() {
        EntityMetadata metadata = new EntityMetadata("event", "Eventos", "/api/events");

        // Configuração de campos/colunas da tabela
        List<FieldMetadata> fields = new ArrayList<>();

        FieldMetadata nameField = new FieldMetadata("name", "Nome do Evento", "string");
        nameField.setWidth(200);
        fields.add(nameField);

        FieldMetadata dateField = new FieldMetadata("eventDate", "Data", "date");
        dateField.setFormat("dd/MM/yyyy");
        dateField.setWidth(120);
        dateField.setAlign("center");
        fields.add(dateField);

        FieldMetadata locationField = new FieldMetadata("location", "Local", "string");
        locationField.setWidth(150);
        fields.add(locationField);

        FieldMetadata eventTypeField = new FieldMetadata("eventType", "Esporte", "enum");
        eventTypeField.setWidth(120);
        fields.add(eventTypeField);

        FieldMetadata organizationField = new FieldMetadata("organization.name", "Organização", "string");
        organizationField.setWidth(150);
        organizationField.setSortable(false);
        fields.add(organizationField);

        FieldMetadata statusField = new FieldMetadata("status", "Status", "enum");
        statusField.setWidth(120);
        statusField.setAlign("center");
        fields.add(statusField);

        FieldMetadata actionsField = new FieldMetadata("actions", "Ações", "actions");
        actionsField.setWidth(150);
        actionsField.setSortable(false);
        actionsField.setSearchable(false);
        actionsField.setAlign("center");
        fields.add(actionsField);

        metadata.setFields(fields);

        // Configuração de filtros
        List<FilterMetadata> filters = new ArrayList<>();

        FilterMetadata statusFilter = new FilterMetadata("status", "Status", "select", "status");
        statusFilter.setOptions(Arrays.asList(
                new FilterOption("Todos os status", ""),
                new FilterOption("Pendente", "DRAFT"),
                new FilterOption("Publicado", "PUBLISHED"),
                new FilterOption("Cancelado", "CANCELLED"),
                new FilterOption("Finalizado", "COMPLETED")));
        filters.add(statusFilter);

        // Filtro de relacionamento: Organization
        FilterMetadata organizationFilter = new FilterMetadata("organizationId", "Organização", "entity",
                "organization.id");
        EntityFilterConfig orgConfig = EntityFilterHelper.createEntityFilterConfig("organizationId", Long.class);
        if (orgConfig != null) {
            organizationFilter.setEntityConfig(orgConfig);
        }
        filters.add(organizationFilter);

        // Filtro de relacionamento: Category
        FilterMetadata categoryFilter = new FilterMetadata("categoryId", "Categoria", "entity", "categoryId");
        EntityFilterConfig catConfig = EntityFilterHelper.createEntityFilterConfig("categoryId", Long.class);
        if (catConfig != null) {
            categoryFilter.setEntityConfig(catConfig);
        }
        filters.add(categoryFilter);

        FilterMetadata cityFilter = new FilterMetadata("city", "Cidade", "text", "city");
        cityFilter.setPlaceholder("Nome da cidade");
        filters.add(cityFilter);

        FilterMetadata stateFilter = new FilterMetadata("state", "Estado", "select", "state");
        stateFilter.setOptions(Arrays.asList(
                new FilterOption("Todos os estados", ""),
                new FilterOption("São Paulo", "SP"),
                new FilterOption("Rio de Janeiro", "RJ"),
                new FilterOption("Minas Gerais", "MG"),
                new FilterOption("Bahia", "BA"),
                new FilterOption("Paraná", "PR"),
                new FilterOption("Santa Catarina", "SC"),
                new FilterOption("Rio Grande do Sul", "RS")));
        filters.add(stateFilter);

        metadata.setFilters(filters);

        // Configuração de paginação
        PaginationConfig pagination = new PaginationConfig(5, new int[] { 5, 10, 20, 50 });
        metadata.setPagination(pagination);

        // ✅ CAMPOS ESPECÍFICOS PARA TABELA
        metadata.setTableFields(fields);

        // ✅ CAMPOS ESPECÍFICOS PARA FORMULÁRIO (via JPA)
        List<FieldMetadata> formFields = jpaExtractor.extractFields(Event.class);
        customizeEventFormFields(formFields);
        metadata.setFormFields(formFields);

        // Compatibilidade: fields = tableFields
        metadata.setFields(fields);

        return metadata;
    }

    private void customizeEventFormFields(List<FieldMetadata> fields) {
        for (FieldMetadata field : fields) {
            switch (field.getName()) {
                case "name":
                    field.setPlaceholder("Digite o nome do evento");
                    break;
                case "eventType":
                    field.setPlaceholder("Selecione o esporte");
                    break;
                case "status":
                    field.setPlaceholder("Selecione o status");
                    break;
                case "location":
                    field.setPlaceholder("Digite o local do evento");
                    break;
                case "description":
                    field.setPlaceholder("Descreva o evento");
                    break;
            }
        }
    }

    private EntityMetadata getRegistrationMetadata() {
        EntityMetadata metadata = new EntityMetadata("registration", "Inscrições", "/api/registrations");

        List<FieldMetadata> fields = new ArrayList<>();

        FieldMetadata userField = new FieldMetadata("user.name", "Participante", "string");
        userField.setWidth(200);
        fields.add(userField);

        FieldMetadata eventField = new FieldMetadata("event.name", "Evento", "string");
        eventField.setWidth(200);
        fields.add(eventField);

        FieldMetadata dateField = new FieldMetadata("registrationDate", "Data Inscrição", "datetime");
        dateField.setFormat("dd/MM/yyyy HH:mm");
        dateField.setWidth(150);
        fields.add(dateField);

        FieldMetadata statusField = new FieldMetadata("status", "Status", "enum");
        statusField.setWidth(120);
        fields.add(statusField);

        FieldMetadata actionsField = new FieldMetadata("actions", "Ações", "actions");
        actionsField.setWidth(150);
        actionsField.setSortable(false);
        actionsField.setSearchable(false);
        fields.add(actionsField);

        metadata.setFields(fields);

        List<FilterMetadata> filters = new ArrayList<>();

        FilterMetadata statusFilter = new FilterMetadata("status", "Status", "select", "status");
        statusFilter.setOptions(Arrays.asList(
                new FilterOption("Todos", ""),
                new FilterOption("Pendente", "PENDING"),
                new FilterOption("Ativa", "ACTIVE"),
                new FilterOption("Cancelada", "CANCELLED")));
        filters.add(statusFilter);

        // Filtro de relacionamento: Event
        FilterMetadata eventFilter = new FilterMetadata("eventId", "Evento", "entity", "event.id");
        EntityFilterConfig eventConfig = EntityFilterHelper.createEntityFilterConfig("eventId", Long.class);
        if (eventConfig != null) {
            eventFilter.setEntityConfig(eventConfig);
        }
        filters.add(eventFilter);

        // Filtro de relacionamento: User
        FilterMetadata userFilter = new FilterMetadata("userId", "Usuário", "entity", "user.id");
        EntityFilterConfig userConfig = EntityFilterHelper.createEntityFilterConfig("userId", java.util.UUID.class);
        if (userConfig != null) {
            userFilter.setEntityConfig(userConfig);
        }
        filters.add(userFilter);

        metadata.setFilters(filters);
        metadata.setPagination(new PaginationConfig());

        // ✅ Adiciona formFields
        addFormFieldsToMetadata(metadata, "registration", Registration.class);

        return metadata;
    }

    private EntityMetadata getOrganizationMetadata() {
        EntityMetadata metadata = new EntityMetadata("organization", "Organizações", "/api/organizations");

        List<FieldMetadata> fields = new ArrayList<>();

        FieldMetadata nameField = new FieldMetadata("name", "Nome", "string");
        nameField.setWidth(250);
        fields.add(nameField);

        FieldMetadata emailField = new FieldMetadata("contactEmail", "E-mail", "string");
        emailField.setWidth(200);
        fields.add(emailField);

        FieldMetadata phoneField = new FieldMetadata("phone", "Telefone", "string");
        phoneField.setWidth(150);
        fields.add(phoneField);

        FieldMetadata actionsField = new FieldMetadata("actions", "Ações", "actions");
        actionsField.setWidth(150);
        actionsField.setSortable(false);
        actionsField.setSearchable(false);
        fields.add(actionsField);

        metadata.setFields(fields);
        metadata.setFilters(new ArrayList<>());
        metadata.setPagination(new PaginationConfig());

        // ✅ Adiciona formFields
        addFormFieldsToMetadata(metadata, "organization", Organization.class);

        return metadata;
    }

    private EntityMetadata getUserMetadata() {
        EntityMetadata metadata = new EntityMetadata("user", "Usuários", "/api/users");

        List<FieldMetadata> fields = new ArrayList<>();

        FieldMetadata nameField = new FieldMetadata("name", "Nome", "string");
        nameField.setWidth(200);
        fields.add(nameField);

        FieldMetadata usernameField = new FieldMetadata("username", "E-mail", "string");
        usernameField.setWidth(200);
        fields.add(usernameField);

        FieldMetadata roleField = new FieldMetadata("role", "Perfil", "enum");
        roleField.setWidth(120);
        fields.add(roleField);

        FieldMetadata enabledField = new FieldMetadata("enabled", "Ativo", "boolean");
        enabledField.setWidth(100);
        fields.add(enabledField);

        FieldMetadata actionsField = new FieldMetadata("actions", "Ações", "actions");
        actionsField.setWidth(150);
        actionsField.setSortable(false);
        actionsField.setSearchable(false);
        fields.add(actionsField);

        metadata.setFields(fields);

        List<FilterMetadata> filters = new ArrayList<>();

        FilterMetadata roleFilter = new FilterMetadata("role", "Perfil", "select", "role");
        roleFilter.setOptions(Arrays.asList(
                new FilterOption("Todos", ""),
                new FilterOption("Admin", "ADMIN"),
                new FilterOption("Organizador", "ORGANIZER"),
                new FilterOption("Usuário/Atleta", "USER")));
        filters.add(roleFilter);

        // Filtro de relacionamento: Organization
        FilterMetadata organizationFilter = new FilterMetadata("organizationId", "Organização", "entity",
                "organization.id");
        EntityFilterConfig orgConfig = EntityFilterHelper.createEntityFilterConfig("organizationId", Long.class);
        if (orgConfig != null) {
            organizationFilter.setEntityConfig(orgConfig);
        }
        filters.add(organizationFilter);

        FilterMetadata enabledFilter = new FilterMetadata("enabled", "Status", "select", "enabled");
        enabledFilter.setOptions(Arrays.asList(
                new FilterOption("Todos", ""),
                new FilterOption("Ativos", "true"),
                new FilterOption("Inativos", "false")));
        filters.add(enabledFilter);

        metadata.setFilters(filters);
        metadata.setPagination(new PaginationConfig());

        // ✅ Adiciona formFields
        addFormFieldsToMetadata(metadata, "user", User.class);

        return metadata;
    }

    private EntityMetadata getPaymentMetadata() {
        EntityMetadata metadata = new EntityMetadata("payment", "Pagamentos", "/api/payments");

        List<FieldMetadata> fields = new ArrayList<>();

        FieldMetadata registrationField = new FieldMetadata("registration.id", "Inscrição ID", "number");
        registrationField.setWidth(120);
        fields.add(registrationField);

        FieldMetadata amountField = new FieldMetadata("amount", "Valor", "currency");
        amountField.setWidth(120);
        amountField.setAlign("right");
        fields.add(amountField);

        FieldMetadata statusField = new FieldMetadata("status", "Status", "enum");
        statusField.setWidth(120);
        statusField.setAlign("center");
        fields.add(statusField);

        FieldMetadata methodField = new FieldMetadata("paymentMethod", "Método", "enum");
        methodField.setWidth(120);
        fields.add(methodField);

        FieldMetadata providerField = new FieldMetadata("gatewayProvider", "Gateway", "string");
        providerField.setWidth(120);
        fields.add(providerField);

        FieldMetadata processedField = new FieldMetadata("processedAt", "Processado em", "datetime");
        processedField.setFormat("dd/MM/yyyy HH:mm");
        processedField.setWidth(150);
        fields.add(processedField);

        FieldMetadata actionsField = new FieldMetadata("actions", "Ações", "actions");
        actionsField.setWidth(150);
        actionsField.setSortable(false);
        actionsField.setSearchable(false);
        fields.add(actionsField);

        metadata.setFields(fields);

        List<FilterMetadata> filters = new ArrayList<>();

        FilterMetadata statusFilter = new FilterMetadata("status", "Status", "select", "status");
        statusFilter.setOptions(Arrays.asList(
                new FilterOption("Todos", ""),
                new FilterOption("Pendente", "PENDING"),
                new FilterOption("Processando", "PROCESSING"),
                new FilterOption("Completado", "COMPLETED"),
                new FilterOption("Falhou", "FAILED"),
                new FilterOption("Reembolsado", "REFUNDED")));
        filters.add(statusFilter);

        // Filtro de relacionamento: Registration
        FilterMetadata registrationFilter = new FilterMetadata("registrationId", "Inscrição", "entity",
                "registration.id");
        EntityFilterConfig regConfig = EntityFilterHelper.createEntityFilterConfig("registrationId", Long.class);
        if (regConfig != null) {
            registrationFilter.setEntityConfig(regConfig);
        }
        filters.add(registrationFilter);

        FilterMetadata providerFilter = new FilterMetadata("provider", "Gateway", "select", "gatewayProvider");
        providerFilter.setOptions(Arrays.asList(
                new FilterOption("Todos", ""),
                new FilterOption("Stripe", "stripe"),
                new FilterOption("Mercado Pago", "mercadopago"),
                new FilterOption("PayPal", "paypal")));
        filters.add(providerFilter);

        metadata.setFilters(filters);
        metadata.setPagination(new PaginationConfig());

        // ✅ Adiciona formFields
        addFormFieldsToMetadata(metadata, "payment", Payment.class);

        return metadata;
    }

    private EntityMetadata getEventCategoryMetadata() {
        EntityMetadata metadata = new EntityMetadata("eventCategory", "Categorias de Evento",
                "/api/event-categories");

        List<FieldMetadata> fields = new ArrayList<>();

        FieldMetadata eventField = new FieldMetadata("event.name", "Evento", "string");
        eventField.setWidth(200);
        fields.add(eventField);

        FieldMetadata nameField = new FieldMetadata("name", "Categoria", "string");
        nameField.setWidth(180);
        fields.add(nameField);

        FieldMetadata genderField = new FieldMetadata("gender", "Gênero", "enum");
        genderField.setWidth(100);
        genderField.setAlign("center");
        fields.add(genderField);

        FieldMetadata minAgeField = new FieldMetadata("minAge", "Idade Min", "number");
        minAgeField.setWidth(100);
        minAgeField.setAlign("center");
        fields.add(minAgeField);

        FieldMetadata maxAgeField = new FieldMetadata("maxAge", "Idade Max", "number");
        maxAgeField.setWidth(100);
        maxAgeField.setAlign("center");
        fields.add(maxAgeField);

        FieldMetadata priceField = new FieldMetadata("price", "Preço", "currency");
        priceField.setWidth(120);
        priceField.setAlign("right");
        fields.add(priceField);

        FieldMetadata maxParticipantsField = new FieldMetadata("maxParticipants", "Vagas", "number");
        maxParticipantsField.setWidth(100);
        maxParticipantsField.setAlign("center");
        fields.add(maxParticipantsField);

        FieldMetadata currentParticipantsField = new FieldMetadata("currentParticipants", "Inscritos", "number");
        currentParticipantsField.setWidth(100);
        currentParticipantsField.setAlign("center");
        fields.add(currentParticipantsField);

        FieldMetadata activeField = new FieldMetadata("isActive", "Ativa", "boolean");
        activeField.setWidth(80);
        activeField.setAlign("center");
        fields.add(activeField);

        FieldMetadata actionsField = new FieldMetadata("actions", "Ações", "actions");
        actionsField.setWidth(150);
        actionsField.setSortable(false);
        actionsField.setSearchable(false);
        fields.add(actionsField);

        metadata.setFields(fields);

        List<FilterMetadata> filters = new ArrayList<>();

        // Filtro de relacionamento: Event
        FilterMetadata eventFilter = new FilterMetadata("eventId", "Evento", "entity", "event.id");
        EntityFilterConfig eventConfig = EntityFilterHelper.createEntityFilterConfig("eventId", Long.class);
        if (eventConfig != null) {
            eventFilter.setEntityConfig(eventConfig);
        }
        filters.add(eventFilter);

        metadata.setFilters(filters);
        metadata.setPagination(new PaginationConfig());

        // ✅ Adiciona formFields
        addFormFieldsToMetadata(metadata, "eventCategory", EventCategory.class);

        return metadata;
    }

    /**
     * Método auxiliar para adicionar formFields extraídos via JPA
     */
    private void addFormFieldsToMetadata(EntityMetadata metadata, String entityName, Class<?> entityClass) {
        List<FieldMetadata> tableFields = metadata.getFields();

        // Extrai campos de formulário via JPA
        List<FieldMetadata> formFields = jpaExtractor.extractFields(entityClass);

        // Aplica customizações específicas
        customizeFormFieldsByEntity(entityName, formFields);

        // Define tableFields e formFields separadamente
        metadata.setTableFields(tableFields);
        metadata.setFormFields(formFields);

        // Mantém compatibilidade com código antigo
        metadata.setFields(tableFields);
    }

    /**
     * Customiza placeholders e outros detalhes dos campos de formulário
     */
    private void customizeFormFieldsByEntity(String entityName, List<FieldMetadata> fields) {
        switch (entityName) {
            case "event":
                customizeEventFormFields(fields);
                break;
            case "registration":
                customizeRegistrationFormFields(fields);
                break;
            case "organization":
                customizeOrganizationFormFields(fields);
                break;
            case "user":
                customizeUserFormFields(fields);
                break;
            case "payment":
                customizePaymentFormFields(fields);
                break;
            case "eventCategory":
                customizeEventCategoryFormFields(fields);
                break;
        }
    }

    private void customizeRegistrationFormFields(List<FieldMetadata> fields) {
        for (FieldMetadata field : fields) {
            switch (field.getName()) {
                case "status":
                    field.setPlaceholder("Selecione o status");
                    break;
            }
        }
    }

    private void customizeOrganizationFormFields(List<FieldMetadata> fields) {
        for (FieldMetadata field : fields) {
            switch (field.getName()) {
                case "name":
                    field.setPlaceholder("Nome da organização");
                    break;
                case "contactEmail":
                    field.setPlaceholder("E-mail de contato");
                    break;
            }
        }
    }

    private void customizeUserFormFields(List<FieldMetadata> fields) {
        for (FieldMetadata field : fields) {
            switch (field.getName()) {
                case "name":
                    field.setPlaceholder("Nome completo");
                    break;
                case "username":
                    field.setPlaceholder("E-mail do usuário");
                    break;
                case "role":
                    field.setPlaceholder("Selecione o perfil");
                    break;
            }
        }
    }

    private void customizePaymentFormFields(List<FieldMetadata> fields) {
        for (FieldMetadata field : fields) {
            switch (field.getName()) {
                case "amount":
                    field.setPlaceholder("Valor do pagamento");
                    break;
                case "paymentMethod":
                    field.setPlaceholder("Método de pagamento");
                    break;
            }
        }
    }

    private void customizeEventCategoryFormFields(List<FieldMetadata> fields) {
        for (FieldMetadata field : fields) {
            switch (field.getName()) {
                case "name":
                    field.setPlaceholder("Nome da categoria");
                    break;
                case "gender":
                    field.setPlaceholder("Selecione o gênero");
                    break;
                case "distanceUnit":
                    field.setPlaceholder("Unidade de distância");
                    break;
            }
        }
    }
}
