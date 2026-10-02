package com.galiledu.asistencia.dominio;

import java.time.LocalDate;
import java.util.Objects;

/** Marcación identificada por estudiante y fecha escolar (RF-143). */
public final class RegistroAsistenciaEstudiante {
	private final String estudiante;
	private final String seccion;
	private final LocalDate fecha;
	private final EstadoAsistencia estado;

	public RegistroAsistenciaEstudiante(String estudiante, String seccion, LocalDate fecha,
		EstadoAsistencia estado) {
		if (estudiante == null || estudiante.isBlank()
			|| seccion == null || seccion.isBlank()) {
			throw new IllegalArgumentException("Estudiante y sección son obligatorios");
		}
		this.estudiante = estudiante;
		this.seccion = seccion;
		this.fecha = Objects.requireNonNull(fecha, "La fecha es obligatoria");
		this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
	}

	public String estudiante() {
		return estudiante;
	}

	public String seccion() {
		return seccion;
	}

	public LocalDate fecha() {
		return fecha;
	}

	public EstadoAsistencia estado() {
		return estado;
	}

	public RegistroAsistenciaEstudiante conEstado(EstadoAsistencia nuevoEstado) {
		return new RegistroAsistenciaEstudiante(estudiante, seccion, fecha, nuevoEstado);
	}
}
