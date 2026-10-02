package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import com.galiledu.pagos.aplicacion.puertos.ConsultaOrdenPago;

/** Evalúa RF-105; no autentica la pasarela ni registra pagos. */
public final class VerificarMontoConfirmado {
	private final ConsultaOrdenPago ordenes;

	public VerificarMontoConfirmado(ConsultaOrdenPago ordenes) {
		this.ordenes = Objects.requireNonNull(ordenes);
	}

	public Optional<Resultado> ejecutar(String identificadorOrden, BigDecimal montoConfirmado) {
		if (identificadorOrden == null || identificadorOrden.isBlank()) {
			throw new IllegalArgumentException("El identificador de orden es obligatorio");
		}
		Objects.requireNonNull(montoConfirmado, "El monto confirmado es obligatorio");
		return ordenes.buscarPorIdentificador(identificadorOrden)
			.map(orden -> new Resultado(orden.identificador(), orden.monto(),
				montoConfirmado, orden.coincideCon(montoConfirmado)));
	}

	public record Resultado(String identificadorOrden, BigDecimal montoOrden,
		BigDecimal montoConfirmado, boolean coincidente) {}
}
