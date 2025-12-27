package com.mvt.mvt_events.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.TimeZone;

/**
 * Configuração do Jackson para serialização JSON
 * 
 * <p><strong>Timezone Strategy:</strong></p>
 * <ul>
 *   <li>Banco de dados: Armazena em UTC (timezone zero)</li>
 *   <li>Backend API: Retorna datas em formato ISO-8601 com timezone Brazil/East (America/Fortaleza)</li>
 *   <li>Frontend: Recebe datas já convertidas para o timezone local</li>
 * </ul>
 * 
 * <p>Exemplo: Banco salva "2024-01-15 10:00:00 UTC" → API retorna "2024-01-15T07:00:00-03:00"</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

        // Adiciona módulo Hibernate para lidar com proxies LAZY
        Hibernate6Module hibernate6Module = new Hibernate6Module();
        hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
        hibernate6Module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);

        mapper.registerModule(hibernate6Module);

        // Adiciona módulo Java 8 Time para serializar LocalDateTime, LocalDate, etc
        // como strings
        mapper.registerModule(new JavaTimeModule());

        // Desabilita serialização de datas como timestamps (arrays)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Configura timezone para America/Fortaleza (Brasília - UTC-3)
        // Isso converte automaticamente todas as datas de UTC para o timezone configurado
        // quando serializar para JSON (LocalDateTime → ZonedDateTime → String ISO-8601)
        mapper.setTimeZone(TimeZone.getTimeZone("America/Fortaleza"));

        return mapper;
    }
}
