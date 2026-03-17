package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.OrganizerEarningsResponse;
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
 * Serviço para gerenciar recebimentos de organizers (empresas).
 * Lista todas as deliveries completadas com pagamento confirmado (PAID),
 * onde o organizer participou, mostrando o detalhamento da repartição de cada corrida.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizerEarningsService {

    private final DeliveryRepository deliveryRepository;
    private final PaymentSplitCalculator splitCalculator;
    private final SiteConfigurationRepository siteConfigurationRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Busca o histórico de recebimentos do organizer.
     * Retorna apenas deliveries COMPLETED com pagamento PAID onde o organizer participou.
     *
     * @param organizerId ID do organizer (user)
     * @param recent Se true, filtra apenas corridas recentes (deliveryHistoryDays da config)
     * @return Histórico de recebimentos com detalhamento por corrida
     */
    public OrganizerEarningsResponse getOrganizerEarnings(UUID organizerId, Boolean recent) {
        log.info("🔍 Buscando recebimentos do organizer: {} (recent={})", organizerId, recent);

        // Buscar configuração de splits
        SiteConfiguration config = siteConfigurationRepository.findActiveConfiguration()
                .orElseThrow(() -> new RuntimeException("SiteConfiguration não encontrada"));

        // Buscar todas as deliveries COMPLETED onde o organizer participou
        List<Delivery> completedDeliveries = deliveryRepository.findByOrganizerIdAndStatus(
                organizerId,
                Delivery.DeliveryStatus.COMPLETED
        );

        // Filtrar por data se recent=true
        if (recent != null && recent) {
            int days = config.getDeliveryHistoryDays();
            java.time.OffsetDateTime cutoffDate = java.time.OffsetDateTime.now(java.time.ZoneId.of("America/Fortaleza")).minusDays(days);
            completedDeliveries = completedDeliveries.stream()
                    .filter(d -> d.getCompletedAt() != null && d.getCompletedAt().isAfter(cutoffDate))
                    .collect(Collectors.toList());
            log.info("   📅 Filtradas por recent=true (últimos {} dias da config): {} deliveries", days, completedDeliveries.size());
        }

        log.info("   📦 Total de deliveries completadas com organizer: {}", completedDeliveries.size());

        // Filtrar apenas as que têm pagamento PAID
        List<Delivery> paidDeliveries = completedDeliveries.stream()
                .filter(this::hasPaidPayment)
                .collect(Collectors.toList());

        log.info("   💰 Total de deliveries com pagamento PAID: {}", paidDeliveries.size());

        // Processar cada delivery e calcular splits
        List<OrganizerEarningsResponse.DeliveryEarningDetail> earningDetails = new ArrayList<>();
        BigDecimal totalEarnings = BigDecimal.ZERO;

        for (Delivery delivery : paidDeliveries) {
            OrganizerEarningsResponse.DeliveryEarningDetail detail = buildEarningDetail(delivery, config);
            earningDetails.add(detail);
            totalEarnings = totalEarnings.add(detail.getOrganizerAmount());
        }

        // Ordenar por data de conclusão (mais recente primeiro)
        earningDetails.sort((d1, d2) -> d2.getCompletedAt().compareTo(d1.getCompletedAt()));

        log.info("   ✅ Total ganho pelo organizer: R$ {}", totalEarnings);

        return OrganizerEarningsResponse.builder()
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
    private OrganizerEarningsResponse.DeliveryEarningDetail buildEarningDetail(Delivery delivery, SiteConfiguration config) {
        Payment payment = getPaidPayment(delivery);

        // Valores base
        BigDecimal shippingFee = delivery.getShippingFee(); // em reais
        BigDecimal shippingFeeCents = splitCalculator.toCents(shippingFee);

        // Como estamos buscando deliveries do organizer, sempre tem organizer
        boolean hasOrganizer = true;

        // Calcular splits
        BigDecimal courierPercentage = splitCalculator.calculateCourierPercentage(config);
        BigDecimal organizerPercentage = config.getOrganizerPercentage(); // 5%
        BigDecimal platformPercentage = splitCalculator.calculatePlatformPercentage(config, hasOrganizer); // 8%

        BigDecimal courierAmountCents = splitCalculator.calculateCourierAmount(shippingFeeCents, config);
        BigDecimal organizerAmountCents = splitCalculator.calculateOrganizerAmount(shippingFeeCents, config);
        BigDecimal platformAmountCents = splitCalculator.calculatePlatformAmount(
                shippingFeeCents, 
                courierAmountCents, 
                organizerAmountCents
        );

        // Converter para reais
        BigDecimal courierAmount = splitCalculator.toReais(courierAmountCents, 2);
        BigDecimal organizerAmount = splitCalculator.toReais(organizerAmountCents, 2);
        BigDecimal platformAmount = splitCalculator.toReais(platformAmountCents, 2);

        // Nome do courier
        String courierName = null;
        if (delivery.getCourier() != null) {
            courierName = delivery.getCourier().getName();
        }

        // Construir response
        return OrganizerEarningsResponse.DeliveryEarningDetail.builder()
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
                .courierName(courierName)
                
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
                .platformAmount(platformAmount)
                .platformPercentage(platformPercentage)
                .build();
    }
}
