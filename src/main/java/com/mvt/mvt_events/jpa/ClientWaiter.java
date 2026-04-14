package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "client_waiters",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"client_id", "waiter_id"}),
                @UniqueConstraint(columnNames = {"client_id", "pin"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientWaiter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waiter_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User waiter;

    @Column(length = 6)
    private String pin;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @com.fasterxml.jackson.annotation.JsonGetter("clientId")
    public String getClientIdValue() {
        return client != null ? client.getId().toString() : null;
    }

    @com.fasterxml.jackson.annotation.JsonGetter("clientName")
    public String getClientName() {
        try { return client != null ? client.getName() : null; } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("waiterId")
    public String getWaiterIdValue() {
        return waiter != null ? waiter.getId().toString() : null;
    }

    @com.fasterxml.jackson.annotation.JsonGetter("waiterName")
    public String getWaiterName() {
        try { return waiter != null ? waiter.getName() : null; } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("waiterEmail")
    public String getWaiterEmail() {
        try { return waiter != null ? waiter.getUsername() : null; } catch (Exception e) { return null; }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
