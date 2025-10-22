package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.*;
import com.mvt.mvt_events.specification.UnifiedPayoutSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service para UnifiedPayout
 * Gerenciamento de repasses periódicos consolidados
 */
@Service
@Transactional
public class UnifiedPayoutService {

    @Autowired
    private UnifiedPayoutRepository payoutRepository;

    @Autowired
    private PayoutItemRepository payoutItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourierProfileRepository courierProfileRepository;

    @Autowired
    private ADMProfileRepository admProfileRepository;

    /**
     * Cria payout manual
     */
    public UnifiedPayout create(UnifiedPayout payout, UUID beneficiaryId) {
        // Validar beneficiário
        User beneficiary = userRepository.findById(beneficiaryId)
                .orElseThrow(() -> new RuntimeException("Beneficiário não encontrado"));

        // Validar tipo do beneficiário
        if (payout.getBeneficiaryType() == UnifiedPayout.BeneficiaryType.COURIER &&
                beneficiary.getRole() != User.Role.COURIER) {
            throw new RuntimeException("Beneficiário não é um courier");
        }

        if (payout.getBeneficiaryType() == UnifiedPayout.BeneficiaryType.ADM &&
                beneficiary.getRole() != User.Role.ORGANIZER) {
            throw new RuntimeException("Beneficiário não é um ORGANIZER");
        }

        // Validar formato do período
        if (!payout.getPeriod().matches("\\d{4}-\\d{2}")) {
            throw new RuntimeException("Período deve estar no formato YYYY-MM");
        }

        // Verificar se já existe payout para o período
        if (payoutRepository.existsByBeneficiaryIdAndPeriod(beneficiaryId, payout.getPeriod())) {
            throw new RuntimeException("Já existe payout para este beneficiário no período informado");
        }

        payout.setBeneficiary(beneficiary);
        payout.setStatus(UnifiedPayout.PayoutStatus.PENDING);
        payout.setItemCount(0);

        if (payout.getTotalAmount() == null) {
            payout.setTotalAmount(BigDecimal.ZERO);
        }

        return payoutRepository.save(payout);
    }

    /**
     * Gera payouts automáticos para um período
     * Agrupa payments completados e cria payouts para couriers e ADMs
     */
    public void generatePayoutsForPeriod(String period) {
        // Validar formato do período
        if (!period.matches("\\d{4}-\\d{2}")) {
            throw new RuntimeException("Período deve estar no formato YYYY-MM");
        }

        YearMonth yearMonth = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Buscar payments não incluídos em payouts
        List<Long> paymentIds = payoutItemRepository.findPaymentIdsNotInAnyPayout();

        // Processar cada payment e agrupar por beneficiário
        // TODO: Implementar lógica completa de agrupamento

        // Por enquanto, apenas marcar como processado
        // A lógica completa requer definir regras de split de pagamento
    }

    /**
     * Busca payout por ID
     */
    public UnifiedPayout findById(Long id) {
        return payoutRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payout não encontrado"));
    }

    /**
     * Lista payouts com filtros
     */
    public Page<UnifiedPayout> findAll(UUID beneficiaryId,
            UnifiedPayout.BeneficiaryType beneficiaryType,
            String period,
            UnifiedPayout.PayoutStatus status,
            Pageable pageable) {
        Specification<UnifiedPayout> spec = UnifiedPayoutSpecification.hasBeneficiaryId(beneficiaryId)
                .and(UnifiedPayoutSpecification.hasBeneficiaryType(beneficiaryType))
                .and(UnifiedPayoutSpecification.hasPeriod(period))
                .and(UnifiedPayoutSpecification.hasStatus(status));

        return payoutRepository.findAll(spec, pageable);
    }

    /**
     * Busca payouts de um beneficiário
     */
    public List<UnifiedPayout> findByBeneficiary(UUID beneficiaryId) {
        return payoutRepository.findByBeneficiaryIdOrderByPeriodDesc(beneficiaryId);
    }

    /**
     * Busca payouts pendentes de um período
     */
    public List<UnifiedPayout> findPendingByPeriod(String period) {
        return payoutRepository.findPendingByPeriod(period);
    }

