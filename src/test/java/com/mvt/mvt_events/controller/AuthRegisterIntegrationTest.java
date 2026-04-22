package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.jpa.CustomerPaymentPreference;
import com.mvt.mvt_events.jpa.RestaurantTable;
import com.mvt.mvt_events.repository.CustomerPaymentPreferenceRepository;
import com.mvt.mvt_events.repository.OrganizationRepository;
import com.mvt.mvt_events.repository.RestaurantTableRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes de integração do fluxo de registro (AuthController.register).
 *
 * Sobe o Spring real + Postgres real (container mvt-events-db na porta 5436,
 * database dedicada "mvt-events-test") e aplica todas as migrations Flyway.
 * Complementa AuthRegisterTest (unit/mocks) verificando o que só o banco pode
 * dizer: constraints NOT NULL/UNIQUE, migrations, ordem de persistência e
 * transações.
 *
 * Requer: container mvt-events-db rodando + database mvt-events-test criada.
 * Setup uma vez: PGPASSWORD=mvtpass psql -h localhost -p 5436 -U mvt -d postgres
 *                -c "CREATE DATABASE \"mvt-events-test\";"
 *                PGPASSWORD=mvtpass psql -h localhost -p 5436 -U mvt
 *                -d mvt-events-test -c "CREATE EXTENSION IF NOT EXISTS postgis;"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@DisplayName("AuthController.register() — integração full-stack com Postgres real")
class AuthRegisterIntegrationTest {

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private CustomerPaymentPreferenceRepository paymentPreferenceRepository;
    @Autowired private RestaurantTableRepository restaurantTableRepository;

    /**
     * Mocka o EmailService para não tentar enviar email de confirmação durante os testes.
     */
    @TestConfiguration
    static class MockMailConfig {
        @Bean
        @Primary
        EmailService emailService() {
            return mock(EmailService.class);
        }
    }

    @AfterEach
    void cleanup() {
        // Limpa tudo criado em cada teste pra garantir isolamento
        restaurantTableRepository.deleteAll();
        paymentPreferenceRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    /** CPFs válidos gerados — um por teste pra não colidir se validator checar unicidade. */
    private static final String[] CPF_POOL = {
            "39053344705", "12345678909", "11144477735",
            "52998224725", "98765432100", "01234567890",
            "33366699911", "87654321098",
    };
    private int cpfIdx = 0;
    private String nextCpf() {
        return CPF_POOL[cpfIdx++ % CPF_POOL.length];
    }

    private Map<String, Object> registerRequest(String email, String name, String role) {
        Map<String, Object> req = new HashMap<>();
        req.put("username", email);
        req.put("password", "Senha@123");
        req.put("name", name);
        req.put("role", role);
        req.put("documentNumber", nextCpf());
        return req;
    }

    // ================================================================
    // CADA ROLE — persistência real + efeitos colaterais corretos
    // ================================================================

    @Nested
    @DisplayName("Registro por role")
    class PorRole {

        @Test
        @DisplayName("CUSTOMER: persiste User com role correto e sem entidades extras")
        void customer() {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url("/auth/register"), registerRequest("cli@demo.com", "Cliente PF", "CUSTOMER"), Map.class);

            assertThat(resp.getStatusCode().is2xxSuccessful())
                    .as("Register deveria retornar 2xx, voltou %s com body %s", resp.getStatusCode(), resp.getBody())
                    .isTrue();

            List<User> users = userRepository.findAll();
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getRole()).isEqualTo(User.Role.CUSTOMER);
            assertThat(users.get(0).getUsername()).isEqualTo("cli@demo.com");
            assertThat(users.get(0).getPassword()).isNotEqualTo("Senha@123"); // hashed
            assertThat(organizationRepository.count()).isZero();
            assertThat(paymentPreferenceRepository.count()).isZero();
        }

        @Test
        @DisplayName("COURIER: persiste User com role correto e sem entidades extras")
        void courier() {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url("/auth/register"), registerRequest("moto@demo.com", "Pedro Moto", "COURIER"), Map.class);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

