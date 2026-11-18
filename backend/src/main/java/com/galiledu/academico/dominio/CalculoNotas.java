package com.galiledu.academico.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Nota oficial de una competencia: una por periodo y una final, aunque la evalúen varios cursos del grado.
 *
 * <ol>
 * <li><b>Ítems → aporte del curso en el periodo:</b> la regla de nota del año sobre los ítems del curso que apuntan a
 * la competencia (es el «Promedio final» del consolidado del docente).</li>
 * <li><b>Aportes → nota de la competencia en el periodo:</b> con PROMEDIO_PONDERADO se usa el peso de cada curso del
 * plan de evaluación; con PROMEDIO_SIMPLE los cursos pesan igual; en la escala cualitativa cada curso vota por su
 * nivel con su peso del plan (gana el nivel con más peso; si empatan, el mayor).</li>
 * <li><b>Notas de periodo → nota final:</b> la consolidación del año (promedio de periodos o último periodo).</li>
 * </ol>
 *
 * Los pasos intermedios no se redondean: solo se redondea lo que se muestra, a 2 decimales. Esta clase no toca la base
 * de datos.
 */
public final class CalculoNotas {
	public static final String PROMEDIO_PONDERADO = "PROMEDIO_PONDERADO";
	public static final String ULTIMO_NIVEL = "ULTIMO_NIVEL";
	public static final String ULTIMO_PERIODO = "ULTIMO_PERIODO";

	private static final int ESCALA_INTERNA = 10;
	private static final BigDecimal PESO_TOTAL = new BigDecimal("100");
	private static final Map<String, Integer> ORDEN_NIVEL = Map.of("C", 0, "B", 1, "A", 2, "AD", 3);

	private CalculoNotas() {
	}

	/** Una nota por combinar: numérica (vigesimal) o nivel (cualitativa). orden sirve para «último». */
	public record Nota(BigDecimal peso, BigDecimal numerica, String nivel, Instant orden) {
		public boolean calificada() {
			return numerica != null || nivel != null;
		}
	}

	/** Un curso que evalúa la competencia en el grado, con su peso del plan (null = sin definir). */
	public record CursoPeso(UUID planCursoId, BigDecimal peso) {
	}

	/** Un ítem de un curso y periodo para un estudiante. Sin nota numérica ni nivel, aún no está calificado. */
	public record Calificacion(UUID planCursoId, UUID periodoId, BigDecimal pesoItem, BigDecimal numerica,
			String nivel, Instant orden) {
	}

	/** Resultado de un periodo: la nota de la competencia y el aporte de cada curso. */
	public record ResultadoPeriodo(UUID periodoId, Optional<Nota> nota, boolean parcial,
			Map<UUID, Optional<Nota>> aportes) {
	}

	/** Resultado de un estudiante en una competencia. */
	public record ResultadoCompetencia(List<ResultadoPeriodo> periodos, Optional<Nota> notaFinal, boolean parcial) {
	}

	/**
	 * Calcula la nota de una competencia para un estudiante. Cuando la regla usa el peso de los cursos (promedio
	 * ponderado o escala cualitativa) y los pesos están incompletos, no hay nota de competencia (solo aportes):
	 * normalizar pesos que el colegio no terminó de definir inventaría la nota.
	 *
	 * @param periodos
	 *            ids de los periodos del año, en orden
	 */
	public static ResultadoCompetencia notaCompetencia(List<UUID> periodos, List<CursoPeso> cursos,
			List<Calificacion> calificaciones, String regla, String consolidacion) {
		boolean ponderado = PROMEDIO_PONDERADO.equals(regla);
		boolean cualitativa = esCualitativa(regla);
		boolean calculable = !usaPesosDeCurso(regla) || pesosCompletos(cursos.stream().map(CursoPeso::peso).toList());
		List<ResultadoPeriodo> resultados = new ArrayList<>();
		for (UUID periodoId : periodos) {
			Map<UUID, Optional<Nota>> aportes = new LinkedHashMap<>();
			boolean parcial = false;
			List<Nota> paraCombinar = new ArrayList<>();
			for (CursoPeso curso : cursos) {
				List<Calificacion> items = calificaciones.stream()
						.filter(c -> c.planCursoId().equals(curso.planCursoId()) && c.periodoId().equals(periodoId))
						.toList();
				Optional<Nota> aporte = combinar(items.stream()
						.map(c -> new Nota(ponderado ? c.pesoItem() : BigDecimal.ONE, c.numerica(), c.nivel(),
								c.orden()))
						.toList(), regla);
				aportes.put(curso.planCursoId(), aporte);
				// Un ítem del curso sin calificar, o un curso sin ninguna nota, deja la nota a medias.
				parcial |= items.stream().anyMatch(c -> c.numerica() == null && c.nivel() == null) || aporte.isEmpty();
				aporte.ifPresent(
						a -> paraCombinar.add(new Nota(ponderado || cualitativa ? curso.peso() : BigDecimal.ONE,
								a.numerica(), a.nivel(), a.orden())));
			}
			Optional<Nota> nota = !calculable
					? Optional.empty()
					: cualitativa ? votoPonderado(paraCombinar) : combinar(paraCombinar, regla);
			resultados.add(new ResultadoPeriodo(periodoId, nota, nota.isPresent() && parcial, aportes));
		}
		return consolidarCompetencia(resultados, consolidacion, regla);
	}

