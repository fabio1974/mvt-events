package com.mvt.mvt_events.startup;

import com.mvt.mvt_events.jpa.City;
import com.mvt.mvt_events.repository.CityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Loads all Brazilian cities from IBGE API on application startup
 * This runs after the application context is loaded and database migrations are
 * complete
 */
@Component
public class CityDataLoader implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(CityDataLoader.class);
    private static final String IBGE_CITIES_API_URL = "https://servicodados.ibge.gov.br/api/v1/localidades/municipios";

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        logger.info("🇧🇷 Iniciando verificação das cidades brasileiras...");

        // Check current cities count
        long existingCitiesCount = cityRepository.count();

        if (existingCitiesCount >= 5570) {
            logger.info("✅ Cidades já carregadas no banco: {} registros (>= 5570). Não é necessário recarregar.",
                    existingCitiesCount);
            return;
        }

        if (existingCitiesCount > 0) {
            logger.warn("⚠️ Tabela de cidades incompleta: {} registros (< 5570). Limpando e recarregando...",
                    existingCitiesCount);
            clearCitiesTable();
        } else {
            logger.info("📊 Tabela de cidades vazia. Iniciando carregamento completo...");
        }

        try {
            loadAllCitiesFromIBGE();
        } catch (Exception e) {
            logger.error("❌ Erro ao carregar cidades do IBGE: {}", e.getMessage(), e);
            // Don't throw exception to prevent application startup failure
            // You might want to set a flag or schedule a retry mechanism
        }
    }

    private void clearCitiesTable() {
        logger.info("🧹 Limpando tabela de cidades...");
        try {
            cityRepository.deleteAll();
            logger.info("✅ Tabela de cidades limpa com sucesso");
        } catch (Exception e) {
            logger.error("❌ Erro ao limpar tabela de cidades: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void loadAllCitiesFromIBGE() {
        logger.info("📡 Buscando dados de todas as cidades brasileiras na API do IBGE...");

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    IBGE_CITIES_API_URL,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            List<Map<String, Object>> citiesData = response.getBody();

            if (citiesData == null || citiesData.isEmpty()) {
                logger.warn("⚠️ Nenhum dado de cidade retornado pela API do IBGE");
                return;
            }

            logger.info("📊 Recebidos {} municípios do IBGE. Processando e salvando...", citiesData.size());

            int batchSize = 100;
            int processedCount = 0;
            int savedCount = 0;

            // Process cities in batches for better performance
            for (int i = 0; i < citiesData.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, citiesData.size());
                List<Map<String, Object>> batch = citiesData.subList(i, endIndex);

                for (Map<String, Object> cityData : batch) {
                    try {
                        City city = mapToCityEntity(cityData);
                        if (city != null) {
                            cityRepository.save(city);
                            savedCount++;
                        }
                        processedCount++;
                    } catch (Exception e) {
                        logger.warn("⚠️ Erro ao processar cidade: {} - {}", cityData.get("nome"), e.getMessage());
                    }
                }

                // Log progress every batch
                if ((i / batchSize + 1) % 10 == 0) {
                    logger.info("📈 Progresso: {} cidades processadas, {} salvas", processedCount, savedCount);
                }

                // Small delay to avoid overwhelming the database
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            logger.info("✅ Carregamento concluído! {} cidades processadas, {} salvas no banco de dados",
                    processedCount, savedCount);

            // Validate final count
            if (savedCount >= 5570) {
                logger.info("🎯 Validação: {} cidades carregadas - Base completa de municípios brasileiros!",
                        savedCount);
            } else {
                logger.warn(
                        "⚠️ Validação: Apenas {} cidades carregadas (esperado >= 5570). Pode haver problemas na API do IBGE.",
                        savedCount);
            }

            // Log summary by state
            logStateSummary();

        } catch (Exception e) {
            logger.error("❌ Erro ao carregar cidades do IBGE: {}", e.getMessage(), e);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private City mapToCityEntity(Map<String, Object> cityData) {
        try {
            String cityName = (String) cityData.get("nome");
            if (cityName == null || cityName.trim().isEmpty()) {
                return null;
            }

            Map<String, Object> microregion = (Map<String, Object>) cityData.get("microrregiao");
            if (microregion == null) {
                return null;
            }

            Map<String, Object> mesoregion = (Map<String, Object>) microregion.get("mesorregiao");
            if (mesoregion == null) {
                return null;
            }

            Map<String, Object> uf = (Map<String, Object>) mesoregion.get("UF");
            if (uf == null) {
                return null;
            }

            String stateName = (String) uf.get("nome");
            String stateCode = (String) uf.get("sigla");
            String ibgeCode = cityData.get("id").toString();

            if (stateName == null || stateCode == null || ibgeCode == null) {
                return null;
            }

            return new City(cityName.trim(), stateName.trim(), stateCode.trim(), ibgeCode.trim());

        } catch (Exception e) {
            logger.warn("⚠️ Erro ao mapear dados da cidade: {}", e.getMessage());
            return null;
        }
    }

    private void logStateSummary() {
        try {
            List<Object[]> stateCounts = cityRepository.countCitiesByState();
            logger.info("📊 Resumo por estado:");
            for (Object[] stateCount : stateCounts) {
                String stateCode = (String) stateCount[0];
                Long count = (Long) stateCount[1];
                logger.info("   {} -> {} cidades", stateCode, count);
            }
        } catch (Exception e) {
            logger.warn("⚠️ Erro ao gerar resumo por estado: {}", e.getMessage());
        }
    }
}