package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_push_tokens")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class UserPushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, length = 500)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Campos específicos para Web Push Notifications
    @Column(name = "web_endpoint", length = 1000)
    private String webEndpoint;

    @Column(name = "web_p256dh", length = 500)
    private String webP256dh;

    @Column(name = "web_auth", length = 500)
    private String webAuth;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Método helper para obter o ID do usuário
    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }

    // Método helper para definir o usuário por ID
    public void setUserId(UUID userId) {
        if (userId != null) {
            this.user = new User();
            this.user.setId(userId);
        } else {
            this.user = null;
        }
    }

    // Métodos helper para Web Push
    public boolean isWebPush() {
        return Platform.WEB.equals(platform) && DeviceType.WEB.equals(deviceType);
    }

    public boolean hasWebPushData() {
        return webEndpoint != null && webP256dh != null && webAuth != null;
    }

    public void setWebPushData(String endpoint, String p256dh, String auth) {
        this.webEndpoint = endpoint;
        this.webP256dh = p256dh;
        this.webAuth = auth;
    }

    public void clearWebPushData() {
        this.webEndpoint = null;
        this.webP256dh = null;
        this.webAuth = null;
    }

    public enum Platform {
        IOS("ios"),
        ANDROID("android"),
        WEB("web");

        private final String value;

        Platform(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum DeviceType {
        MOBILE("mobile"),
        WEB("web"),
        TABLET("tablet");

        private final String value;

        DeviceType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}