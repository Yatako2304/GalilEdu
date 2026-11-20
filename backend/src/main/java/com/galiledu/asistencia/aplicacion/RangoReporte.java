package com.galiledu.asistencia.aplicacion;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.galiledu.asistencia.aplicacion.CalendarioEscolar.CalendarioDelNivel;
import com.galiledu.asistencia.aplicacion.CalendarioEscolar.Periodo;

/**
 * HU-68: el rango de fechas del reporte. RF-146 pide el consolidado mensual y el plan de pruebas lo pide por
 * bimestre: se puede elegir un mes ({@code MES}, "2026-11") o un periodo académico ({@code PERIODO}, su número).
 * Solo se ofrecen los meses y periodos del año que ya empezaron.
 */
public final class RangoReporte {
	private static final Locale ES = Locale.forLanguageTag("es-PE");

	public enum Tipo {
		MES, PERIODO
	}

	/** Rango inclusivo con su nombre ("Noviembre 2026", "IV Bimestre"). */
	public record Rango(Tipo tipo, String valor, String etiqueta, LocalDate desde, LocalDate hasta) {
	}

	/** Una opción del selector. */
	public record Opcion(String valor, String etiqueta) {
	}

	private RangoReporte() {
	}

	/**
	 * Resuelve el rango pedido dentro del año. Sin tipo, el mes de hoy (o el último mes del año si ya terminó).
	 * Lanza {@link IllegalArgumentException} con el mensaje para el usuario si el mes o el periodo no son del año.
	 */
	public static Rango resolver(Tipo tipo, String valor, CalendarioDelNivel calendario, LocalDate hoy) {
		if (tipo == null || valor == null || valor.isBlank()) {
			YearMonth actual = YearMonth.from(limitar(hoy, calendario));
			return mes(actual, calendario);
		}
		if (tipo == Tipo.MES) {
			YearMonth mes;
			try {
				mes = YearMonth.parse(valor);
			} catch (DateTimeParseException excepcion) {
				throw new IllegalArgumentException("El mes del reporte no es válido.");
			}
			if (mes.isBefore(YearMonth.from(calendario.inicio())) || mes.isAfter(YearMonth.from(calendario.fin()))) {
				throw new IllegalArgumentException("El mes no es parte del año escolar " + calendario.anio() + ".");
			}
			return mes(mes, calendario);
		}
		Periodo periodo = calendario.periodos().stream().filter(p -> String.valueOf(p.numero()).equals(valor))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("El periodo no es parte del año escolar."));
		return new Rango(Tipo.PERIODO, valor, periodo.nombre(), periodo.inicio(), periodo.fin());
	}

	/** Meses del año escolar que ya empezaron, del más reciente al más antiguo. */
	public static List<Opcion> meses(CalendarioDelNivel calendario, LocalDate hoy) {
		List<Opcion> meses = new ArrayList<>();
		YearMonth ultimo = YearMonth.from(limitar(hoy, calendario));
		for (YearMonth mes = ultimo; !mes.isBefore(YearMonth.from(calendario.inicio())); mes = mes.minusMonths(1)) {
			meses.add(new Opcion(mes.toString(), nombre(mes)));
		}
		return meses;
	}

	/** Periodos que ya empezaron, en orden. */
	public static List<Opcion> periodos(CalendarioDelNivel calendario, LocalDate hoy) {
		return calendario.periodos().stream().filter(p -> !p.inicio().isAfter(hoy))
				.map(p -> new Opcion(String.valueOf(p.numero()), p.nombre())).toList();
	}

	private static Rango mes(YearMonth mes, CalendarioDelNivel calendario) {
		LocalDate desde = mes.atDay(1).isBefore(calendario.inicio()) ? calendario.inicio() : mes.atDay(1);
		LocalDate hasta = mes.atEndOfMonth().isAfter(calendario.fin()) ? calendario.fin() : mes.atEndOfMonth();
		return new Rango(Tipo.MES, mes.toString(), nombre(mes), desde, hasta);
	}

	private static LocalDate limitar(LocalDate fecha, CalendarioDelNivel calendario) {
		if (fecha.isBefore(calendario.inicio())) {
			return calendario.inicio();
		}
		return fecha.isAfter(calendario.fin()) ? calendario.fin() : fecha;
	}

	/** "Noviembre 2026". */
	static String nombre(YearMonth mes) {
		String texto = mes.getMonth().getDisplayName(TextStyle.FULL, ES);
		return Character.toUpperCase(texto.charAt(0)) + texto.substring(1) + " " + mes.getYear();
	}
}
