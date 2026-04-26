package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.SupportMessage;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.SupportMessageRepository;
import com.mvt.mvt_events.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre as regras core do chat de suporte: validação de mensagem, persistência,
 * notificação ao destinatário correto e debounce de push.
 */
@ExtendWith(MockitoExtension.class)
class SupportMessageServiceTest {

    @Mock private SupportMessageRepository repository;
    @Mock private UserRepository userRepository;
    @Mock private PushNotificationService pushNotificationService;

    @InjectMocks
    private SupportMessageService service;

    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private User makeUser(UUID id, String name, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setUsername(name.toLowerCase() + "@test.com");
        u.setRole(role);
        return u;
    }

    @Nested
    @DisplayName("Validação de mensagem")
    class ValidationTests {

        @Test
        @DisplayName("Mensagem vazia lança exceção")
        void emptyText() {
            User u = makeUser(userId, "Maria", User.Role.CLIENT);
            when(userRepository.findById(userId)).thenReturn(Optional.of(u));

            assertThatThrownBy(() -> service.sendFromUser(userId, "  "))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("vazia");
        }

        @Test
        @DisplayName("Mensagem maior que 4000 chars lança exceção")
        void tooLong() {
            User u = makeUser(userId, "Maria", User.Role.CLIENT);
            when(userRepository.findById(userId)).thenReturn(Optional.of(u));

            String huge = "x".repeat(4001);
            assertThatThrownBy(() -> service.sendFromUser(userId, huge))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("longa");
        }

        @Test
        @DisplayName("Usuário inexistente lança exceção")
        void unknownUser() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.sendFromUser(userId, "oi"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrado");
        }
    }

    @Nested
    @DisplayName("sendFromUser — usuário envia → notifica admins")
    class SendFromUserTests {

        @Test
        @DisplayName("Persiste com fromAdmin=false e dispara push pros admins")
        void persistsAndNotifiesAdmins() {
            User user = makeUser(userId, "Maria", User.Role.CLIENT);
            User admin = makeUser(adminId, "Admin", User.Role.ADMIN);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(repository.save(any(SupportMessage.class))).thenAnswer(inv -> inv.getArgument(0));
            when(repository.lastMessageAt(userId, false)).thenReturn(null); // sem msg anterior → não debounca
            when(repository.countUnread(userId, false)).thenReturn(1L);
            when(userRepository.findActiveAdmins()).thenReturn(List.of(admin));

            SupportMessage saved = service.sendFromUser(userId, "preciso de ajuda");

            ArgumentCaptor<SupportMessage> captor = ArgumentCaptor.forClass(SupportMessage.class);
            verify(repository).save(captor.capture());
            SupportMessage persisted = captor.getValue();
            assertThat(persisted.getUser()).isEqualTo(user);
            assertThat(persisted.getFromAdmin()).isFalse();
            assertThat(persisted.getText()).isEqualTo("preciso de ajuda");

            verify(pushNotificationService).sendHybridNotificationToUser(eq(adminId), anyString(), anyString(), anyMap());
            assertThat(saved.getText()).isEqualTo("preciso de ajuda");
        }

        @Test
        @DisplayName("Não envia push se a última mensagem foi há < 60s (debounce)")
        void debouncesRecentPush() {
            User user = makeUser(userId, "Maria", User.Role.CLIENT);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(repository.save(any(SupportMessage.class))).thenAnswer(inv -> inv.getArgument(0));
            // Última mensagem foi há 10s — dentro da janela do debounce (60s) e mais de 1s atrás
            when(repository.lastMessageAt(userId, false))
                    .thenReturn(OffsetDateTime.now().minusSeconds(10));

            service.sendFromUser(userId, "outra mensagem");

            verify(pushNotificationService, never()).sendHybridNotificationToUser(any(), anyString(), anyString(), anyMap());
        }
    }

    @Nested
    @DisplayName("sendFromAdmin — admin responde → notifica user")
    class SendFromAdminTests {

        @Test
        @DisplayName("Persiste com fromAdmin=true e notifica o dono do thread")
        void persistsAndNotifiesUser() {
            User user = makeUser(userId, "Maria", User.Role.CLIENT);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(repository.save(any(SupportMessage.class))).thenAnswer(inv -> inv.getArgument(0));
            when(repository.lastMessageAt(userId, true)).thenReturn(null);
            when(repository.countUnread(userId, true)).thenReturn(1L);

            service.sendFromAdmin(userId, "vou te ajudar");

            ArgumentCaptor<SupportMessage> captor = ArgumentCaptor.forClass(SupportMessage.class);
            verify(repository).save(captor.capture());
            SupportMessage persisted = captor.getValue();
            assertThat(persisted.getFromAdmin()).isTrue();

            verify(pushNotificationService).sendHybridNotificationToUser(eq(userId), eq("Suporte Zapi10"), anyString(), anyMap());
        }
    }

    @Nested
    @DisplayName("Body do push com count de não-lidas")
    class PushBodyTests {

        @Test
        @DisplayName("Quando count=1, body é só a última mensagem")
        void singleMessageBody() {
            User user = makeUser(userId, "Maria", User.Role.CLIENT);
            User admin = makeUser(adminId, "Admin", User.Role.ADMIN);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(repository.save(any(SupportMessage.class))).thenAnswer(inv -> inv.getArgument(0));
            lenient().when(repository.lastMessageAt(userId, false)).thenReturn(null);
            when(repository.countUnread(userId, false)).thenReturn(1L);
            when(userRepository.findActiveAdmins()).thenReturn(List.of(admin));

            service.sendFromUser(userId, "primeira");

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationService).sendHybridNotificationToUser(eq(adminId), anyString(), bodyCaptor.capture(), anyMap());
            assertThat(bodyCaptor.getValue()).isEqualTo("primeira");
        }

        @Test
        @DisplayName("Quando count > 1, body começa com (N)")
        void multipleMessagesBody() {
            User user = makeUser(userId, "Maria", User.Role.CLIENT);
            User admin = makeUser(adminId, "Admin", User.Role.ADMIN);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(repository.save(any(SupportMessage.class))).thenAnswer(inv -> inv.getArgument(0));
            lenient().when(repository.lastMessageAt(userId, false)).thenReturn(null);
            when(repository.countUnread(userId, false)).thenReturn(3L);
            when(userRepository.findActiveAdmins()).thenReturn(List.of(admin));

            service.sendFromUser(userId, "terceira");

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationService).sendHybridNotificationToUser(eq(adminId), anyString(), bodyCaptor.capture(), anyMap());
            assertThat(bodyCaptor.getValue()).isEqualTo("(3) terceira");
        }
    }

    @Nested
    @DisplayName("Mark-read e count de não-lidas")
    class ReadStatusTests {

        @Test
        @DisplayName("markAdminMessagesRead delega ao repo com fromAdmin=true")
        void markAdminAsRead() {
            when(repository.markAsRead(eq(userId), eq(true), any(OffsetDateTime.class))).thenReturn(2);
            assertThat(service.markAdminMessagesRead(userId)).isEqualTo(2);
        }

        @Test
        @DisplayName("countMyUnread retorna count de mensagens do admin (fromAdmin=true)")
        void countMyUnread() {
            when(repository.countUnread(userId, true)).thenReturn(5L);
            assertThat(service.countMyUnread(userId)).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("listMyMessages")
    class ListMessagesTests {

        @Test
        @DisplayName("Lista vazia para usuário sem mensagens")
        void emptyThread() {
            when(repository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(Collections.emptyList());
            assertThat(service.listMyMessages(userId)).isEmpty();
        }
    }
}
