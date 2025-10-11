package com.mvt.mvt_events.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

        @Bean
        public OpenAPI mvtEventsOpenAPI() {
                final String securitySchemeName = "bearerAuth";

                return new OpenAPI()
                                .info(new Info()
                                                .title("MVT Events API")
                                                .version("1.0.0")
                                                .description("""
                                                                # API Completa para Gerenciamento de Eventos Esportivos

                                                                ## 🎯 Recursos Principais

                                                                - **Autenticação JWT**: Todas as rotas protegidas requerem token Bearer
                                                                - **Multi-tenant**: Isolamento automático por organização
                                                                - **Filtros Avançados**: Todos os campos podem ser filtrados via query parameters
                                                                - **Paginação**: Suporte a `page`, `size` e `sort` em todas as listagens
                                                                - **Metadata Automático**: Endpoint `/api/{entity}/metadata` com campos, tipos e traduções PT-BR

                                                                ## 🔍 Sistema de Filtros

                                                                Cada entidade suporta filtros por **TODOS os seus campos** via query parameters:

                                                                ### Events (`/api/events`)
                                                                - **Texto**: `name`, `slug`, `description`, `location`
                                                                - **Enums**: `eventType` (RUNNING, CYCLING, SWIMMING, TRIATHLON, WALKING, OTHER), `status` (DRAFT, PUBLISHED, CANCELLED, COMPLETED), `currency` (BRL, USD, EUR), `transferFrequency`
                                                                - **Relacionamentos**: `organization` (ID), `category` (ID), `city` (nome ou ID), `state` (sigla)
                                                                - **Datas**: `eventDate`, `registrationStartDate`, `registrationEndDate`
                                                                - **Numéricos**: `maxParticipants`, `price`
                                                                - **Booleanos**: `registrationOpen`

                                                                ### Registrations (`/api/registrations`)
                                                                - **Relacionamentos**: `user` (UUID), `event` (ID)
                                                                - **Enums**: `status` (PENDING, CONFIRMED, CANCELLED, WAITLISTED)
                                                                - **Datas**: `registrationDate`
                                                                - **Texto**: `notes`

                                                                ### Event Categories (`/api/event-categories`)
                                                                - **Relacionamentos**: `event` (ID)
                                                                - **Texto**: `name`
                                                                - **Enums**: `gender` (MALE, FEMALE, MIXED), `distanceUnit` (METERS, KILOMETERS, MILES)
                                                                - **Numéricos**: `minAge`, `maxAge`, `distance`, `price`, `maxParticipants`, `currentParticipants`

                                                                ### Payments (`/api/payments`)
                                                                - **Relacionamentos**: `registration` (ID)
                                                                - **Enums**: `status` (PENDING, COMPLETED, FAILED, REFUNDED), `paymentMethod` (CREDIT_CARD, DEBIT_CARD, PIX, BOLETO, PAYPAL), `currency`
                                                                - **Numéricos**: `amount`, `gatewayFee`, `refundAmount`
                                                                - **Texto**: `gatewayProvider`, `gatewayPaymentId`, `refundReason`
                                                                - **Datas**: `processedAt`, `refundedAt`

                                                                ### Organizations (`/api/organizations`)
                                                                - **Texto**: `name`, `contactEmail`, `contactPhone`, `website`
                                                                - **Enums**: `type` (SPORTS_CLUB, EVENT_ORGANIZER, FEDERATION, GOVERNMENT, NGO, PRIVATE_COMPANY, OTHER)
                                                                - **Booleanos**: `active`

                                                                ## 📝 Exemplos de Uso

                                                                ```
                                                                # Filtrar eventos por tipo e nome
                                                                GET /api/events?eventType=RUNNING&name=maratona

                                                                # Filtrar por cidade, estado e status
                                                                GET /api/events?city=São Paulo&state=SP&status=PUBLISHED

                                                                # Combinar múltiplos filtros
                                                                GET /api/events?eventType=RUNNING&registrationOpen=true&organization=5

                                                                # Paginação e ordenação
                                                                GET /api/events?page=0&size=20&sort=eventDate,desc

                                                                # Filtrar registrations por usuário e status
                                                                GET /api/registrations?user=742f58ea-5bc1-4bb5-84dc-5ea463d15044&status=CONFIRMED

                                                                # Filtrar payments por status e valor
                                                                GET /api/payments?status=COMPLETED&amount=100.00

                                                                # Buscar organizações ativas
                                                                GET /api/organizations?active=true&type=SPORTS_CLUB

                                                                # Obter metadata de uma entidade
                                                                GET /api/events/metadata
                                                                ```

                                                                ## 🔐 Autenticação

                                                                1. Fazer login em `/api/auth/login` com email e senha
                                                                2. Copiar o token JWT retornado
                                                                3. Clicar em "Authorize" 🔓 no topo da página
                                                                4. Colar o token (sem 'Bearer ')
                                                                5. Testar os endpoints
                                                                """)
                                                .contact(new Contact()
                                                                .name("MVT Events Team")
                                                                .email("support@mvt-events.com"))
                                                .license(new License()
                                                                .name("Apache 2.0")
                                                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                                .components(new Components()
                                                .addSecuritySchemes(securitySchemeName,
                                                                new SecurityScheme()
                                                                                .name(securitySchemeName)
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("Insira o token JWT obtido no endpoint /api/auth/login (sem 'Bearer ')")));
        }
}
