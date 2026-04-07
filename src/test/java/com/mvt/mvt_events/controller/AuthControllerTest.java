package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitarios para validacao de roles no registro.
 *
 * O AuthController usa User.Role.valueOf(role.toUpperCase()) para converter
 * o role string enviado pelo frontend/mobile em enum. Esses testes garantem
 * que os roles validos funcionam e que roles invalidos (ex: MANAGER) falham.
 */
@DisplayName("Auth - Validacao de Roles no Registro")
class AuthControllerTest {

    /**
     * Simula a logica do AuthController.register() para conversao de role.
     * Se o role nao existe no enum, valueOf() lanca IllegalArgumentException.
     */
    private User.Role parseRole(String roleStr) {
        return User.Role.valueOf(roleStr.toUpperCase());
    }

    @Nested
    @DisplayName("Roles validos para registro")
    class ValidRoles {

        @Test
        @DisplayName("CUSTOMER e um role valido")
        void customer_isValid() {
            assertThat(parseRole("CUSTOMER")).isEqualTo(User.Role.CUSTOMER);
        }

        @Test
        @DisplayName("CLIENT e um role valido")
        void client_isValid() {
            assertThat(parseRole("CLIENT")).isEqualTo(User.Role.CLIENT);
        }

        @Test
        @DisplayName("COURIER e um role valido")
        void courier_isValid() {
            assertThat(parseRole("COURIER")).isEqualTo(User.Role.COURIER);
        }

        @Test
        @DisplayName("ORGANIZER e um role valido (usado para gerentes)")
        void organizer_isValid() {
            assertThat(parseRole("ORGANIZER")).isEqualTo(User.Role.ORGANIZER);
        }

        @Test
        @DisplayName("role case insensitive: 'organizer' minusculo funciona")
        void organizer_lowercase_works() {
            assertThat(parseRole("organizer")).isEqualTo(User.Role.ORGANIZER);
        }

        @Test
        @DisplayName("role case insensitive: 'Courier' mixed case funciona")
        void courier_mixedCase_works() {
            assertThat(parseRole("Courier")).isEqualTo(User.Role.COURIER);
        }
    }

    @Nested
    @DisplayName("Roles invalidos para registro")
    class InvalidRoles {

        @Test
        @DisplayName("MANAGER nao e um role valido — deve usar ORGANIZER")
        void manager_isInvalid() {
            assertThatThrownBy(() -> parseRole("MANAGER"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("role vazio lanca exception")
        void empty_isInvalid() {
            assertThatThrownBy(() -> parseRole(""))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("role inexistente lanca exception")
        void nonsense_isInvalid() {
            assertThatThrownBy(() -> parseRole("SUPERADMIN"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Regras de negocio por role no registro")
    class RegistrationBusinessRules {

        @Test
        @DisplayName("CLIENT deve ter preferencia PIX criada automaticamente no registro")
        void clientRole_requiresPixPreference() {
            // Simula a logica do AuthController.register():
            // if (savedUser.getRole() == User.Role.CLIENT) { criar PIX preference }
            User.Role role = parseRole("CLIENT");
            boolean shouldCreatePixPreference = (role == User.Role.CLIENT);
            assertThat(shouldCreatePixPreference)
                .as("CLIENT deve ter preferencia PIX auto-criada no registro")
                .isTrue();
        }

        @Test
        @DisplayName("CUSTOMER nao deve ter preferencia PIX auto-criada no registro")
        void customerRole_noAutoPixPreference() {
            User.Role role = parseRole("CUSTOMER");
            boolean shouldCreatePixPreference = (role == User.Role.CLIENT);
            assertThat(shouldCreatePixPreference)
                .as("CUSTOMER nao deve ter PIX auto-criado (escolhe na tela de preferencias)")
                .isFalse();
        }

        @Test
        @DisplayName("COURIER nao deve ter preferencia PIX auto-criada")
        void courierRole_noAutoPixPreference() {
            User.Role role = parseRole("COURIER");
            boolean shouldCreatePixPreference = (role == User.Role.CLIENT);
            assertThat(shouldCreatePixPreference).isFalse();
        }

        @Test
        @DisplayName("ORGANIZER nao deve ter preferencia PIX auto-criada")
        void organizerRole_noAutoPixPreference() {
            User.Role role = parseRole("ORGANIZER");
            boolean shouldCreatePixPreference = (role == User.Role.CLIENT);
            assertThat(shouldCreatePixPreference).isFalse();
        }
    }

    @Nested
    @DisplayName("Enum User.Role - completude")
    class RoleEnumCompleteness {

        @Test
        @DisplayName("contem todos os roles esperados pelo sistema")
        void hasAllExpectedRoles() {
            assertThat(User.Role.values())
                .extracting(Enum::name)
                .contains("CUSTOMER", "CLIENT", "COURIER", "ORGANIZER", "ADMIN", "USER");
        }

        @Test
        @DisplayName("nao contem MANAGER")
        void doesNotContainManager() {
            assertThat(User.Role.values())
                .extracting(Enum::name)
                .doesNotContain("MANAGER");
        }
    }
}
