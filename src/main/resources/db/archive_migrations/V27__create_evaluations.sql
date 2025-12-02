-- ============================================================================
-- V27: Criar tabela evaluations
-- ============================================================================
-- Avaliações de entregas
-- Permite que clientes avaliem couriers e vice-versa
-- Relacionamento 1:1 com deliveries
-- ============================================================================

CREATE TABLE evaluations (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Relacionamentos
    delivery_id BIGINT NOT NULL UNIQUE,
    evaluator_id UUID NOT NULL,
    evaluated_id UUID NOT NULL,
    
    -- Avaliação
    rating INTEGER NOT NULL,
    comment TEXT,
    evaluation_type VARCHAR(20) NOT NULL,
    
    -- Foreign Keys
    CONSTRAINT fk_evaluation_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE,
    CONSTRAINT fk_evaluation_evaluator FOREIGN KEY (evaluator_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_evaluation_evaluated FOREIGN KEY (evaluated_id) REFERENCES users(id) ON DELETE RESTRICT,
    
    -- Constraints
    CONSTRAINT chk_evaluation_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT chk_evaluation_type CHECK (evaluation_type IN ('CLIENT_TO_COURIER', 'COURIER_TO_CLIENT'))
);

-- Índices
CREATE INDEX idx_evaluation_delivery ON evaluations(delivery_id);
CREATE INDEX idx_evaluation_evaluator ON evaluations(evaluator_id);
CREATE INDEX idx_evaluation_evaluated ON evaluations(evaluated_id);
CREATE INDEX idx_evaluation_rating ON evaluations(rating);
CREATE INDEX idx_evaluation_type ON evaluations(evaluation_type);

-- Comentários
COMMENT ON TABLE evaluations IS 'Avaliações de entregas (1:1 com deliveries)';
COMMENT ON COLUMN evaluations.rating IS 'Nota de 1 a 5 estrelas';
COMMENT ON COLUMN evaluations.evaluation_type IS 'CLIENT_TO_COURIER (cliente avalia motoboy) ou COURIER_TO_CLIENT (vice-versa)';
