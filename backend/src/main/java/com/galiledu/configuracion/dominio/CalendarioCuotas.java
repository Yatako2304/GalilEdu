package com.galiledu.configuracion.dominio;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Calendario de pensiones del tarifario (HU-34, P19). Una cuota por mes, desde el mes de inicio de clases hasta el de
 * fin: vence el último día del mes y la última el día 15 (o el fin del año, si es anterior). El calendario se genera;
 * no se edita.
 */
public final class CalendarioCuotas {
	private static final String[] MESES = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
			"septiembre", "octubre", "noviembre", "diciembre"};
	private static final int DIA_ULTIMA_CUOTA = 15;

	/** Una cuota del calendario: lo mismo que se guarda en pagos.cuota_tarifario. */
	public record Cuota(int numero, String nombre, LocalDate fechaVencimiento) {
	}

	private CalendarioCuotas() {
	}

	public static List<Cuota> generar(LocalDate inicioAnio, LocalDate finAnio) {
		YearMonth ultimo = YearMonth.from(finAnio);
		List<Cuota> cuotas = new ArrayList<>();
		int numero = 1;
		for (YearMonth mes = YearMonth.from(inicioAnio); !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
			LocalDate vencimiento = mes.equals(ultimo) ? min(mes.atDay(DIA_ULTIMA_CUOTA), finAnio) : mes.atEndOfMonth();
			cuotas.add(new Cuota(numero++, "Pensión de " + MESES[mes.getMonthValue() - 1], vencimiento));
		}
		return cuotas;
	}

	/** La matrícula vence el último día del mes anterior al inicio de clases. */
	public static LocalDate vencimientoMatricula(LocalDate inicioAnio) {
		return inicioAnio.withDayOfMonth(1).minusDays(1);
	}

	private static LocalDate min(LocalDate a, LocalDate b) {
		return a.isBefore(b) ? a : b;
	}
}
