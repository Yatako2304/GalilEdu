package com.galiledu.usuarios.infraestructura.seguridad;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelojConfig {
	@Bean
	Clock reloj() {
		return Clock.systemUTC();
	}
}
