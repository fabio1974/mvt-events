package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.SiteConfiguration;
import com.mvt.mvt_events.repository.SiteConfigurationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios do SiteConfigurationService -- cobre getActiveConfiguration,
 * updateConfiguration, getAllConfigurations, getAllConfigurationsPaged, findById e save.
 */
@ExtendWith(MockitoExtension.class)
class SiteConfigurationServiceTest {

    @Mock
    private SiteConfigurationRepository siteConfigurationRepository;

    @InjectMocks
    private SiteConfigurationService siteConfigurationService;

    // ========== Helpers ==========

    private SiteConfiguration makeConfig(Long id, boolean active) {
        SiteConfiguration c = SiteConfiguration.builder()
                .pricePerKm(BigDecimal.valueOf(1.00))
                .carPricePerKm(BigDecimal.valueOf(2.00))
                .minimumShippingFee(BigDecimal.valueOf(5.00))
                .carMinimumShippingFee(BigDecimal.valueOf(8.00))
                .organizerPercentage(BigDecimal.valueOf(5.00))
                .platformPercentage(BigDecimal.valueOf(10.00))
                .dangerFeePercentage(BigDecimal.ZERO)
                .highIncomeFeePercentage(BigDecimal.ZERO)
                .creditCardFeePercentage(BigDecimal.ZERO)
                .isActive(active)
                .updatedBy("SYSTEM")
                .build();
        c.setId(id);
        return c;
    }

    // ================================================================
    // getActiveConfiguration
    // ================================================================

    @Nested
    @DisplayName("getActiveConfiguration() -- Busca configuracao ativa")
    class GetActiveConfigurationTests {

        @Test
        @DisplayName("Retorna configuracao existente quando encontrada")
        void retornaExistente() {
            SiteConfiguration existing = makeConfig(1L, true);
            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.of(existing));

            SiteConfiguration result = siteConfigurationService.getActiveConfiguration();

            assertThat(result).isSameAs(existing);
            verify(siteConfigurationRepository).findActiveConfiguration();
            verify(siteConfigurationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cria configuracao padrao quando nao existe nenhuma")
        void criaPadraoQuandoNaoExiste() {
            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.empty());
            when(siteConfigurationRepository.save(any(SiteConfiguration.class)))
                    .thenAnswer(inv -> {
                        SiteConfiguration c = inv.getArgument(0);
                        c.setId(1L);
                        return c;
                    });

            SiteConfiguration result = siteConfigurationService.getActiveConfiguration();

            assertThat(result.getIsActive()).isTrue();
            assertThat(result.getPricePerKm()).isEqualByComparingTo("1.00");
            assertThat(result.getCarPricePerKm()).isEqualByComparingTo("2.00");
            assertThat(result.getMinimumShippingFee()).isEqualByComparingTo("5.00");
            assertThat(result.getCarMinimumShippingFee()).isEqualByComparingTo("8.00");
            assertThat(result.getOrganizerPercentage()).isEqualByComparingTo("5.00");
            assertThat(result.getPlatformPercentage()).isEqualByComparingTo("10.00");
            assertThat(result.getUpdatedBy()).isEqualTo("SYSTEM");
            verify(siteConfigurationRepository).save(any(SiteConfiguration.class));
        }
    }

    // ================================================================
    // updateConfiguration
    // ================================================================

    @Nested
    @DisplayName("updateConfiguration() -- Atualiza configuracao ativa")
    class UpdateConfigurationTests {

