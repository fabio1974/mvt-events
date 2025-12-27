package com.mvt.mvt_events.metadata;

import com.mvt.mvt_events.metadata.FilterMetadata.FilterOption;
import com.mvt.mvt_events.util.BrazilianBanks;
import com.mvt.mvt_events.validation.ValidBankCode;
import jakarta.persistence.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Extrator genérico de metadata a partir de entidades JPA.
 * Lê anotações @Entity, @Column, @Enumerated, @OneToMany, etc.
 * e gera FieldMetadata e RelationshipMetadata automaticamente.
 */
@Component
public class JpaMetadataExtractor {

    // Mapa de traduções de campos comuns (inglês → português)
    private static final Map<String, String> FIELD_TRANSLATIONS = new HashMap<>();

    static {
        // ==================== CAMPOS BÁSICOS ====================
        FIELD_TRANSLATIONS.put("name", "Nome");
        FIELD_TRANSLATIONS.put("description", "Descrição");
        FIELD_TRANSLATIONS.put("email", "E-mail");
        FIELD_TRANSLATIONS.put("phone", "Telefone");
        FIELD_TRANSLATIONS.put("address", "Endereço");
        FIELD_TRANSLATIONS.put("city", "Cidade");
        FIELD_TRANSLATIONS.put("state", "Estado");
        FIELD_TRANSLATIONS.put("country", "País");
        FIELD_TRANSLATIONS.put("zipCode", "CEP");
        FIELD_TRANSLATIONS.put("postalCode", "CEP");
        FIELD_TRANSLATIONS.put("slug", "URL Amigável");
        FIELD_TRANSLATIONS.put("website", "Website");
        FIELD_TRANSLATIONS.put("notes", "Observações");

        // ==================== DATAS ====================
        FIELD_TRANSLATIONS.put("date", "Data");
        FIELD_TRANSLATIONS.put("eventDate", "Data do Evento");
        FIELD_TRANSLATIONS.put("registrationDate", "Data de Inscrição");
        FIELD_TRANSLATIONS.put("startDate", "Data de Início");
        FIELD_TRANSLATIONS.put("endDate", "Data de Término");
        FIELD_TRANSLATIONS.put("registrationStartDate", "Início das Inscrições");
        FIELD_TRANSLATIONS.put("registrationEndDate", "Fim das Inscrições");
        FIELD_TRANSLATIONS.put("dateOfBirth", "Data de Nascimento");
        FIELD_TRANSLATIONS.put("processedAt", "Processado em");
        FIELD_TRANSLATIONS.put("refundedAt", "Reembolsado em");

        // ==================== EVENT ====================
        FIELD_TRANSLATIONS.put("eventType", "Tipo de Evento");
        FIELD_TRANSLATIONS.put("location", "Local");
        FIELD_TRANSLATIONS.put("maxParticipants", "Máximo de Participantes");
        FIELD_TRANSLATIONS.put("currentParticipants", "Participantes Atuais");
        FIELD_TRANSLATIONS.put("registrationOpen", "Inscrições Abertas");
        FIELD_TRANSLATIONS.put("categories", "Categorias");
        FIELD_TRANSLATIONS.put("currency", "Moeda");
        FIELD_TRANSLATIONS.put("termsAndConditions", "Termos e Condições");
        FIELD_TRANSLATIONS.put("platformFeePercentage", "Taxa da Plataforma (%)");
        FIELD_TRANSLATIONS.put("transferFrequency", "Frequência de Transferência");

        // ==================== EVENT CATEGORY ====================
        FIELD_TRANSLATIONS.put("gender", "Gênero");
        FIELD_TRANSLATIONS.put("minAge", "Idade Mínima");
        FIELD_TRANSLATIONS.put("maxAge", "Idade Máxima");
        FIELD_TRANSLATIONS.put("distance", "Distância");
        FIELD_TRANSLATIONS.put("distanceUnit", "Unidade de Distância");
        FIELD_TRANSLATIONS.put("observations", "Observações");

        // ==================== FINANCEIRO/PAYMENT ====================
        FIELD_TRANSLATIONS.put("price", "Preço");
        FIELD_TRANSLATIONS.put("amount", "Valor");
        FIELD_TRANSLATIONS.put("paymentMethod", "Método de Pagamento");
        FIELD_TRANSLATIONS.put("gatewayProvider", "Gateway de Pagamento");
        FIELD_TRANSLATIONS.put("gatewayPaymentId", "ID do Pagamento");
        FIELD_TRANSLATIONS.put("gatewayFee", "Taxa do Gateway");
        FIELD_TRANSLATIONS.put("gatewayResponse", "Resposta do Gateway");
        FIELD_TRANSLATIONS.put("transactionId", "ID da Transação");
        FIELD_TRANSLATIONS.put("refundAmount", "Valor do Reembolso");
        FIELD_TRANSLATIONS.put("refundReason", "Motivo do Reembolso");

        // ==================== PAGAR.ME / BANK ACCOUNT ====================
        FIELD_TRANSLATIONS.put("pagarmeRecipientId", "ID Recipient Pagar.me");
        FIELD_TRANSLATIONS.put("pagarmeOrderId", "ID Order Pagar.me");
        FIELD_TRANSLATIONS.put("pagarmeStatus", "Status Pagar.me");
        FIELD_TRANSLATIONS.put("pixQrCode", "Código PIX");
        FIELD_TRANSLATIONS.put("pixQrCodeUrl", "QR Code PIX (URL)");
        FIELD_TRANSLATIONS.put("expiresAt", "Expira em");
        FIELD_TRANSLATIONS.put("splitRules", "Regras de Split");
        FIELD_TRANSLATIONS.put("bankAccount", "Conta Bancária");
        FIELD_TRANSLATIONS.put("bankCode", "Código do Banco");
        FIELD_TRANSLATIONS.put("bankName", "Nome do Banco");
        FIELD_TRANSLATIONS.put("agency", "Agência");
        FIELD_TRANSLATIONS.put("agencyDigit", "Dígito da Agência");
        FIELD_TRANSLATIONS.put("accountNumber", "Número da Conta");
        FIELD_TRANSLATIONS.put("accountDigit", "Dígito da Conta");
        FIELD_TRANSLATIONS.put("accountType", "Tipo de Conta");
        FIELD_TRANSLATIONS.put("accountHolderName", "Nome do Titular");
        FIELD_TRANSLATIONS.put("accountHolderDocument", "CPF do Titular");
        FIELD_TRANSLATIONS.put("motherName", "Nome da Mãe");
        FIELD_TRANSLATIONS.put("birthdate", "Data de Nascimento");
        FIELD_TRANSLATIONS.put("monthlyIncome", "Renda Mensal");
        FIELD_TRANSLATIONS.put("professionalOccupation", "Ocupação Profissional");
        FIELD_TRANSLATIONS.put("phoneDdd", "DDD");
        FIELD_TRANSLATIONS.put("phoneNumber", "Número do Telefone");
        FIELD_TRANSLATIONS.put("addressStreet", "Rua");
        FIELD_TRANSLATIONS.put("addressNumber", "Número");
        FIELD_TRANSLATIONS.put("addressComplement", "Complemento");
        FIELD_TRANSLATIONS.put("addressNeighborhood", "Bairro");
        FIELD_TRANSLATIONS.put("addressCity", "Cidade");
        FIELD_TRANSLATIONS.put("addressState", "Estado");
        FIELD_TRANSLATIONS.put("addressZipCode", "CEP");
        FIELD_TRANSLATIONS.put("addressReferencePoint", "Ponto de Referência");
        FIELD_TRANSLATIONS.put("validatedAt", "Validado em");

        // ==================== ADDRESS (nova entidade) ====================
        FIELD_TRANSLATIONS.put("street", "Rua");
        FIELD_TRANSLATIONS.put("number", "Número");
        FIELD_TRANSLATIONS.put("complement", "Complemento");
        FIELD_TRANSLATIONS.put("neighborhood", "Bairro");
        FIELD_TRANSLATIONS.put("referencePoint", "Ponto de Referência");
        FIELD_TRANSLATIONS.put("zipCode", "CEP");
        FIELD_TRANSLATIONS.put("latitude", "Latitude");
        FIELD_TRANSLATIONS.put("longitude", "Longitude");
        FIELD_TRANSLATIONS.put("city", "Cidade");
        FIELD_TRANSLATIONS.put("user", "Usuário");
        FIELD_TRANSLATIONS.put("isDefault", "Padrão");

        // ==================== STATUS/FLAGS ====================
        FIELD_TRANSLATIONS.put("status", "Status");
        FIELD_TRANSLATIONS.put("enabled", "Ativo");
        FIELD_TRANSLATIONS.put("active", "Ativo");

        // ==================== USER ====================
        FIELD_TRANSLATIONS.put("username", "E-mail");
        FIELD_TRANSLATIONS.put("password", "Senha");
        FIELD_TRANSLATIONS.put("role", "Perfil");
        FIELD_TRANSLATIONS.put("cpf", "CPF");
        FIELD_TRANSLATIONS.put("documentNumber", "CPF/CNPJ");
        FIELD_TRANSLATIONS.put("address", "Endereço");
        FIELD_TRANSLATIONS.put("latitude", "Latitude");
        FIELD_TRANSLATIONS.put("longitude", "Longitude");
        FIELD_TRANSLATIONS.put("gpsLatitude", "Latitude GPS");
        FIELD_TRANSLATIONS.put("gpsLongitude", "Longitude GPS");

        // ==================== ORGANIZATION ====================
        FIELD_TRANSLATIONS.put("organization", "Grupo");
        FIELD_TRANSLATIONS.put("contactEmail", "E-mail de Contato");
        FIELD_TRANSLATIONS.put("contactPhone", "Telefone de Contato");
        FIELD_TRANSLATIONS.put("logoUrl", "URL do Logo");
        FIELD_TRANSLATIONS.put("commissionPercentage", "Comissão (%)");

        // ==================== SITE CONFIGURATION ====================
        FIELD_TRANSLATIONS.put("pricePerKm", "Preço por Km (R$)");
        FIELD_TRANSLATIONS.put("minimumShippingFee", "Valor Mínimo do Frete (R$)");
        FIELD_TRANSLATIONS.put("organizerPercentage", "Comissão do Gerente (%)");
        FIELD_TRANSLATIONS.put("platformPercentage", "Comissão da Plataforma (%)");
        FIELD_TRANSLATIONS.put("dangerFeePercentage", "Taxa de Periculosidade (%)");
        FIELD_TRANSLATIONS.put("highIncomeFeePercentage", "Taxa de Renda Alta (%)");
        FIELD_TRANSLATIONS.put("isActive", "Ativo");
        FIELD_TRANSLATIONS.put("updatedBy", "Atualizado por");
        FIELD_TRANSLATIONS.put("notes", "Observações");

        // ==================== SPECIAL ZONES ====================
        FIELD_TRANSLATIONS.put("latitude", "Latitude");
        FIELD_TRANSLATIONS.put("longitude", "Longitude");
        FIELD_TRANSLATIONS.put("address", "Endereço");
        FIELD_TRANSLATIONS.put("zoneType", "Tipo de Zona");
        FIELD_TRANSLATIONS.put("radiusMeters", "Raio (metros)");

        // ==================== ZAPI10 - DELIVERY ====================
        FIELD_TRANSLATIONS.put("client", "Cliente");
        FIELD_TRANSLATIONS.put("courier", "Motoboy");
        FIELD_TRANSLATIONS.put("organizer", "Gerente");
        FIELD_TRANSLATIONS.put("adm", "Gerente");
        FIELD_TRANSLATIONS.put("fromAddress", "Endereço de Origem");
        FIELD_TRANSLATIONS.put("fromLatitude", "Latitude de Origem");
        FIELD_TRANSLATIONS.put("fromLongitude", "Longitude de Origem");
        FIELD_TRANSLATIONS.put("toAddress", "Endereço de Destino");
        FIELD_TRANSLATIONS.put("toLatitude", "Latitude de Destino");
        FIELD_TRANSLATIONS.put("toLongitude", "Longitude de Destino");
        FIELD_TRANSLATIONS.put("distanceKm", "Distância (km)");
        FIELD_TRANSLATIONS.put("estimatedTimeMinutes", "Tempo Estimado (min)");
        FIELD_TRANSLATIONS.put("itemDescription", "Descrição do Item");
        FIELD_TRANSLATIONS.put("recipientName", "Nome do Destinatário");
        FIELD_TRANSLATIONS.put("recipientPhone", "Telefone do Destinatário");
        FIELD_TRANSLATIONS.put("shippingFee", "Valor do Frete");
        FIELD_TRANSLATIONS.put("totalAmount", "Valor Total");
        FIELD_TRANSLATIONS.put("scheduledPickupAt", "Data/Hora Retirada");
        FIELD_TRANSLATIONS.put("acceptedAt", "Aceita em");
        FIELD_TRANSLATIONS.put("pickedUpAt", "Coletada em");
        FIELD_TRANSLATIONS.put("completedAt", "Concluída em");
        FIELD_TRANSLATIONS.put("cancelledAt", "Cancelada em");
        FIELD_TRANSLATIONS.put("cancellationReason", "Motivo do Cancelamento");
        FIELD_TRANSLATIONS.put("partnership", "Parceria Municipal");

        // ==================== ZAPI10 - COURIER PROFILE ====================
        FIELD_TRANSLATIONS.put("vehicleType", "Tipo de Veículo");
        FIELD_TRANSLATIONS.put("vehiclePlate", "Placa do Veículo");
        FIELD_TRANSLATIONS.put("cnh", "CNH");
        FIELD_TRANSLATIONS.put("cnhCategory", "Categoria da CNH");
        FIELD_TRANSLATIONS.put("profileStatus", "Status do Perfil");
        FIELD_TRANSLATIONS.put("approvedAt", "Aprovado em");
        FIELD_TRANSLATIONS.put("totalDeliveries", "Total de Entregas");
        FIELD_TRANSLATIONS.put("averageRating", "Avaliação Média");
        FIELD_TRANSLATIONS.put("isAvailable", "Disponível");
        FIELD_TRANSLATIONS.put("currentLatitude", "Latitude Atual");
        FIELD_TRANSLATIONS.put("currentLongitude", "Longitude Atual");
        FIELD_TRANSLATIONS.put("lastLocationUpdate", "Última Atualização de Localização");

        // ==================== ZAPI10 - CONTRACTS ====================
        FIELD_TRANSLATIONS.put("employmentContracts", "Contratos de Motoboy");
        FIELD_TRANSLATIONS.put("contracts", "Contratos de Serviço");
        FIELD_TRANSLATIONS.put("clientContracts", "Contratos de Serviço");

        // Campos específicos de EmploymentContract
        FIELD_TRANSLATIONS.put("linkedAt", "Vinculado em");
        FIELD_TRANSLATIONS.put("isActive", "Ativo");

        // Campos específicos de Contract
        FIELD_TRANSLATIONS.put("isPrimary", "Contrato Principal");
        FIELD_TRANSLATIONS.put("contractDate", "Data do Contrato");
        FIELD_TRANSLATIONS.put("client", "Cliente");
        FIELD_TRANSLATIONS.put("organization", "Grupo");

        // ==================== ZAPI10 - EVALUATION ====================
        FIELD_TRANSLATIONS.put("delivery", "Entrega");
        FIELD_TRANSLATIONS.put("evaluator", "Avaliador");
        FIELD_TRANSLATIONS.put("evaluated", "Avaliado");
        FIELD_TRANSLATIONS.put("rating", "Nota");
        FIELD_TRANSLATIONS.put("comment", "Comentário");

        // ==================== ZAPI10 - MUNICIPAL PARTNERSHIP ====================
        FIELD_TRANSLATIONS.put("partnershipName", "Nome da Parceria");
        FIELD_TRANSLATIONS.put("municipality", "Município");
        FIELD_TRANSLATIONS.put("startDate", "Data de Início");
        FIELD_TRANSLATIONS.put("endDate", "Data de Término");
        FIELD_TRANSLATIONS.put("discountPercentage", "Desconto (%)");
        FIELD_TRANSLATIONS.put("active", "Ativo");

        // ==================== ZAPI10 - PAYOUT ====================
        FIELD_TRANSLATIONS.put("payoutType", "Tipo de Repasse");
        FIELD_TRANSLATIONS.put("totalPayout", "Valor Total");
        FIELD_TRANSLATIONS.put("netAmount", "Valor Líquido");
        FIELD_TRANSLATIONS.put("platformFee", "Taxa da Plataforma");
        FIELD_TRANSLATIONS.put("processedAt", "Processado em");
        FIELD_TRANSLATIONS.put("recipient", "Destinatário");

        // ==================== RELACIONAMENTOS ====================
        FIELD_TRANSLATIONS.put("event", "Evento");
        FIELD_TRANSLATIONS.put("user", "Usuário");
        FIELD_TRANSLATIONS.put("registration", "Inscrição");
        FIELD_TRANSLATIONS.put("payments", "Pagamentos");
        FIELD_TRANSLATIONS.put("category", "Categoria");
    }

