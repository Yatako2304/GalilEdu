package com.galiledu.horarios.aplicacion.puertos;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.galiledu.horarios.dominio.DiaSemana;

/** Vistas de lectura del horario vigente; nunca devuelven borradores. */
public interface ConsultaHorarioSemanal {
	List<EntradaHorario> porDocente(UUID docenteId, UUID periodoId);

	List<EntradaHorario> porSeccion(UUID seccionId, UUID periodoId);

	/** Sesiones de la sección de la matrícula, incluidas las de sus subgrupos y no las de otros. */
	List<EntradaHorario> porMatricula(UUID matriculaId, UUID periodoId);

	/** Ordenadas por día y hora. {@code espacio} y {@code subgrupo} pueden ser nulos. */
	record EntradaHorario(UUID asignacionId, DiaSemana dia, int ordenBloque, LocalTime horaInicio,
		LocalTime horaFin, String seccion, String curso, UUID docenteId, String espacio,
		String subgrupo, String tipoSesion) {}
}
