package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.SiteConfiguration;
import com.mvt.mvt_events.jpa.SpecialZone;
import com.mvt.mvt_events.repository.SpecialZoneRepository;
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
 * Testes unitarios do SpecialZoneService -- cobre findNearestZone,
 * findWorstZoneAcrossStops, save, findById, findAll, findActiveZones,
 * findActiveZonesByType e delete.
 */
@ExtendWith(MockitoExtension.class)
class SpecialZoneServiceTest {

    @Mock
    private SpecialZoneRepository specialZoneRepository;

    @InjectMocks
    private SpecialZoneService specialZoneService;

    // ========== Helpers ==========

    private SpecialZone makeZone(Long id, SpecialZone.ZoneType type) {
        SpecialZone z = SpecialZone.builder()
                .latitude(-3.72)
                .longitude(-38.52)
                .address("Zona Teste")
                .zoneType(type)
                .radiusMeters(300.0)
                .isActive(true)
                .build();
        z.setId(id);
        return z;
    }

    private SiteConfiguration makeConfig() {
        return SiteConfiguration.builder()
                .dangerFeePercentage(BigDecimal.valueOf(20))
                .highIncomeFeePercentage(BigDecimal.valueOf(10))
                .pricePerKm(BigDecimal.ONE)
                .carPricePerKm(BigDecimal.valueOf(2))
                .minimumShippingFee(BigDecimal.valueOf(5))
                .carMinimumShippingFee(BigDecimal.valueOf(8))
                .organizerPercentage(BigDecimal.valueOf(5))
                .platformPercentage(BigDecimal.valueOf(10))
                .creditCardFeePercentage(BigDecimal.ZERO)
                .build();
    }

    // ================================================================
    // findNearestZone
    // ================================================================

    @Nested
    @DisplayName("findNearestZone() -- Busca zona mais proxima")
    class FindNearestZoneTests {

        @Test
        @DisplayName("Retorna zona quando destino esta dentro do raio")
        void retornaZonaQuandoDentroDorRaio() {
            SpecialZone zone = makeZone(1L, SpecialZone.ZoneType.DANGER);
            when(specialZoneRepository.findNearestZoneWithinRadius(-3.72, -38.52))
                    .thenReturn(Optional.of(zone));

            Optional<SpecialZone> result = specialZoneService.findNearestZone(-3.72, -38.52);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(zone);
        }

