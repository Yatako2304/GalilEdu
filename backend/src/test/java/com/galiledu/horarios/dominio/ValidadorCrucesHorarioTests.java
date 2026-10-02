package com.galiledu.horarios.dominio;

import java.time.DayOfWeek;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorCrucesHorarioTests {
	private final ValidadorCrucesHorario validador = new ValidadorCrucesHorario();

	@Test
	void bloqueaDocenteAsignadoAOtraSeccionEInformaCursoYSeccion() {
		var propuesta = bloque("a", 2027, DayOfWeek.MONDAY, 1, "5A", "Historia", "D1", "A1");
		var vigente = bloque("b", 2027, DayOfWeek.MONDAY, 1, "6B", "Lengua", "D1", "A2");

		assertThat(validador.detectar(propuesta, List.of(vigente)))
			.containsExactly(new CruceHorario(TipoCruceHorario.DOCENTE, "6B", "Lengua"));
	}

	@Test
	void bloqueaAulaOcupadaPorOtraSeccion() {
		var propuesta = bloque("a", 2027, DayOfWeek.MONDAY, 1, "5A", "Historia", "D1", "A1");
		var vigente = bloque("b", 2027, DayOfWeek.MONDAY, 1, "6B", "Lengua", "D2", "A1");

		assertThat(validador.detectar(propuesta, List.of(vigente)))
			.containsExactly(new CruceHorario(TipoCruceHorario.AULA, "6B", "Lengua"));
	}

	@Test
	void informaAmbosCrucesCuandoDocenteYAulaEstanOcupados() {
		var propuesta = bloque("a", 2027, DayOfWeek.MONDAY, 1, "5A", "Historia", "D1", "A1");
		var vigente = bloque("b", 2027, DayOfWeek.MONDAY, 1, "6B", "Lengua", "D1", "A1");

		assertThat(validador.detectar(propuesta, List.of(vigente)))
			.extracting(CruceHorario::tipo)
			.containsExactly(TipoCruceHorario.DOCENTE, TipoCruceHorario.AULA);
	}

	@Test
	void noCruzaOtroBloqueDiaOAnio() {
		var propuesta = bloque("a", 2027, DayOfWeek.MONDAY, 1, "5A", "Historia", "D1", "A1");
		var otroBloque = bloque("b", 2027, DayOfWeek.MONDAY, 2, "6B", "Lengua", "D1", "A1");
		var otroDia = bloque("c", 2027, DayOfWeek.TUESDAY, 1, "6B", "Lengua", "D1", "A1");
		var otroAnio = bloque("d", 2028, DayOfWeek.MONDAY, 1, "6B", "Lengua", "D1", "A1");

		assertThat(validador.detectar(propuesta, List.of(otroBloque, otroDia, otroAnio)))
			.isEmpty();
	}

	@Test
	void ignoraLaMismaAsignacionCuandoSeEdita() {
		var propuesta = bloque("a", 2027, DayOfWeek.MONDAY, 1, "5A", "Historia", "D1", "A1");
		var anterior = bloque("a", 2027, DayOfWeek.MONDAY, 1, "6B", "Lengua", "D1", "A1");

		assertThat(validador.detectar(propuesta, List.of(anterior))).isEmpty();
	}

	@Test
	void rechazaBloqueSinDatosObligatorios() {
		assertThatThrownBy(() -> bloque("a", 2027, DayOfWeek.MONDAY, 0,
			"5A", "Historia", "D1", "A1"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private static AsignacionHorario bloque(String id, int anio, DayOfWeek dia, int numero,
		String seccion, String curso, String docente, String aula) {
		return new AsignacionHorario(id, anio, dia, numero, seccion, curso, docente, aula);
	}
}
