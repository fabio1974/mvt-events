package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    /** Thread completo de um usuário, do mais antigo pro mais recente (ordem natural de chat). */
    List<SupportMessage> findByUserIdOrderByCreatedAtAsc(UUID userId);

    /**
     * Conta mensagens não lidas em uma direção específica.
     * <p>Para badge do usuário: {@code countUnread(userId, fromAdmin=true)} = quantas o admin
     * mandou que o user ainda não leu. Para o admin global: precisa somar por user, ver
     * {@link #countAdminUnreadConversations()}.
     */
    @Query("SELECT COUNT(m) FROM SupportMessage m " +
           "WHERE m.user.id = :userId AND m.fromAdmin = :fromAdmin AND m.readAt IS NULL")
    long countUnread(@Param("userId") UUID userId, @Param("fromAdmin") boolean fromAdmin);

    /** Marca como lidas todas as mensagens de uma direção pra um usuário. */
    @Modifying
    @Query("UPDATE SupportMessage m SET m.readAt = :now " +
           "WHERE m.user.id = :userId AND m.fromAdmin = :fromAdmin AND m.readAt IS NULL")
    int markAsRead(@Param("userId") UUID userId,
                   @Param("fromAdmin") boolean fromAdmin,
                   @Param("now") OffsetDateTime now);

    /**
     * Lista de conversas pra view do admin: 1 linha por usuário com mensagem,
     * incluindo última mensagem e count de não-lidas (do user → admin).
     * <p>Retorna projeção via JPQL constructor expression.
     */
    @Query("""
        SELECT new com.mvt.mvt_events.repository.SupportMessageRepository$ConversationSummary(
            u.id,
            u.name,
            u.username,
            CAST(u.role AS string),
            (SELECT MAX(m2.createdAt) FROM SupportMessage m2 WHERE m2.user.id = u.id),
            (SELECT m3.text FROM SupportMessage m3 WHERE m3.user.id = u.id
                AND m3.createdAt = (SELECT MAX(m4.createdAt) FROM SupportMessage m4 WHERE m4.user.id = u.id)),
            (SELECT COUNT(m5) FROM SupportMessage m5 WHERE m5.user.id = u.id
                AND m5.fromAdmin = false AND m5.readAt IS NULL),
            (SELECT MAX(m6.resolvedAt) FROM SupportMessage m6 WHERE m6.user.id = u.id)
        )
        FROM com.mvt.mvt_events.jpa.User u
        WHERE EXISTS (SELECT 1 FROM SupportMessage m WHERE m.user.id = u.id)
        ORDER BY (SELECT MAX(m7.createdAt) FROM SupportMessage m7 WHERE m7.user.id = u.id) DESC
        """)
    List<ConversationSummary> listConversationsForAdmin();

    /** Total de conversas com pelo menos 1 mensagem não-lida do user pro admin (badge admin). */
    @Query("SELECT COUNT(DISTINCT m.user.id) FROM SupportMessage m " +
           "WHERE m.fromAdmin = false AND m.readAt IS NULL")
    long countAdminUnreadConversations();

    /** Marca todas as mensagens user→admin de uma conversa como resolvidas. */
    @Modifying
    @Query("UPDATE SupportMessage m SET m.resolvedAt = :now " +
           "WHERE m.user.id = :userId AND m.resolvedAt IS NULL")
    int markConversationResolved(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    /** Limpa o flag de resolvida da conversa (reabre o thread). */
    @Modifying
    @Query("UPDATE SupportMessage m SET m.resolvedAt = NULL " +
           "WHERE m.user.id = :userId AND m.resolvedAt IS NOT NULL")
    int reopenConversation(@Param("userId") UUID userId);

    /** Indica se a conversa do usuário está atualmente marcada como resolvida. */
    @Query("SELECT (COUNT(m) > 0) FROM SupportMessage m " +
           "WHERE m.user.id = :userId AND m.resolvedAt IS NOT NULL")
    boolean isConversationResolved(@Param("userId") UUID userId);

    /** Última timestamp em que o BE disparou push pra esse destinatário (debounce 60s). */
    @Query("SELECT MAX(m.createdAt) FROM SupportMessage m " +
           "WHERE m.user.id = :userId AND m.fromAdmin = :fromAdmin")
    OffsetDateTime lastMessageAt(@Param("userId") UUID userId, @Param("fromAdmin") boolean fromAdmin);

    /**
     * DTO de projeção pra listagem do admin. Cada linha = 1 conversa.
     */
    record ConversationSummary(
        UUID userId,
        String userName,
        String userEmail,
        String userRole,
        OffsetDateTime lastMessageAt,
        String lastMessageText,
        long unreadCount,
        OffsetDateTime resolvedAt
    ) {}
}
