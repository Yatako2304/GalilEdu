package com.galiledu.horarios.aplicacion.puertos;

import java.time.DayOfWeek;
import java.util.List;

import com.galiledu.horarios.dominio.AsignacionHorario;

/** Consulta las asignaciones vigentes de un bloque; la BD implementará este puerto. */
public interface ConsultaAsignacionesHorario {
	List<AsignacionHorario> buscarEnBloque(int anioEscolar, DayOfWeek dia, int bloque);
}
