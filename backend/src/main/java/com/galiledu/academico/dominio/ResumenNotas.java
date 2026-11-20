package com.galiledu.academico.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import com.galiledu.academico.dominio.CalculoNotas.Nota;
import com.galiledu.academico.dominio.CalculoNotas.Rango;

/**
 * Resume las notas oficiales de varios estudiantes (HU-63 y HU-64): el promedio de un grupo y cuántos alcanzan cada
 * nivel de logro. No toca la base de datos.
 *
 * <ul>
 * <li>Escala vigesimal: promedio simple de las notas del grupo; su nivel sale de la tabla de conversión (redondeo al
 * entero, 0,5 sube).</li>
 * <li>Escala cualitativa: no se promedian niveles; el resumen es el nivel más frecuente (si empatan, el mayor).</li>
 * <li>«Logro esperado» cuenta a quienes tienen A o AD. No hay «aprobados»: ver la decisión del 18/11.</li>
 * <li>Una nota <b>pendiente</b> (falta registrar algún ítem) no entra en el promedio ni en los niveles: se cuenta
 * aparte, para que la coordinación sepa cuántos registros faltan.</li>
 * </ul>
 */
public final class ResumenNotas {
	private static final List<String> LOGRO_ESPERADO = List.of("AD", "A");

	private ResumenNotas() {
	}

	/** La nota oficial de un estudiante: vacía si aún no hay o si está pendiente por registros faltantes. */
	public record Valor(Optional<Nota> nota, boolean pendiente) {
		public static final Valor SIN_NOTA = new Valor(Optional.empty(), false);
	}

	/**
	 * Resumen de un grupo. promedio es "—" sin notas; porcentajeLogro es null sin notas.
	 *
	 * @param conNota
	 *            estudiantes del grupo con nota oficial
	 * @param logroEsperado
	 *            de ellos, los que tienen A o AD
	 * @param pendientes
	 *            estudiantes cuya nota está pendiente porque falta registrar algún ítem
	 */
	public record Celda(String promedio, String nivel, String logro, int conNota, int logroEsperado,
			Integer porcentajeLogro, int pendientes) {
	}

	/**
	 * Cuántas notas hay en cada nivel. pendientes: falta registrar algún ítem; sinNotas: todavía no hay ítems que
	 * cuenten. porcentajeLogro: A o AD sobre las notas que existen.
	 */
	public record Distribucion(int ad, int a, int b, int c, int pendientes, int sinNotas) {
		public static final Distribucion VACIA = new Distribucion(0, 0, 0, 0, 0, 0);

		public int conNota() {
			return ad + a + b + c;
		}

		public Integer porcentajeLogro() {
			return porcentaje(ad + a, conNota());
		}

		public Distribucion mas(Distribucion otra) {
			return new Distribucion(ad + otra.ad, a + otra.a, b + otra.b, c + otra.c, pendientes + otra.pendientes,
					sinNotas + otra.sinNotas);
		}
	}

	public static Celda celda(List<Valor> valores, List<Rango> rangos) {
		int pendientes = (int) valores.stream().filter(Valor::pendiente).count();
		List<Nota> calificadas = valores.stream().map(Valor::nota).flatMap(Optional::stream).filter(Nota::calificada)
				.toList();
		if (calificadas.isEmpty()) {
			return new Celda("—", null, CalculoNotas.PENDIENTE, 0, 0, null, pendientes);
		}
		Nota resumen = calificadas.get(0).numerica() != null
				? CalculoNotas.combinar(calificadas, "PROMEDIO_SIMPLE").orElseThrow()
				: CalculoNotas.combinar(calificadas, "NIVEL_MAS_FRECUENTE").orElseThrow();
		int logro = (int) calificadas.stream().filter(n -> LOGRO_ESPERADO.contains(CalculoNotas.nivel(n, rangos)))
				.count();
		return new Celda(CalculoNotas.texto(resumen), CalculoNotas.nivel(resumen, rangos),
				CalculoNotas.logro(resumen, rangos), calificadas.size(), logro, porcentaje(logro, calificadas.size()),
				pendientes);
	}

	public static Distribucion distribucion(List<Valor> valores, List<Rango> rangos) {
		int ad = 0, a = 0, b = 0, c = 0, pendientes = 0, sinNotas = 0;
		for (Valor valor : valores) {
			String nivel = valor.nota().filter(Nota::calificada).map(n -> CalculoNotas.nivel(n, rangos)).orElse(null);
			if (nivel == null) {
				if (valor.pendiente()) {
					pendientes++;
				} else {
					sinNotas++;
				}
				continue;
			}
			switch (nivel) {
				case "AD" -> ad++;
				case "A" -> a++;
				case "B" -> b++;
				default -> c++;
			}
		}
		return new Distribucion(ad, a, b, c, pendientes, sinNotas);
	}

	/** Porcentaje entero (0,5 sube); null si no hay base. */
	static Integer porcentaje(int parte, int total) {
		if (total == 0) {
			return null;
		}
		return BigDecimal.valueOf(parte * 100L).divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
				.intValueExact();
	}
}
