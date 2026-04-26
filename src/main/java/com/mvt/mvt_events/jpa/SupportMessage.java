package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Mensagem do chat de suporte (Fale Conosco) — v1.0 async sem IA.
 *
 * <p>Modelo: 1 thread por usuário. {@code user_id} aponta SEMPRE pro cliente final
 * (CLIENT/CUSTOMER/COURIER/ORGANIZER/WAITER) — o admin não tem thread próprio.
 * O campo {@code from_admin} discrimina quem escreveu cada mensagem do thread.
 */
@Entity
@Table(name = "support_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dono do thread — sempre o cliente final (não-admin). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    /** True se a mensagem foi escrita por um admin; false se foi pelo dono do thread. */
    @Column(name = "from_admin", nullable = false)
    private Boolean fromAdmin;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Timestamp em que o destinatário (oposto a fromAdmin) abriu/leu a mensagem. */
    @Column(name = "read_at")
    private OffsetDateTime readAt;

    /** Marca a CONVERSA como resolvida. Setado pelo admin via endpoint /resolve. */
    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (fromAdmin == null) {
            fromAdmin = false;
        }
    }
}
