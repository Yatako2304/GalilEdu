package com.galiledu.pagos.dominio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Pago confirmado de un estudiante con su constancia (pagos.pago y pago_detalle, V5). HU-41 lo registra al aprobarse
 * una orden; queda OBSERVADO si el monto confirmado por la pasarela no coincide con el de la orden (RF-105).
 */
@Entity
@Table(schema = "pagos", name = "pago")
public class Pago {
	public static final String CONFIRMADO = "CONFIRMADO";
	public static final String OBSERVADO = "OBSERVADO";

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "orden_pago_id")
	private UUID ordenPagoId;
	@Column(name = "estudiante_id", nullable = false)
	private UUID estudianteId;
	@Column(name = "numero_constancia", nullable = false, unique = true)
	private String numeroConstancia;
	@Column(name = "fecha_pago", nullable = false)
	private OffsetDateTime fechaPago;
	private String referencia;
	@Column(name = "monto_total", nullable = false)
	private BigDecimal montoTotal;
	@Column(nullable = false)
	private String estado;
	private String observacion;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(schema = "pagos", name = "pago_detalle", joinColumns = @JoinColumn(name = "pago_id"))
	private List<Detalle> detalle = new ArrayList<>();

	@Embeddable
	public static class Detalle {
		@Column(name = "cuota_id", nullable = false)
		private UUID cuotaId;
		@Column(nullable = false)
		private BigDecimal capital;
		@Column(nullable = false)
		private BigDecimal recargo;

		protected Detalle() {
		}

		Detalle(UUID cuotaId, BigDecimal capital, BigDecimal recargo) {
			this.cuotaId = cuotaId;
			this.capital = capital;
			this.recargo = recargo;
		}

		public UUID getCuotaId() {
			return cuotaId;
		}

		public BigDecimal getCapital() {
			return capital;
		}

		public BigDecimal getRecargo() {
			return recargo;
		}
	}

	protected Pago() {
	}

	public static Pago enLinea(UUID ordenPagoId, UUID estudianteId, String numeroConstancia, OffsetDateTime fecha,
			String referencia) {
		Pago p = new Pago();
		p.ordenPagoId = ordenPagoId;
		p.estudianteId = estudianteId;
		p.numeroConstancia = numeroConstancia;
		p.fechaPago = fecha;
		p.referencia = referencia;
		p.montoTotal = BigDecimal.ZERO;
		p.estado = CONFIRMADO;
		return p;
	}

	public void agregar(UUID cuotaId, BigDecimal capital, BigDecimal recargo) {
		detalle.add(new Detalle(cuotaId, capital, recargo));
		montoTotal = montoTotal.add(capital).add(recargo);
	}

	public void observar(String motivo) {
		estado = OBSERVADO;
		observacion = motivo;
	}

	public UUID getId() {
		return id;
	}

	public UUID getEstudianteId() {
		return estudianteId;
	}

	public String getNumeroConstancia() {
		return numeroConstancia;
	}

	public OffsetDateTime getFechaPago() {
		return fechaPago;
	}

	public BigDecimal getMontoTotal() {
		return montoTotal;
	}

	public String getEstado() {
		return estado;
	}

	public String getObservacion() {
		return observacion;
	}

	public List<Detalle> getDetalle() {
		return detalle;
	}
}
