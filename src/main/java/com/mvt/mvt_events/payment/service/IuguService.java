package com.mvt.mvt_events.payment.service;

import com.mvt.mvt_events.config.IuguConfig;
import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Serviço de integração com o gateway de pagamentos Iugu
 * 
 * <p>Responsável por:</p>
 * <ul>
 *   <li>Criar subcontas de marketplace para motoboys e gerentes</li>
 *   <li>Atualizar dados bancários de subcontas</li>
 *   <li>Criar faturas PIX com split de pagamento (87/5/8)</li>
 *   <li>Validar assinaturas de webhooks</li>
 *   <li>Tratamento de erros com retry automático</li>
 * </ul>
 * 
 * <p><strong>Configuração:</strong></p>
 * <pre>
 * iugu.api.key=test_xxx  # API Key do Iugu
 * iugu.api.url=https://api.iugu.com/v1
 * iugu.account.id=MASTER_ACCOUNT_ID
 * iugu.webhook.token=WEBHOOK_TOKEN
 * </pre>
 * 
 * @see IuguConfig
 * @see <a href="https://dev.iugu.com/reference/api-overview">Documentação Iugu</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IuguService {

    private final RestTemplate iuguRestTemplate;
    private final IuguConfig iuguConfig;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Cria uma subconta de marketplace no Iugu para receber pagamentos
     * 
     * <p><strong>Fluxo:</strong></p>
     * <ol>
     *   <li>Valida dados bancários do usuário</li>
     *   <li>Cria subconta via API Iugu com auto-withdraw habilitado</li>
     *   <li>Retorna account_id para armazenar no User.iuguAccountId</li>
     * </ol>
     * 
     * <p><strong>Endpoint:</strong> POST /v1/marketplace/create_account</p>
     * 
     * @param user Usuário (motoboy ou gerente)
     * @param bankAccount Dados bancários validados
     * @return Response com account_id e status de verificação
     * @throws IuguApiException se ocorrer erro na API Iugu
     * @throws IllegalArgumentException se dados inválidos
     */
    public SubAccountResponse createSubAccount(User user, BankAccount bankAccount) {
        log.info("Criando subconta Iugu para usuário: {} ({})", user.getUsername(), user.getId());

        // Validações
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Usuário deve ter username cadastrado");
        }
        if (!bankAccount.isActive()) {
            throw new IllegalArgumentException("Conta bancária deve estar ativa");
        }

        // Email = username (pode ser email ou telefone)
        String email = user.getUsername();

        // Monta request
        CreateSubAccountRequest request = CreateSubAccountRequest.withDefaults(
                user.getName(),
                email,
                user.getCpf().replaceAll("[^0-9]", ""), // Remove formatação
                bankAccount.getBankCode(),
                bankAccount.getAgency(),
                bankAccount.getAccountNumber(),
                bankAccount.getAccountType().getIuguValue()
        );

        try {
            // Chama API Iugu
            String url = iuguConfig.getApi().getUrl() + "/marketplace/create_account";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<CreateSubAccountRequest> entity = new HttpEntity<>(request, headers);

            log.debug("POST {} - Criando subconta para {}", url, email);
            ResponseEntity<SubAccountResponse> response = iuguRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    SubAccountResponse.class
            );

            SubAccountResponse subAccount = response.getBody();
            if (subAccount == null || subAccount.accountId() == null) {
                throw new IuguApiException("Resposta inválida da API Iugu: " + response);
            }

            log.info("✅ Subconta criada com sucesso: {} (status: {})", 
                    subAccount.accountId(), 
                    subAccount.verificationStatus());

            return subAccount;

        } catch (HttpClientErrorException e) {
            log.error("❌ Erro ao criar subconta Iugu: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IuguApiException("Erro ao criar subconta: " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.error("❌ Erro de comunicação com Iugu: {}", e.getMessage());
            throw new IuguApiException("Erro de comunicação com Iugu", e);
        }
    }

    /**
     * Atualiza dados bancários de uma subconta existente
     * 
     * <p><strong>Endpoint:</strong> PUT /v1/accounts/{account_id}/bank_verification</p>
     * 
     * @param iuguAccountId ID da subconta no Iugu
     * @param bankAccount Novos dados bancários
     * @throws IuguApiException se ocorrer erro na API
     */
    public void updateBankAccount(String iuguAccountId, BankAccount bankAccount) {
        log.info("Atualizando dados bancários da subconta: {}", iuguAccountId);

        try {
            String url = iuguConfig.getApi().getUrl() + "/accounts/" + iuguAccountId + "/bank_verification";
            HttpHeaders headers = createAuthHeaders();

            // Monta body com novos dados
            var body = new java.util.HashMap<String, String>();
            body.put("bank", bankAccount.getBankCode());
            body.put("bank_ag", bankAccount.getAgency());
            body.put("bank_cc", bankAccount.getAccountNumber());
            body.put("account_type", bankAccount.getAccountType().getIuguValue());

            HttpEntity<java.util.Map<String, String>> entity = new HttpEntity<>(body, headers);

            log.debug("PUT {} - Atualizando dados bancários", url);
            iuguRestTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            log.info("✅ Dados bancários atualizados com sucesso: {}", iuguAccountId);

        } catch (HttpClientErrorException e) {
            log.error("❌ Erro ao atualizar dados bancários: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IuguApiException("Erro ao atualizar dados bancários: " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.error("❌ Erro de comunicação com Iugu: {}", e.getMessage());
            throw new IuguApiException("Erro de comunicação com Iugu", e);
        }
    }

    /**
     * Cria uma fatura PIX com split de pagamento entre motoboy, gerente e plataforma
     * 
     * <p><strong>Split padrão:</strong></p>
     * <ul>
     *   <li>87% para o motoboy</li>
     *   <li>5% para o gerente</li>
     *   <li>8% para a plataforma (conta master)</li>
     *   <li>R$ 0,59 taxa fixa do Iugu (descontada do total)</li>
     * </ul>
     * 
     * <p><strong>Endpoint:</strong> POST /v1/invoices</p>
     * 
     * @param deliveryId ID da entrega
     * @param amount Valor total da entrega
     * @param clientEmail Email do cliente (pagador)
     * @param motoboyAccountId ID da subconta do motoboy
     * @param managerAccountId ID da subconta do gerente
     * @return Response com QR Code PIX e dados da fatura
     * @throws IuguApiException se ocorrer erro na API
     */
    public InvoiceResponse createInvoiceWithSplit(
            String deliveryId,
            BigDecimal amount,
            String clientEmail,
            String motoboyAccountId,
            String managerAccountId
    ) {
        log.info("Criando fatura PIX para entrega {} - Valor: R$ {}", deliveryId, amount);

        // Validações
        if (amount.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Valor mínimo é R$ 1,00");
        }
        if (motoboyAccountId == null || motoboyAccountId.isBlank()) {
            throw new IllegalArgumentException("Motoboy deve ter subconta Iugu");
        }
        if (managerAccountId == null || managerAccountId.isBlank()) {
            throw new IllegalArgumentException("Gerente deve ter subconta Iugu");
        }

        // Calcula splits (percentuais configuráveis)
        List<SplitRule> splits = buildSplitRules(motoboyAccountId, managerAccountId);

        // Data de vencimento: hoje + 1 dia
        String dueDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);

        // Monta request
        CreateInvoiceRequest request = CreateInvoiceRequest.forDelivery(
                clientEmail,
                dueDate,
                amount,
                deliveryId,
                splits
        );

        try {
            String url = iuguConfig.getApi().getUrl() + "/invoices";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<CreateInvoiceRequest> entity = new HttpEntity<>(request, headers);

            log.debug("POST {} - Criando invoice com splits", url);
            ResponseEntity<InvoiceResponse> response = iuguRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    InvoiceResponse.class
            );

            InvoiceResponse invoice = response.getBody();
            if (invoice == null || invoice.id() == null) {
                throw new IuguApiException("Resposta inválida da API Iugu: " + response);
            }

            log.info("✅ Fatura criada com sucesso: {} (PIX QR Code gerado)", invoice.id());
            log.debug("   └─ Splits: {}% motoboy ({}), {}% gerente ({}), {}% plataforma",
                    iuguConfig.getSplit().getMotoboyPercentage(),
                    motoboyAccountId,
                    iuguConfig.getSplit().getManagerPercentage(),
                    managerAccountId,
                    iuguConfig.getSplit().getPlatformPercentage()
            );

            return invoice;

        } catch (HttpClientErrorException e) {
            log.error("❌ Erro ao criar fatura: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IuguApiException("Erro ao criar fatura: " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.error("❌ Erro de comunicação com Iugu: {}", e.getMessage());
            throw new IuguApiException("Erro de comunicação com Iugu", e);
        }
    }

    /**
     * Consulta o status atual de uma subconta no Iugu
     * 
     * <p>Usado para verificar se a validação bancária foi concluída.
     * O processo de verificação é assíncrono e pode demorar 2-5 dias úteis.</p>
     * 
     * <p><strong>Endpoint:</strong> GET /v1/accounts/{account_id}</p>
     * 
     * <p><strong>Possíveis status de verificação:</strong></p>
     * <ul>
     *   <li><strong>pending:</strong> Aguardando verificação</li>
     *   <li><strong>verified:</strong> Dados verificados, pode receber pagamentos</li>
     *   <li><strong>rejected:</strong> Dados rejeitados, precisa corrigir</li>
     * </ul>
     * 
     * @param iuguAccountId ID da subconta no Iugu
     * @return Response com status atualizado
     * @throws IuguApiException se ocorrer erro na API
     * @throws IllegalArgumentException se accountId inválido
     */
    public SubAccountResponse getSubAccountStatus(String iuguAccountId) {
        if (iuguAccountId == null || iuguAccountId.isBlank()) {
            throw new IllegalArgumentException("Account ID é obrigatório");
        }

        log.debug("Consultando status da subconta: {}", iuguAccountId);

        try {
            String url = iuguConfig.getApi().getUrl() + "/accounts/" + iuguAccountId;
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<SubAccountResponse> response = iuguRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    SubAccountResponse.class
            );

            SubAccountResponse account = response.getBody();
            if (account == null) {
                throw new IuguApiException("Resposta inválida da API Iugu");
            }

            log.debug("📊 Status da subconta {}: {} (active: {}, verified: {})",
                    iuguAccountId,
                    account.verificationStatus(),
                    account.isActive(),
                    account.canReceivePayments()
            );

            return account;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                log.error("❌ Subconta não encontrada no Iugu: {}", iuguAccountId);
                throw new IuguApiException("Subconta não encontrada: " + iuguAccountId, e);
            }
            log.error("❌ Erro ao consultar subconta: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new IuguApiException("Erro ao consultar subconta: " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.error("❌ Erro de comunicação com Iugu: {}", e.getMessage());
            throw new IuguApiException("Erro de comunicação com Iugu", e);
        }
    }

    /**
     * Valida a assinatura de um webhook do Iugu
     * 
     * <p>O Iugu envia um header <code>X-Iugu-Signature</code> com um token HMAC
     * para validar a autenticidade do webhook.</p>
     * 
     * @param signature Valor do header X-Iugu-Signature
     * @return true se assinatura válida
     */
    public boolean validateWebhookSignature(String signature) {
        if (signature == null || signature.isBlank()) {
            log.warn("⚠️ Webhook sem assinatura recebido");
            return false;
        }

        // Validação simples: compara com token configurado
        // Em produção, implementar HMAC SHA256
        String expectedToken = iuguConfig.getWebhook().getToken();
        boolean isValid = expectedToken.equals(signature);

        if (isValid) {
            log.debug("✅ Assinatura do webhook validada");
        } else {
            log.warn("❌ Assinatura do webhook inválida");
        }

        return isValid;
    }

    /**
     * Constrói as regras de split conforme configuração
     * 
     * @param motoboyAccountId ID da subconta do motoboy
     * @param managerAccountId ID da subconta do gerente
     * @return Lista de SplitRules
     */
    private List<SplitRule> buildSplitRules(String motoboyAccountId, String managerAccountId) {
        List<SplitRule> splits = new ArrayList<>();

        // 87% para motoboy
        splits.add(SplitRule.percentage(
                motoboyAccountId,
                iuguConfig.getSplit().getMotoboyPercentage(),
                "Pagamento ao motoboy"
        ));

        // 5% para gerente
        splits.add(SplitRule.percentage(
                managerAccountId,
                iuguConfig.getSplit().getManagerPercentage(),
                "Comissão do gerente"
        ));

        // 8% para plataforma (receiverId = null = conta master)
        splits.add(SplitRule.percentage(
                null,
                iuguConfig.getSplit().getPlatformPercentage(),
                "Taxa da plataforma"
        ));

        return splits;
    }

    /**
     * Cria headers HTTP com autenticação Basic Auth
     * 
     * <p>O Iugu usa Basic Auth com API Key como username (password vazio).</p>
     * 
     * @return HttpHeaders configurados
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Basic Auth: apiKey + ":"
        String auth = iuguConfig.getApi().getKey() + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        return headers;
    }

    /**
     * Cria uma fatura no Iugu com múltiplos splits consolidados
     * 
     * <p>Este método aceita uma lista de RecipientSplit já calculados
     * pelo SplitCalculator e cria a invoice no Iugu.</p>
     * 
     * @param clientEmail Email do cliente que vai pagar
     * @param totalCents Valor total em centavos
     * @param description Descrição da invoice
     * @param expirationHours Horas até expirar
     * @param splits Lista de splits consolidados
     * @return InvoiceResponse com QR Code PIX
     * @throws IuguApiException se ocorrer erro na API
     */
    public InvoiceResponse createInvoiceWithConsolidatedSplits(
            String clientEmail,
            int totalCents,
            String description,
            int expirationHours,
            List<com.mvt.mvt_events.dto.RecipientSplit> splits
    ) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("🚀 PREPARANDO REQUEST PARA IUGU");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📧 Email: {}", clientEmail);
        log.info("💰 Valor Total: R$ {} ({}¢)", 
                BigDecimal.valueOf(totalCents).divide(BigDecimal.valueOf(100)),
                totalCents);
        log.info("📝 Descrição: {}", description);
        log.info("⏰ Expiração: {} horas", expirationHours);
        log.info("───────────────────────────────────────────────────────────────");

        // Converte RecipientSplit para SplitRule (formato Iugu)
        log.info("🔄 Convertendo splits para formato Iugu (excluindo PLATFORM):");
        List<SplitRule> iuguSplits = splits.stream()
                .filter(split -> {
                    boolean isPlatform = split.getType() == com.mvt.mvt_events.dto.RecipientSplit.RecipientType.PLATFORM;
                    if (isPlatform) {
                        log.info("   ⏭️  Pulando PLATFORM ({}¢) - receberá automaticamente o resto", 
                                split.getAmountCents());
                    }
                    return !isPlatform;
                })
                .map(split -> {
                    log.info("   ✅ {} {}: {}¢ (R$ {})", 
                            split.getType().name(),
                            split.getIuguAccountId(),
                            split.getAmountCents(),
                            BigDecimal.valueOf(split.getAmountCents()).divide(BigDecimal.valueOf(100)));
                    
                    return SplitRule.fixedCents(
                            split.getIuguAccountId(),
                            split.getAmountCents(),
                            split.getType().name()
                    );
                })
                .collect(java.util.stream.Collectors.toList());

        log.info("───────────────────────────────────────────────────────────────");
        log.info("📦 Splits para Iugu: {} (PLATFORM não incluído)", iuguSplits.size());

        // Monta request
        InvoiceRequest request = new InvoiceRequest();
        request.setEmail(clientEmail);
        request.setDueDate(LocalDateTime.now().plusHours(expirationHours));
        request.setPayableWith("pix");
        
        // Item único
        InvoiceItemRequest item = new InvoiceItemRequest();
        item.setDescription(description);
        item.setQuantity(1);
        item.setPriceCents(totalCents);
        request.setItems(java.util.Collections.singletonList(item));
        
        // Splits convertidos
        List<SplitRequest> splitRequests = iuguSplits.stream()
                .map(splitRule -> {
                    SplitRequest sr = new SplitRequest();
                    sr.setReceiverId(splitRule.receiverId());
                    sr.setCents(splitRule.cents());
                    return sr;
                })
                .collect(java.util.stream.Collectors.toList());
        request.setSplits(splitRequests);
        
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📤 ENVIANDO REQUEST PARA IUGU API");
        log.info("═══════════════════════════════════════════════════════════════");

        InvoiceResponse response = createInvoice(request);
        
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("✅ RESPOSTA RECEBIDA DO IUGU");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("🆔 Invoice ID: {}", response.id());
        log.info("🔗 Secure URL: {}", response.secureUrl());
        log.info("🖼️  QR Code URL: {}", response.pixQrCodeUrl());
        log.info("📋 QR Code: {} caracteres", response.pixQrCode() != null ? response.pixQrCode().length() : 0);
        log.info("═══════════════════════════════════════════════════════════════");
        
        return response;
    }

    /**
     * Cria uma fatura no Iugu com o InvoiceRequest customizado
     * 
     * <p>Este método aceita um InvoiceRequest já montado pelo PaymentService,
     * permitindo total flexibilidade na criação da fatura.</p>
     * 
     * <p><strong>Modos de Operação:</strong></p>
     * <ul>
     *   <li><strong>dry-run:</strong> Mock local (não chama Iugu, retorna dados fake)</li>
     *   <li><strong>sandbox:</strong> Iugu Sandbox (teste com API key test_xxx)</li>
     *   <li><strong>production:</strong> Iugu Production (real com API key live_xxx)</li>
     * </ul>
     * 
     * @param request Dados da fatura (items, splits, due_date, etc)
     * @return InvoiceResponse com QR Code PIX e dados da fatura
     * @throws IuguApiException se ocorrer erro na API
     */
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        log.info("📝 Criando fatura Iugu - Email: {}, Due Date: {}", 
                request.getEmail(), request.getDueDate());

        // Modo DRY-RUN: retorna mock sem chamar Iugu
        if (iuguConfig.isDryRun()) {
            return createMockInvoice(request);
        }

        // Modo SANDBOX ou PRODUCTION: chama Iugu real
        try {
            String url = iuguConfig.getApi().getUrl() + "/invoices";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<InvoiceRequest> entity = new HttpEntity<>(request, headers);

            log.debug("POST {} - Criando invoice", url);
            ResponseEntity<InvoiceResponse> response = iuguRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    InvoiceResponse.class
            );

            InvoiceResponse invoice = response.getBody();
            if (invoice == null || invoice.id() == null) {
                throw new IuguApiException("Resposta inválida da API Iugu: " + response);
            }

            log.info("✅ Fatura criada com sucesso: {}", invoice.id());
            return invoice;

        } catch (HttpClientErrorException e) {
            log.error("❌ Erro ao criar fatura: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IuguApiException("Erro ao criar fatura: " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.error("❌ Erro ao comunicar com Iugu", e);
            throw new IuguApiException("Erro ao criar fatura: " + e.getMessage(), e);
        }
    }

    /**
     * Cria uma fatura MOCK para testes locais (modo dry-run)
     * 
     * <p>Este método gera uma resposta simulada sem chamar o Iugu,
     * útil para desenvolvimento e testes sem depender de API keys válidas.</p>
     * 
     * @param request Dados da fatura (usado para gerar mock realista)
     * @return InvoiceResponse simulada com QR Code fake
     */
    private InvoiceResponse createMockInvoice(InvoiceRequest request) {
        log.warn("🧪 DRY-RUN MODE: Criando fatura MOCK (não será enviada ao Iugu)");
        
        String mockId = "MOCK_INV_" + System.currentTimeMillis();
        String mockQrCode = "00020126360014BR.GOV.BCB.PIX0114+5511999999999520400005303986540525.005802BR5913MVT Events SA6009SAO PAULO62070503***" + mockId.substring(mockId.length() - 6) + "6304ABCD";
        String mockQrCodeUrl = "https://via.placeholder.com/300x300.png?text=QR+CODE+MOCK";
        String mockSecureUrl = "https://mock.iugu.com/invoice/" + mockId;
        
        int totalCents = request.getItems().stream()
                .mapToInt(item -> item.getPriceCents() * item.getQuantity())
                .sum();
        
        log.info("📝 Mock Invoice criada:");
        log.info("   🆔 ID: {}", mockId);
        log.info("   💰 Valor: {}¢ (R$ {})", totalCents, BigDecimal.valueOf(totalCents).divide(BigDecimal.valueOf(100)));
        log.info("   📧 Email: {}", request.getEmail());
        log.info("   📦 Splits: {} recipient(s)", request.getSplits() != null ? request.getSplits().size() : 0);
        log.info("   🔗 Secure URL: {}", mockSecureUrl);
        log.info("   ⚠️  Este é um pagamento SIMULADO - nenhum valor real será cobrado");
        
        return new InvoiceResponse(
                mockId,
                mockQrCode,
                mockQrCodeUrl,
                mockSecureUrl,
                "pending",
                totalCents,
                request.getDueDate() != null ? request.getDueDate().toString() : LocalDateTime.now().plusDays(1).toString(),
                request.getEmail(),
                null  // customVariables
        );
    }

    /**
     * Exception customizada para erros da API Iugu
     */
    public static class IuguApiException extends RuntimeException {
        public IuguApiException(String message) {
            super(message);
        }

        public IuguApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
