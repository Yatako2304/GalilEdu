package com.galiledu.asistencia.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.galiledu.asistencia.aplicacion.CalendarioEscolar.CalendarioDelNivel;
import com.galiledu.asistencia.aplicacion.CalendarioEscolar.Periodo;

/** Día escolar: año activo, dentro del año y de un periodo, y día lectivo del nivel (sin feriados). */
class ReglaDiaEscolarTests {
	private static final List<Periodo> PERIODOS = List.of(
			new Periodo(1, "I Bimestre", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 5, 8)),
			new Periodo(2, "II Bimestre", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 7, 24)));
	private static final EnumSet<DayOfWeek> LUN_A_VIE = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

	private static CalendarioDelNivel calendario(String estado) {
		return new CalendarioDelNivel(2026, estado, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 12, 18), PERIODOS,
				LUN_A_VIE);
	}

	@Test
	void unMartesDeBimestreEsDiaEscolar() {
		assertThat(ReglaDiaEscolar.motivoNoEscolar(calendario("ACTIVO"), LocalDate.of(2026, 5, 5))).isEmpty();
	}

	@Test
	void elPrimerYUltimoDiaDelPeriodoCuentan() {
		assertThat(ReglaDiaEscolar.motivoNoEscolar(calendario("ACTIVO"), LocalDate.of(2026, 3, 2))).isEmpty();
		assertThat(ReglaDiaEscolar.motivoNoEscolar(calendario("ACTIVO"), LocalDate.of(2026, 5, 8))).isEmpty();
	}

	@Test
	void unAnioQueNoEstaActivoNoTieneDiasEscolares() {
		assertThat(ReglaDiaEscolar.motivoNoEscolar(calendario("CERRADO"), LocalDate.of(2026, 5, 5)))
				.contains("El año escolar 2026 no está activo.");
	}

	@Test
	void fueraDelAnioEntreBimestresYSabadoNoSonDiasEscolares() {
		assertThat(ReglaDiaEscolar.motivoNoEscolar(calendario("ACTIVO"), LocalDate.of(2026, 2, 27)))
				.contains("La fecha está fuera del año escolar 2026.");
		assertThat(ReglaDiaEscolar.motivoNoEscolar(calendario("ACTIVO"), LocalDate.of(2026, 5, 12)))
				.contains("La fecha cae entre dos periodos académicos (vacaciones).");
		assertThat(ReglaDiaEscolar.motivoNoEscolar(calendario("ACTIVO"), LocalDate.of(2026, 5, 2)))
				.contains("El sábado no es día de clases.");
	}
}
