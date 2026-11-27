package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import com.galiledu.pagos.dominio.EstadoCompromiso;
import com.galiledu.pagos.dominio.EstadoCuota;

/** Contratos JSON del módulo de pagos. Deben coincidir con frontend/src/types/api.ts. */
public final class DtosPagos {
	private DtosPagos() {
	}

	/** Opción del selector "Compromiso 2026" (Figma P8). */
	public record CompromisoAnio(UUID id, int anio, EstadoCompromiso estado) {
	}

	/** HU-37 · Figma "P8 · Compromiso — Cronograma de cuotas". */
	public record CompromisoDetalle(
			UUID id,
			int anio,
			EstadoCompromiso estado,
			EstudianteCompromiso estudiante,
			ResumenCompromiso resumen,
			List<CuotaDetalle> cuotas) {
	}

	public record EstudianteCompromiso(UUID id, String nombre, String numeroDocumento, String apoderado,
			String seccion) {
	}

	/** Las 5 tarjetas del encabezado: total anual, capital pagado, descuento, pensión mensual y saldo. */
	public record ResumenCompromiso(
			BigDecimal totalAnual,
			int cuotasActivas,
			BigDecimal capitalPagado,
			int pensionesPagadas,
			int pensionesTotal,
			BigDecimal descuentoPorcentaje,
			String descuentoNombre,
			BigDecimal pensionMensual,
			BigDecimal saldoPendiente,
			int cuotasVencidas) {
	}

	public record CuotaDetalle(
			UUID id,
			String concepto,
			Short numero,
			String nombre,
			LocalDate fechaVencimiento,
			BigDecimal montoBase,
			BigDecimal montoDescuento,
			BigDecimal porcentajeDescuento,
			BigDecimal montoFinal,
			BigDecimal montoPagado,
			EstadoCuota estado,
			boolean vencida,
			LocalDate fechaPago,
			int diasAtraso,
			BigDecimal recargo) {
	}

	/** Respuesta de POST /pagos/matriculas/{id}/cronograma (RF-81, RF-82). */
	public record CronogramaGenerado(UUID compromisoId, int anio, int cuotasGeneradas) {
	}

	/** Cuerpo de POST /pagos/compromisos/{id}/retiro (RF-85). */
	public record SolicitudRetiro(@NotNull LocalDate fechaRetiro) {
	}

	public record RetiroRegistrado(int cuotasAnuladas) {
	}

	/** HU-37 · Figma "P2 · Estado de cuenta — Todos los alumnos": tarjetas y tabla de compromisos del año. */
	/**
	 * P2 · Estado de cuenta. {@code anio}: el año pedido, o null si cada alumno trae su compromiso más reciente. Los
	 * totales suman solo los compromisos VIGENTES de la lista.
	 */
	public record EstadoCuentaGeneral(
			Integer anio,
			BigDecimal comprometido,
			BigDecimal cobrado,
			int porcentajeCobrado,
			BigDecimal deudaVencida,
			int cuotasVencidas,
			int alumnosConDeuda,
			List<AlumnoEstadoCuenta> alumnos) {
	}

	/**
	 * Una fila de P2: cada estudiante no eliminado, también los retirados. {@code antiguedad}: años con matrícula en el
	 * colegio hasta el año del
	 * compromiso. Sin compromiso, {@code compromisoId} y {@code estadoCompromiso} son null y los montos, cero.
	 */
	public record AlumnoEstadoCuenta(
			UUID estudianteId,
			UUID compromisoId,
			String nombre,
			String numeroDocumento,
			String apoderado,
			String seccion,
			int antiguedad,
			EstadoCompromiso estadoCompromiso,
			BigDecimal totalAnual,
			BigDecimal capitalPagado,
			BigDecimal deudaVencida,
			int cuotasVencidas,
			/** Año del compromiso que se muestra (columna «Compromiso»); null si no tiene. */
			Integer anioCompromiso,
			/** El estudiante está retirado (inactivo); sale igual por si dejó deuda. */
			boolean retirado) {
	}

