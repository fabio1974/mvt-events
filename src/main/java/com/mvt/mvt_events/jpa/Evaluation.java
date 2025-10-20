package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Avaliações de entregas.
 * Permite que clientes avaliem o serviço do motoboy e vice-versa.
 * Relacionamento 1:1 com Delivery (uma avaliação por entrega).
 */
@Entity
@Table(name = "evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Evaluation extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @NotNull(message = "Entrega é obrigatória")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false, unique = true)
    @Visible(table = false, form = false, filter = true)
    private Delivery delivery;

    @NotNull(message = "Avaliador é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", nullable = false)
    @Visible(table = true, form = false, filter = true)
    private User evaluator;

    @NotNull(message = "Avaliado é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluated_id", nullable = false)
    @Visible(table = true, form = false, filter = true)
    private User evaluated;

    // ============================================================================
    // EVALUATION DATA
    // ============================================================================

    @NotNull(message = "Nota é obrigatória")
    @Min(value = 1, message = "Nota mínima é 1")
    @Max(value = 5, message = "Nota máxima é 5")
    @Column(nullable = false)
    @Visible(table = true, form = true, filter = true)
    private Integer rating;

    @Size(max = 500, message = "Comentário deve ter no máximo 500 caracteres")
    @Column(columnDefinition = "TEXT")
    @Visible(table = false, form = true, filter = false)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type", nullable = false, length = 20)
    @Visible(table = true, form = false, filter = true)
    private EvaluationType evaluationType;

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum EvaluationType {
        CLIENT_TO_COURIER, // Cliente avaliando motoboy
        COURIER_TO_CLIENT // Motoboy avaliando cliente
    }
}
