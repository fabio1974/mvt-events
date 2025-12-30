package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.common.JwtUtil;
import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.jpa.UserPushToken;
import com.mvt.mvt_events.repository.EmploymentContractRepository;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.service.DeliveryNotificationService;
import com.mvt.mvt_events.service.DeliveryService;
import com.mvt.mvt_events.service.PushNotificationService;
import com.mvt.mvt_events.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/debug")
@Slf4j
public class DebugController {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private JwtUtil jwtUtil;

        @Autowired
        private EmploymentContractRepository employmentContractRepository;

        @Autowired
        private DeliveryNotificationService deliveryNotificationService;

        @Autowired
        private com.mvt.mvt_events.service.PushNotificationService pushNotificationService;

        @Autowired
        private com.mvt.mvt_events.service.DeliveryService deliveryService;

        @Autowired
        private com.mvt.mvt_events.repository.UserRepository userRepository;

        @Autowired
        private com.mvt.mvt_events.repository.DeliveryRepository deliveryRepository;

        @Autowired
        private nl.martijndwars.webpush.PushService pushService;

        @org.springframework.beans.factory.annotation.Value("${webpush.vapid.public-key}")
        private String vapidPublicKey;

        @GetMapping("/events-raw")
        public ResponseEntity<?> getEventsRaw() {
                try {
                        // Buscar algumas deliveries para verificar estrutura
                        List<Map<String, Object>> deliveries = jdbcTemplate.queryForList(
                                        "SELECT d.id, d.status, d.from_address, d.to_address, " +
                                                        "c.username as client_username, o.name as org_name, o.id as org_id "
                                                        +
                                                        "FROM deliveries d " +
                                                        "LEFT JOIN users c ON d.client_id = c.id " +
                                                        "LEFT JOIN organizations o ON c.organization_id = o.id " +
                                                        "ORDER BY d.created_at DESC LIMIT 10");

                        return ResponseEntity.ok(Map.of(
                                        "rawDeliveries", deliveries,
                                        "totalCount",
                                        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deliveries", Long.class)));
                } catch (Exception e) {
                        return ResponseEntity.ok(Map.of("error", e.getMessage()));
                }
        }

        @GetMapping("/vapid-public-key")
        public ResponseEntity<?> getVapidPublicKey() {
                try {
                        return ResponseEntity.ok(Map.of(
                                        "publicKey", vapidPublicKey,
                                        "info", "Use esta chave no front-end para registrar Web Push tokens",
                                        "format", "base64url"));
                } catch (Exception e) {
                        return ResponseEntity.ok(Map.of("error", e.getMessage()));
                }
        }

        @GetMapping("/delivery-organizations")
        public ResponseEntity<?> getDeliveryOrganizations() {
                try {
                        // Buscar deliveries com informações detalhadas de organização
                        List<Map<String, Object>> deliveries = jdbcTemplate.queryForList(
                                        "SELECT " +
                                                        "d.id as delivery_id, " +
                                                        "d.status, " +
                                                        "d.from_address, " +
                                                        "c.id as client_id, " +
                                                        "c.username as client_username, " +
                                                        "c.organization_id as client_org_id, " +
                                                        "o.id as org_id, " +
                                                        "o.name as org_name " +
                                                        "FROM deliveries d " +
                                                        "LEFT JOIN users c ON d.client_id = c.id " +
                                                        "LEFT JOIN organizations o ON c.organization_id = o.id " +
                                                        "ORDER BY d.created_at DESC");

                        // Buscar também contratos ativos por organização
                        List<Map<String, Object>> contracts = jdbcTemplate.queryForList(
                                        "SELECT " +
                                                        "ec.courier_id, " +
                                                        "ec.organization_id, " +
                                                        "ec.is_active, " +
                                                        "u.username as courier_username, " +
                                                        "o.name as org_name " +
                                                        "FROM employment_contracts ec " +
                                                        "JOIN users u ON ec.courier_id = u.id " +
                                                        "JOIN organizations o ON ec.organization_id = o.id " +
                                                        "WHERE ec.is_active = true");

                        return ResponseEntity.ok(Map.of(
                                        "deliveries", deliveries,
                                        "contracts", contracts,
                                        "deliveriesCount", deliveries.size(),
                                        "contractsCount", contracts.size()));
                } catch (Exception e) {
                        return ResponseEntity.ok(Map.of("error", e.getMessage()));
                }
        }

        @GetMapping("/check-data")
        public Map<String, Object> checkData() {
                try {
                        List<Map<String, Object>> organizations = jdbcTemplate.queryForList(
                                        "SELECT id, name, slug FROM organizations LIMIT 10");

                        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                                        "SELECT id, username, role, organization_id FROM users WHERE role IN ('COURIER', 'CLIENT', 'ADMIN') LIMIT 10");

                        List<Map<String, Object>> contracts = jdbcTemplate.queryForList(
                                        "SELECT courier_id, organization_id, is_active FROM employment_contracts LIMIT 10");

                        List<Map<String, Object>> deliveries = jdbcTemplate.queryForList(
                                        "SELECT d.id, d.status, c.organization_id " +
                                                        "FROM deliveries d " +
                                                        "LEFT JOIN users c ON d.client_id = c.id " +
                                                        "WHERE d.status = 'PENDING' LIMIT 10");

                        return Map.of(
                                        "organizations", organizations,
                                        "users", users,
                                        "contracts", contracts,
                                        "deliveries", deliveries);

                } catch (Exception e) {
                        return Map.of("error", e.getMessage());
                }
        }

        @GetMapping("/create-test-data")
        public Map<String, Object> createTestData() {
                try {
                        // 1. Verificar e criar organização
                        Long orgCount = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM organizations WHERE id = 6", Long.class);

                        if (orgCount == 0) {
                                jdbcTemplate.update(
                                                "INSERT INTO organizations (id, name, slug, created_at, updated_at) "
                                                                +
                                                                "VALUES (6, 'Test Organization', 'test-org', NOW(), NOW())");
                        }

                        // 2. Verificar e criar courier
                        Long courierCount = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM users WHERE id = '60085b55-9f67-4489-b1e4-e310ee5c4f27'",
                                        Long.class);

                        if (courierCount == 0) {
                                jdbcTemplate.update(
                                                "INSERT INTO users " +
                                                                "(id, username, password, email, name, role, enabled, organization_id, created_at, updated_at) "
                                                                +
                                                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                                                "60085b55-9f67-4489-b1e4-e310ee5c4f27",
                                                "courier@test.com",
                                                "$2a$10$2X8wNBhT5p5Nw8aKzE3/xOQh7L.jF9yV2sL8mJ6qN3pR4tY7uZ9wG",
                                                "courier@test.com",
                                                "Test Courier",
                                                "COURIER",
                                                true,
                                                6);
                        } else {
                                // Atualizar courier existente
                                jdbcTemplate.update(
                                                "UPDATE users SET organization_id = ?, role = ?, enabled = ? WHERE id = ?",
                                                6, "COURIER", true, "60085b55-9f67-4489-b1e4-e310ee5c4f27");
                        }

                        // 3. Verificar e criar contrato
                        Long contractCount = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM employment_contracts WHERE courier_id = ? AND organization_id = ?",
                                        Long.class, "60085b55-9f67-4489-b1e4-e310ee5c4f27", 6);

                        if (contractCount == 0) {
                                jdbcTemplate.update(
                                                "INSERT INTO employment_contracts " +
                                                                "(courier_id, organization_id, is_active, created_at, updated_at) "
                                                                +
                                                                "VALUES (?, ?, ?, NOW(), NOW())",
                                                "60085b55-9f67-4489-b1e4-e310ee5c4f27", 6, true);
                        }

                        // 4. Criar clientes se não existem
                        Long clientCount = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM users WHERE role = 'CLIENT' AND organization_id = 6",
                                        Long.class);

