package com.galiledu.usuarios.dominio;

/** Catálogo de roles (tabla seguridad.rol). Los nombres coinciden con los códigos de la BD y del frontend. */
public enum Rol {
	ADMINISTRADOR("Administrador"), PERSONAL_ADMINISTRATIVO("Personal administrativo"), COORDINADOR(
			"Coordinador"), APODERADO("Apoderado"), DOCENTE("Docente"), TUTOR("Tutor"), ESTUDIANTE("Estudiante");

	private final String nombre;

	Rol(String nombre) {
		this.nombre = nombre;
	}

	public String nombre() {
		return nombre;
	}
}
