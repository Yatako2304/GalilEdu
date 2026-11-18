package com.galiledu.academico.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.galiledu.academico.dominio.CalculoNotas.Calificacion;
import com.galiledu.academico.dominio.CalculoNotas.CursoPeso;
import com.galiledu.academico.dominio.CalculoNotas.ResultadoCompetencia;

/**
 * Nota oficial de una competencia que evalúan varios cursos: ítems → aporte del curso → nota del periodo → nota final.
 */
class CalculoNotasTests {
	private static final UUID ALGEBRA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
	private static final UUID ARITMETICA = UUID.fromString("00000000-0000-0000-0000-00000000000b");
	private static final UUID P1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID P2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final List<UUID> PERIODOS = List.of(P1, P2);

	private static final List<CursoPeso> CURSOS_40_60 = List.of(new CursoPeso(ALGEBRA, new BigDecimal("40")),
			new CursoPeso(ARITMETICA, new BigDecimal("60")));

	private static Calificacion nota(UUID curso, UUID periodo, String nota) {
		return nota(curso, periodo, "100", nota);
	}

	private static Calificacion nota(UUID curso, UUID periodo, String pesoItem, String nota) {
		return new Calificacion(curso, periodo, new BigDecimal(pesoItem), nota == null ? null : new BigDecimal(nota),
				null, Instant.parse("2026-05-01T10:00:00Z"));
	}

	private static Calificacion nivel(UUID curso, UUID periodo, String nivel, String instante) {
		return new Calificacion(curso, periodo, null, null, nivel, Instant.parse(instante));
	}

	private static String texto(ResultadoCompetencia r, int periodo) {
		return r.periodos().get(periodo).nota().map(CalculoNotas::texto).orElse("—");
	}

	@Test
	void combinaLosCursosConSuPesoEnCadaPeriodoYPromediaLosPeriodos() {
		// Ejemplo de la ficha: P1 16 y 12 → 13,6; P2 18 y 14 → 15,6; final 14,6.
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(PERIODOS, CURSOS_40_60,
				List.of(nota(ALGEBRA, P1, "16"), nota(ARITMETICA, P1, "12"), nota(ALGEBRA, P2, "18"),
						nota(ARITMETICA, P2, "14")),
				"PROMEDIO_PONDERADO", "PROMEDIO_PERIODOS");

		assertThat(texto(r, 0)).isEqualTo("13.6");
		assertThat(texto(r, 1)).isEqualTo("15.6");
		assertThat(r.notaFinal().map(CalculoNotas::texto)).contains("14.6");
		assertThat(r.parcial()).isFalse();
		assertThat(r.periodos().get(0).aportes().get(ALGEBRA).map(CalculoNotas::texto)).contains("16");
	}

	@Test
	void dentroDeCadaCursoUsaElPesoDeLosItems() {
		// Álgebra: 20 (30 %) y 10 (70 %) = 13. Con Aritmética 13 → la competencia es 13.
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(List.of(P1), CURSOS_40_60,
				List.of(nota(ALGEBRA, P1, "30", "20"), nota(ALGEBRA, P1, "70", "10"), nota(ARITMETICA, P1, "13")),
				"PROMEDIO_PONDERADO", "PROMEDIO_PERIODOS");

		assertThat(texto(r, 0)).isEqualTo("13");
	}

	@Test
	void unCursoSinNotasNoCuentaYLaNotaQuedaParcial() {
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(List.of(P1), CURSOS_40_60,
				List.of(nota(ARITMETICA, P1, "12")), "PROMEDIO_PONDERADO", "PROMEDIO_PERIODOS");

		assertThat(texto(r, 0)).isEqualTo("12");
		assertThat(r.periodos().get(0).parcial()).isTrue();
		assertThat(r.periodos().get(0).aportes().get(ALGEBRA)).isEmpty();
	}

	@Test
	void unItemSinCalificarDejaLaNotaParcial() {
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(List.of(P1), CURSOS_40_60,
				List.of(nota(ALGEBRA, P1, "16"), nota(ARITMETICA, P1, "50", "12"), nota(ARITMETICA, P1, "50", null)),
				"PROMEDIO_PONDERADO", "PROMEDIO_PERIODOS");

		assertThat(texto(r, 0)).isEqualTo("13.6");
		assertThat(r.periodos().get(0).parcial()).isTrue();
	}

	@Test
	void conPesosIncompletosNoHayNotaDeCompetenciaPeroSiAportes() {
		List<CursoPeso> incompletos = List.of(new CursoPeso(ALGEBRA, new BigDecimal("40")),
				new CursoPeso(ARITMETICA, null));
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(List.of(P1), incompletos,
				List.of(nota(ALGEBRA, P1, "16"), nota(ARITMETICA, P1, "12")), "PROMEDIO_PONDERADO",
				"PROMEDIO_PERIODOS");

		assertThat(r.periodos().get(0).nota()).isEmpty();
		assertThat(r.notaFinal()).isEmpty();
		assertThat(r.periodos().get(0).aportes().get(ARITMETICA).map(CalculoNotas::texto)).contains("12");
	}

