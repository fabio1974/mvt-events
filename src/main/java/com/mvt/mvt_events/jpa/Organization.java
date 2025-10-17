package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organizations")
@Data
@EqualsAndHashCode(callSuper = true)
public class Organization extends BaseEntity {

    @DisplayLabel
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Column(nullable = false)
    private String name;

    @Size(max = 100, message = "Slug must not exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Contact email must be valid")
    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(length = 255)
    private String website;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    @Visible(filter = true, table = true, form = false)
    private String logoUrl;

    // Relationships
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Event> events = new ArrayList<>();
}