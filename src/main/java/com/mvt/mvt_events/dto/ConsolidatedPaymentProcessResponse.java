package com.mvt.mvt_events.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Resposta para requisições assíncronas de consolidação de pagamentos
 * 
 * <p>Fornece informações sobre uma tarefa de processamento em background.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsolidatedPaymentProcessResponse {

    /**
     * ID único da tarefa de processamento
     */
    private String taskId;

    /**
     * Status da tarefa: QUEUED, PROCESSING, COMPLETED, FAILED
     */
    private String status;

    /**
     * Mensagem informativa
     */
    private String message;

    /**
     * Timestamp de quando a tarefa foi iniciada
     */
    private LocalDateTime startedAt;

    /**
     * Timestamp de quando a tarefa foi concluída
     */
    private LocalDateTime completedAt;

    /**
     * Estatísticas gerais de processamento
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> statistics;

    /**
     * Lista de erros ocorridos durante processamento
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> errors;

    /**
     * Progresso em percentual (0-100)
     */
    private Integer progressPercentage;
}
