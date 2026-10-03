package com.galiledu.academica.dominio;

/** La regla configurada no produce un único resultado con las notas disponibles. */
public class CalculoNotaIndeterminadoException extends RuntimeException {
	public CalculoNotaIndeterminadoException(String mensaje) {
		super(mensaje);
	}
}
