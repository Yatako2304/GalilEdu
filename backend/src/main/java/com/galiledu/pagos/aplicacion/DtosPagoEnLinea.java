package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

import com.galiledu.pagos.dominio.EstadoCuota;
import com.galiledu.pagos.dominio.EstadoOrdenPago;

/** Contratos JSON del pago en línea (HU-41). Deben coincidir con frontend/src/types/api.ts. */
public final class DtosPagoEnLinea {
	private DtosPagoEnLinea() {
	}

	/** GET /pagos/pago-en-linea/cuotas: las cuotas por pagar de cada hijo del apoderado. */
	public record CuotasPorPagar(List<EstudianteConCuotas> estudiantes) {
	}

	public record EstudianteConCuotas(UUID estudianteId, String nombre, List<CuotaPorPagar> cuotas) {
	}

	/**
	 * Una cuota con saldo. {@code seleccionable}: se puede marcar ahora. {@code requiere}: pensiones anteriores
	 * impagas del mismo hijo que se marcan junto con esta (se paga en orden de antigüedad).
	 */
	public record CuotaPorPagar(
			UUID cuotaId,
			String concepto,
			Short numero,
			String nombre,
			int anio,
			LocalDate fechaVencimiento,
			BigDecimal capital,
			BigDecimal recargo,
			BigDecimal total,
			boolean vencida,
			EstadoCuota estado,
			boolean seleccionable,
			List<UUID> requiere) {
	}

	public record SolicitudOrden(@NotEmpty(message = "Selecciona al menos una cuota.") List<UUID> cuotaIds) {
	}

	/** POST /pagos/pago-en-linea/ordenes: a dónde redirigir al apoderado. */
	public record OrdenCreada(String codigo, BigDecimal total, OffsetDateTime expiraEn, String urlPago) {
	}

	/** GET /pagos/pago-en-linea/ordenes/{codigo}: resultado del pago para la pantalla de regreso. */
	public record OrdenDetalle(
			String codigo,
			EstadoOrdenPago estado,
			BigDecimal total,
			BigDecimal montoConfirmado,
			String motivo,
			OffsetDateTime creadoEn,
			OffsetDateTime expiraEn,
			String urlPago,
			List<LineaOrden> cuotas,
			List<ConstanciaEmitida> constancias) {
	}

	public record LineaOrden(UUID cuotaId, UUID estudianteId, String estudiante, String nombre, boolean matricula,
			BigDecimal capital, BigDecimal recargo) {
	}

	public record ConstanciaEmitida(String numero, String estudiante, BigDecimal monto, String estado) {
	}

	/** Cuerpo del simulador de la pasarela (solo con galiledu.pagos.pasarela=simulada). */
	public record SolicitudSimulacion(boolean aprobar) {
	}
}
