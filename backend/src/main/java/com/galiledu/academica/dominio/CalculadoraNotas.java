package com.galiledu.academica.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Aplica las reglas de agregación y consolidación de RF-130 sin acceder a datos.
 * Las notas vigesimales calculadas se redondean a dos decimales (NUMERIC(5,2)).
 */
public final class CalculadoraNotas {
	private static final int DECIMALES = 2;

	private CalculadoraNotas() {
	}

	/** Nota de un periodo a partir de las calificaciones de sus ítems. */
	public static Calificacion agregarItems(ConfiguracionCompetencia configuracion, List<NotaItem> notas) {
		Objects.requireNonNull(configuracion, "La configuración es obligatoria");
		exigirCalculable(configuracion);
		List<NotaItem> lista = exigirNotas(notas, configuracion.escala(), NotaItem::valor);
		return switch (configuracion.reglaAgregacion()) {
			case PROMEDIO_SIMPLE -> Calificacion.vigesimal(promedio(lista.stream()
				.map(nota -> nota.valor().vigesimal()).toList()));
			case PROMEDIO_PONDERADO -> Calificacion.vigesimal(promedioPonderado(lista));
			case MODA -> moda(lista.stream().map(NotaItem::valor).toList());
			case ULTIMO_LOGRO -> lista.stream()
				.max(Comparator.comparing(NotaItem::fechaEvaluacion, Comparator.nullsFirst(Comparator.naturalOrder()))
					.thenComparing(NotaItem::fechaRegistro))
				.orElseThrow().valor();
		};
	}

	/** Nota final de la competencia a partir de las notas de periodo disponibles. */
	public static Calificacion consolidarPeriodos(ConfiguracionCompetencia configuracion,
		List<NotaPeriodo> notas) {
		Objects.requireNonNull(configuracion, "La configuración es obligatoria");
		exigirCalculable(configuracion);
		List<NotaPeriodo> lista = exigirNotas(notas, configuracion.escala(), NotaPeriodo::valor);
		return switch (configuracion.reglaConsolidacion()) {
			case ULTIMO_PERIODO -> lista.stream().max(Comparator.comparingInt(NotaPeriodo::orden))
				.orElseThrow().valor();
			case PROMEDIO_PERIODOS -> Calificacion.vigesimal(promedio(lista.stream()
				.map(nota -> nota.valor().vigesimal()).toList()));
		};
	}

	/** Añade el equivalente AD/A/B/C solo cuando la nota es vigesimal y la tabla lo define. */
	public static NotaConsolidada conEquivalente(Calificacion valor, TablaConversion tabla) {
		Objects.requireNonNull(valor, "El valor es obligatorio");
		if (valor.escala() != EscalaCalificacion.VIGESIMAL || tabla == null) {
			return new NotaConsolidada(valor, null);
		}
		return new NotaConsolidada(valor, tabla.convertir(valor.vigesimal()).orElse(null));
	}

	private static void exigirCalculable(ConfiguracionCompetencia configuracion) {
		if (!configuracion.admiteCalculo()) {
			throw new CalculoNotaIndeterminadoException(
				"La escala cualitativa no admite promedios sin una equivalencia aprobada");
		}
	}

	private static <T> List<T> exigirNotas(List<T> notas, EscalaCalificacion escala,
		Function<T, Calificacion> valor) {
		if (notas == null || notas.isEmpty()) {
			throw new CalculoNotaIndeterminadoException("No hay notas para calcular");
		}
		if (notas.stream().anyMatch(nota -> valor.apply(nota).escala() != escala)) {
			throw new IllegalArgumentException("Todas las notas deben usar la escala configurada");
		}
		return List.copyOf(notas);
	}

	private static BigDecimal promedio(List<BigDecimal> valores) {
		BigDecimal suma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return suma.divide(BigDecimal.valueOf(valores.size()), DECIMALES, RoundingMode.HALF_UP);
	}

	private static BigDecimal promedioPonderado(List<NotaItem> notas) {
		if (notas.stream().anyMatch(nota -> nota.peso() == null)) {
			throw new CalculoNotaIndeterminadoException("Todos los ítems requieren peso para el promedio ponderado");
		}
		BigDecimal pesos = notas.stream().map(NotaItem::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (pesos.signum() == 0) {
			throw new CalculoNotaIndeterminadoException("La suma de pesos no puede ser cero");
		}
		BigDecimal suma = notas.stream().map(nota -> nota.valor().vigesimal().multiply(nota.peso()))
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return suma.divide(pesos, DECIMALES, RoundingMode.HALF_UP);
	}

	private static Calificacion moda(List<Calificacion> valores) {
		Map<Object, Long> frecuencias = new LinkedHashMap<>();
		Map<Object, Calificacion> representantes = new LinkedHashMap<>();
		for (Calificacion valor : valores) {
			Object clave = valor.escala() == EscalaCalificacion.VIGESIMAL
				? valor.vigesimal().stripTrailingZeros() : valor.cualitativa();
			frecuencias.merge(clave, 1L, Long::sum);
			representantes.putIfAbsent(clave, valor);
		}
		long maxima = frecuencias.values().stream().mapToLong(Long::longValue).max().orElseThrow();
		List<Object> modas = frecuencias.entrySet().stream().filter(entrada -> entrada.getValue() == maxima)
			.map(Map.Entry::getKey).toList();
		if (modas.size() != 1) {
			throw new CalculoNotaIndeterminadoException("La moda tiene empate; se requiere una regla de desempate");
		}
		return representantes.get(modas.getFirst());
	}

	/** Calificación de un ítem con los datos que necesitan las reglas de agregación. */
	public record NotaItem(Calificacion valor, BigDecimal peso, LocalDate fechaEvaluacion, Instant fechaRegistro) {
		public NotaItem {
			Objects.requireNonNull(valor, "El valor es obligatorio");
			Objects.requireNonNull(fechaRegistro, "La fecha de registro es obligatoria");
		}
	}

	/** Nota ya calculada de un periodo, identificada por su orden dentro del año. */
	public record NotaPeriodo(int orden, Calificacion valor) {
		public NotaPeriodo {
			if (orden <= 0) throw new IllegalArgumentException("El orden del periodo debe ser positivo");
			Objects.requireNonNull(valor, "El valor es obligatorio");
		}
	}
}