	/** IEstadoCuenta: pensión vencida e impaga de un estudiante (la usa Matrícula en HU-30). */
	public record CuotaVencida(UUID estudianteId, String estudiante, String concepto, LocalDate fechaVencimiento,
			BigDecimal saldo) {
	}

	/**
	 * HU-36 · Cuota de matrícula (RF-78, RF-80). Figma B6 "Pago de matrícula" (concepto, monto, estado, responsable) y
	 * B7 (estado "Pagada · fecha"). {@code estudianteId} sirve para el enlace "Ver compromiso de pago" (P8).
	 */
	public record CuotaMatriculaDetalle(
			UUID cuotaId,
			UUID compromisoId,
			UUID estudianteId,
			int anio,
			String concepto,
			BigDecimal montoBase,
			BigDecimal montoDescuento,
			BigDecimal monto,
			BigDecimal montoPagado,
			LocalDate fechaVencimiento,
			EstadoCuota estado,
			LocalDate fechaPago,
			String responsable) {
	}

	/** HU-39 · P11 Morosidad: una pensión vencida con sus días de atraso, capital y recargo (RF-93). */
	public record CuotaMorosa(
			UUID cuotaId,
			String concepto,
			Short numero,
			LocalDate fechaVencimiento,
			int diasAtraso,
			BigDecimal capital,
			BigDecimal recargo,
			BigDecimal total) {
	}

	/**
	 * HU-39 · P11 Morosidad del compromiso (RF-91 a RF-93). {@code fecha} es el "Total adeudado al …" de Figma. El
	 * recargo va siempre separado del capital; la política (gracia, tasa y tope) es la del tarifario del año.
	 */
	public record MorosidadCompromiso(
			UUID compromisoId,
			LocalDate fecha,
			int anio,
			UUID estudianteId,
			String estudiante,
			int diasGracia,
			BigDecimal recargoPorcentaje,
			BigDecimal topeRecargoPorcentaje,
			BigDecimal capital,
			BigDecimal recargo,
			BigDecimal total,
			List<CuotaMorosa> cuotas) {
	}

	/**
	 * HU-40 · RF-95: un hijo del apoderado. {@code codigo} es el número de documento (la ficha del estudiante no tiene
	 * otro código); {@code grado} y {@code seccion} son los de su matrícula más reciente; {@code saldo} suma las cuotas
	 * pendientes de sus compromisos vigentes y {@code cuotasVencidas} cuenta las pensiones vencidas (RF-91).
	 */
	public record HijoConDeuda(
			UUID estudianteId,
			String codigo,
			String nombres,
			String apellidos,
			String grado,
			String seccion,
			BigDecimal saldo,
			int cuotasVencidas) {
	}

	/** RF-97 y pestaña "Pagos": un pago con su constancia y las cuotas que cubrió. */
	public record PagoRealizado(
			UUID id,
			String numeroConstancia,
			OffsetDateTime fechaPago,
			BigDecimal monto,
			BigDecimal capital,
			BigDecimal recargo,
			String estado,
			List<String> cuotas,
			/** "En línea" o, si se pagó en caja, el medio con su número de operación ("Transferencia · 00123456"). */
			String medio) {
	}

	/**
	 * HU-42 · RF-107: constancia de un pago confirmado. {@code anio}, {@code grado} y {@code seccion} pueden ser null
	 * si el compromiso no está enlazado a una matrícula.
	 */
	public record ConstanciaPago(
			UUID pagoId,
			String numeroConstancia,
			OffsetDateTime fechaPago,
			String referencia,
			BigDecimal montoTotal,
			BigDecimal capital,
			BigDecimal recargo,
			String estado,
			String observacion,
			String estudiante,
			String documento,
			Integer anio,
			String grado,
			String seccion,
			List<CuotaConstancia> cuotas) {
	}

	public record CuotaConstancia(String concepto, String cuota, LocalDate vencimiento, BigDecimal capital,
			BigDecimal recargo) {
	}
}
