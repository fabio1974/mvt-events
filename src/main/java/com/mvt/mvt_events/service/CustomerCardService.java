package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.CustomerCard;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.BillingAddressDTO;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.repository.CustomerCardRepository;
import com.mvt.mvt_events.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service para gerenciar cartões de crédito tokenizados dos clientes.
 * 
 * SEGURANÇA (PCI Compliance):
 * - NUNCA processa número completo do cartão no backend
 * - Espera token do Pagar.me já criado no frontend
 * - Armazena apenas: token + últimos 4 dígitos + bandeira
 * 
 * Funcionalidades:
 * - Adicionar cartão (recebe token do frontend)
 * - Listar cartões do cliente
 * - Definir cartão padrão
 * - Deletar cartão (soft delete)
 * - Verificar expiração automática
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerCardService {

    private final CustomerCardRepository cardRepository;
    private final UserRepository userRepository;
    private final PagarMeService pagarMeService;
    private final com.mvt.mvt_events.repository.DeliveryRepository deliveryRepository;
    private final com.mvt.mvt_events.repository.PaymentRepository paymentRepository;
    private final com.mvt.mvt_events.service.SiteConfigurationService siteConfigurationService;
    private final TransactionTemplate transactionTemplate;

    /**
     * Adiciona um novo cartão para o cliente.
     * 
     * @param customerId ID do cliente
     * @param cardToken Token do cartão (gerado no frontend com Pagar.me JS)
     * @param setAsDefault Se deve marcar como cartão padrão
     * @param billingAddress Endereço de cobrança (opcional, repassado ao Pagar.me)
     * @return Cartão salvo
     */
    @Transactional
    public CustomerCard addCard(UUID customerId, String cardToken, Boolean setAsDefault, BillingAddressDTO billingAddress) {
        log.info("Adicionando cartão para customer: {} | Token: {}", customerId, cardToken);

        // Validar cliente
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // Garantir que cliente tem pagarmeCustomerId
        String pagarmeCustomerId = customer.getPagarmeCustomerId();
        if (pagarmeCustomerId == null) {
            // Criar customer no Pagar.me
            pagarmeCustomerId = pagarMeService.createCustomer(customer);
            customer.setPagarmeCustomerId(pagarmeCustomerId);
            userRepository.save(customer);
            log.info("   └─ Customer Pagar.me criado: {}", pagarmeCustomerId);
        }

        // Criar cartão no Pagar.me (com billing address se fornecido)
        Map<String, Object> cardData = pagarMeService.createCard(pagarmeCustomerId, cardToken, billingAddress);
        
        // Extrair dados do cartão
        String pagarmeCardId = (String) cardData.get("id");
        String lastFourDigits = (String) cardData.get("last_four_digits");
        String brandStr = (String) cardData.get("brand");
        String holderName = (String) cardData.get("holder_name");
        Integer expMonth = (Integer) cardData.get("exp_month");
        Integer expYear = (Integer) cardData.get("exp_year");

        // Verificar se cartão já não foi cadastrado
        if (cardRepository.findByPagarmeCardId(pagarmeCardId).isPresent()) {
            throw new RuntimeException("Este cartão já está cadastrado");
        }

        // Criar entidade
        CustomerCard card = new CustomerCard();
        card.setCustomer(customer);
        card.setPagarmeCardId(pagarmeCardId);
        card.setLastFourDigits(lastFourDigits);
        card.setBrand(mapBrand(brandStr));
        card.setHolderName(holderName);
        card.setExpMonth(expMonth);
        card.setExpYear(expYear);
        card.setIsActive(true);
        card.setIsVerified(true); // Já verificado pelo Pagar.me

        // Se não tem cartões, este é o padrão automaticamente
        boolean hasCards = cardRepository.countActiveCardsByCustomerId(customerId) > 0;
        if (!hasCards || Boolean.TRUE.equals(setAsDefault)) {
            cardRepository.clearDefaultFlag(customerId);
            card.setIsDefault(true);
        } else {
            card.setIsDefault(false);
        }

        CustomerCard saved = cardRepository.save(card);
        log.info("Cartão adicionado com sucesso: {} | Default: {}", saved.getId(), saved.getIsDefault());
        
        // Se foi marcado como padrão, verificar deliveries ativas não pagas
        // Wrapped em try-catch para NÃO afetar a criação do cartão se falhar
        if (saved.getIsDefault()) {
            try {
                processUnpaidDeliveries(customer, saved);
            } catch (Exception e) {
                log.error("⚠️ Erro ao processar deliveries não pagas (cartão foi salvo com sucesso): {}", e.getMessage(), e);
            }
        }
        
        return saved;
    }
    
    /**
     * Retry de pagamento para TODAS as deliveries não pagas do customer logado.
     * Busca o cartão padrão atual e cria pagamento para cada delivery IN_TRANSIT ou COMPLETED sem pagamento.
     * 
     * @param customerId ID do customer logado
     * @return Mapa com resultado: total encontradas, sucesso, falhas e detalhes
     */
    public Map<String, Object> retryUnpaidDeliveries(UUID customerId) {
        log.info("🔄 Retry de pagamentos não pagos para customer: {}", customerId);
        
        // 1. Buscar customer
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        
        // 2. Buscar cartão padrão
        CustomerCard defaultCard = cardRepository.findDefaultCardByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Nenhum cartão padrão cadastrado. Cadastre um cartão primeiro."));
        
        // 3. Verificar se cartão está ativo e não expirado
        if (!defaultCard.getIsActive()) {
            throw new RuntimeException("Cartão padrão está inativo. Defina outro cartão como padrão.");
        }
        if (defaultCard.isExpired()) {
            throw new RuntimeException("Cartão padrão está expirado. Defina outro cartão como padrão.");
        }
        
        // 4. Buscar deliveries não pagas (IN_TRANSIT ou COMPLETED) via query filtrada
        var statuses = List.of(
                com.mvt.mvt_events.jpa.Delivery.DeliveryStatus.IN_TRANSIT,
                com.mvt.mvt_events.jpa.Delivery.DeliveryStatus.COMPLETED
        );
        var unpaidDeliveries = deliveryRepository.findByClientIdAndStatusesWithJoins(customerId, statuses)
                .stream()
                .filter(d -> !Boolean.TRUE.equals(d.getPaymentCompleted()) || !Boolean.TRUE.equals(d.getPaymentCaptured()))
                .toList();
        
        if (unpaidDeliveries.isEmpty()) {
            log.info("   └─ Nenhuma delivery não paga encontrada");
            return Map.of(
                "message", "Nenhuma entrega pendente de pagamento",
                "total", 0,
                "success", 0,
                "failed", 0
            );
        }
        
        // 5. Filtrar deliveries que JÁ possuem pagamento PENDING ou PAID (evitar duplicata)
        var deliveryIds = unpaidDeliveries.stream()
                .map(d -> d.getId())
                .toList();
        
        var existingPayments = paymentRepository.findPendingOrCompletedPaymentsForDeliveries(deliveryIds);
        
        // Coletar IDs de deliveries que já têm pagamento ativo
        var deliveriesWithPayment = new java.util.HashSet<Long>();
        for (var payment : existingPayments) {
            for (var d : payment.getDeliveries()) {
                deliveriesWithPayment.add(d.getId());
            }
        }
        
        var deliveriesToProcess = unpaidDeliveries.stream()
                .filter(d -> !deliveriesWithPayment.contains(d.getId()))
                .toList();
        
        if (deliveriesToProcess.isEmpty()) {
            log.info("   └─ Todas as deliveries já possuem pagamento pendente/pago");
            return Map.of(
                "message", "Todas as entregas já possuem pagamento em processamento",
                "total", unpaidDeliveries.size(),
                "success", 0,
                "failed", 0,
                "skipped", unpaidDeliveries.size()
            );
        }
        
        log.info("   ├─ {} deliveries não pagas, {} a processar (excluindo duplicatas)", 
                unpaidDeliveries.size(), deliveriesToProcess.size());
        
        // 6. Processar cada delivery em transação independente
        //    Se uma falhar, as outras não são afetadas
        int[] counters = {0, 0}; // [success, failed]
        var details = new java.util.ArrayList<Map<String, Object>>();
        
        for (var delivery : deliveriesToProcess) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    // Re-carregar delivery dentro desta transação para evitar LazyInitializationException
                    var freshDelivery = deliveryRepository.findByIdWithJoins(delivery.getId())
                            .orElseThrow(() -> new RuntimeException("Delivery #" + delivery.getId() + " não encontrada"));
                    createPaymentForDelivery(freshDelivery, customer, defaultCard);
                });
                counters[0]++;
                details.add(Map.of(
                    "deliveryId", delivery.getId().toString(),
                    "status", "success",
                    "amount", delivery.getShippingFee().toString()
                ));
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Constraint violation = já existe pagamento ativo (race condition)
                log.warn("   ├─ ⚠️ Delivery #{} já possui pagamento ativo (constraint)", delivery.getId());
                details.add(Map.of(
                    "deliveryId", delivery.getId().toString(),
                    "status", "skipped",
                    "error", "Já existe pagamento em processamento para esta entrega"
                ));
            } catch (Exception e) {
                counters[1]++;
                details.add(Map.of(
                    "deliveryId", delivery.getId().toString(),
                    "status", "failed",
                    "error", e.getMessage() != null ? e.getMessage() : "Erro desconhecido"
                ));
                log.error("   ├─ ❌ Falha delivery #{}: {}", delivery.getId(), e.getMessage());
            }
        }
        
        int success = counters[0];
        int failed = counters[1];
        
        String message = success > 0 
            ? String.format("Pagamento criado para %d entrega(s) com cartão **** %s", success, defaultCard.getLastFourDigits())
            : "Não foi possível criar pagamentos";
        
        log.info("   └─ ✅ Retry concluído: {} sucesso, {} falhas de {} total", success, failed, deliveriesToProcess.size());
        
        return Map.of(
            "message", message,
            "total", deliveriesToProcess.size(),
            "success", success,
            "failed", failed,
            "card", Map.of(
                "lastFourDigits", defaultCard.getLastFourDigits(),
                "brand", defaultCard.getBrand().getDisplayName()
            ),
            "details", details
        );
    }

    /**
     * Processa deliveries ativas não pagas após troca de cartão padrão.
     * Cria pagamentos automáticos com o novo cartão.
     * 
     * Nota: ACCEPTED não é incluído pois em RIDE o pagamento só ocorre ao entrar em trânsito.
     */
    private void processUnpaidDeliveries(User customer, CustomerCard newCard) {
        log.info("🔍 Verificando deliveries ativas não pagas para customer: {}", customer.getId());
        
        // Buscar deliveries não pagas (IN_TRANSIT ou COMPLETED) via query filtrada
        // ACCEPTED fica de fora: em RIDE, pagamento só ocorre ao iniciar viagem
        var statuses = List.of(
                com.mvt.mvt_events.jpa.Delivery.DeliveryStatus.IN_TRANSIT,
                com.mvt.mvt_events.jpa.Delivery.DeliveryStatus.COMPLETED
        );
        var unpaidDeliveries = deliveryRepository.findByClientIdAndStatusesWithJoins(customer.getId(), statuses)
                .stream()
                .filter(d -> !Boolean.TRUE.equals(d.getPaymentCompleted()) || !Boolean.TRUE.equals(d.getPaymentCaptured()))
                .toList();
        
        if (unpaidDeliveries.isEmpty()) {
            log.info("   └─ Nenhuma delivery não paga encontrada");
            return;
        }
        
        log.info("   ├─ Encontradas {} deliveries não pagas", unpaidDeliveries.size());
        
        // Para cada delivery, tentar criar pagamento
        for (var delivery : unpaidDeliveries) {
            try {
                log.info("   ├─ Processando delivery #{}", delivery.getId());
                createPaymentForDelivery(delivery, customer, newCard);
            } catch (Exception e) {
                log.error("   ├─ ❌ Erro ao criar pagamento para delivery #{}: {}", 
                        delivery.getId(), e.getMessage());
            }
        }
        
        log.info("   └─ ✅ Processamento de deliveries não pagas concluído");
    }
    
    /**
     * Cria pagamento automático para uma delivery usando o novo cartão.
     */
    private void createPaymentForDelivery(
            com.mvt.mvt_events.jpa.Delivery delivery, 
            User customer, 
            CustomerCard card) {
        
        log.info("      ├─ Criando pagamento para delivery #{} com cartão **** {}", 
                delivery.getId(), card.getLastFourDigits());
        
        // Buscar courier e organizer
        User courier = delivery.getCourier();
        if (courier == null) {
            log.warn("      └─ ⚠️ Delivery sem courier, pulando");
            return;
        }
        
        String courierRecipientId = courier.getPagarmeRecipientId();
        if (courierRecipientId == null || courierRecipientId.isBlank()) {
            log.warn("      └─ ⚠️ Courier sem recipientId, pulando");
            return;
        }
        
        String organizerRecipientId = null;
        if (delivery.getOrganizer() != null) {
            organizerRecipientId = delivery.getOrganizer().getPagarmeRecipientId();
        }
        
        // Buscar recipientId da plataforma
        var config = siteConfigurationService.getActiveConfiguration();
        String platformRecipientId = config.getPagarmeRecipientId();
        
        // Preparar billing address
        var billingAddress = com.mvt.mvt_events.payment.dto.OrderRequest.BillingAddressRequest.builder()
                .line1(delivery.getFromAddress() != null ? delivery.getFromAddress() : "Endereço não informado")
                .zipCode("00000000")
                .city("São Paulo")
                .state("SP")
                .country("BR")
                .build();
        
        // Criar order no Pagar.me
        try {
            var orderResponse = pagarMeService.createOrderWithCreditCardSplit(
                    delivery.getShippingFee(),
                    "Entrega #" + delivery.getId() + " (Retry com novo cartão)",
                    card.getPagarmeCardId(),
                    customer.getName() != null ? customer.getName() : customer.getUsername(),
                    customer.getUsername(),
                    customer.getDocumentNumber() != null ? customer.getDocumentNumber() : "00000000000",
                    billingAddress,
                    courierRecipientId,
                    organizerRecipientId,
                    "ZAPI10",
                    platformRecipientId
            );
            
            log.info("      ├─ ✅ Order criada: {} (status: {})", orderResponse.getId(), orderResponse.getStatus());
            
            // Verificar se a order foi recusada pelo Pagar.me
            String orderStatus = orderResponse.getStatus();
            if ("failed".equalsIgnoreCase(orderStatus) || "canceled".equalsIgnoreCase(orderStatus)) {
                log.warn("      └─ ⚠️ Order recusada pelo Pagar.me (status: {}). Não salvando payment.", orderStatus);
                throw new RuntimeException("Pagamento recusado pela operadora do cartão (status: " + orderStatus + ")");
            }
            
            // Determinar status do payment com base na resposta do Pagar.me
            // Se já veio "paid", salvar direto como COMPLETED (sandbox não envia webhook para localhost)
            boolean isPaid = "paid".equalsIgnoreCase(orderStatus);
            var paymentStatus = isPaid 
                    ? com.mvt.mvt_events.jpa.PaymentStatus.PAID 
                    : com.mvt.mvt_events.jpa.PaymentStatus.PENDING;
            
            // Criar Payment no banco
            var payment = new com.mvt.mvt_events.jpa.Payment();
            payment.setProviderPaymentId(orderResponse.getId());
            payment.setAmount(delivery.getShippingFee());
            payment.setCurrency(com.mvt.mvt_events.jpa.Currency.BRL);
            payment.setPaymentMethod(com.mvt.mvt_events.jpa.PaymentMethod.CREDIT_CARD);
            payment.setProvider(com.mvt.mvt_events.jpa.PaymentProvider.PAGARME);
            payment.setPayer(customer);
            payment.setStatus(paymentStatus);
            payment.addDelivery(delivery);
            
            if (isPaid) {
                // Marcar delivery como paga
                delivery.setPaymentCaptured(true);
                delivery.setPaymentCompleted(true);
                deliveryRepository.save(delivery);
            }
            
            paymentRepository.saveAndFlush(payment);
            
            log.info("      └─ ✅ Payment criado com status {} {}", paymentStatus, 
                    isPaid ? "(pago direto)" : "(aguardando webhook)");
            
        } catch (Exception e) {
            log.error("      └─ ❌ Erro ao criar order: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Sobrecarga para manter compatibilidade com assinatura antiga.
     */
    @Transactional
    public CustomerCard addCard(
            UUID customerId,
            String pagarmeCardId,
            String lastFourDigits,
            CustomerCard.CardBrand brand,
            String holderName,
            Integer expMonth,
            Integer expYear,
            Boolean setAsDefault) {
        
        // Esta versão é para quando já temos os dados extraídos
        // Usado quando o cartão já foi criado no Pagar.me antes
        log.info("Adicionando cartão (dados pré-extraídos) para customer: {}", customerId);

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        if (cardRepository.findByPagarmeCardId(pagarmeCardId).isPresent()) {
            throw new RuntimeException("Este cartão já está cadastrado");
        }

        CustomerCard card = new CustomerCard();
        card.setCustomer(customer);
        card.setPagarmeCardId(pagarmeCardId);
        card.setLastFourDigits(lastFourDigits);
        card.setBrand(brand);
        card.setHolderName(holderName);
        card.setExpMonth(expMonth);
        card.setExpYear(expYear);
        card.setIsActive(true);
        card.setIsVerified(false);

        boolean hasCards = cardRepository.countActiveCardsByCustomerId(customerId) > 0;
        if (!hasCards || Boolean.TRUE.equals(setAsDefault)) {
            cardRepository.clearDefaultFlag(customerId);
            card.setIsDefault(true);
        } else {
            card.setIsDefault(false);
        }

        CustomerCard saved = cardRepository.save(card);
        log.info("Cartão adicionado com sucesso: {} | Default: {}", saved.getId(), saved.getIsDefault());
        
        return saved;
    }

    /**
     * Lista todos os cartões ativos do cliente.
     * Ordena por: padrão primeiro, depois por último uso.
     */
    @Transactional(readOnly = true)
    public List<CustomerCard> listCustomerCards(UUID customerId) {
        return cardRepository.findActiveCardsByCustomerId(customerId);
    }

    /**
     * Busca o cartão padrão do cliente.
     */
    @Transactional(readOnly = true)
    public CustomerCard getDefaultCard(UUID customerId) {
        return cardRepository.findDefaultCardByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente não possui cartão padrão cadastrado"));
    }

    /**
     * Define um cartão como padrão.
     * Remove flag de todos os outros cartões do cliente.
     */
    @Transactional
    public CustomerCard setDefaultCard(UUID customerId, Long cardId, Authentication authentication) {
        // Verificar autorização
        validateCustomerAccess(customerId, authentication);

        // Buscar cartão
        CustomerCard card = cardRepository.findByIdAndCustomerId(cardId, customerId)
                .orElseThrow(() -> new RuntimeException("Cartão não encontrado"));

        // Verificar se está ativo
        if (!card.getIsActive()) {
            throw new RuntimeException("Cartão inativo não pode ser definido como padrão");
        }

        // Verificar se está expirado
        if (card.isExpired()) {
            throw new RuntimeException("Cartão expirado não pode ser definido como padrão");
        }

        // Remover flag de outros cartões
        cardRepository.clearDefaultFlag(customerId);

        // Definir como padrão
        card.setIsDefault(true);
        CustomerCard updated = cardRepository.save(card);

        log.info("Cartão {} definido como padrão para customer {}", cardId, customerId);
        return updated;
    }

    /**
     * Deleta um cartão (soft delete).
     * Mantém no banco para auditoria e histórico de transações.
     * 
     * Deleta PRIMEIRO no Pagar.me e depois localmente.
     * Se falhar no Pagar.me, a exclusão local não acontece (consistência).
     */
    @Transactional
    public void deleteCard(UUID customerId, Long cardId, Authentication authentication) {
        // Verificar autorização
        validateCustomerAccess(customerId, authentication);

        // Buscar cartão
        CustomerCard card = cardRepository.findByIdAndCustomerId(cardId, customerId)
                .orElseThrow(() -> new RuntimeException("Cartão não encontrado"));

        // Buscar customer para obter pagarmeCustomerId
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // 1️⃣ DELETAR NO PAGAR.ME PRIMEIRO
        //    Se falhar aqui, a exceção impede a exclusão local (rollback automático)
        if (customer.getPagarmeCustomerId() != null && card.getPagarmeCardId() != null) {
            try {
                pagarMeService.deleteCard(customer.getPagarmeCustomerId(), card.getPagarmeCardId());
                log.info("   └─ ✅ Cartão deletado no Pagar.me: {}", card.getPagarmeCardId());
            } catch (Exception e) {
                log.error("   └─ ❌ Falha ao deletar no Pagar.me, abortando exclusão local", e);
                throw new RuntimeException("Não foi possível deletar o cartão no Pagar.me: " + e.getMessage(), e);
            }
        } else {
            log.warn("   └─ ⚠️ Cartão sem ID do Pagar.me, deletando apenas localmente");
        }

        // 2️⃣ SE CHEGOU AQUI, Pagar.me deletou com sucesso → deletar localmente
        card.softDelete();
        cardRepository.save(card);

        log.info("Cartão {} deletado (soft) para customer {}", cardId, customerId);

        // Se era o padrão, definir outro como padrão
        if (Boolean.TRUE.equals(card.getIsDefault())) {
            List<CustomerCard> activeCards = cardRepository.findActiveCardsByCustomerId(customerId);
            if (!activeCards.isEmpty()) {
                CustomerCard newDefault = activeCards.get(0);
                newDefault.setIsDefault(true);
                cardRepository.save(newDefault);
                log.info("Novo cartão padrão: {} para customer {}", newDefault.getId(), customerId);
            }
        }
    }

    /**
     * Atualiza timestamp de último uso do cartão.
     * Chamado após pagamento bem-sucedido.
     */
    @Transactional
    public void markCardAsUsed(String pagarmeCardId) {
        cardRepository.findByPagarmeCardId(pagarmeCardId).ifPresent(card -> {
            card.setLastUsedAt(LocalDateTime.now());
            cardRepository.save(card);
            log.debug("Cartão {} marcado como usado", pagarmeCardId);
        });
    }

    /**
     * Verifica se cliente possui cartões ativos.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveCards(UUID customerId) {
        return cardRepository.countActiveCardsByCustomerId(customerId) > 0;
    }

    /**
     * Busca cartão por ID do Pagar.me.
     */
    @Transactional(readOnly = true)
    public CustomerCard findByPagarmeCardId(String pagarmeCardId) {
        return cardRepository.findByPagarmeCardId(pagarmeCardId)
                .orElseThrow(() -> new RuntimeException("Cartão não encontrado: " + pagarmeCardId));
    }

    // ============================================================================
    // MÉTODOS PRIVADOS
    // ============================================================================

    /**
     * Valida se usuário logado tem acesso ao customer.
     * Apenas o próprio customer ou admin pode gerenciar cartões.
     */
    private void validateCustomerAccess(UUID customerId, Authentication authentication) {
        String username = authentication.getName();
        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

        if (!loggedUser.getId().equals(customerId) && !loggedUser.isAdmin()) {
            throw new RuntimeException("Não autorizado a gerenciar cartões deste cliente");
        }
    }

    /**
     * Mapeia string de bandeira para enum.
     */
    public static CustomerCard.CardBrand mapBrand(String brandStr) {
        if (brandStr == null) return CustomerCard.CardBrand.OTHER;
        
        return switch (brandStr.toUpperCase()) {
            case "VISA" -> CustomerCard.CardBrand.VISA;
            case "MASTERCARD", "MASTER" -> CustomerCard.CardBrand.MASTERCARD;
            case "AMEX", "AMERICAN EXPRESS" -> CustomerCard.CardBrand.AMEX;
            case "ELO" -> CustomerCard.CardBrand.ELO;
            case "HIPERCARD" -> CustomerCard.CardBrand.HIPERCARD;
            case "DINERS", "DINERS CLUB" -> CustomerCard.CardBrand.DINERS;
            case "DISCOVER" -> CustomerCard.CardBrand.DISCOVER;
            case "JCB" -> CustomerCard.CardBrand.JCB;
            default -> CustomerCard.CardBrand.OTHER;
        };
    }
}