    /**
     * Busca payouts de couriers em um período
     */
    public List<UnifiedPayout> findCourierPayoutsByPeriod(String period) {
        return payoutRepository.findCourierPayoutsByPeriod(period);
    }

    /**
     * Busca payouts de ADMs em um período
     */
    public List<UnifiedPayout> findAdmPayoutsByPeriod(String period) {
        return payoutRepository.findAdmPayoutsByPeriod(period);
    }

    /**
     * Atualiza status do payout
     */
    public UnifiedPayout updateStatus(Long id, UnifiedPayout.PayoutStatus status) {
        UnifiedPayout payout = findById(id);
        payout.setStatus(status);

        if (status == UnifiedPayout.PayoutStatus.COMPLETED) {
            payout.setPaidAt(LocalDateTime.now());
        }

        return payoutRepository.save(payout);
    }

    /**
     * Processa payout (marca como pago)
     */
    public UnifiedPayout processPayout(Long id, UnifiedPayout.PayoutMethod paymentMethod,
            String paymentReference) {
        UnifiedPayout payout = findById(id);

        if (payout.getStatus() != UnifiedPayout.PayoutStatus.PENDING) {
            throw new RuntimeException("Apenas payouts pendentes podem ser processados");
        }

        payout.setStatus(UnifiedPayout.PayoutStatus.PROCESSING);
        payout.setPaymentMethod(paymentMethod);
        payout.setPaymentReference(paymentReference);

        return payoutRepository.save(payout);
    }

    /**
     * Completa payout
     */
    public UnifiedPayout completePayout(Long id) {
        UnifiedPayout payout = findById(id);

        if (payout.getStatus() != UnifiedPayout.PayoutStatus.PROCESSING) {
            throw new RuntimeException("Apenas payouts em processamento podem ser completados");
        }

        payout.setStatus(UnifiedPayout.PayoutStatus.COMPLETED);
        payout.setPaidAt(LocalDateTime.now());

        return payoutRepository.save(payout);
    }

    /**
     * Cancela payout
     */
    public UnifiedPayout cancelPayout(Long id, String reason) {
        UnifiedPayout payout = findById(id);

        if (payout.getStatus() == UnifiedPayout.PayoutStatus.COMPLETED) {
            throw new RuntimeException("Não é possível cancelar payout completado");
        }

        payout.setStatus(UnifiedPayout.PayoutStatus.CANCELLED);
        payout.setNotes(reason);

        return payoutRepository.save(payout);
    }

    /**
     * Adiciona item ao payout
     */
    public PayoutItem addItem(Long payoutId, Long paymentId, BigDecimal itemValue,
            PayoutItem.ValueType valueType) {
        UnifiedPayout payout = findById(payoutId);

        if (payout.getStatus() != UnifiedPayout.PayoutStatus.PENDING) {
            throw new RuntimeException("Apenas payouts pendentes podem receber items");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment não encontrado"));

        // Verificar se item já existe
        if (payoutItemRepository.findByPayoutIdAndPaymentId(payoutId, paymentId).isPresent()) {
            throw new RuntimeException("Item já existe no payout");
        }

        PayoutItem item = new PayoutItem();
        item.setPayout(payout);
        item.setPayment(payment);
        item.setItemValue(itemValue);
        item.setValueType(valueType);

        PayoutItem saved = payoutItemRepository.save(item);

        // Atualizar totais do payout
        payout.setTotalAmount(payout.getTotalAmount().add(itemValue));
        payout.setItemCount(payout.getItemCount() + 1);
        payoutRepository.save(payout);

        return saved;
    }

    /**
     * Busca items de um payout
     */
    public List<PayoutItem> findItemsByPayout(Long payoutId) {
        return payoutItemRepository.findByPayoutIdOrderByCreatedAtAsc(payoutId);
    }

    /**
     * Calcula total pago em um período
     */
    public Double getTotalPaidByPeriod(String period) {
        Double total = payoutRepository.sumPaidAmountByPeriod(period);
        return total != null ? total : 0.0;
    }
}