	/** Regla de la escala cualitativa (AD, A, B, C): nivel más frecuente o último nivel. */
	public static boolean esCualitativa(String regla) {
		return "NIVEL_MAS_FRECUENTE".equals(regla) || ULTIMO_NIVEL.equals(regla);
	}

	/**
	 * El peso de cada curso del plan cuenta con promedio ponderado (pondera las notas) y en la escala cualitativa (pesa
	 * el voto de cada curso). Con promedio simple los cursos aportan por igual.
	 */
	public static boolean usaPesosDeCurso(String regla) {
		return PROMEDIO_PONDERADO.equals(regla) || esCualitativa(regla);
	}

	/**
	 * Escala cualitativa, cursos → competencia: cada curso vota por su nivel con su peso del plan; gana el nivel con
	 * más
	 * peso acumulado y, si empatan, el nivel mayor. Así un curso que pesa 60 % no queda empatado con uno de 40 %. Los
	 * cursos sin nota en el periodo no votan.
	 */
	public static Optional<Nota> votoPonderado(List<Nota> niveles) {
		List<Nota> conNivel = niveles.stream().filter(n -> n.nivel() != null).toList();
		if (conNivel.isEmpty()) {
			return Optional.empty();
		}
		Map<String, BigDecimal> votos = new LinkedHashMap<>();
		conNivel.forEach(n -> votos.merge(n.nivel(), n.peso() == null ? BigDecimal.ZERO : n.peso(), BigDecimal::add));
		String ganador = votos.entrySet().stream()
				.max(Map.Entry.<String, BigDecimal>comparingByValue()
						.thenComparing(e -> ORDEN_NIVEL.getOrDefault(e.getKey(), -1)))
				.orElseThrow().getKey();
		Instant orden = conNivel.stream().map(Nota::orden).filter(Objects::nonNull).max(Comparator.naturalOrder())
				.orElse(null);
		return Optional.of(new Nota(BigDecimal.ONE, null, ganador, orden));
	}

