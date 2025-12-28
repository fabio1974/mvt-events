package com.mvt.mvt_events.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.jpa.Address;
import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.config.PagarMeConfig;
import com.mvt.mvt_events.payment.dto.*;
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
        return createRecipient(user, bankAccount, true); // Default: transferência automática habilitada
    }
    
    public String createRecipient(User user, BankAccount bankAccount, boolean automaticTransfer) {
        String name = user.getName();
        String email = user.getUsername();
        String document = user.getDocumentClean();
        
        log.info("🏦 Criando recipient no Pagar.me: {} ({})", name, document);
        log.info("   ├─ Transferência automática: {}", automaticTransfer ? "✅ Habilitada (Daily)" : "❌ Desabilitada");

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
        
        // Adicionar transfer_settings
        requestBuilder.transferSettings(RecipientRequest.TransferSettings.builder()
                .transferEnabled(automaticTransfer)
                .transferInterval("Daily")
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
     * @param managerRecipientId ID do recipient do manager
     * @return Response com QR Code PIX e detalhes
     */
    public OrderResponse createOrderWithSplit(
            BigDecimal amount,
            String description,
            String customerName,
            String customerEmail,
            String customerDocument,
            String courierRecipientId,
            String managerRecipientId
    ) {
        log.info("💳 Criando order com PIX e split: R$ {}", amount);

        // Converter para centavos
        int amountInCents = amount.multiply(new BigDecimal(100)).intValue();

        // Calcular splits (87% courier, 5% manager, 8% plataforma automático)
        int courierAmount = (amountInCents * config.getSplit().getCourierPercentage()) / 10000;
        int managerAmount = (amountInCents * config.getSplit().getManagerPercentage()) / 10000;

        log.info("   ├─ Total: {} centavos", amountInCents);
        log.info("   ├─ Courier (87%): {} centavos", courierAmount);
        log.info("   ├─ Manager (5%): {} centavos", managerAmount);
        log.info("   └─ Plataforma (8%): {} centavos (automático)", amountInCents - courierAmount - managerAmount);

        // Configurar splits
        List<PagarMeSplitRequest> splits = new ArrayList<>();

        // Split do courier (87%)
        splits.add(PagarMeSplitRequest.builder()
                .amount(courierAmount)
                .type("flat")
                .recipientId(courierRecipientId)
                .options(PagarMeSplitRequest.SplitOptions.builder()
                        .liable(config.getSplit().getCourierLiable())
                        .chargeProcessingFee(config.getSplit().getCourierChargeProcessingFee())
                        .chargeRemainderFee(false)
                        .build())
                .build());

        // Split do manager (5%)
        splits.add(PagarMeSplitRequest.builder()
                .amount(managerAmount)
                .type("flat")
                .recipientId(managerRecipientId)
                .options(PagarMeSplitRequest.SplitOptions.builder()
                        .liable(false)
                        .chargeProcessingFee(config.getSplit().getManagerChargeProcessingFee())
                        .chargeRemainderFee(false)
                        .build())
                .build());

        // Converter splits para OrderRequest.SplitRequest
        List<OrderRequest.SplitRequest> orderSplits = splits.stream()
                .map(s -> OrderRequest.SplitRequest.builder()
                        .amount(s.getAmount().intValue())
                        .type(s.getType())
                        .recipientId(s.getRecipientId())
                        .options(OrderRequest.SplitOptionsRequest.builder()
                                .chargeProcessingFee(s.getOptions().getChargeProcessingFee())
                                .chargeRemainderFee(s.getOptions().getChargeRemainderFee())
                                .liable(s.getOptions().getLiable())
                                .build())
                        .build())
                .toList();

        // Criar request
        OrderRequest request = OrderRequest.builder()
                .items(List.of(OrderRequest.ItemRequest.builder()
                        .amount((long) amountInCents)
                        .description(description)
                        .quantity(1L)
                        .build()))
                .customer(OrderRequest.CustomerRequest.builder()
                        .name(customerName)
                        .email(customerEmail)
                        .document(customerDocument)
                        .type("individual")
                        .build())
                .payments(List.of(OrderRequest.PaymentRequest.builder()
                        .paymentMethod("pix")
                        .pix(OrderRequest.PixRequest.builder()
                                .expiresIn("86400")
                                .build())
                        .split(orderSplits)
                        .build()))
                .build();

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);

            String url = config.getApi().getUrl() + "/orders";
            ResponseEntity<OrderResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    OrderResponse.class
            );

            OrderResponse order = response.getBody();
            if (order != null) {
                log.info("✅ Order criada: {} (status: {})", order.getId(), order.getStatus());
                return order;
            }

            throw new RuntimeException("Resposta vazia do Pagar.me");

        } catch (Exception e) {
            log.error("❌ Erro ao criar order no Pagar.me", e);
            throw new RuntimeException("Falha ao criar order: " + e.getMessage(), e);
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
            
            // Log da response
            try {
                log.info("📥 JSON Response Body:\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body));
            } catch (Exception e) {
                log.debug("Erro ao serializar response para log", e);
            }
            
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
            
            // Log da response
            try {
                log.info("📥 JSON Response Body:\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body));
            } catch (Exception e) {
                log.debug("Erro ao serializar response para log", e);
            }
            
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
     * @param transferEnabled Se transferências automáticas estão habilitadas
     * @param transferInterval Intervalo de transferência: "Daily", "Weekly", "Monthly"
     */
    public void updateTransferSettings(String recipientId, boolean transferEnabled, String transferInterval) {
        log.info("💰 Atualizando transfer settings do recipient: {}", recipientId);
        log.info("   ├─ Transfer enabled: {}", transferEnabled);
        log.info("   ├─ Transfer interval: {}", transferInterval);
        
        try {
            String url = config.getApi().getUrl() + "/recipients/" + recipientId + "/transfer-settings";
            
            // Montar request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("transfer_enabled", transferEnabled);
            requestBody.put("transfer_interval", transferInterval);
            
            // Pagar.me exige transfer_day quando interval é Daily
            // 0 = Todos os dias (padrão para Daily)
            if ("Daily".equalsIgnoreCase(transferInterval)) {
                requestBody.put("transfer_day", 0);
            }
            
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
}