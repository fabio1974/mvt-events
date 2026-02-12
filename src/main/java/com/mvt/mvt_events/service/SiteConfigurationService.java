package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.SiteConfiguration;
import com.mvt.mvt_events.repository.SiteConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service para gerenciar configurações do site
 */
@Service
public class SiteConfigurationService {

    @Autowired
    private SiteConfigurationRepository siteConfigurationRepository;

    /**
     * Retorna a configuração ativa do site
     * Se não existir, cria uma com valores padrão
     */
    public SiteConfiguration getActiveConfiguration() {
        return siteConfigurationRepository.findActiveConfiguration()
                .orElseGet(this::createDefaultConfiguration);
    }

    /**
     * Cria configuração padrão se não existir
     */
    @Transactional
    private SiteConfiguration createDefaultConfiguration() {
        SiteConfiguration defaultConfig = SiteConfiguration.builder()
                .pricePerKm(BigDecimal.valueOf(1.00))            // R$ 1,00 por km (moto)
                .carPricePerKm(BigDecimal.valueOf(2.00))         // R$ 2,00 por km (automóvel)
                .minimumShippingFee(BigDecimal.valueOf(5.00))    // R$ 5,00 mínimo (moto)
                .carMinimumShippingFee(BigDecimal.valueOf(8.00)) // R$ 8,00 mínimo (automóvel)
                .organizerPercentage(BigDecimal.valueOf(5.00))   // 5% para o gerente
                .platformPercentage(BigDecimal.valueOf(10.00))   // 10% para a plataforma
                .dangerFeePercentage(BigDecimal.valueOf(0.00))   // 0% taxa de periculosidade
                .highIncomeFeePercentage(BigDecimal.valueOf(0.00)) // 0% taxa de renda alta
                .creditCardFeePercentage(BigDecimal.valueOf(0.00)) // 0% taxa de cartão de crédito
                .isActive(true)
                .notes("Configuração padrão criada automaticamente")
                .updatedBy("SYSTEM")
                .build();
        
        return siteConfigurationRepository.save(defaultConfig);
    }

    /**
     * Atualiza a configuração ativa
     * Desativa todas as outras configurações e ativa a nova
     */
    @Transactional
    public SiteConfiguration updateConfiguration(SiteConfiguration newConfig, String updatedBy) {
        // Validar percentuais (soma não pode exceder 100%)
        BigDecimal totalPercentage = newConfig.getOrganizerPercentage()
                .add(newConfig.getPlatformPercentage());
        
        if (totalPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("Soma dos percentuais não pode exceder 100%");
        }

        // Desativar todas as configurações existentes
        List<SiteConfiguration> allConfigs = siteConfigurationRepository.findAll();
        allConfigs.forEach(config -> config.setIsActive(false));
        siteConfigurationRepository.saveAll(allConfigs);

        // Criar nova configuração ativa
        newConfig.setIsActive(true);
        newConfig.setUpdatedBy(updatedBy);
        
        return siteConfigurationRepository.save(newConfig);
    }

    /**
     * Lista todas as configurações (histórico)
     */
    public List<SiteConfiguration> getAllConfigurations() {
        return siteConfigurationRepository.findAll();
    }

    /**
     * Lista todas as configurações com paginação
     */
    public Page<SiteConfiguration> getAllConfigurationsPaged(Pageable pageable, Boolean isActive) {
        if (isActive != null) {
            return siteConfigurationRepository.findByIsActive(isActive, pageable);
        }
        return siteConfigurationRepository.findAll(pageable);
    }

    /**
     * Busca configuração por ID
     */
    public SiteConfiguration findById(Long id) {
        return siteConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuração não encontrada"));
    }

    /**
     * Salva uma configuração (para PUT ou POST)
     */
    @Transactional
    public SiteConfiguration save(SiteConfiguration config) {
        // Se está ativando esta configuração, desativar as outras
        if (config.getIsActive() != null && config.getIsActive()) {
            List<SiteConfiguration> allConfigs = siteConfigurationRepository.findAll();
            allConfigs.stream()
                    .filter(c -> c.getId() == null || !c.getId().equals(config.getId()))
                    .forEach(c -> c.setIsActive(false));
            siteConfigurationRepository.saveAll(allConfigs);
        }
        
        return siteConfigurationRepository.save(config);
    }
}
