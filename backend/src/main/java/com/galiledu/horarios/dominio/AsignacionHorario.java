package com.galiledu.horarios.dominio;

import java.time.DayOfWeek;
import java.util.Objects;

/** Datos de un bloque asignado; no representa una tabla de base de datos. */
public record AsignacionHorario(
	String identificador,
	int anioEscolar,
	DayOfWeek dia,
	int bloque,
	String seccion,
	String curso,
	String docente,
	String aula
) {
	public AsignacionHorario {
		identificador = obligatorio(identificador, "identificador");
		if (anioEscolar < 1 || bloque < 1) {
			throw new IllegalArgumentException("Año escolar y bloque deben ser positivos");
		}
		dia = Objects.requireNonNull(dia, "El día es obligatorio");
		seccion = obligatorio(seccion, "sección");
		curso = obligatorio(curso, "curso");
		docente = obligatorio(docente, "docente");
		aula = obligatorio(aula, "aula");
	}

	private static String obligatorio(String valor, String nombre) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalArgumentException("El campo " + nombre + " es obligatorio");
		}
		return valor.strip();
	}
}
