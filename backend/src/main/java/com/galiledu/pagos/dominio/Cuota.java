package com.galiledu.pagos.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Formula;

/** Cuota de un compromiso: matrícula (HU-36), pensiones (HU-37), especiales y masivas (tabla pagos.cuota). */
@Entity
@Table(schema = "pagos", name = "cuota")
public class Cuota {
	public static final String CONCEPTO_PENSION = "PENSION";
	public static final String CONCEPTO_MATRICULA = "MATRICULA";

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "compromiso_id", nullable = false)
	private CompromisoPago compromiso;

	@Column(name = "concepto_codigo", nullable = false)
	private String conceptoCodigo;
	private Short numero;
	@Column(nullable = false)
	private String nombre;

	@Column(name = "monto_base", nullable = false)
	private BigDecimal montoBase;
	@Column(name = "monto_descuento", nullable = false)
	private BigDecimal montoDescuento;
	/** Columna calculada por la BD: monto_base - monto_descuento. */
	@Column(name = "monto_final", insertable = false, updatable = false)
	private BigDecimal montoFinal;
	@Column(name = "monto_pagado", nullable = false)
	private BigDecimal montoPagado;

	@Column(name = "fecha_vencimiento", nullable = false)
	private LocalDate fechaVencimiento;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoCuota estado;
	@Column(name = "fecha_pago")
	private LocalDate fechaPago;

	@Column(name = "descuento_aplicado_id")
	private UUID descuentoAplicadoId;
	private String motivo;
	@Column(name = "creado_por")
	private UUID creadoPor;

	@Formula("(SELECT d.porcentaje FROM pagos.descuento_aplicado d WHERE d.id = descuento_aplicado_id)")
	private BigDecimal porcentajeDescuento;

	protected Cuota() {
	}

	/**
	 * RF-81: pensión PENDIENTE del cronograma. Si el estudiante tiene un descuento activo que aplica a pensiones, la
	 * cuota guarda el monto rebajado y el id del descuento (la BD lo exige: ck_cuota_descuento).
	 */
	public static Cuota pension(CompromisoPago compromiso, short numero, String nombre, BigDecimal monto,
			LocalDate vencimiento, BigDecimal descuento, UUID descuentoAplicadoId, UUID creadoPor) {
		Cuota q = new Cuota();
		q.compromiso = compromiso;
		q.conceptoCodigo = CONCEPTO_PENSION;
		q.numero = numero;
		q.nombre = nombre;
		q.montoBase = monto;
		q.montoDescuento = descuento;
		q.descuentoAplicadoId = descuento.signum() > 0 ? descuentoAplicadoId : null;
		q.montoPagado = BigDecimal.ZERO;
		q.fechaVencimiento = vencimiento;
		q.estado = EstadoCuota.PENDIENTE;
		q.creadoPor = creadoPor;
		return q;
	}

	/**
	 * RF-78: cuota de matrícula PENDIENTE con el monto y el vencimiento del tarifario. Si el estudiante tiene un
	 * descuento activo que aplica a MATRICULA, se guarda rebajada (ck_cuota_descuento exige el id del descuento).
	 */
	public static Cuota matricula(CompromisoPago compromiso, String nombre, BigDecimal monto, LocalDate vencimiento,
			BigDecimal descuento, UUID descuentoAplicadoId, UUID creadoPor) {
		Cuota q = pension(compromiso, (short) 0, nombre, monto, vencimiento, descuento, descuentoAplicadoId,
				creadoPor);
		q.conceptoCodigo = CONCEPTO_MATRICULA;
		q.numero = null;
		return q;
	}

	public boolean esMatricula() {
		return CONCEPTO_MATRICULA.equals(conceptoCodigo);
	}

	/** RF-85: por retiro se anulan solo las pensiones PENDIENTES que vencen después de la fecha de retiro. */
	public boolean anularPorRetiro(LocalDate fechaRetiro) {
		if (!esPension() || estado != EstadoCuota.PENDIENTE || !fechaVencimiento.isAfter(fechaRetiro)) {
			return false;
		}
		estado = EstadoCuota.ANULADA;
		motivo = "Retiro del estudiante el " + fechaRetiro;
		return true;
	}

	/** RF-91: una pensión impaga es vencida después de su vencimiento más los días de gracia del tarifario. */
	public boolean estaVencida(LocalDate hoy, int diasGracia) {
		return CONCEPTO_PENSION.equals(conceptoCodigo)
				&& estado != EstadoCuota.PAGADA && estado != EstadoCuota.ANULADA
				&& hoy.isAfter(fechaVencimiento.plusDays(diasGracia));
	}

	/** Capital que falta pagar: monto base menos descuento menos lo ya pagado (nunca negativo). */
	public BigDecimal capitalPendiente() {
		return montoBase.subtract(montoDescuento).subtract(montoPagado).max(BigDecimal.ZERO);
	}

	/** Días desde el vencimiento hasta hoy (0 si aún no vence). */
	public int diasAtraso(LocalDate hoy) {
		return (int) Math.max(0, ChronoUnit.DAYS.between(fechaVencimiento, hoy));
	}

	/**
	 * RF-92: recargo por mora de una pensión vencida (RF-91). Se cobra la tasa del tarifario por cada 30 días completos
	 * de
	 * atraso sobre el capital pendiente, sin capitalizar, y nunca más que el tope (porcentaje del capital). Figma P11:
	 * S/ 600 con 63 días de atraso y tasa 2 % → 2 × 2 % × 600 = S/ 24; con 32 días → S/ 12.
	 */
	public BigDecimal recargo(LocalDate hoy, int diasGracia, BigDecimal tasa, BigDecimal tope) {
		if (!estaVencida(hoy, diasGracia)) {
			return BigDecimal.ZERO;
		}
		BigDecimal capital = capitalPendiente();
		long periodos = diasAtraso(hoy) / 30;
		BigDecimal cien = BigDecimal.valueOf(100);
		BigDecimal recargo = capital.multiply(tasa).multiply(BigDecimal.valueOf(periodos))
				.divide(cien, 2, RoundingMode.HALF_UP);
		BigDecimal maximo = capital.multiply(tope).divide(cien, 2, RoundingMode.HALF_UP);
		return recargo.min(maximo);
	}

	/** RF-88: solo se recalculan las cuotas PENDIENTES sin pagos; las pagadas o parciales conservan su monto. */
	public boolean admiteRecalculo() {
		return estado == EstadoCuota.PENDIENTE && montoPagado.signum() == 0;
	}

	/** RF-88: aplica el porcentaje sobre el monto original (monto base), no sobre un monto ya rebajado. */
	public boolean aplicarDescuento(BigDecimal porcentaje, UUID descuentoAplicadoId) {
		if (!admiteRecalculo()) {
			return false;
		}
		montoDescuento = montoBase.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		this.descuentoAplicadoId = descuentoAplicadoId;
		// monto_final lo calcula la BD; se actualiza aquí para que la misma sesión lea el valor nuevo.
		montoFinal = montoBase.subtract(montoDescuento);
		porcentajeDescuento = porcentaje;
		return true;
	}

	/** RF-87, RF-90: restituye el monto original si la cuota tenía ese descuento y todavía no se paga. */
	public boolean quitarDescuento(UUID descuentoAplicadoId) {
		if (!descuentoAplicadoId.equals(this.descuentoAplicadoId) || !admiteRecalculo()) {
			return false;
		}
		montoDescuento = BigDecimal.ZERO;
		this.descuentoAplicadoId = null;
		montoFinal = montoBase;
		porcentajeDescuento = null;
		return true;
	}

	public boolean estaActiva() {
		return estado != EstadoCuota.ANULADA;
	}

	public boolean esPension() {
		return CONCEPTO_PENSION.equals(conceptoCodigo);
	}

	public UUID getId() {
		return id;
	}

	public String getConceptoCodigo() {
		return conceptoCodigo;
	}

	public Short getNumero() {
		return numero;
	}

	public String getNombre() {
		return nombre;
	}

	public BigDecimal getMontoBase() {
		return montoBase;
	}

	public BigDecimal getMontoDescuento() {
		return montoDescuento;
	}

	public BigDecimal getMontoFinal() {
		return montoFinal;
	}

	public BigDecimal getMontoPagado() {
		return montoPagado;
	}

	public LocalDate getFechaVencimiento() {
		return fechaVencimiento;
	}

	public EstadoCuota getEstado() {
		return estado;
	}

	public LocalDate getFechaPago() {
		return fechaPago;
	}

	public UUID getDescuentoAplicadoId() {
		return descuentoAplicadoId;
	}

	public BigDecimal getPorcentajeDescuento() {
		return porcentajeDescuento;
	}
}
