package com.galiledu.asistencia.dominio;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
/** Marcación de una matrícula dentro de una jornada. */
public record DetalleAsistencia(UUID id, UUID jornadaAsistenciaId, UUID matriculaId,
	EstadoAsistencia estado, CondicionAsistencia condicion, LocalTime horaMarcacion, String observacion, boolean activo) {
	public DetalleAsistencia {
		Objects.requireNonNull(id); Objects.requireNonNull(jornadaAsistenciaId);
		Objects.requireNonNull(matriculaId); Objects.requireNonNull(estado);
		observacion = observacion == null || observacion.isBlank() ? null : observacion.strip();
		if (observacion != null && observacion.length() > 255) throw new IllegalArgumentException("La observación supera 255 caracteres");
	}
	public static DetalleAsistencia nuevo(UUID jornada, UUID matricula, EstadoAsistencia estado,
		CondicionAsistencia condicion, LocalTime hora, String observacion) {
		return new DetalleAsistencia(UUID.randomUUID(), jornada, matricula, estado, condicion, hora, observacion, true);
	}
	public DetalleAsistencia cambiar(UUID jornada, UUID matricula, EstadoAsistencia estado,
		CondicionAsistencia condicion, LocalTime hora, String observacion) {
		return new DetalleAsistencia(id, jornada, matricula, estado, condicion, hora, observacion, activo);
	}
}
