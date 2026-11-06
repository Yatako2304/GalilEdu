package com.galiledu.academico.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reglas para combinar notas con los parámetros del año escolar: ítems en un periodo, periodos en un curso y cursos en
 * una competencia. Los pasos intermedios no se redondean a 2 decimales: solo se redondea lo que se muestra.
 */
public final class CalculoNotas {
	private static final int ESCALA_INTERNA = 10;
	private static final BigDecimal PESO_TOTAL = new BigDecimal("100");
	private static final Map<String, Integer> ORDEN_NIVEL = Map.of("C", 0, "B", 1, "A", 2, "AD", 3);

	private CalculoNotas() {
	}

	/** Una nota por combinar. Sin nota numérica ni nivel, todavía no está calificada y no cuenta. */
	public record Nota(BigDecimal peso, BigDecimal numerica, String nivel, Instant orden) {
		boolean calificada() {
			return numerica != null || nivel != null;
		}
	}

	/**
	 * Aplica la regla de nota del año. PROMEDIO_PONDERADO normaliza por los pesos de lo calificado, PROMEDIO_SIMPLE
	 * ignora los pesos, NIVEL_MAS_FRECUENTE desempata por el nivel mayor y ULTIMO_NIVEL toma la nota más reciente.
	 */
	public static Optional<Nota> combinar(List<Nota> notas, String regla) {
		List<Nota> calificadas = notas.stream().filter(Nota::calificada).toList();
		if (calificadas.isEmpty()) {
			return Optional.empty();
		}
		Instant orden = calificadas.stream().map(Nota::orden).filter(Objects::nonNull).max(Comparator.naturalOrder())
				.orElse(null);
		if (calificadas.get(0).numerica() != null) {
			return Optional.of(new Nota(BigDecimal.ONE, promedio(calificadas, regla), null, orden));
		}
		return Optional.of(new Nota(BigDecimal.ONE, null, nivel(calificadas, regla), orden));
	}

	/**
	 * Consolida los resultados de un curso, en el orden de los periodos. ULTIMO_PERIODO toma el último con notas;
	 * PROMEDIO_PERIODOS los promedia. En la escala cualitativa no hay promedio: se aplica la regla de nota del año.
	 */
	public static Optional<Nota> consolidar(List<Nota> porPeriodo, String consolidacion, String regla) {
		if ("ULTIMO_PERIODO".equals(consolidacion)) {
			return porPeriodo.stream().filter(Nota::calificada).reduce((anterior, siguiente) -> siguiente);
		}
		return combinar(porPeriodo.stream().map(n -> new Nota(BigDecimal.ONE, n.numerica(), n.nivel(), n.orden()))
				.toList(), regla);
	}

	/** Pesos de una competencia entre sus cursos: completos si todos tienen peso y suman 100 %. */
	public static boolean pesosCompletos(List<BigDecimal> pesos) {
		return !pesos.isEmpty() && pesos.stream().allMatch(Objects::nonNull)
				&& pesos.stream().reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(PESO_TOTAL) == 0;
	}

	/** Lo que se muestra: hasta 2 decimales en la escala vigesimal, o el nivel en la cualitativa. */
	public static String texto(Nota nota) {
		if (nota.numerica() == null) {
			return nota.nivel();
		}
		return nota.numerica().setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}

	/** Aprobado desde la nota aprobatoria en la escala vigesimal; con A o AD en la cualitativa. */
	public static String estado(Nota nota, int notaAprobatoria) {
		if (nota.numerica() != null) {
			return nota.numerica().setScale(2, RoundingMode.HALF_UP)
					.compareTo(BigDecimal.valueOf(notaAprobatoria)) >= 0 ? "Aprobado" : "Desaprobado";
		}
		return "A".equals(nota.nivel()) || "AD".equals(nota.nivel()) ? "Aprobado" : "Desaprobado";
	}

	private static BigDecimal promedio(List<Nota> calificadas, String regla) {
		List<Nota> numericas = calificadas.stream().filter(n -> n.numerica() != null).toList();
		if ("PROMEDIO_PONDERADO".equals(regla)) {
			BigDecimal suma = numericas.stream().map(n -> n.numerica().multiply(n.peso())).reduce(BigDecimal.ZERO,
					BigDecimal::add);
			BigDecimal pesos = numericas.stream().map(Nota::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
			return suma.divide(pesos, ESCALA_INTERNA, RoundingMode.HALF_UP);
		}
		return numericas.stream().map(Nota::numerica).reduce(BigDecimal.ZERO, BigDecimal::add)
				.divide(BigDecimal.valueOf(numericas.size()), ESCALA_INTERNA, RoundingMode.HALF_UP);
	}

	private static String nivel(List<Nota> calificadas, String regla) {
		List<Nota> conNivel = calificadas.stream().filter(n -> n.nivel() != null).toList();
		if ("ULTIMO_NIVEL".equals(regla)) {
			return conNivel.stream().max(Comparator.comparing(Nota::orden)).orElseThrow().nivel();
		}
		return conNivel.stream().collect(Collectors.groupingBy(Nota::nivel, Collectors.counting())).entrySet().stream()
				.max(Map.Entry.<String, Long>comparingByValue()
						.thenComparing(e -> ORDEN_NIVEL.getOrDefault(e.getKey(), -1)))
				.orElseThrow().getKey();
	}
}
