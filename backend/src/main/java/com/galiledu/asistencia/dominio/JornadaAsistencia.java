package com.galiledu.asistencia.dominio;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
/** Jornada única por carga académica y fecha. */
public record JornadaAsistencia(UUID id, UUID cargaAcademicaId, LocalDate fecha, Instant fechaRegistro, boolean activa) {
	public JornadaAsistencia {
		Objects.requireNonNull(id); Objects.requireNonNull(cargaAcademicaId);
		Objects.requireNonNull(fecha); Objects.requireNonNull(fechaRegistro);
	}
	public static JornadaAsistencia nueva(UUID carga, LocalDate fecha, Instant registrada) {
		return new JornadaAsistencia(UUID.randomUUID(), carga, fecha, registrada, true);
	}
	public JornadaAsistencia cambiar(UUID carga, LocalDate fecha) {
		return new JornadaAsistencia(id, carga, fecha, fechaRegistro, activa);
	}
}
