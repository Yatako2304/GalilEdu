package com.galiledu.horarios.aplicacion;

import java.time.LocalTime;
import java.util.UUID;

import com.galiledu.configuracion.dominio.DiaSemana;

/**
 * Lo que se quiere guardar en un bloque: con las horas reales (según la estructura de la sección) y el docente ya
 * resuelto desde academico.docente_seccion. Lo produce {@link ConversorDeBloques}.
 */
public record BloqueSolicitado(DiaSemana dia, int numeroBloque, LocalTime inicio, LocalTime fin, UUID planCursoId,
		UUID docenteId, UUID ambienteId) {
}
