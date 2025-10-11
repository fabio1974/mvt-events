package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.repository.EventRepository;
import com.mvt.mvt_events.specification.EventSpecifications;
import com.mvt.mvt_events.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class SpecificationTestController {

    private final EventRepository repository;

    public SpecificationTestController(EventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/test-specification")
    public Map<String, Object> testSpecification() {
        Long tenantId = TenantContext.getCurrentTenantId();

        System.out.println("\n============ SPECIFICATION TEST ============");
        System.out.println("Tenant ID: " + tenantId);

        // Test 1: findAll without filter
        List<Event> allEvents = repository.findAll();
        System.out.println("Test 1 - findAll(): " + allEvents.size() + " events");

        // Test 2: findAll with specification
        Specification<Event> spec = EventSpecifications.forOrganization(tenantId);
        List<Event> filtered = repository.findAll(spec);
        System.out.println("Test 2 - findAll(spec): " + filtered.size() + " events");

        // Test 3: findAll with page
        Page<Event> pagedFiltered = repository.findAll(spec, PageRequest.of(0, 10));
        System.out.println("Test 3 - findAll(spec, page): " + pagedFiltered.getTotalElements() + " events");

        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("allEventsCount", allEvents.size());
        result.put("filteredCount", filtered.size());
        result.put("pagedFilteredCount", pagedFiltered.getTotalElements());
        result.put("filteredNames", filtered.stream().map(Event::getName).toList());
        result.put("filteredOrgIds", filtered.stream().map(e -> e.getOrganization().getId()).toList());

        System.out.println("============ END TEST ============\n");

        return result;
    }
}
