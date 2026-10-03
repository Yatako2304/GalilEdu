package com.galiledu.horarios.dominio;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidadorCrucesHorarioTests {
	private static final UUID BLOQUE = UUID.randomUUID();
	private static final UUID PERIODO_1 = UUID.randomUUID();
	private static final UUID PERIODO_2 = UUID.randomUUID();
	private static final UUID DOCENTE = UUID.randomUUID();
	private static final UUID AULA = UUID.randomUUID();

	private final ValidadorCrucesHorario validador = new ValidadorCrucesHorario();

	@Test
	void bloqueaDocenteAsignadoAOtraSeccionEInformaCursoYSeccion() {
		var propuesta = ocupacion(horario(), "5A", "Historia", DOCENTE, UUID.randomUUID(), null);
		var vigente = ocupacion(horario(), "6B", "Lengua", DOCENTE, UUID.randomUUID(), null);

		assertThat(validador.detectar(propuesta, List.of(vigente)))
			.containsExactly(new CruceHorario(TipoCruceHorario.DOCENTE, vigente.asignacionId(), "6B", "Lengua"));
	}

	@Test
	void bloqueaAulaOcupadaPorOtraSeccion() {
		var propuesta = ocupacion(horario(), "5A", "Historia", UUID.randomUUID(), AULA, null);
		var vigente = ocupacion(horario(), "6B", "Lengua", UUID.randomUUID(), AULA, null);

		assertThat(validador.detectar(propuesta, List.of(vigente)))
			.extracting(CruceHorario::tipo).containsExactly(TipoCruceHorario.AULA);
	}

	@Test
	void informaAmbosCrucesCuandoDocenteYAulaEstanOcupados() {
		var propuesta = ocupacion(horario(), "5A", "Historia", DOCENTE, AULA, null);
		var vigente = ocupacion(horario(), "6B", "Lengua", DOCENTE, AULA, null);

		assertThat(validador.detectar(propuesta, List.of(vigente)))
			.extracting(CruceHorario::tipo)
			.containsExactly(TipoCruceHorario.DOCENTE, TipoCruceHorario.AULA);
	}

	@Test
	void sinEspacioNoHayCruceDeAulaAunqueAmbasLoOmitan() {
		var propuesta = ocupacion(horario(), "5A", "Historia", UUID.randomUUID(), null, null);
		var vigente = ocupacion(horario(), "6B", "Lengua", UUID.randomUUID(), null, null);

		assertThat(validador.detectar(propuesta, List.of(vigente))).isEmpty();
	}

	@Test
	void laMismaSeccionOcupadaSeInformaUnaSolaVezSinRepetirDocenteNiAula() {
		var horario = horario();
		var propuesta = ocupacion(horario, "5A", "Historia", DOCENTE, AULA, null);
		var vigente = ocupacion(horario, "5A", "Lengua", DOCENTE, AULA, null);

		assertThat(validador.detectar(propuesta, List.of(vigente)))
			.extracting(CruceHorario::tipo).containsExactly(TipoCruceHorario.SECCION);
	}

	@Test
	void subgruposDistintosDeUnaSeccionPuedenCoincidirSiNoComparteDocenteNiAula() {
		var horario = horario();
		var propuesta = ocupacion(horario, "5A", "Taller", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		var vigente = ocupacion(horario, "5A", "Taller", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		assertThat(validador.detectar(propuesta, List.of(vigente))).isEmpty();
	}

	@Test
	void subgruposDistintosSigueCruzandoSiElDocenteEsElMismo() {
		var horario = horario();
		var propuesta = ocupacion(horario, "5A", "Taller", DOCENTE, UUID.randomUUID(), UUID.randomUUID());
		var vigente = ocupacion(horario, "5A", "Taller", DOCENTE, UUID.randomUUID(), UUID.randomUUID());

		assertThat(validador.detectar(propuesta, List.of(vigente)))
			.extracting(CruceHorario::tipo).containsExactly(TipoCruceHorario.DOCENTE);
	}

	@Test
	void unaSesionDeTodaLaSeccionChocaConLaDeUnSubgrupo() {
		var horario = horario();
		var propuesta = ocupacion(horario, "5A", "Historia", UUID.randomUUID(), UUID.randomUUID(), null);
		var vigente = ocupacion(horario, "5A", "Taller", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		assertThat(validador.detectar(propuesta, List.of(vigente)))
			.extracting(CruceHorario::tipo).containsExactly(TipoCruceHorario.SECCION);
	}

	@Test
	void noCruzaOtroBloqueDiaOPeriodoSinCoincidencia() {
		var propuesta = ocupacion(horario(), "5A", "Historia", DOCENTE, AULA, null);
		var otroBloque = otra(propuesta, UUID.randomUUID(), propuesta.dia(), Set.of(PERIODO_1));
		var otroDia = otra(propuesta, BLOQUE, DiaSemana.MARTES, Set.of(PERIODO_1));
		var otroPeriodo = otra(propuesta, BLOQUE, propuesta.dia(), Set.of(PERIODO_2));

		assertThat(validador.detectar(propuesta, List.of(otroBloque, otroDia, otroPeriodo))).isEmpty();
	}

	@Test
	void cruzaSiAlMenosUnPeriodoCoincide() {
		var propuesta = ocupacion(horario(), "5A", "Historia", DOCENTE, null, null);
		var vigente = otra(propuesta, BLOQUE, propuesta.dia(), Set.of(PERIODO_1, PERIODO_2));

		assertThat(validador.detectar(propuesta, List.of(vigente))).hasSize(1);
	}

	@Test
	void ignoraLaMismaAsignacionCuandoSeEdita() {
		var horario = horario();
		var propuesta = ocupacion(horario, "5A", "Historia", DOCENTE, AULA, null);
		var anterior = new OcupacionHoraria(propuesta.asignacionId(), horario, "5A", "Lengua", DOCENTE,
			AULA, null, BLOQUE, DiaSemana.LUNES, Set.of(PERIODO_1));

		assertThat(validador.detectar(propuesta, List.of(anterior))).isEmpty();
	}

	private static UUID horario() {
		return UUID.randomUUID();
	}

	private static OcupacionHoraria ocupacion(UUID horario, String seccion, String curso, UUID docente,
		UUID aula, UUID subgrupo) {
		return new OcupacionHoraria(UUID.randomUUID(), horario, seccion, curso, docente, aula, subgrupo,
			BLOQUE, DiaSemana.LUNES, Set.of(PERIODO_1));
	}

	private static OcupacionHoraria otra(OcupacionHoraria base, UUID bloque, DiaSemana dia, Set<UUID> periodos) {
		return new OcupacionHoraria(UUID.randomUUID(), UUID.randomUUID(), "6B", "Lengua", base.docenteId(),
			base.espacioFisicoId(), null, bloque, dia, periodos);
	}
}