	@Test
	void conPromedioSimpleLosCursosPesanIgual() {
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(List.of(P1), CURSOS_40_60,
				List.of(nota(ALGEBRA, P1, "16"), nota(ARITMETICA, P1, "12")), "PROMEDIO_SIMPLE", "PROMEDIO_PERIODOS");

		assertThat(texto(r, 0)).isEqualTo("14");
	}

	@Test
	void conUltimoPeriodoLaFinalEsElUltimoConNotaYEsParcialSiFaltaElUltimo() {
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(PERIODOS, CURSOS_40_60,
				List.of(nota(ALGEBRA, P1, "16"), nota(ARITMETICA, P1, "12")), "PROMEDIO_PONDERADO",
				"ULTIMO_PERIODO");

		assertThat(r.notaFinal().map(CalculoNotas::texto)).contains("13.6");
		assertThat(r.parcial()).isTrue();
	}

	@Test
	void mientrasFalteUnPeriodoLaFinalEsParcial() {
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(PERIODOS, CURSOS_40_60,
				List.of(nota(ALGEBRA, P1, "16"), nota(ARITMETICA, P1, "12")), "PROMEDIO_PONDERADO",
				"PROMEDIO_PERIODOS");

		assertThat(r.notaFinal().map(CalculoNotas::texto)).contains("13.6");
		assertThat(r.parcial()).isTrue();
		assertThat(r.periodos().get(1).nota()).isEmpty();
	}

	@Test
	void enLaEscalaCualitativaCadaCursoVotaConSuPeso() {
		// Álgebra (40 %) B y Aritmética (60 %) A: gana A con 60.
		ResultadoCompetencia a = CalculoNotas.notaCompetencia(List.of(P1), CURSOS_40_60,
				List.of(nivel(ALGEBRA, P1, "B", "2026-05-01T10:00:00Z"),
						nivel(ARITMETICA, P1, "A", "2026-05-02T10:00:00Z")),
				"NIVEL_MAS_FRECUENTE", "ULTIMO_PERIODO");
		assertThat(texto(a, 0)).isEqualTo("A");

		// El de más peso decide aunque su nivel sea menor: Álgebra (40 %) AD y Aritmética (60 %) B → B.
		ResultadoCompetencia b = CalculoNotas.notaCompetencia(List.of(P1), CURSOS_40_60,
				List.of(nivel(ALGEBRA, P1, "AD", "2026-05-03T10:00:00Z"),
						nivel(ARITMETICA, P1, "B", "2026-05-01T10:00:00Z")),
				"NIVEL_MAS_FRECUENTE", "ULTIMO_PERIODO");
		assertThat(texto(b, 0)).isEqualTo("B");
	}

	@Test
	void conUltimoNivelLosCursosTambienVotanConSuPeso() {
		// Dentro de cada curso manda el último ítem; entre cursos decide el peso, no quién calificó al último.
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(List.of(P1), CURSOS_40_60,
				List.of(nivel(ALGEBRA, P1, "C", "2026-05-01T10:00:00Z"),
						nivel(ALGEBRA, P1, "AD", "2026-05-09T10:00:00Z"),
						nivel(ARITMETICA, P1, "A", "2026-05-02T10:00:00Z")),
				"ULTIMO_NIVEL", "ULTIMO_PERIODO");
		assertThat(r.periodos().get(0).aportes().get(ALGEBRA).map(CalculoNotas::texto)).contains("AD");
		assertThat(texto(r, 0)).isEqualTo("A");
	}

	@Test
	void unEmpateDeVotosLoGanaElNivelMayor() {
		List<CursoPeso> mitades = List.of(new CursoPeso(ALGEBRA, new BigDecimal("50")),
				new CursoPeso(ARITMETICA, new BigDecimal("50")));
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(List.of(P1), mitades,
				List.of(nivel(ALGEBRA, P1, "A", "2026-05-01T10:00:00Z"),
						nivel(ARITMETICA, P1, "B", "2026-05-02T10:00:00Z")),
				"NIVEL_MAS_FRECUENTE", "ULTIMO_PERIODO");
		assertThat(texto(r, 0)).isEqualTo("A");
	}

