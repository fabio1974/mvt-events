package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.*;
import com.mvt.mvt_events.specification.DeliverySpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service para Delivery - ENTIDADE CORE DO ZAPI10
 * IMPORTANTE: Todas as operações devem filtrar por ADM (tenant)
 */
@Service
@Transactional
public class DeliveryService {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourierProfileRepository courierProfileRepository;

    @Autowired
    private DeliveryNotificationService deliveryNotificationService;

    @Autowired
    private com.mvt.mvt_events.repository.OrganizationRepository organizationRepository;

    // TODO: ADMProfileRepository não mais usado após remoção de CourierADMLink
    // @Autowired
    // private ADMProfileRepository admProfileRepository;

    // TODO: CourierADMLinkRepository removido - agora Courier se relaciona com
    // Organization via EmploymentContract
    // @Autowired
    // private CourierADMLinkRepository courierADMLinkRepository;

    /**
     * Criar nova delivery (qualquer usuário autenticado)
     * VALIDA: Cliente existe, usuário existe, parceria (se fornecida)
     */
    public Delivery create(Delivery delivery, UUID creatorId, UUID clientId) {
        // Validar usuário que está criando (pode ser qualquer role)
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Validar cliente
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        if (client.getRole() != User.Role.CLIENT) {
            throw new RuntimeException("Usuário não é um cliente");
        }

        // A organização da delivery é determinada pela organização do cliente
        // Não precisamos mais do campo adm - usar client.organization
        delivery.setClient(client);
        delivery.setStatus(Delivery.DeliveryStatus.PENDING); // Calcular distância estimada (se coordenadas fornecidas)
        if (delivery.getFromLatitude() != null && delivery.getFromLongitude() != null &&
                delivery.getToLatitude() != null && delivery.getToLongitude() != null) {
            @SuppressWarnings("unused")
            double distance = calculateDistance(
                    delivery.getFromLatitude(), delivery.getFromLongitude(),
                    delivery.getToLatitude(), delivery.getToLongitude());
            // Apenas calcular, não salvar (campo não existe na entidade)
        }

        Delivery savedDelivery = deliveryRepository.save(delivery);

        // Iniciar processo de notificação para motoboys disponíveis
        deliveryNotificationService.notifyAvailableDrivers(savedDelivery);

        return savedDelivery;
    }

    /**
     * Busca delivery por ID com validação de tenant
     */
    public Delivery findById(Long id, Long organizationId) {
        Specification<Delivery> spec = DeliverySpecification.hasId(id)
                .and(DeliverySpecification.hasClientOrganizationId(organizationId));

        return deliveryRepository.findOne(spec)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada ou sem acesso"));
    }

