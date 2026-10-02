package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.galiledu.pagos.dominio.OrdenPago;

import static org.assertj.core.api.Assertions.assertThat;

class VerificarMontoConfirmadoTests {
	private final VerificarMontoConfirmado verificacion = new VerificarMontoConfirmado(
		id -> id.equals("orden-1")
			? Optional.of(new OrdenPago("orden-1", new BigDecimal("850.00")))
			: Optional.empty());

	@Test
	void comparaElValorNumericoSinConfundirEscalasDecimales() {
		var resultado = verificacion.ejecutar("orden-1", new BigDecimal("850.0"))
			.orElseThrow();

		assertThat(resultado.coincidente()).isTrue();
		assertThat(resultado.montoOrden()).isEqualByComparingTo("850.00");
	}

	@Test
	void senalaLaDiscrepanciaSinMarcarElPagoComoRechazado() {
		var resultado = verificacion.ejecutar("orden-1", new BigDecimal("849.00"))
			.orElseThrow();

		assertThat(resultado.coincidente()).isFalse();
		assertThat(resultado.montoConfirmado()).isEqualByComparingTo("849.00");
	}

	@Test
	void noInventaUnaOrdenSiNoExiste() {
		assertThat(verificacion.ejecutar("desconocida", BigDecimal.TEN)).isEmpty();
	}
}
