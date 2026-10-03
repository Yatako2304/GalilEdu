package com.galiledu.horarios.dominio;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Lo que una asignación ocupa: sección, docente y espacio en un bloque, día y periodos. Es la
 * vista que necesita la validación de cruces; el docente llega por la carga académica.
 */
public record OcupacionHoraria(
	UUID asignacionId,
	UUID horarioSeccionId,
	String seccion,
	String curso,
	UUID docenteId,
	UUID espacioFisicoId,
	UUID subgrupoId,
	UUID bloqueHorarioId,
	DiaSemana dia,
	Set<UUID> periodos
) {
	public OcupacionHoraria {
		Objects.requireNonNull(asignacionId, "La asignación es obligatoria");
		Objects.requireNonNull(horarioSeccionId, "El horario es obligatorio");
		Objects.requireNonNull(seccion, "La sección es obligatoria");
		Objects.requireNonNull(curso, "El curso es obligatorio");
		Objects.requireNonNull(docenteId, "El docente es obligatorio");
		Objects.requireNonNull(bloqueHorarioId, "El bloque es obligatorio");
		Objects.requireNonNull(dia, "El día es obligatorio");
		periodos = Set.copyOf(Objects.requireNonNull(periodos, "Los periodos son obligatorios"));
	}

	/** Mismo bloque, mismo día y al menos un periodo en común. */
	public boolean coincideEnTiempo(OcupacionHoraria otra) {
		return bloqueHorarioId.equals(otra.bloqueHorarioId)
			&& dia == otra.dia
			&& !Collections.disjoint(periodos, otra.periodos);
	}

	/**
	 * Dos sesiones de la misma versión del horario chocan si alguna es para toda la sección o si
	 * son del mismo subgrupo; subgrupos distintos pueden coincidir.
	 */
	public boolean ocupaLaMismaSeccionQue(OcupacionHoraria otra) {
		return horarioSeccionId.equals(otra.horarioSeccionId)
			&& (subgrupoId == null || otra.subgrupoId == null || subgrupoId.equals(otra.subgrupoId));
	}
}
