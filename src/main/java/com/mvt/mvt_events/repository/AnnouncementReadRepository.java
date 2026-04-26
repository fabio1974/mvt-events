package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, UUID> {

    /** Announcement.id é Long (BaseEntity); user.id é UUID. */
    boolean existsByAnnouncementIdAndUserId(Long announcementId, UUID userId);

    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, UUID userId);
}
