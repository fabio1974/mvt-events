package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.push.DeliveryNotificationData;
import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service responsável por gerenciar notificações push para motoboys com sistema
 * de 3 níveis:
 * 
 * Nível 1: Motoboys da organização titular do cliente (isPrimary=true)
 * Nível 2: Motoboys de outras organizações conectadas ao cliente
 * Nível 3: Todos os motoboys próximos geograficamente
 * 
 * Escalação geográfica: 5km → 10km se não houver motoboys
 * Intervalos de tempo: 2 minutos entre cada nível
 */
@Service
@Slf4j
@Transactional
public class DeliveryNotificationService {

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private ClientContractRepository clientContractRepository;

    @Autowired
    private EmploymentContractRepository employmentContractRepository;

    @Autowired
    private UserPushTokenRepository userPushTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    // Configurações do sistema de notificação
    private static final double INITIAL_RADIUS_KM = 5.0;
    private static final double EXTENDED_RADIUS_KM = 10.0;
    private static final long LEVEL_TIMEOUT_MINUTES = 2;

    /**
     * Inicia o processo de notificação para uma nova delivery seguindo o sistema de
     * 3 níveis
     */
    @Async("notificationTaskExecutor")
    public CompletableFuture<Void> notifyAvailableDrivers(Delivery delivery) {
        log.info("Iniciando notificação de motoboys para delivery {}", delivery.getId());

        try {
            // Nível 1: Organização titular do cliente
            boolean level1Notified = notifyLevel1PrimaryOrganization(delivery);
            if (level1Notified) {
                log.info("Notificação Nível 1 enviada para delivery {}", delivery.getId());
                
                // Aguardar 2 minutos antes do Nível 2
                Thread.sleep(TimeUnit.MINUTES.toMillis(LEVEL_TIMEOUT_MINUTES));

                // Verificar se a delivery ainda está PENDING
                if (!isDeliveryStillPending(delivery.getId())) {
                    log.info("Delivery {} foi aceita durante timeout do Nível 1", delivery.getId());
                    return CompletableFuture.completedFuture(null);
                }
            } else {
                log.info("Nível 1 não encontrou motoboys, pulando direto para Nível 2");
            }

            // Nível 2: Outras organizações do cliente
            boolean level2Notified = notifyLevel2OtherOrganizations(delivery);
            if (level2Notified) {
                log.info("Notificação Nível 2 enviada para delivery {}", delivery.getId());
                
                // Aguardar 2 minutos antes do Nível 3
                Thread.sleep(TimeUnit.MINUTES.toMillis(LEVEL_TIMEOUT_MINUTES));

                // Verificar se a delivery ainda está PENDING
                if (!isDeliveryStillPending(delivery.getId())) {
                    log.info("Delivery {} foi aceita durante timeout do Nível 2", delivery.getId());
                    return CompletableFuture.completedFuture(null);
                }
            } else {
                log.info("Nível 2 não encontrou motoboys, pulando direto para Nível 3");
            }

            // Nível 3: Todos os motoboys próximos
            notifyLevel3AllNearbyDrivers(delivery);
            log.info("Notificação Nível 3 enviada para delivery {}", delivery.getId());

        } catch (InterruptedException e) {
            log.error("Processo de notificação interrompido para delivery {}", delivery.getId(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Erro no processo de notificação para delivery {}", delivery.getId(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Nível 1: Notificar motoboys da organização titular do cliente
     * (isPrimary=true)
     */
    private boolean notifyLevel1PrimaryOrganization(Delivery delivery) {
        log.info("Executando Nível 1: Organização titular para delivery {}", delivery.getId());

        // Buscar contrato titular do cliente
        ClientContract primaryContract = clientContractRepository.findPrimaryByClient(delivery.getClient())
                .orElse(null);

        if (primaryContract == null) {
            log.warn("Cliente {} não possui contrato titular", delivery.getClient().getId());
            return false;
        }

        // Buscar motoboys ativos da organização titular
        List<EmploymentContract> employmentContracts = employmentContractRepository
                .findActiveByOrganization(primaryContract.getOrganization());

        Set<UUID> courierIds = employmentContracts.stream()
                .map(contract -> contract.getCourier().getId())
                .collect(Collectors.toSet());

        if (courierIds.isEmpty()) {
            log.warn("Nenhum motoboy ativo encontrado na organização titular {} para delivery {}",
                    primaryContract.getOrganization().getId(), delivery.getId());
            return false;
        }

        // Filtrar apenas motoboys disponíveis e próximos
        List<User> availableCouriers = getAvailableCouriersInArea(
                delivery.getFromLatitude(), delivery.getFromLongitude(), INITIAL_RADIUS_KM, courierIds);

        if (availableCouriers.isEmpty()) {
            // Tentar raio estendido
            availableCouriers = getAvailableCouriersInArea(
                    delivery.getFromLatitude(), delivery.getFromLongitude(), EXTENDED_RADIUS_KM, courierIds);
        }

        if (availableCouriers.isEmpty()) {
            log.warn("Nenhum motoboy disponível encontrado na organização titular para delivery {}", delivery.getId());
            return false;
        }

        // Enviar notificações
        sendNotificationsToDrivers(availableCouriers, delivery, "Nível 1 - Organização Titular");
        return true;
    }

    /**
     * Nível 2: Notificar motoboys de outras organizações conectadas ao cliente
     */
    private boolean notifyLevel2OtherOrganizations(Delivery delivery) {
        log.info("Executando Nível 2: Outras organizações para delivery {}", delivery.getId());

        // Buscar todos os contratos ativos do cliente (exceto o titular)
        List<ClientContract> allContracts = clientContractRepository.findActiveByClient(delivery.getClient());
        List<ClientContract> secondaryContracts = allContracts.stream()
                .filter(contract -> !contract.isPrimary())
                .collect(Collectors.toList());

        if (secondaryContracts.isEmpty()) {
            log.warn("Cliente {} não possui contratos secundários", delivery.getClient().getId());
            return false;
        }

        // Coletar motoboys de todas as organizações secundárias
        Set<UUID> courierIds = new HashSet<>();
        for (ClientContract contract : secondaryContracts) {
            List<EmploymentContract> employmentContracts = employmentContractRepository
                    .findActiveByOrganization(contract.getOrganization());

            courierIds.addAll(employmentContracts.stream()
                    .map(emp -> emp.getCourier().getId())
                    .collect(Collectors.toSet()));
        }

        if (courierIds.isEmpty()) {
            log.warn("Nenhum motoboy ativo encontrado nas organizações secundárias para delivery {}", delivery.getId());
            return false;
        }

        // Filtrar apenas motoboys disponíveis e próximos
        List<User> availableCouriers = getAvailableCouriersInArea(
                delivery.getFromLatitude(), delivery.getFromLongitude(), INITIAL_RADIUS_KM, courierIds);

        if (availableCouriers.isEmpty()) {
            // Tentar raio estendido
            availableCouriers = getAvailableCouriersInArea(
                    delivery.getFromLatitude(), delivery.getFromLongitude(), EXTENDED_RADIUS_KM, courierIds);
        }

        if (availableCouriers.isEmpty()) {
            log.warn("Nenhum motoboy disponível encontrado nas organizações secundárias para delivery {}",
                    delivery.getId());
            return false;
        }

        // Enviar notificações
        sendNotificationsToDrivers(availableCouriers, delivery, "Nível 2 - Outras Organizações");
        return true;
    }

    /**
     * Nível 3: Notificar todos os motoboys próximos geograficamente (sem restrição
     * de organização)
     */
    private boolean notifyLevel3AllNearbyDrivers(Delivery delivery) {
        log.info("Executando Nível 3: Todos motoboys próximos para delivery {}", delivery.getId());

        // Buscar todos os motoboys disponíveis próximos (sem filtro de organização)
        List<User> availableCouriers = userRepository.findAvailableCouriersNearby(
                delivery.getFromLatitude(), delivery.getFromLongitude(), INITIAL_RADIUS_KM);

        if (availableCouriers.isEmpty()) {
            // Tentar raio estendido
            availableCouriers = userRepository.findAvailableCouriersNearby(
                    delivery.getFromLatitude(), delivery.getFromLongitude(), EXTENDED_RADIUS_KM);
        }

        if (availableCouriers.isEmpty()) {
            log.warn("Nenhum motoboy disponível encontrado em toda a área para delivery {}", delivery.getId());
            return false;
        }

        // Enviar notificações
        sendNotificationsToDrivers(availableCouriers, delivery, "Nível 3 - Todos Próximos");
        return true;
    }

    /**
     * Busca motoboys disponíveis em uma área específica, filtrados por IDs
     */
    private List<User> getAvailableCouriersInArea(Double latitude, Double longitude,
            Double radiusKm, Set<UUID> courierIds) {

        // Buscar todos os motoboys próximos
        List<User> nearbyCouriers = userRepository.findAvailableCouriersNearby(
                latitude, longitude, radiusKm);

        // Filtrar pelos IDs especificados
        return nearbyCouriers.stream()
                .filter(courier -> courierIds.contains(courier.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Envia notificações push HÍBRIDAS para uma lista de motoboys (Expo + Web Push)
     * Envia sequencialmente com delay de 5 segundos entre cada notificação
     * Interrompe se a delivery for aceita durante o processo
     */
    private void sendNotificationsToDrivers(List<User> couriers, Delivery delivery, String level) {
        log.info("Enviando notificações HÍBRIDAS {} sequencialmente para {} motoboys da delivery {} (delay 5s entre cada)",
                level, couriers.size(), delivery.getId());

        int notificationCount = 0;
        
        for (User courier : couriers) {
            try {
                // Verificar se a delivery ainda está PENDING antes de cada notificação
                if (!isDeliveryStillPending(delivery.getId())) {
                    log.info("Delivery {} foi aceita durante processo de notificação {} - Parando envio. Total enviado: {}/{}",
                            delivery.getId(), level, notificationCount, couriers.size());
                    return;
                }

                // Buscar tokens ativos do motoboy
                List<UserPushToken> tokens = userPushTokenRepository
                        .findByUserIdAndIsActiveTrue(courier.getId());

                if (tokens.isEmpty()) {
                    log.debug("Motoboy {} não possui tokens push ativos - pulando", courier.getId());
                    continue;
                }

                log.info("Notificando motoboy {} ({}) - Posição: {}/{}",
                        courier.getName(), courier.getId(), notificationCount + 1, couriers.size());

                // Criar dados da notificação
                DeliveryNotificationData notificationData = createNotificationData(delivery, level);

                // Título e corpo da notificação
                String title = "🚚 Nova Entrega Disponível!";
                String body = String.format("Entrega de R$ %.2f - %s (%s)",
                        delivery.getTotalAmount(), delivery.getClientName(), level);

                // Dados da notificação como Map para compatibilidade
                Map<String, Object> data = Map.of(
                        "type", "delivery_invite",
                        "deliveryId", delivery.getId().toString(),
                        "level", level,
                        "clientName", delivery.getClientName() != null ? delivery.getClientName() : "Cliente",
                        "value", delivery.getTotalAmount().toString(),
                        "address", delivery.getFromAddress() != null ? delivery.getFromAddress() : "",
                        "pickupLatitude",
                        delivery.getFromLatitude() != null ? delivery.getFromLatitude().toString() : "0",
                        "pickupLongitude",
                        delivery.getFromLongitude() != null ? delivery.getFromLongitude().toString() : "0",
                        "deliveryLatitude",
                        delivery.getToLatitude() != null ? delivery.getToLatitude().toString() : "0",
                        "deliveryLongitude",
                        delivery.getToLongitude() != null ? delivery.getToLongitude().toString() : "0");

                // Enviar notificação HÍBRIDA usando o novo método
                pushNotificationService.sendHybridNotificationToUser(
                        courier.getId(),
                        title,
                        body,
                        data);

                notificationCount++;
                log.debug("Notificação híbrida {} enviada com sucesso para motoboy {} ({}/{})",
                        level, courier.getId(), notificationCount, couriers.size());

                // Aguardar 5 segundos antes da próxima notificação (exceto na última)
                if (notificationCount < couriers.size()) {
                    log.debug("Aguardando 5 segundos antes de notificar próximo motoboy...");
                    Thread.sleep(5000);
                }

            } catch (InterruptedException e) {
                log.warn("Processo de notificação {} interrompido para delivery {}", level, delivery.getId());
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Erro ao enviar notificação híbrida {} para motoboy {}: {}",
                        level, courier.getId(), e.getMessage());
                // Continua para o próximo motoboy mesmo se houver erro
            }
        }
        
        log.info("Processo de notificação {} concluído para delivery {} - Total enviado: {}/{}",
                level, delivery.getId(), notificationCount, couriers.size());
    }

    /**
     * Cria objeto de dados da notificação
     */
    private DeliveryNotificationData createNotificationData(Delivery delivery, String level) {
        String message = String.format("Nova entrega disponível (%s) - R$ %.2f",
                level, delivery.getTotalAmount());

        DeliveryNotificationData.DeliveryData deliveryData = DeliveryNotificationData.DeliveryData.builder()
                .clientName(delivery.getClientName())
                .value(delivery.getTotalAmount())
                .address(delivery.getFromAddress())
                .pickupLatitude(delivery.getFromLatitude())
                .pickupLongitude(delivery.getFromLongitude())
                .deliveryLatitude(delivery.getToLatitude())
                .deliveryLongitude(delivery.getToLongitude())
                .description(delivery.getItemDescription())
                .estimatedTime(delivery.getEstimatedTimeMinutes() != null ? delivery.getEstimatedTimeMinutes() + " min"
                        : "N/A")
                .build();

        return DeliveryNotificationData.builder()
                .type("NEW_DELIVERY")
                .deliveryId(delivery.getId().toString())
                .message(message)
                .deliveryData(deliveryData)
                .build();
    }

    /**
     * Verifica se a delivery ainda está pendente
     */
    private boolean isDeliveryStillPending(Long deliveryId) {
        try {
            Delivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
            return delivery != null && delivery.getStatus() == Delivery.DeliveryStatus.PENDING;
        } catch (Exception e) {
            log.error("Erro ao verificar status da delivery {}: {}", deliveryId, e.getMessage());
            return false; // Se houve erro, considerar que não está mais pendente
        }
    }

    /**
     * Converte Long ID para UUID (necessário pois PushNotificationService espera
     * UUID)
     * Usa uma conversão determinística baseada no ID
     */
    private UUID convertLongToUUID(Long id) {
        // Criar UUID determinístico baseado no ID da delivery
        String idString = String.format("00000000-0000-0000-0000-%012d", id);
        return UUID.fromString(idString);
    }
}