    /**
     * Lista deliveries com filtros e tenant
     */
    public Page<Delivery> findAll(Long organizationId, UUID clientId, UUID courierId,
            Delivery.DeliveryStatus status,
            LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable) {
        // Para simplificar e evitar o problema de lazy loading,
        // vamos usar apenas o filtro por organizationId primeiro
        if (clientId == null && courierId == null && status == null &&
                startDate == null && endDate == null) {
            // Caso simples - usar query com fetch joins por organização
            List<Delivery> deliveries = deliveryRepository.findAllWithJoinsByOrganizationId(organizationId);
            // Converter para Page manualmente
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), deliveries.size());
            List<Delivery> pageContent = deliveries.subList(start, end);
            return new PageImpl<>(pageContent, pageable, deliveries.size());
        }

        // Caso complexo - usar specifications (pode ter lazy loading)
        Specification<Delivery> spec = DeliverySpecification.hasClientOrganizationId(organizationId)
                .and(DeliverySpecification.hasClientId(clientId))
                .and(DeliverySpecification.hasCourierId(courierId))
                .and(DeliverySpecification.hasStatus(status))
                .and(DeliverySpecification.createdBetween(startDate, endDate));

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
     */
    public Delivery assignToCourier(Long deliveryId, UUID courierId, Long organizationId) {
        Delivery delivery = findById(deliveryId, organizationId);

        if (delivery.getStatus() != Delivery.DeliveryStatus.PENDING) {
            throw new RuntimeException("Delivery não está pendente");
        }

        // Validar courier
        CourierProfile courier = courierProfileRepository.findByUserId(courierId)
                .orElseThrow(() -> new RuntimeException("Courier não encontrado"));

        if (courier.getStatus() != CourierProfile.CourierStatus.AVAILABLE &&
                courier.getStatus() != CourierProfile.CourierStatus.ON_DELIVERY) {
            throw new RuntimeException("Courier não está disponível");
        }

        // TODO: Validação de vínculo courier-Organization via EmploymentContract
        // Verificar se o courier tem contrato ativo com a organização do cliente

        // Atribuir
        delivery.setCourier(courier.getUser());
        delivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);

        // Atualizar métricas do courier
        courier.setTotalDeliveries(courier.getTotalDeliveries() + 1);
        courierProfileRepository.save(courier);

        return deliveryRepository.save(delivery);
    }

    /**
     * Courier confirma coleta
     */
    public Delivery confirmPickup(Long deliveryId, UUID courierId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));

        if (!delivery.getCourier().getId().equals(courierId)) {
            throw new RuntimeException("Delivery não pertence a este courier");
        }

        if (delivery.getStatus() != Delivery.DeliveryStatus.ACCEPTED) {
            throw new RuntimeException("Status inválido para coleta");
        }

        delivery.setStatus(Delivery.DeliveryStatus.PICKED_UP);

        return deliveryRepository.save(delivery);
    }

    /**
     * Courier inicia transporte
     */
    public Delivery startTransit(Long deliveryId, UUID courierId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));

        if (!delivery.getCourier().getId().equals(courierId)) {
            throw new RuntimeException("Delivery não pertence a este courier");
        }

        if (delivery.getStatus() != Delivery.DeliveryStatus.PICKED_UP) {
            throw new RuntimeException("Status inválido para iniciar transporte");
        }

        delivery.setStatus(Delivery.DeliveryStatus.IN_TRANSIT);

        return deliveryRepository.save(delivery);
    }

    /**
     * Completa delivery
     * Atualiza métricas do courier
     */
    public Delivery complete(Long deliveryId, UUID courierId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));

        if (!delivery.getCourier().getId().equals(courierId)) {
            throw new RuntimeException("Delivery não pertence a este courier");
        }

        if (delivery.getStatus() != Delivery.DeliveryStatus.IN_TRANSIT) {
            throw new RuntimeException("Status inválido para completar");
        }

        delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);

        // Atualizar métricas do courier
        CourierProfile courier = courierProfileRepository.findByUserId(courierId)
                .orElseThrow(() -> new RuntimeException("Courier não encontrado"));
        courier.setCompletedDeliveries(courier.getCompletedDeliveries() + 1);
        courierProfileRepository.save(courier);

        return deliveryRepository.save(delivery);
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

        // Se tinha courier atribuído, atualizar métricas
        if (delivery.getCourier() != null) {
            CourierProfile courier = courierProfileRepository.findByUserId(delivery.getCourier().getId())
                    .orElse(null);
            if (courier != null) {
                courier.setCancelledDeliveries(courier.getCancelledDeliveries() + 1);
                courierProfileRepository.save(courier);
            }
        }

        return deliveryRepository.save(delivery);
    }

    /**
     * Busca deliveries pendentes de atribuição
     */
    public List<Delivery> findPendingAssignment(Long organizationId) {
        return deliveryRepository.findPendingAssignmentByOrganizationId(organizationId);
    }

    /**
     * Busca deliveries ativas de um courier
     */
    public List<Delivery> findActiveByCourier(UUID courierId) {
        return deliveryRepository.findActiveByCourierId(courierId);
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
}
