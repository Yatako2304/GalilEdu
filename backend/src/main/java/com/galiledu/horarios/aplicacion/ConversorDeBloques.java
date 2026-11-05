package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Pasa las celdas del horario a bloques con horas reales y docente. DUEÑO: Jose (HU-45). Lo usan el PUT, el GET y el
 * endpoint de cruces. Hoy responde {@code ConversorDeBloquesStub}.
 */
public interface ConversorDeBloques {
	/**
	 * Calcula las horas con la estructura horaria de la sección y resuelve el docente de cada celda desde
	 * academico.docente_seccion (plan_curso_id, seccion_id).
	 */
	List<BloqueSolicitado> aBloquesSolicitados(UUID seccionId, List<CeldaHorario> celdas);
}
