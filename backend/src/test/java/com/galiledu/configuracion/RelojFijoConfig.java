package com.galiledu.configuracion;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Reemplaza el {@code Clock} de la aplicación por un {@link RelojPrueba} (hoy es 15/09/2026 en Lima). Lo importa
 * {@link PruebaIntegracionConfiguracion}; las pruebas no lo usan directamente.
 */
@TestConfiguration
public class RelojFijoConfig {
	@Bean
	@Primary
	RelojPrueba relojFijo() {
		return new RelojPrueba();
	}
}
