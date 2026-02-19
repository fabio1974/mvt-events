package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.jpa.CustomerPaymentPreference.PreferredPaymentType;
import com.mvt.mvt_events.payment.dto.OrderRequest;
import com.mvt.mvt_events.payment.dto.OrderResponse;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.repository.*;
import com.mvt.mvt_events.specification.DeliverySpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service para Delivery - ENTIDADE CORE DO ZAPI10
 * IMPORTANTE: Todas as operações devem filtrar por ADM (tenant)
 */
@Service
@Transactional
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeliveryNotificationService deliveryNotificationService;

    @Autowired
    private com.mvt.mvt_events.repository.OrganizationRepository organizationRepository;

    @Autowired
    private EmploymentContractRepository employmentContractRepository;

    @Autowired
    private ClientContractRepository clientContractRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private SiteConfigurationService siteConfigurationService;

    @Autowired
    private SpecialZoneService specialZoneService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerPaymentPreferenceService preferenceService;

    @Autowired
    private CustomerCardService cardService;

    @Autowired
    private PagarMeService pagarMeService;

    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private PushNotificationService pushNotificationService;

    // TODO: ADMProfileRepository não mais usado após remoção de CourierADMLink
    // @Autowired
    // private ADMProfileRepository admProfileRepository;

    // TODO: CourierADMLinkRepository removido - agora Courier se relaciona com
    // Organization via EmploymentContract
    // @Autowired
    // private CourierADMLinkRepository courierADMLinkRepository;

    /**
     * Criar nova delivery
     * VALIDA: Cliente existe, usuário existe, parceria (se fornecida)
     * 
     * ROLES PERMITIDAS:
     * - CLIENT: pode criar entregas para si mesmo
     * - ADMIN: pode criar entregas para qualquer cliente
     * 
     * ROLES NÃO PERMITIDAS:
     * - ORGANIZER: não pode criar entregas (apenas gerenciar)
     * - COURIER: não pode criar entregas (apenas executar)
     */
    public Delivery create(Delivery delivery, UUID creatorId, UUID clientId) {
        // Validar usuário que está criando
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Usuário criador não encontrado"));

        // Validar cliente
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // Validação de role: cliente deve ser CLIENT ou CUSTOMER
        if (client.getRole() != User.Role.CLIENT && client.getRole() != User.Role.CUSTOMER) {
            throw new RuntimeException("O destinatário da entrega deve ser um CLIENT ou CUSTOMER (role atual: " + client.getRole() + ")");
        }

        // Validação de permissões do criador
        User.Role creatorRole = creator.getRole();
        
        if (creatorRole == User.Role.CLIENT || creatorRole == User.Role.CUSTOMER) {
            // CLIENT/CUSTOMER só pode criar entregas para si mesmo
            if (!creator.getId().equals(client.getId())) {
                throw new RuntimeException("CLIENT/CUSTOMER só pode criar entregas para si mesmo");
            }
        } else if (creatorRole == User.Role.ADMIN) {
            // ADMIN pode criar entregas para qualquer cliente (sem restrições)
        } else if (creatorRole == User.Role.ORGANIZER) {
            // ORGANIZER não pode criar entregas, apenas gerenciar
            throw new RuntimeException("ORGANIZER não pode criar entregas, apenas gerenciar as existentes");
        } else if (creatorRole == User.Role.COURIER) {
            // COURIER não pode criar entregas
            throw new RuntimeException("COURIER não pode criar entregas");
        }

        // A organização da delivery é determinada pela organização do cliente
        // Não precisamos mais do campo adm - usar client.organization
        delivery.setClient(client);
        delivery.setStatus(Delivery.DeliveryStatus.PENDING);

        // Calcular o frete automaticamente baseado na distância e configuração ativa
        if (delivery.getDistanceKm() != null && delivery.getDistanceKm().compareTo(BigDecimal.ZERO) > 0) {
            SiteConfiguration activeConfig = siteConfigurationService.getActiveConfiguration();
            
            // Selecionar preço por km baseado na preferência de veículo
            BigDecimal pricePerKm = activeConfig.getPricePerKm(); // default: moto
            BigDecimal minimumFee = activeConfig.getMinimumShippingFee(); // default: moto
            if (delivery.getPreferredVehicleType() == Delivery.PreferredVehicleType.CAR) {
                pricePerKm = activeConfig.getCarPricePerKm() != null 
                    ? activeConfig.getCarPricePerKm() : activeConfig.getPricePerKm();
                minimumFee = activeConfig.getCarMinimumShippingFee() != null
                    ? activeConfig.getCarMinimumShippingFee() : activeConfig.getMinimumShippingFee();
            }
            
            BigDecimal calculatedFee = delivery.getDistanceKm().multiply(pricePerKm);
            
            // Aplicar valor mínimo do frete (por tipo de veículo)
            if (calculatedFee.compareTo(minimumFee) < 0) {
                calculatedFee = minimumFee;
            }
            
            // Verificar se o destino está em uma zona especial
            // Se houver zonas sobrepostas, aplica a taxa da zona MAIS PRÓXIMA
            if (delivery.getToLatitude() != null && delivery.getToLongitude() != null) {
                var nearestZone = specialZoneService.findNearestZone(
                    delivery.getToLatitude(), 
                    delivery.getToLongitude()
                );
                
                if (nearestZone.isPresent()) {
                    SpecialZone zone = nearestZone.get();
                    BigDecimal additionalFeePercentage = BigDecimal.ZERO;
                    
                    // Aplica a taxa da zona mais próxima (independente do tipo)
                    if (zone.getZoneType() == SpecialZone.ZoneType.DANGER) {
                        additionalFeePercentage = activeConfig.getDangerFeePercentage();
                    } else if (zone.getZoneType() == SpecialZone.ZoneType.HIGH_INCOME) {
                        additionalFeePercentage = activeConfig.getHighIncomeFeePercentage();
                    }
                    
                    // Aplicar taxa adicional sobre o frete
                    if (additionalFeePercentage.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal additionalFee = calculatedFee
                            .multiply(additionalFeePercentage)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        calculatedFee = calculatedFee.add(additionalFee);
                    }
                }
            }
            
            // Arredondar para 2 casas decimais
            delivery.setShippingFee(calculatedFee.setScale(2, RoundingMode.HALF_UP));
        }

        Delivery savedDelivery = deliveryRepository.save(delivery);

        // Iniciar processo de notificação para motoboys disponíveis
        deliveryNotificationService.notifyAvailableDrivers(savedDelivery);

        return savedDelivery;
    }

    /**
     * Atualiza uma delivery existente
     * Apenas campos editáveis podem ser atualizados
     * Status PENDING permite mais edições
     */
    public Delivery update(Long id, Delivery updatedDelivery, UUID userId) {
        // Buscar delivery existente
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));

        // Validar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Validar permissões
        User.Role userRole = user.getRole();
        
        // CLIENT/CUSTOMER só pode editar suas próprias deliveries PENDING
        if (userRole == User.Role.CLIENT || userRole == User.Role.CUSTOMER) {
            if (!delivery.getClient().getId().equals(userId)) {
                throw new RuntimeException("CLIENT/CUSTOMER só pode editar suas próprias entregas");
            }
            if (delivery.getStatus() != Delivery.DeliveryStatus.PENDING) {
                throw new RuntimeException("Apenas entregas PENDING podem ser editadas");
            }
        }
        // ADMIN pode editar qualquer delivery PENDING
        else if (userRole == User.Role.ADMIN) {
            if (delivery.getStatus() != Delivery.DeliveryStatus.PENDING) {
                throw new RuntimeException("Apenas entregas PENDING podem ser editadas");
            }
        }
        // ORGANIZER e COURIER não podem editar deliveries
        else {
            throw new RuntimeException(userRole + " não pode editar entregas");
        }

        // Atualizar campos permitidos (apenas se PENDING)
        delivery.setFromAddress(updatedDelivery.getFromAddress());
        delivery.setFromLatitude(updatedDelivery.getFromLatitude());
        delivery.setFromLongitude(updatedDelivery.getFromLongitude());
        delivery.setToAddress(updatedDelivery.getToAddress());
        delivery.setToLatitude(updatedDelivery.getToLatitude());
        delivery.setToLongitude(updatedDelivery.getToLongitude());
        delivery.setRecipientName(updatedDelivery.getRecipientName());
        delivery.setRecipientPhone(updatedDelivery.getRecipientPhone());
        delivery.setItemDescription(updatedDelivery.getItemDescription());
        delivery.setTotalAmount(updatedDelivery.getTotalAmount());
        delivery.setShippingFee(updatedDelivery.getShippingFee());
        delivery.setScheduledPickupAt(updatedDelivery.getScheduledPickupAt());

        return deliveryRepository.save(delivery);
    }

    /**
     * Exclui uma delivery
     * 
     * REGRAS:
     * - ADMIN: pode excluir qualquer delivery
     * - ORGANIZER: pode excluir apenas deliveries PENDING ou CANCELLED que ele criou (organizer)
     * - CLIENT: pode excluir apenas suas próprias deliveries PENDING
     * - COURIER: não pode excluir deliveries
     */
    public void delete(Long id, UUID userId, String role) {
        Delivery delivery = deliveryRepository.findByIdWithJoins(id)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));

        // PROTEÇÃO: Não permitir excluir delivery associada a um Payment
        if (paymentRepository.existsByDeliveryIdLong(id)) {
            throw new RuntimeException("Esta delivery não pode ser excluída pois está associada a um pagamento. Cancele ou exclua o pagamento primeiro.");
        }

        if ("ADMIN".equals(role)) {
            // ADMIN pode excluir qualquer delivery
            deliveryRepository.delete(delivery);
        } else if ("ORGANIZER".equals(role)) {
            // ORGANIZER pode excluir apenas deliveries PENDING ou CANCELLED que ele criou
            if (delivery.getOrganizer() == null || !delivery.getOrganizer().getId().equals(userId)) {
                throw new RuntimeException("Você não tem permissão para excluir esta delivery (não é o organizador)");
            }
            if (delivery.getStatus() != Delivery.DeliveryStatus.PENDING && 
                delivery.getStatus() != Delivery.DeliveryStatus.CANCELLED) {
                throw new RuntimeException("Apenas deliveries com status PENDING ou CANCELLED podem ser excluídas");
            }
            deliveryRepository.delete(delivery);
        } else if ("CLIENT".equals(role)) {
            // CLIENT pode excluir apenas suas próprias deliveries PENDING
            if (delivery.getClient() == null || !delivery.getClient().getId().equals(userId)) {
                throw new RuntimeException("Você não tem permissão para excluir esta delivery (não é o cliente)");
            }
            if (delivery.getStatus() != Delivery.DeliveryStatus.PENDING) {
                throw new RuntimeException("Apenas deliveries com status PENDING podem ser excluídas");
            }
            deliveryRepository.delete(delivery);
        } else {
            throw new RuntimeException("Você não tem permissão para excluir deliveries");
        }
    }

    /**
     * Busca delivery por ID com validação de tenant
     */
    public Delivery findById(Long id, Long organizationId) {
        // Buscar com joins para evitar lazy loading
        Delivery delivery = deliveryRepository.findByIdWithJoins(id)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));
        
        // Nota: organizationId NÃO é utilizado para filtrar deliveries
        // organization_id em users é apenas para agrupar motoboys (couriers)
        // Deliveries são filtradas por client_id, courier_id ou organizer_id diretamente
        // O parâmetro organizationId está mantido para compatibilidade com assinaturas antigas,
        // mas não deve ser usado para validação de acesso
        
        return delivery;
    }

    /**
     * Expande status PENDING para incluir WAITING_PAYMENT.
     * Deliveries PIX ficam em WAITING_PAYMENT até confirmação de pagamento,
     * e devem aparecer junto com as PENDING nas listagens.
     */
    private List<Delivery.DeliveryStatus> expandPendingStatus(Delivery.DeliveryStatus status) {
        if (status == Delivery.DeliveryStatus.PENDING) {
            return List.of(Delivery.DeliveryStatus.PENDING, Delivery.DeliveryStatus.WAITING_PAYMENT);
        }
        return List.of(status);
    }

    /**
     * Lista deliveries com filtros e tenant
     */
    public Page<Delivery> findAll(Long organizationId, UUID clientId, UUID courierId, UUID organizerId,
            Delivery.DeliveryStatus status,
            LocalDateTime startDate, LocalDateTime endDate,
            Boolean hasPayment, LocalDateTime completedAfter, LocalDateTime completedBefore,
            Pageable pageable) {
        
        // Caso especial: busca por clientId específico (para role CLIENT)
        if (clientId != null && courierId == null && organizerId == null && organizationId == null &&
                startDate == null && endDate == null && 
                hasPayment == null && completedAfter == null && completedBefore == null) {
            List<Delivery> deliveries;
            
            if (status != null) {
                // Com filtro de status - expandir PENDING para incluir WAITING_PAYMENT (PIX)
                List<Delivery.DeliveryStatus> statuses = expandPendingStatus(status);
                deliveries = deliveryRepository.findByClientIdAndStatusesWithJoins(clientId, statuses);
            } else {
                // Sem filtro de status
                deliveries = deliveryRepository.findByClientIdWithJoins(clientId);
            }
            
            // Converter para Page manualmente
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), deliveries.size());
            List<Delivery> pageContent = deliveries.subList(start, end);
            return new PageImpl<>(pageContent, pageable, deliveries.size());
        }
        
        // Caso especial: busca por organizerId específico (para role ORGANIZER)
        if (organizerId != null && clientId == null && courierId == null && organizationId == null &&
                startDate == null && endDate == null &&
                hasPayment == null && completedAfter == null && completedBefore == null) {
            List<Delivery> deliveries;
            
            if (status != null) {
                // Com filtro de status - expandir PENDING para incluir WAITING_PAYMENT (PIX)
                List<Delivery.DeliveryStatus> statuses = expandPendingStatus(status);
                deliveries = deliveryRepository.findByOrganizerIdAndStatusesWithJoins(organizerId, statuses);
            } else {
                // Sem filtro de status
                deliveries = deliveryRepository.findByOrganizerIdWithJoins(organizerId);
            }
            
            // Converter para Page manualmente
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), deliveries.size());
            List<Delivery> pageContent = deliveries.subList(start, end);
            return new PageImpl<>(pageContent, pageable, deliveries.size());
        }
        
        // Para simplificar e evitar o problema de lazy loading,
        // vamos usar apenas o filtro por organizationId primeiro
        if (clientId == null && courierId == null && organizerId == null && status == null &&
                startDate == null && endDate == null &&
                hasPayment == null && completedAfter == null && completedBefore == null) {
            
            // Caso especial: ADMIN sem filtros - retornar TODAS as deliveries com JOIN FETCH
            if (organizationId == null) {
                // Para ADMIN: usar query com fetch joins para evitar lazy loading
                return deliveryRepository.findAllWithJoins(pageable);
            }
            
            // Caso simples - usar query com fetch joins por organização
            List<Delivery> deliveries = deliveryRepository.findAllWithJoinsByOrganizationId(organizationId);
            // Converter para Page manualmente
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), deliveries.size());
            List<Delivery> pageContent = deliveries.subList(start, end);
            return new PageImpl<>(pageContent, pageable, deliveries.size());
        }

        // Caso complexo - usar specifications (pode ter lazy loading)
        // Expandir PENDING para incluir WAITING_PAYMENT (PIX)
        List<Delivery.DeliveryStatus> expandedStatuses = status != null ? expandPendingStatus(status) : null;
        Specification<Delivery> spec = DeliverySpecification.hasClientOrganizationId(organizationId)
                .and(DeliverySpecification.hasClientId(clientId))
                .and(DeliverySpecification.hasCourierId(courierId))
                .and(DeliverySpecification.hasOrganizerId(organizerId))
                .and(DeliverySpecification.hasStatusIn(expandedStatuses))
                .and(DeliverySpecification.createdBetween(startDate, endDate))
                .and(DeliverySpecification.hasPayment(hasPayment))
                .and(DeliverySpecification.completedBetween(completedAfter, completedBefore));

        return deliveryRepository.findAll(spec, pageable);
    }

    /**
     * Busca deliveries de múltiplas organizações (para COURIERs)
     */
    public Page<Delivery> findAllByOrganizationIds(List<Long> organizationIds, UUID clientId, UUID courierId,
            Delivery.DeliveryStatus status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        // Para evitar lazy loading, usar query com fetch joins
        if (clientId == null && courierId == null && startDate == null && endDate == null) {
            List<Delivery> deliveries;

            if (status == null) {
                // Sem filtro de status - usar query simples
                deliveries = deliveryRepository.findAllWithJoinsByClientContracts(organizationIds);
            } else {
                // Com filtro de status - usar query com status
                deliveries = deliveryRepository.findAllWithJoinsByClientContractsAndStatus(organizationIds, status);
            }

            // Converter para Page manualmente
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), deliveries.size());
            List<Delivery> pageContent = deliveries.subList(start, end);
            return new PageImpl<>(pageContent, pageable, deliveries.size());
        }

        // Caso complexo - usar specifications (pode ter lazy loading)
        Specification<Delivery> spec = DeliverySpecification.hasClientWithContractsInOrganizations(organizationIds)
                .and(DeliverySpecification.hasClientId(clientId))
                .and(DeliverySpecification.hasCourierId(courierId))
                .and(DeliverySpecification.hasStatus(status))
                .and(DeliverySpecification.createdBetween(startDate, endDate));

        return deliveryRepository.findAll(spec, pageable);
    }

    /**
     * Atribui delivery a um courier
     * VALIDA: Courier existe, está ativo, pertence ao ADM
     * 
     * VALIDAÇÃO DE PAGAMENTO:
     * - Para CLIENT (estabelecimento): Pode aceitar SEM aguardar confirmação de pagamento
     *   → Pagamento automático por cartão criado no aceite (se preferência CREDIT_CARD)
     * - Para CUSTOMER (app mobile) + DELIVERY ou RIDE type:
     *   → PIX: pagamento criado no aceite (imediato)
     *   → CREDIT_CARD: pagamento criado ao entrar em trânsito (confirmPickup)
     *   → Split sem ORGANIZER (87% courier, 13% plataforma)
     */
    public Delivery assignToCourier(Long deliveryId, UUID courierId, Long organizationId) {
        Delivery delivery = findById(deliveryId, organizationId);

        if (delivery.getStatus() != Delivery.DeliveryStatus.PENDING 
                && delivery.getStatus() != Delivery.DeliveryStatus.WAITING_PAYMENT) {
            throw new RuntimeException("Esta Delivery já foi aceita por outro motoboy");
        }

        // Buscar o User do courier
        User courierUser = userRepository.findById(courierId)
                .orElseThrow(() -> new RuntimeException("Usuário courier não encontrado"));
        
        // Validar se é COURIER
        if (courierUser.getRole() != User.Role.COURIER) {
            throw new RuntimeException("Usuário não é um courier");
        }

        // Atribuir courier
        delivery.setCourier(courierUser);
        delivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
        delivery.setAcceptedAt(LocalDateTime.now());

        // Setar o veículo ativo do courier no momento do aceite
        vehicleRepository.findActiveVehicleByOwnerId(courierUser.getId())
                .ifPresent(delivery::setVehicle);

        if (delivery.isFromTrustedClient()) {
            // ─── FLUXO CLIENT (estabelecimento): buscar organização comum ───
            Organization commonOrganization = findCommonOrganization(courierUser, delivery.getClient());
            if (commonOrganization == null) {
                throw new RuntimeException("Courier e Client não compartilham uma organização comum através de contratos ativos");
            }

            User organizer = commonOrganization.getOwner();
            if (organizer == null) {
                throw new RuntimeException("Organização não possui um owner definido");
            }

            delivery.setOrganizer(organizer);

            Delivery saved = deliveryRepository.save(delivery);

            // 💳 PAGAMENTO AUTOMÁTICO CLIENT: Se tem preferência CREDIT_CARD
            try {
                createAutomaticCreditCardPayment(saved, delivery.getClient());
            } catch (Exception e) {
                log.warn("⚠️ Falha ao criar pagamento automático por cartão para delivery #{}: {}", 
                    saved.getId(), e.getMessage());
            }

            return deliveryRepository.findByIdWithJoins(saved.getId()).orElse(saved);

        } else {
            // ─── FLUXO CUSTOMER (app mobile): sem organização, pagamento no aceite (PIX) ou trânsito (cartão) ───
            // CUSTOMER não tem organizer (entrega direta, sem estabelecimento)
            delivery.setOrganizer(null);

            // Verificar preferência de pagamento ANTES de salvar
            boolean isCustomerPix = false;
            if (delivery.getDeliveryType() == Delivery.DeliveryType.DELIVERY 
                    || delivery.getDeliveryType() == Delivery.DeliveryType.RIDE) {
                CustomerPaymentPreference pref = preferenceService.getPreference(delivery.getClient().getId());
                if (pref.getPreferredPaymentType() == PreferredPaymentType.PIX) {
                    isCustomerPix = true;
                    // CUSTOMER + PIX → WAITING_PAYMENT (não ACCEPTED)
                    delivery.setStatus(Delivery.DeliveryStatus.WAITING_PAYMENT);
                    log.info("📱 CUSTOMER + PIX na delivery #{} — status → WAITING_PAYMENT", delivery.getId());
                } else {
                    log.info("💳 CUSTOMER com preferência CARTÃO na delivery #{} — cobrança será feita ao entrar em trânsito", delivery.getId());
                }
            }

            Delivery saved = deliveryRepository.save(delivery);

            // 💳 PAGAMENTO CUSTOMER PIX: Criar pagamento PIX no aceite (DELIVERY e RIDE)
            // Cartão de crédito será cobrado quando entrar em trânsito (confirmPickup)
            if (isCustomerPix) {
                try {
                    createPixPaymentForCustomer(saved, delivery.getClient());
                } catch (Exception e) {
                    log.error("❌ Falha ao criar pagamento PIX para CUSTOMER na delivery #{}: {}", 
                        saved.getId(), e.getMessage(), e);
                    // Reverter aceite se pagamento PIX falhar
                    saved.setStatus(Delivery.DeliveryStatus.PENDING);
                    saved.setCourier(null);
                    saved.setAcceptedAt(null);
                    saved.setVehicle(null);
                    deliveryRepository.save(saved);
                    throw new RuntimeException("Não foi possível processar o pagamento PIX: " + e.getMessage());
                }
            }

            return deliveryRepository.findByIdWithJoins(saved.getId()).orElse(saved);
        }
    }

    /**
     * Courier confirma coleta
     * 
     * VALIDAÇÃO DE PAGAMENTO:
     * - Para CLIENT (estabelecimento): Pode iniciar SEM aguardar confirmação de pagamento
     * - Para CUSTOMER (app mobile) + DELIVERY ou RIDE + CREDIT_CARD: cria pagamento ao entrar em trânsito
     * - Para CUSTOMER (app mobile) + DELIVERY ou RIDE + PIX: Pagamento já foi criado no accept
     */
    public Delivery confirmPickup(Long deliveryId, UUID courierId) {
        Delivery delivery = deliveryRepository.findByIdWithJoins(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));

        if (!delivery.getCourier().getId().equals(courierId)) {
            throw new RuntimeException("Delivery não pertence a este courier");
        }

        if (delivery.getStatus() != Delivery.DeliveryStatus.ACCEPTED) {
            throw new RuntimeException("Status inválido para iniciar transporte");
        }

        delivery.setStatus(Delivery.DeliveryStatus.IN_TRANSIT);
        delivery.setPickedUpAt(LocalDateTime.now());
        delivery.setInTransitAt(LocalDateTime.now());

        Delivery saved = deliveryRepository.save(delivery);

        // 💳 PAGAMENTO CUSTOMER CARTÃO: Criar pagamento por cartão ao entrar em trânsito (DELIVERY e RIDE)
        if (!delivery.isFromTrustedClient() 
                && (delivery.getDeliveryType() == Delivery.DeliveryType.DELIVERY 
                    || delivery.getDeliveryType() == Delivery.DeliveryType.RIDE)) {
            CustomerPaymentPreference pref = preferenceService.getPreference(delivery.getClient().getId());
            if (pref.getPreferredPaymentType() == PreferredPaymentType.CREDIT_CARD) {
                try {
                    createCreditCardPaymentForCustomer(saved, delivery.getClient());
                } catch (Exception e) {
                    log.error("❌ Falha ao criar pagamento por cartão para CUSTOMER na delivery #{}: {}", 
                        saved.getId(), e.getMessage(), e);
                    // Reverter para ACCEPTED se pagamento falhar
                    saved.setStatus(Delivery.DeliveryStatus.ACCEPTED);
                    saved.setPickedUpAt(null);
                    saved.setInTransitAt(null);
                    deliveryRepository.save(saved);
                    throw new RuntimeException("Não foi possível processar o pagamento por cartão: " + e.getMessage());
                }
            }
        }
        
        // Recarregar com joins
        return deliveryRepository.findByIdWithJoins(saved.getId())
                .orElse(saved);
    }

    /**
     * Courier inicia transporte
     * @deprecated Usar confirmPickup() que já coloca em IN_TRANSIT
     */
    @Deprecated
    public Delivery startTransit(Long deliveryId, UUID courierId) {
        // Agora confirmPickup já coloca em IN_TRANSIT, este método não é mais necessário
        // Mantido por compatibilidade, mas redireciona para confirmPickup
        return confirmPickup(deliveryId, courierId);
    }

    /**
     * Completa delivery
     * Atualiza métricas do courier
     */
    public Delivery complete(Long deliveryId, UUID courierId) {
        Delivery delivery = deliveryRepository.findByIdWithJoins(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));

        if (!delivery.getCourier().getId().equals(courierId)) {
            throw new RuntimeException("Delivery não pertence a este courier");
        }

        if (delivery.getStatus() != Delivery.DeliveryStatus.IN_TRANSIT) {
            throw new RuntimeException("Status inválido para completar");
        }

        delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);
        delivery.setCompletedAt(LocalDateTime.now());

        Delivery saved = deliveryRepository.save(delivery);
        
        // Recarregar com joins
        return deliveryRepository.findByIdWithJoins(saved.getId())
                .orElse(saved);
    }

    /**
     * Cancela delivery
     */
    public Delivery cancel(Long deliveryId, Long organizationId, String reason) {
        Delivery delivery = findById(deliveryId, organizationId);

        if (delivery.getStatus() == Delivery.DeliveryStatus.COMPLETED) {
            throw new RuntimeException("Não é possível cancelar delivery completada");
        }

        if (delivery.getStatus() == Delivery.DeliveryStatus.CANCELLED) {
            throw new RuntimeException("Delivery já está cancelada");
        }

        delivery.setStatus(Delivery.DeliveryStatus.CANCELLED);
        delivery.setCancelledAt(LocalDateTime.now());
        delivery.setCancellationReason(reason);

        // Se tinha courier atribuído, remover courier e organization
        if (delivery.getCourier() != null) {
            // Remover courier e organizer
            delivery.setCourier(null);
            delivery.setOrganizer(null);
        }

        return deliveryRepository.save(delivery);
    }

    /**
     * Atualiza o status de uma delivery com validações e atualização de timestamps
     * Quando cancelada, remove o courier e volta para PENDING
     */
    public Delivery updateStatus(Long deliveryId, Delivery.DeliveryStatus newStatus, String reason, Long organizationId) {
        Delivery delivery = findById(deliveryId, organizationId);
        Delivery.DeliveryStatus currentStatus = delivery.getStatus();

        // Validar se a transição é válida
        validateStatusTransition(currentStatus, newStatus);

        // Atualizar status e timestamps correspondentes
        delivery.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();

        switch (newStatus) {
            case PENDING:
                // Limpar dados quando volta para pending
                delivery.setCourier(null);
                delivery.setAcceptedAt(null);
                delivery.setPickedUpAt(null);
                delivery.setInTransitAt(null);
                delivery.setCompletedAt(null);
                delivery.setCancelledAt(null);
                delivery.setCancellationReason(null);
                break;

            case ACCEPTED:
                delivery.setAcceptedAt(now);
                // Limpar timestamps posteriores
                delivery.setPickedUpAt(null);
                delivery.setInTransitAt(null);
                delivery.setCompletedAt(null);
                break;

            case IN_TRANSIT:
                if (delivery.getAcceptedAt() == null) {
                    delivery.setAcceptedAt(now);
                }
                // Coleta e início de transporte são simultâneos agora
                if (delivery.getPickedUpAt() == null) {
                    delivery.setPickedUpAt(now);
                }
                delivery.setInTransitAt(now);
                // Limpar timestamp posterior
                delivery.setCompletedAt(null);
                break;

            case COMPLETED:
                if (delivery.getAcceptedAt() == null) {
                    delivery.setAcceptedAt(now);
                }
                if (delivery.getPickedUpAt() == null) {
                    delivery.setPickedUpAt(now);
                }
                if (delivery.getInTransitAt() == null) {
                    delivery.setInTransitAt(now);
                }
                delivery.setCompletedAt(now);
                break;

            case CANCELLED:
                delivery.setCancelledAt(now);
                delivery.setCancellationReason(reason);
                
                // IMPORTANTE: Remover courier e voltar para PENDING
                if (delivery.getCourier() != null) {
                    // Remover courier da delivery
                    delivery.setCourier(null);
                }
                
                // Voltar para PENDING
                delivery.setStatus(Delivery.DeliveryStatus.PENDING);
                delivery.setAcceptedAt(null);
                delivery.setPickedUpAt(null);
                delivery.setInTransitAt(null);
                delivery.setCompletedAt(null);
                break;
        }

        return deliveryRepository.save(delivery);
    }

    /**
     * Valida se a transição de status é permitida
     */
    private void validateStatusTransition(Delivery.DeliveryStatus current, Delivery.DeliveryStatus target) {
        // CANCELLED pode ser acionado de qualquer status (exceto COMPLETED)
        if (target == Delivery.DeliveryStatus.CANCELLED) {
            if (current == Delivery.DeliveryStatus.COMPLETED) {
                throw new RuntimeException("Não é possível cancelar delivery completada");
            }
            return;
        }

        // PENDING só pode vir de CANCELLED ou ser o estado inicial
        if (target == Delivery.DeliveryStatus.PENDING) {
            if (current != Delivery.DeliveryStatus.CANCELLED && current != Delivery.DeliveryStatus.PENDING) {
                throw new RuntimeException("Não é possível voltar para PENDING exceto após cancelamento");
            }
            return;
        }

        // Validar fluxo normal: PENDING -> WAITING_PAYMENT/ACCEPTED -> ACCEPTED -> IN_TRANSIT -> COMPLETED
        switch (current) {
            case PENDING:
                if (target != Delivery.DeliveryStatus.ACCEPTED 
                        && target != Delivery.DeliveryStatus.WAITING_PAYMENT 
                        && target != Delivery.DeliveryStatus.CANCELLED) {
                    throw new RuntimeException("De PENDING só pode ir para ACCEPTED, WAITING_PAYMENT ou CANCELLED");
                }
                break;
            case WAITING_PAYMENT:
                if (target != Delivery.DeliveryStatus.ACCEPTED 
                        && target != Delivery.DeliveryStatus.PENDING 
                        && target != Delivery.DeliveryStatus.CANCELLED) {
                    throw new RuntimeException("De WAITING_PAYMENT só pode ir para ACCEPTED, PENDING ou CANCELLED");
                }
                break;
            case ACCEPTED:
                if (target != Delivery.DeliveryStatus.IN_TRANSIT) {
                    throw new RuntimeException("De ACCEPTED só pode ir para IN_TRANSIT (coletar e iniciar transporte)");
                }
                break;
            case IN_TRANSIT:
                if (target != Delivery.DeliveryStatus.COMPLETED) {
                    throw new RuntimeException("De IN_TRANSIT só pode ir para COMPLETED");
                }
                break;
            case COMPLETED:
                throw new RuntimeException("Delivery completada não pode mudar de status");
            case CANCELLED:
                throw new RuntimeException("Delivery cancelada não pode mudar de status");
        }
    }

    /**
     * Busca deliveries pendentes de atribuição
     */
    public List<Delivery> findPendingAssignment(Long organizationId) {
        return deliveryRepository.findPendingAssignmentByOrganizationId(organizationId);
    }

    /**
     * Busca deliveries ativas de um courier
     * Inicializa relacionamentos lazy para evitar LazyInitializationException
     */
    @Transactional(readOnly = true)
    public List<Delivery> findActiveByCourier(UUID courierId) {
        List<Delivery> deliveries = deliveryRepository.findActiveByCourierId(courierId);
        
        // Inicializar relacionamentos lazy-loaded para evitar LazyInitializationException
        for (Delivery delivery : deliveries) {
            org.hibernate.Hibernate.initialize(delivery.getClient());
            org.hibernate.Hibernate.initialize(delivery.getCourier());
            org.hibernate.Hibernate.initialize(delivery.getOrganizer());
        }
        
        return deliveries;
    }

    /**
     * Busca deliveries concluídas de um courier
     * Ordenadas por completedAt DESC (mais recentes primeiro)
     * 
     * @param courierId ID do courier
     * @param unpaidOnly Se true, retorna apenas deliveries sem nenhum pagamento PAID
     */
    @Transactional(readOnly = true)
    public List<Delivery> findCompletedByCourier(UUID courierId, boolean unpaidOnly) {
        List<Delivery> deliveries;
        
        if (unpaidOnly) {
            deliveries = deliveryRepository.findCompletedUnpaidByCourierId(courierId);
        } else {
            deliveries = deliveryRepository.findCompletedByCourierId(courierId);
        }
        
        // Inicializar relacionamentos lazy-loaded para evitar LazyInitializationException
        for (Delivery delivery : deliveries) {
            org.hibernate.Hibernate.initialize(delivery.getClient());
            org.hibernate.Hibernate.initialize(delivery.getCourier());
            org.hibernate.Hibernate.initialize(delivery.getOrganizer());
        }
        
        return deliveries;
    }

    /**
     * Busca deliveries concluídas de um courier (todas, sem filtro de pagamento)
     * Ordenadas por completedAt DESC (mais recentes primeiro)
     */
    @Transactional(readOnly = true)
    public List<Delivery> findCompletedByCourier(UUID courierId) {
        return findCompletedByCourier(courierId, false);
    }

    /**
     * Lista deliveries PENDING e sem courier de clientes CUSTOMER,
     * aplicando filtro de proximidade (<= radiusKm) em relação ao pickup OU destino.
     * NÃO exige contratos - qualquer delivery de cliente CUSTOMER é elegível.
     */
    @Transactional(readOnly = true)
    public List<Delivery> findPendingNearbyInPrimaryOrgs(UUID courierId, double radiusKm) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
        log.info("🔍 [COURIER PENDINGS] Buscando entregas para courier: {}, raio: {}km", courierId, radiusKm);
        
        // Verificar se o courier tem entregas ativas
        List<Delivery> activeDeliveries = deliveryRepository.findActiveByCourierId(courierId);
        if (!activeDeliveries.isEmpty()) {
            log.info("⚠️ [COURIER PENDINGS] Courier {} possui {} entrega(s) ativa(s) - não retornando pendentes",
                    courierId, activeDeliveries.size());
            return java.util.Collections.emptyList();
        }
        
        // Carregar courier para obter coordenadas GPS
        User courier = userRepository.findById(courierId)
                .orElseThrow(() -> new RuntimeException("Courier não encontrado: " + courierId));

        if (courier.getRole() != User.Role.COURIER) {
            throw new RuntimeException("Apenas COURIER pode listar entregas específicas");
        }

        Double courierLat = courier.getGpsLatitude();
        Double courierLng = courier.getGpsLongitude();
        log.info("📍 [COURIER PENDINGS] Courier location: lat={}, lng={}", courierLat, courierLng);
        
        if (courierLat == null || courierLng == null) {
            log.warn("⚠️ [COURIER PENDINGS] Courier sem localização GPS");
            return java.util.Collections.emptyList();
        }

        // Buscar TODAS as deliveries pendentes de clientes CUSTOMER (sem exigir contratos)
        List<Delivery> candidates = deliveryRepository.findPendingForCustomerClients();
        log.info("📦 [COURIER PENDINGS] Candidatas (clientes CUSTOMER): {}", candidates.size());

        // Log detalhado de cada candidata
        for (Delivery d : candidates) {
            log.info("   → Delivery #{}: from=({}, {}), to=({}, {}), status={}, courier={}", 
                d.getId(), 
                d.getFromLatitude(), d.getFromLongitude(),
                d.getToLatitude(), d.getToLongitude(),
                d.getStatus(),
                d.getCourier() != null ? d.getCourier().getId() : "null");
        }

        // Aplicar filtro de proximidade: pickup OU destino dentro do raio
        List<Delivery> result = candidates.stream()
                .filter(d -> isWithinRadius(courierLat, courierLng, d, radiusKm, log))
                .toList();
        
        log.info("✅ [COURIER PENDINGS] Resultado final: {} deliveries", result.size());
        
        // IMPORTANTE: Inicializar relacionamentos lazy-loaded para evitar LazyInitializationException
        // Força a inicialização dentro da transação @Transactional
        for (Delivery delivery : result) {
            org.hibernate.Hibernate.initialize(delivery.getClient());
            org.hibernate.Hibernate.initialize(delivery.getCourier());
            org.hibernate.Hibernate.initialize(delivery.getOrganizer());
        }
        
        return result;
    }

    private boolean isWithinRadius(Double courierLat, Double courierLng, Delivery d, double radiusKm, org.slf4j.Logger log) {
        // Pickup
        if (d.getFromLatitude() != null && d.getFromLongitude() != null) {
            double dist = calculateDistance(courierLat, courierLng, d.getFromLatitude(), d.getFromLongitude());
            log.info("   📏 Delivery #{} pickup distance: {:.2f}km (limit: {}km) -> {}", 
                d.getId(), dist, radiusKm, dist <= radiusKm ? "✅ PASS" : "❌ FAIL");
            if (dist <= radiusKm) return true;
        }
        // Destino
        if (d.getToLatitude() != null && d.getToLongitude() != null) {
            double dist = calculateDistance(courierLat, courierLng, d.getToLatitude(), d.getToLongitude());
            log.info("   📏 Delivery #{} destination distance: {:.2f}km (limit: {}km) -> {}", 
                d.getId(), dist, radiusKm, dist <= radiusKm ? "✅ PASS" : "❌ FAIL");
            if (dist <= radiusKm) return true;
        }
        return false;
    }

    /**
     * Estatísticas de deliveries por status
     */
    public Long countByStatus(Long organizationId, Delivery.DeliveryStatus status) {
        return deliveryRepository.countByOrganizationIdAndStatus(organizationId, status.name());
    }

    /**
     * Calcula distância entre dois pontos (Haversine formula)
     * 
     * @return distância em km
     */
    private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int R = 6371; // Raio da Terra em km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Encontra a organização comum entre courier e client
     * O courier deve ter EmploymentContract ativo com a organização
     * O client deve ter ClientContract ativo com a mesma organização
     * 
     * Regras de priorização:
     * 1. Se houver múltiplas organizações em comum, prioriza a que tem isPrimary = true no ClientContract
     * 2. Se nenhuma tiver isPrimary = true, retorna a primeira encontrada
     * 
     * @param courier O usuário courier
     * @param client O usuário client
     * @return A organização comum ou null se não houver
     */
    private Organization findCommonOrganization(User courier, User client) {
        // Buscar organizações onde o courier trabalha (EmploymentContract ativo)
        List<EmploymentContract> courierContracts = employmentContractRepository
                .findActiveByCourierId(courier.getId());
        
        // Buscar organizações onde o client tem contrato (ClientContract ativo)
        List<ClientContract> clientContracts = clientContractRepository
                .findActiveByClientId(client.getId());
        
        // Lista de organizações em comum
        List<Organization> commonOrganizations = new java.util.ArrayList<>();
        Organization primaryOrganization = null;
        
        // Encontrar organizações em comum
        for (EmploymentContract ec : courierContracts) {
            for (ClientContract cc : clientContracts) {
                if (ec.getOrganization().getId().equals(cc.getOrganization().getId())) {
                    Organization org = ec.getOrganization();
                    commonOrganizations.add(org);
                    
                    // Se o contrato do cliente for primário, salvar como prioritário
                    if (cc.isPrimary()) {
                        primaryOrganization = org;
                    }
                }
            }
        }
        
        // Retornar de acordo com a prioridade
        if (primaryOrganization != null) {
            return primaryOrganization; // Prioridade 1: isPrimary = true
        } else if (!commonOrganizations.isEmpty()) {
            return commonOrganizations.get(0); // Prioridade 2: primeira encontrada
        }
        
        return null; // Não há organização em comum
    }

    /**
     * Cria pagamento automático por cartão de crédito quando CLIENT com preferência CREDIT_CARD cria delivery.
     * 
     * FLUXO:
     * 1. Verifica se CLIENT tem preferência CREDIT_CARD
     * 2. Busca cartão padrão
     * 3. Encontra courier e organizer (se houver) da delivery
     * 4. Cria order no Pagar.me com split (87% courier, 5% organizer, 8% plataforma)
     * 5. Salva Payment no banco
     * 6. Marca paymentCompleted=true e paymentCaptured=true na delivery
     * 
     * @param delivery Delivery recém-criada
     * @param client CLIENT que criou a delivery
     */
    private void createAutomaticCreditCardPayment(Delivery delivery, User client) {
        log.info("💳 Verificando criação automática de pagamento por cartão para delivery #{}", delivery.getId());
        
        // Forçar carregamento completo do client do banco para garantir que documentNumber esteja disponível
        User fullClient = userRepository.findById(client.getId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        
        // 1. Verificar preferência de pagamento
        CustomerPaymentPreference preference = preferenceService.getPreference(fullClient.getId());
        if (preference.getPreferredPaymentType() != PreferredPaymentType.CREDIT_CARD) {
            log.info("   ├─ Cliente prefere PIX, não criar pagamento automático");
            return;
        }
        
        // 2. Buscar cartão padrão
        CustomerCard card;
        try {
            card = cardService.getDefaultCard(fullClient.getId());
        } catch (Exception e) {
            log.warn("   ├─ ⚠️ Cliente não tem cartão padrão cadastrado: {}", e.getMessage());
            return;
        }
        
        if (!card.getIsActive() || card.isExpired()) {
            log.warn("   ├─ ⚠️ Cartão padrão inativo ou expirado");
            return;
        }
        
        log.info("   ├─ Cartão encontrado: {} **** {}", card.getBrand(), card.getLastFourDigits());
        
        // 3. Buscar courier e organizer (já definidos após assignToCourier)
        User courier = delivery.getCourier();
        User organizer = delivery.getOrganizer();
        
        String courierRecipientId = courier.getPagarmeRecipientId();
        if (courierRecipientId == null || courierRecipientId.isBlank()) {
            log.warn("   ├─ ⚠️ Courier não tem recipientId configurado no Pagar.me");
            return;
        }
        
        String organizerRecipientId = null;
        if (organizer != null) {
            organizerRecipientId = organizer.getPagarmeRecipientId();
        }
        
        // 4. Preparar dados do endereço de cobrança
        OrderRequest.BillingAddressRequest billingAddress = OrderRequest.BillingAddressRequest.builder()
                .line1(delivery.getFromAddress() != null ? delivery.getFromAddress() : "Endereço não informado")
                .zipCode("00000000")
                .city("São Paulo")
                .state("SP")
                .country("BR")
                .build();
        
        // 5. Verificar se já existe pagamento PENDING para esta delivery
        if (paymentRepository.existsPendingPaymentForDelivery(delivery.getId())) {
            log.warn("   ├─ ❌ Já existe um pagamento PENDING para esta entrega (ID: {})", delivery.getId());
            log.warn("   └─ Abortando criação de novo pagamento para evitar duplicação");
            throw new IllegalStateException(
                String.format("Já existe um pagamento pendente para a entrega #%d. " +
                             "Aguarde a conclusão ou cancele o pagamento anterior.", delivery.getId())
            );
        }
        
        // 6. Criar order no Pagar.me com split
        Payment payment = new Payment();
        payment.setCurrency(Currency.BRL);
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        payment.setProvider(PaymentProvider.PAGARME);
        payment.setPayer(fullClient);
        payment.setStatus(PaymentStatus.PENDING);
        payment.addDelivery(delivery);
        
        try {
            log.info("   ├─ Criando order no Pagar.me com split...");

            // Buscar recipientId da plataforma
            SiteConfiguration config = siteConfigurationService.getActiveConfiguration();
            String platformRecipientId = config.getPagarmeRecipientId();
            
            OrderResponse orderResponse = pagarMeService.createOrderWithCreditCardSplit(
                    delivery.getShippingFee(),
                    "Entrega #" + delivery.getId(),
                    card.getPagarmeCardId(),
                    fullClient.getName() != null ? fullClient.getName() : fullClient.getUsername(),
                    fullClient.getUsername(), // Usando username como email
                    fullClient.getDocumentNumber() != null ? fullClient.getDocumentNumber() : "00000000000",
                    billingAddress,
                    courierRecipientId,
                    organizerRecipientId,
                    "ZAPI10",
                    platformRecipientId
            );
            
            log.info("   ├─ ✅ Order criada: {}", orderResponse.getId());
            
            // Atualizar Payment com dados de sucesso
            payment.setProviderPaymentId(orderResponse.getId());
            payment.setAmount(delivery.getShippingFee());
            payment.setPaymentDate(java.time.LocalDateTime.now());
            
            // CRÍTICO: Capturar request/response do OrderResponse
            String req = orderResponse.getRequestPayload();
            String resp = orderResponse.getResponsePayload();
            
            log.info("   ├─ 🔍 Request payload: {} caracteres", req != null ? req.length() : "NULL");
            log.info("   ├─ 🔍 Response payload: {} caracteres", resp != null ? resp.length() : "NULL");
            
            // Garantir que SEMPRE salvamos request/response
            if (req != null && !req.isEmpty()) {
                payment.setRequest(req);
                log.info("   ├─ ✅ Request setado no Payment");
            } else {
                log.error("   ├─ ❌ Request está NULL ou vazio! Isso não deveria acontecer.");
            }
            
            if (resp != null && !resp.isEmpty()) {
                payment.setResponse(resp);
                log.info("   ├─ ✅ Response setado no Payment");
            } else {
                log.error("   ├─ ❌ Response está NULL ou vazio! Isso não deveria acontecer.");
            }
            
            // Adicionar notes com informações do pagamento
            String notes = String.format("Pagamento %s - Order ID: %s - Cartão: %s****%s",
                    orderResponse.getStatus(),
                    orderResponse.getId(),
                    card.getBrand() != null ? card.getBrand() : "?",
                    card.getLastFourDigits());
            payment.setNotes(notes);
            log.info("   ├─ 📝 Notes setado: {}", notes);
            
            // Determinar status real do pagamento (verificando order + charges + transactions)
            PaymentStatus finalStatus = determinePaymentStatus(orderResponse);
            payment.setStatus(finalStatus);
            
            if (finalStatus == PaymentStatus.FAILED) {
                log.warn("   ├─ ⚠️ Pagamento FAILED - Order Status: {}", orderResponse.getStatus());
            } else if (finalStatus == PaymentStatus.PAID) {
                log.info("   ├─ ✅ Pagamento PAID imediatamente");
            } else {
                log.info("   ├─ ⏳ Pagamento PENDING");
            }
            
            Payment savedPayment = paymentRepository.save(payment);
            log.info("   ├─ ✅ Payment #{} salvo no banco (status: {})", savedPayment.getId(), savedPayment.getStatus());
            
            // Enviar notificação push se o pagamento falhou
            if (finalStatus == PaymentStatus.FAILED) {
                try {
                    String failureMessage = paymentService.extractPaymentFailureMessage(orderResponse);
                    String notificationBody = String.format("Pagamento de R$ %.2f não foi aprovado. %s Por favor, escolha outro método de pagamento.", 
                        delivery.getShippingFee(), failureMessage);
                    
                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("type", "payment_failed");
                    notificationData.put("deliveryId", delivery.getId());
                    notificationData.put("paymentId", savedPayment.getId());
                    notificationData.put("amount", delivery.getShippingFee().toString());
                    notificationData.put("failureReason", failureMessage);
                    
                    boolean sent = pushNotificationService.sendNotificationToUser(
                        fullClient.getId(),
                        "❌ Pagamento não aprovado",
                        notificationBody,
                        notificationData
                    );
                    
                    if (sent) {
                        log.info("   ├─ ✅ Notificação de falha enviada ao cliente #{}", fullClient.getId());
                    } else {
                        log.warn("   ├─ ⚠️ Não foi possível enviar notificação - cliente #{} sem token push ativo", fullClient.getId());
                    }
                } catch (Exception e) {
                    log.error("   ├─ ❌ Erro ao enviar notificação de falha: {}", e.getMessage());
                }
            }
            
            // 7. Marcar delivery apenas se o pagamento não falhou
            if (finalStatus != PaymentStatus.FAILED) {
                delivery.setPaymentCompleted(false);
                delivery.setPaymentCaptured(false);
                deliveryRepository.save(delivery);
                log.info("   └─ ✅ Pagamento automático criado com sucesso para delivery #{}", delivery.getId());
            } else {
                log.error("   └─ ❌ Pagamento FAILED - Delivery não marcada como paga");
            }
            
        } catch (com.mvt.mvt_events.payment.exception.PaymentProcessingException e) {
            log.error("   ├─ ❌ Erro ao criar pagamento automático: {}", e.getMessage(), e);
            
            // Salvar Payment mesmo em caso de falha, com os dados capturados
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRequest(e.getRequestPayload());
            payment.setResponse(e.getResponsePayload());
            payment.setAmount(delivery.getShippingFee());
            payment.setNotes("Erro: " + e.getMessage() + " | Code: " + e.getErrorCode());
            
            Payment savedPayment = paymentRepository.save(payment);
            log.info("   ├─ 💾 Payment #{} salvo com status FAILED para auditoria", savedPayment.getId());
            
            // Enviar notificação push de falha
            try {
                String notificationBody = String.format("Pagamento de R$ %.2f não foi aprovado. %s Por favor, escolha outro método de pagamento.", 
                    delivery.getShippingFee(), e.getMessage());
                
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("type", "payment_failed");
                notificationData.put("deliveryId", delivery.getId());
                notificationData.put("paymentId", savedPayment.getId());
                notificationData.put("amount", delivery.getShippingFee().toString());
                notificationData.put("failureReason", e.getMessage());
                
                boolean sent = pushNotificationService.sendNotificationToUser(
                    fullClient.getId(),
                    "❌ Pagamento não aprovado",
                    notificationBody,
                    notificationData
                );
                
                if (sent) {
                    log.info("   ├─ ✅ Notificação de falha enviada ao cliente #{}", fullClient.getId());
                } else {
                    log.warn("   ├─ ⚠️ Não foi possível enviar notificação - cliente #{} sem token push ativo", fullClient.getId());
                }
            } catch (Exception notifError) {
                log.error("   ├─ ❌ Erro ao enviar notificação de falha: {}", notifError.getMessage());
            }
            
            log.error("   └─ ❌ Falha ao criar pagamento automático: {}", e.getMessage());
            
            throw new RuntimeException("Falha ao criar pagamento automático: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("   ├─ ❌ Erro inesperado ao criar pagamento automático: {}", e.getMessage(), e);
            
            // Salvar Payment mesmo em caso de erro inesperado
            payment.setStatus(PaymentStatus.FAILED);
            payment.setAmount(delivery.getShippingFee());
            payment.setNotes("Erro inesperado: " + e.getMessage());
            
            Payment savedPayment = paymentRepository.save(payment);
            log.info("   ├─ 💾 Payment #{} salvo com status FAILED para auditoria", savedPayment.getId());
            
            // Enviar notificação push de falha
            try {
                String notificationBody = String.format("Pagamento de R$ %.2f não foi aprovado. Erro: %s Por favor, escolha outro método de pagamento.", 
                    delivery.getShippingFee(), e.getMessage());
                
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("type", "payment_failed");
                notificationData.put("deliveryId", delivery.getId());
                notificationData.put("paymentId", savedPayment.getId());
                notificationData.put("amount", delivery.getShippingFee().toString());
                notificationData.put("failureReason", e.getMessage());
                
                boolean sent = pushNotificationService.sendNotificationToUser(
                    fullClient.getId(),
                    "❌ Pagamento não aprovado",
                    notificationBody,
                    notificationData
                );
                
                if (sent) {
                    log.info("   ├─ ✅ Notificação de falha enviada ao cliente #{}", fullClient.getId());
                } else {
                    log.warn("   ├─ ⚠️ Não foi possível enviar notificação - cliente #{} sem token push ativo", fullClient.getId());
                }
            } catch (Exception notifError) {
                log.error("   ├─ ❌ Erro ao enviar notificação de falha: {}", notifError.getMessage());
            }
            
            log.error("   └─ ❌ Falha ao criar pagamento automático: {}", e.getMessage());
            
            throw new RuntimeException("Falha ao criar pagamento automático: " + e.getMessage(), e);
        }
    }

    /**
     * Cria pagamento PIX para CUSTOMER no momento do aceite da delivery.
     * 
     * Chamado durante assignToCourier quando CUSTOMER tem preferência PIX.
     * Split SEM ORGANIZER: 87% courier, 13% plataforma.
     * 
     * @param delivery Delivery recém-aceita
     * @param customer CUSTOMER que criou a delivery
     */
    private void createPixPaymentForCustomer(Delivery delivery, User customer) {
        log.info("💳 Criando pagamento PIX para CUSTOMER na delivery #{} (5 min expiração)", delivery.getId());

        // Buscar recipientId do courier
        User courier = delivery.getCourier();
        String courierRecipientId = courier.getPagarmeRecipientId();
        if (courierRecipientId == null || courierRecipientId.isBlank()) {
            throw new RuntimeException("Courier não tem recipientId configurado no Pagar.me");
        }

        // CUSTOMER não tem organizer → split sem organizer (87% courier, 13% plataforma)
        log.info("   ├─ Criando order PIX no Pagar.me (sem organizer, 300s expiração)...");

        // Buscar recipientId da plataforma
        SiteConfiguration config = siteConfigurationService.getActiveConfiguration();
        String platformRecipientId = config.getPagarmeRecipientId();

        OrderResponse orderResponse;
        try {
            // Usar CPF real do customer (documentNumber)
            String customerDocument = customer.getDocumentClean();
            if (customerDocument == null || customerDocument.isBlank()) {
                log.warn("   ├─ ⚠️ Customer sem CPF cadastrado, usando placeholder");
                customerDocument = "00000000000";
            }

            orderResponse = pagarMeService.createOrderWithSplit(
                    delivery.getShippingFee(),
                    "Entrega #" + delivery.getId(),
                    customer.getName() != null ? customer.getName() : customer.getUsername(),
                    customer.getUsername(),
                    customerDocument,
                    courierRecipientId,
                    null, // organizerRecipientId — CUSTOMER não tem organizer
                    platformRecipientId,
                    300 // 5 minutos para CUSTOMER PIX
            );
        } catch (Exception e) {
            log.error("   ├─ ❌ PIX recusado pelo gateway: {}", e.getMessage());

            // Salvar Payment com status FAILED em transação independente (não sofre rollback)
            paymentService.saveFailedPayment(
                    delivery.getShippingFee(),
                    PaymentMethod.PIX,
                    customer,
                    delivery,
                    "PIX recusado: " + e.getMessage()
            );

            throw e;
        }

        log.info("   ├─ ✅ Order PIX criada: {}", orderResponse.getId());

        // Criar Payment no banco
        Payment payment = new Payment();
        payment.setProviderPaymentId(orderResponse.getId());
        payment.setAmount(delivery.getShippingFee());
        payment.setCurrency(Currency.BRL);
        payment.setPaymentMethod(PaymentMethod.PIX);
        payment.setProvider(PaymentProvider.PAGARME);
        payment.setPayer(customer);
        payment.setStatus(PaymentStatus.PENDING);
        payment.addDelivery(delivery);

        // Extrair QR Code e expiresAt da resposta Pagar.me
        if (orderResponse.getCharges() != null && !orderResponse.getCharges().isEmpty()) {
            OrderResponse.Charge charge = orderResponse.getCharges().get(0);
            if (charge.getLastTransaction() != null) {
                OrderResponse.LastTransaction tx = charge.getLastTransaction();
                
                // QR Code PIX
                if (tx.getQrCode() != null) {
                    payment.setPixQrCode(tx.getQrCode());
                    log.info("   ├─ ✅ PIX QR Code extraído ({} chars)", tx.getQrCode().length());
                }
                if (tx.getQrCodeUrl() != null) {
                    payment.setPixQrCodeUrl(tx.getQrCodeUrl());
                    log.info("   ├─ ✅ PIX QR Code URL extraída");
                }
                
                // Expiração do QR Code
                if (tx.getExpiresAt() != null) {
                    try {
                        java.time.OffsetDateTime offsetDateTime = java.time.OffsetDateTime.parse(tx.getExpiresAt());
                        LocalDateTime expiresAt = offsetDateTime
                                .atZoneSameInstant(java.time.ZoneId.systemDefault())
                                .toLocalDateTime();
                        payment.setExpiresAt(expiresAt);
                        log.info("   ├─ ⏰ Expiração PIX: {} (UTC: {}, Timezone: {})", 
                                expiresAt, tx.getExpiresAt(), java.time.ZoneId.systemDefault());
                    } catch (Exception e) {
                        log.warn("   ├─ ⚠️ Falha ao parsear expiresAt: {} — usando fallback now+300s", tx.getExpiresAt());
                        payment.setExpiresAt(LocalDateTime.now().plusSeconds(300));
                    }
                } else {
                    log.warn("   ├─ ⚠️ expiresAt ausente na resposta Pagar.me — usando fallback now+300s");
                    payment.setExpiresAt(LocalDateTime.now().plusSeconds(300));
                }
            }
        }

        // Garantir que expiresAt NUNCA é null (spec: expiresAt NEVER null)
        if (payment.getExpiresAt() == null) {
            log.warn("   ├─ ⚠️ expiresAt ainda null após extração — usando fallback now+300s");
            payment.setExpiresAt(LocalDateTime.now().plusSeconds(300));
        }

        // CRÍTICO: Capturar request/response do OrderResponse
        String req = orderResponse.getRequestPayload();
        String resp = orderResponse.getResponsePayload();
        
        log.info("   ├─ 🔍 Request payload: {} caracteres", req != null ? req.length() : "NULL");
        log.info("   ├─ 🔍 Response payload: {} caracteres", resp != null ? resp.length() : "NULL");
        
        if (req != null && !req.isEmpty()) {
            payment.setRequest(req);
        }
        if (resp != null && !resp.isEmpty()) {
            payment.setResponse(resp);
        }
        
        // Adicionar notes com informações do pagamento PIX
        String notes = String.format("Pagamento PIX CUSTOMER (5min) %s - Order ID: %s - Delivery #%d",
                orderResponse.getStatus(),
                orderResponse.getId(),
                delivery.getId());
        payment.setNotes(notes);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("   ├─ ✅ Payment #{} salvo (QR: {}, URL: {}, ExpiresAt: {})", 
            savedPayment.getId(),
            savedPayment.getPixQrCode() != null ? "✅" : "❌",
            savedPayment.getPixQrCodeUrl() != null ? "✅" : "❌",
            savedPayment.getExpiresAt() != null ? savedPayment.getExpiresAt().toString() : "❌");

        // PIX é assíncrono: SEMPRE começa PENDING, status atualizado via webhook (order.paid)
        // NÃO usar determinePaymentStatus aqui — PIX retorna success=false e waiting_payment na criação,
        // o que é comportamento normal (customer ainda não pagou o QR Code)
        log.info("   ├─ ⏳ PIX criado com status PENDING (aguardando customer pagar QR Code)");

        // Marcar delivery (aguarda webhook para confirmar pagamento)
        delivery.setPaymentCompleted(false);
        delivery.setPaymentCaptured(false);
        deliveryRepository.save(delivery);

        log.info("   └─ ✅ Pagamento PIX CUSTOMER criado para delivery #{} — Status: WAITING_PAYMENT (5 min)", delivery.getId());
    }

    /**
     * Cria pagamento por CARTÃO DE CRÉDITO para CUSTOMER ao entrar em trânsito.
     * 
     * Chamado durante confirmPickup quando CUSTOMER tem preferência CREDIT_CARD.
     * Split SEM ORGANIZER: 87% courier, 13% plataforma.
     * 
     * @param delivery Delivery entrando em trânsito
     * @param customer CUSTOMER que criou a delivery
     */
    private void createCreditCardPaymentForCustomer(Delivery delivery, User customer) {
        log.info("💳 Criando pagamento por CARTÃO para CUSTOMER na delivery #{}", delivery.getId());

        // Buscar recipientId do courier
        User courier = delivery.getCourier();
        String courierRecipientId = courier.getPagarmeRecipientId();
        if (courierRecipientId == null || courierRecipientId.isBlank()) {
            throw new RuntimeException("Courier não tem recipientId configurado no Pagar.me");
        }

        // Buscar cartão padrão do CUSTOMER
        CustomerCard card;
        try {
            card = cardService.getDefaultCard(customer.getId());
        } catch (Exception e) {
            throw new RuntimeException("Cliente CUSTOMER não tem cartão padrão cadastrado: " + e.getMessage());
        }

        if (!card.getIsActive() || card.isExpired()) {
            throw new RuntimeException("Cartão padrão do CUSTOMER está inativo ou expirado");
        }

        log.info("   ├─ Cartão: {} **** {}", card.getBrand(), card.getLastFourDigits());

        OrderRequest.BillingAddressRequest billingAddress = OrderRequest.BillingAddressRequest.builder()
                .line1(delivery.getFromAddress() != null ? delivery.getFromAddress() : "Endereço não informado")
                .zipCode("00000000")
                .city("São Paulo")
                .state("SP")
                .country("BR")
                .build();

        // CUSTOMER não tem organizer → split sem organizer (87% courier, 13% plataforma)
        log.info("   ├─ Criando order Cartão no Pagar.me (sem organizer)...");

        // Buscar recipientId da plataforma
        SiteConfiguration config = siteConfigurationService.getActiveConfiguration();
        String platformRecipientId = config.getPagarmeRecipientId();

        OrderResponse orderResponse;
        try {
            orderResponse = pagarMeService.createOrderWithCreditCardSplit(
                    delivery.getShippingFee(),
                    "Entrega #" + delivery.getId(),
                    card.getPagarmeCardId(),
                    customer.getName() != null ? customer.getName() : customer.getUsername(),
                    customer.getUsername(),
                    "00000000000",
                    billingAddress,
                    courierRecipientId,
                    null, // organizerRecipientId — CUSTOMER não tem organizer
                    "ZAPI10",
                    platformRecipientId
            );
        } catch (Exception e) {
            log.error("   ├─ ❌ Cartão recusado pelo gateway: {}", e.getMessage());

            // Salvar Payment com status FAILED em transação independente (não sofre rollback)
            paymentService.saveFailedPayment(
                    delivery.getShippingFee(),
                    PaymentMethod.CREDIT_CARD,
                    customer,
                    delivery,
                    "Cartão recusado: " + e.getMessage()
            );

            throw e;
        }

        log.info("   ├─ ✅ Order Cartão criada: {}", orderResponse.getId());

        // Criar Payment no banco
        Payment payment = new Payment();
        payment.setProviderPaymentId(orderResponse.getId());
        payment.setAmount(delivery.getShippingFee());
        payment.setCurrency(Currency.BRL);
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        payment.setProvider(PaymentProvider.PAGARME);
        payment.setPayer(customer);
        payment.setStatus(PaymentStatus.PENDING);
        payment.addDelivery(delivery);

        // CRÍTICO: Capturar request/response do OrderResponse
        String req = orderResponse.getRequestPayload();
        String resp = orderResponse.getResponsePayload();
        
        log.info("   ├─ 🔍 Request payload: {} caracteres", req != null ? req.length() : "NULL");
        log.info("   ├─ 🔍 Response payload: {} caracteres", resp != null ? resp.length() : "NULL");
        
        if (req != null && !req.isEmpty()) {
            payment.setRequest(req);
            log.info("   ├─ ✅ Request setado no Payment");
        } else {
            log.error("   ├─ ❌ Request está NULL ou vazio!");
        }
        
        if (resp != null && !resp.isEmpty()) {
            payment.setResponse(resp);
            log.info("   ├─ ✅ Response setado no Payment");
        } else {
            log.error("   ├─ ❌ Response está NULL ou vazio!");
        }
        
        // Adicionar notes com informações do pagamento
        String notes = String.format("Pagamento %s - Order ID: %s - Cartão: %s****%s",
                orderResponse.getStatus(),
                orderResponse.getId(),
                card.getBrand(),
                card.getLastFourDigits());
        payment.setNotes(notes);
        log.info("   ├─ 📝 Notes setado: {}", notes);
        
        // Determinar status real do pagamento (verificando order + charges + transactions)
        PaymentStatus finalStatus = determinePaymentStatus(orderResponse);
        payment.setStatus(finalStatus);
        
        if (finalStatus == PaymentStatus.FAILED) {
            log.warn("   ├─ ⚠️ Pagamento FAILED - Order Status: {}", orderResponse.getStatus());
        } else if (finalStatus == PaymentStatus.PAID) {
            log.info("   ├─ ✅ Pagamento PAID imediatamente");
        } else {
            log.info("   ├─ ⏳ Pagamento PENDING");
        }

        Payment savedPayment = paymentRepository.save(payment);
        log.info("   ├─ ✅ Payment #{} salvo no banco (Request: {} chars, Response: {} chars, Status: {})", 
            savedPayment.getId(),
            savedPayment.getRequest() != null ? savedPayment.getRequest().length() : 0,
            savedPayment.getResponse() != null ? savedPayment.getResponse().length() : 0,
            savedPayment.getStatus());

        // Enviar notificação push se o pagamento falhou
        if (finalStatus == PaymentStatus.FAILED) {
            try {
                String failureMessage = paymentService.extractPaymentFailureMessage(orderResponse);
                String notificationBody = String.format("Pagamento de R$ %.2f não foi aprovado. %s Por favor, escolha outro método de pagamento.", 
                    delivery.getShippingFee(), failureMessage);
                
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("type", "payment_failed");
                notificationData.put("deliveryId", delivery.getId());
                notificationData.put("paymentId", savedPayment.getId());
                notificationData.put("amount", delivery.getShippingFee().toString());
                notificationData.put("failureReason", failureMessage);
                
                boolean sent = pushNotificationService.sendNotificationToUser(
                    customer.getId(),
                    "❌ Pagamento não aprovado",
                    notificationBody,
                    notificationData
                );
                
                if (sent) {
                    log.info("   ├─ ✅ Notificação de falha enviada ao cliente #{}", customer.getId());
                } else {
                    log.warn("   ├─ ⚠️ Não foi possível enviar notificação - cliente #{} sem token push ativo", customer.getId());
                }
            } catch (Exception e) {
                log.error("   ├─ ❌ Erro ao enviar notificação de falha: {}", e.getMessage());
            }
        }

        // Marcar delivery apenas se não falhou
        if (finalStatus != PaymentStatus.FAILED) {
            delivery.setPaymentCompleted(false);
            delivery.setPaymentCaptured(false);
            deliveryRepository.save(delivery);
            log.info("   └─ ✅ Pagamento CARTÃO CUSTOMER criado com sucesso para delivery #{}", delivery.getId());
        } else {
            log.error("   └─ ❌ Pagamento FAILED - Delivery não marcada como paga");
        }
    }
    
    /**
     * Determina o status final do Payment baseado na OrderResponse do Pagar.me.
     * 
     * Regras:
     * - Status "paid" → PAID
     * - Status "failed", "canceled", "cancelled" → FAILED
     * - Transação com status "not_authorized", "refused", "failed" → FAILED
     * - Antifraude "reproved" → FAILED
     * - Caso contrário → PENDING
     * 
     * @param orderResponse Response recebida do Pagar.me
     * @return PaymentStatus apropriado
     */
    private PaymentStatus determinePaymentStatus(com.mvt.mvt_events.payment.dto.OrderResponse orderResponse) {
        if (orderResponse == null) {
            return PaymentStatus.PENDING;
        }
        
        String orderStatus = orderResponse.getStatus();
        
        // 1. Verificar status da order
        if ("paid".equalsIgnoreCase(orderStatus)) {
            return PaymentStatus.PAID;
        }
        
        if ("failed".equalsIgnoreCase(orderStatus) || 
            "canceled".equalsIgnoreCase(orderStatus) || 
            "cancelled".equalsIgnoreCase(orderStatus)) {
            return PaymentStatus.FAILED;
        }
        
        // 2. Verificar charges e última transação
        if (orderResponse.getCharges() != null && !orderResponse.getCharges().isEmpty()) {
            com.mvt.mvt_events.payment.dto.OrderResponse.Charge charge = orderResponse.getCharges().get(0);
            
            // Status da charge
            if ("failed".equalsIgnoreCase(charge.getStatus())) {
                return PaymentStatus.FAILED;
            }
            
            // Última transação
            if (charge.getLastTransaction() != null) {
                com.mvt.mvt_events.payment.dto.OrderResponse.LastTransaction transaction = charge.getLastTransaction();
                
                // Status da transação
                String txStatus = transaction.getStatus();
                
                // PIX: status 'waiting_payment' é normal (aguardando customer pagar QR Code)
                if ("waiting_payment".equalsIgnoreCase(txStatus)) {
                    log.info("⏳ Transação PIX aguardando pagamento (waiting_payment) — status PENDING");
                    return PaymentStatus.PENDING;
                }
                
                if ("not_authorized".equalsIgnoreCase(txStatus) ||
                    "refused".equalsIgnoreCase(txStatus) ||
                    "failed".equalsIgnoreCase(txStatus)) {
                    log.warn("🚫 Transação com status de falha: {}", txStatus);
                    return PaymentStatus.FAILED;
                }
                
                // Success flag (só verifica para transações não-PIX que já terminaram)
                if (transaction.getSuccess() != null && !transaction.getSuccess()
                    && !"pix".equalsIgnoreCase(transaction.getTransactionType())) {
                    log.warn("🚫 Transação marcada como success=false");
                    return PaymentStatus.FAILED;
                }
                
                // Antifraude (pode estar como Map ou objeto complexo)
                if (transaction.getAntifraudResponse() != null) {
                    try {
                        // Tentar como Map
                        if (transaction.getAntifraudResponse() instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> antifraudMap = 
                                (java.util.Map<String, Object>) transaction.getAntifraudResponse();
                            Object status = antifraudMap.get("status");
                            if ("reproved".equalsIgnoreCase(String.valueOf(status))) {
                                log.warn("🚫 Antifraude reprovou a transação");
                                return PaymentStatus.FAILED;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Erro ao verificar antifraude: {}", e.getMessage());
                    }
                }
            }
        }
        
        // Default: PENDING
        return PaymentStatus.PENDING;
    }
}
