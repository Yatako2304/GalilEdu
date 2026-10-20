package com.galiledu.academico.dominio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

/** HU-55: los rangos de la tabla de conversión cubren de 0 a 20 sin vacíos ni cruces. */
class TablaConversionTests {
	private static List<RangoNivel> rangos(int cDesde, int cHasta, int bHasta, int aHasta, int adHasta) {
		return List.of(
				new RangoNivel(NivelLogro.AD, aHasta + 1, adHasta),
				new RangoNivel(NivelLogro.A, bHasta + 1, aHasta),
				new RangoNivel(NivelLogro.B, cHasta + 1, bHasta),
				new RangoNivel(NivelLogro.C, cDesde, cHasta));
	}

	@Test
	void aceptaLaTablaDeFigma() {
		assertThatCode(() -> TablaConversion.validarRangos(rangos(0, 10, 13, 17, 20))).doesNotThrowAnyException();
	}

	@Test
	void cDebeEmpezarEnCero() {
		assertThatThrownBy(() -> TablaConversion.validarRangos(rangos(1, 10, 13, 17, 20)))
				.hasMessage("Falta cubrir de 0 a 0 antes de C.");
	}

	@Test
	void adDebeTerminarEnVeinte() {
		assertThatThrownBy(() -> TablaConversion.validarRangos(rangos(0, 10, 13, 17, 19)))
				.hasMessage("El nivel AD debe terminar en 20.");
	}

	@Test
	void detectaUnVacioEntreNiveles() {
		var conVacio = List.of(
				new RangoNivel(NivelLogro.AD, 18, 20),
				new RangoNivel(NivelLogro.A, 15, 17),
				new RangoNivel(NivelLogro.B, 11, 13),
				new RangoNivel(NivelLogro.C, 0, 10));
		assertThatThrownBy(() -> TablaConversion.validarRangos(conVacio))
				.hasMessage("Falta cubrir de 14 a 14 antes de A.");
	}

	@Test
	void detectaUnCruceEntreNiveles() {
		var conCruce = List.of(
				new RangoNivel(NivelLogro.AD, 18, 20),
				new RangoNivel(NivelLogro.A, 13, 17),
				new RangoNivel(NivelLogro.B, 11, 13),
				new RangoNivel(NivelLogro.C, 0, 10));
		assertThatThrownBy(() -> TablaConversion.validarRangos(conCruce))
				.hasMessage("El rango de A se cruza con el nivel anterior.");
	}

	@Test
	void exigeLosCuatroNivelesSinRepetir() {
		var sinC = List.of(
				new RangoNivel(NivelLogro.AD, 18, 20),
				new RangoNivel(NivelLogro.A, 14, 17),
				new RangoNivel(NivelLogro.B, 0, 13),
				new RangoNivel(NivelLogro.B, 0, 13));
		assertThatThrownBy(() -> TablaConversion.validarRangos(sinC))
				.hasMessage("Define un rango para cada nivel: AD, A, B y C.");
	}

	@Test
	void desdeNoPuedeSerMayorQueHasta() {
		var invertido = List.of(
				new RangoNivel(NivelLogro.AD, 18, 20),
				new RangoNivel(NivelLogro.A, 14, 17),
				new RangoNivel(NivelLogro.B, 13, 11),
				new RangoNivel(NivelLogro.C, 0, 10));
		assertThatThrownBy(() -> TablaConversion.validarRangos(invertido))
				.hasMessage("En B, la nota \"desde\" no puede ser mayor que \"hasta\".");
	}
}
