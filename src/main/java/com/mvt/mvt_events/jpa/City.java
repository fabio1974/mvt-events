package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Entity
@Table(name = "cities")
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "state", nullable = false, length = 50)
    private String state;

    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Column(name = "ibge_code", unique = true, length = 10)
    private String ibgeCode;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Constructors
    public City() {
        this.createdAt = OffsetDateTime.now(ZoneId.of("America/Fortaleza"));
        this.updatedAt = OffsetDateTime.now(ZoneId.of("America/Fortaleza"));
    }

    public City(String name, String state, String stateCode, String ibgeCode) {
        this();
        this.name = name;
        this.state = state;
        this.stateCode = stateCode;
        this.ibgeCode = ibgeCode;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getIbgeCode() {
        return ibgeCode;
    }

    public void setIbgeCode(String ibgeCode) {
        this.ibgeCode = ibgeCode;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Lifecycle methods
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now(ZoneId.of("America/Fortaleza"));
        updatedAt = OffsetDateTime.now(ZoneId.of("America/Fortaleza"));
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneId.of("America/Fortaleza"));
    }

    // toString, equals, hashCode
    @Override
    public String toString() {
        return "City{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", state='" + state + '\'' +
                ", stateCode='" + stateCode + '\'' +
                ", ibgeCode='" + ibgeCode + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        City city = (City) o;
        return Objects.equals(ibgeCode, city.ibgeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ibgeCode);
    }
}