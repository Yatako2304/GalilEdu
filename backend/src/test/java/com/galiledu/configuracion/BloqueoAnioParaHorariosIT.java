package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.galiledu.PruebaIntegracion;
import com.galiledu.configuracion.aplicacion.ServicioEstructurasHorarias;

/**
 * HU-45: {@code bloquearAnioParaHorarios} exige una transacción abierta, porque el bloqueo consultivo se libera al
 * terminar la transacción que lo pidió.
 */
class BloqueoAnioParaHorariosIT extends PruebaIntegracion {
	private static final UUID ANIO_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");

	@Autowired
	private ServicioEstructurasHorarias servicio;
	@Autowired
	private PlatformTransactionManager transacciones;

	@Test
	void dentroDeUnaTransaccionBloqueaElAnio() {
		// La prueba ya corre en una transacción (PruebaIntegracion).
		assertThatCode(() -> servicio.bloquearAnioParaHorarios(ANIO_2026)).doesNotThrowAnyException();
	}

	@Test
	void fueraDeUnaTransaccionFalla() {
		TransactionTemplate sinTransaccion = new TransactionTemplate(transacciones);
		sinTransaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);

		assertThatThrownBy(
				() -> sinTransaccion.executeWithoutResult(estado -> servicio.bloquearAnioParaHorarios(ANIO_2026)))
				.isInstanceOf(IllegalTransactionStateException.class);
	}
}