    // Mapa de traduções para valores de ENUMS
    private static final Map<String, String> ENUM_TRANSLATIONS = new HashMap<>();

    static {
        // ==================== EVENT TYPE ====================
        ENUM_TRANSLATIONS.put("RUNNING", "Corrida");
        ENUM_TRANSLATIONS.put("CYCLING", "Ciclismo");
        ENUM_TRANSLATIONS.put("TRIATHLON", "Triatlo");
        ENUM_TRANSLATIONS.put("SWIMMING", "Natação");
        ENUM_TRANSLATIONS.put("WALKING", "Caminhada");
        ENUM_TRANSLATIONS.put("TRAIL_RUNNING", "Trail Running");
        ENUM_TRANSLATIONS.put("MOUNTAIN_BIKING", "Mountain Bike");
        ENUM_TRANSLATIONS.put("ROAD_CYCLING", "Ciclismo de Estrada");
        ENUM_TRANSLATIONS.put("MARATHON", "Maratona");
        ENUM_TRANSLATIONS.put("HALF_MARATHON", "Meia Maratona");
        ENUM_TRANSLATIONS.put("ULTRA_MARATHON", "Ultra Maratona");
        ENUM_TRANSLATIONS.put("OBSTACLE_RACE", "Corrida de Obstáculos");
        ENUM_TRANSLATIONS.put("DUATHLON", "Duatlo");
        ENUM_TRANSLATIONS.put("HIKING", "Caminhada");
        ENUM_TRANSLATIONS.put("ADVENTURE_RACE", "Corrida de Aventura");

        // ==================== STATUS (Event, Payment, Registration)
        // ====================
        ENUM_TRANSLATIONS.put("DRAFT", "Rascunho");
        ENUM_TRANSLATIONS.put("PUBLISHED", "Publicado");
        ENUM_TRANSLATIONS.put("CANCELLED", "Cancelado");
        ENUM_TRANSLATIONS.put("COMPLETED", "Pago");
        ENUM_TRANSLATIONS.put("PENDING", "Pendente");
        ENUM_TRANSLATIONS.put("ACTIVE", "Ativa");
        ENUM_TRANSLATIONS.put("PROCESSING", "Processando");
        ENUM_TRANSLATIONS.put("FAILED", "Falhou");
        ENUM_TRANSLATIONS.put("REFUNDED", "Reembolsado");
        ENUM_TRANSLATIONS.put("EXPIRED", "Expirado");

        // ==================== GENDER ====================
        ENUM_TRANSLATIONS.put("MALE", "Masculino");
        ENUM_TRANSLATIONS.put("FEMALE", "Feminino");
        ENUM_TRANSLATIONS.put("MIXED", "Misto");
        ENUM_TRANSLATIONS.put("OTHER", "Outro");

        // ==================== DISTANCE UNIT ====================
        ENUM_TRANSLATIONS.put("KM", "Quilômetros (km)");
        ENUM_TRANSLATIONS.put("MILES", "Milhas (mi)");
        ENUM_TRANSLATIONS.put("METERS", "Metros (m)");

        // ==================== TRANSFER FREQUENCY ====================
        ENUM_TRANSLATIONS.put("IMMEDIATE", "Imediato");
        ENUM_TRANSLATIONS.put("DAILY", "Diário");
        ENUM_TRANSLATIONS.put("WEEKLY", "Semanal");
        ENUM_TRANSLATIONS.put("MONTHLY", "Mensal");
        ENUM_TRANSLATIONS.put("ON_DEMAND", "Sob Demanda");

        // ==================== PAYMENT METHOD ====================
        ENUM_TRANSLATIONS.put("CREDIT_CARD", "Cartão de Crédito");
        ENUM_TRANSLATIONS.put("DEBIT_CARD", "Cartão de Débito");
        ENUM_TRANSLATIONS.put("PIX", "PIX");
        ENUM_TRANSLATIONS.put("BANK_TRANSFER", "Transferência Bancária");
        ENUM_TRANSLATIONS.put("PAYPAL_ACCOUNT", "Conta PayPal");
        ENUM_TRANSLATIONS.put("CASH", "Dinheiro");

        // ==================== ACCOUNT TYPE ====================
        ENUM_TRANSLATIONS.put("CHECKING", "Conta Corrente");
        ENUM_TRANSLATIONS.put("SAVINGS", "Conta Poupança");

        // ==================== BANK ACCOUNT STATUS ====================
        ENUM_TRANSLATIONS.put("PENDING_VALIDATION", "Pendente de Validação");
        ENUM_TRANSLATIONS.put("ACTIVE", "Ativa");
        ENUM_TRANSLATIONS.put("BLOCKED", "Bloqueada");
        // CANCELLED já existe na seção STATUS acima

        // ==================== ROLE ====================
        ENUM_TRANSLATIONS.put("USER", "Usuário");
        ENUM_TRANSLATIONS.put("ORGANIZER", "Gerente ADM");
        ENUM_TRANSLATIONS.put("ADMIN", "Administrador");
        ENUM_TRANSLATIONS.put("CLIENT", "Cliente");
        ENUM_TRANSLATIONS.put("COURIER", "Motoboy");

        // ==================== ORGANIZATION STATUS ====================
        ENUM_TRANSLATIONS.put("INACTIVE", "Inativo");
        ENUM_TRANSLATIONS.put("SUSPENDED", "Suspenso");
        // ACTIVE já existe na seção STATUS acima

        // ==================== ZAPI10 - DELIVERY STATUS (feminino: "Entrega")
        // ====================
        ENUM_TRANSLATIONS.put("ACCEPTED", "Aceita");
        ENUM_TRANSLATIONS.put("PICKED_UP", "Coletada");
        ENUM_TRANSLATIONS.put("IN_TRANSIT", "Em Trânsito");
        // Nota: PENDING, COMPLETED e CANCELLED já estão definidos na seção STATUS genérica
        // Para Delivery: COMPLETED = "Pago" (compartilhado com Payment)
        // Se precisar de traduções diferentes por contexto, seria necessário refatorar o sistema

        // ==================== ZAPI10 - VEHICLE TYPE ====================
        ENUM_TRANSLATIONS.put("MOTORCYCLE", "Moto");
        ENUM_TRANSLATIONS.put("BICYCLE", "Bicicleta");
        ENUM_TRANSLATIONS.put("CAR", "Carro");
        ENUM_TRANSLATIONS.put("SCOOTER", "Patinete/Scooter");
        ENUM_TRANSLATIONS.put("ON_FOOT", "A Pé");

        // ==================== SPECIAL ZONE TYPE ====================
        ENUM_TRANSLATIONS.put("DANGER", "Zona Perigosa");
        ENUM_TRANSLATIONS.put("HIGH_INCOME", "Zona de Alta Renda");

        // ==================== ZAPI10 - COURIER STATUS ====================
        ENUM_TRANSLATIONS.put("AVAILABLE", "Disponível");
        ENUM_TRANSLATIONS.put("ON_DELIVERY", "Em Entrega");
        ENUM_TRANSLATIONS.put("OFFLINE", "Offline");
        // SUSPENDED já existe acima

        // ==================== ZAPI10 - CONTRACT STATUS ====================
        // ACTIVE já existe na seção STATUS acima
        // SUSPENDED já existe na seção ORGANIZATION STATUS acima
        // CANCELLED já existe na seção DELIVERY STATUS acima

        // ==================== GENERIC ====================
        ENUM_TRANSLATIONS.put("OTHER", "Outro");
    }

