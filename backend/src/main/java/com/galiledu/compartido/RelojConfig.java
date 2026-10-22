package com.galiledu.compartido;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj de la aplicación en hora de Lima. Los servicios lo inyectan para calcular "hoy"; las pruebas lo fijan. */
@Configuration
public class RelojConfig {
	public static final ZoneId LIMA = ZoneId.of("America/Lima");

	@Bean
	Clock reloj() {
		return Clock.system(LIMA);
	}
}
