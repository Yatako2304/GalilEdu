package com.galiledu.asistencia.aplicacion.puertos;

/** Verifica si el actor es tutor o co-tutor autorizado de la sección. */
public interface AutorizacionAsistencia {
	boolean puedeGestionarSeccion(String actor, String seccion);
}
