package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.galiledu.horarios.aplicacion.CalendarioDelAnio;
import com.galiledu.horarios.aplicacion.CalendarioDelAnio.AnioConPeriodos;
import com.galiledu.horarios.aplicacion.CalendarioDelAnio.PeriodoDelAnio;

/** HU-48: las fechas del año y de sus periodos, leídas de la BD real (años 2026 y 2027 de los datos de prueba). */
class CalendarioDelAnioSqlIT extends PruebaReservas {
	@Autowired
	private CalendarioDelAnio calendario;

	@Test
	void traeElAnioActivoConSusCuatroBimestresEnOrden() {
		AnioConPeriodos anio = calendario.deAnio(ANIO_2026).orElseThrow();

		assertThat(anio.anio()).isEqualTo(2026);
		assertThat(anio.estado()).isEqualTo("ACTIVO");
		assertThat(anio.inicio()).isEqualTo(LocalDate.of(2026, 3, 2));
		assertThat(anio.fin()).isEqualTo(LocalDate.of(2026, 12, 18));
		assertThat(anio.periodos()).extracting(PeriodoDelAnio::numero, PeriodoDelAnio::nombre, PeriodoDelAnio::inicio,
				PeriodoDelAnio::fin).containsExactly(
						org.assertj.core.api.Assertions.tuple(1, "I Bimestre", LocalDate.of(2026, 3, 2),
								LocalDate.of(2026, 5, 8)),
						org.assertj.core.api.Assertions.tuple(2, "II Bimestre", LocalDate.of(2026, 5, 18),
								LocalDate.of(2026, 7, 24)),
						org.assertj.core.api.Assertions.tuple(3, "III Bimestre", LocalDate.of(2026, 8, 3),
								LocalDate.of(2026, 10, 9)),
						org.assertj.core.api.Assertions.tuple(4, "IV Bimestre", LocalDate.of(2026, 10, 19),
								LocalDate.of(2026, 12, 18)));
	}

	@Test
	void traeElAnioPlanificadoConSusTresTrimestres() {
		AnioConPeriodos anio = calendario.deAnio(ANIO_2027).orElseThrow();

		assertThat(anio.estado()).isEqualTo("PLANIFICADO");
		assertThat(anio.periodos()).extracting(PeriodoDelAnio::nombre).containsExactly("I Trimestre", "II Trimestre",
				"III Trimestre");
	}

	@Test
	void losDiasLimiteDelAnioYDeUnPeriodoSonInclusivosYElHuecoNoCuenta() {
		AnioConPeriodos anio = calendario.deAnio(ANIO_2026).orElseThrow();
		PeriodoDelAnio segundo = anio.periodos().get(1);

		assertThat(anio.contiene(LocalDate.of(2026, 3, 2))).isTrue();
		assertThat(anio.contiene(LocalDate.of(2026, 12, 18))).isTrue();
		assertThat(anio.contiene(LocalDate.of(2026, 3, 1))).isFalse();
		assertThat(segundo.contiene(LocalDate.of(2026, 5, 18))).isTrue();
		assertThat(segundo.contiene(LocalDate.of(2026, 7, 24))).isTrue();
		assertThat(anio.periodos().stream().anyMatch(p -> p.contiene(LocalDate.of(2026, 7, 27)))).isFalse();
	}

	@Test
	void unAnioInexistenteNoDevuelveNada() {
		assertThat(calendario.deAnio(UUID.randomUUID())).isEmpty();
	}
}
