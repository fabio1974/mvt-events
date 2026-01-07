package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.util.BrazilianBanks;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller para operações relacionadas a bancos brasileiros.
 * Fornece endpoints públicos para consulta de códigos e nomes de bancos.
 */
@RestController
@RequestMapping("/api/banks")
@Tag(name = "Bancos", description = "Endpoints para consulta de bancos brasileiros")
public class BankController {

    /**
     * Listar todos os bancos brasileiros disponíveis.
     * Endpoint público para uso em selects/dropdowns no frontend/mobile.
     * 
     * @return Lista de bancos com código e nome
     */
    @GetMapping
    @Operation(
            summary = "Listar todos os bancos brasileiros",
            description = "Retorna a lista completa de bancos cadastrados com seus códigos BACEN e nomes. " +
                    "Útil para construir selects/dropdowns de bancos no frontend e mobile."
    )
    public ResponseEntity<List<BankResponse>> listAllBanks() {
        List<BankResponse> banks = BrazilianBanks.getAllBanks().entrySet().stream()
                .map(entry -> BankResponse.builder()
                        .code(entry.getKey())
                        .name(entry.getValue())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(banks);
    }

    /**
     * DTO para resposta com informações de banco
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankResponse {
        /**
         * Código BACEN do banco (3 dígitos)
         * Exemplo: "001", "260", "341"
         */
        private String code;
        
        /**
         * Nome completo do banco
         * Exemplo: "Banco do Brasil", "Nubank (Nu Pagamentos)", "Banco Itaú"
         */
        private String name;
    }
}
