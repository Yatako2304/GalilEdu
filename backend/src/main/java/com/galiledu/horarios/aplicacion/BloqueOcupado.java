package com.galiledu.horarios.aplicacion;

import java.time.LocalTime;
import java.util.UUID;

import com.galiledu.configuracion.dominio.DiaSemana;

/**
 * Un bloque ya guardado en otra sección del año, con sus horas calculadas con la estructura de ESA sección. Lo
 * produce {@link OcupacionHorarios}.
 */
public record BloqueOcupado(UUID seccionId, DiaSemana dia, LocalTime inicio, LocalTime fin, UUID docenteId,
		UUID ambienteId) {
}
