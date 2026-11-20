package com.galiledu.asistencia.aplicacion;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

import com.galiledu.asistencia.aplicacion.CalendarioEscolar.CalendarioDelNivel;

/**
 * Un día es escolar si el año está ACTIVO, la fecha cae dentro del año y de algún periodo académico, y es un día
 * lectivo de la estructura horaria principal del nivel. Es la misma regla de las reservas de Horarios (HU-48): el
 * sistema no tiene feriados ni días no lectivos sueltos (acordado el 19/11).
 */
public final class ReglaDiaEscolar {
	private static final Locale ES = Locale.forLanguageTag("es-PE");

	private ReglaDiaEscolar() {
	}

	/** Vacío si es día escolar; si no, el motivo para mostrarlo al tutor. */
	public static Optional<String> motivoNoEscolar(CalendarioDelNivel calendario, LocalDate fecha) {
		if (!"ACTIVO".equals(calendario.estado())) {
			return Optional.of("El año escolar " + calendario.anio() + " no está activo.");
		}
		if (fecha.isBefore(calendario.inicio()) || fecha.isAfter(calendario.fin())) {
			return Optional.of("La fecha está fuera del año escolar " + calendario.anio() + ".");
		}
		if (calendario.periodos().stream().noneMatch(p -> p.contiene(fecha))) {
			return Optional.of("La fecha cae entre dos periodos académicos (vacaciones).");
		}
		if (!calendario.diasLectivos().contains(fecha.getDayOfWeek())) {
			return Optional
					.of("El " + fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, ES) + " no es día de clases.");
		}
		return Optional.empty();
	}
}
