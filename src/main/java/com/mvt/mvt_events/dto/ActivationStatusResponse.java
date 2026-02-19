package com.mvt.mvt_events.dto;

import com.mvt.mvt_events.jpa.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO de resposta para status de ativação do usuário.
 * Retorna informações sobre o que está faltando para o usuário estar completamente habilitado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationStatusResponse {

    /**
     * Se o usuário está completamente habilitado para usar o sistema
     */
    private Boolean enabled;

    /**
     * Role do usuário (COURIER, CUSTOMER, etc.)
     */
    private User.Role role;

    /**
     * Lista de itens obrigatórios que estão faltando
     * Exemplos: "vehicle", "bankAccount", "withdrawalSettings", "phone", "paymentMethod"
     */
    private List<String> missing;

    /**
     * Mensagens amigáveis descrevendo cada item faltante
     * Chave: nome do item (ex: "vehicle")
     * Valor: mensagem (ex: "Cadastre um veículo")
     */
    private Map<String, String> messages;

    /**
     * Lista de itens opcionais/sugeridos que ajudariam a melhorar a experiência
     * Exemplos: "defaultAddress"
     */
    private List<String> suggested;
}