    /**
     * Extrai todos os campos de uma entidade JPA
     */
    public List<FieldMetadata> extractFields(Class<?> entityClass) {
        List<FieldMetadata> fields = new ArrayList<>();

        // Processa todos os campos da classe e superclasses
        getAllFields(entityClass).forEach(field -> {
            // Ignora apenas campos anotados com @Transient
            if (field.isAnnotationPresent(Transient.class)) {
                return;
            }

            FieldMetadata metadata = createFieldMetadata(field, entityClass);
            if (metadata != null) {
                fields.add(metadata);
            }
        });

        return fields;
    }

    /**
     * Cria FieldMetadata a partir de um Field da entidade
     */
    private FieldMetadata createFieldMetadata(Field field, Class<?> entityClass) {
        String fieldName = field.getName();

        // ✅ OCULTA campos de sistema
        if (isSystemField(fieldName)) {
            return null;
        }

        String label = extractLabel(field);
        String type = determineFieldType(field);

        // Se for OneToMany, cria metadata de relacionamento
        if (field.isAnnotationPresent(OneToMany.class)) {
            return createRelationshipField(field);
        }

        // Se for ManyToOne ou OneToOne, cria metadata de relacionamento simples
        if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
            return createManyToOneField(field);
        }

        FieldMetadata metadata = new FieldMetadata(fieldName, label, type);

