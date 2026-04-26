package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Announcement;
import com.mvt.mvt_events.jpa.AnnouncementRead;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.AnnouncementReadRepository;
import com.mvt.mvt_events.repository.AnnouncementRepository;
import com.mvt.mvt_events.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD admin de anúncios + endpoints de consumo pro app (active list + mark read).
 *
 * Persistência da leitura é por-usuário via {@link AnnouncementRead}, então
 * usuário lê uma vez e não vê mais em nenhum dispositivo.
 */
@RestController
@RequestMapping("/api/announcements")
@Tag(name = "Announcements", description = "Anúncios in-app (admin CRUD + listagem ativa pro mobile)")
public class AnnouncementController {

    @Autowired private AnnouncementRepository announcementRepository;
    @Autowired private AnnouncementReadRepository readRepository;
    @Autowired private UserRepository userRepository;

    // ─── ADMIN: CRUD ──────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar anúncios (admin)")
    public ResponseEntity<Page<Announcement>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return ResponseEntity.ok(announcementRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Buscar anúncio por ID (admin)")
    public ResponseEntity<Announcement> getById(@PathVariable Long id) {
        return announcementRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar anúncio (admin)")
    public ResponseEntity<Announcement> create(@RequestBody @Valid Announcement payload) {
        payload.setId(null);
        return ResponseEntity.ok(announcementRepository.save(payload));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar anúncio (admin)")
    public ResponseEntity<Announcement> update(@PathVariable Long id, @RequestBody @Valid Announcement payload) {
        return announcementRepository.findById(id)
                .map(existing -> {
                    existing.setTitle(payload.getTitle());
                    existing.setBodyMarkdown(payload.getBodyMarkdown());
                    existing.setRolesCsv(payload.getRolesCsv());
                    existing.setPublishedAt(payload.getPublishedAt());
                    existing.setExpiresAt(payload.getExpiresAt());
                    existing.setIsActive(payload.getIsActive());
                    return ResponseEntity.ok(announcementRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Excluir anúncio (admin)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!announcementRepository.existsById(id)) return ResponseEntity.notFound().build();
        announcementRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ─── USER: consumo pelo app ──────────────────────────────────────────

    @GetMapping("/active")
    @Operation(summary = "Lista anúncios ativos não-lidos pelo usuário logado",
               description = "Filtra por role do usuário, publishedAt, expiresAt e leitura prévia")
    public ResponseEntity<List<Announcement>> activeForCurrentUser(Authentication authentication) {
        User user = currentUser(authentication);
        if (user.getRole() == null) return ResponseEntity.ok(List.of());
        List<Announcement> list = announcementRepository.findActiveUnreadFor(
                user.getId(), user.getRole().name(), OffsetDateTime.now());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Marca anúncio como lido pelo usuário logado")
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        Announcement announcement = announcementRepository.findById(id).orElse(null);
        if (announcement == null) return ResponseEntity.notFound().build();

        if (readRepository.existsByAnnouncementIdAndUserId(id, user.getId())) {
            return ResponseEntity.ok(Map.of("alreadyRead", true));
        }
        AnnouncementRead read = AnnouncementRead.builder()
                .announcement(announcement)
                .user(user)
                .build();
        readRepository.save(read);
        return ResponseEntity.ok(Map.of("alreadyRead", false));
    }

    // ─── helpers ──────────────────────────────────────────────────────────

    private User currentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByUsername(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}
