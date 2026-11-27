package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.galiledu.pagos.dominio.MedioPagoCaja;

/**
 * Contratos JSON del pago en caja que registra el Personal administrativo. Deben coincidir con
 * frontend/src/types/api.ts.
 */
public final class DtosPagoEnCaja {
	private DtosPagoEnCaja() {
	}

	/** POST /pagos/compromisos/{id}/pagos */
	public record SolicitudPagoCaja(
			@NotEmpty(message = "Selecciona al menos una cuota.") List<UUID> cuotaIds,
			@NotNull(message = "Ingresa la fecha de pago.") LocalDate fechaPago,
			@NotNull(message = "Elige el medio de pago.") MedioPagoCaja medioPago,
			@Size(max = 30, message = "El número de operación tiene como máximo 30 caracteres.") @Pattern(regexp = "[A-Za-z0-9-]*", message = "El número de operación solo lleva letras, números y guiones.") String numeroOperacion,
			@Size(max = 300, message = "La observación tiene como máximo 300 caracteres.") String observacion) {
	}

	/** El pago quedó registrado con su constancia. */
	public record PagoCajaRegistrado(UUID pagoId, String numeroConstancia, BigDecimal capital, BigDecimal recargo,
			BigDecimal total) {
	}
}
