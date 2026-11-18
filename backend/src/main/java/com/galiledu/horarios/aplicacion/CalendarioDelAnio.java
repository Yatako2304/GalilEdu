package com.galiledu.horarios.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Las fechas de un año escolar y de sus periodos académicos (HU-48): una reserva solo cabe dentro del año y de algún
 * periodo. Es de solo lectura. Hoy lo resuelve {@code CalendarioDelAnioSql}; cuando Configuración ofrezca un servicio
 * equivalente, solo se reemplaza esa clase.
 */
public interface CalendarioDelAnio {
	/** Un periodo (bimestre o trimestre). Las fechas son inclusivas. */
	record PeriodoDelAnio(int numero, String nombre, LocalDate inicio, LocalDate fin) {
		public boolean contiene(LocalDate fecha) {
			return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
		}
	}

	/** El año con sus fechas inclusivas. {@code estado} es PLANIFICADO, ACTIVO o CERRADO. Los periodos van en orden. */
	record AnioConPeriodos(UUID id, int anio, String estado, LocalDate inicio, LocalDate fin,
			List<PeriodoDelAnio> periodos) {
		public boolean contiene(LocalDate fecha) {
			return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
		}
	}

	/** Vacío si el año no existe. */
	Optional<AnioConPeriodos> deAnio(UUID anioEscolarId);
}
