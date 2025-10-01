package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.entity.City;
import com.mvt.mvt_events.service.CityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cities")
@CrossOrigin(origins = "*")
public class CityController {

    @Autowired
    private CityService cityService;

    /**
     * Search cities for autocomplete with enhanced fuzzy search
     * GET /api/cities/search?q=são+jose
     */
    @GetMapping("/search")
    public ResponseEntity<List<City>> searchCities(@RequestParam("q") String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        List<City> cities = cityService.searchCities(query);
        return ResponseEntity.ok(cities);
    }

    /**
     * Test endpoint to demonstrate enhanced search capabilities
     * GET /api/cities/search/test?q=sao+jose
     */
    @GetMapping("/search/test")
    public ResponseEntity<Map<String, Object>> testSearch(@RequestParam("q") String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        List<City> results = cityService.searchCities(query);

        Map<String, Object> response = Map.of(
                "query", query,
                "normalized_query", query.trim().toLowerCase()
                        .replace("á", "a").replace("à", "a").replace("ã", "a")
                        .replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u"),
                "total_results", results.size(),
                "results", results.stream().limit(10).toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get city by ID
     * GET /api/cities/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<City> getCityById(@PathVariable Long id) {
        Optional<City> city = cityService.getCityById(id);
        return city.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get city by IBGE code
     * GET /api/cities/ibge/3550308
     */
    @GetMapping("/ibge/{ibgeCode}")
    public ResponseEntity<City> getCityByIbgeCode(@PathVariable String ibgeCode) {
        Optional<City> city = cityService.getCityByIbgeCode(ibgeCode);
        return city.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get cities by state
     * GET /api/cities/state/SP
     */
    @GetMapping("/state/{stateCode}")
    public ResponseEntity<List<City>> getCitiesByState(@PathVariable String stateCode) {
        List<City> cities = cityService.getCitiesByState(stateCode);
        return ResponseEntity.ok(cities);
    }

    /**
     * Get all states
     * GET /api/cities/states
     */
    @GetMapping("/states")
    public ResponseEntity<List<Map<String, String>>> getStates() {
        List<Map<String, String>> states = cityService.getStates();
        return ResponseEntity.ok(states);
    }

    /**
     * Get cities statistics
     * GET /api/cities/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCitiesStats() {
        long totalCities = cityService.getTotalCitiesCount();
        List<Object[]> citiesByState = cityService.getCitiesCountByState();

        Map<String, Object> stats = Map.of(
                "totalCities", totalCities,
                "citiesByState", citiesByState);

        return ResponseEntity.ok(stats);
    }
}