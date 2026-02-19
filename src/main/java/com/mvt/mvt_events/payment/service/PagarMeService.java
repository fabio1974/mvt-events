package com.mvt.mvt_events.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.jpa.Address;
import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.config.PagarMeConfig;
import com.mvt.mvt_events.payment.dto.*;
import com.mvt.mvt_events.payment.exception.PaymentProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.List;

/**
 * Serviço de integração com gateway de pagamento Pagar.me
 * 
 * Funcionalidades:
 * - Criar recipients (subcontas) para couriers e managers
 * - Criar orders com PIX e split automático (87% courier, 5% manager, 8% plataforma)
 * - Validar webhooks com HMAC SHA256
 * 
 * @see <a href="https://docs.pagar.me/reference/API-overview">Documentação Pagar.me</a>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PagarMeService {

    private final PagarMeConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Lista todos os recipients cadastrados no Pagar.me
     * 
     * @return Lista de recipients
     */
    public List<RecipientResponse> listRecipients() {
        log.info("🔍 Listando recipients no Pagar.me");

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = config.getApi().getUrl() + "/recipients";
            ResponseEntity<RecipientListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RecipientListResponse.class
            );

            RecipientListResponse recipientList = response.getBody();
            if (recipientList != null && recipientList.getData() != null) {
                log.info("   └─ ✅ {} recipients encontrados", recipientList.getData().size());
                return recipientList.getData();
            }

            return List.of();

        } catch (Exception e) {
            log.error("❌ Erro ao listar recipients no Pagar.me", e);
            throw new RuntimeException("Falha ao listar recipients: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se já existe um recipient com o mesmo CPF E dados bancários
     * 
     * IMPORTANTE: Verifica duplicidade por CPF + banco + agência + conta.
     * Isso permite que o mesmo CPF tenha múltiplos recipients se usar contas diferentes.
     * 
     * @param document CPF/CNPJ sem pontuação
     * @param bankCode Código do banco (ex: "001")
     * @param agency Agência sem dígito verificador
     * @param accountNumber Conta com dígito (ex: "12345-6")
     * @return RecipientResponse se encontrado, null caso contrário
     */
    public RecipientResponse findDuplicateRecipient(String document, String bankCode, String agency, String accountNumber) {
        log.info("🔍 Verificando duplicidade de recipient por CPF: {} e dados bancários: Banco={}, Ag={}, Conta={}", 
            document, bankCode, agency, accountNumber);

        List<RecipientResponse> recipients = listRecipients();

        for (RecipientResponse recipient : recipients) {
            // Comparar CPF E dados bancários
            if (document.equals(recipient.getDocument()) && 
                recipient.getDefaultBankAccount() != null) {
                
                String recBankCode = recipient.getDefaultBankAccount().getBank();
                String recAgency = recipient.getDefaultBankAccount().getBranchNumber();
                String recAccount = recipient.getDefaultBankAccount().getAccountNumber();
                
                // Remove hífen e espaços da conta para comparar apenas números
                String normalizedAccount = accountNumber != null ? accountNumber.replaceAll("[-\\s]", "") : "";
                String normalizedRecAccount = recAccount != null ? recAccount.replaceAll("[-\\s]", "") : "";
                
                if (bankCode.equals(recBankCode) && 
                    agency.equals(recAgency) && 
                    normalizedAccount.equals(normalizedRecAccount)) {
                    
                    log.warn("   └─ ⚠️ DUPLICADO! Recipient existente: {} (CPF: {}, Banco: {}, Ag: {}, Conta: {})", 
                        recipient.getId(), recipient.getDocument(), recBankCode, recAgency, recAccount);
                    return recipient;
                }
            }
        }

        log.info("   └─ ✅ Nenhum recipient duplicado encontrado");
        return null;
    }

    /**
     * Verifica se já existe um recipient cadastrado no Pagar.me com o mesmo CPF/CNPJ
     * (independente dos dados bancários).
     * 
     * Esta verificação é mais restritiva e impede que o mesmo CPF/CNPJ tenha múltiplos
     * recipients, mesmo com contas bancárias diferentes.
     * 
     * @param document CPF/CNPJ sem pontuação
     * @return RecipientResponse se encontrado, null caso contrário
     */
    public RecipientResponse findRecipientByDocument(String document) {
        log.info("🔍 Verificando se existe recipient com CPF/CNPJ: {}", document);

        List<RecipientResponse> recipients = listRecipients();

        for (RecipientResponse recipient : recipients) {
            if (document.equals(recipient.getDocument())) {
                log.warn("   └─ ⚠️ ENCONTRADO! Recipient existente: {} (CPF: {}, Email: {})", 
                    recipient.getId(), recipient.getDocument(), recipient.getEmail());
                return recipient;
            }
        }

        log.info("   └─ ✅ Nenhum recipient encontrado com este CPF/CNPJ");
        return null;
    }

    /**
     * Cria um recipient (subconta) no Pagar.me
     * 
     * Envia dados mínimos obrigatórios + dados opcionais do User (se disponíveis)
     * para permitir que o recipient faça saques.
     * 
     * @param user Usuário com dados pessoais (nome, CPF, email, telefone, endereço, data nascimento)
     * @param bankAccount Dados bancários
     * @return ID do recipient criado
     */
    public String createRecipient(User user, BankAccount bankAccount) {
        return createRecipient(user, bankAccount, "Daily", 0); // Default: transferência automática diária
    }
    
    public String createRecipient(User user, BankAccount bankAccount, String transferInterval, Integer transferDay) {
        String name = user.getName();
        String email = user.getUsername();
        String document = user.getDocumentClean();
        
        // Validar e normalizar intervalo
        if (transferInterval == null || transferInterval.isBlank()) {
            transferInterval = "Daily";
        }
        // Validar transferDay conforme o intervalo
        if (transferDay == null) {
            transferDay = 0;
        }
        // Weekly: Pagar.me aceita apenas 1-5 (segunda a sexta-feira)
        // 0=Domingo → 1=Segunda, 6=Sábado → 5=Sexta
        if ("Weekly".equalsIgnoreCase(transferInterval)) {
            if (transferDay == 0 || transferDay == 6) {
                // Fim de semana não é aceito, usar segunda-feira
                transferDay = 1;
                log.warn("   ⚠️ Dia {} não permitido para Weekly (Pagar.me aceita 1-5). Usando 1 (segunda-feira)", transferDay);
            } else if (transferDay < 1 || transferDay > 5) {
                transferDay = 1; // Default: segunda-feira
                log.warn("   ⚠️ Dia inválido para Weekly. Usando 1 (segunda-feira)");
            }
        }
        if ("Monthly".equalsIgnoreCase(transferInterval) && (transferDay < 1 || transferDay > 31)) {
            transferDay = 1; // Default: dia 1
        }
        
        log.info("🏦 Criando recipient no Pagar.me: {} ({})", name, document);
        log.info("   ├─ Transferência automática: ✅ Habilitada");
        log.info("   ├─ Intervalo: {} | Dia: {}", transferInterval, transferDay);

        // Builder do request com dados obrigatórios
        RecipientRequest.RecipientRequestBuilder requestBuilder = RecipientRequest.builder()
                .name(name)
                .email(email)
                .document(document)
                .type(document.length() == 11 ? "INDIVIDUAL" : "COMPANY")
                .description("Recipient para " + name)
                .defaultBankAccount(RecipientRequest.DefaultBankAccount.builder()
                        .holderName(name)
                        .holderType("individual")
                        .holderDocument(document)
                        .bank(bankAccount.getBankCode())
                        .branchNumber(bankAccount.getAgency())
                        .branchCheckDigit(bankAccount.getAgencyDigit() != null ? bankAccount.getAgencyDigit() : "")
                        .accountNumber(bankAccount.getAccountNumber())
                        .accountCheckDigit(bankAccount.getAccountDigit())
                        .type(bankAccount.getAccountType() == BankAccount.AccountType.CHECKING ? "checking" : "savings")
                        .build());

        // Adicionar registerInformation com dados opcionais do User (se disponíveis)
        // Isso permite que o recipient faça saques no Pagar.me
        // IMPORTANTE: Se incluir registerInformation, TODOS os campos obrigatórios devem estar presentes
        // Para simplificar, verificamos se temos dados COMPLETOS
        RecipientRequest.RegisterInformation.RegisterInformationBuilder regInfoBuilder = 
            RecipientRequest.RegisterInformation.builder()
                .email(email)
                .document(document)
                .type(document.length() == 11 ? "INDIVIDUAL" : "COMPANY")
                .name(name);
        
        boolean hasCompleteRegistrationData = false;
        
        // Para incluir registerInformation, precisamos de:
        // - birthdate (opcional mas vamos incluir se tiver)
        // - mothername, monthly_income, professional_occupation (obrigatórios para individual)
        // - address completo (obrigatório)
        // Como não temos todos esses dados, vamos enviar registrationInformation SÓ SE TIVER DADOS SUFICIENTES
        
        // Data de nascimento (formato DD/MM/YYYY)
        if (user.getDateOfBirth() != null) {
            String birthdate = String.format("%02d/%02d/%04d", 
                user.getDateOfBirth().getDayOfMonth(),
                user.getDateOfBirth().getMonthValue(),
                user.getDateOfBirth().getYear());
            regInfoBuilder.birthdate(birthdate);
            log.debug("   ├─ Birthdate: {}", birthdate);
        }
        
        // Telefone (DDD + número)
        if (user.getPhoneDdd() != null && user.getPhoneNumber() != null) {
            regInfoBuilder.phoneNumbers(List.of(
                RecipientRequest.PhoneNumber.builder()
                    .ddd(user.getPhoneDdd())
                    .number(user.getPhoneNumber())
                    .type("mobile")
                    .build()
            ));
            log.debug("   ├─ Phone: ({}) {}", user.getPhoneDdd(), user.getPhoneNumber());
        }
        
        // Verificar se temos TODOS os dados para incluir registerInformation
        boolean hasMotherName = bankAccount.getMotherName() != null;
        boolean hasMonthlyIncome = bankAccount.getMonthlyIncome() != null;
        boolean hasProfessionalOccupation = bankAccount.getProfessionalOccupation() != null;
        
        // Endereço (TODOS os campos obrigatórios)
        Address address = user.getAddress();
        boolean hasCompleteAddress = address != null && address.getStreet() != null && 
                                     address.getNumber() != null && address.getCity() != null && 
                                     address.getCity().getStateCode() != null && address.getZipCode() != null;
        
        // SÓ incluir registerInformation se temos TODOS os dados obrigatórios
        if (hasMotherName && hasMonthlyIncome && hasProfessionalOccupation && hasCompleteAddress) {
            regInfoBuilder.motherName(bankAccount.getMotherName());
            regInfoBuilder.monthlyIncome(bankAccount.getMonthlyIncome());
            regInfoBuilder.professionalOccupation(bankAccount.getProfessionalOccupation());
            
            // Construir address com valores padrão para campos obrigatórios
            RecipientRequest.Address.AddressBuilder addrBuilder = RecipientRequest.Address.builder()
                .street(address.getStreet())
                .streetNumber(address.getNumber())
                .neighborhood(address.getNeighborhood())
                .complementary(address.getComplement() != null ? address.getComplement() : "")
                .referencePoint(address.getReferencePoint() != null ? address.getReferencePoint() : "")
                .zipCode(address.getZipCode())
                .city(address.getCity().getName())
                .state(address.getCity().getStateCode());
            
            regInfoBuilder.address(addrBuilder.build());
            requestBuilder.registerInformation(regInfoBuilder.build());
            hasCompleteRegistrationData = true;
            log.info("   ├─ ✅ RegisterInformation incluído com dados COMPLETOS");
        } else {
            log.info("   ├─ ℹ️ RegisterInformation NÃO incluído - dados insuficientes (motherName={}, monthlyIncome={}, professionalOccupation={}, completeAddress={})",
                hasMotherName, hasMonthlyIncome, hasProfessionalOccupation, hasCompleteAddress);
        }
        
        // Adicionar transfer_settings com os parâmetros fornecidos pelo usuário
        requestBuilder.transferSettings(RecipientRequest.TransferSettings.builder()
                .transferEnabled(true) // Sempre habilitado
                .transferInterval(transferInterval)
                .transferDay(transferDay)
                .build());

        RecipientRequest request = requestBuilder.build();

        try {
            // Log the JSON request body for debugging
            try {
                String jsonBody = objectMapper.writeValueAsString(request);
                log.info("📤 JSON Request Body:\n{}", jsonBody);
            } catch (Exception e) {
                log.warn("⚠️ Failed to serialize request to JSON: {}", e.getMessage());
            }

            HttpHeaders headers = createHeaders();
            HttpEntity<RecipientRequest> entity = new HttpEntity<>(request, headers);

            String url = config.getApi().getUrl() + "/recipients";
            ResponseEntity<RecipientResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    RecipientResponse.class
            );

            RecipientResponse recipient = response.getBody();
            if (recipient != null) {
                log.info("✅ Recipient criado: {}", recipient.getId());
                return recipient.getId();
            }

            throw new RuntimeException("Resposta vazia do Pagar.me");

        } catch (Exception e) {
            log.error("❌ Erro ao criar recipient no Pagar.me", e);
            
            // Extrair mensagem específica do Pagar.me e campo com erro
            String errorMessage = e.getMessage();
            String enhancedError = buildEnhancedErrorMessage(errorMessage, document, name, email, bankAccount);
            
            throw new RuntimeException(enhancedError, e);
        }
    }
    
    /**
     * Constrói mensagem de erro detalhada incluindo o valor do campo que falhou
     */
    private String buildEnhancedErrorMessage(String originalError, String document, String name, String email, BankAccount bankAccount) {
        // Mapa de campos do Pagar.me para valores enviados
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put("document_number", document);
        fieldValues.put("document", document);
        fieldValues.put("name", name);
        fieldValues.put("email", email);
        fieldValues.put("bank", bankAccount.getBankCode());
        fieldValues.put("branch_number", bankAccount.getAgency());
        fieldValues.put("branch_check_digit", bankAccount.getAgencyDigit());
        fieldValues.put("account_number", bankAccount.getAccountNumber());
        fieldValues.put("account_check_digit", bankAccount.getAccountDigit());
        
        // Tentar extrair o campo com erro da mensagem do Pagar.me
        // Formato: "invalid_parameter | CAMPO | mensagem"
        String fieldWithError = null;
        String valueWithError = null;
        
        if (originalError != null && originalError.contains("|")) {
            String[] parts = originalError.split("\\|");
            if (parts.length >= 2) {
                fieldWithError = parts[1].trim();
                valueWithError = fieldValues.get(fieldWithError);
            }
        }
        
        // Construir mensagem aprimorada
        StringBuilder enhanced = new StringBuilder("Falha ao criar recipient: ");
        enhanced.append(originalError);
        
        if (fieldWithError != null && valueWithError != null) {
            enhanced.append(" | Campo com erro: ").append(fieldWithError)
                    .append("=").append(valueWithError);
        }
        
        return enhanced.toString();
    }

    /**
     * Cria uma order com PIX e split automático
     * 
     * @param amount Valor total em BRL
     * @param description Descrição do pagamento
     * @param customerName Nome do cliente
     * @param customerEmail Email do cliente
     * @param customerDocument CPF do cliente
     * @param courierRecipientId ID do recipient do courier
     * @param managerRecipientId ID do recipient do manager (null se não houver)
     * @return Response com QR Code PIX e detalhes
     */
    public OrderResponse createOrderWithSplit(
            BigDecimal amount,
            String description,
            String customerName,
            String customerEmail,
            String customerDocument,
            String courierRecipientId,
            String managerRecipientId,
            String platformRecipientId,
            int expiresInSeconds
    ) {
        log.info("💳 Criando order com PIX e split: R$ {}", amount);

        boolean hasManager = managerRecipientId != null && !managerRecipientId.isBlank();

        // Converter para centavos
        int amountInCents = amount.multiply(new BigDecimal(100)).intValue();

        // Calcular splits
        // Courier sempre recebe 87%
        int courierAmount = (amountInCents * config.getSplit().getCourierPercentage()) / 10000;
        
        // Manager recebe 5% apenas se existir
        int managerAmount = 0;
        if (hasManager) {
            managerAmount = (amountInCents * config.getSplit().getManagerPercentage()) / 10000;
        }
        
        // Plataforma recebe o resto (8% ou 13% se não houver manager)
        int platformAmount = amountInCents - courierAmount - managerAmount;

        log.info("   ├─ Total: {} centavos", amountInCents);
        log.info("   ├─ Courier (87%): {} centavos", courierAmount);
        if (hasManager) {
            log.info("   ├─ Manager (5%): {} centavos", managerAmount);
            log.info("   └─ Plataforma (8%): {} centavos (automático)", platformAmount);
        } else {
            log.info("   └─ Plataforma (13%): {} centavos (incorporou 5% do manager ausente)", platformAmount);
        }

        // Configurar splits
        List<OrderRequest.SplitRequest> orderSplits = new ArrayList<>();

        // Split do courier (87%) - sempre presente
        orderSplits.add(OrderRequest.SplitRequest.builder()
                .amount(courierAmount)
                .type("flat")
                .recipientId(courierRecipientId)
                .options(OrderRequest.SplitOptionsRequest.builder()
                        .liable(config.getSplit().getCourierLiable())
                        .chargeProcessingFee(config.getSplit().getCourierChargeProcessingFee())
                        .chargeRemainderFee(false)
                        .build())
                .build());

        // Split do manager (5%) - apenas se existir
        if (hasManager) {
            orderSplits.add(OrderRequest.SplitRequest.builder()
                    .amount(managerAmount)
                    .type("flat")
                    .recipientId(managerRecipientId)
                    .options(OrderRequest.SplitOptionsRequest.builder()
                            .liable(false)
                            .chargeProcessingFee(config.getSplit().getManagerChargeProcessingFee())
                            .chargeRemainderFee(false)
                            .build())
                    .build());
        }

        // Split da plataforma (calculado por DIFERENÇA para garantir integridade)
        if (platformRecipientId != null && !platformRecipientId.isBlank()) {
            orderSplits.add(OrderRequest.SplitRequest.builder()
                    .amount(platformAmount)
                    .type("flat")
                    .recipientId(platformRecipientId)
                    .options(OrderRequest.SplitOptionsRequest.builder()
                            .liable(true)
                            .chargeProcessingFee(true)
                            .chargeRemainderFee(true)
                            .build())
                    .build());
            log.info("   ├─ ✅ Split da plataforma incluído explicitamente: {} centavos", platformAmount);
        }

        // Extrair código da delivery da description ("Entrega #45" → "45")
        String itemCode = description.replaceAll("[^0-9]", "");

        // Criar request
        OrderRequest request = OrderRequest.builder()
                .closed(true)
                .items(List.of(OrderRequest.ItemRequest.builder()
                        .amount((long) amountInCents)
                        .description(description)
                        .quantity(1L)
                        .code(itemCode)
                        .build()))
                .customer(OrderRequest.CustomerRequest.builder()
                        .name(customerName)
                        .email(customerEmail)
                        .document(customerDocument)
                        .type("individual")
                        .build())
                .payments(List.of(OrderRequest.PaymentRequest.builder()
                        .paymentMethod("pix")
                        .amount((long) amountInCents)
                        .pix(OrderRequest.PixRequest.builder()
                                .expiresIn(String.valueOf(expiresInSeconds))
                                .additionalInformation(List.of(
                                        OrderRequest.AdditionalInfoRequest.builder()
                                                .name("Entregas")
                                                .value("#" + itemCode)
                                                .build()
                                ))
                                .build())
                        .split(orderSplits)
                        .build()))
                .build();

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);

            String url = config.getApi().getUrl() + "/orders";
            
            // Serializar request para auditoria
            String requestJson = null;
            try {
                requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                log.info("📤 PIX Order Request:\n{}", requestJson);
            } catch (Exception e) {
                log.debug("Erro ao serializar request para log", e);
            }
            
            // Receber resposta como String para preservar raw JSON e logar
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String responseJson = response.getBody();
            if (responseJson == null || responseJson.isBlank()) {
                throw new RuntimeException("Resposta vazia do Pagar.me");
            }
            
            log.info("📥 PIX Order Response (raw):\n{}", responseJson);
            
            // Desserializar manualmente para ter controle
            OrderResponse order = objectMapper.readValue(responseJson, OrderResponse.class);
            
            // Popular campos de auditoria (transient — não vêm do JSON)
            order.setRequestPayload(requestJson);
            order.setResponsePayload(responseJson);
            
            if (order.getId() != null) {
                log.info("✅ Order PIX criada: {} (status: {})", order.getId(), order.getStatus());
                
                // Log de debug para QR Code
                if (order.getCharges() != null && !order.getCharges().isEmpty()) {
                    OrderResponse.Charge charge = order.getCharges().get(0);
                    if (charge.getLastTransaction() != null) {
                        OrderResponse.LastTransaction tx = charge.getLastTransaction();
                        log.info("   ├─ 🔍 QR Code presente: {}", tx.getQrCode() != null ? "✅ (" + tx.getQrCode().length() + " chars)" : "❌");
                        log.info("   ├─ 🔍 QR Code URL presente: {}", tx.getQrCodeUrl() != null ? "✅" : "❌");
                        log.info("   ├─ 🔍 ExpiresAt: {}", tx.getExpiresAt());
                        log.info("   ├─ 🔍 Status transação: {}", tx.getStatus());
                        log.info("   ├─ 🔍 Success: {}", tx.getSuccess());
                    } else {
                        log.warn("   ├─ ⚠️ lastTransaction é NULL na charge");
                    }
                } else {
                    log.warn("   ├─ ⚠️ charges é NULL ou vazio na order");
                }
                
                return order;
            }

            throw new RuntimeException("Order criada mas sem ID na resposta");

        } catch (HttpClientErrorException e) {
            log.error("❌ Erro HTTP ao criar order PIX: {} - Body: {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Falha ao criar order PIX: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            log.error("❌ Erro do servidor Pagar.me ao criar order PIX: {} - Body: {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Falha ao criar order PIX: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("❌ Erro ao criar order PIX no Pagar.me", e);
            throw new RuntimeException("Falha ao criar order: " + e.getMessage(), e);
        }
    }

    /**
     * Cria uma order com Cartão de Crédito e split automático
     * 
     * SPLIT DE VALORES:
     * - Com Organizer: 87% courier, 5% organizer, 8% plataforma
     * - Sem Organizer (delivery criada por CUSTOMER): 87% courier, 13% plataforma
     * 
     * A plataforma (conta master) absorve automaticamente o percentual restante.
     * 
     * @param amount Valor total em BRL
     * @param description Descrição do pagamento
     * @param cardToken Token do cartão gerado via Pagar.me
     * @param customerName Nome do cliente
     * @param customerEmail Email do cliente
     * @param customerDocument CPF do cliente
     * @param customerAddress Endereço do cliente (para billing)
     * @param courierRecipientId ID do recipient do courier (obrigatório)
     * @param organizerRecipientId ID do recipient do organizer (null se não houver)
     * @param statementDescriptor Nome na fatura do cartão (máx 13 chars)
     * @return Response com detalhes do pagamento
     */
    public OrderResponse createOrderWithCreditCardSplit(
            BigDecimal amount,
            String description,
            String cardToken,
            String customerName,
            String customerEmail,
            String customerDocument,
            OrderRequest.BillingAddressRequest customerAddress,
            String courierRecipientId,
            String organizerRecipientId,
            String statementDescriptor,
            String platformRecipientId
    ) {
        log.info("💳 Criando order com Cartão de Crédito e split: R$ {}", amount);
        
        boolean hasOrganizer = organizerRecipientId != null && !organizerRecipientId.isBlank();
        
        // Converter para centavos
        int amountInCents = amount.multiply(new BigDecimal(100)).intValue();

        // Calcular splits
        // Courier sempre recebe 87%
        int courierAmount = (amountInCents * config.getSplit().getCourierPercentage()) / 10000;
        
        // Organizer recebe 5% apenas se existir
        int organizerAmount = 0;
        if (hasOrganizer) {
            organizerAmount = (amountInCents * config.getSplit().getManagerPercentage()) / 10000;
        }
        
        // Plataforma recebe o resto (8% ou 13% se não houver organizer)
        int platformAmount = amountInCents - courierAmount - organizerAmount;

        log.info("   ├─ Total: {} centavos", amountInCents);
        log.info("   ├─ Courier (87%): {} centavos", courierAmount);
        if (hasOrganizer) {
            log.info("   ├─ Organizer (5%): {} centavos", organizerAmount);
            log.info("   └─ Plataforma (8%): {} centavos (automático)", platformAmount);
        } else {
            log.info("   └─ Plataforma (13%): {} centavos (incorporou 5% do organizer ausente)", platformAmount);
        }

        // Configurar splits
        List<OrderRequest.SplitRequest> orderSplits = new ArrayList<>();

        // Split do courier (87%) - sempre presente
        orderSplits.add(OrderRequest.SplitRequest.builder()
                .amount(courierAmount)
                .type("flat")
                .recipientId(courierRecipientId)
                .options(OrderRequest.SplitOptionsRequest.builder()
                        .liable(false) // Plataforma é liable
                        .chargeProcessingFee(false) // Plataforma paga taxas
                        .chargeRemainderFee(false)
                        .build())
                .build());

        // Split do organizer (5%) - apenas se existir
        if (hasOrganizer) {
            orderSplits.add(OrderRequest.SplitRequest.builder()
                    .amount(organizerAmount)
                    .type("flat")
                    .recipientId(organizerRecipientId)
                    .options(OrderRequest.SplitOptionsRequest.builder()
                            .liable(false) // Plataforma é liable
                            .chargeProcessingFee(false) // Plataforma paga taxas
                            .chargeRemainderFee(false)
                            .build())
                    .build());
        }

        // Split da plataforma (calculado por DIFERENÇA para garantir integridade)
        if (platformRecipientId != null && !platformRecipientId.isBlank()) {
            orderSplits.add(OrderRequest.SplitRequest.builder()
                    .amount(platformAmount)
                    .type("flat")
                    .recipientId(platformRecipientId)
                    .options(OrderRequest.SplitOptionsRequest.builder()
                            .liable(true)
                            .chargeProcessingFee(true)
                            .chargeRemainderFee(true)
                            .build())
                    .build());
            log.info("   ├─ ✅ Split da plataforma incluído explicitamente: {} centavos", platformAmount);
        }

        // Validar statement descriptor (máximo 13 caracteres)
        if (statementDescriptor == null || statementDescriptor.isBlank()) {
            statementDescriptor = "ZAPI10";
        }
        if (statementDescriptor.length() > 13) {
            statementDescriptor = statementDescriptor.substring(0, 13);
        }
        
        // Parcela única - pagamentos de delivery não permitem parcelamento
        final int installments = 1;

        // Criar request
        OrderRequest request = OrderRequest.builder()
                .closed(true) // Encerrar order imediatamente
                .items(List.of(OrderRequest.ItemRequest.builder()
                        .amount((long) amountInCents)
                        .description(description)
                        .quantity(1L)
                        .code("DELIVERY")
                        .build()))
                .customer(OrderRequest.CustomerRequest.builder()
                        .name(customerName)
                        .email(customerEmail)
                        .document(customerDocument)
                        .type("individual")
                        .build())
                .payments(List.of(OrderRequest.PaymentRequest.builder()
                        .paymentMethod("credit_card")
                        .amount((long) amountInCents)
                        .creditCard(OrderRequest.CreditCardRequest.builder()
                                .operationType("auth_and_capture")
                                .installments(installments)
                                .statementDescriptor(statementDescriptor)
                                // Se cardToken começa com "card_", é um ID de cartão salvo
                                .cardId(cardToken.startsWith("card_") ? cardToken : null)
                                // Se não começa com "card_", é um token descartável
                                .cardToken(cardToken.startsWith("card_") ? null : cardToken)
                                .build())
                        .split(orderSplits)
                        .build()))
                .build();

        String requestPayload = null;
        String responsePayload = null;
        
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);

            String url = config.getApi().getUrl() + "/orders";
            
            // Serializar request para auditoria
            try {
                requestPayload = objectMapper.writeValueAsString(request);
                log.info("📤 Enviando request para Pagar.me: {}", url);
            } catch (Exception e) {
                log.warn("⚠️ Não foi possível serializar request para auditoria: {}", e.getMessage());
            }
            
            ResponseEntity<OrderResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    OrderResponse.class
            );

            OrderResponse order = response.getBody();
            
            // Serializar response para auditoria
            try {
                responsePayload = objectMapper.writeValueAsString(order);
            } catch (Exception e) {
                log.warn("⚠️ Não foi possível serializar response para auditoria: {}", e.getMessage());
            }
            
            // Adicionar request/response ao objeto de retorno para persistência
            if (order != null) {
                order.setRequestPayload(requestPayload);
                order.setResponsePayload(responsePayload);
                
                // Debug: Confirmar que estamos setando os valores
                log.info("   ├─ 🔍 DEBUG Setou request payload: {} chars", requestPayload != null ? requestPayload.length() : "NULL");
                log.info("   ├─ 🔍 DEBUG Setou response payload: {} chars", responsePayload != null ? responsePayload.length() : "NULL");
                
                log.info("✅ Order com cartão criada: {} (status: {})", order.getId(), order.getStatus());
                
                // Log detalhado se a order falhou — mostrar motivo da recusa
                if ("failed".equalsIgnoreCase(order.getStatus()) && order.getCharges() != null) {
                    for (var charge : order.getCharges()) {
                        log.warn("   ├─ ⚠️ Charge {}: status={}", charge.getId(), charge.getStatus());
                        if (charge.getLastTransaction() != null) {
                            var tx = charge.getLastTransaction();
                            log.warn("   ├─ ⚠️ Transaction: status={}, success={}", tx.getStatus(), tx.getSuccess());
                            
                            // Informações da adquirente
                            if (tx.getAcquirerName() != null || tx.getAcquirerReturnCode() != null || tx.getAcquirerMessage() != null) {
                                log.warn("   ├─ 🏦 Acquirer: name={}, code={}, message={}", 
                                        tx.getAcquirerName(), 
                                        tx.getAcquirerReturnCode(), 
                                        tx.getAcquirerMessage());
                            }
                            
                            // Informações do antifraude
                            if (tx.getAntifraudResponse() != null) {
                                try {
                                    String antifraudJson = objectMapper.writerWithDefaultPrettyPrinter()
                                            .writeValueAsString(tx.getAntifraudResponse());
                                    log.warn("   ├─ 🛡️ Antifraud Response:\n{}", antifraudJson);
                                } catch (Exception e) {
                                    log.warn("   ├─ 🛡️ Antifraud Response: {}", tx.getAntifraudResponse());
                                }
                            }
                            
                            // Gateway Response
                            if (tx.getGatewayResponse() != null) {
                                log.warn("   ├─ ⚠️ Gateway code={}, errors={}", 
                                        tx.getGatewayResponse().getCode(), tx.getGatewayResponse().getErrors());
                            }
                        }
                    }
                }
                
                return order;
            }

            throw new RuntimeException("Resposta vazia do Pagar.me");

        } catch (HttpClientErrorException e) {
            // Capturar response de erro
            responsePayload = e.getResponseBodyAsString();
            
            log.error("❌ Erro 4xx ao criar order com cartão no Pagar.me: {} - {}", 
                e.getStatusCode(), responsePayload);
            
            throw new PaymentProcessingException(
                "Falha ao processar cartão: " + extractErrorMessage(e),
                requestPayload,
                responsePayload,
                e.getStatusCode().toString(),
                e
            );
        } catch (HttpServerErrorException e) {
            // Capturar response de erro
            responsePayload = e.getResponseBodyAsString();
            
            log.error("❌ Erro 5xx do Pagar.me: {} - {}", 
                e.getStatusCode(), responsePayload);
            
            throw new PaymentProcessingException(
                "Erro no servidor de pagamento. Tente novamente.",
                requestPayload,
                responsePayload,
                e.getStatusCode().toString(),
                e
            );
        } catch (PaymentProcessingException e) {
            // Re-lançar exceção customizada
            throw e;
        } catch (Exception e) {
            log.error("❌ Erro ao criar order com cartão no Pagar.me", e);
            
            throw new PaymentProcessingException(
                "Falha ao criar order: " + e.getMessage(),
                requestPayload,
                responsePayload,
                "UNKNOWN",
                e
            );
        }
    }
    
    /**
     * Extrai mensagem de erro amigável da resposta do Pagar.me
     */
    private String extractErrorMessage(HttpClientErrorException e) {
        try {
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("message")) {
                // Tenta extrair a mensagem do JSON
                int start = body.indexOf("\"message\":\"") + 11;
                int end = body.indexOf("\"", start);
                if (start > 10 && end > start) {
                    return body.substring(start, end);
                }
            }
            return e.getMessage();
        } catch (Exception ex) {
            return e.getMessage();
        }
    }

    /**
     * Valida assinatura de webhook usando HMAC SHA256
     * 
     * @param payload Payload recebido do webhook
     * @param signature Signature do header X-Hub-Signature
     * @return true se válido
     */
    public boolean validateWebhookSignature(String payload, String signature) {
        try {
            String secret = config.getWebhook().getSecret();
            
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = "sha256=" + bytesToHex(hash);

            boolean isValid = calculatedSignature.equals(signature);
            
            if (isValid) {
                log.info("✅ Webhook signature válida");
            } else {
                log.warn("⚠️ Webhook signature inválida");
            }

            return isValid;

        } catch (Exception e) {
            log.error("❌ Erro ao validar webhook signature", e);
            return false;
        }
    }

    /**
     * Processa evento de webhook
     * 
     * @param event Evento recebido
     */
    public void processWebhookEvent(PagarMeWebhookEvent event) {
        log.info("📨 Processando webhook: {} (type: {})", event.getId(), event.getType());

        switch (event.getType()) {
            case "order.paid":
                log.info("✅ Pagamento confirmado: {}", event.getData().getId());
                // TODO: Atualizar status do Payment para COMPLETED
                break;

            case "order.payment_failed":
                log.error("❌ Pagamento falhou: {}", event.getData().getId());
                // TODO: Atualizar status do Payment para FAILED
                break;

            case "order.canceled":
                log.warn("⚠️ Pagamento cancelado: {}", event.getData().getId());
                // TODO: Atualizar status do Payment para CANCELLED
                break;

            default:
                log.info("ℹ️ Evento não tratado: {}", event.getType());
        }
    }

    /**
     * Cria uma order (pedido) no Pagar.me com PIX e split automático
     * 
     * @param orderRequest Dados da order (items, customer, payments)
     * @return Order ID do Pagar.me
     */
    public String createOrder(OrderRequest orderRequest) {
        log.info("📦 Criando order no Pagar.me");

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<OrderRequest> entity = new HttpEntity<>(orderRequest, headers);

            String url = config.getApi().getUrl() + "/orders";
            
            // Log do curl command completo
            try {
                String jsonBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(orderRequest);
                String authHeader = headers.getFirst("Authorization");
                
                StringBuilder curlCommand = new StringBuilder();
                curlCommand.append("curl -X POST '").append(url).append("' \\\n");
                curlCommand.append("  -H 'Content-Type: application/json' \\\n");
                curlCommand.append("  -H 'Authorization: ").append(authHeader).append("' \\\n");
                curlCommand.append("  -d '").append(jsonBody.replace("'", "'\\''")).append("'");
                
                log.info("📤 CURL Command (copy-paste ready):\n{}", curlCommand.toString());
                log.info("📤 JSON Request Body:\n{}", jsonBody);
            } catch (Exception e) {
                log.debug("Erro ao serializar request para log", e);
            }

            ResponseEntity<com.mvt.mvt_events.payment.dto.OrderResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    com.mvt.mvt_events.payment.dto.OrderResponse.class
            );

            com.mvt.mvt_events.payment.dto.OrderResponse body = response.getBody();
            
            if (body != null && body.getId() != null) {
                log.info("✅ Order criada com sucesso: {}", body.getId());
                return body.getId();
            }

            throw new RuntimeException("Order criada mas sem ID na resposta");

        } catch (Exception e) {
            log.error("❌ Erro ao criar order no Pagar.me", e);
            throw new RuntimeException("Falha ao criar order: " + e.getMessage(), e);
        }
    }

    /**
     * Cria order no Pagar.me e retorna a response completa
     * 
     * @param orderRequest Dados da order
     * @return OrderResponse completo do gateway
     */
    public com.mvt.mvt_events.payment.dto.OrderResponse createOrderWithFullResponse(OrderRequest orderRequest) {
        log.info("📦 Criando order no Pagar.me");

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<OrderRequest> entity = new HttpEntity<>(orderRequest, headers);

            String url = config.getApi().getUrl() + "/orders";
            
            // Log do curl command completo
            try {
                String jsonBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(orderRequest);
                String authHeader = headers.getFirst("Authorization");
                
                StringBuilder curlCommand = new StringBuilder();
                curlCommand.append("curl -X POST '").append(url).append("' \\\n");
                curlCommand.append("  -H 'Content-Type: application/json' \\\n");
                curlCommand.append("  -H 'Authorization: ").append(authHeader).append("' \\\n");
                curlCommand.append("  -d '").append(jsonBody.replace("'", "'\\''")).append("'");
                
                log.info("📤 CURL Command (copy-paste ready):\n{}", curlCommand.toString());
                log.info("📤 JSON Request Body:\n{}", jsonBody);
            } catch (Exception e) {
                log.debug("Erro ao serializar request para log", e);
            }

            ResponseEntity<com.mvt.mvt_events.payment.dto.OrderResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    com.mvt.mvt_events.payment.dto.OrderResponse.class
            );

            com.mvt.mvt_events.payment.dto.OrderResponse body = response.getBody();
            
            if (body != null && body.getId() != null) {
                log.info("✅ Order criada com sucesso: {}", body.getId());
                return body;
            }

            throw new RuntimeException("Order criada mas sem ID na resposta");

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Erro HTTP 4xx (como 422) - capturar o corpo da resposta
            log.error("❌ Erro HTTP ao criar order no Pagar.me: {} - Body: {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Falha ao criar order: " + e.getStatusCode() + " " + 
                e.getStatusText() + " on " + e.getMessage(), e);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // Erro HTTP 5xx - capturar o corpo da resposta
            log.error("❌ Erro do servidor Pagar.me ao criar order: {} - Body: {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Falha ao criar order: " + e.getStatusCode() + " " + 
                e.getStatusText() + " on " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Erro ao criar order no Pagar.me", e);
            throw new RuntimeException("Falha ao criar order: " + e.getMessage(), e);
        }
    }

    /**
     * Cria headers para autenticação com Pagar.me
     * 
     * Usa Basic Auth com a Secret Key:
     * User: sk_test_xxx ou sk_xxx (a chave)
     * Password: (vazio)
     * 
     * Formato: Authorization: Basic base64(secretKey:)
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Basic Auth: secretKey como user, password vazio
        // Exemplo: sk_test_xxx:
        String auth = config.getApi().getKey() + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        
        return headers;
    }

    /**
     * Converte bytes para hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Atualiza a conta bancária padrão de um recipient no Pagar.me
     * 
     * @param recipientId ID do recipient no Pagar.me
     * @param bankAccount Dados da nova conta bancária
     * @param user Usuário dono da conta
     */
    public void updateDefaultBankAccount(String recipientId, BankAccount bankAccount, User user) {
        log.info("🏦 Atualizando conta bancária padrão do recipient: {}", recipientId);
        
        try {
            String url = config.getApi().getUrl() + "/recipients/" + recipientId + "/default-bank-account";
            
            // Montar request body com estrutura correta (bank_account wrapper)
            Map<String, Object> bankData = new HashMap<>();
            bankData.put("holder_name", user.getName());
            bankData.put("holder_type", "individual");
            bankData.put("holder_document", user.getDocumentClean());
            bankData.put("bank", bankAccount.getBankCode());
            bankData.put("branch_number", bankAccount.getAgency());
            bankData.put("branch_check_digit", bankAccount.getAgencyDigit() != null ? bankAccount.getAgencyDigit() : "");
            bankData.put("account_number", bankAccount.getAccountNumber());
            bankData.put("account_check_digit", bankAccount.getAccountDigit());
            bankData.put("type", bankAccount.getAccountType() == BankAccount.AccountType.CHECKING ? "checking" : "savings");
            
            // Wrapper: Pagar.me espera { "bank_account": { ... } }
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("bank_account", bankData);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.debug("   ├─ URL: PATCH {}", url);
            log.debug("   ├─ Bank: {} Agency: {}-{} Account: {}-{}", 
                bankAccount.getBankCode(), 
                bankAccount.getAgency(),
                bankAccount.getAgencyDigit(),
                bankAccount.getAccountNumber(), 
                bankAccount.getAccountDigit());
            
            // RestTemplate suporta PATCH normalmente
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PATCH,
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("   └─ ✅ Conta bancária padrão atualizada com sucesso no Pagar.me");
            } else if (response.getStatusCode().value() == 412) {
                log.warn("   └─ ⚠️ Pagar.me retornou 412: Requer configuração de Allow List");
                log.warn("      Acesse https://docs.pagar.me/reference/allow-list para configurar");
            } else {
                log.error("   └─ ❌ Erro HTTP {} ao atualizar conta bancária no Pagar.me: {}", 
                    response.getStatusCode(), response.getBody());
            }
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 412) {
                log.warn("   └─ ⚠️ Pagar.me retornou 412: Requer configuração de Allow List");
                log.warn("      Acesse https://docs.pagar.me/reference/allow-list para configurar");
                log.warn("      Response body: {}", e.getResponseBodyAsString());
                log.warn("      Dados locais salvos, mas Pagar.me não atualizado");
            } else {
                log.error("   └─ ❌ Erro HTTP {} ao atualizar conta bancária no Pagar.me: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
                throw new RuntimeException("Erro ao atualizar conta bancária no Pagar.me: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao atualizar conta bancária no Pagar.me", e);
            throw new RuntimeException("Erro ao atualizar conta bancária no Pagar.me: " + e.getMessage(), e);
        }
    }
    
    /**
     * Atualiza as configurações de transferência automática do recipient no Pagar.me
     * 
     * @param recipientId ID do recipient no Pagar.me
     * @param transferInterval Intervalo de transferência: "Daily", "Weekly", "Monthly"
     * @param transferDay Dia da transferência (0 para Daily, 0-6 para Weekly, 1-31 para Monthly)
     */
    public void updateTransferSettings(String recipientId, String transferInterval, Integer transferDay) {
        log.info("💰 Atualizando transfer settings do recipient: {}", recipientId);
        log.info("   ├─ Transfer interval: {}", transferInterval);
        log.info("   ├─ Transfer day (original): {}", transferDay);
        
        // Validar transferDay conforme o intervalo
        if (transferDay == null) {
            transferDay = 0;
        }
        // Weekly: Pagar.me aceita apenas 1-5 (segunda a sexta-feira)
        if ("Weekly".equalsIgnoreCase(transferInterval)) {
            if (transferDay == 0 || transferDay == 6) {
                // Fim de semana não é aceito, usar segunda-feira
                log.warn("   ⚠️ Dia {} não permitido para Weekly (Pagar.me aceita 1-5). Usando 1 (segunda-feira)", transferDay);
                transferDay = 1;
            } else if (transferDay < 1 || transferDay > 5) {
                log.warn("   ⚠️ Dia inválido para Weekly. Usando 1 (segunda-feira)");
                transferDay = 1;
            }
        }
        
        log.info("   ├─ Transfer day (validado): {}", transferDay);
        
        try {
            String url = config.getApi().getUrl() + "/recipients/" + recipientId + "/transfer-settings";
            
            // Montar request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("transfer_enabled", true); // Sempre habilitado
            requestBody.put("transfer_interval", transferInterval);
            requestBody.put("transfer_day", transferDay);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.debug("   ├─ URL: PATCH {}", url);
            
            // Enviar PATCH request
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PATCH,
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("   └─ ✅ Transfer settings atualizados com sucesso no Pagar.me");
            } else if (response.getStatusCode().value() == 412) {
                log.warn("   └─ ⚠️ Pagar.me retornou 412: Requer configuração de Allow List");
                log.warn("      Acesse https://docs.pagar.me/reference/allow-list para configurar");
            } else {
                log.error("   └─ ❌ Erro HTTP {} ao atualizar transfer settings no Pagar.me: {}", 
                    response.getStatusCode(), response.getBody());
            }
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 412) {
                log.warn("   └─ ⚠️ Pagar.me retornou 412: Requer configuração de Allow List");
                log.warn("      Acesse https://docs.pagar.me/reference/allow-list para configurar");
                log.warn("      Response body: {}", e.getResponseBodyAsString());
            } else {
                log.error("   └─ ❌ Erro HTTP {} ao atualizar transfer settings no Pagar.me: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
                throw new RuntimeException("Erro ao atualizar transfer settings no Pagar.me: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao atualizar transfer settings no Pagar.me", e);
            throw new RuntimeException("Erro ao atualizar transfer settings no Pagar.me: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // CUSTOMER MANAGEMENT (para CUSTOMER/CLIENT pagar com cartão)
    // ============================================================================

    /**
     * Cria um customer no Pagar.me para poder gerenciar cartões.
     * 
     * @param user Usuário (CUSTOMER/CLIENT)
     * @return ID do customer criado (cus_xxxxx)
     */
    public String createCustomer(User user) {
        log.info("💳 Criando customer no Pagar.me para user: {} ({})", user.getName(), user.getUsername());

        try {
            HttpHeaders headers = createHeaders();
            
            // Preparar payload
            Map<String, Object> customerData = new HashMap<>();
            customerData.put("name", user.getName());
            customerData.put("email", user.getUsername());
            
            // Documento (sem formatação - apenas números)
            String documentClean = user.getDocumentClean();
            if (documentClean == null || documentClean.isEmpty()) {
                throw new RuntimeException("Usuário sem documento cadastrado");
            }
            
            customerData.put("type", documentClean.length() == 11 ? "individual" : "company");
            customerData.put("document", documentClean); // Documento diretamente como string
            
            // Telefone (se disponível)
            if (user.getPhoneDdd() != null && user.getPhoneNumber() != null) {
                Map<String, Object> phones = new HashMap<>();
                Map<String, String> mobilePhone = new HashMap<>();
                mobilePhone.put("country_code", "55");
                mobilePhone.put("area_code", user.getPhoneDdd());
                mobilePhone.put("number", user.getPhoneNumber());
                phones.put("mobile_phone", mobilePhone);
                customerData.put("phones", phones);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerData, headers);
            String url = config.getApi().getUrl() + "/customers";

            log.info("📤 Payload enviado ao Pagar.me: {}", customerData);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("id")) {
                String customerId = (String) responseBody.get("id");
                log.info("   └─ ✅ Customer criado: {}", customerId);
                return customerId;
            }

            throw new RuntimeException("Resposta do Pagar.me não contém ID do customer");

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("   └─ ❌ Erro HTTP {} ao criar customer: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao criar customer no Pagar.me: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao criar customer no Pagar.me", e);
            throw new RuntimeException("Erro ao criar customer no Pagar.me: " + e.getMessage(), e);
        }
    }

    /**
     * Busca um customer existente no Pagar.me pelo ID.
     * 
     * @param customerId ID do customer (cus_xxxxx)
     * @return Map com dados do customer
     */
    public Map<String, Object> getCustomer(String customerId) {
        log.info("🔍 Buscando customer no Pagar.me: {}", customerId);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = config.getApi().getUrl() + "/customers/" + customerId;
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            log.info("   └─ ✅ Customer encontrado");
            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("   └─ ⚠️ Customer não encontrado no Pagar.me: {}", customerId);
            return null;
        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao buscar customer no Pagar.me", e);
            throw new RuntimeException("Erro ao buscar customer: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // CARD MANAGEMENT (gerenciar cartões do customer)
    // ============================================================================

    /**
     * Cria um cartão no Pagar.me a partir de um token.
     * 
     * @param customerId ID do customer no Pagar.me (cus_xxxxx)
     * @param cardToken Token do cartão (card_xxxxx ou token gerado no frontend)
     * @param billingAddress Endereço de cobrança (opcional)
     * @return Map com dados do cartão criado
     */
    public Map<String, Object> createCard(String customerId, String cardToken, com.mvt.mvt_events.payment.dto.BillingAddressDTO billingAddress) {
        log.info("💳 Criando cartão no Pagar.me para customer: {}", customerId);

        try {
            HttpHeaders headers = createHeaders();
            
            // Payload
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("token", cardToken);

            // Adicionar billing_address se fornecido
            if (billingAddress != null) {
                Map<String, String> address = new HashMap<>();
                address.put("line_1", billingAddress.getLine1());
                
                // line_2 é opcional
                if (billingAddress.getLine2() != null && !billingAddress.getLine2().isEmpty()) {
                    address.put("line_2", billingAddress.getLine2());
                }
                
                address.put("zip_code", billingAddress.getZipCode());
                address.put("city", billingAddress.getCity());
                address.put("state", billingAddress.getState());
                address.put("country", billingAddress.getCountry());
                
                cardData.put("billing_address", address);
                log.info("   └─ 📍 Billing address incluído: {}, {} - {}", 
                    billingAddress.getCity(), billingAddress.getState(), billingAddress.getZipCode());
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(cardData, headers);
            String url = config.getApi().getUrl() + "/customers/" + customerId + "/cards";

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                log.info("   └─ ✅ Cartão criado: {}", responseBody.get("id"));
                return responseBody;
            }

            throw new RuntimeException("Resposta do Pagar.me não contém dados do cartão");

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("   └─ ❌ Erro HTTP {} ao criar cartão: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao criar cartão no Pagar.me: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao criar cartão no Pagar.me", e);
            throw new RuntimeException("Erro ao criar cartão: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todos os cartões de um customer.
     * 
     * @param customerId ID do customer (cus_xxxxx)
     * @return Lista de cartões
     */
    public List<Map<String, Object>> listCustomerCards(String customerId) {
        log.info("🔍 Listando cartões do customer: {}", customerId);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = config.getApi().getUrl() + "/customers/" + customerId + "/cards";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("data")) {
                List<Map<String, Object>> cards = (List<Map<String, Object>>) responseBody.get("data");
                log.info("   └─ ✅ {} cartões encontrados", cards.size());
                return cards;
            }

            return List.of();

        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao listar cartões do customer", e);
            throw new RuntimeException("Erro ao listar cartões: " + e.getMessage(), e);
        }
    }

    /**
     * Busca um cartão específico do customer.
     * 
     * @param customerId ID do customer (cus_xxxxx)
     * @param cardId ID do cartão (card_xxxxx)
     * @return Map com dados do cartão
     */
    public Map<String, Object> getCard(String customerId, String cardId) {
        log.info("🔍 Buscando cartão: {} do customer: {}", cardId, customerId);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = config.getApi().getUrl() + "/customers/" + customerId + "/cards/" + cardId;
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            log.info("   └─ ✅ Cartão encontrado");
            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("   └─ ⚠️ Cartão não encontrado: {}", cardId);
            return null;
        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao buscar cartão", e);
            throw new RuntimeException("Erro ao buscar cartão: " + e.getMessage(), e);
        }
    }

    /**
     * Deleta um cartão do customer no Pagar.me.
     * 
     * @param customerId ID do customer (cus_xxxxx)
     * @param cardId ID do cartão (card_xxxxx)
     */
    public void deleteCard(String customerId, String cardId) {
        log.info("🗑️ Deletando cartão: {} do customer: {}", cardId, customerId);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = config.getApi().getUrl() + "/customers/" + customerId + "/cards/" + cardId;
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            log.info("   └─ ✅ Cartão deletado com sucesso");

        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao deletar cartão", e);
            throw new RuntimeException("Erro ao deletar cartão: " + e.getMessage(), e);
        }
    }
}