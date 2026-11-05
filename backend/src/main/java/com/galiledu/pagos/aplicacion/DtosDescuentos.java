package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.galiledu.pagos.dominio.AplicacionDescuento;
import com.galiledu.pagos.dominio.EstadoDescuento;
import com.galiledu.pagos.dominio.EstadoDescuentoAplicado;
import com.galiledu.pagos.dominio.TipoDescuento;

/** Contratos JSON del catálogo de descuentos (HU-35) y de los descuentos aplicados (HU-38). */
public final class DtosDescuentos {
	private DtosDescuentos() {
	}

	// ---------- HU-35 · Figma "P21 · Catálogo de descuentos" ----------

	/** Una fila de P21. {@code porcentaje} solo en PORCENTAJE_UNICO; POR_HERMANOS trae {@code tramos}. */
	public record DescuentoVista(
			UUID id,
			String nombre,
			String descripcion,
			TipoDescuento tipo,
			AplicacionDescuento aplicacion,
			BigDecimal porcentaje,
			List<TramoDescuento> tramos,
			List<String> conceptos,
			String condicion,
			EstadoDescuento estado) {
	}

	/** {@code hermanosHasta} null = "o más" (3+). */
	public record TramoDescuento(
			@NotNull(message = "Indica desde cuántos hermanos rige el tramo.") Integer hermanosDesde,
			Integer hermanosHasta,
			@NotNull(message = "Indica el porcentaje del tramo.") BigDecimal porcentaje) {
	}

	/** Cuerpo de POST y PUT /pagos/descuentos (RF-74, RF-75). */
	public record SolicitudDescuento(
			@NotBlank(message = "Ingresa la denominación del descuento.") @Size(max = 80, message = "La denominación admite hasta 80 caracteres.") String nombre,
			@Size(max = 300, message = "La descripción admite hasta 300 caracteres.") String descripcion,
			@NotNull(message = "Indica el tipo de descuento.") TipoDescuento tipo,
			@NotNull(message = "Indica si el descuento es manual o automático.") AplicacionDescuento aplicacion,
			BigDecimal porcentaje,
			@Valid List<TramoDescuento> tramos,
			List<String> conceptos,
			@Size(max = 300, message = "La condición admite hasta 300 caracteres.") String condicion) {
	}

	/** RF-77: límite de descuento de la institución. */
	public record ConfiguracionDescuentos(BigDecimal porcentajeMaximo) {
	}

	public record SolicitudConfiguracionDescuentos(
			@NotNull(message = "Ingresa el porcentaje máximo.") @DecimalMin(value = "1", message = "El porcentaje máximo debe estar entre 1 y 100.") @DecimalMax(value = "100", message = "El porcentaje máximo debe estar entre 1 y 100.") BigDecimal porcentajeMaximo) {
	}

	// ---------- HU-38 · Figma "P10 · Descuento aplicado" ----------

	/**
	 * RF-86: descuento ACTIVO del catálogo con el porcentaje que le corresponde al estudiante. Un POR_HERMANOS sin
	 * tramo
	 * para sus hermanos matriculados llega con {@code aplicable = false} y el motivo.
	 */
	public record DescuentoAplicable(UUID id, String nombre, TipoDescuento tipo, AplicacionDescuento aplicacion,
			BigDecimal porcentaje, List<String> conceptos, String condicion, boolean aplicable, String motivo) {
	}

	/** Respuesta de GET /pagos/compromisos/{id}/descuentos-aplicables. */
	public record DescuentosAplicables(int hermanosMatriculados, DescuentoActual actual,
			List<DescuentoAplicable> descuentos) {
	}

	/** Descuento vigente del estudiante, para advertir el reemplazo (RF-87). */
	public record DescuentoActual(UUID id, String nombre, BigDecimal porcentaje) {
	}

	/**
	 * Cuerpo de POST /pagos/compromisos/{id}/descuentos. Sin {@code vigenciaDesde} rige desde hoy; sin
	 * {@code vigenciaHasta}, hasta fin de año. {@code reemplazar} confirma el reemplazo del descuento vigente (RF-87).
	 */
	public record SolicitudAplicarDescuento(
			@NotNull(message = "Elige un descuento del catálogo.") UUID descuentoId,
			@NotBlank(message = "Ingresa el motivo del descuento.") @Size(max = 500, message = "El motivo admite hasta 500 caracteres.") String motivo,
			LocalDate vigenciaDesde,
			LocalDate vigenciaHasta,
			boolean reemplazar) {
	}

	/** Cuerpo de POST /pagos/descuentos-aplicados/{id}/revocacion (RF-90). */
	public record SolicitudRevocarDescuento(
			@NotBlank(message = "Ingresa el motivo de la revocación.") @Size(max = 500, message = "El motivo admite hasta 500 caracteres.") String motivo) {
	}

	/** Un descuento aplicado al estudiante (P10): nombre, porcentaje, vigencia, fecha, motivo y quién lo aplicó. */
	public record DescuentoAplicadoVista(
			UUID id,
			UUID descuentoId,
			String nombre,
			BigDecimal porcentaje,
			LocalDate vigenciaDesde,
			LocalDate vigenciaHasta,
			String motivo,
			EstadoDescuentoAplicado estado,
			String aplicadoPor,
			Instant aplicadoEn,
			String motivoRevocacion,
			Instant revocadoEn) {
	}

	/** Pestaña "Descuento" de P10: el vigente (null si no hay), cuántas cuotas del compromiso rebaja e historial. */
	public record DescuentoDelCompromiso(DescuentoAplicadoVista activo, int cuotasConDescuento,
			List<DescuentoAplicadoVista> historial) {
	}

	/** Respuesta de aplicar (RF-88) o revocar (RF-90): cuántas cuotas pendientes se recalcularon. */
	public record ResultadoDescuento(DescuentoAplicadoVista descuento, int cuotasRecalculadas) {
	}
}
