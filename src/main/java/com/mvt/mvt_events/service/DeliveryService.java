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

    @Autowired
    private EmploymentContractRepository employmentContractRepository;

    @Autowired
    private ClientContractRepository clientContractRepository;

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

        // Validação de role: cliente deve ser CLIENT
        if (client.getRole() != User.Role.CLIENT) {
            throw new RuntimeException("O destinatário da entrega deve ser um CLIENT (role atual: " + client.getRole() + ")");
        }

        // Validação de permissões do criador
        User.Role creatorRole = creator.getRole();
        
        if (creatorRole == User.Role.CLIENT) {
            // CLIENT só pode criar entregas para si mesmo
            if (!creator.getId().equals(client.getId())) {
                throw new RuntimeException("CLIENT só pode criar entregas para si mesmo");
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
        // Buscar com joins para evitar lazy loading
        Delivery delivery = deliveryRepository.findByIdWithJoins(id)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));
        
        // Validar acesso por organização se necessário
        if (organizationId != null) {
            if (delivery.getClient() == null || 
                delivery.getClient().getOrganization() == null || 
                !delivery.getClient().getOrganization().getId().equals(organizationId)) {
                throw new RuntimeException("Delivery não encontrada ou sem acesso");
            }
        }
        
        return delivery;
    }

    /**
     * Lista deliveries com filtros e tenant
     */
    public Page<Delivery> findAll(Long organizationId, UUID clientId, UUID courierId,
            Delivery.DeliveryStatus status,
            LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable) {
        
        // Caso especial: busca por clientId específico (para role CLIENT)
        if (clientId != null && courierId == null && organizationId == null &&
                startDate == null && endDate == null) {
            List<Delivery> deliveries;
            
            if (status != null) {
                // Com filtro de status
                deliveries = deliveryRepository.findByClientIdAndStatusWithJoins(clientId, status);
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

        // Buscar o User do courier para evitar lazy loading
        User courierUser = userRepository.findById(courierId)
                .orElseThrow(() -> new RuntimeException("Usuário courier não encontrado"));

        // Buscar a organização comum entre courier (employment) e client (client contract)
        Organization commonOrganization = findCommonOrganization(courierUser, delivery.getClient());
        if (commonOrganization == null) {
            throw new RuntimeException("Courier e Client não compartilham uma organização comum através de contratos ativos");
        }

        // Atribuir
        delivery.setCourier(courierUser);
        delivery.setOrganization(commonOrganization);
        delivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
        delivery.setAcceptedAt(LocalDateTime.now());

        // Atualizar métricas do courier
        courier.setTotalDeliveries(courier.getTotalDeliveries() + 1);
        courierProfileRepository.save(courier);

        Delivery saved = deliveryRepository.save(delivery);
        
        // Recarregar a delivery com todos os relacionamentos para evitar lazy loading
        return deliveryRepository.findByIdWithJoins(saved.getId())
                .orElse(saved);
    }

    /**
     * Courier confirma coleta
     */
    public Delivery confirmPickup(Long deliveryId, UUID courierId) {
        Delivery delivery = deliveryRepository.findByIdWithJoins(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));

        if (!delivery.getCourier().getId().equals(courierId)) {
            throw new RuntimeException("Delivery não pertence a este courier");
        }

        if (delivery.getStatus() != Delivery.DeliveryStatus.ACCEPTED) {
            throw new RuntimeException("Status inválido para coleta");
        }

        delivery.setStatus(Delivery.DeliveryStatus.PICKED_UP);
        delivery.setPickedUpAt(LocalDateTime.now());

        Delivery saved = deliveryRepository.save(delivery);
        
        // Recarregar com joins
        return deliveryRepository.findByIdWithJoins(saved.getId())
                .orElse(saved);
    }

    /**
     * Courier inicia transporte
     */
    public Delivery startTransit(Long deliveryId, UUID courierId) {
        Delivery delivery = deliveryRepository.findByIdWithJoins(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));

        if (!delivery.getCourier().getId().equals(courierId)) {
            throw new RuntimeException("Delivery não pertence a este courier");
        }

        if (delivery.getStatus() != Delivery.DeliveryStatus.PICKED_UP) {
            throw new RuntimeException("Status inválido para iniciar transporte");
        }

        delivery.setStatus(Delivery.DeliveryStatus.IN_TRANSIT);
        delivery.setInTransitAt(LocalDateTime.now());

        Delivery saved = deliveryRepository.save(delivery);
        
        // Recarregar com joins
        return deliveryRepository.findByIdWithJoins(saved.getId())
                .orElse(saved);
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

        // Atualizar métricas do courier
        CourierProfile courier = courierProfileRepository.findByUserId(courierId)
                .orElseThrow(() -> new RuntimeException("Courier não encontrado"));
        courier.setCompletedDeliveries(courier.getCompletedDeliveries() + 1);
        courierProfileRepository.save(courier);

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

        // Se tinha courier atribuído, atualizar métricas e remover courier e organization
        if (delivery.getCourier() != null) {
            CourierProfile courier = courierProfileRepository.findByUserId(delivery.getCourier().getId())
                    .orElse(null);
            if (courier != null) {
                courier.setCancelledDeliveries(courier.getCancelledDeliveries() + 1);
                courierProfileRepository.save(courier);
            }
            
            // Remover courier e organization
            delivery.setCourier(null);
            delivery.setOrganization(null);
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

            case PICKED_UP:
                if (delivery.getAcceptedAt() == null) {
                    delivery.setAcceptedAt(now);
                }
                delivery.setPickedUpAt(now);
                // Limpar timestamps posteriores
                delivery.setInTransitAt(null);
                delivery.setCompletedAt(null);
                break;

            case IN_TRANSIT:
                if (delivery.getAcceptedAt() == null) {
                    delivery.setAcceptedAt(now);
                }
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

                // Atualizar métricas do courier
                if (delivery.getCourier() != null) {
                    CourierProfile courier = courierProfileRepository.findByUserId(delivery.getCourier().getId())
                            .orElse(null);
                    if (courier != null) {
                        courier.setCompletedDeliveries(courier.getCompletedDeliveries() + 1);
                        courierProfileRepository.save(courier);
                    }
                }
                break;

            case CANCELLED:
                delivery.setCancelledAt(now);
                delivery.setCancellationReason(reason);
                
                // IMPORTANTE: Remover courier e voltar para PENDING
                if (delivery.getCourier() != null) {
                    // Atualizar métricas do courier
                    CourierProfile courier = courierProfileRepository.findByUserId(delivery.getCourier().getId())
                            .orElse(null);
                    if (courier != null) {
                        courier.setCancelledDeliveries(courier.getCancelledDeliveries() + 1);
                        courierProfileRepository.save(courier);
                    }
                    
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

        // Validar fluxo normal: PENDING -> ACCEPTED -> PICKED_UP -> IN_TRANSIT -> COMPLETED
        switch (current) {
            case PENDING:
                if (target != Delivery.DeliveryStatus.ACCEPTED) {
                    throw new RuntimeException("De PENDING só pode ir para ACCEPTED");
                }
                break;
            case ACCEPTED:
                if (target != Delivery.DeliveryStatus.PICKED_UP) {
                    throw new RuntimeException("De ACCEPTED só pode ir para PICKED_UP");
                }
                break;
            case PICKED_UP:
                if (target != Delivery.DeliveryStatus.IN_TRANSIT) {
                    throw new RuntimeException("De PICKED_UP só pode ir para IN_TRANSIT");
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
}
