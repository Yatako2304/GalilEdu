package com.galiledu.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class AnioEscolarTests {
	private static final LocalDate INICIO = LocalDate.of(2028, 3, 1);
	private static final LocalDate FIN = LocalDate.of(2028, 12, 15);

	private static AnioEscolar anio(LocalDate inicio, LocalDate fin, Regimen regimen) {
		return AnioEscolar.planificar(2028, inicio, fin, regimen);
	}

	@Test
	void unAnioBimestralGeneraCuatroBimestresNumeradosDesdeUno() {
		var periodos = anio(INICIO, FIN, Regimen.BIMESTRAL).getPeriodos();

		assertThat(periodos).extracting(PeriodoAcademico::getNumero).containsExactly(1, 2, 3, 4);
		assertThat(periodos).extracting(PeriodoAcademico::getNombre)
				.containsExactly("I Bimestre", "II Bimestre", "III Bimestre", "IV Bimestre");
	}

	@Test
	void unAnioTrimestralGeneraTresTrimestresNumeradosDesdeUno() {
		var periodos = anio(INICIO, FIN, Regimen.TRIMESTRAL).getPeriodos();

		assertThat(periodos).extracting(PeriodoAcademico::getNumero).containsExactly(1, 2, 3);
		assertThat(periodos).extracting(PeriodoAcademico::getNombre)
				.containsExactly("I Trimestre", "II Trimestre", "III Trimestre");
	}

	@Test
	void elAnioYSusPeriodosNacenPlanificados() {
		var anio = anio(INICIO, FIN, Regimen.BIMESTRAL);

		assertThat(anio.getEstado()).isEqualTo(EstadoAnioEscolar.PLANIFICADO);
		assertThat(anio.getPeriodos()).extracting(PeriodoAcademico::getEstado)
				.containsOnly(EstadoPeriodo.PLANIFICADO);
	}

	@Test
	void losPeriodosCubrenElAnioSinCruzarseNiDejarHuecos() {
		for (Regimen regimen : Regimen.values()) {
			List<PeriodoAcademico> periodos = anio(INICIO, FIN, regimen).getPeriodos();

			assertThat(periodos.getFirst().getFechaInicio()).isEqualTo(INICIO);
			assertThat(periodos.getLast().getFechaFin()).isEqualTo(FIN);
			for (int i = 1; i < periodos.size(); i++) {
				assertThat(periodos.get(i).getFechaInicio())
						.isEqualTo(periodos.get(i - 1).getFechaFin().plusDays(1));
			}
		}
	}

	@Test
	void cadaPeriodoQuedaDentroDelAnioYTerminaDespuesDeEmpezar() {
		for (Regimen regimen : Regimen.values()) {
			for (PeriodoAcademico p : anio(INICIO, FIN, regimen).getPeriodos()) {
				assertThat(p.getFechaInicio()).isAfterOrEqualTo(INICIO);
				assertThat(p.getFechaFin()).isBeforeOrEqualTo(FIN).isAfter(p.getFechaInicio());
			}
		}
	}

	@Test
	void laFechaLimiteDeNotasEsCincoDiasDespuesDelFinDelPeriodo() {
		for (PeriodoAcademico p : anio(INICIO, FIN, Regimen.BIMESTRAL).getPeriodos()) {
			assertThat(p.getFechaLimiteNotas()).isEqualTo(p.getFechaFin().plusDays(5));
		}
	}

	@Test
	void losDiasQueSobranSeRepartenEntreLosPrimerosPeriodos() {
		// 10 días en 4 bimestres: 3 + 3 + 2 + 2.
		var periodos = anio(LocalDate.of(2028, 3, 1), LocalDate.of(2028, 3, 10), Regimen.BIMESTRAL).getPeriodos();

		assertThat(periodos).extracting(p -> p.getFechaFin().getDayOfMonth() - p.getFechaInicio().getDayOfMonth() + 1)
				.containsExactly(3, 3, 2, 2);
	}

	@Test
	void conLosDiasMinimosCadaPeriodoSigueTerminandoDespuesDeEmpezar() {
		for (Regimen regimen : Regimen.values()) {
			LocalDate fin = INICIO.plusDays(AnioEscolar.diasMinimos(regimen) - 1);

			for (PeriodoAcademico p : anio(INICIO, fin, regimen).getPeriodos()) {
				assertThat(p.getFechaFin()).isAfter(p.getFechaInicio());
			}
		}
	}
}
