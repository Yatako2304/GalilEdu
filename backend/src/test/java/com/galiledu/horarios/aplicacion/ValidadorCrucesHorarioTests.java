package com.galiledu.horarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.galiledu.configuracion.dominio.DiaSemana;

/** HU-46: la detección de cruces de docente y de ambiente, sin Spring ni BD. */
class ValidadorCrucesHorarioTests {
	private static final UUID ANIO = UUID.randomUUID();
	private static final UUID SECCION = UUID.randomUUID();
	private static final UUID DOCENTE = UUID.randomUUID();
	private static final UUID OTRO_DOCENTE = UUID.randomUUID();
	private static final UUID AMBIENTE = UUID.randomUUID();
	private static final UUID OTRO_AMBIENTE = UUID.randomUUID();

	private final OcupacionHorarios ocupacion = mock(OcupacionHorarios.class);
	private final ValidadorCrucesHorario validador = new ValidadorCrucesHorario(ocupacion);

	private static BloqueSolicitado solicitado(DiaSemana dia, int numero, String inicio, String fin, UUID docente,
			UUID ambiente) {
		return new BloqueSolicitado(dia, numero, LocalTime.parse(inicio), LocalTime.parse(fin), UUID.randomUUID(),
				docente, ambiente);
	}

	private static BloqueOcupado ocupado(DiaSemana dia, String inicio, String fin, UUID docente, UUID ambiente) {
		return new BloqueOcupado(UUID.randomUUID(), dia, LocalTime.parse(inicio), LocalTime.parse(fin), docente,
				ambiente, "5.° B", "Álgebra", "Rosa Quispe", "Aula 12");
	}

	@Test
	void sinOcupadosNoHayCruces() {
		var solicitados = List.of(solicitado(DiaSemana.LUN, 1, "08:00", "08:45", DOCENTE, AMBIENTE));

		assertThat(validador.compararCruces(solicitados, List.of())).isEmpty();
	}

	@Test
	void elCruceDeDocenteDiceConQuienChocaYCuando() {
		var solicitados = List.of(solicitado(DiaSemana.LUN, 1, "08:00", "08:45", DOCENTE, AMBIENTE));
		var ocupados = List.of(ocupado(DiaSemana.LUN, "08:00", "08:45", DOCENTE, OTRO_AMBIENTE));

		List<CruceDetectado> cruces = validador.compararCruces(solicitados, ocupados);

		assertThat(cruces).singleElement().satisfies(c -> {
			assertThat(c.tipo()).isEqualTo(TipoCruce.DOCENTE);
			assertThat(c.dia()).isEqualTo(DiaSemana.LUN);
			assertThat(c.numeroBloque()).isEqualTo(1);
			assertThat(c.seccion()).isEqualTo("5.° B");
			assertThat(c.curso()).isEqualTo("Álgebra");
			assertThat(c.mensaje())
					.isEqualTo(
							"Rosa Quispe ya dicta Álgebra con 5.° B el lunes de 8:00 a 8:45.");
		});
	}

	@Test
	void elCruceDeAmbienteDiceQuienLoOcupaYCuando() {
		var solicitados = List.of(solicitado(DiaSemana.MIE, 3, "10:00", "10:45", DOCENTE, AMBIENTE));
		var ocupados = List.of(ocupado(DiaSemana.MIE, "10:00", "10:45", OTRO_DOCENTE, AMBIENTE));

		List<CruceDetectado> cruces = validador.compararCruces(solicitados, ocupados);

		assertThat(cruces).singleElement().satisfies(c -> {
			assertThat(c.tipo()).isEqualTo(TipoCruce.AMBIENTE);
			assertThat(c.numeroBloque()).isEqualTo(3);
			assertThat(c.mensaje()).isEqualTo(
					"5.° B ya usa Aula 12 para Álgebra el miércoles de 10:00 a 10:45.");
		});
	}

