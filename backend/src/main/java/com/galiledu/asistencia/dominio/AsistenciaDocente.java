package com.galiledu.asistencia.dominio;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
/** Marcación diaria del docente, separada de la jornada de estudiantes. */
public record AsistenciaDocente(UUID id, UUID docenteId, LocalDate fecha, LocalTime horaEntrada,
	LocalTime horaSalida, String observacion, boolean activa) {
	public AsistenciaDocente {
		Objects.requireNonNull(id); Objects.requireNonNull(docenteId); Objects.requireNonNull(fecha);
		observacion = observacion == null || observacion.isBlank() ? null : observacion.strip();
		if (observacion != null && observacion.length() > 255) throw new IllegalArgumentException("La observación supera 255 caracteres");
	}
	public static AsistenciaDocente nueva(UUID docente, LocalDate fecha, LocalTime entrada, LocalTime salida, String observacion) {
		return new AsistenciaDocente(UUID.randomUUID(), docente, fecha, entrada, salida, observacion, true);
	}
	public AsistenciaDocente cambiar(UUID docente, LocalDate fecha, LocalTime entrada, LocalTime salida, String observacion) {
		return new AsistenciaDocente(id, docente, fecha, entrada, salida, observacion, activa);
	}
}
