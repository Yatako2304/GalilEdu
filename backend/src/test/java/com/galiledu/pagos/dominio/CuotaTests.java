package com.galiledu.pagos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** HU-39: días de atraso y recargo por mora de una pensión (RF-91, RF-92). Tasa 2 %, tope 10 %, gracia 5 días. */
class CuotaTests {
	private static final BigDecimal TASA = BigDecimal.valueOf(2);
	private static final BigDecimal TOPE = BigDecimal.valueOf(10);
	private static final int GRACIA = 5;
	private static final LocalDate VENCE = LocalDate.of(2026, 9, 30);

	private static Cuota pension(String monto, String descuento) {
		return Cuota.pension(null, (short) 7, "Pensión de septiembre", new BigDecimal(monto), VENCE,
				new BigDecimal(descuento), null, null);
	}

	@Test
	void dentroDeLaGraciaNoHayRecargo() {
		Cuota q = pension("600.00", "0");
		assertThat(q.recargo(VENCE.plusDays(GRACIA), GRACIA, TASA, TOPE)).isEqualByComparingTo("0");
	}

	@Test
	void cobraLaTasaPorCadaTreintaDiasCompletosComoEnFigma() {
		Cuota q = pension("600.00", "0");
		// vencida pero aún sin 30 días completos: sin recargo
		assertThat(q.recargo(VENCE.plusDays(6), GRACIA, TASA, TOPE)).isEqualByComparingTo("0");
		// Figma P11: 32 días → S/ 12 y 63 días → S/ 24
		assertThat(q.diasAtraso(VENCE.plusDays(32))).isEqualTo(32);
		assertThat(q.recargo(VENCE.plusDays(32), GRACIA, TASA, TOPE)).isEqualByComparingTo("12.00");
		assertThat(q.recargo(VENCE.plusDays(63), GRACIA, TASA, TOPE)).isEqualByComparingTo("24.00");
	}

	@Test
	void nuncaPasaDelTope() {
		Cuota q = pension("600.00", "0");
		// 200 días: 6 periodos × 2 % = 12 %, pero el tope es 10 % de 600
		assertThat(q.recargo(VENCE.plusDays(200), GRACIA, TASA, TOPE)).isEqualByComparingTo("60.00");
	}

	@Test
	void seCalculaSobreElCapitalConDescuento() {
		Cuota q = pension("600.00", "90.00");
		assertThat(q.capitalPendiente()).isEqualByComparingTo("510.00");
		assertThat(q.recargo(VENCE.plusDays(40), GRACIA, TASA, TOPE)).isEqualByComparingTo("10.20");
	}

	@Test
	void unaCuotaQueNoEsPensionNoGeneraMora() {
		Cuota q = Cuota.matricula(null, "Matrícula 2026", new BigDecimal("420.00"), VENCE, BigDecimal.ZERO, null,
				null);
		assertThat(q.recargo(VENCE.plusDays(60), GRACIA, TASA, TOPE)).isEqualByComparingTo("0");
	}
}