        @Test
        @DisplayName("Retorna empty quando nao ha zona no raio")
        void retornaEmptyQuandoForaDoRaio() {
            when(specialZoneRepository.findNearestZoneWithinRadius(anyDouble(), anyDouble()))
                    .thenReturn(Optional.empty());

            Optional<SpecialZone> result = specialZoneService.findNearestZone(-3.80, -38.60);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Retorna empty quando latitude e null")
        void retornaEmptyQuandoLatitudeNull() {
            Optional<SpecialZone> result = specialZoneService.findNearestZone(null, -38.52);

            assertThat(result).isEmpty();
            verify(specialZoneRepository, never()).findNearestZoneWithinRadius(anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("Retorna empty quando longitude e null")
        void retornaEmptyQuandoLongitudeNull() {
            Optional<SpecialZone> result = specialZoneService.findNearestZone(-3.72, null);

            assertThat(result).isEmpty();
            verify(specialZoneRepository, never()).findNearestZoneWithinRadius(anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("Retorna empty quando ambos sao null")
        void retornaEmptyQuandoAmbosNull() {
            Optional<SpecialZone> result = specialZoneService.findNearestZone(null, null);

            assertThat(result).isEmpty();
        }
    }

    // ================================================================
    // findWorstZoneAcrossStops
    // ================================================================

    @Nested
    @DisplayName("findWorstZoneAcrossStops() -- Zona com pior taxa entre multiplas paradas")
    class FindWorstZoneAcrossStopsTests {

        @Test
        @DisplayName("Retorna zona DANGER quando tem maior percentual que HIGH_INCOME")
        void retornaDangerQuandoMaiorPercentual() {
            SiteConfiguration config = makeConfig(); // danger=20%, highIncome=10%
            SpecialZone dangerZone = makeZone(1L, SpecialZone.ZoneType.DANGER);
            SpecialZone highIncomeZone = makeZone(2L, SpecialZone.ZoneType.HIGH_INCOME);

            when(specialZoneRepository.findNearestZoneWithinRadius(-3.72, -38.52))
                    .thenReturn(Optional.of(dangerZone));
            when(specialZoneRepository.findNearestZoneWithinRadius(-3.73, -38.53))
                    .thenReturn(Optional.of(highIncomeZone));

            List<double[]> coords = List.of(
                    new double[]{-3.72, -38.52},
                    new double[]{-3.73, -38.53}
            );

            Optional<SpecialZone> result = specialZoneService.findWorstZoneAcrossStops(coords, config);

            assertThat(result).isPresent();
            assertThat(result.get().getZoneType()).isEqualTo(SpecialZone.ZoneType.DANGER);
        }

        @Test
        @DisplayName("Retorna HIGH_INCOME quando tem maior percentual que DANGER")
        void retornaHighIncomeQuandoMaiorPercentual() {
            SiteConfiguration config = makeConfig();
            config.setDangerFeePercentage(BigDecimal.valueOf(5));
            config.setHighIncomeFeePercentage(BigDecimal.valueOf(15));

            SpecialZone dangerZone = makeZone(1L, SpecialZone.ZoneType.DANGER);
            SpecialZone highIncomeZone = makeZone(2L, SpecialZone.ZoneType.HIGH_INCOME);

            when(specialZoneRepository.findNearestZoneWithinRadius(-3.72, -38.52))
                    .thenReturn(Optional.of(dangerZone));
            when(specialZoneRepository.findNearestZoneWithinRadius(-3.73, -38.53))
                    .thenReturn(Optional.of(highIncomeZone));

            List<double[]> coords = List.of(
                    new double[]{-3.72, -38.52},
                    new double[]{-3.73, -38.53}
            );

            Optional<SpecialZone> result = specialZoneService.findWorstZoneAcrossStops(coords, config);

            assertThat(result).isPresent();
            assertThat(result.get().getZoneType()).isEqualTo(SpecialZone.ZoneType.HIGH_INCOME);
        }

        @Test
        @DisplayName("Retorna empty quando nenhuma parada cai em zona especial")
        void retornaEmptyQuandoNenhumaParadaEmZona() {
            SiteConfiguration config = makeConfig();

            when(specialZoneRepository.findNearestZoneWithinRadius(anyDouble(), anyDouble()))
                    .thenReturn(Optional.empty());

            List<double[]> coords = List.of(
                    new double[]{-3.72, -38.52},
                    new double[]{-3.73, -38.53}
            );

            Optional<SpecialZone> result = specialZoneService.findWorstZoneAcrossStops(coords, config);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Retorna zona unica quando apenas uma parada esta em zona")
        void retornaZonaUnica() {
            SiteConfiguration config = makeConfig();
            SpecialZone zone = makeZone(1L, SpecialZone.ZoneType.DANGER);

            when(specialZoneRepository.findNearestZoneWithinRadius(-3.72, -38.52))
                    .thenReturn(Optional.of(zone));
            when(specialZoneRepository.findNearestZoneWithinRadius(-3.73, -38.53))
                    .thenReturn(Optional.empty());

            List<double[]> coords = List.of(
                    new double[]{-3.72, -38.52},
                    new double[]{-3.73, -38.53}
            );

            Optional<SpecialZone> result = specialZoneService.findWorstZoneAcrossStops(coords, config);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(zone);
        }

        @Test
        @DisplayName("Ignora zona com percentual zero")
        void ignoraZonaComPercentualZero() {
            SiteConfiguration config = makeConfig();
            config.setDangerFeePercentage(BigDecimal.ZERO);
            config.setHighIncomeFeePercentage(BigDecimal.ZERO);

            SpecialZone zone = makeZone(1L, SpecialZone.ZoneType.DANGER);
            when(specialZoneRepository.findNearestZoneWithinRadius(-3.72, -38.52))
                    .thenReturn(Optional.of(zone));

            List<double[]> coords = List.of(new double[]{-3.72, -38.52});

            Optional<SpecialZone> result = specialZoneService.findWorstZoneAcrossStops(coords, config);

            // percentual zero nao e > 0, entao nao e "pior"
            assertThat(result).isEmpty();
        }
    }

    // ================================================================
    // save
    // ================================================================

    @Nested
    @DisplayName("save() -- Salva zona especial")
    class SaveTests {

        @Test
        @DisplayName("Salva zona com sucesso")
        void salvaComSucesso() {
            SpecialZone zone = makeZone(null, SpecialZone.ZoneType.DANGER);
            when(specialZoneRepository.save(any())).thenAnswer(inv -> {
                SpecialZone z = inv.getArgument(0);
                z.setId(1L);
                return z;
            });

            SpecialZone result = specialZoneService.save(zone);

            assertThat(result.getId()).isEqualTo(1L);
            verify(specialZoneRepository).save(zone);
        }
    }

    // ================================================================
    // findById
    // ================================================================

    @Nested
    @DisplayName("findById() -- Busca por ID")
    class FindByIdTests {

        @Test
        @DisplayName("Retorna zona quando encontrada")
        void retornaQuandoEncontrada() {
            SpecialZone zone = makeZone(1L, SpecialZone.ZoneType.HIGH_INCOME);
            when(specialZoneRepository.findById(1L)).thenReturn(Optional.of(zone));

            SpecialZone result = specialZoneService.findById(1L);

            assertThat(result).isSameAs(zone);
        }

        @Test
        @DisplayName("Lanca excecao quando nao encontrada")
        void lancaExcecaoQuandoNaoEncontrada() {
            when(specialZoneRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> specialZoneService.findById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrada");
        }
    }

    // ================================================================
    // findAll
    // ================================================================

    @Nested
    @DisplayName("findAll() -- Lista com paginacao")
    class FindAllTests {

        @Test
        @DisplayName("Retorna pagina de zonas")
        void retornaPagina() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SpecialZone> page = new PageImpl<>(List.of(makeZone(1L, SpecialZone.ZoneType.DANGER)));
            when(specialZoneRepository.findAll(pageable)).thenReturn(page);

            Page<SpecialZone> result = specialZoneService.findAll(pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    // ================================================================
    // findActiveZones / findActiveZonesByType
    // ================================================================

    @Nested
    @DisplayName("findActiveZones() / findActiveZonesByType()")
    class FindActiveTests {

        @Test
        @DisplayName("Retorna apenas zonas ativas")
        void retornaAtivas() {
            List<SpecialZone> zones = List.of(makeZone(1L, SpecialZone.ZoneType.DANGER));
            when(specialZoneRepository.findByIsActive(true)).thenReturn(zones);

            List<SpecialZone> result = specialZoneService.findActiveZones();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Retorna zonas ativas filtradas por tipo")
        void retornaAtivasPorTipo() {
            List<SpecialZone> zones = List.of(makeZone(1L, SpecialZone.ZoneType.HIGH_INCOME));
            when(specialZoneRepository.findByIsActiveAndZoneType(true, SpecialZone.ZoneType.HIGH_INCOME))
                    .thenReturn(zones);

            List<SpecialZone> result = specialZoneService.findActiveZonesByType(SpecialZone.ZoneType.HIGH_INCOME);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getZoneType()).isEqualTo(SpecialZone.ZoneType.HIGH_INCOME);
        }
    }

    // ================================================================
    // delete
    // ================================================================

    @Nested
    @DisplayName("delete() -- Deleta zona")
    class DeleteTests {

        @Test
        @DisplayName("Deleta zona por ID")
        void deletaPorId() {
            specialZoneService.delete(1L);

            verify(specialZoneRepository).deleteById(1L);
        }
    }
}