	@Test
	void unMismoParPuedeCruzarseEnDocenteYEnAmbiente() {
		var solicitados = List.of(solicitado(DiaSemana.LUN, 1, "08:00", "08:45", DOCENTE, AMBIENTE));
		var ocupados = List.of(ocupado(DiaSemana.LUN, "08:00", "08:45", DOCENTE, AMBIENTE));

		assertThat(validador.compararCruces(solicitados, ocupados)).extracting(CruceDetectado::tipo)
				.containsExactly(TipoCruce.DOCENTE, TipoCruce.AMBIENTE);
	}

	@Test
	void losBloquesContiguosNoSeCruzan() {
		var solicitados = List.of(solicitado(DiaSemana.LUN, 2, "08:45", "09:30", DOCENTE, AMBIENTE));
		var ocupados = List.of(ocupado(DiaSemana.LUN, "08:00", "08:45", DOCENTE, AMBIENTE),
				ocupado(DiaSemana.LUN, "09:30", "10:15", DOCENTE, AMBIENTE));

		assertThat(validador.compararCruces(solicitados, ocupados)).isEmpty();
	}

	@Test
	void elMismoRecursoEnOtroDiaNoSeCruza() {
		var solicitados = List.of(solicitado(DiaSemana.LUN, 1, "08:00", "08:45", DOCENTE, AMBIENTE));
		var ocupados = List.of(ocupado(DiaSemana.MAR, "08:00", "08:45", DOCENTE, AMBIENTE));

		assertThat(validador.compararCruces(solicitados, ocupados)).isEmpty();
	}

	@Test
	void jornadasDistintasQueSeSuperponenSeCruzan() {
		var solicitados = List.of(solicitado(DiaSemana.JUE, 1, "08:00", "08:45", DOCENTE, AMBIENTE));
		var ocupados = List.of(ocupado(DiaSemana.JUE, "08:30", "09:15", DOCENTE, OTRO_AMBIENTE));

		assertThat(validador.compararCruces(solicitados, ocupados)).singleElement().satisfies(c -> {
			assertThat(c.tipo()).isEqualTo(TipoCruce.DOCENTE);
			assertThat(c.mensaje()).endsWith("el jueves de 8:30 a 9:15.");
		});
	}

	@Test
	void sinDocenteEnAlgunLadoNoHayCruceDeDocente() {
		var sinDocente = List.of(solicitado(DiaSemana.LUN, 1, "08:00", "08:45", null, AMBIENTE));
		var ocupadoSinDocente = ocupado(DiaSemana.LUN, "08:00", "08:45", null, OTRO_AMBIENTE);

		assertThat(validador.compararCruces(sinDocente, List.of(ocupadoSinDocente))).isEmpty();
		assertThat(validador.compararCruces(sinDocente,
				List.of(ocupado(DiaSemana.LUN, "08:00", "08:45", DOCENTE, OTRO_AMBIENTE)))).isEmpty();
		assertThat(validador.compararCruces(
				List.of(solicitado(DiaSemana.LUN, 1, "08:00", "08:45", DOCENTE, AMBIENTE)),
				List.of(ocupadoSinDocente))).isEmpty();
	}

	@Test
	void sinAmbienteEnAlgunLadoNoHayCruceDeAmbiente() {
		var sinAmbiente = List.of(solicitado(DiaSemana.LUN, 1, "08:00", "08:45", DOCENTE, null));
		var ocupadoSinAmbiente = ocupado(DiaSemana.LUN, "08:00", "08:45", OTRO_DOCENTE, null);

		assertThat(validador.compararCruces(sinAmbiente, List.of(ocupadoSinAmbiente))).isEmpty();
		assertThat(validador.compararCruces(sinAmbiente,
				List.of(ocupado(DiaSemana.LUN, "08:00", "08:45", OTRO_DOCENTE, AMBIENTE)))).isEmpty();
		assertThat(validador.compararCruces(
				List.of(solicitado(DiaSemana.LUN, 1, "08:00", "08:45", DOCENTE, AMBIENTE)),
				List.of(ocupadoSinAmbiente))).isEmpty();
	}