        // Extrai validações da anotação @Column
        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            metadata.setRequired(!column.nullable());

            if (column.length() > 0 && type.equals("string")) {
                metadata.setMaxLength(column.length());
            }

            if (field.getType().equals(BigDecimal.class) && column.precision() > 0) {
                // Define max baseado na precisão
                metadata.setMax(Math.pow(10, column.precision() - column.scale()) - 1);
            }
        }

        // Extrai validações da anotação @Size (Bean Validation)
        if (field.isAnnotationPresent(jakarta.validation.constraints.Size.class)) {
            jakarta.validation.constraints.Size size = field.getAnnotation(jakarta.validation.constraints.Size.class);
            if (size.max() > 0) {
                metadata.setMaxLength(size.max());
            }
            if (size.min() > 0) {
                metadata.setMinLength(size.min());
            }
        }

        // Extrai validações da anotação @Max (Bean Validation)
        if (field.isAnnotationPresent(jakarta.validation.constraints.Max.class)) {
            jakarta.validation.constraints.Max max = field.getAnnotation(jakarta.validation.constraints.Max.class);
            metadata.setMax((double) max.value());
        }

        // Extrai validações da anotação @Min (Bean Validation)
        if (field.isAnnotationPresent(jakarta.validation.constraints.Min.class)) {
            jakarta.validation.constraints.Min min = field.getAnnotation(jakarta.validation.constraints.Min.class);
            metadata.setMin((double) min.value());
        }

        // Define width automaticamente baseado no tipo e tamanho
        metadata.setWidth(calculateWidth(field, type, metadata.getMaxLength()));

        // Define align baseado no tipo
        if (type.equals("number") || type.equals("currency")) {
            metadata.setAlign("right");
        } else if (type.equals("boolean") || type.equals("date") || type.equals("select")) {
            metadata.setAlign("center");
        }

        // Define format para tipos específicos
        if (type.equals("date")) {
            metadata.setFormat("dd/MM/yyyy");
        } else if (type.equals("datetime")) {
            metadata.setFormat("dd/MM/yyyy HH:mm");
        } else if (type.equals("currency")) {
            metadata.setFormat("currency");
        }

        // Se for ENUM, extrai as opções
        if (field.isAnnotationPresent(Enumerated.class) && field.getType().isEnum()) {
            metadata.setType("select"); // Enum sempre é select
            metadata.setOptions(extractEnumOptions(field.getType()));
        }

        // ✅ Se campo tem @ValidBankCode, transforma em select com lista de bancos
        if (field.isAnnotationPresent(ValidBankCode.class)) {
            metadata.setType("select"); // Campo de banco vira select
            metadata.setOptions(extractBankOptions());
        }

        // ✅ Verifica anotação @Visible (será processada pelo MetadataService para cada
        // contexto)
        // Aqui apenas armazenamos os flags para uso posterior
        if (field.isAnnotationPresent(Visible.class)) {
            Visible visible = field.getAnnotation(Visible.class);

            // Armazena os flags para processamento posterior
            metadata.setHiddenFromForm(!visible.form());
            metadata.setHiddenFromTable(!visible.table());
            metadata.setHiddenFromFilter(!visible.filter());

            // Define readonly se especificado
            if (visible.readonly()) {
                metadata.setReadonly(true);
            }
        }

        // ✅ Extrai valor default do campo
        Object defaultValue = extractDefaultValue(entityClass, field);
        if (defaultValue != null) {
            metadata.setDefaultValue(defaultValue);
        }

        // ✅ Verifica anotação @Computed para campos computados
        if (field.isAnnotationPresent(Computed.class)) {
            Computed computed = field.getAnnotation(Computed.class);
            metadata.setComputed(computed.function());
            metadata.setComputedDependencies(java.util.Arrays.asList(computed.dependencies()));
        }

        return metadata;
    }

    /**
     * Calcula width ideal para a coluna da tabela baseado no tipo e tamanho
     */
    private Integer calculateWidth(Field field, String type, Integer maxLength) {
        // Se for @DisplayLabel ou campo principal, usa width maior
        if (field.isAnnotationPresent(DisplayLabel.class) || "name".equals(field.getName())) {
            return 200;
        }

        switch (type) {
            case "boolean":
                return 80;
            case "date":
                return 120;
            case "datetime":
                return 160;
            case "number":
                return 100;
            case "currency":
                return 120;
            case "select":
                return 120;
            case "string":
                // Para strings, baseamos no maxLength
                if (maxLength != null) {
                    if (maxLength <= 50)
                        return 120;
                    if (maxLength <= 100)
                        return 150;
                    if (maxLength <= 255)
                        return 180;
                    return 200;
                }
                return 150; // default para strings sem maxLength
            default:
                return 150;
        }
    }

    /**
     * Extrai o valor default de um campo instanciando a entidade
     */
    private Object extractDefaultValue(Class<?> entityClass, Field field) {
        try {
            // Instancia a entidade usando o construtor padrão
            Object instance = entityClass.getDeclaredConstructor().newInstance();

            // Torna o campo acessível
            field.setAccessible(true);

            // Lê o valor default do campo
            Object value = field.get(instance);

            // Se for enum, retorna o nome do enum (não o objeto)
            if (value != null && value.getClass().isEnum()) {
                return ((Enum<?>) value).name();
            }

            return value;
        } catch (Exception e) {
            // Se não conseguir instanciar ou ler o campo, retorna null
            return null;
        }
    }

    /**
     * Cria FieldMetadata para relacionamento OneToMany
     */
    private FieldMetadata createRelationshipField(Field field) {
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        String fieldName = field.getName();
        String label = extractLabel(field);

        // Determina a entidade alvo do relacionamento
        Class<?> targetEntity = getTargetEntity(field, oneToMany);
        if (targetEntity == null) {
            return null;
        }

        String targetEntityName = toEntityName(targetEntity.getSimpleName());
        String targetEndpoint = "/api/" + toKebabCase(field.getName());

        RelationshipMetadata relationship = new RelationshipMetadata(
                "ONE_TO_MANY",
                targetEntityName,
                targetEndpoint);

        // Extrai cascade e orphanRemoval
        relationship.setCascade(oneToMany.cascade().length > 0);
        relationship.setOrphanRemoval(oneToMany.orphanRemoval());

        // Extrai campos da entidade alvo recursivamente
        relationship.setFields(extractFields(targetEntity));

        FieldMetadata metadata = new FieldMetadata(fieldName, label, "nested");

        // IMPORTANTE: Respeitar anotação @Visible se presente
        Visible visibleAnnotation = field.getAnnotation(Visible.class);
        if (visibleAnnotation != null) {
            // Setar propriedades baseado em @Visible
            metadata.setVisible(visibleAnnotation.table()); // Visibilidade inicial baseada em table
            metadata.setHiddenFromTable(!visibleAnnotation.table()); // Inverso de table()
            metadata.setHiddenFromForm(!visibleAnnotation.form()); // Inverso de form()
            metadata.setHiddenFromFilter(!visibleAnnotation.filter()); // Inverso de filter()
        } else {
            // Default: relacionamentos não aparecem na tabela
            metadata.setVisible(false);
            metadata.setHiddenFromTable(true); // Esconder por padrão da tabela
            metadata.setHiddenFromForm(false); // Mostrar por padrão no formulário
            metadata.setHiddenFromFilter(true); // Esconder por padrão dos filtros
        }

        metadata.setSortable(false);
        metadata.setSearchable(false);
        metadata.setRelationship(relationship);

        return metadata;
    }

    /**
     * Cria FieldMetadata para relacionamento ManyToOne ou OneToOne
     */
    private FieldMetadata createManyToOneField(Field field) {
        String fieldName = field.getName();
        String label = extractLabel(field);
        Class<?> targetEntity = field.getType();

        // Cria metadata como tipo "entity" para exibir na tabela e formulário
        FieldMetadata metadata = new FieldMetadata(fieldName, label, "entity");

        // Configuração do relacionamento
        String targetEntityName = toEntityName(targetEntity.getSimpleName());
        String targetEndpoint = "/api/" + toKebabCase(toPlural(targetEntity.getSimpleName()));

        RelationshipMetadata relationship = new RelationshipMetadata(
                "MANY_TO_ONE",
                targetEntityName,
                targetEndpoint);

        metadata.setRelationship(relationship);

        // Configuração para exibição na tabela
        metadata.setVisible(true);
        metadata.setSortable(true);
        metadata.setSearchable(true);
        metadata.setWidth(150);
        metadata.setAlign("left");

        // Verifica se é obrigatório
        if (field.isAnnotationPresent(JoinColumn.class)) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            metadata.setRequired(!joinColumn.nullable());
        }

        // ✅ Verifica anotação @Visible também para relacionamentos
        if (field.isAnnotationPresent(Visible.class)) {
            Visible visible = field.getAnnotation(Visible.class);

            // Armazena os flags para processamento posterior
            metadata.setHiddenFromForm(!visible.form());
            metadata.setHiddenFromTable(!visible.table());
            metadata.setHiddenFromFilter(!visible.filter());

            // Define readonly se especificado
            if (visible.readonly()) {
                metadata.setReadonly(true);
            }
        }

        return metadata;
    }

    /**
     * Determina o tipo do campo baseado no tipo Java
     */
    private String determineFieldType(Field field) {
        Class<?> type = field.getType();

        if (type.equals(String.class)) {
            // Verifica se é TEXT (textarea)
            if (field.isAnnotationPresent(Column.class)) {
                Column col = field.getAnnotation(Column.class);
                if (col.columnDefinition().toUpperCase().contains("TEXT")) {
                    return "textarea";
                }
            }
            return "string";
        }

        if (type.equals(Integer.class) || type.equals(int.class) ||
                type.equals(Long.class) || type.equals(long.class)) {
            return "number";
        }

        if (type.equals(BigDecimal.class)) {
            // Verifica se é moeda (campo com "price" ou "valor" no nome)
            String fieldName = field.getName().toLowerCase();
            if (fieldName.contains("price") || fieldName.contains("valor")) {
                return "currency";
            }
            return "number";
        }

        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return "boolean";
        }

        if (type.equals(LocalDate.class)) {
            return "date";
        }

        if (type.equals(LocalDateTime.class)) {
            return "datetime";
        }

        if (type.isEnum()) {
            return "select"; // Enums são sempre select
        }

        return "string"; // Fallback
    }

    /**
     * Extrai o label do campo (traduzido para português)
     */
    private String extractLabel(Field field) {
        String fieldName = field.getName();

        // ✅ Usa tradução se disponível
        if (FIELD_TRANSLATIONS.containsKey(fieldName)) {
            return FIELD_TRANSLATIONS.get(fieldName);
        }

        // Fallback: converte camelCase para "Título Capitalizado"
        return toTitleCase(fieldName);
    }

    /**
     * Extrai opções de um Enum com seus displayNames traduzidos
     */
    private List<FilterOption> extractEnumOptions(Class<?> enumClass) {
        List<FilterOption> options = new ArrayList<>();

        for (Object enumConstant : enumClass.getEnumConstants()) {
            String value = ((Enum<?>) enumConstant).name();
            String label;

            // 1️⃣ Primeiro verifica o mapa de traduções (prioridade máxima)
            if (ENUM_TRANSLATIONS.containsKey(value)) {
                label = ENUM_TRANSLATIONS.get(value);
            } else {
                // 2️⃣ Se não tiver tradução, tenta buscar método getDisplayName() no enum
                try {
                    Method getDisplayName = enumClass.getMethod("getDisplayName");
                    label = (String) getDisplayName.invoke(enumConstant);
                } catch (Exception e) {
                    // 3️⃣ Fallback: converte nome para title case
                    label = toTitleCase(value);
                }
            }

            // ✅ CORRIGIDO: FilterOption(label, value) - label PRIMEIRO!
            options.add(new FilterOption(label, value));
        }

        return options;
    }

    /**
     * Extrai opções de bancos brasileiros para campos com @ValidBankCode.
     * Retorna lista de FilterOption com código e nome de ~50 bancos.
     */
    private List<FilterOption> extractBankOptions() {
        List<FilterOption> options = new ArrayList<>();

        // Itera sobre o mapa de bancos (LinkedHashMap mantém ordem de inserção)
        for (Map.Entry<String, String> entry : BrazilianBanks.getAllBanks().entrySet()) {
            String code = entry.getKey();
            String name = entry.getValue();
            
            // Label: "001 - Banco do Brasil"
            // Value: "001"
            String label = code + " - " + name;
            options.add(new FilterOption(label, code));
        }

        return options;
    }

    /**
     * Obtém a entidade alvo de um relacionamento
     */
    private Class<?> getTargetEntity(Field field, OneToMany annotation) {
        // Primeiro tenta pegar do targetEntity explícito
        if (!annotation.targetEntity().equals(void.class)) {
            return annotation.targetEntity();
        }

        // Tenta extrair do tipo genérico (List<EventCategory>)
        try {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) field
                    .getGenericType();
            return (Class<?>) paramType.getActualTypeArguments()[0];
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtém todos os campos incluindo de superclasses
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }

        return fields;
    }

    /**
     * Converte "EventCategory" para "eventCategory"
     */
    private String toEntityName(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /**
     * Converte "categories" para "categories" (kebab-case)
     */
    private String toKebabCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    /**
     * Converte "eventType" para "Event Type" ou "USER" para "User"
     */
    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Se está todo em MAIÚSCULAS (PENDING, USER, etc), converte para Title Case
        if (input.equals(input.toUpperCase())) {
            return input.charAt(0) + input.substring(1).toLowerCase();
        }

        // Se é camelCase (eventType), adiciona espaços antes de maiúsculas
        String withSpaces = input.replaceAll("([A-Z])", " $1");
        return withSpaces.substring(0, 1).toUpperCase() + withSpaces.substring(1);
    }

    /**
     * Verifica se é um campo de sistema que não deve aparecer em formulários
     */
    private boolean isSystemField(String fieldName) {
        return fieldName.equals("id")
                || fieldName.equals("createdAt")
                || fieldName.equals("updatedAt")
                || fieldName.equals("createdDate")
                || fieldName.equals("lastModifiedDate")
                || fieldName.equals("tenantId");
    }

    /**
     * Extrai filtros automaticamente a partir dos campos da entidade JPA.
     * Cria filtros baseados no tipo de campo e anotações.
     */
    public List<FilterMetadata> extractFilters(Class<?> entityClass) {
        List<FilterMetadata> filters = new ArrayList<>();
        Field[] fields = entityClass.getDeclaredFields();

        for (Field field : fields) {
            // Ignora campos de sistema e relacionamentos OneToMany/OneToOne lazy
            if (isSystemField(field.getName()) || field.isAnnotationPresent(OneToMany.class)) {
                continue;
            }

            // Ignora campos @Transient
            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }

            FilterMetadata filter = createFilterFromField(field);
            if (filter != null) {
                filters.add(filter);
            }
        }

        return filters;
    }

    /**
     * Cria um FilterMetadata a partir de um campo JPA
     */
    private FilterMetadata createFilterFromField(Field field) {
        String fieldName = field.getName();
        String label = extractLabel(field);
        Class<?> fieldType = field.getType();

        FilterMetadata filter = new FilterMetadata();
        filter.setName(fieldName);
        filter.setLabel(label);
        filter.setField(fieldName);

        // Determina o tipo do filtro baseado no tipo do campo

        // 1. ENUM → Select com opções
        if (fieldType.isEnum()) {
            filter.setType("select");
            filter.setOptions(extractEnumOptions(fieldType));
            return filter;
        }

        // 2. BOOLEAN → Boolean select (Sim/Não/Todos)
        if (fieldType == Boolean.class || fieldType == boolean.class) {
            filter.setType("boolean");
            return filter;
        }

        // 3. DATA → Date picker
        if (fieldType == LocalDate.class) {
            filter.setType("date");
            filter.setPlaceholder("Selecione a data");
            return filter;
        }

        // 4. DATA/HORA → DateTime picker
        if (fieldType == LocalDateTime.class) {
            filter.setType("datetime");
            filter.setPlaceholder("Selecione data e hora");
            return filter;
        }

        // 5. NÚMERO → Number input
        if (fieldType == Integer.class || fieldType == int.class ||
                fieldType == Long.class || fieldType == long.class) {
            filter.setType("number");
            filter.setPlaceholder("Digite um número");
            return filter;
        }

        // 6. DECIMAL/MOEDA → Number input
        if (fieldType == BigDecimal.class || fieldType == Double.class || fieldType == double.class) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.name().toLowerCase().contains("price")) {
                filter.setType("currency");
                filter.setPlaceholder("R$ 0,00");
            } else {
                filter.setType("number");
                filter.setPlaceholder("Digite um valor");
            }
            return filter;
        }

        // 7. RELACIONAMENTO @ManyToOne → Entity filter (typeahead/select)
        if (field.isAnnotationPresent(ManyToOne.class)) {
            Class<?> targetEntity = field.getType();

            filter.setType("entity");
            filter.setField(fieldName + ".id"); // Filtra pelo ID da entidade relacionada

            EntityFilterConfig entityConfig = EntityFilterConfig.builder()
                    .entityName(toEntityName(targetEntity.getSimpleName()))
                    .endpoint("/api/" + toKebabCase(toPlural(targetEntity.getSimpleName())))
                    .labelField("name") // Assume que a entidade tem campo "name"
                    .valueField("id")
                    .renderAs("typeahead") // Typeahead para busca dinâmica
                    .searchable(true)
                    .searchPlaceholder("Digite para buscar...")
                    .build();

            filter.setEntityConfig(entityConfig);
            return filter;
        }

        // 8. STRING → Text input
        if (fieldType == String.class) {
            Column column = field.getAnnotation(Column.class);

            // Se é um campo único (slug, email), não é bom para filtro
            if (column != null && column.unique()) {
                return null; // Não adiciona filtro para campos únicos
            }

            // Se é um campo de texto longo (TEXT), não adiciona filtro
            if (column != null && column.columnDefinition() != null &&
                    column.columnDefinition().toUpperCase().contains("TEXT")) {
                return null;
            }

            filter.setType("text");

            // Define placeholder personalizado baseado no nome do campo
            String placeholder = "Buscar por " + label.toLowerCase() + "...";
            filter.setPlaceholder(placeholder);

            return filter;
        }

        return null; // Tipo não suportado para filtro
    }

    /**
     * Converte nome de classe para plural (aproximado)
     */
    private String toPlural(String className) {
        if (className.endsWith("y")) {
            return className.substring(0, className.length() - 1) + "ies";
        }
        if (className.endsWith("s")) {
            return className + "es";
        }
        return className + "s";
    }
}
