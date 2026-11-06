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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * HU-41 · Orden de pago en línea (pagos.orden_pago, V5 y V13): las cuotas que el apoderado eligió, con el capital y
 * el recargo de cada una. Mientras la pasarela no responde está EN_PROCESO y sus cuotas también.
 */
@Entity
@Table(schema = "pagos", name = "orden_pago")
public class OrdenPago {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String codigo;
	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;
	@Column(name = "monto_total", nullable = false)
	private BigDecimal montoTotal;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoOrdenPago estado;

	@Column(name = "referencia_pasarela", unique = true)
	private String referenciaPasarela;
	@Column(name = "url_pago")
	private String urlPago;
	@Column(name = "monto_confirmado")
	private BigDecimal montoConfirmado;
	@Column(name = "motivo_rechazo")
	private String motivoRechazo;

	@Column(name = "creado_en", nullable = false)
	private OffsetDateTime creadoEn;
	@Column(name = "expira_en", nullable = false)
	private OffsetDateTime expiraEn;
	@Column(name = "resuelto_en")
	private OffsetDateTime resueltoEn;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(schema = "pagos", name = "orden_pago_cuota", joinColumns = @JoinColumn(name = "orden_pago_id"))
	private List<Linea> lineas = new ArrayList<>();

	/** Una cuota de la orden: capital y recargo cobrados, y el estado al que vuelve si la orden no se paga. */
	@Embeddable
	public static class Linea {
		@Column(name = "cuota_id", nullable = false)
		private UUID cuotaId;
		@Column(nullable = false)
		private BigDecimal capital;
		@Column(nullable = false)
		private BigDecimal recargo;
		@Enumerated(EnumType.STRING)
		@Column(name = "estado_anterior", nullable = false)
		private EstadoCuota estadoAnterior;

		protected Linea() {
		}

		Linea(UUID cuotaId, BigDecimal capital, BigDecimal recargo, EstadoCuota estadoAnterior) {
			this.cuotaId = cuotaId;
			this.capital = capital;
			this.recargo = recargo;
			this.estadoAnterior = estadoAnterior;
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

		public BigDecimal getMonto() {
			return capital.add(recargo);
		}

		public EstadoCuota getEstadoAnterior() {
			return estadoAnterior;
		}
	}

	protected OrdenPago() {
	}

	public static OrdenPago nueva(String codigo, UUID usuarioId, OffsetDateTime ahora, OffsetDateTime expiraEn) {
		OrdenPago o = new OrdenPago();
		o.codigo = codigo;
		o.usuarioId = usuarioId;
		o.montoTotal = BigDecimal.ZERO;
		o.estado = EstadoOrdenPago.EN_PROCESO;
		o.creadoEn = ahora;
		o.expiraEn = expiraEn;
		return o;
	}

	/** Pone la cuota EN_PROCESO y la agrega a la orden con su capital pendiente y su recargo. */
	public void agregar(Cuota cuota, BigDecimal recargo) {
		BigDecimal capital = cuota.saldo();
		EstadoCuota anterior = cuota.iniciarPago();
		lineas.add(new Linea(cuota.getId(), capital, recargo, anterior));
		montoTotal = montoTotal.add(capital).add(recargo);
	}

	/** La pasarela ya creó su sesión de pago. */
	public void asignarSesion(String referencia, String url) {
		this.referenciaPasarela = referencia;
		this.urlPago = url;
	}

	public boolean estaEnProceso() {
		return estado == EstadoOrdenPago.EN_PROCESO;
	}

	public void aprobar(BigDecimal montoConfirmado, OffsetDateTime ahora) {
		this.estado = EstadoOrdenPago.APROBADA;
		this.montoConfirmado = montoConfirmado;
		this.resueltoEn = ahora;
	}

	/** Rechazada (por la pasarela o porque el apoderado canceló) o expirada: queda el motivo. */
	public void cerrarSinPago(EstadoOrdenPago estadoFinal, String motivo, OffsetDateTime ahora) {
		this.estado = estadoFinal;
		this.motivoRechazo = motivo;
		this.resueltoEn = ahora;
	}

	public UUID getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public UUID getUsuarioId() {
		return usuarioId;
	}

	public BigDecimal getMontoTotal() {
		return montoTotal;
	}

	public EstadoOrdenPago getEstado() {
		return estado;
	}

	public String getReferenciaPasarela() {
		return referenciaPasarela;
	}

	public String getUrlPago() {
		return urlPago;
	}

	public BigDecimal getMontoConfirmado() {
		return montoConfirmado;
	}

	public String getMotivoRechazo() {
		return motivoRechazo;
	}

	public OffsetDateTime getCreadoEn() {
		return creadoEn;
	}

	public OffsetDateTime getExpiraEn() {
		return expiraEn;
	}

	public List<Linea> getLineas() {
		return lineas;
	}
}
