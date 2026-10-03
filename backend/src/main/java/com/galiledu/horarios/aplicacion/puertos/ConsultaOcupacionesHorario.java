package com.galiledu.horarios.aplicacion.puertos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.galiledu.horarios.dominio.DiaSemana;
import com.galiledu.horarios.dominio.OcupacionHoraria;

/** Consulta qué hay ocupado en un bloque; la base de datos implementa este puerto. */
public interface ConsultaOcupacionesHorario {
	/**
	 * Ocupaciones activas en ese bloque y día que rigen en alguno de los periodos indicados: las
	 * de la versión {@code horarioSeccionId} y las de la versión más reciente de las demás
	 * secciones. Cada una trae solo los periodos que coinciden con los consultados.
	 */
	List<OcupacionHoraria> buscarOcupaciones(UUID horarioSeccionId, UUID bloqueHorarioId,
		DiaSemana dia, Set<UUID> periodos);
}
