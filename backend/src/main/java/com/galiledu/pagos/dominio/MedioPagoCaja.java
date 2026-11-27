package com.galiledu.pagos.dominio;

/**
 * Medio con el que se pagó en caja. No tiene columna propia: se guarda como texto en {@code pagos.pago.referencia}
 * junto con el número de operación ("Transferencia · 00123456"). Ver docs/modulos/pagos.md.
 */
public enum MedioPagoCaja {
	EFECTIVO("Efectivo", false), TRANSFERENCIA("Transferencia", true), DEPOSITO("Depósito",
			true), YAPE_PLIN("Yape/Plin", true), TARJETA_POS("Tarjeta (POS)", true);

	private final String etiqueta;
	private final boolean requiereOperacion;

	MedioPagoCaja(String etiqueta, boolean requiereOperacion) {
		this.etiqueta = etiqueta;
		this.requiereOperacion = requiereOperacion;
	}

	public String etiqueta() {
		return etiqueta;
	}

	/** Todos piden número de operación menos el efectivo. */
	public boolean requiereOperacion() {
		return requiereOperacion;
	}

	/** Texto que se guarda en la referencia del pago: "Efectivo" o "Transferencia · 00123456". */
	public String referencia(String numeroOperacion) {
		return numeroOperacion == null || numeroOperacion.isBlank() ? etiqueta : etiqueta + " · " + numeroOperacion;
	}
}
