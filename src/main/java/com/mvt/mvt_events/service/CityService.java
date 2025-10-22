package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.City;
import com.mvt.mvt_events.repository.CityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CityService {

    @Autowired
    private CityRepository cityRepository;

    /**
     * Enhanced search cities with multiple strategies
     */
    public List<City> searchCities(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        String cleanQuery = normalizeQuery(query.trim());

        try {
            // Strategy 1: Enhanced search
            List<City> results = cityRepository.findCitiesWithEnhancedSearch(cleanQuery);

            // Strategy 2: If few results and has space, try word-based search
            if (results.size() < 10 && cleanQuery.contains(" ")) {
                List<City> wordResults = cityRepository.findCitiesByWords(cleanQuery);
                addUniqueResults(results, wordResults);

                // Strategy 3: Two-word search for better matching
                String[] words = cleanQuery.split("\\s+");
                if (words.length >= 2) {
                    List<City> twoWordResults = cityRepository.findCitiesByTwoWords(words[0], words[1]);
                    addUniqueResults(results, twoWordResults);
                }
            }

            // Strategy 4: Fallback to original search
            if (results.isEmpty()) {
                results = cityRepository.findCitiesForAutocomplete(cleanQuery);
            }

            return results.stream().limit(20).toList();

        } catch (Exception e) {
            // Log error and fallback to simple search
            System.err.println("Error in enhanced search: " + e.getMessage());
            return cityRepository.findCitiesForAutocomplete(cleanQuery);
        }
    }

    /**
     * Add unique results to the main list, avoiding duplicates
     */
    private void addUniqueResults(List<City> mainResults, List<City> newResults) {
        for (City city : newResults) {
            if (mainResults.stream().noneMatch(existing -> existing.getId().equals(city.getId()))) {
                mainResults.add(city);
            }
        }
    }

    /**
     * Normalize query for better search results
     */
    private String normalizeQuery(String query) {
        if (query == null)
            return "";

        return query
                .trim()
                .replaceAll("\\s+", " ") // Multiple spaces to single space
                .toLowerCase();
    }

    /**
     * Get city by ID
     */
    public Optional<City> getCityById(Long id) {
        return cityRepository.findById(id);
    }

    /**
     * Get city by IBGE code
     */
    public Optional<City> getCityByIbgeCode(String ibgeCode) {
        return cityRepository.findByIbgeCode(ibgeCode);
    }

    /**
     * Get cities by state code
     */
    public List<City> getCitiesByState(String stateCode) {
        return cityRepository.findByStateCodeOrderByName(stateCode.toUpperCase());
    }

    /**
     * Get all states with their codes
     */
    public List<Map<String, String>> getStates() {
        return cityRepository.findDistinctStates();
    }

    /**
     * Get total cities count
     */
    public long getTotalCitiesCount() {
        return cityRepository.count();
    }

    /**
     * Get cities count by state
     */
    public List<Object[]> getCitiesCountByState() {
        return cityRepository.countCitiesByState();
    }
}