            List<User> users = userRepository.findAll();
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getRole()).isEqualTo(User.Role.COURIER);
            assertThat(organizationRepository.count()).isZero();
            assertThat(paymentPreferenceRepository.count()).isZero();
        }

        @Test
        @DisplayName("WAITER: persiste User com role correto e sem entidades extras")
        void waiter() {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url("/auth/register"), registerRequest("ana@demo.com", "Ana Garçom", "WAITER"), Map.class);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

            List<User> users = userRepository.findAll();
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getRole()).isEqualTo(User.Role.WAITER);
            assertThat(organizationRepository.count()).isZero();
            assertThat(paymentPreferenceRepository.count()).isZero();
        }

        @Test
        @DisplayName("CLIENT: persiste User + PaymentPreference PIX + mesa Balcão")
        void client() {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url("/auth/register"), registerRequest("loja@demo.com", "Pizza Express", "CLIENT"), Map.class);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

            List<User> users = userRepository.findAll();
            assertThat(users).hasSize(1);
            User client = users.get(0);
            assertThat(client.getRole()).isEqualTo(User.Role.CLIENT);

            // PaymentPreference PIX criada
            List<CustomerPaymentPreference> prefs = paymentPreferenceRepository.findAll();
            assertThat(prefs).hasSize(1);
            assertThat(prefs.get(0).getPreferredPaymentType())
                    .isEqualTo(CustomerPaymentPreference.PreferredPaymentType.PIX);

            // Mesa Balcão (number=0) criada
            List<RestaurantTable> tables = restaurantTableRepository.findAll();
            assertThat(tables).hasSize(1);
            assertThat(tables.get(0).getNumber()).isZero();

            // Organization NÃO é criada pra CLIENT
            assertThat(organizationRepository.count()).isZero();
        }

        @Test
        @DisplayName("ORGANIZER: persiste User + Organization (sem slug — removido em V129)")
        void organizer() {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url("/auth/register"), registerRequest("gerente@demo.com", "Fabio Gerente", "ORGANIZER"), Map.class);

            assertThat(resp.getStatusCode().is2xxSuccessful())
                    .as("ORGANIZER deve registrar sem erro de constraint (bug do slug corrigido)")
                    .isTrue();

            List<User> users = userRepository.findAll();
            assertThat(users).hasSize(1);
            User organizer = users.get(0);
            assertThat(organizer.getRole()).isEqualTo(User.Role.ORGANIZER);

            // Organization criada com owner = este ORGANIZER
            List<Organization> orgs = organizationRepository.findAll();
            assertThat(orgs).hasSize(1);
            Organization org = orgs.get(0);
            assertThat(org.getName()).isEqualTo("Grupo de Fabio Gerente");
            assertThat(org.getOwner()).isNotNull();
            assertThat(org.getOwner().getId()).isEqualTo(organizer.getId());

            assertThat(paymentPreferenceRepository.count()).isZero();
        }
    }

    // ================================================================
    // Unicidade — constraint do banco
    // ================================================================

    @Nested
    @DisplayName("Unicidade de email")
    class Unicidade {

        @Test
        @DisplayName("Segundo registro com mesmo email retorna erro")
        void emailDuplicado() {
            restTemplate.postForEntity(url("/auth/register"),
                    registerRequest("dup@demo.com", "Primeiro", "CUSTOMER"), Map.class);

            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url("/auth/register"), registerRequest("dup@demo.com", "Segundo", "CUSTOMER"), Map.class);

            assertThat(resp.getStatusCode().is4xxClientError())
                    .as("Segundo registro deveria falhar com 4xx")
                    .isTrue();
            assertThat(userRepository.findAll()).hasSize(1);
        }
    }

    // ================================================================
    // Validações
    // ================================================================

    @Nested
    @DisplayName("Validações")
    class Validacoes {

        @Test
        @DisplayName("Role inválido retorna erro 4xx (não 500)")
        void roleInvalido() {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url("/auth/register"), registerRequest("x@y.com", "Fulano", "HACKER"), Map.class);

            assertThat(resp.getStatusCode().is4xxClientError())
                    .as("Role inválido deve voltar 4xx, não deve explodir em 500")
                    .isTrue();
            assertThat(userRepository.count()).isZero();
        }

        @Test
        @DisplayName("Resposta de registro NÃO vaza a senha")
        void respostaSemSenha() {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url("/auth/register"), registerRequest("secret@demo.com", "Fulano", "CUSTOMER"), Map.class);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            Map body = resp.getBody();
            assertThat(body).as("Response deve existir").isNotNull();
            assertThat(body.toString())
                    .as("Response JSON não pode conter senha em claro nem hash")
                    .doesNotContain("Senha@123")
                    .doesNotContainIgnoringCase("password");
        }
    }
}