	private static ResultadoCompetencia consolidarCompetencia(List<ResultadoPeriodo> periodos, String consolidacion,
			String regla) {
		List<Nota> notas = periodos.stream().map(p -> p.nota().orElse(new Nota(BigDecimal.ONE, null, null, null)))
				.toList();
		Optional<Nota> notaFinal = consolidar(notas, consolidacion, regla);
		if (notaFinal.isEmpty()) {
			return new ResultadoCompetencia(periodos, notaFinal, false);
		}
		boolean parcial;
		if (ULTIMO_PERIODO.equals(consolidacion)) {
			// Solo cuenta el último periodo del año: es parcial mientras ese periodo no tenga su nota completa.
			ResultadoPeriodo ultimo = periodos.get(periodos.size() - 1);
			parcial = ultimo.nota().isEmpty() || ultimo.parcial();
		} else {
			parcial = periodos.stream().anyMatch(p -> p.nota().isEmpty() || p.parcial());
		}
		return new ResultadoCompetencia(periodos, notaFinal, parcial);
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
	 * Consolida las notas de los periodos, en orden. ULTIMO_PERIODO toma el último con nota; PROMEDIO_PERIODOS los
	 * promedia por igual. En la escala cualitativa no hay promedio: se aplica la regla de nota del año a los niveles.
	 */
	public static Optional<Nota> consolidar(List<Nota> porPeriodo, String consolidacion, String regla) {
		if (ULTIMO_PERIODO.equals(consolidacion)) {
			return porPeriodo.stream().filter(Nota::calificada).reduce((anterior, siguiente) -> siguiente);
		}
		String reglaPeriodos = PROMEDIO_PONDERADO.equals(regla) ? "PROMEDIO_SIMPLE" : regla;
		return combinar(porPeriodo.stream().map(n -> new Nota(BigDecimal.ONE, n.numerica(), n.nivel(), n.orden()))
				.toList(), reglaPeriodos);
	}

	/** Pesos de una competencia entre sus cursos: completos si todos tienen peso y suman exactamente 100 %. */
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

	/**
	 * Aprobado con A o AD. En la escala vigesimal equivale a que la nota, redondeada al entero (0,5 sube), llegue al
	 * inicio del rango A: así el estado nunca contradice la letra.
	 */
	public static String estado(Nota nota, int notaAprobatoria) {
		if (nota.numerica() != null) {
			return entero(nota.numerica()) >= notaAprobatoria ? "Aprobado" : "Desaprobado";
		}
		return "A".equals(nota.nivel()) || "AD".equals(nota.nivel()) ? "Aprobado" : "Desaprobado";
	}

	/** Rango de la tabla de conversión vigesimal → nivel de logro (academico.tabla_conversion_nivel). */
	public record Rango(String nivel, int notaDesde, int notaHasta) {
	}

	/** Si no hay ninguna versión vigente de la tabla de conversión: la escala habitual de MINEDU. */
	public static final List<Rango> RANGOS_POR_DEFECTO = List.of(new Rango("AD", 18, 20), new Rango("A", 14, 17),
			new Rango("B", 11, 13), new Rango("C", 0, 10));

	/**
	 * Nivel de logro (AD, A, B o C) de una nota. En la escala vigesimal la nota se redondea al entero, 0,5 sube (16,5 →
	 * 17; 13,5 → 14), y se busca en los rangos enteros de la tabla de conversión. En la escala cualitativa ya es un
	 * nivel.
	 */
	public static String nivel(Nota nota, List<Rango> rangos) {
		if (nota.numerica() == null) {
			return nota.nivel();
		}
		int entero = entero(nota.numerica());
		return rangos.stream().filter(r -> r.notaDesde() <= entero && entero <= r.notaHasta()).map(Rango::nivel)
				.findFirst().orElse(null);
	}

	/** Inicio del rango A: la nota aprobatoria de esa versión de la tabla. */
	public static int notaAprobatoria(List<Rango> rangos) {
		return rangos.stream().filter(r -> "A".equals(r.nivel())).mapToInt(Rango::notaDesde).findFirst().orElse(14);
	}

	/**
	 * Redondeo al entero para convertir a letra y decidir el estado: 0,5 sube. Parte de la nota que se muestra (2
	 * decimales), para que un 13,495 que se ve como «13,5» también cuente como 14.
	 */
	public static int entero(BigDecimal nota) {
		return nota.setScale(2, RoundingMode.HALF_UP).setScale(0, RoundingMode.HALF_UP).intValueExact();
	}

	private static BigDecimal promedio(List<Nota> calificadas, String regla) {
		List<Nota> numericas = calificadas.stream().filter(n -> n.numerica() != null).toList();
		if (PROMEDIO_PONDERADO.equals(regla)) {
			BigDecimal pesos = numericas.stream().map(n -> n.peso() == null ? BigDecimal.ZERO : n.peso())
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			if (pesos.signum() > 0) {
				BigDecimal suma = numericas.stream()
						.map(n -> n.numerica().multiply(n.peso() == null ? BigDecimal.ZERO : n.peso()))
						.reduce(BigDecimal.ZERO, BigDecimal::add);
				return suma.divide(pesos, ESCALA_INTERNA, RoundingMode.HALF_UP);
			}
		}
		return numericas.stream().map(Nota::numerica).reduce(BigDecimal.ZERO, BigDecimal::add)
				.divide(BigDecimal.valueOf(numericas.size()), ESCALA_INTERNA, RoundingMode.HALF_UP);
	}

	private static String nivel(List<Nota> calificadas, String regla) {
		List<Nota> conNivel = calificadas.stream().filter(n -> n.nivel() != null).toList();
		if (ULTIMO_NIVEL.equals(regla)) {
			return conNivel.stream()
					.max(Comparator.comparing(Nota::orden, Comparator.nullsFirst(Comparator.naturalOrder())))
					.orElseThrow().nivel();
		}
		return conNivel.stream().collect(Collectors.groupingBy(Nota::nivel, Collectors.counting())).entrySet().stream()
				.max(Map.Entry.<String, Long>comparingByValue()
						.thenComparing(e -> ORDEN_NIVEL.getOrDefault(e.getKey(), -1)))
				.orElseThrow().getKey();
	}
}
