package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialService {

    private final EventFinancialsRepository eventFinancialsRepository;
    private final TransferRepository transferRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final EventRepository eventRepository;

    private static final BigDecimal DEFAULT_PLATFORM_FEE_RATE = new BigDecimal("0.05"); // 5%
    private static final BigDecimal MINIMUM_TRANSFER_AMOUNT = new BigDecimal("10.00");

    /**
     * Process a payment and update event financials
     */
    @Transactional
    public void processPayment(Payment payment) {
        log.info("Processing payment {} for event {}", payment.getId(), payment.getRegistration().getEvent().getId());

        Event event = payment.getRegistration().getEvent();
        EventFinancials financials = getOrCreateEventFinancials(event);

        // Calculate platform fee
        BigDecimal platformFeeRate = event.getPlatformFeePercentage() != null ? event.getPlatformFeePercentage()
                : DEFAULT_PLATFORM_FEE_RATE;
        BigDecimal platformFee = payment.getAmount().multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = payment.getAmount().subtract(platformFee);

        // Update financials
        financials.setTotalRevenue(financials.getTotalRevenue().add(payment.getAmount()));
        financials.setPlatformFees(financials.getPlatformFees().add(platformFee));
        financials.setNetRevenue(financials.getNetRevenue().add(netAmount));
        financials.setPendingTransferAmount(financials.getPendingTransferAmount().add(netAmount));

        // Update payment count
        financials.setTotalPayments(financials.getTotalPayments() + 1);

        // Update next transfer date if needed
        updateNextTransferDate(financials);

        eventFinancialsRepository.save(financials);

        // Create payment event
        createPaymentEvent(event, payment, PaymentEvent.PaymentEventType.PAYMENT_RECEIVED,
                payment.getAmount(), "Payment received and processed");

        // Create platform fee event
        createPaymentEvent(event, payment, PaymentEvent.PaymentEventType.PLATFORM_FEE_CALCULATED,
                platformFee, "Platform fee calculated: " + platformFeeRate.multiply(new BigDecimal("100")) + "%");

        log.info("Payment processed. Event financials updated for event {}", event.getId());
    }

    /**
     * Process a refund and update event financials
     */
    @Transactional
    public void processRefund(Payment payment, BigDecimal refundAmount) {
        log.info("Processing refund of {} for payment {} in event {}",
                refundAmount, payment.getId(), payment.getRegistration().getEvent().getId());

        Event event = payment.getRegistration().getEvent();
        EventFinancials financials = getOrCreateEventFinancials(event);

        // Calculate platform fee to refund
        BigDecimal platformFeeRate = event.getPlatformFeePercentage() != null ? event.getPlatformFeePercentage()
                : DEFAULT_PLATFORM_FEE_RATE;
        BigDecimal platformFeeRefund = refundAmount.multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netRefund = refundAmount.subtract(platformFeeRefund);

        // Update financials (subtract amounts)
        financials.setTotalRevenue(financials.getTotalRevenue().subtract(refundAmount));
        financials.setPlatformFees(financials.getPlatformFees().subtract(platformFeeRefund));
        financials.setNetRevenue(financials.getNetRevenue().subtract(netRefund));
        financials.setPendingTransferAmount(financials.getPendingTransferAmount().subtract(netRefund));

        eventFinancialsRepository.save(financials);

        // Create refund event
        PaymentEvent.PaymentEventType eventType = refundAmount.equals(payment.getAmount())
                ? PaymentEvent.PaymentEventType.PAYMENT_REFUNDED
                : PaymentEvent.PaymentEventType.PAYMENT_PARTIALLY_REFUNDED;

        createPaymentEvent(event, payment, eventType, refundAmount.negate(),
                "Refund processed: " + refundAmount);

        log.info("Refund processed. Event financials updated for event {}", event.getId());
    }

    /**
     * Create a transfer for an event
     */
    @Transactional
    public Transfer createTransfer(Long eventId, BigDecimal amount, Transfer.TransferType transferType) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        EventFinancials financials = getOrCreateEventFinancials(event);

        if (amount.compareTo(financials.getPendingTransferAmount()) > 0) {
            throw new IllegalArgumentException("Transfer amount exceeds pending transfer amount");
        }

        if (amount.compareTo(MINIMUM_TRANSFER_AMOUNT) < 0) {
            throw new IllegalArgumentException("Transfer amount below minimum: " + MINIMUM_TRANSFER_AMOUNT);
        }

        // Create transfer
        Transfer transfer = new Transfer();
        transfer.setEvent(event);
        transfer.setOrganization(event.getOrganization());
        transfer.setAmount(amount);
        transfer.setTransferType(transferType);
        transfer.setTransferMethod(Transfer.TransferMethod.PIX); // Default to PIX
        transfer.setStatus(Transfer.TransferStatus.PENDING);

        Transfer savedTransfer = transferRepository.save(transfer);

        // Update financials
        financials.setPendingTransferAmount(financials.getPendingTransferAmount().subtract(amount));
        financials.setLastTransferDate(LocalDateTime.now());
        updateNextTransferDate(financials);
        eventFinancialsRepository.save(financials);

        // Create transfer event
        createPaymentEvent(event, null, PaymentEvent.PaymentEventType.TRANSFER_INITIATED,
                amount.negate(), "Transfer initiated: " + savedTransfer.getId());

        log.info("Transfer created: {} for amount {} in event {}", savedTransfer.getId(), amount, eventId);
        return savedTransfer;
    }

    /**
     * Mark transfer as completed
     */
    @Transactional
    public void completeTransfer(Long transferId, String gatewayTransferId, BigDecimal gatewayFee) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        transfer.setStatus(Transfer.TransferStatus.COMPLETED);
        transfer.setGatewayTransferId(gatewayTransferId);
        transfer.setGatewayFee(gatewayFee);
        transfer.setCompletedAt(LocalDateTime.now());

        transferRepository.save(transfer);

        // Update event financials
        EventFinancials financials = getOrCreateEventFinancials(transfer.getEvent());
        financials.setTransferredAmount(financials.getTransferredAmount().add(transfer.getAmount()));
        financials.setTotalTransferFees(financials.getTotalTransferFees().add(gatewayFee));
        eventFinancialsRepository.save(financials);

        // Create completion event
        createPaymentEvent(transfer.getEvent(), null, PaymentEvent.PaymentEventType.TRANSFER_COMPLETED,
                transfer.getAmount().negate(), "Transfer completed: " + transferId);

        log.info("Transfer completed: {}", transferId);
    }

    /**
     * Mark transfer as failed
     */
    @Transactional
    public void failTransfer(Long transferId, String failureReason) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        transfer.setStatus(Transfer.TransferStatus.FAILED);
        transfer.setFailureReason(failureReason);
        transfer.setFailedAt(LocalDateTime.now());
        transfer.setRetryCount(transfer.getRetryCount() + 1);

        transferRepository.save(transfer);

        // Return amount to pending transfers
        EventFinancials financials = getOrCreateEventFinancials(transfer.getEvent());
        financials.setPendingTransferAmount(financials.getPendingTransferAmount().add(transfer.getAmount()));
        eventFinancialsRepository.save(financials);

        // Create failure event
        createPaymentEvent(transfer.getEvent(), null, PaymentEvent.PaymentEventType.TRANSFER_FAILED,
                BigDecimal.ZERO, "Transfer failed: " + failureReason);

        log.warn("Transfer failed: {} - {}", transferId, failureReason);
    }

    /**
     * Get events ready for automatic transfer
     */
    public List<EventFinancials> getEventsReadyForTransfer() {
        return eventFinancialsRepository.findDueForTransfer(LocalDateTime.now());
    }

    /**
     * Get or create event financials
     */
    private EventFinancials getOrCreateEventFinancials(Event event) {
        return eventFinancialsRepository.findByEventId(event.getId())
                .orElseGet(() -> {
                    EventFinancials financials = new EventFinancials();
                    financials.setEvent(event);
                    financials.setTransferFrequency(event.getTransferFrequency() != null ? event.getTransferFrequency()
                            : TransferFrequency.WEEKLY);
                    updateNextTransferDate(financials);
                    return eventFinancialsRepository.save(financials);
                });
    }

    /**
     * Update next transfer date based on frequency
     */
    private void updateNextTransferDate(EventFinancials financials) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextDate;

        switch (financials.getTransferFrequency()) {
            case IMMEDIATE:
                nextDate = now;
                break;
            case DAILY:
                nextDate = now.plusDays(1);
                break;
            case WEEKLY:
                nextDate = now.plusWeeks(1);
                break;
            case MONTHLY:
                nextDate = now.plusMonths(1);
                break;
            default:
                nextDate = now.plusWeeks(1); // Default to weekly
        }

        financials.setNextTransferDate(nextDate);
    }

    /**
     * Create a payment event
     */
    private void createPaymentEvent(Event event, Payment payment, PaymentEvent.PaymentEventType eventType,
            BigDecimal amount, String description) {
        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setEvent(event);
        paymentEvent.setPayment(payment);
        paymentEvent.setEventType(eventType);
        paymentEvent.setAmount(amount);
        paymentEvent.setDescription(description);
        paymentEvent.setProcessedBy("system");

        paymentEventRepository.save(paymentEvent);
    }

    /**
     * Get financial summary for an event
     */
    public Map<String, Object> getEventFinancialSummary(Long eventId) {
        Optional<EventFinancials> financialsOpt = eventFinancialsRepository.findByEventId(eventId);
        if (financialsOpt.isEmpty()) {
            return Map.of("message", "No financial data available for this event");
        }

        EventFinancials financials = financialsOpt.get();
        Map<String, Object> summary = new HashMap<>();

        summary.put("totalRevenue", financials.getTotalRevenue());
        summary.put("platformFees", financials.getPlatformFees());
        summary.put("netRevenue", financials.getNetRevenue());
        summary.put("transferredAmount", financials.getTransferredAmount());
        summary.put("pendingTransferAmount", financials.getPendingTransferAmount());
        summary.put("totalTransferFees", financials.getTotalTransferFees());
        summary.put("totalPayments", financials.getTotalPayments());
        summary.put("transferFrequency", financials.getTransferFrequency());
        summary.put("lastTransferDate", financials.getLastTransferDate());
        summary.put("nextTransferDate", financials.getNextTransferDate());

        return summary;
    }
}