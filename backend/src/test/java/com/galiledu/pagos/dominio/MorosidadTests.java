package com.galiledu.pagos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Sección 3.10: recargo por mora de las pensiones (tasa por cada 30 días, sin pasar del tope, sin capitalizar). */
class MorosidadTests {
	/** 5 días de gracia, 2 % cada 30 días y tope de 10 % (tarifario 2026 de los datos de prueba). */
	private final Morosidad morosidad = new Morosidad(5, new BigDecimal("2"), new BigDecimal("10"));
	private static final LocalDate VENCE = LocalDate.of(2026, 9, 30);

	private static Cuota pension(String monto) {
		return Cuota.pension(null, (short) 7, "Pensión de septiembre", new BigDecimal(monto), VENCE, BigDecimal.ZERO,
				null, UUID.randomUUID());
	}

	@Test
	void sinRecargoDentroDeLosDiasDeGracia() {
		assertThat(morosidad.recargo(pension("600"), LocalDate.of(2026, 10, 5))).isEqualByComparingTo("0");
	}

	@Test
	void unPeriodoDeRecargoAlPasarLaGracia() {
		assertThat(morosidad.recargo(pension("600"), LocalDate.of(2026, 10, 6))).isEqualByComparingTo("12.00");
	}

	@Test
	void cadaTreintaDiasSumaOtroPeriodo() {
		// 31 días de atraso = 2 períodos = 4 %
		assertThat(morosidad.recargo(pension("600"), LocalDate.of(2026, 10, 31))).isEqualByComparingTo("24.00");
	}

	@Test
	void noPasaDelTope() {
		// 9 meses de atraso serían 18 %, pero el tope es 10 %
		assertThat(morosidad.recargo(pension("600"), LocalDate.of(2027, 6, 30))).isEqualByComparingTo("60.00");
	}

	@Test
	void laMatriculaNoGeneraMora() {
		Cuota matricula = Cuota.matricula(null, "Matrícula 2027", new BigDecimal("450"), VENCE, BigDecimal.ZERO, null,
				UUID.randomUUID());
		assertThat(morosidad.recargo(matricula, LocalDate.of(2026, 12, 1))).isEqualByComparingTo("0");
	}
}
