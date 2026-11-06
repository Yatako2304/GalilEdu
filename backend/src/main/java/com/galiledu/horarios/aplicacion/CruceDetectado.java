package com.galiledu.horarios.aplicacion;

import com.galiledu.configuracion.dominio.DiaSemana;

/**
 * Un choque entre un bloque solicitado y uno ya guardado. {@code numeroBloque} es el del bloque solicitado;
 * {@code seccion} y {@code curso} son los nombres de la sección y del curso con los que choca.
 */
public record CruceDetectado(TipoCruce tipo, DiaSemana dia, int numeroBloque, String mensaje, String seccion,
		String curso) {
}
