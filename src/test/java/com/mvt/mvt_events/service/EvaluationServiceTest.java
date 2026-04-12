package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.Evaluation;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.repository.EvaluationRepository;
import com.mvt.mvt_events.repository.UserRepository;
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
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios do EvaluationService -- cobre create, findByDeliveryId,
 * findAll, findReceivedByCourier, getAverageRatingForCourier, findPoorRatings
 * e getRatingDistribution.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock private EvaluationRepository evaluationRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private EvaluationService evaluationService;

    // ========== Helpers ==========

    private final UUID clientId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID courierId = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private User makeUser(UUID id, String name, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setUsername(name.toLowerCase().replace(" ", ".") + "@zapi10.com");
        u.setRole(role);
        return u;
    }

    private Delivery makeCompletedDelivery(Long id) {
        Delivery d = new Delivery();
        d.setId(id);
        d.setClient(makeUser(clientId, "Joao Cliente", User.Role.CUSTOMER));
        d.setCourier(makeUser(courierId, "Pedro Moto", User.Role.COURIER));
        d.setStatus(Delivery.DeliveryStatus.COMPLETED);
        return d;
    }

    private Evaluation makeEvaluation(Evaluation.EvaluationType type, int rating) {
        Evaluation e = new Evaluation();
        e.setEvaluationType(type);
        e.setRating(rating);
        e.setComment("Teste");
        return e;
    }

    // ================================================================
    // create
    // ================================================================

    @Nested
    @DisplayName("create() -- Cria avaliacao")
    class CreateTests {

        @Test
        @DisplayName("Cliente avalia courier com sucesso")
        void clienteAvaliaCourier() {
            Delivery delivery = makeCompletedDelivery(1L);
            Evaluation evaluation = makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 5);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(evaluationRepository.existsByDeliveryId(1L)).thenReturn(false);
            when(userRepository.findById(clientId)).thenReturn(Optional.of(delivery.getClient()));
            when(evaluationRepository.save(any())).thenAnswer(inv -> {
                Evaluation ev = inv.getArgument(0);
                ev.setId(1L);
                return ev;
            });

            Evaluation result = evaluationService.create(evaluation, 1L, clientId);

            assertThat(result.getDelivery()).isSameAs(delivery);
            assertThat(result.getEvaluator()).isSameAs(delivery.getClient());
            assertThat(result.getRating()).isEqualTo(5);
            verify(evaluationRepository).save(evaluation);
        }

        @Test
        @DisplayName("Courier avalia cliente com sucesso")
        void courierAvaliaCliente() {
            Delivery delivery = makeCompletedDelivery(1L);
            Evaluation evaluation = makeEvaluation(Evaluation.EvaluationType.COURIER_TO_CLIENT, 4);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(evaluationRepository.existsByDeliveryId(1L)).thenReturn(false);
            when(userRepository.findById(courierId)).thenReturn(Optional.of(delivery.getCourier()));
            when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Evaluation result = evaluationService.create(evaluation, 1L, courierId);

            assertThat(result.getEvaluator()).isSameAs(delivery.getCourier());
        }

        @Test
        @DisplayName("Lanca excecao quando delivery nao encontrada")
        void deliveryNaoEncontrada() {
            Evaluation evaluation = makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 5);
            when(deliveryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> evaluationService.create(evaluation, 999L, clientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrada");
        }

        @Test
        @DisplayName("Lanca excecao quando delivery nao esta completada")
        void deliveryNaoCompletada() {
            Delivery delivery = makeCompletedDelivery(1L);
            delivery.setStatus(Delivery.DeliveryStatus.IN_TRANSIT);
            Evaluation evaluation = makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 5);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> evaluationService.create(evaluation, 1L, clientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("completada");
        }

        @Test
        @DisplayName("Lanca excecao quando delivery ja possui avaliacao")
        void deliveryJaPossuiAvaliacao() {
            Delivery delivery = makeCompletedDelivery(1L);
            Evaluation evaluation = makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 5);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(evaluationRepository.existsByDeliveryId(1L)).thenReturn(true);

            assertThatThrownBy(() -> evaluationService.create(evaluation, 1L, clientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("já possui avaliação");
        }

        @Test
        @DisplayName("Lanca excecao quando avaliador nao encontrado")
        void avaliadorNaoEncontrado() {
            Delivery delivery = makeCompletedDelivery(1L);
            Evaluation evaluation = makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 5);
            UUID desconhecidoId = UUID.randomUUID();

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(evaluationRepository.existsByDeliveryId(1L)).thenReturn(false);
            when(userRepository.findById(desconhecidoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> evaluationService.create(evaluation, 1L, desconhecidoId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrado");
        }

        @Test
        @DisplayName("Lanca excecao quando cliente tenta avaliar mas nao e dono da delivery")
        void clienteNaoDonoDaDelivery() {
            Delivery delivery = makeCompletedDelivery(1L);
            Evaluation evaluation = makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 5);
            UUID outroClienteId = UUID.randomUUID();
            User outroCliente = makeUser(outroClienteId, "Outro", User.Role.CUSTOMER);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(evaluationRepository.existsByDeliveryId(1L)).thenReturn(false);
            when(userRepository.findById(outroClienteId)).thenReturn(Optional.of(outroCliente));

            assertThatThrownBy(() -> evaluationService.create(evaluation, 1L, outroClienteId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Apenas o cliente");
        }

        @Test
        @DisplayName("Lanca excecao quando courier tenta avaliar mas nao e o courier da delivery")
        void courierNaoDaDelivery() {
            Delivery delivery = makeCompletedDelivery(1L);
            Evaluation evaluation = makeEvaluation(Evaluation.EvaluationType.COURIER_TO_CLIENT, 4);
            UUID outroCourierId = UUID.randomUUID();
            User outroCourier = makeUser(outroCourierId, "Outro Moto", User.Role.COURIER);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(evaluationRepository.existsByDeliveryId(1L)).thenReturn(false);
            when(userRepository.findById(outroCourierId)).thenReturn(Optional.of(outroCourier));

            assertThatThrownBy(() -> evaluationService.create(evaluation, 1L, outroCourierId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Apenas o courier");
        }

        @Test
        @DisplayName("Lanca excecao quando rating menor que 1")
        void ratingMenorQue1() {
            Delivery delivery = makeCompletedDelivery(1L);
            Evaluation evaluation = makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 0);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(evaluationRepository.existsByDeliveryId(1L)).thenReturn(false);
            when(userRepository.findById(clientId)).thenReturn(Optional.of(delivery.getClient()));

            assertThatThrownBy(() -> evaluationService.create(evaluation, 1L, clientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("entre 1 e 5");
        }

        @Test
        @DisplayName("Lanca excecao quando rating maior que 5")
        void ratingMaiorQue5() {
            Delivery delivery = makeCompletedDelivery(1L);
            Evaluation evaluation = makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 6);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(evaluationRepository.existsByDeliveryId(1L)).thenReturn(false);
            when(userRepository.findById(clientId)).thenReturn(Optional.of(delivery.getClient()));

            assertThatThrownBy(() -> evaluationService.create(evaluation, 1L, clientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("entre 1 e 5");
        }
    }

    // ================================================================
    // findByDeliveryId
    // ================================================================

    @Nested
    @DisplayName("findByDeliveryId() -- Busca avaliacao por delivery")
    class FindByDeliveryIdTests {

        @Test
        @DisplayName("Retorna avaliacao quando encontrada")
        void retornaQuandoEncontrada() {
            Evaluation evaluation = makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 5);
            when(evaluationRepository.findByDeliveryId(1L)).thenReturn(Optional.of(evaluation));

            Evaluation result = evaluationService.findByDeliveryId(1L);

            assertThat(result).isSameAs(evaluation);
        }

        @Test
        @DisplayName("Lanca excecao quando nao encontrada")
        void lancaExcecaoQuandoNaoEncontrada() {
            when(evaluationRepository.findByDeliveryId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> evaluationService.findByDeliveryId(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrada");
        }
    }

    // ================================================================
    // findAll
    // ================================================================

    @Nested
    @DisplayName("findAll() -- Lista avaliacoes com filtros")
    class FindAllTests {

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("Retorna pagina de avaliacoes")
        void retornaPagina() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Evaluation> page = new PageImpl<>(List.of(
                    makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 5)
            ));
            when(evaluationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            Page<Evaluation> result = evaluationService.findAll(null, null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    // ================================================================
    // findReceivedByCourier
    // ================================================================

    @Nested
    @DisplayName("findReceivedByCourier() -- Avaliacoes recebidas pelo courier")
    class FindReceivedByCourierTests {

        @Test
        @DisplayName("Retorna avaliacoes recebidas")
        void retornaAvaliacoes() {
            List<Evaluation> evals = List.of(
                    makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 5),
                    makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 4)
            );
            when(evaluationRepository.findReceivedByCourierId(courierId)).thenReturn(evals);

            List<Evaluation> result = evaluationService.findReceivedByCourier(courierId);

            assertThat(result).hasSize(2);
        }
    }

    // ================================================================
    // getAverageRatingForCourier
    // ================================================================

    @Nested
    @DisplayName("getAverageRatingForCourier() -- Rating medio do courier")
    class GetAverageRatingTests {

        @Test
        @DisplayName("Retorna media quando existem avaliacoes")
        void retornaMedia() {
            when(evaluationRepository.calculateAverageRatingForCourier(courierId)).thenReturn(4.5);

            Double result = evaluationService.getAverageRatingForCourier(courierId);

            assertThat(result).isEqualTo(4.5);
        }

        @Test
        @DisplayName("Retorna 0.0 quando nao ha avaliacoes")
        void retornaZeroQuandoNaoHa() {
            when(evaluationRepository.calculateAverageRatingForCourier(courierId)).thenReturn(null);

            Double result = evaluationService.getAverageRatingForCourier(courierId);

            assertThat(result).isEqualTo(0.0);
        }
    }

    // ================================================================
    // findPoorRatings
    // ================================================================

    @Nested
    @DisplayName("findPoorRatings() -- Avaliacoes ruins")
    class FindPoorRatingsTests {

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("Retorna avaliacoes com rating <= 2")
        void retornaAvaliacoesRuins() {
            UUID admId = UUID.randomUUID();
            List<Evaluation> poorEvals = List.of(
                    makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 1),
                    makeEvaluation(Evaluation.EvaluationType.CLIENT_TO_COURIER, 2)
            );
            when(evaluationRepository.findAll(any(Specification.class))).thenReturn(poorEvals);

            List<Evaluation> result = evaluationService.findPoorRatings(admId);

            assertThat(result).hasSize(2);
        }
    }

    // ================================================================
    // getRatingDistribution
    // ================================================================

    @Nested
    @DisplayName("getRatingDistribution() -- Distribuicao de ratings")
    class GetRatingDistributionTests {

        @Test
        @DisplayName("Retorna distribuicao de ratings")
        void retornaDistribuicao() {
            List<Object[]> distribution = List.of(
                    new Object[]{5, 10L},
                    new Object[]{4, 5L},
                    new Object[]{3, 2L}
            );
            when(evaluationRepository.countRatingDistributionForCourier(courierId)).thenReturn(distribution);

            List<Object[]> result = evaluationService.getRatingDistribution(courierId);

            assertThat(result).hasSize(3);
            assertThat(result.get(0)[0]).isEqualTo(5);
            assertThat(result.get(0)[1]).isEqualTo(10L);
        }
    }
}