                        if (clientCount < 2) {
                                String clientId1 = java.util.UUID.randomUUID().toString();
                                String clientId2 = java.util.UUID.randomUUID().toString();

                                jdbcTemplate.update(
                                                "INSERT INTO users " +
                                                                "(id, username, password, email, name, role, enabled, organization_id, created_at, updated_at) "
                                                                +
                                                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                                                clientId1, "client1@test.com",
                                                "$2a$10$2X8wNBhT5p5Nw8aKzE3/xOQh7L.jF9yV2sL8mJ6qN3pR4tY7uZ9wG",
                                                "client1@test.com", "Test Client 1", "CLIENT", true, 6);

                                jdbcTemplate.update(
                                                "INSERT INTO users " +
                                                                "(id, username, password, email, name, role, enabled, organization_id, created_at, updated_at) "
                                                                +
                                                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                                                clientId2, "client2@test.com",
                                                "$2a$10$2X8wNBhT5p5Nw8aKzE3/xOQh7L.jF9yV2sL8mJ6qN3pR4tY7uZ9wG",
                                                "client2@test.com", "Test Client 2", "CLIENT", true, 6);
                        }

                        // 5. Criar deliveries se não existem
                        Long deliveryCount = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM deliveries d " +
                                                        "JOIN users c ON d.client_id = c.id " +
                                                        "WHERE d.status = 'PENDING' AND c.organization_id = 6",
                                        Long.class);

                        if (deliveryCount < 2) {
                                List<Map<String, Object>> clients = jdbcTemplate.queryForList(
                                                "SELECT id, name FROM users WHERE role = 'CLIENT' AND organization_id = 6 LIMIT 2");

                                for (Map<String, Object> client : clients) {
                                        String deliveryId = java.util.UUID.randomUUID().toString();
                                        jdbcTemplate.update(
                                                        "INSERT INTO deliveries " +
                                                                        "(id, client_id, status, created_at, updated_at, pickup_address, delivery_address, package_description) "
                                                                        +
                                                                        "VALUES (?, ?, ?, NOW(), NOW(), ?, ?, ?)",
                                                        deliveryId,
                                                        client.get("id"),
                                                        "PENDING",
                                                        "Test Pickup Address " + client.get("name"),
                                                        "Test Delivery Address " + client.get("name"),
                                                        "Test Package for " + client.get("name"));
                                }
                        }

                        // Verificar dados criados
                        List<Map<String, Object>> stats = jdbcTemplate.queryForList(
                                        "SELECT 'Organizations' as table_name, count(*) as count FROM organizations WHERE id = 6 "
                                                        +
                                                        "UNION ALL " +
                                                        "SELECT 'Users COURIER', count(*) FROM users WHERE role = 'COURIER' AND organization_id = 6 "
                                                        +
                                                        "UNION ALL " +
                                                        "SELECT 'Users CLIENT', count(*) FROM users WHERE role = 'CLIENT' AND organization_id = 6 "
                                                        +
                                                        "UNION ALL " +
                                                        "SELECT 'Employment Contracts', count(*) FROM employment_contracts WHERE organization_id = 6 "
                                                        +
                                                        "UNION ALL " +
                                                        "SELECT 'Deliveries PENDING', count(*) FROM deliveries d " +
                                                        "JOIN users c ON d.client_id = c.id " +
                                                        "WHERE d.status = 'PENDING' AND c.organization_id = 6");

                        return Map.of(
                                        "message", "Dados de teste criados com sucesso",
                                        "stats", stats,
                                        "courier", Map.of(
                                                        "username", "courier@test.com",
                                                        "password", "password123",
                                                        "organizationId", 6));

                } catch (Exception e) {
                        return Map.of("error", e.getMessage());
                }
        }

        @GetMapping("/create-test-courier")
        public Map<String, Object> createTestCourier() {
                try {
                        // Verificar se já existe o courier de teste
                        List<Map<String, Object>> existingCourier = jdbcTemplate.queryForList(
                                        "SELECT id, username, role FROM users WHERE username = 'courier@test.com'");

                        if (!existingCourier.isEmpty()) {
                                return Map.of(
                                                "message", "Courier de teste já existe",
                                                "courier", existingCourier.get(0));
                        }

                        // Criar organização de teste se não existir
                        List<Map<String, Object>> existingOrg = jdbcTemplate.queryForList(
                                        "SELECT id FROM organizations WHERE slug = 'test-org'");

                        Long orgId;
                        if (existingOrg.isEmpty()) {
                                orgId = jdbcTemplate.queryForObject(
                                                "INSERT INTO organizations (name, slug, created_at, updated_at) "
                                                                +
                                                                "VALUES ('Test Organization', 'test-org', NOW(), NOW()) RETURNING id",
                                                Long.class);
                        } else {
                                orgId = ((Number) existingOrg.get(0).get("id")).longValue();
                        }

                        // Criar courier de teste
                        String insertCourierSql = "INSERT INTO users " +
                                        "(id, username, password, email, name, role, enabled, organization_id, created_at, updated_at) "
                                        +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) RETURNING id";

                        UUID courierId = UUID.randomUUID();
                        jdbcTemplate.update(insertCourierSql,
                                        courierId,
                                        "courier@test.com",
                                        "$2a$10$2X8wNBhT5p5Nw8aKzE3/xOQh7L.jF9yV2sL8mJ6qN3pR4tY7uZ9wG", // password123
                                        "courier@test.com",
                                        "Test Courier",
                                        "COURIER",
                                        true,
                                        orgId);

                        // Criar contrato de emprego
                        String insertContractSql = "INSERT INTO employment_contracts " +
                                        "(courier_id, organization_id, is_active, created_at, updated_at) " +
                                        "VALUES (?, ?, ?, NOW(), NOW())";

                        jdbcTemplate.update(insertContractSql, courierId, orgId, true);

                        return Map.of(
                                        "message", "Courier de teste criado com sucesso",
                                        "courierId", courierId.toString(),
                                        "organizationId", orgId,
                                        "username", "courier@test.com",
                                        "password", "password123");

                } catch (Exception e) {
                        return Map.of("error", e.getMessage(), "stack", e.getStackTrace());
                }
        }

        @GetMapping("/courier-sql-trace")
        public ResponseEntity<?> getCourierSqlTrace(HttpServletRequest request) {
                try {
                        String token = request.getHeader("Authorization");
                        if (token != null && token.startsWith("Bearer ")) {
                                token = token.substring(7);
                        }

                        Map<String, Object> userData = jwtUtil.getUserDataFromToken(token);
                        String role = (String) userData.get("role");
                        String courierUserIdStr = (String) userData.get("userId");

                        if (!"COURIER".equals(role)) {
                                return ResponseEntity.ok(Map.of("error", "Only COURIER role supported"));
                        }

                        UUID courierUserId = UUID.fromString(courierUserIdStr);

                        // 1. Buscar contratos ativos
                        List<Object[]> contractData = employmentContractRepository
                                        .findContractDataByCourierId(courierUserId);
                        List<Long> organizationIds = contractData.stream()
                                        .filter(data -> (Boolean) data[3]) // is_active = true
                                        .map(data -> (Long) data[0]) // organization_id
                                        .toList();

                        // 2. Construir a query SQL que seria executada
                        String sqlQuery = "SELECT d.* FROM deliveries d " +
                                        "INNER JOIN users c ON d.client_id = c.id " +
                                        "INNER JOIN organizations o ON c.organization_id = o.id " +
                                        "WHERE o.id IN ("
                                        + String.join(",", organizationIds.stream().map(String::valueOf).toList())
                                        + ") " +
                                        "AND d.status = 'PENDING' " +
                                        "ORDER BY d.created_at DESC " +
                                        "LIMIT 20 OFFSET 0";

                        // 3. Executar a query diretamente para debugging
                        List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery.replace("d.*",
                                        "d.id, d.status, d.from_address, d.to_address, o.name as org_name"));

                        return ResponseEntity.ok(Map.of(
                                        "courierUserId", courierUserId,
                                        "activeOrganizationIds", organizationIds,
                                        "sqlQuery", sqlQuery,
                                        "results", results,
                                        "resultCount", results.size()));
                } catch (Exception e) {
                        return ResponseEntity.ok(Map.of("error", e.getMessage(), "stackTrace", e.getStackTrace()));
                }
        }

        @GetMapping("/courier-trace")
        public Map<String, Object> traceCourierDeliveries(HttpServletRequest request) {
                try {
                        // Extrair dados do token
                        String authHeader = request.getHeader("Authorization");
                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                                return Map.of("error", "Token não encontrado");
                        }

                        String token = authHeader.substring(7);
                        String role = jwtUtil.getRoleFromToken(token);
                        UUID courierUserId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
                        Long organizationId = jwtUtil.getOrganizationIdFromToken(token);

                        // 1. Verificar contratos do courier
                        List<Object[]> contractData = employmentContractRepository
                                        .findContractDataByCourierId(courierUserId);

                        List<Map<String, Object>> contracts = contractData.stream()
                                        .map(data -> Map.of(
                                                        "organizationId", data[0],
                                                        "organizationName", data[1],
                                                        "linkedAt", data[2],
                                                        "isActive", data[3]))
                                        .toList();

                        // 2. Extrair organizações ativas
                        List<Long> activeOrgIds = contractData.stream()
                                        .filter(data -> (Boolean) data[3]) // is_active = true
                                        .map(data -> (Long) data[0]) // organization_id
                                        .toList();

                        // 3. Verificar deliveries nas organizações ativas
                        List<Map<String, Object>> deliveries = new ArrayList<>();
                        if (!activeOrgIds.isEmpty()) {
                                List<Map<String, Object>> deliveryData = jdbcTemplate.queryForList(
                                                "SELECT d.id, d.status, c.organization_id, o.name as org_name " +
                                                                "FROM deliveries d " +
                                                                "LEFT JOIN users c ON d.client_id = c.id " +
                                                                "LEFT JOIN organizations o ON c.organization_id = o.id "
                                                                +
                                                                "WHERE c.organization_id IN (" +
                                                                activeOrgIds.stream().map(String::valueOf)
                                                                                .collect(java.util.stream.Collectors
                                                                                                .joining(","))
                                                                + ")");
                                deliveries.addAll(deliveryData);
                        }

                        // 4. Verificar deliveries PENDING especificamente
                        List<Map<String, Object>> pendingDeliveries = new ArrayList<>();
                        if (!activeOrgIds.isEmpty()) {
                                List<Map<String, Object>> pendingData = jdbcTemplate.queryForList(
                                                "SELECT d.id, d.status, c.organization_id, o.name as org_name " +
                                                                "FROM deliveries d " +
                                                                "LEFT JOIN users c ON d.client_id = c.id " +
                                                                "LEFT JOIN organizations o ON c.organization_id = o.id "
                                                                +
                                                                "WHERE d.status = 'PENDING' AND c.organization_id IN ("
                                                                +
                                                                activeOrgIds.stream().map(String::valueOf)
                                                                                .collect(java.util.stream.Collectors
                                                                                                .joining(","))
                                                                + ")");
                                pendingDeliveries.addAll(pendingData);
                        }

                        return Map.of(
                                        "courierUserId", courierUserId.toString(),
                                        "role", role,
                                        "tokenOrganizationId", organizationId != null ? organizationId : "NULL",
                                        "allContracts", contracts,
                                        "activeOrgIds", activeOrgIds,
                                        "allDeliveries", deliveries,
                                        "pendingDeliveries", pendingDeliveries,
                                        "summary", Map.of(
                                                        "totalContracts", contracts.size(),
                                                        "activeContracts", activeOrgIds.size(),
                                                        "totalDeliveries", deliveries.size(),
                                                        "pendingDeliveries", pendingDeliveries.size()));

                } catch (Exception e) {
                        return Map.of("error", e.getMessage());
                }
        }

        @GetMapping("/courier-contracts")
        public Map<String, Object> getCourierContracts(HttpServletRequest request) {
                try {
                        // Extrair dados do token
                        String authHeader = request.getHeader("Authorization");
                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                                return Map.of("error", "Token não encontrado");
                        }

                        String token = authHeader.substring(7);
                        String role = jwtUtil.getRoleFromToken(token);
                        UUID courierUserId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
                        Long organizationId = jwtUtil.getOrganizationIdFromToken(token);

                        // Verificar contratos do courier
                        List<Map<String, Object>> contracts = jdbcTemplate.queryForList(
                                        "SELECT ec.id, ec.organization_id, ec.courier_id, ec.is_active, o.name as org_name "
                                                        +
                                                        "FROM employment_contracts ec " +
                                                        "LEFT JOIN organizations o ON ec.organization_id = o.id " +
                                                        "WHERE ec.courier_id = ?",
                                        courierUserId);

                        // Verificar deliveries PENDING
                        List<Map<String, Object>> deliveries = jdbcTemplate.queryForList(
                                        "SELECT d.id, d.status, c.organization_id " +
                                                        "FROM deliveries d " +
                                                        "LEFT JOIN users c ON d.client_id = c.id " +
                                                        "WHERE d.status = 'PENDING'");

                        // Verificar deliveries PENDING da organização do courier
                        List<Map<String, Object>> orgDeliveries = jdbcTemplate.queryForList(
                                        "SELECT d.id, d.status, c.organization_id " +
                                                        "FROM deliveries d " +
                                                        "LEFT JOIN users c ON d.client_id = c.id " +
                                                        "WHERE d.status = 'PENDING' AND c.organization_id = ?",
                                        organizationId);

                        return Map.of(
                                        "courierUserId", courierUserId.toString(),
                                        "role", role,
                                        "tokenOrganizationId", organizationId != null ? organizationId : "NULL",
                                        "contracts", contracts,
                                        "allPendingDeliveries", deliveries,
                                        "orgPendingDeliveries", orgDeliveries,
                                        "contractsCount", contracts.size(),
                                        "allPendingCount", deliveries.size(),
                                        "orgPendingCount", orgDeliveries.size());

                } catch (Exception e) {
                        return Map.of("error", e.getMessage(), "stack", e.getStackTrace());
                }
        }

        @PostMapping("/test-delivery-notification")
        public ResponseEntity<?> testDeliveryNotification(
                        @RequestParam(required = false) String deliveryId,
                        @RequestParam(required = false) Long organizationId) {
                try {
                        // Se delivery ID específico foi fornecido, usar ele
                        String targetDeliveryId = deliveryId;

                        if (targetDeliveryId == null) {
                                // Buscar uma delivery PENDING para teste
                                List<Map<String, Object>> pendingDeliveries = jdbcTemplate.queryForList(
                                                "SELECT d.id FROM deliveries d " +
                                                                "JOIN users c ON d.client_id = c.id " +
                                                                "WHERE d.status = 'PENDING' " +
                                                                (organizationId != null ? "AND c.organization_id = ? "
                                                                                : "")
                                                                +
                                                                "LIMIT 1",
                                                organizationId != null ? new Object[] { organizationId }
                                                                : new Object[] {});

                                if (pendingDeliveries.isEmpty()) {
                                        return ResponseEntity.ok(Map.of(
                                                        "error", "Nenhuma delivery PENDING encontrada",
                                                        "suggestion",
                                                        "Crie uma delivery primeiro ou use organizationId específico"));
                                }

                                targetDeliveryId = String.valueOf(pendingDeliveries.get(0).get("id"));
                        }

                        // Buscar informações da delivery
                        List<Map<String, Object>> deliveryInfo = jdbcTemplate.queryForList(
                                        "SELECT d.id, d.status, c.organization_id, o.name as org_name " +
                                                        "FROM deliveries d " +
                                                        "JOIN users c ON d.client_id = c.id " +
                                                        "JOIN organizations o ON c.organization_id = o.id " +
                                                        "WHERE d.id = ?",
                                        targetDeliveryId);

                        if (deliveryInfo.isEmpty()) {
                                return ResponseEntity
                                                .ok(Map.of("error", "Delivery não encontrada: " + targetDeliveryId));
                        }

                        Map<String, Object> delivery = deliveryInfo.get(0);
                        Long deliveryOrgId = ((Number) delivery.get("organization_id")).longValue();

                        // Buscar couriers da organização com push tokens
                        List<Map<String, Object>> couriersWithTokens = jdbcTemplate.queryForList(
                                        "SELECT u.id, u.username, u.name, " +
                                                        "COUNT(pt.id) as token_count, " +
                                                        "STRING_AGG(pt.device_type, ', ') as device_types " +
                                                        "FROM users u " +
                                                        "JOIN employment_contracts ec ON u.id = ec.courier_id " +
                                                        "LEFT JOIN user_push_tokens pt ON u.id = pt.user_id " +
                                                        "WHERE u.role = 'COURIER' " +
                                                        "AND ec.organization_id = ? " +
                                                        "AND ec.is_active = true " +
                                                        "GROUP BY u.id, u.username, u.name",
                                        deliveryOrgId);

                        // Buscar a entidade Delivery do repositório
                        Long deliveryIdLong = Long.valueOf(targetDeliveryId);
                        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryIdLong);
                        if (deliveryOpt.isEmpty()) {
                                return ResponseEntity
                                                .ok(Map.of("error",
                                                                "Delivery entity não encontrada: " + targetDeliveryId));
                        }

                        Delivery deliveryEntity = deliveryOpt.get();

                        // Simular o envio de notificação com o objeto Delivery completo
                        deliveryNotificationService.notifyAvailableDrivers(deliveryEntity);

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Notificação de delivery testada",
                                        "deliveryId", targetDeliveryId,
                                        "deliveryInfo", deliveryInfo.get(0),
                                        "targetCouriers", couriersWithTokens,
                                        "courierCount", couriersWithTokens.size()));

                } catch (Exception e) {
                        return ResponseEntity.ok(Map.of(
                                        "error", e.getMessage(),
                                        "stackTrace", e.getStackTrace()));
                }
        }

        @GetMapping("/push-tokens-summary")
        public ResponseEntity<?> getPushTokensSummary() {
                try {
                        // Resumo geral de tokens
                        List<Map<String, Object>> tokensSummary = jdbcTemplate.queryForList(
                                        "SELECT " +
                                                        "device_type, " +
                                                        "COUNT(*) as total_tokens, " +
                                                        "COUNT(DISTINCT user_id) as unique_users " +
                                                        "FROM user_push_tokens " +
                                                        "GROUP BY device_type");

                        // Tokens por usuário específico (user com tokens web)
                        List<Map<String, Object>> webTokenUsers = jdbcTemplate.queryForList(
                                        "SELECT " +
                                                        "u.id, u.username, u.name, u.role, " +
                                                        "pt.device_type, pt.id as token_id, " +
                                                        "pt.created_at as token_created " +
                                                        "FROM users u " +
                                                        "JOIN user_push_tokens pt ON u.id = pt.user_id " +
                                                        "WHERE pt.device_type = 'WEB' " +
                                                        "ORDER BY pt.created_at DESC");

                        // Usuários com múltiplos tokens
                        List<Map<String, Object>> multiTokenUsers = jdbcTemplate.queryForList(
                                        "SELECT " +
                                                        "u.id, u.username, u.name, u.role, " +
                                                        "COUNT(pt.id) as token_count, " +
                                                        "STRING_AGG(pt.device_type, ', ') as device_types " +
                                                        "FROM users u " +
                                                        "JOIN user_push_tokens pt ON u.id = pt.user_id " +
                                                        "GROUP BY u.id, u.username, u.name, u.role " +
                                                        "HAVING COUNT(pt.id) > 0 " +
                                                        "ORDER BY token_count DESC");

                        return ResponseEntity.ok(Map.of(
                                        "tokensSummary", tokensSummary,
                                        "webTokenUsers", webTokenUsers,
                                        "multiTokenUsers", multiTokenUsers));

                } catch (Exception e) {
                        return ResponseEntity.ok(Map.of("error", e.getMessage()));
                }
        }

        @GetMapping("/test-delivery-notification")
        public ResponseEntity<Map<String, Object>> testDeliveryNotification(@RequestParam String token) {
                try {
                        // Find the user by token using SQL query
                        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                                        "SELECT u.id, u.username FROM users u " +
                                                        "JOIN user_push_tokens pt ON u.id = pt.user_id " +
                                                        "WHERE pt.token = ?",
                                        token);

                        if (users.isEmpty()) {
                                return ResponseEntity.ok(Map.of("error", "User not found for this token"));
                        }

                        UUID userId = UUID.fromString(users.get(0).get("id").toString());
                        String username = users.get(0).get("username").toString();

                        // Create a test delivery object (just for the notification data)
                        UUID testDeliveryId = UUID.randomUUID();
                        String pickupAddress = "Test Pickup Address";
                        String deliveryAddress = "Test Delivery Address";
                        String price = "15.00";
                        String description = "Test delivery for notification";

                        // Manually trigger the notification system
                        try {
                                // Send direct notification to this user
                                pushNotificationService.sendNotificationToUser(
                                                userId,
                                                "Nova Entrega Disponível!",
                                                "Uma nova entrega de teste está disponível: " + description,
                                                Map.of(
                                                                "type", "new_delivery",
                                                                "deliveryId", testDeliveryId.toString(),
                                                                "price", price,
                                                                "pickup", pickupAddress,
                                                                "delivery", deliveryAddress));

                                return ResponseEntity.ok(Map.of(
                                                "success", true,
                                                "message", "Test notification sent successfully",
                                                "userId", userId.toString(),
                                                "username", username,
                                                "deliveryId", testDeliveryId.toString(),
                                                "notification", Map.of(
                                                                "title", "Nova Entrega Disponível!",
                                                                "body",
                                                                "Uma nova entrega de teste está disponível: "
                                                                                + description,
                                                                "data", Map.of(
                                                                                "type", "new_delivery",
                                                                                "deliveryId", testDeliveryId.toString(),
                                                                                "price", price))));

                        } catch (Exception e) {
                                return ResponseEntity.ok(Map.of(
                                                "error", "Failed to send notification: " + e.getMessage(),
                                                "userId", userId.toString(),
                                                "username", username));
                        }

                } catch (Exception e) {
                        return ResponseEntity.ok(Map.of("error", "General error: " + e.getMessage()));
                }
        }

        @GetMapping("/test-real-web-push")
        public ResponseEntity<Map<String, Object>> testRealWebPush(@RequestParam String tokenId) {
                try {
                        // Find the user by token ID using SQL query
                        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                                        "SELECT u.id, u.username, pt.* FROM users u " +
                                                        "JOIN user_push_tokens pt ON u.id = pt.user_id " +
                                                        "WHERE pt.id::text = ?",
                                        tokenId);

                        if (users.isEmpty()) {
                                return ResponseEntity.ok(Map.of("error", "User not found for this token"));
                        }

                        Map<String, Object> userData = users.get(0);
                        UUID userId = UUID.fromString(userData.get("id").toString());
                        String username = userData.get("username").toString();

                        // Criar objeto UserPushToken do resultado da query
                        UserPushToken pushToken = new UserPushToken();
                        pushToken.setId(UUID.fromString(userData.get("id").toString()));
                        pushToken.setUserId(userId);
                        pushToken.setToken(userData.get("token").toString());
                        pushToken.setPlatform(UserPushToken.Platform.valueOf(userData.get("platform").toString()));
                        pushToken.setDeviceType(
                                        UserPushToken.DeviceType.valueOf(userData.get("device_type").toString()));
                        pushToken.setWebEndpoint(
                                        userData.get("web_endpoint") != null ? userData.get("web_endpoint").toString()
                                                        : null);
                        pushToken.setWebP256dh(
                                        userData.get("web_p256dh") != null ? userData.get("web_p256dh").toString()
                                                        : null);
                        pushToken.setWebAuth(
                                        userData.get("web_auth") != null ? userData.get("web_auth").toString() : null);

                        // Tentar envio direto via WebPushService (forçando modo real)
                        Map<String, Object> testData = Map.of(
                                        "type", "test_real_push",
                                        "deliveryId", UUID.randomUUID().toString(),
                                        "price", "25.00",
                                        "timestamp", System.currentTimeMillis());

                        boolean success = false;
                        String errorMessage = null;

                        try {
                                // Chamada direta ao WebPushService
                                success = sendRealWebPush(pushToken, "🔥 TESTE REAL WEB PUSH!",
                                                "Esta é uma notificação REAL enviada para o browser mobile", testData);
                        } catch (Exception e) {
                                errorMessage = e.getMessage();
                                log.error("Erro ao enviar Web Push real: {}", e.getMessage(), e);
                        }

                        Map<String, Object> response = new HashMap<>();
                        response.put("success", success);
                        response.put("message",
                                        success ? "Web Push REAL enviado com sucesso!" : "Falha no envio Web Push");
                        if (errorMessage != null) {
                                response.put("error", errorMessage);
                        }
                        response.put("userId", userId.toString());
                        response.put("username", username);
                        response.put("tokenType", pushToken.getDeviceType().toString());
                        response.put("platform", pushToken.getPlatform().toString());
                        response.put("hasWebData", pushToken.hasWebPushData());
                        response.put("endpoint", pushToken.getWebEndpoint());

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        log.error("General error in test-real-web-push: {}", e.getMessage(), e);
                        return ResponseEntity.ok(Map.of("error", "General error: "
                                        + (e.getMessage() != null ? e.getMessage() : e.getClass().getName())));
                }
        }

        private boolean sendRealWebPush(UserPushToken token, String title, String body, Map<String, Object> data)
                        throws Exception {
                if (!token.isWebPush() || !token.hasWebPushData()) {
                        throw new IllegalArgumentException("Token não é Web Push ou não tem dados completos");
                }

                // Criar subscription do Web Push
                nl.martijndwars.webpush.Subscription subscription = new nl.martijndwars.webpush.Subscription(
                                token.getWebEndpoint(),
                                new nl.martijndwars.webpush.Subscription.Keys(
                                                token.getWebP256dh(),
                                                token.getWebAuth()));

                // Criar payload da notificação
                Map<String, Object> payload = new HashMap<>();
                payload.put("title", title);
                payload.put("body", body);
                payload.put("data", data);
                payload.put("icon", "/icon-192x192.png");
                payload.put("badge", "/badge-72x72.png");

                String payloadJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);

                // Criar notificação
                nl.martijndwars.webpush.Notification notification = new nl.martijndwars.webpush.Notification(
                                subscription,
                                payloadJson);

                // Enviar usando o PushService
                log.info("🚀 ENVIANDO WEB PUSH REAL: {} -> {}", title, body);
                log.info("📍 ENDPOINT: {}", token.getWebEndpoint());
                log.info("📦 PAYLOAD: {}", payloadJson);

                pushService.send(notification);

                log.info("✅ Web Push REAL enviado com sucesso!");
                return true;
        }
}
