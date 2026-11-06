package com.galiledu.horarios.aplicacion;

import java.time.LocalTime;
import java.util.UUID;

import com.galiledu.configuracion.dominio.DiaSemana;

/**
 * Un bloque ya guardado en otra sección del año, con sus horas calculadas con la estructura de ESA sección. Lo
 * produce {@link OcupacionHorarios}.
 * <p>
 * {@code seccion}, {@code curso}, {@code docente} y {@code ambiente} son los nombres legibles (por ejemplo "5.° B")
 * para armar los mensajes de cruce. {@link OcupacionHorarios} es quien llena estos campos; quien detecta los cruces
 * solo los lee.
 */
public record BloqueOcupado(UUID seccionId, DiaSemana dia, LocalTime inicio, LocalTime fin, UUID docenteId,
		UUID ambienteId, String seccion, String curso, String docente, String ambiente) {
}
