package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.PagarmeTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PagarmeTransferRepository extends JpaRepository<PagarmeTransfer, Long> {
    List<PagarmeTransfer> findByFoodOrderId(Long foodOrderId);
    List<PagarmeTransfer> findByStatus(PagarmeTransfer.Status status);

    @Query("SELECT t FROM PagarmeTransfer t " +
           "LEFT JOIN FETCH t.recipient r " +
           "LEFT JOIN FETCH t.foodOrder f " +
           "WHERE t.status = :status " +
           "ORDER BY r.id, t.createdAt")
    List<PagarmeTransfer> findByStatusWithRecipient(@Param("status") PagarmeTransfer.Status status);

    /** Transfers de um destinatário específico, em algum status, com FoodOrder carregado. */
    @Query("SELECT t FROM PagarmeTransfer t " +
           "LEFT JOIN FETCH t.foodOrder f " +
           "WHERE t.recipient.id = :recipientId AND t.status = :status " +
           "ORDER BY t.createdAt DESC")
    List<PagarmeTransfer> findByRecipientIdAndStatus(@Param("recipientId") UUID recipientId,
                                                     @Param("status") PagarmeTransfer.Status status);
}
