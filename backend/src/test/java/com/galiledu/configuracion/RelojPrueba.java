package com.galiledu.configuracion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.galiledu.compartido.RelojConfig;

/**
 * Reloj de las pruebas de integración: empieza fijo en 15/09/2026 en Lima (el III Bimestre de 2026 está en curso) y una
 * prueba puede moverlo con {@link #fijarEn}. {@link RestauradorRelojPrueba} lo reinicia solo después de cada prueba.
 */
public class RelojPrueba extends Clock {
	private static final Instant INICIAL = Instant.parse("2026-09-15T15:00:00Z");

	private Instant instante = INICIAL;

	/** Hoy es esa fecha, a las 10:00 en Lima. */
	public void fijarEn(LocalDate fecha) {
		instante = fecha.atTime(10, 0).atZone(RelojConfig.LIMA).toInstant();
	}

	public void reiniciar() {
		instante = INICIAL;
	}

	@Override
	public ZoneId getZone() {
		return RelojConfig.LIMA;
	}

	@Override
	public Clock withZone(ZoneId zona) {
		return Clock.fixed(instante, zona);
	}

	@Override
	public Instant instant() {
		return instante;
	}
}
