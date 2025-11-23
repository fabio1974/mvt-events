package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.SiteConfiguration;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.service.SiteConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gerenciar configurações do site
 * Apenas ADMINs podem alterar configurações
 */
@RestController
@RequestMapping("/api/site-configuration")
public class SiteConfigurationController {

    @Autowired
    private SiteConfigurationService siteConfigurationService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Lista todas as configurações com paginação (ordenado por data DESC)
     * Público - qualquer usuário autenticado pode ver
     */
    @GetMapping
    public ResponseEntity<Page<SiteConfiguration>> getAllConfigurations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isActive) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SiteConfiguration> configs = siteConfigurationService.getAllConfigurationsPaged(pageable, isActive);
        return ResponseEntity.ok(configs);
    }

    /**
     * Retorna apenas a configuração ativa
     * Público - qualquer usuário autenticado pode ver
     */
    @GetMapping("/active")
    public ResponseEntity<SiteConfiguration> getActiveConfiguration() {
        SiteConfiguration config = siteConfigurationService.getActiveConfiguration();
        return ResponseEntity.ok(config);
    }

    /**
     * Lista todas as configurações (histórico sem paginação)
     * Apenas ADMIN
     */
    @GetMapping("/history")
    public ResponseEntity<List<SiteConfiguration>> getAllConfigurationsHistory(Authentication authentication) {
        // Validar se é ADMIN
        validateAdmin(authentication);
        
        List<SiteConfiguration> configs = siteConfigurationService.getAllConfigurations();
        return ResponseEntity.ok(configs);
    }

    /**
     * Cria uma nova configuração ou atualiza como nova versão
     * Apenas ADMIN
     */
    @PostMapping
    public ResponseEntity<SiteConfiguration> createConfiguration(
            @RequestBody SiteConfiguration newConfig,
            Authentication authentication) {
        
        // Validar se é ADMIN
        User admin = validateAdmin(authentication);
        
        // Se isActive não foi especificado, assumir false (não ativar automaticamente)
        if (newConfig.getIsActive() == null) {
            newConfig.setIsActive(false);
        }
        
        // Definir quem criou
        newConfig.setUpdatedBy(admin.getUsername());
        
        // Se está marcando como ativa, usar o método que desativa as outras
        if (newConfig.getIsActive()) {
            SiteConfiguration updated = siteConfigurationService.updateConfiguration(
                    newConfig, 
                    admin.getUsername()
            );
            return ResponseEntity.ok(updated);
        }
        
        // Caso contrário, apenas salvar como inativa
        SiteConfiguration saved = siteConfigurationService.save(newConfig);
        return ResponseEntity.ok(saved);
    }

    /**
     * Busca configuração específica por ID
     * Apenas ADMIN
     */
    @GetMapping("/{id}")
    public ResponseEntity<SiteConfiguration> getConfigurationById(
            @PathVariable Long id,
            Authentication authentication) {
        
        // Validar se é ADMIN
        validateAdmin(authentication);
        
        SiteConfiguration config = siteConfigurationService.findById(id);
        return ResponseEntity.ok(config);
    }

    /**
     * Atualiza uma configuração existente por ID
     * Apenas ADMIN
     */
    @PutMapping("/{id}")
    public ResponseEntity<SiteConfiguration> updateConfigurationById(
            @PathVariable Long id,
            @RequestBody SiteConfiguration updatedConfig,
            Authentication authentication) {
        
        // Validar se é ADMIN
        User admin = validateAdmin(authentication);
        
        // Buscar configuração existente
        SiteConfiguration existingConfig = siteConfigurationService.findById(id);
        
        // Atualizar campos
        existingConfig.setPricePerKm(updatedConfig.getPricePerKm());
        existingConfig.setMinimumShippingFee(updatedConfig.getMinimumShippingFee());
        existingConfig.setOrganizerPercentage(updatedConfig.getOrganizerPercentage());
        existingConfig.setPlatformPercentage(updatedConfig.getPlatformPercentage());
        existingConfig.setDangerFeePercentage(updatedConfig.getDangerFeePercentage());
        existingConfig.setHighIncomeFeePercentage(updatedConfig.getHighIncomeFeePercentage());
        existingConfig.setIsActive(updatedConfig.getIsActive());
        existingConfig.setNotes(updatedConfig.getNotes());
        existingConfig.setUpdatedBy(admin.getUsername());
        
        // Salvar
        SiteConfiguration saved = siteConfigurationService.save(existingConfig);
        
        return ResponseEntity.ok(saved);
    }

    /**
     * Valida se o usuário é ADMIN
     */
    private User validateAdmin(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Apenas ADMIN pode gerenciar configurações do site");
        }
        
        return user;
    }
}
