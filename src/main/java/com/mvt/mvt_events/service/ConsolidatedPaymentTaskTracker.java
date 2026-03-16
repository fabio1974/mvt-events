package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.ConsolidatedPaymentProcessResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rastreador de tarefas assíncronas de consolidação de pagamentos
 * 
 * <p>Mantém estado de tasks em memória para permitir que o frontend
 * consulte o status de um processamento disparado anteriormente.</p>
 */
@Slf4j
@Component
public class ConsolidatedPaymentTaskTracker {

    private final Map<String, ConsolidatedPaymentProcessResponse> tasks = new ConcurrentHashMap<>();

    /**
     * Cria uma nova tarefa no estado QUEUED
     */
    public String createTask() {
        String taskId = UUID.randomUUID().toString();
        ConsolidatedPaymentProcessResponse response = new ConsolidatedPaymentProcessResponse();
        response.setTaskId(taskId);
        response.setStatus("QUEUED");
        response.setMessage("Tarefa enfileirada para processamento");
        response.setStartedAt(OffsetDateTime.now(ZoneId.of("America/Fortaleza")));
        response.setProgressPercentage(0);

        tasks.put(taskId, response);
        log.info("📌 Nova tarefa criada: {}", taskId);
        return taskId;
    }

    /**
     * Marca a tarefa como iniciada
     */
    public void markAsProcessing(String taskId) {
        ConsolidatedPaymentProcessResponse task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("PROCESSING");
            task.setMessage("Processamento em andamento");
            task.setStartedAt(OffsetDateTime.now(ZoneId.of("America/Fortaleza")));
            task.setProgressPercentage(10);
            log.info("▶️ Tarefa marcada como PROCESSING: {}", taskId);
        }
    }

    /**
     * Atualiza progresso da tarefa
     */
    public void updateProgress(String taskId, int percentage, String message) {
        ConsolidatedPaymentProcessResponse task = tasks.get(taskId);
        if (task != null) {
            task.setProgressPercentage(percentage);
            task.setMessage(message);
            log.debug("📊 Progresso atualizado - Tarefa: {} - {}%: {}", taskId, percentage, message);
        }
    }

    /**
     * Marca a tarefa como concluída com sucesso
     */
    public void markAsCompleted(String taskId, Map<String, Object> statistics) {
        ConsolidatedPaymentProcessResponse task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("COMPLETED");
            task.setMessage("Processamento concluído com sucesso");
            task.setCompletedAt(OffsetDateTime.now(ZoneId.of("America/Fortaleza")));
            task.setStatistics(statistics);
            task.setProgressPercentage(100);
            log.info("✅ Tarefa marcada como COMPLETED: {}", taskId);
        }
    }

    /**
     * Marca a tarefa como falhada
     */
    public void markAsFailed(String taskId, String errorMessage, List<String> errors) {
        ConsolidatedPaymentProcessResponse task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("FAILED");
            task.setMessage(errorMessage);
            task.setCompletedAt(OffsetDateTime.now(ZoneId.of("America/Fortaleza")));
            task.setErrors(errors);
            task.setProgressPercentage(0);
            log.error("❌ Tarefa marcada como FAILED: {} - {}", taskId, errorMessage);
        }
    }

    /**
     * Recupera o status de uma tarefa
     */
    public ConsolidatedPaymentProcessResponse getTaskStatus(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * Verifica se a tarefa existe
     */
    public boolean taskExists(String taskId) {
        return tasks.containsKey(taskId);
    }

    /**
     * Remove tarefa (limpeza após expiração)
     */
    public void removeTask(String taskId) {
        tasks.remove(taskId);
        log.debug("🗑️ Tarefa removida: {}", taskId);
    }

    /**
     * Limpa tarefas antigas (completadas há mais de 24h)
     */
    public void cleanupOldTasks() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneId.of("America/Fortaleza")).minusHours(24);
        tasks.entrySet().removeIf(entry -> {
            OffsetDateTime completed = entry.getValue().getCompletedAt();
            return completed != null && completed.isBefore(cutoff);
        });
        log.info("🧹 Limpeza de tarefas antigas concluída");
    }

    /**
     * Retorna todas as tarefas ativas (para debug)
     */
    public List<ConsolidatedPaymentProcessResponse> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
}
