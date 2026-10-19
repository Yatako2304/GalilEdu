package com.galiledu.compartido.web;

import org.springframework.http.HttpStatus;

/**
 * Error esperado de una regla de negocio. El mensaje se muestra tal cual en el frontend,
 * así que debe estar en español y ser entendible para el usuario.
 */
public class ExcepcionNegocio extends RuntimeException {
	private final HttpStatus estado;

	public ExcepcionNegocio(HttpStatus estado, String mensaje) {
		super(mensaje);
		this.estado = estado;
	}

	public HttpStatus estado() {
		return estado;
	}

	public static ExcepcionNegocio noEncontrado(String mensaje) {
		return new ExcepcionNegocio(HttpStatus.NOT_FOUND, mensaje);
	}

	public static ExcepcionNegocio conflicto(String mensaje) {
		return new ExcepcionNegocio(HttpStatus.CONFLICT, mensaje);
	}

	public static ExcepcionNegocio invalido(String mensaje) {
		return new ExcepcionNegocio(HttpStatus.BAD_REQUEST, mensaje);
	}
}
