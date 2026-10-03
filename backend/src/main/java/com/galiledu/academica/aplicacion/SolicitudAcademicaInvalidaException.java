package com.galiledu.academica.aplicacion;

/** Rechazo de una operación académica por reglas de negocio o datos inconsistentes. */
public class SolicitudAcademicaInvalidaException extends RuntimeException {
	public SolicitudAcademicaInvalidaException(String mensaje) {
		super(mensaje);
	}
}