        @Test
        @DisplayName("Desativa todas as existentes e ativa a nova")
        void desativaExistentesAtivaNoiva() {
            SiteConfiguration old1 = makeConfig(1L, true);
            SiteConfiguration old2 = makeConfig(2L, false);
            when(siteConfigurationRepository.findAll()).thenReturn(List.of(old1, old2));
            when(siteConfigurationRepository.saveAll(anyList())).thenReturn(List.of(old1, old2));

            SiteConfiguration newConfig = makeConfig(null, false);
            newConfig.setOrganizerPercentage(BigDecimal.valueOf(10));
            newConfig.setPlatformPercentage(BigDecimal.valueOf(15));
            when(siteConfigurationRepository.save(any(SiteConfiguration.class))).thenAnswer(inv -> inv.getArgument(0));

            SiteConfiguration result = siteConfigurationService.updateConfiguration(newConfig, "admin@zapi10.com");

            assertThat(result.getIsActive()).isTrue();
            assertThat(result.getUpdatedBy()).isEqualTo("admin@zapi10.com");
            assertThat(old1.getIsActive()).isFalse();
            assertThat(old2.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Lanca excecao quando soma dos percentuais excede 100%")
        void rejeitaPercentuaisExcedendo100() {
            SiteConfiguration newConfig = makeConfig(null, false);
            newConfig.setOrganizerPercentage(BigDecimal.valueOf(60));
            newConfig.setPlatformPercentage(BigDecimal.valueOf(50));

            assertThatThrownBy(() -> siteConfigurationService.updateConfiguration(newConfig, "admin"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("exceder 100%");
        }

        @Test
        @DisplayName("Aceita percentuais que somam exatamente 100%")
        void aceitaPercentuaisExatos100() {
            SiteConfiguration newConfig = makeConfig(null, false);
            newConfig.setOrganizerPercentage(BigDecimal.valueOf(50));
            newConfig.setPlatformPercentage(BigDecimal.valueOf(50));

            when(siteConfigurationRepository.findAll()).thenReturn(List.of());
            when(siteConfigurationRepository.saveAll(anyList())).thenReturn(List.of());
            when(siteConfigurationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SiteConfiguration result = siteConfigurationService.updateConfiguration(newConfig, "admin");

            assertThat(result.getIsActive()).isTrue();
        }
    }

    // ================================================================
    // getAllConfigurations
    // ================================================================

    @Nested
    @DisplayName("getAllConfigurations() -- Lista todas")
    class GetAllConfigurationsTests {

        @Test
        @DisplayName("Retorna todas as configuracoes")
        void retornaTodas() {
            List<SiteConfiguration> configs = List.of(makeConfig(1L, true), makeConfig(2L, false));
            when(siteConfigurationRepository.findAll()).thenReturn(configs);

            List<SiteConfiguration> result = siteConfigurationService.getAllConfigurations();

            assertThat(result).hasSize(2);
        }
    }

    // ================================================================
    // getAllConfigurationsPaged
    // ================================================================

    @Nested
    @DisplayName("getAllConfigurationsPaged() -- Lista com paginacao")
    class GetAllConfigurationsPagedTests {

        @Test
        @DisplayName("Filtra por isActive quando parametro nao e null")
        void filtraPorIsActive() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SiteConfiguration> page = new PageImpl<>(List.of(makeConfig(1L, true)));
            when(siteConfigurationRepository.findByIsActive(true, pageable)).thenReturn(page);

            Page<SiteConfiguration> result = siteConfigurationService.getAllConfigurationsPaged(pageable, true);

            assertThat(result.getContent()).hasSize(1);
            verify(siteConfigurationRepository).findByIsActive(true, pageable);
            verify(siteConfigurationRepository, never()).findAll(pageable);
        }

        @Test
        @DisplayName("Retorna todas quando isActive e null")
        void retornaTodasQuandoIsActiveNull() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SiteConfiguration> page = new PageImpl<>(List.of(makeConfig(1L, true), makeConfig(2L, false)));
            when(siteConfigurationRepository.findAll(pageable)).thenReturn(page);

            Page<SiteConfiguration> result = siteConfigurationService.getAllConfigurationsPaged(pageable, null);

            assertThat(result.getContent()).hasSize(2);
            verify(siteConfigurationRepository).findAll(pageable);
        }
    }

    // ================================================================
    // findById
    // ================================================================

    @Nested
    @DisplayName("findById() -- Busca por ID")
    class FindByIdTests {

        @Test
        @DisplayName("Retorna configuracao quando encontrada")
        void retornaQuandoEncontrada() {
            SiteConfiguration config = makeConfig(1L, true);
            when(siteConfigurationRepository.findById(1L)).thenReturn(Optional.of(config));

            SiteConfiguration result = siteConfigurationService.findById(1L);

            assertThat(result).isSameAs(config);
        }

        @Test
        @DisplayName("Lanca excecao quando nao encontrada")
        void lancaExcecaoQuandoNaoEncontrada() {
            when(siteConfigurationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> siteConfigurationService.findById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrada");
        }
    }

    // ================================================================
    // save
    // ================================================================

    @Nested
    @DisplayName("save() -- Salva configuracao")
    class SaveTests {

        @Test
        @DisplayName("Desativa outras quando ativando nova configuracao")
        void desativaOutrasQuandoAtivando() {
            SiteConfiguration existing = makeConfig(1L, true);
            SiteConfiguration newConfig = makeConfig(null, true);

            when(siteConfigurationRepository.findAll()).thenReturn(List.of(existing));
            when(siteConfigurationRepository.saveAll(anyList())).thenReturn(List.of(existing));
            when(siteConfigurationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SiteConfiguration result = siteConfigurationService.save(newConfig);

            assertThat(existing.getIsActive()).isFalse();
            verify(siteConfigurationRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Nao desativa outras quando salvando configuracao inativa")
        void naoDesativaQuandoInativa() {
            SiteConfiguration newConfig = makeConfig(null, false);
            when(siteConfigurationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            siteConfigurationService.save(newConfig);

            verify(siteConfigurationRepository, never()).findAll();
            verify(siteConfigurationRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Nao desativa a propria configuracao ao salvar")
        void naoDesativaPropriaAoSalvar() {
            SiteConfiguration config = makeConfig(5L, true);
            SiteConfiguration other = makeConfig(3L, true);

            when(siteConfigurationRepository.findAll()).thenReturn(List.of(config, other));
            when(siteConfigurationRepository.saveAll(anyList())).thenReturn(List.of(config, other));
            when(siteConfigurationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            siteConfigurationService.save(config);

            // A propria config nao deve ter sido desativada
            assertThat(config.getIsActive()).isTrue();
            // A outra sim
            assertThat(other.getIsActive()).isFalse();
        }
    }
}
