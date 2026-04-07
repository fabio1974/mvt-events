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
                                                                # Zapi10 — API de Mobilidade e Entregas

                                                                ## Recursos Principais

                                                                - **Autenticação JWT**: Todas as rotas protegidas requerem token Bearer
                                                                - **Multi-tenant**: Isolamento automático por organização
                                                                - **Filtros Avançados**: Todos os campos podem ser filtrados via query parameters
                                                                - **Paginação**: Suporte a `page`, `size` e `sort` em todas as listagens
                                                                - **Metadata Automático**: Endpoint `/api/{entity}/metadata` com campos, tipos e traduções PT-BR

                                                                ## Sistema de Filtros

                                                                Cada entidade suporta filtros por **TODOS os seus campos** via query parameters.

                                                                ### Deliveries (`/api/deliveries`)
                                                                - **Enums**: `status` (PENDING, WAITING_PAYMENT, ACCEPTED, IN_TRANSIT, COMPLETED, CANCELLED), `deliveryType` (DELIVERY, RIDE), `preferredVehicleType` (MOTORCYCLE, CAR, ANY)
                                                                - **Relacionamentos**: `client` (UUID), `courier` (UUID), `organizer` (UUID), `vehicle` (ID)
                                                                - **Texto**: `fromAddress`, `toAddress`

                                                                ### Payments (`/api/payments`)
                                                                - **Enums**: `status` (PENDING, PAID, FAILED, REFUNDED, CANCELLED), `paymentMethod` (CREDIT_CARD, PIX, CASH)
                                                                - **Numéricos**: `amount`

                                                                ### Users (`/api/users`)
                                                                - **Enums**: `role` (ADMIN, CLIENT, CUSTOMER, COURIER, ORGANIZER)
                                                                - **Texto**: `name`, `email`, `documentNumber`

                                                                ## Exemplos de Uso

                                                                ```
                                                                # Filtrar entregas por status
                                                                GET /api/deliveries?status=PENDING&page=0&size=20

                                                                # Filtrar entregas de um cliente
                                                                GET /api/deliveries?client=uuid-do-cliente

                                                                # Obter metadata de uma entidade
                                                                GET /api/deliveries/metadata
                                                                ```

                                                                ## Autenticação

                                                                1. Fazer login em `/api/auth/login` com email e senha
                                                                2. Copiar o token JWT retornado
                                                                3. Clicar em "Authorize" no topo da página
                                                                4. Colar o token (sem 'Bearer ')
                                                                """)
                                                .contact(new Contact()
                                                                .name("Zapi10")
                                                                .email("suporte@zapi10.com.br"))
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
