package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.service.ConsolidatedPaymentService;
import com.mvt.mvt_events.service.ConsolidatedPaymentTaskTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller REST para gerenciamento de pagamentos consolidados
 * 
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>POST /api/consolidated-payments/process-all - Processa pagamentos consolidados para todos os clientes</li>
 * </ul>
 * 
 * <p><strong>Autenticação:</strong> Requer token JWT válido</p>
 * <p><strong>Autorização:</strong> ADMIN apenas</p>
 * 
 * <p><strong>Fluxo de consolidação:</strong></p>
 * <ol>
 *   <li>Para cada CLIENT com deliveries COMPLETED não pagas:</li>
 *   <li>Filtra deliveries que têm payments NULL, FAILED ou EXPIRED</li>
 *   <li>Agrupa por COURIER + ORGANIZER</li>
 *   <li>Cria splits automáticos com percentuais da SiteConfiguration</li>
 *   <li>Cria pagamento consolidado no Pagar.me</li>
 *   <li>Associa payment a todas as deliveries envolvidas</li>
 * </ol>
 * 
 * @see ConsolidatedPaymentService
 */
@Slf4j
@RestController
@RequestMapping("/api/consolidated-payments")
@RequiredArgsConstructor
@Tag(name = "Consolidated Payments", description = "Gerenciamento de pagamentos consolidados para múltiplas deliveries")
public class ConsolidatedPaymentController {

    private final ConsolidatedPaymentService consolidatedPaymentService;
    private final ConsolidatedPaymentTaskTracker taskTracker;

    /**
     * Processa pagamentos consolidados para todos os clientes em thread separada
     * 
     * <p>Este endpoint dispara o processamento de consolidação de pagamentos em uma thread
     * separada do backend, permitindo que o frontend continue funcionando normalmente.
     * A resposta retorna imediatamente com status 202 (Accepted), indicando que o
     * processamento foi enfileirado.</p>
     * 
     * <p><strong>Fluxo:</strong></p>
     * <ol>
     *   <li>Endpoint recebe requisição (POST /api/consolidated-payments/process-all)</li>
     *   <li>Valida autenticação (requer ADMIN)</li>
     *   <li>Cria tarefa no tracker (status QUEUED)</li>
     *   <li>Dispara processamento em CompletableFuture (thread separada)</li>
     *   <li>Retorna 202 ACCEPTED com ID da tarefa</li>
     *   <li>Backend continua processando em background</li>
     *   <li>Frontend pode consultar status via GET /api/consolidated-payments/status/{taskId}</li>
     * </ol>
     * 
     * <p><strong>Resposta de sucesso (202 Accepted):</strong></p>
     * <pre>
     * {
     *   "taskId": "5fa2c5d0-1234-4567-89ab-cdef01234567",
     *   "status": "QUEUED",
     *   "message": "Tarefa enfileirada para processamento",
     *   "progressPercentage": 0
     * }
     * </pre>
     * 
     * <p><strong>Exemplo de uso no frontend:</strong></p>
     * <pre>
     * // 1. Disparar processamento
     * const response = await fetch('/api/consolidated-payments/process-all', {
     *   method: 'POST',
     *   headers: {
     *     'Authorization': `Bearer ${token}`,
     *     'Content-Type': 'application/json'
     *   }
     * });
     * 
     * const { taskId } = await response.json();
     * console.log('Processamento iniciado:', taskId);
     * 
     * // 2. Polling para verificar status
     * const pollStatus = async () => {
     *   const statusResponse = await fetch(
     *     `/api/consolidated-payments/status/${taskId}`,
     *     { headers: { 'Authorization': `Bearer ${token}` } }
     *   );
     *   return await statusResponse.json();
     * };
     * 
     * // Verificar a cada 5 segundos
     * const interval = setInterval(async () => {
     *   const status = await pollStatus();
     *   console.log(`Progresso: ${status.progressPercentage}%`);
     *   
     *   if (status.status === 'COMPLETED' || status.status === 'FAILED') {
     *     clearInterval(interval);
     *     console.log('Processamento finalizado:', status);
     *   }
     * }, 5000);
     * </pre>
     * 
     * @return ResponseEntity com status 202 e informações da tarefa
     */
    @PostMapping("/process-all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Processa pagamentos consolidados para todos os clientes",
        description = "Dispara processamento assíncrono de consolidação de pagamentos em thread separada. " +
                      "Retorna imediatamente com status 202 (Accepted).",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> processAllClientsPayments() {
        log.info("🚀 Requisição recebida para processar pagamentos consolidados");

        // Criar tarefa no tracker
        String taskId = taskTracker.createTask();
        log.info("📌 Tarefa criada: {}", taskId);
        
        // Disparar em thread separada (CompletableFuture)
        CompletableFuture.runAsync(() -> {
            try {
                log.info("📋 Iniciando processamento em thread separada - TaskID: {}", taskId);
                consolidatedPaymentService.processAllClientsConsolidatedPayments(taskId);
                log.info("✅ Processamento concluído - TaskID: {}", taskId);
            } catch (Exception e) {
                log.error("❌ Erro durante processamento assíncrono - TaskID: {}", taskId, e);
                taskTracker.markAsFailed(taskId, "Erro durante processamento", List.of(e.getMessage()));
            }
        });

        // Obter status da tarefa recém-criada
        Map<String, Object> response = Map.of(
            "taskId", taskId,
            "status", "QUEUED",
            "message", "Processamento de pagamentos consolidados enfileirado",
            "progressPercentage", 0
        );

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(response);
    }

    /**
     * Verifica o status de uma tarefa de processamento
     * 
     * @param taskId ID da tarefa a verificar
     * @return Status completo da tarefa (QUEUED, PROCESSING, COMPLETED, FAILED)
     */
    @GetMapping("/status/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Verifica o status de uma tarefa de consolidação",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getTaskStatus(@PathVariable String taskId) {
        log.debug("📊 Consultando status da tarefa: {}", taskId);

        if (!taskTracker.taskExists(taskId)) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "error", "Tarefa não encontrada",
                    "taskId", taskId
                ));
        }

        return ResponseEntity.ok(taskTracker.getTaskStatus(taskId));
    }
}
