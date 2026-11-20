package com.galiledu.asistencia.aplicacion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Las fechas del año escolar, sus periodos y los días lectivos de la estructura horaria principal de un nivel (HU-74,
 * HU-44). Es de solo lectura; hoy lo resuelve {@code CalendarioEscolarSql}.
 */
public interface CalendarioEscolar {
	/** Un periodo académico (bimestre o trimestre). Las fechas son inclusivas. */
	record Periodo(int numero, String nombre, LocalDate inicio, LocalDate fin) {
		public boolean contiene(LocalDate fecha) {
			return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
		}
	}

	/**
	 * El año con su estado (PLANIFICADO, ACTIVO o CERRADO), sus fechas inclusivas, sus periodos en orden y los días
	 * lectivos del nivel (vacío si el nivel no tiene estructura principal).
	 */
	record CalendarioDelNivel(int anio, String estado, LocalDate inicio, LocalDate fin, List<Periodo> periodos,
			Set<DayOfWeek> diasLectivos) {
	}

	/** Vacío si el año no existe. */
	Optional<CalendarioDelNivel> de(UUID anioEscolarId, String nivel);
}
