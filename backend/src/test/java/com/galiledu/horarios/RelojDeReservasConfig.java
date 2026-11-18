package com.galiledu.horarios;

import java.time.Clock;
import java.time.Instant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.galiledu.compartido.RelojConfig;

/**
 * Fija «hoy» de las pruebas de reservas en el miércoles 3 de marzo de 2027, 10:00 en Lima: el año 2027 (PLANIFICADO)
 * es donde las pruebas arman sus datos, y así las fechas «futuras» y los filtros de semana, mes y año no dependen del
 * día en que se corran. Semana: 1 a 7 de marzo; mes: marzo; año: 2027.
 */
@TestConfiguration
class RelojDeReservasConfig {
	@Bean
	@Primary
	Clock relojFijoDeReservas() {
		return Clock.fixed(Instant.parse("2027-03-03T15:00:00Z"), RelojConfig.LIMA);
	}
}
