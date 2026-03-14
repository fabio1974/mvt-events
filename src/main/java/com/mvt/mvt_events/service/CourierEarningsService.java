package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.CourierEarningsResponse;
import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.PaymentStatus;
import com.mvt.mvt_events.jpa.SiteConfiguration;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.repository.SiteConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço para gerenciar recebimentos de couriers.
 * Lista todas as deliveries completadas com pagamento confirmado (PAID),
 * mostrando o detalhamento da repartição de cada corrida.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourierEarningsService {

    private final DeliveryRepository deliveryRepository;
    private final PaymentSplitCalculator splitCalculator;
    private final SiteConfigurationRepository siteConfigurationRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Busca o histórico de recebimentos do courier.
     * Retorna apenas deliveries COMPLETED com pagamento PAID.
     *
     * @param courierId ID do courier
     * @param recent Se true, filtra apenas corridas recentes (deliveryHistoryDays da config)
     * @return Histórico de recebimentos com detalhamento por corrida
     */
    public CourierEarningsResponse getCourierEarnings(UUID courierId, Boolean recent) {
        log.info("🔍 Buscando recebimentos do courier: {} (recent={})", courierId, recent);

        // Buscar configuração de splits
        SiteConfiguration config = siteConfigurationRepository.findActiveConfiguration()
                .orElseThrow(() -> new RuntimeException("SiteConfiguration não encontrada"));

        // Buscar todas as deliveries COMPLETED do courier
        List<Delivery> completedDeliveries = deliveryRepository.findByCourierIdAndStatus(
                courierId,
                Delivery.DeliveryStatus.COMPLETED
        );

        // Filtrar por data se recent=true
        if (recent != null && recent) {
            int days = config.getDeliveryHistoryDays();
            java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(days);
            completedDeliveries = completedDeliveries.stream()
                    .filter(d -> d.getCompletedAt() != null && d.getCompletedAt().isAfter(cutoffDate))
                    .collect(Collectors.toList());
            log.info("   📅 Filtradas por recent=true (últimos {} dias da config): {} deliveries", days, completedDeliveries.size());
        }

        log.info("   📦 Total de deliveries completadas: {}", completedDeliveries.size());

        // Filtrar apenas as que têm pagamento PAID
        List<Delivery> paidDeliveries = completedDeliveries.stream()
                .filter(this::hasPaidPayment)
                .collect(Collectors.toList());

        log.info("   💰 Total de deliveries com pagamento PAID: {}", paidDeliveries.size());

        // Processar cada delivery e calcular splits
        List<CourierEarningsResponse.DeliveryEarningDetail> earningDetails = new ArrayList<>();
        BigDecimal totalEarnings = BigDecimal.ZERO;

        for (Delivery delivery : paidDeliveries) {
            CourierEarningsResponse.DeliveryEarningDetail detail = buildEarningDetail(delivery, config);
            earningDetails.add(detail);
            totalEarnings = totalEarnings.add(detail.getCourierAmount());
        }

        // Ordenar por data de conclusão (mais recente primeiro)
        earningDetails.sort((d1, d2) -> d2.getCompletedAt().compareTo(d1.getCompletedAt()));

        log.info("   ✅ Total ganho pelo courier: R$ {}", totalEarnings);

        return CourierEarningsResponse.builder()
                .totalDeliveries(paidDeliveries.size())
                .totalEarnings(totalEarnings)
                .deliveries(earningDetails)
                .build();
    }

    /**
     * Verifica se a delivery possui pelo menos um pagamento PAID
     */
    private boolean hasPaidPayment(Delivery delivery) {
        return delivery.getPayments() != null && delivery.getPayments().stream()
                .anyMatch(payment -> payment.getStatus() == PaymentStatus.PAID);
    }

    /**
     * Obtém o primeiro pagamento PAID da delivery
     */
    private Payment getPaidPayment(Delivery delivery) {
        return delivery.getPayments().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Pagamento PAID não encontrado para delivery " + delivery.getId()));
    }

    /**
     * Constrói o detalhamento de uma delivery com repartição de valores
     */
    private CourierEarningsResponse.DeliveryEarningDetail buildEarningDetail(Delivery delivery, SiteConfiguration config) {
        Payment payment = getPaidPayment(delivery);

        // Valores base
        BigDecimal shippingFee = delivery.getShippingFee(); // em reais
        BigDecimal shippingFeeCents = splitCalculator.toCents(shippingFee);

        // Verificar se há organizer
        boolean hasOrganizer = splitCalculator.hasValidOrganizer(delivery);

        // Calcular splits
        BigDecimal courierPercentage = splitCalculator.calculateCourierPercentage(config);
        BigDecimal organizerPercentage = hasOrganizer ? config.getOrganizerPercentage() : BigDecimal.ZERO;
        BigDecimal platformPercentage = splitCalculator.calculatePlatformPercentage(config, hasOrganizer);

        BigDecimal courierAmountCents = splitCalculator.calculateCourierAmount(shippingFeeCents, config);
        BigDecimal organizerAmountCents = hasOrganizer 
                ? splitCalculator.calculateOrganizerAmount(shippingFeeCents, config)
                : BigDecimal.ZERO;
        BigDecimal platformAmountCents = splitCalculator.calculatePlatformAmount(
                shippingFeeCents, 
                courierAmountCents, 
                organizerAmountCents
        );

        // Converter para reais
        BigDecimal courierAmount = splitCalculator.toReais(courierAmountCents, 2);
        BigDecimal organizerAmount = splitCalculator.toReais(organizerAmountCents, 2);
        BigDecimal platformAmount = splitCalculator.toReais(platformAmountCents, 2);

        // Nome do organizer (se houver)
        String organizerName = null;
        if (hasOrganizer) {
            User organizer = delivery.getOrganizer();
            organizerName = organizer != null ? organizer.getName() : null;
        }

        // Construir response
        return CourierEarningsResponse.DeliveryEarningDetail.builder()
                // Delivery info
                .deliveryId(delivery.getId())
                .completedAt(delivery.getCompletedAt() != null 
                        ? delivery.getCompletedAt().format(DATE_FORMATTER) 
                        : null)
                .fromAddress(delivery.getFromAddress())
                .toAddress(delivery.getToAddress())
                .distanceKm(delivery.getDistanceKm())
                .clientName(delivery.getClient() != null ? delivery.getClient().getName() : "N/A")
                .deliveryType(delivery.getDeliveryType() != null ? delivery.getDeliveryType().name() : "N/A")
                
                // Payment info
                .paymentId(payment.getId())
                .totalAmount(shippingFee)
                .paymentStatus(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "N/A")
                
                // Splits
                .courierAmount(courierAmount)
                .courierPercentage(courierPercentage)
                .organizerAmount(organizerAmount)
                .organizerPercentage(organizerPercentage)
                .organizerName(organizerName)
                .platformAmount(platformAmount)
                .platformPercentage(platformPercentage)
                .build();
    }
}
