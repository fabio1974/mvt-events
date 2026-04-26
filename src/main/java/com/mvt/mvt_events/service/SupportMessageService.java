package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.SupportMessage;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.SupportMessageRepository;
import com.mvt.mvt_events.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço do chat de suporte (Fale Conosco) v1.0 — async, sem IA.
 *
 * <p>Gera push notifications ao destinatário com:
 * <ul>
 *   <li>{@code collapseKey: "support_user_<userId>"} → OS agrupa notificações</li>
 *   <li>Debounce de {@link #PUSH_DEBOUNCE_SECONDS}s — não dispara push se a anterior pra
 *       o mesmo destinatário foi enviada há menos disso (a notificação já visível
 *       é atualizada pelo collapseKey de qualquer jeito quando o app abrir)</li>
 *   <li>Body com count de não-lidas se houver mais de 1 acumulada</li>
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SupportMessageService {

    /** Janela em segundos pra suprimir pushs repetidos pro mesmo destinatário. */
    private static final long PUSH_DEBOUNCE_SECONDS = 60;

    /** Tamanho máximo do preview da mensagem no body do push. */
    private static final int PUSH_PREVIEW_LEN = 100;

    private final SupportMessageRepository repository;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    // ===== USER (dono do thread) =====

    /** Lista o thread completo do usuário logado. */
    public List<SupportMessage> listMyMessages(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    /**
     * Usuário envia mensagem (from_admin=false). Dispara push pros admins.
     * <p>Se a conversa estava marcada como resolvida, reabre automaticamente
     * (clear resolved_at) antes de persistir — usuário voltando à carga = caso ativo de novo.
     */
    public SupportMessage sendFromUser(UUID userId, String text) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + userId));
        validateText(text);

        // Auto-reabertura: nova mensagem do usuário invalida o estado "resolvida".
        repository.reopenConversation(userId);

        SupportMessage saved = persist(user, false, text);
        notifyAdmins(user, text);
        return saved;
    }

    /** Marca como lidas as mensagens do admin no thread do usuário. */
    public int markAdminMessagesRead(UUID userId) {
        return repository.markAsRead(userId, true, OffsetDateTime.now());
    }

    /** Quantas mensagens do admin o usuário ainda não leu (badge no app dele). */
    public long countMyUnread(UUID userId) {
        return repository.countUnread(userId, true);
    }

    // ===== ADMIN =====

    /** Lista de conversas pra view do admin. */
    public List<SupportMessageRepository.ConversationSummary> listConversationsForAdmin() {
        return repository.listConversationsForAdmin();
    }

    /** Thread completo de uma conversa específica (pelo userId do dono). */
    public List<SupportMessage> getConversation(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    /** Admin responde no thread de um usuário. Dispara push pro usuário. */
    public SupportMessage sendFromAdmin(UUID userId, String text) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário do thread não encontrado: " + userId));
        validateText(text);

        SupportMessage saved = persist(user, true, text);
        notifyUser(user, text);
        return saved;
    }

    /** Marca como lidas as mensagens do usuário no thread (admin abriu a conversa). */
    public int markUserMessagesRead(UUID userId) {
        return repository.markAsRead(userId, false, OffsetDateTime.now());
    }

    /** Conta de conversas com mensagens não lidas do user pro admin (badge admin). */
    public long countAdminUnreadConversations() {
        return repository.countAdminUnreadConversations();
    }

    /** Marca a conversa como resolvida (apenas admin). */
    public void markConversationResolved(UUID userId) {
        repository.markConversationResolved(userId, OffsetDateTime.now());
    }

    /** Reabre conversa marcada como resolvida (ação manual do admin). */
    public void reopenConversation(UUID userId) {
        repository.reopenConversation(userId);
    }

    /** True se a conversa do usuário está atualmente marcada como resolvida. */
    public boolean isConversationResolved(UUID userId) {
        return repository.isConversationResolved(userId);
    }

    // ===== Helpers =====

    private void validateText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("Mensagem vazia");
        }
        if (text.length() > 4000) {
            throw new RuntimeException("Mensagem muito longa (máx 4000 caracteres)");
        }
    }

    private SupportMessage persist(User user, boolean fromAdmin, String text) {
        SupportMessage msg = SupportMessage.builder()
                .user(user)
                .fromAdmin(fromAdmin)
                .text(text.trim())
                .build();
        return repository.save(msg);
    }

    /**
     * Push pros admins ativos. Aplica debounce de 60s — se já mandamos um push
     * em < 60s, OS já tem a notificação anterior visível (collapseKey substituirá
     * automaticamente quando admin abrir o app).
     */
    private void notifyAdmins(User user, String latestText) {
        if (shouldDebounce(user.getId(), false)) {
            log.debug("⏭️  Push pro admin debouncado (msg do user {} em < {}s)", user.getId(), PUSH_DEBOUNCE_SECONDS);
            return;
        }

        long unread = repository.countUnread(user.getId(), false);
        String body = formatBody(latestText, unread);
        String title = "Nova mensagem de " + (user.getName() != null ? user.getName() : "usuário");

        Map<String, Object> data = new HashMap<>();
        data.put("type", "support_message");
        data.put("userId", user.getId().toString());
        data.put("collapseKey", "support_user_" + user.getId());

        for (User admin : userRepository.findActiveAdmins()) {
            try {
                pushNotificationService.sendHybridNotificationToUser(admin.getId(), title, body, data);
            } catch (Exception e) {
                log.warn("Falha ao enviar push pro admin {}: {}", admin.getId(), e.getMessage());
            }
        }
    }

    /** Push pro dono do thread (usuário). Mesmo padrão de debounce + collapseKey. */
    private void notifyUser(User user, String latestText) {
        if (shouldDebounce(user.getId(), true)) {
            log.debug("⏭️  Push pro user {} debouncado (resposta do admin em < {}s)", user.getId(), PUSH_DEBOUNCE_SECONDS);
            return;
        }

        long unread = repository.countUnread(user.getId(), true);
        String body = formatBody(latestText, unread);

        Map<String, Object> data = new HashMap<>();
        data.put("type", "support_message");
        data.put("collapseKey", "support_user_" + user.getId());

        try {
            pushNotificationService.sendHybridNotificationToUser(user.getId(), "Suporte Zapi10", body, data);
        } catch (Exception e) {
            log.warn("Falha ao enviar push pro user {}: {}", user.getId(), e.getMessage());
        }
    }

    /**
     * @return true se houve uma mensagem na MESMA direção há menos de PUSH_DEBOUNCE_SECONDS.
     *         A direção é: from_admin=true significa "mandando pro user"; from_admin=false
     *         significa "mandando pro admin". O método checa a última msg dessa direção.
     */
    private boolean shouldDebounce(UUID userId, boolean fromAdmin) {
        OffsetDateTime last = repository.lastMessageAt(userId, fromAdmin);
        if (last == null) return false;
        // last é a mensagem que ACABAMOS de salvar — usar penúltima exigiria query mais cara.
        // Truque: comparar com (now - 1s) pra excluir a própria mensagem que disparou esse push.
        OffsetDateTime windowStart = OffsetDateTime.now().minusSeconds(PUSH_DEBOUNCE_SECONDS);
        // Se houver QUALQUER mensagem nessa direção entre [windowStart, now-1s], debounce.
        return last.isBefore(OffsetDateTime.now().minus(1, ChronoUnit.SECONDS))
                && last.isAfter(windowStart);
    }

    private String formatBody(String latestText, long unreadCount) {
        String preview = latestText.length() > PUSH_PREVIEW_LEN
                ? latestText.substring(0, PUSH_PREVIEW_LEN) + "..."
                : latestText;
        if (unreadCount > 1) {
            return String.format("(%d) %s", unreadCount, preview);
        }
        return preview;
    }
}
