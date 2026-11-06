package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.galiledu.academico.dominio.CalculoNotas;
import com.galiledu.academico.dominio.CalculoNotas.Nota;

class CalculoNotasTests {
	private static final Instant LUNES = Instant.parse("2026-11-02T10:00:00Z");
	private static final Instant MARTES = Instant.parse("2026-11-03T10:00:00Z");
	private static final Instant MIERCOLES = Instant.parse("2026-11-04T10:00:00Z");

	private static Nota numerica(String peso, String nota) {
		return new Nota(new BigDecimal(peso), new BigDecimal(nota), null, LUNES);
	}

	private static Nota nivel(String nivel, Instant orden) {
		return new Nota(BigDecimal.ONE, null, nivel, orden);
	}

	private static String combinar(String regla, Nota... notas) {
		return CalculoNotas.combinar(List.of(notas), regla).map(CalculoNotas::texto).orElse("sin nota");
	}

	@Test
	void elPromedioPonderadoNormalizaPorLosPesosDeLoCalificado() {
		assertThat(combinar("PROMEDIO_PONDERADO", numerica("15", "20"))).isEqualTo("20");
		assertThat(combinar("PROMEDIO_PONDERADO", numerica("40", "20"), numerica("60", "10"))).isEqualTo("14");
	}

	@Test
	void loQueNoEstaCalificadoNoCuenta() {
		Nota pendiente = new Nota(new BigDecimal("60"), null, null, LUNES);
		assertThat(combinar("PROMEDIO_PONDERADO", numerica("40", "18"), pendiente)).isEqualTo("18");
		assertThat(combinar("PROMEDIO_PONDERADO", pendiente)).isEqualTo("sin nota");
	}

	@Test
	void elPromedioSimpleIgnoraLosPesos() {
		assertThat(combinar("PROMEDIO_SIMPLE", numerica("90", "20"), numerica("10", "10"))).isEqualTo("15");
	}

	@Test
	void redondeaSoloAlMostrar() {
		// 15,333… · 2 cursos: redondear antes de combinar daría 15,34.
		Nota curso = CalculoNotas.combinar(List.of(numerica("1", "15"), numerica("1", "15"), numerica("1", "16")),
				"PROMEDIO_SIMPLE").orElseThrow();
		assertThat(CalculoNotas.texto(curso)).isEqualTo("15.33");
		Nota doble = CalculoNotas.combinar(List.of(curso, curso), "PROMEDIO_SIMPLE").orElseThrow();
		assertThat(CalculoNotas.texto(doble)).isEqualTo("15.33");
		assertThat(CalculoNotas.texto(numerica("1", "20.00"))).isEqualTo("20");
	}

	@Test
	void elNivelMasFrecuenteDesempataPorElMayor() {
		assertThat(combinar("NIVEL_MAS_FRECUENTE", nivel("B", LUNES), nivel("B", MARTES), nivel("AD", MIERCOLES)))
				.isEqualTo("B");
		assertThat(combinar("NIVEL_MAS_FRECUENTE", nivel("A", LUNES), nivel("C", MARTES))).isEqualTo("A");
	}

	@Test
	void elUltimoNivelTomaLaNotaMasReciente() {
		assertThat(combinar("ULTIMO_NIVEL", nivel("C", MARTES), nivel("AD", LUNES))).isEqualTo("C");
	}

	@Test
	void consolidaPeriodosPromediandoOTomandoElUltimoConNotas() {
		List<Nota> periodos = List.of(numerica("1", "10"), numerica("1", "20"));
		assertThat(CalculoNotas.consolidar(periodos, "PROMEDIO_PERIODOS", "PROMEDIO_PONDERADO")
				.map(CalculoNotas::texto)).contains("15");
		assertThat(CalculoNotas.consolidar(periodos, "ULTIMO_PERIODO", "PROMEDIO_PONDERADO").map(CalculoNotas::texto))
				.contains("20");
		assertThat(CalculoNotas.consolidar(List.of(), "ULTIMO_PERIODO", "PROMEDIO_PONDERADO")).isEmpty();
	}

	@Test
	void enLaEscalaCualitativaLosPeriodosSiguenLaReglaDeNota() {
		List<Nota> periodos = List.of(nivel("B", LUNES), nivel("A", MARTES), nivel("B", MIERCOLES));
		assertThat(CalculoNotas.consolidar(periodos, "PROMEDIO_PERIODOS", "NIVEL_MAS_FRECUENTE")
				.map(CalculoNotas::texto)).contains("B");
		assertThat(CalculoNotas.consolidar(periodos, "ULTIMO_PERIODO", "NIVEL_MAS_FRECUENTE").map(CalculoNotas::texto))
				.contains("B");
		assertThat(CalculoNotas.consolidar(List.of(nivel("C", LUNES), nivel("AD", MARTES)), "ULTIMO_PERIODO",
				"ULTIMO_NIVEL").map(CalculoNotas::texto)).contains("AD");
	}

	@Test
	void losPesosEstanCompletosSiTodosExistenYSuman100() {
		assertThat(CalculoNotas.pesosCompletos(List.of(new BigDecimal("33.33"), new BigDecimal("66.67")))).isTrue();
		assertThat(CalculoNotas.pesosCompletos(List.of(new BigDecimal("100.00")))).isTrue();
		assertThat(CalculoNotas.pesosCompletos(List.of(new BigDecimal("15")))).isFalse();
		assertThat(CalculoNotas.pesosCompletos(Arrays.asList(new BigDecimal("100"), null))).isFalse();
		assertThat(CalculoNotas.pesosCompletos(List.of())).isFalse();
	}

	@Test
	void apruebaDesdeLaNotaAprobatoriaODesdeElNivelA() {
		assertThat(CalculoNotas.estado(numerica("1", "14"), 14)).isEqualTo("Aprobado");
		assertThat(CalculoNotas.estado(numerica("1", "13.99"), 14)).isEqualTo("Desaprobado");
		assertThat(CalculoNotas.estado(nivel("A", LUNES), 14)).isEqualTo("Aprobado");
		assertThat(CalculoNotas.estado(nivel("B", LUNES), 14)).isEqualTo("Desaprobado");
	}
}
