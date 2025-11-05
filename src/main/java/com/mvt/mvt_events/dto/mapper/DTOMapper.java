package com.mvt.mvt_events.dto.mapper;

import com.mvt.mvt_events.dto.common.CityDTO;
import com.mvt.mvt_events.dto.common.OrganizationDTO;
import com.mvt.mvt_events.jpa.City;
import com.mvt.mvt_events.jpa.Organization;

/**
 * Utility class for mapping entities to DTOs.
 * Centralizes the mapping logic to ensure consistency across all controllers.
 * 
 * Usage:
 * - CityDTO cityDTO = DTOMapper.toDTO(city);
 * - OrganizationDTO orgDTO = DTOMapper.toDTO(organization);
 */
public class DTOMapper {

    private DTOMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Maps City entity to CityDTO
     * 
     * @param city City entity
     * @return CityDTO or null if city is null
     */
    public static CityDTO toDTO(City city) {
        if (city == null) {
            return null;
        }
        return new CityDTO(
                city.getId(),
                city.getName(),
                city.getState());
    }

    /**
     * Maps Organization entity to OrganizationDTO
     * 
     * @param organization Organization entity
     * @return OrganizationDTO or null if organization is null
     */
    public static OrganizationDTO toDTO(Organization organization) {
        if (organization == null) {
            return null;
        }
        return new OrganizationDTO(
                organization.getId(),
                organization.getName());
    }
}
