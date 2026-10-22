package com.galiledu.configuracion.aplicacion;

import java.util.UUID;

/**
 * Lo que las secciones necesitan saber de la matrícula (módulo matricula). Hoy se lee con SQL de solo lectura; cuando
 * matricula ofrezca un servicio equivalente, solo cambia la clase que implementa esta interfaz.
 */
public interface MatriculasDeSecciones {
	/**
	 * Si alguna matrícula, en cualquier estado, o algún cambio de sección (como origen o destino) apunta a la sección.
	 * Una sección así no se puede eliminar.
	 */
	boolean tieneMatriculasOCambios(UUID seccionId);
}
