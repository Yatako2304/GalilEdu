package com.galiledu.academica.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.galiledu.academica.dominio.CalculadoraNotas.NotaItem;
import com.galiledu.academica.dominio.CalculadoraNotas.NotaPeriodo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraNotasTests {
	private static final Instant REGISTRO = Instant.parse("2026-05-01T12:00:00Z");

	@Test
	void promediaYRedondeaADosDecimales() {
		var configuracion = vigesimal(ReglaAgregacionItems.PROMEDIO_SIMPLE, ReglaConsolidacionPeriodos.ULTIMO_PERIODO);
		assertThat(CalculadoraNotas.agregarItems(configuracion, List.of(nota("15"), nota("16"), nota("16")))
			.vigesimal()).isEqualByComparingTo("15.67");
	}

	@Test
	void ponderaConPesosYExigeTodosLosPesos() {
		var configuracion = vigesimal(ReglaAgregacionItems.PROMEDIO_PONDERADO, ReglaConsolidacionPeriodos.ULTIMO_PERIODO);
		var notas = List.of(nota("20", "3", null), nota("10", "1", null));
		assertThat(CalculadoraNotas.agregarItems(configuracion, notas).vigesimal()).isEqualByComparingTo("17.50");
		assertThatThrownBy(() -> CalculadoraNotas.agregarItems(configuracion,
			List.of(nota("20", "3", null), nota("10"))))
			.isInstanceOf(CalculoNotaIndeterminadoException.class);
	}

	@Test
	void modaCualitativaYEmpateSinRegla() {
		var configuracion = new ConfiguracionCompetencia(EscalaCalificacion.CUALITATIVA,
			ReglaAgregacionItems.MODA, ReglaConsolidacionPeriodos.ULTIMO_PERIODO);
		assertThat(CalculadoraNotas.agregarItems(configuracion, List.of(nivel(NivelCualitativo.A),
			nivel(NivelCualitativo.A), nivel(NivelCualitativo.B))).cualitativa()).isEqualTo(NivelCualitativo.A);
		assertThatThrownBy(() -> CalculadoraNotas.agregarItems(configuracion,
			List.of(nivel(NivelCualitativo.A), nivel(NivelCualitativo.B))))
			.isInstanceOf(CalculoNotaIndeterminadoException.class);
	}

	@Test
	void ultimoLogroUsaLaFechaDeEvaluacionMasReciente() {
		var configuracion = vigesimal(ReglaAgregacionItems.ULTIMO_LOGRO, ReglaConsolidacionPeriodos.ULTIMO_PERIODO);
		var notas = List.of(nota("18", null, LocalDate.of(2026, 4, 10)), nota("12", null, LocalDate.of(2026, 3, 1)));
		assertThat(CalculadoraNotas.agregarItems(configuracion, notas).vigesimal()).isEqualByComparingTo("18");
	}

	@Test
	void consolidaPeriodosPorUltimoOPromedio() {
		var periodos = List.of(new NotaPeriodo(2, Calificacion.vigesimal(new BigDecimal("14"))),
			new NotaPeriodo(1, Calificacion.vigesimal(new BigDecimal("17"))));
		assertThat(CalculadoraNotas.consolidarPeriodos(vigesimal(ReglaAgregacionItems.PROMEDIO_SIMPLE,
			ReglaConsolidacionPeriodos.ULTIMO_PERIODO), periodos).vigesimal()).isEqualByComparingTo("14");
		assertThat(CalculadoraNotas.consolidarPeriodos(vigesimal(ReglaAgregacionItems.PROMEDIO_SIMPLE,
			ReglaConsolidacionPeriodos.PROMEDIO_PERIODOS), periodos).vigesimal()).isEqualByComparingTo("15.50");
	}

	@Test
	void cualitativaNoAdmitePromediosNiEscalasMezcladas() {
		var cualitativa = new ConfiguracionCompetencia(EscalaCalificacion.CUALITATIVA,
			ReglaAgregacionItems.PROMEDIO_SIMPLE, ReglaConsolidacionPeriodos.ULTIMO_PERIODO);
		assertThatThrownBy(() -> CalculadoraNotas.agregarItems(cualitativa, List.of(nivel(NivelCualitativo.A))))
			.isInstanceOf(CalculoNotaIndeterminadoException.class);
		assertThatThrownBy(() -> CalculadoraNotas.agregarItems(vigesimal(ReglaAgregacionItems.MODA,
			ReglaConsolidacionPeriodos.ULTIMO_PERIODO), List.of(nivel(NivelCualitativo.A))))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void convierteConLaTablaVigenteSinInventarHuecos() {
		var tabla = new TablaConversion(List.of(
			rango("18", "20", NivelCualitativo.AD), rango("14", "17.99", NivelCualitativo.A),
			rango("11", "13.99", NivelCualitativo.B), rango("0", "10.99", NivelCualitativo.C)));
		assertThat(CalculadoraNotas.conEquivalente(Calificacion.vigesimal(new BigDecimal("15.5")), tabla)
			.equivalenteCualitativo()).isEqualTo(NivelCualitativo.A);
		assertThat(CalculadoraNotas.conEquivalente(Calificacion.vigesimal(new BigDecimal("17.995")), tabla)
			.equivalenteCualitativo()).isNull();
		assertThatThrownBy(() -> new TablaConversion(List.of(rango("0", "20", NivelCualitativo.AD))))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new TablaConversion(List.of(
			rango("15", "20", NivelCualitativo.AD), rango("14", "17", NivelCualitativo.A),
			rango("11", "13", NivelCualitativo.B), rango("0", "10", NivelCualitativo.C))))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void validaItemsYEvidencias() {
		assertThatThrownBy(() -> ItemEvaluacion.nuevo(UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID(), " ", TipoItemEvaluacion.EXAMEN, null, null))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ItemEvaluacion.nuevo(UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID(), "Examen", TipoItemEvaluacion.EXAMEN, null, new BigDecimal("-1")))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new EvidenciaPedagogica("foto.png", "s3://bucket/foto.png", null, 0L))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void unaCorreccionConservaLaEscalaYLaFechaDeRegistro() {
		var original = CalificacionEstudiante.nueva(UUID.randomUUID(), UUID.randomUUID(),
			Calificacion.vigesimal(new BigDecimal("11")), REGISTRO);
		var corregida = original.corregir(Calificacion.vigesimal(new BigDecimal("13")), REGISTRO.plusSeconds(60));
		assertThat(corregida.fechaRegistro()).isEqualTo(REGISTRO);
		assertThat(corregida.fechaModificacion()).isEqualTo(REGISTRO.plusSeconds(60));
		assertThatThrownBy(() -> original.corregir(Calificacion.cualitativa(NivelCualitativo.A), REGISTRO))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private static ConfiguracionCompetencia vigesimal(ReglaAgregacionItems agregacion,
		ReglaConsolidacionPeriodos consolidacion) {
		return new ConfiguracionCompetencia(EscalaCalificacion.VIGESIMAL, agregacion, consolidacion);
	}

	private static NotaItem nota(String valor) {
		return nota(valor, null, null);
	}

	private static NotaItem nota(String valor, String peso, LocalDate fecha) {
		return new NotaItem(Calificacion.vigesimal(new BigDecimal(valor)), peso == null ? null : new BigDecimal(peso),
			fecha, REGISTRO);
	}

	private static NotaItem nivel(NivelCualitativo nivel) {
		return new NotaItem(Calificacion.cualitativa(nivel), null, null, REGISTRO);
	}

	private static RangoConversion rango(String desde, String hasta, NivelCualitativo nivel) {
		return new RangoConversion(new BigDecimal(desde), new BigDecimal(hasta), nivel);
	}
}
