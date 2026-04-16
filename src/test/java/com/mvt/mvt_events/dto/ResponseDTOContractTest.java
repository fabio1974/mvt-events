package com.mvt.mvt_events.dto;

import com.mvt.mvt_events.controller.UserController.AddressResponseDTO;
import com.mvt.mvt_events.controller.UserController.UserResponse;
import com.mvt.mvt_events.dto.common.CityDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de contrato dos DTOs de resposta.
 * Garante que campos de relacionamento nunca são retornados como String flat.
 * Relacionamentos devem ser objetos com pelo menos {id} — nunca String/Long avulso.
 *
 * Quando um novo DTO quebrar estes testes, significa que um campo de relacionamento
 * foi declarado como String ou Long quando deveria ser um objeto DTO.
 */
class ResponseDTOContractTest {

    /** Campos que representam relacionamentos e DEVEM ser objetos (não String/Long) */
    private static final Set<String> RELATIONSHIP_FIELDS = Set.of(
            "city", "organization", "owner", "client", "courier",
            "waiter", "customer", "evaluator", "evaluated",
            "delivery", "bankAccount", "storeProfile"
    );

    /** Tipos permitidos para campos de relacionamento (devem ser objetos/DTOs) */
    private static final Set<Class<?>> FORBIDDEN_RELATIONSHIP_TYPES = Set.of(
            String.class, Long.class, long.class, Integer.class, int.class
    );

    @Nested
    @DisplayName("AddressResponseDTO")
    class AddressResponseDTOTest {

        @Test
        @DisplayName("city deve ser CityDTO, não String")
        void cityDeveSerObjeto() throws NoSuchFieldException {
            Field cityField = AddressResponseDTO.class.getDeclaredField("city");
            assertThat(cityField.getType())
                    .as("AddressResponseDTO.city deve ser CityDTO, não %s", cityField.getType().getSimpleName())
                    .isEqualTo(CityDTO.class);
        }
    }

    @Nested
    @DisplayName("UserResponse")
    class UserResponseTest {

        @Test
        @DisplayName("city deve ser CityDTO, não String")
        void cityDeveSerObjeto() throws NoSuchFieldException {
            Field cityField = UserResponse.class.getDeclaredField("city");
            assertThat(cityField.getType())
                    .as("UserResponse.city deve ser CityDTO, não %s", cityField.getType().getSimpleName())
                    .isEqualTo(CityDTO.class);
        }

        @Test
        @DisplayName("organization deve ser objeto, não String")
        void organizationDeveSerObjeto() throws NoSuchFieldException {
            Field field = UserResponse.class.getDeclaredField("organization");
            assertThat(FORBIDDEN_RELATIONSHIP_TYPES)
                    .as("UserResponse.organization não pode ser %s", field.getType().getSimpleName())
                    .doesNotContain(field.getType());
        }
    }

    @Nested
    @DisplayName("Varredura genérica de DTOs")
    class GenericScanTest {

        /** Lista de todos os DTOs de resposta que devem ser verificados */
        private final List<Class<?>> RESPONSE_DTOS = List.of(
                AddressResponseDTO.class,
                UserResponse.class
        );

        @Test
        @DisplayName("Nenhum campo de relacionamento pode ser String ou Long em DTOs de resposta")
        void nenhumRelacionamentoComoStringOuLong() {
            for (Class<?> dtoClass : RESPONSE_DTOS) {
                for (Field field : dtoClass.getDeclaredFields()) {
                    if (RELATIONSHIP_FIELDS.contains(field.getName())) {
                        assertThat(FORBIDDEN_RELATIONSHIP_TYPES)
                                .as("%s.%s é %s — relacionamentos devem ser objetos DTO, não tipos primitivos/String",
                                        dtoClass.getSimpleName(), field.getName(), field.getType().getSimpleName())
                                .doesNotContain(field.getType());
                    }
                }
            }
        }

        @Test
        @DisplayName("Campos 'addresses' devem ser List, não String")
        void addressesDeveSerLista() {
            for (Class<?> dtoClass : RESPONSE_DTOS) {
                Arrays.stream(dtoClass.getDeclaredFields())
                        .filter(f -> f.getName().equals("addresses"))
                        .forEach(f -> {
                            assertThat(f.getType())
                                    .as("%s.addresses deve ser List, não %s",
                                            dtoClass.getSimpleName(), f.getType().getSimpleName())
                                    .isAssignableFrom(List.class);
                        });
            }
        }
    }
}