	@Test
	void unSolicitadoQueChocaConVariosOcupadosDaUnCrucePorCadaUno() {
		var solicitados = List.of(solicitado(DiaSemana.LUN, 1, "08:00", "09:00", DOCENTE, AMBIENTE));
		var ocupados = List.of(
				new BloqueOcupado(UUID.randomUUID(), DiaSemana.LUN, LocalTime.of(8, 0), LocalTime.of(8, 30), DOCENTE,
						OTRO_AMBIENTE, "3.° A", "Historia", "Rosa Quispe", "Aula 3"),
				new BloqueOcupado(UUID.randomUUID(), DiaSemana.LUN, LocalTime.of(8, 30), LocalTime.of(9, 0),
						OTRO_DOCENTE, AMBIENTE, "4.° C", "Química", "Luis Vera", "Aula 12"));

		List<CruceDetectado> cruces = validador.compararCruces(solicitados, ocupados);

		assertThat(cruces).extracting(CruceDetectado::tipo, CruceDetectado::seccion, CruceDetectado::curso)
				.containsExactly(tuple(TipoCruce.DOCENTE, "3.° A", "Historia"),
						tuple(TipoCruce.AMBIENTE, "4.° C", "Química"));
	}

	@Test
	void losCrucesSalenPorDiaLuegoPorBloqueYConElDocenteAntesQueElAmbiente() {
		var solicitados = List.of(solicitado(DiaSemana.VIE, 1, "08:00", "08:45", DOCENTE, AMBIENTE),
				solicitado(DiaSemana.LUN, 2, "08:45", "09:30", DOCENTE, AMBIENTE),
				solicitado(DiaSemana.LUN, 1, "08:00", "08:45", DOCENTE, AMBIENTE));
		var ocupados = List.of(ocupado(DiaSemana.VIE, "08:00", "08:45", DOCENTE, AMBIENTE),
				ocupado(DiaSemana.LUN, "08:00", "09:30", DOCENTE, AMBIENTE));

		List<CruceDetectado> cruces = validador.compararCruces(solicitados, ocupados);

		assertThat(cruces).extracting(CruceDetectado::dia, CruceDetectado::numeroBloque, CruceDetectado::tipo)
				.containsExactly(tuple(DiaSemana.LUN, 1, TipoCruce.DOCENTE),
						tuple(DiaSemana.LUN, 1, TipoCruce.AMBIENTE),
						tuple(DiaSemana.LUN, 2, TipoCruce.DOCENTE),
						tuple(DiaSemana.LUN, 2, TipoCruce.AMBIENTE),
						tuple(DiaSemana.VIE, 1, TipoCruce.DOCENTE),
						tuple(DiaSemana.VIE, 1, TipoCruce.AMBIENTE));
	}

	@Test
	void buscarCrucesPideLosOcupadosDelAnioExcluyendoLaSeccion() {
		var solicitados = List.of(solicitado(DiaSemana.LUN, 1, "08:00", "08:45", DOCENTE, AMBIENTE));
		when(ocupacion.bloquesOcupados(ANIO, SECCION))
				.thenReturn(List.of(ocupado(DiaSemana.LUN, "08:00", "08:45", DOCENTE, OTRO_AMBIENTE)));

		List<CruceDetectado> cruces = validador.buscarCruces(ANIO, SECCION, solicitados);

		verify(ocupacion).bloquesOcupados(ANIO, SECCION);
		assertThat(cruces).extracting(CruceDetectado::tipo).containsExactly(TipoCruce.DOCENTE);
	}

	@Test
	void buscarCrucesSinSolicitadosNoConsultaNada() {
		assertThat(validador.buscarCruces(ANIO, SECCION, List.of())).isEmpty();

		verifyNoInteractions(ocupacion);
	}
}
