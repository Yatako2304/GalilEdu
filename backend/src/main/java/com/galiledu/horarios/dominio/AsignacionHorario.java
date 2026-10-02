package com.galiledu.horarios.dominio;

import java.time.DayOfWeek;
import java.util.Objects;

/** Bloque con identidad propia; no es un DTO ni una entidad JPA. */
public final class AsignacionHorario {
	private final String identificador;
	private final int anioEscolar;
	private final DayOfWeek dia;
	private final int bloque;
	private final String seccion;
	private final String curso;
	private final String docente;
	private final String aula;

	public AsignacionHorario(String identificador, int anioEscolar, DayOfWeek dia, int bloque,
		String seccion, String curso, String docente, String aula) {
		this.identificador = obligatorio(identificador, "identificador");
		if (anioEscolar < 1 || bloque < 1) {
			throw new IllegalArgumentException("Año escolar y bloque deben ser positivos");
		}
		this.anioEscolar = anioEscolar;
		this.dia = Objects.requireNonNull(dia, "El día es obligatorio");
		this.bloque = bloque;
		this.seccion = obligatorio(seccion, "sección");
		this.curso = obligatorio(curso, "curso");
		this.docente = obligatorio(docente, "docente");
		this.aula = obligatorio(aula, "aula");
	}

	public String identificador() {
		return identificador;
	}

	public int anioEscolar() {
		return anioEscolar;
	}

	public DayOfWeek dia() {
		return dia;
	}

	public int bloque() {
		return bloque;
	}

	public String seccion() {
		return seccion;
	}

	public String curso() {
		return curso;
	}

	public String docente() {
		return docente;
	}

	public String aula() {
		return aula;
	}

	private static String obligatorio(String valor, String nombre) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalArgumentException("El campo " + nombre + " es obligatorio");
		}
		return valor.strip();
	}
}
