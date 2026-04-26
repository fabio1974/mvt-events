package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Anúncio in-app. Admin cadastra; mobile mostra como popup pra usuários cujo
 * role esteja em {@link #rolesCsv}, com persistência de leitura via
 * {@link AnnouncementRead} (cross-device).
 */
@Entity
@Table(name = "announcements")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement extends BaseEntity {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título pode ter no máximo 200 caracteres")
    @Column(name = "title", nullable = false, length = 200)
    @Visible(table = true, form = true, filter = true)
    private String title;

    /** Conteúdo em Markdown (negrito, listas, links, etc.) */
    @NotBlank(message = "Corpo da mensagem é obrigatório")
    @Size(max = 5000, message = "Corpo pode ter no máximo 5000 caracteres")
    @Column(name = "body_markdown", nullable = false, columnDefinition = "TEXT")
    @Visible(table = false, form = true, filter = false)
    private String bodyMarkdown;

    /**
     * CSV de roles que devem ver este anúncio.
     * Exemplos: "COURIER", "COURIER,ORGANIZER", "COURIER,ORGANIZER,CLIENT".
     * Roles válidos: ADMIN, COURIER, ORGANIZER, CLIENT, CUSTOMER, WAITER.
     */
    @NotBlank(message = "Roles é obrigatório")
    @Size(max = 200)
    @Column(name = "roles_csv", nullable = false, length = 200)
    @Visible(table = true, form = true, filter = true)
    private String rolesCsv;

    @NotNull(message = "Data de publicação é obrigatória")
    @Column(name = "published_at", nullable = false)
    @Visible(table = true, form = true, filter = false)
    private OffsetDateTime publishedAt;

    /** Opcional. Se null, o anúncio nunca expira. */
    @Column(name = "expires_at")
    @Visible(table = true, form = true, filter = false)
    private OffsetDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    @Visible(table = true, form = true, filter = true)
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) isActive = true;
        if (publishedAt == null) publishedAt = OffsetDateTime.now();
    }
}