	@Test
	void enLaEscalaCualitativaConPesosIncompletosNoHayNota() {
		List<CursoPeso> incompletos = List.of(new CursoPeso(ALGEBRA, new BigDecimal("40")),
				new CursoPeso(ARITMETICA, null));
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(List.of(P1), incompletos,
				List.of(nivel(ALGEBRA, P1, "A", "2026-05-01T10:00:00Z"),
						nivel(ARITMETICA, P1, "B", "2026-05-02T10:00:00Z")),
				"NIVEL_MAS_FRECUENTE", "ULTIMO_PERIODO");
		assertThat(r.periodos().get(0).nota()).isEmpty();
		assertThat(r.periodos().get(0).aportes().get(ARITMETICA).map(CalculoNotas::texto)).contains("B");
	}

	@Test
	void unCursoSinNotaNoVotaYLaNotaQuedaParcial() {
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(List.of(P1), CURSOS_40_60,
				List.of(nivel(ALGEBRA, P1, "C", "2026-05-01T10:00:00Z")), "NIVEL_MAS_FRECUENTE", "ULTIMO_PERIODO");
		assertThat(texto(r, 0)).isEqualTo("C");
		assertThat(r.periodos().get(0).parcial()).isTrue();
	}

	@Test
	void sinNotasNoHayNotaNiEsParcial() {
		ResultadoCompetencia r = CalculoNotas.notaCompetencia(PERIODOS, CURSOS_40_60, List.of(),
				"PROMEDIO_PONDERADO", "PROMEDIO_PERIODOS");

		assertThat(r.notaFinal()).isEmpty();
		assertThat(r.parcial()).isFalse();
	}

	@Test
	void losPesosSonCompletosSoloSiTodosTienenPesoYSuman100() {
		assertThat(CalculoNotas.pesosCompletos(List.of(new BigDecimal("40"), new BigDecimal("60")))).isTrue();
		assertThat(CalculoNotas.pesosCompletos(List.of(new BigDecimal("40"), new BigDecimal("50")))).isFalse();
		assertThat(CalculoNotas.pesosCompletos(java.util.Arrays.asList(new BigDecimal("100"), null))).isFalse();
		assertThat(CalculoNotas.pesosCompletos(List.of())).isFalse();
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({"16.5,A", "17.49,A", "17.5,AD", "13.5,A", "13.49,B", "13.495,A",
			"10.5,B", "10.4,C", "20,AD", "0,C"})
	void laLetraSaleDeRedondearAlEnteroYUbicarloEnLaTabla(String nota, String nivel) {
		var valor = new CalculoNotas.Nota(BigDecimal.ONE, new BigDecimal(nota), null, null);
		// C 0–10, B 11–13, A 14–17, AD 18–20. 13,495 se muestra como 13,5 y cuenta como 14.
		assertThat(CalculoNotas.nivel(valor, CalculoNotas.RANGOS_POR_DEFECTO)).isEqualTo(nivel);
	}

	@Test
	void elEstadoNuncaContradiceLaLetra() {
		var limite = new CalculoNotas.Nota(BigDecimal.ONE, new BigDecimal("13.5"), null, null);
		assertThat(CalculoNotas.nivel(limite, CalculoNotas.RANGOS_POR_DEFECTO)).isEqualTo("A");
		assertThat(CalculoNotas.estado(limite, CalculoNotas.notaAprobatoria(CalculoNotas.RANGOS_POR_DEFECTO)))
				.isEqualTo("Aprobado");
		var debajo = new CalculoNotas.Nota(BigDecimal.ONE, new BigDecimal("13.49"), null, null);
		assertThat(CalculoNotas.estado(debajo, 14)).isEqualTo("Desaprobado");
	}

	@Test
	void usaLosRangosDeLaVersionDeLaTablaYEnLaCualitativaYaEsUnNivel() {
		// Versión programada para 2027: A empieza en 15.
		var rangos2027 = List.of(new CalculoNotas.Rango("AD", 18, 20), new CalculoNotas.Rango("A", 15, 17),
				new CalculoNotas.Rango("B", 11, 14), new CalculoNotas.Rango("C", 0, 10));
		var catorce = new CalculoNotas.Nota(BigDecimal.ONE, new BigDecimal("14.4"), null, null);
		assertThat(CalculoNotas.nivel(catorce, rangos2027)).isEqualTo("B");
		assertThat(CalculoNotas.notaAprobatoria(rangos2027)).isEqualTo(15);
		assertThat(CalculoNotas.nivel(new CalculoNotas.Nota(BigDecimal.ONE, null, "AD", null), rangos2027))
				.isEqualTo("AD");
	}

	@Test
	void elEstadoSeDecideConLaNotaRedondeada() {
		var nota = new CalculoNotas.Nota(BigDecimal.ONE, new BigDecimal("13.995"), null, null);
		assertThat(CalculoNotas.texto(nota)).isEqualTo("14");
		assertThat(CalculoNotas.estado(nota, 14)).isEqualTo("Aprobado");
	}
}
