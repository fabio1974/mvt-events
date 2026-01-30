package com.mvt.mvt_events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.mvt.mvt_events.jpa.City;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    /**
     * Find cities by name containing (case-insensitive)
     */
    List<City> findByNameContainingIgnoreCase(String name);

    /**
     * Find city by exact name and state (for address lookup)
     */
    Optional<City> findByNameAndState(String name, String state);

    /**
     * Find city by IBGE code
     */
    Optional<City> findByIbgeCode(String ibgeCode);

    /**
     * Check if city exists by IBGE code
     */
    boolean existsByIbgeCode(String ibgeCode);

    /**
     * Find cities by state code, ordered by name
     */
    List<City> findByStateCodeOrderByName(String stateCode);

    /**
     * Find cities by state code containing name
     */
    List<City> findByStateCodeAndNameContainingIgnoreCase(String stateCode, String name);

    /**
     * Get distinct states with their codes
     */
    @Query("""
                SELECT DISTINCT c.state as state, c.stateCode as stateCode
                FROM City c
                ORDER BY c.stateCode
            """)
    List<Map<String, String>> findDistinctStates();

    /**
     * Find top cities by name for autocomplete with enhanced search
     * Usa immutable_unaccent para busca insensível a acentos (native query)
     */
    @Query(value = """
                SELECT * FROM cities c
                WHERE immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER('%' || :query || '%'))
                ORDER BY
                    CASE WHEN immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER(:query || '%')) THEN 1 ELSE 2 END,
                    c.name
            """, nativeQuery = true)
    List<City> findCitiesForAutocomplete(String query);

    /**
     * Enhanced search with accent handling - PostgreSQL compatible
     * Usa immutable_unaccent para busca insensível a acentos
     */
    @Query(value = """
                SELECT * FROM cities c
                WHERE immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER('%' || :query || '%'))
                ORDER BY
                    CASE
                        WHEN immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER(:query || '%')) THEN 1
                        WHEN immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER('%' || :query || '%')) THEN 2
                        ELSE 3
                    END,
                    char_length(c.name),
                    c.name
                LIMIT 20
            """, nativeQuery = true)
    List<City> findCitiesWithEnhancedSearch(String query);

    /**
     * Search cities by partial word matches (for "São José" finding "São José dos
     * Campos")
     * Usa immutable_unaccent para busca insensível a acentos (native query)
     */
    @Query(value = """
                SELECT * FROM cities c
                WHERE immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER('%' || :query || '%'))
                ORDER BY
                    CASE
                        WHEN immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER(:query || '%')) THEN 1
                        WHEN immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER('%' || :query || '%')) THEN 2
                        ELSE 3
                    END,
                    c.name
            """, nativeQuery = true)
    List<City> findCitiesByWords(String query);

    /**
     * Search by two separate words
     * Usa immutable_unaccent para busca insensível a acentos (native query)
     */
    @Query(value = """
                SELECT * FROM cities c
                WHERE (immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER('%' || :word1 || '%'))
                   AND immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER('%' || :word2 || '%')))
                   OR immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER('%' || :word1 || '%'))
                   OR immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER('%' || :word2 || '%'))
                ORDER BY
                    CASE
                        WHEN (immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER('%' || :word1 || '%'))
                             AND immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER('%' || :word2 || '%'))) THEN 1
                        WHEN immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER(:word1 || '%')) THEN 2
                        WHEN immutable_unaccent(LOWER(c.name)) LIKE immutable_unaccent(LOWER(:word2 || '%')) THEN 3
                        ELSE 4
                    END,
                    c.name
            """, nativeQuery = true)
    List<City> findCitiesByTwoWords(String word1, String word2);

    /**
     * Count cities by state
     */
    @Query("SELECT c.stateCode, COUNT(c) FROM City c GROUP BY c.stateCode ORDER BY c.stateCode")
    List<Object[]> countCitiesByState();
}