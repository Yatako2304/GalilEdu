package com.galiledu.horarios.aplicacion;

/** Rechazo de una solicitud porque contradice los datos vigentes o una regla de Horarios. */
public class HorarioInvalidoException extends RuntimeException {
	public HorarioInvalidoException(String mensaje) {
		super(mensaje);
	}
}
