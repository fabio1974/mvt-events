package com.mvt.mvt_events.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

/**
 * Configuração de auditoria JPA com OffsetDateTime.
 * 
 * <p>Fornece um DateTimeProvider customizado que retorna OffsetDateTime
 * com timezone America/Fortaleza para os campos anotados com
 * {@code @CreatedDate} e {@code @LastModifiedDate}.</p>
 * 
 * <p>Sem esta configuração, o Spring Data JPA usa LocalDateTime.now()
 * por padrão, o que causa erro ao tentar atribuir a campos OffsetDateTime.</p>
 */
@Configuration
public class JpaAuditingConfiguration {

    /**
     * Fornece OffsetDateTime com timezone America/Fortaleza para auditoria.
     * 
     * @return DateTimeProvider que retorna OffsetDateTime atual
     */
    @Bean
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now(ZoneId.of("America/Fortaleza")));
    }
}
