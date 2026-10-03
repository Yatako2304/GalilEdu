package com.galiledu.pagos.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguracionTarifarioTests {
	@Test
	void cantidadYFechasNoPuedenContradecirse() {
		assertThatThrownBy(() -> datos(2, List.of(LocalDate.of(2027, 3, 10))))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> datos(2,
			List.of(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 10))))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void exigeTopeYNoPermiteMontosNegativos() {
		assertThatThrownBy(() -> new ConfiguracionTarifario(UUID.randomUUID(),
			new BigDecimal("100.00"), new BigDecimal("200.00"), 1,
			LocalDate.of(2027, 2, 10), List.of(LocalDate.of(2027, 3, 10)),
			5, new BigDecimal("1.00"), null))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new ConfiguracionTarifario(UUID.randomUUID(),
			new BigDecimal("-1.00"), new BigDecimal("200.00"), 1,
			LocalDate.of(2027, 2, 10), List.of(LocalDate.of(2027, 3, 10)),
			5, new BigDecimal("1.00"), new BigDecimal("10.00")))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void exigeVencimientoDeMatriculaIndependienteDeLasPensiones() {
		assertThatThrownBy(() -> new ConfiguracionTarifario(UUID.randomUUID(),
			new BigDecimal("100.00"), new BigDecimal("200.00"), 1,
			null, List.of(LocalDate.of(2027, 3, 10)), 5,
			new BigDecimal("1.00"), new BigDecimal("10.00")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("matrícula");
	}

	private static ConfiguracionTarifario datos(int cuotas, List<LocalDate> fechas) {
		return new ConfiguracionTarifario(UUID.randomUUID(), new BigDecimal("100.00"),
			new BigDecimal("200.00"), cuotas, LocalDate.of(2027, 2, 10), fechas, 5,
			new BigDecimal("1.00"), new BigDecimal("10.00"));
	}
}
