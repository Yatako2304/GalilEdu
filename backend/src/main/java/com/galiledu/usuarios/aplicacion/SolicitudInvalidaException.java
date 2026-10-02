package com.galiledu.usuarios.aplicacion;

/** Rechazo de datos de negocio detectado al consultar la persistencia. */
public class SolicitudInvalidaException extends RuntimeException {
	public SolicitudInvalidaException(String mensaje) {
		super(mensaje);
	}
}
