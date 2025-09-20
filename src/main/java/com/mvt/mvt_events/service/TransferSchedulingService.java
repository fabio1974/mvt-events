package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.EventFinancials;
import com.mvt.mvt_events.jpa.Transfer;
import com.mvt.mvt_events.jpa.TransferFrequency;
import com.mvt.mvt_events.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferSchedulingService {

    private final FinancialService financialService;
    private final TransferRepository transferRepository;
    private final PaymentGatewayService paymentGatewayService;

    private static final BigDecimal MINIMUM_TRANSFER_AMOUNT = new BigDecimal("10.00");

    /**
     * Process automatic transfers every hour
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void processAutomaticTransfers() {
        log.info("Starting automatic transfer processing");

        List<EventFinancials> eventsReadyForTransfer = financialService.getEventsReadyForTransfer();

        for (EventFinancials financials : eventsReadyForTransfer) {
            try {
                processEventTransfer(financials);
            } catch (Exception e) {
                log.error("Error processing transfer for event {}: {}",
                        financials.getEvent().getId(), e.getMessage(), e);
            }
        }

        log.info("Automatic transfer processing completed. Processed {} events", eventsReadyForTransfer.size());
    }

    /**
     * Retry failed transfers every 4 hours
     */
    @Scheduled(fixedRate = 14400000) // Every 4 hours
    @Transactional
    public void retryFailedTransfers() {
        log.info("Starting failed transfer retry process");

        List<Transfer> failedTransfers = transferRepository.findFailedTransfersForRetry();

        for (Transfer transfer : failedTransfers) {
            try {
                if (transfer.canRetry()) {
                    retryTransfer(transfer);
                }
            } catch (Exception e) {
                log.error("Error retrying transfer {}: {}", transfer.getId(), e.getMessage(), e);
            }
        }

        log.info("Failed transfer retry process completed. Attempted {} transfers", failedTransfers.size());
    }

    /**
     * Process pending transfers every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // Every 30 minutes
    @Transactional
    public void processPendingTransfers() {
        log.info("Processing pending transfers");

        List<Transfer> pendingTransfers = transferRepository.findByStatus(Transfer.TransferStatus.PENDING);

        for (Transfer transfer : pendingTransfers) {
            try {
                executeTransfer(transfer);
            } catch (Exception e) {
                log.error("Error executing transfer {}: {}", transfer.getId(), e.getMessage(), e);
                financialService.failTransfer(transfer.getId(), "Execution error: " + e.getMessage());
            }
        }

        log.info("Pending transfer processing completed. Processed {} transfers", pendingTransfers.size());
    }

    /**
     * Process transfer for a specific event
     */
    private void processEventTransfer(EventFinancials financials) {
        if (financials.getPendingTransferAmount().compareTo(MINIMUM_TRANSFER_AMOUNT) < 0) {
            log.debug("Pending amount {} below minimum for event {}",
                    financials.getPendingTransferAmount(), financials.getEvent().getId());
            return;
        }

        // Only process immediate and scheduled transfers automatically
        if (financials.getTransferFrequency() == TransferFrequency.ON_DEMAND) {
            log.debug("Event {} has on-demand transfer frequency, skipping automatic processing",
                    financials.getEvent().getId());
            return;
        }

        try {
            Transfer transfer = financialService.createTransfer(
                    financials.getEvent().getId(),
                    financials.getPendingTransferAmount(),
                    Transfer.TransferType.AUTOMATIC);

            log.info("Created automatic transfer {} for event {} with amount {}",
                    transfer.getId(), financials.getEvent().getId(), transfer.getAmount());

        } catch (Exception e) {
            log.error("Failed to create automatic transfer for event {}: {}",
                    financials.getEvent().getId(), e.getMessage(), e);
        }
    }

    /**
     * Execute a transfer through the payment gateway
     */
    private void executeTransfer(Transfer transfer) {
        log.info("Executing transfer {}", transfer.getId());

        // Update status to processing
        transfer.setStatus(Transfer.TransferStatus.PROCESSING);
        transfer.setProcessedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        try {
            // Execute through payment gateway
            String gatewayTransferId = paymentGatewayService.executeTransfer(transfer);
            BigDecimal gatewayFee = paymentGatewayService.getTransferFee(transfer.getAmount());

            // Mark as completed
            financialService.completeTransfer(transfer.getId(), gatewayTransferId, gatewayFee);

            log.info("Transfer {} executed successfully with gateway ID {}",
                    transfer.getId(), gatewayTransferId);

        } catch (Exception e) {
            log.error("Transfer {} execution failed: {}", transfer.getId(), e.getMessage(), e);
            financialService.failTransfer(transfer.getId(), e.getMessage());
        }
    }

    /**
     * Retry a failed transfer
     */
    private void retryTransfer(Transfer transfer) {
        log.info("Retrying failed transfer {} (attempt {})", transfer.getId(), transfer.getRetryCount() + 1);

        // Reset status to pending for retry
        transfer.setStatus(Transfer.TransferStatus.PENDING);
        transfer.setFailureReason(null);
        transfer.setFailedAt(null);
        transferRepository.save(transfer);

        // The transfer will be picked up by the next pending transfer processing cycle
    }

    /**
     * Get transfer statistics
     */
    public TransferStats getTransferStats() {
        long pendingCount = transferRepository.countPendingAutomaticTransfers();
        BigDecimal totalTransferred = transferRepository.getTotalTransferredByOrganization(null); // This needs to be
                                                                                                  // fixed in repo

        return new TransferStats(pendingCount, totalTransferred);
    }

    /**
     * Transfer statistics record
     */
    public record TransferStats(long pendingTransfers, BigDecimal totalTransferred) {
    }
}