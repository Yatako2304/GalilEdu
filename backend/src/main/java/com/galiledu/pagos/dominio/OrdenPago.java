package com.galiledu.pagos.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/** Orden identificable con el monto necesario para contrastar RF-105. */
public final class OrdenPago {
	private final String identificador;
	private final BigDecimal monto;

	public OrdenPago(String identificador, BigDecimal monto) {
		if (identificador == null || identificador.isBlank()) {
			throw new IllegalArgumentException("El identificador de orden es obligatorio");
		}
		this.identificador = identificador;
		this.monto = Objects.requireNonNull(monto, "El monto de la orden es obligatorio");
	}

	public String identificador() {
		return identificador;
	}

	public BigDecimal monto() {
		return monto;
	}

	public boolean coincideCon(BigDecimal montoConfirmado) {
		Objects.requireNonNull(montoConfirmado, "El monto confirmado es obligatorio");
		return monto.compareTo(montoConfirmado) == 0;
	}
}
