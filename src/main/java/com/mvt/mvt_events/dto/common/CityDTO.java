package com.mvt.mvt_events.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for City entity used in nested responses.
 * Contains only the essential fields to avoid circular references and lazy
 * loading issues.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityDTO {
    private Long id;
    private String name;
    private String state;
}
