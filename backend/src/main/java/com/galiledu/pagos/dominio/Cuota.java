package com.galiledu.pagos.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
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

	/**
	 * RF-91: una pensión impaga es vencida después de su vencimiento más los días de gracia del tarifario. Una cuota
	 * con un pago en línea en proceso no cuenta como vencida mientras la pasarela responde (sección 3.12).
	 */
	public boolean estaVencida(LocalDate hoy, int diasGracia) {
		return CONCEPTO_PENSION.equals(conceptoCodigo)
				&& estado != EstadoCuota.PAGADA && estado != EstadoCuota.ANULADA && estado != EstadoCuota.EN_PROCESO
				&& hoy.isAfter(fechaVencimiento.plusDays(diasGracia));
	}

	public boolean estaActiva() {
		return estado != EstadoCuota.ANULADA;
	}

	/** HU-41: se puede pagar en línea si tiene saldo y no está pagada, anulada ni en otro pago en proceso. */
	public boolean esPagable() {
		return (estado == EstadoCuota.PENDIENTE || estado == EstadoCuota.PAGADA_PARCIAL) && saldo().signum() > 0;
	}

	/** Capital que falta pagar. */
	public BigDecimal saldo() {
		return getMontoFinal().subtract(montoPagado);
	}

	/**
	 * HU-41: entra a una orden de pago. Queda EN_PROCESO hasta que la pasarela responda (sección 3.8) y devuelve el
	 * estado anterior, que se restaura si la orden se rechaza o expira.
	 */
	public EstadoCuota iniciarPago() {
		if (!esPagable()) {
			throw new IllegalStateException("La cuota " + nombre + " no se puede pagar.");
		}
		EstadoCuota anterior = estado;
		estado = EstadoCuota.EN_PROCESO;
		return anterior;
	}

	/** La pasarela rechazó o no confirmó a tiempo: la cuota vuelve a como estaba. */
	public void restaurar(EstadoCuota anterior) {
		if (estado == EstadoCuota.EN_PROCESO) {
			estado = anterior;
		}
	}

	/** Pago confirmado: suma el capital cubierto; queda PAGADA si se completó y PAGADA_PARCIAL si no. */
	public void registrarPago(BigDecimal capital, LocalDate fecha) {
		montoPagado = montoPagado.add(capital);
		if (saldo().signum() <= 0) {
			estado = EstadoCuota.PAGADA;
			fechaPago = fecha;
		} else {
			estado = EstadoCuota.PAGADA_PARCIAL;
		}
	}

	public CompromisoPago getCompromiso() {
		return compromiso;
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

	/** monto_final lo calcula la BD; en una cuota recién creada (aún sin leer) se calcula aquí. */
	public BigDecimal getMontoFinal() {
		return montoFinal != null ? montoFinal : montoBase.subtract(montoDescuento);
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

	public BigDecimal getPorcentajeDescuento() {
		return porcentajeDescuento;
	}
}
