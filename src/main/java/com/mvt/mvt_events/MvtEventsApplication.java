package com.mvt.mvt_events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

/**
 * Aplicação principal do sistema MVT Events
 * 
 * <p><strong>Timezone Configuration:</strong></p>
 * <ul>
 *   <li>JVM timezone: America/Fortaleza (Brasília - UTC-3)</li>
 *   <li>Database: Armazena em UTC via Hibernate</li>
 *   <li>Jackson: Serializa para JSON com timezone configurado</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class MvtEventsApplication {

	/**
	 * Configura timezone padrão da JVM para America/Fortaleza
	 * Isso garante que todas as operações com datas usem o timezone brasileiro
	 */
	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Fortaleza"));
	}

	public static void main(String[] args) {
		SpringApplication.run(MvtEventsApplication.class, args);
	}
}
