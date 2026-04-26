package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.SupportMessage;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.SupportMessageRepository;
import com.mvt.mvt_events.service.SupportMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chat de suporte (Fale Conosco) v1.0.
 *
 * <ul>
 *   <li>{@code /my-*} endpoints: usuário autenticado vê/envia em seu próprio thread</li>
 *   <li>{@code /conversations*} endpoints: ADMIN vê todos os threads</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/support")
@Tag(name = "Support", description = "Chat de suporte (Fale Conosco)")
@RequiredArgsConstructor
public class SupportController {

    private final SupportMessageService service;

    // ============================================================
    // USER endpoints — qualquer authenticated
    // ============================================================

    @GetMapping("/my-messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista as mensagens do thread do usuário logado")
    public List<MessageDto> myMessages(Authentication auth) {
        UUID userId = ((User) auth.getPrincipal()).getId();
        return service.listMyMessages(userId).stream().map(MessageDto::from).toList();
    }

    @PostMapping("/my-messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Envia uma mensagem do usuário pro suporte")
    public ResponseEntity<MessageDto> sendMyMessage(Authentication auth, @RequestBody SendMessageRequest body) {
        UUID userId = ((User) auth.getPrincipal()).getId();
        SupportMessage msg = service.sendFromUser(userId, body.text);
        return ResponseEntity.ok(MessageDto.from(msg));
    }

    @PostMapping("/my-messages/mark-read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marca todas as mensagens do admin como lidas")
    public Map<String, Object> markMyRead(Authentication auth) {
        UUID userId = ((User) auth.getPrincipal()).getId();
        int updated = service.markAdminMessagesRead(userId);
        return Map.of("markedAsRead", updated);
    }

    @GetMapping("/my-unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Quantas mensagens do admin o usuário ainda não leu (badge)")
    public Map<String, Long> myUnreadCount(Authentication auth) {
        UUID userId = ((User) auth.getPrincipal()).getId();
        return Map.of("count", service.countMyUnread(userId));
    }

    // ============================================================
    // ADMIN endpoints
    // ============================================================

    @GetMapping("/conversations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista de conversas (1 por usuário com mensagens)")
    public List<SupportMessageRepository.ConversationSummary> listConversations() {
        return service.listConversationsForAdmin();
    }

    @GetMapping("/conversations/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thread completo da conversa de um usuário específico")
    public List<MessageDto> getConversation(@PathVariable UUID userId) {
        return service.getConversation(userId).stream().map(MessageDto::from).toList();
    }

    @PostMapping("/conversations/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin responde no thread de um usuário")
    public ResponseEntity<MessageDto> replyToUser(@PathVariable UUID userId,
                                                   @RequestBody SendMessageRequest body) {
        SupportMessage msg = service.sendFromAdmin(userId, body.text);
        return ResponseEntity.ok(MessageDto.from(msg));
    }

    @PostMapping("/conversations/{userId}/mark-read")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marca as mensagens do usuário como lidas (admin abriu o thread)")
    public Map<String, Object> markUserRead(@PathVariable UUID userId) {
        int updated = service.markUserMessagesRead(userId);
        return Map.of("markedAsRead", updated);
    }

    @PostMapping("/conversations/{userId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marca a conversa como resolvida")
    public Map<String, String> resolve(@PathVariable UUID userId) {
        service.markConversationResolved(userId);
        return Map.of("status", "resolved");
    }

    @PostMapping("/conversations/{userId}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reabre conversa previamente marcada como resolvida")
    public Map<String, String> reopen(@PathVariable UUID userId) {
        service.reopenConversation(userId);
        return Map.of("status", "active");
    }

    @GetMapping("/admin-unread-count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Total de conversas com mensagens novas do user (badge admin)")
    public Map<String, Long> adminUnreadCount() {
        return Map.of("count", service.countAdminUnreadConversations());
    }

    // ============================================================
    // DTOs
    // ============================================================

    @Data
    public static class SendMessageRequest {
        public String text;
    }

    public record MessageDto(
            Long id,
            UUID userId,
            boolean fromAdmin,
            String text,
            OffsetDateTime createdAt,
            OffsetDateTime readAt,
            OffsetDateTime resolvedAt
    ) {
        static MessageDto from(SupportMessage m) {
            return new MessageDto(
                    m.getId(),
                    m.getUser() != null ? m.getUser().getId() : null,
                    Boolean.TRUE.equals(m.getFromAdmin()),
                    m.getText(),
                    m.getCreatedAt(),
                    m.getReadAt(),
                    m.getResolvedAt()
            );
        }
    }
}
