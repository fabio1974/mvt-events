package com.mvt.mvt_events.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * Request simplificado para criar invoice consolidada
 * 
 * <p>Frontend envia apenas IDs das deliveries, backend calcula splits automaticamente</p>
 * 
 * <p><strong>Exemplo:</strong></p>
 * <pre>
 * POST /api/payment/create-invoice
 * {
 *   "deliveryIds": [1, 2, 3, 4, 5],
 *   "clientEmail": "cliente@example.com",
 *   "expirationHours": 24
 * }
 * </pre>
 * 
 * <p><strong>Backend irá:</strong></p>
 * <ol>
 *   <li>Buscar todas as deliveries</li>
 *   <li>Calcular valor total (soma totalAmount)</li>
 *   <li>Agrupar por motoboy/gerente</li>
 *   <li>Calcular splits individuais (87% motoboy, 5% gerente, resto plataforma)</li>
 *   <li>Criar UMA invoice no Iugu com múltiplos splits</li>
 * </ol>
 */
@Data
public class CreateInvoiceRequest {

    /**
     * IDs das deliveries que serão pagas nesta invoice
     */
    @NotEmpty(message = "É necessário informar ao menos uma delivery")
    private List<Long> deliveryIds;

    /**
     * Email do cliente que irá pagar (payer)
     * 
     * <p>Este email será usado para:
     * <ul>
     *   <li>Buscar ou criar um usuário do tipo CLIENT no sistema</li>
     *   <li>Enviar a invoice do Iugu</li>
     *   <li>Associar o pagamento a este cliente</li>
     * </ul>
     */
    @NotBlank(message = "Email do cliente é obrigatório")
    @Email(message = "Email inválido")
    private String clientEmail;

    /**
     * Nome do cliente (opcional, usado se precisar criar novo usuário)
     */
    private String clientName;

    /**
     * CPF do cliente (opcional, usado se precisar criar novo usuário)
     * Se não informado, será gerado um CPF dummy válido
     */
    private String clientCpf;

    /**
     * Horas até expiração da invoice (padrão: 24h)
     */
    @Min(value = 1, message = "Mínimo 1 hora")
    @Max(value = 168, message = "Máximo 7 dias (168 horas)")
    private Integer expirationHours = 24;
}
