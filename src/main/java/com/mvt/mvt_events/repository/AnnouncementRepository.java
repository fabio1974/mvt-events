package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /**
     * Anúncios ativos, já publicados, não expirados, cujo CSV de roles
     * contém o role do usuário, e que ele ainda não marcou como lido.
     * Mais recente primeiro.
     */
    @Query("""
        SELECT a FROM Announcement a
        WHERE a.isActive = true
          AND a.publishedAt <= :now
          AND (a.expiresAt IS NULL OR a.expiresAt > :now)
          AND CONCAT(',', a.rolesCsv, ',') LIKE CONCAT('%,', :role, ',%')
          AND NOT EXISTS (
            SELECT 1 FROM AnnouncementRead r
            WHERE r.announcement = a AND r.user.id = :userId
          )
        ORDER BY a.publishedAt DESC
    """)
    List<Announcement> findActiveUnreadFor(@Param("userId") UUID userId,
                                           @Param("role") String role,
                                           @Param("now") OffsetDateTime now);
}
