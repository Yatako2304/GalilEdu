package com.galiledu.asistencia.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.galiledu.asistencia.aplicacion.CalendarioEscolar.CalendarioDelNivel;
import com.galiledu.asistencia.aplicacion.CalendarioEscolar.Periodo;
import com.galiledu.asistencia.aplicacion.RangoReporte.Opcion;
import com.galiledu.asistencia.aplicacion.RangoReporte.Rango;
import com.galiledu.asistencia.aplicacion.RangoReporte.Tipo;

/** HU-68: el rango del reporte por mes o por periodo, dentro del año escolar. */
class RangoReporteTests {
	private static final CalendarioDelNivel ANIO = new CalendarioDelNivel(2026, "ACTIVO", LocalDate.of(2026, 3, 2),
			LocalDate.of(2026, 12, 18),
			List.of(new Periodo(1, "I Bimestre", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 5, 8)),
					new Periodo(2, "II Bimestre", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 7, 24)),
					new Periodo(3, "III Bimestre", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 10, 9)),
					new Periodo(4, "IV Bimestre", LocalDate.of(2026, 10, 19), LocalDate.of(2026, 12, 18))),
			EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
	private static final LocalDate HOY = LocalDate.of(2026, 9, 15);

	@Test
	void sinParametrosEsElMesDeHoy() {
		Rango rango = RangoReporte.resolver(null, null, ANIO, HOY);
		assertThat(rango.etiqueta()).isEqualTo("Setiembre 2026");
		assertThat(rango.desde()).isEqualTo(LocalDate.of(2026, 9, 1));
		assertThat(rango.hasta()).isEqualTo(LocalDate.of(2026, 9, 30));
	}

	@Test
	void elPrimerYUltimoMesSeRecortanAlAnioEscolar() {
		assertThat(RangoReporte.resolver(Tipo.MES, "2026-03", ANIO, HOY).desde()).isEqualTo(LocalDate.of(2026, 3, 2));
		assertThat(RangoReporte.resolver(Tipo.MES, "2026-12", ANIO, HOY).hasta())
				.isEqualTo(LocalDate.of(2026, 12, 18));
	}

	@Test
	void unPeriodoUsaSusFechas() {
		Rango rango = RangoReporte.resolver(Tipo.PERIODO, "3", ANIO, HOY);
		assertThat(rango.etiqueta()).isEqualTo("III Bimestre");
		assertThat(rango.desde()).isEqualTo(LocalDate.of(2026, 8, 3));
		assertThat(rango.hasta()).isEqualTo(LocalDate.of(2026, 10, 9));
	}

	@Test
	void unMesOPeriodoAjenoAlAnioSeRechaza() {
		assertThatThrownBy(() -> RangoReporte.resolver(Tipo.MES, "2025-12", ANIO, HOY))
				.hasMessage("El mes no es parte del año escolar 2026.");
		assertThatThrownBy(() -> RangoReporte.resolver(Tipo.MES, "setiembre", ANIO, HOY))
				.hasMessage("El mes del reporte no es válido.");
		assertThatThrownBy(() -> RangoReporte.resolver(Tipo.PERIODO, "9", ANIO, HOY))
				.hasMessage("El periodo no es parte del año escolar.");
	}

	@Test
	void soloSeOfrecenLosMesesYPeriodosQueYaEmpezaron() {
		assertThat(RangoReporte.meses(ANIO, HOY)).extracting(Opcion::valor).containsExactly("2026-09", "2026-08",
				"2026-07", "2026-06", "2026-05", "2026-04", "2026-03");
		assertThat(RangoReporte.periodos(ANIO, HOY)).extracting(Opcion::etiqueta).containsExactly("I Bimestre",
				"II Bimestre", "III Bimestre");
	}
}
