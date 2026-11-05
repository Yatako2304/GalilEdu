package com.galiledu.horarios.aplicacion;

import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Servicio del módulo Horarios. DUEÑA: Valeria (HU-45). En la Fase 0 solo expone lo que otros módulos necesitan.
 */
@Service
public class ServicioHorarios {
	/**
	 * Cuántos bloques tiene asignados el curso del plan en el horario de la sección. Lo usará Académico. TODO (Valeria,
	 * HU-45): contar las filas de horarios.horario_bloque; hoy devuelve 0.
	 */
	public int bloquesDeCurso(UUID planCursoId, UUID seccionId) {
		return 0;
	}
}
