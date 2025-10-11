package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/events-raw")
    public Map<String, Object> getEventsRaw() {
        Long tenantId = TenantContext.getCurrentTenantId();

        // Query SEM filtro
        List<Map<String, Object>> allEvents = jdbcTemplate.queryForList(
                "SELECT id, name, organization_id FROM events ORDER BY id");

        // Query COM filtro
        List<Map<String, Object>> filteredEvents = jdbcTemplate.queryForList(
                "SELECT id, name, organization_id FROM events WHERE organization_id = ? ORDER BY id",
                tenantId);

        return Map.of(
                "tenantId", tenantId != null ? tenantId : "NULL",
                "allEvents", allEvents,
                "filteredEvents", filteredEvents,
                "allCount", allEvents.size(),
                "filteredCount", filteredEvents.size());
    }
}
