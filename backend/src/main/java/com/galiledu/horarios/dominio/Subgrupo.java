package com.galiledu.horarios.dominio;

import java.util.Objects;
import java.util.UUID;

/** Parte de una sección para cursos que se dividen; el nombre es único dentro de la sección. */
public final class Subgrupo {
	private static final int LONGITUD_MAXIMA_NOMBRE = 40;

	private final UUID id;
	private final UUID seccionId;
	private final String nombre;
	private final boolean activo;

	public Subgrupo(UUID id, UUID seccionId, String nombre, boolean activo) {
		this.id = Objects.requireNonNull(id, "El identificador es obligatorio");
		this.seccionId = Objects.requireNonNull(seccionId, "La sección es obligatoria");
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre del subgrupo es obligatorio");
		}
		this.nombre = nombre.strip();
		if (this.nombre.length() > LONGITUD_MAXIMA_NOMBRE) {
			throw new IllegalArgumentException(
				"El nombre del subgrupo admite hasta " + LONGITUD_MAXIMA_NOMBRE + " caracteres");
		}
		this.activo = activo;
	}

	public static Subgrupo nuevo(UUID seccionId, String nombre) {
		return new Subgrupo(UUID.randomUUID(), seccionId, nombre, true);
	}

	public UUID id() {
		return id;
	}

	public UUID seccionId() {
		return seccionId;
	}

	public String nombre() {
		return nombre;
	}

	public boolean activo() {
		return activo;
	}

	public Subgrupo desactivar() {
		return new Subgrupo(id, seccionId, nombre, false);
	}
}
