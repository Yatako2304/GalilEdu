package com.galiledu.horarios.aplicacion;

import java.time.DayOfWeek;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.galiledu.horarios.aplicacion.puertos.ConsultaAsignacionesHorario;
import com.galiledu.horarios.dominio.AsignacionHorario;
import com.galiledu.horarios.dominio.CruceHorario;
import com.galiledu.horarios.dominio.TipoCruceHorario;
import com.galiledu.horarios.dominio.ValidadorCrucesHorario;

import static org.assertj.core.api.Assertions.assertThat;

class VerificarDisponibilidadHorarioTests {
	@Test
	void detectaCrucesAntesDeRegistrar() {
		var vigente = bloque("actual", "6B", "Lengua", "D1", "A1");
		var caso = new VerificarDisponibilidadHorario((anio, dia, bloque) -> List.of(vigente),
			new ValidadorCrucesHorario());

		var resultado = caso.ejecutar(bloque("nuevo", "5A", "Historia", "D1", "A1"));

		assertThat(resultado.disponible()).isFalse();
		assertThat(resultado.seccionOcupada()).isFalse();
		assertThat(resultado.cruces()).containsExactly(
			new CruceHorario(TipoCruceHorario.DOCENTE, "6B", "Lengua"),
			new CruceHorario(TipoCruceHorario.AULA, "6B", "Lengua"));
	}

	@Test
	void detectaSeccionYaOcupadaSinConfundirlaConCruceExterno() {
		var vigente = bloque("actual", "5A", "Lengua", "D2", "A2");
		var caso = new VerificarDisponibilidadHorario((anio, dia, bloque) -> List.of(vigente),
			new ValidadorCrucesHorario());

		var resultado = caso.ejecutar(bloque("nuevo", "5A", "Historia", "D1", "A1"));

		assertThat(resultado.disponible()).isFalse();
		assertThat(resultado.seccionOcupada()).isTrue();
		assertThat(resultado.cruces()).isEmpty();
	}

	@Test
	void permiteEditarLaMismaAsignacion() {
		var vigente = bloque("actual", "5A", "Lengua", "D1", "A1");
		var caso = new VerificarDisponibilidadHorario((anio, dia, bloque) -> List.of(vigente),
			new ValidadorCrucesHorario());

		assertThat(caso.ejecutar(bloque("actual", "5A", "Historia", "D1", "A1"))
			.disponible()).isTrue();
	}

	@Test
	void consultaSoloElBloqueDelAnioYDiaSolicitados() {
		var consulta = new ConsultaDePrueba();
		var caso = new VerificarDisponibilidadHorario(consulta, new ValidadorCrucesHorario());

		assertThat(caso.ejecutar(bloque("nuevo", "5A", "Historia", "D1", "A1"))
			.disponible()).isTrue();
		assertThat(consulta.anio).isEqualTo(2027);
		assertThat(consulta.dia).isEqualTo(DayOfWeek.MONDAY);
		assertThat(consulta.bloque).isEqualTo(1);
	}

	private static AsignacionHorario bloque(String id, String seccion, String curso,
		String docente, String aula) {
		return new AsignacionHorario(id, 2027, DayOfWeek.MONDAY, 1, seccion, curso, docente, aula);
	}

	private static final class ConsultaDePrueba implements ConsultaAsignacionesHorario {
		private int anio;
		private DayOfWeek dia;
		private int bloque;

		@Override
		public List<AsignacionHorario> buscarEnBloque(int anioEscolar, DayOfWeek dia, int bloque) {
			this.anio = anioEscolar;
			this.dia = dia;
			this.bloque = bloque;
			return List.of();
		}
	}
}
