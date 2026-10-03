package com.galiledu.academica.aplicacion;

/** La cuenta autenticada no tiene relación con la carga, sección o matrícula solicitada. */
public class AccesoAcademicoDenegadoException extends RuntimeException {
	public AccesoAcademicoDenegadoException(String mensaje) {
		super(mensaje);
	}
}
