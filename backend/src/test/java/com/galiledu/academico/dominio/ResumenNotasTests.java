package com.galiledu.academico.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.galiledu.academico.dominio.CalculoNotas.Nota;

/** Resumen de las notas de un grupo para los reportes HU-63 y HU-64. Sin base de datos. */
class ResumenNotasTests {
	private static ResumenNotas.Valor nota(String valor) {
		return new ResumenNotas.Valor(Optional.of(new Nota(BigDecimal.ONE, new BigDecimal(valor), null, null)), false);
	}

	private static ResumenNotas.Valor nivel(String nivel) {
		return new ResumenNotas.Valor(Optional.of(new Nota(BigDecimal.ONE, null, nivel, null)), false);
	}

	private static final ResumenNotas.Valor SIN_NOTA = ResumenNotas.Valor.SIN_NOTA;
	private static final ResumenNotas.Valor PENDIENTE = new ResumenNotas.Valor(Optional.empty(), true);

	@Test
	void promediaLasNotasVigesimalesYCuentaQuienesTienenAoAD() {
		// 18 → AD, 15 → A, 12 → B, 9 → C; promedio 13,5 → redondea a 14 → A.
		var celda = ResumenNotas.celda(List.of(nota("18"), nota("15"), nota("12"), nota("9"), PENDIENTE),
				CalculoNotas.RANGOS_POR_DEFECTO);

		assertThat(celda.promedio()).isEqualTo("13.5");
		assertThat(celda.nivel()).isEqualTo("A");
		assertThat(celda.logro()).isEqualTo("Logro esperado");
		assertThat(celda.conNota()).isEqualTo(4);
		assertThat(celda.logroEsperado()).isEqualTo(2);
		assertThat(celda.porcentajeLogro()).isEqualTo(50);
		// La nota pendiente no entra al promedio: se cuenta aparte.
		assertThat(celda.pendientes()).isEqualTo(1);
	}

	@Test
	void enLaEscalaCualitativaNoPromediaNivelesSinoQueTomaElMasFrecuente() {
		var celda = ResumenNotas.celda(List.of(nivel("B"), nivel("A"), nivel("B"), nivel("AD")),
				CalculoNotas.RANGOS_POR_DEFECTO);

		assertThat(celda.promedio()).isEqualTo("B");
		assertThat(celda.logro()).isEqualTo("En proceso");
		assertThat(celda.porcentajeLogro()).isEqualTo(50);
	}

	@Test
	void sinNotasQuedaPendienteSinPorcentaje() {
		var celda = ResumenNotas.celda(List.of(SIN_NOTA, SIN_NOTA), CalculoNotas.RANGOS_POR_DEFECTO);

		assertThat(celda.promedio()).isEqualTo("—");
		assertThat(celda.nivel()).isNull();
		assertThat(celda.logro()).isEqualTo(CalculoNotas.PENDIENTE);
		assertThat(celda.porcentajeLogro()).isNull();
	}

	@Test
	void laDistribucionCuentaCadaNivelLasPendientesYLasQueAunNoTienenNotas() {
		var distribucion = ResumenNotas.distribucion(
				List.of(nota("19"), nota("17.5"), nota("13.49"), nota("10"), nivel("A"), PENDIENTE, SIN_NOTA),
				CalculoNotas.RANGOS_POR_DEFECTO);

		// 17,5 redondea a 18 → AD; 13,49 → 13 → B.
		assertThat(distribucion).isEqualTo(new ResumenNotas.Distribucion(2, 1, 1, 1, 1, 1));
		assertThat(distribucion.conNota()).isEqualTo(5);
		assertThat(distribucion.porcentajeLogro()).isEqualTo(60);
		assertThat(distribucion.mas(distribucion).pendientes()).isEqualTo(2);
		assertThat(ResumenNotas.Distribucion.VACIA.porcentajeLogro()).isNull();
	}
}
