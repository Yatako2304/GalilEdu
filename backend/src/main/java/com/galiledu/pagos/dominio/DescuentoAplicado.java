package com.galiledu.pagos.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * HU-38: descuento aplicado a un estudiante (tabla pagos.descuento_aplicado). Guarda una copia del nombre y del
 * porcentaje para que editar o desactivar el catálogo no cambie lo ya otorgado (RF-76, RF-89).
 */
@Entity
@Table(schema = "pagos", name = "descuento_aplicado")
public class DescuentoAplicado {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "estudiante_id", nullable = false)
	private UUID estudianteId;
	@Column(name = "descuento_id", nullable = false)
	private UUID descuentoId;
	@Column(nullable = false)
	private String nombre;
	@Column(nullable = false)
	private BigDecimal porcentaje;
	@Column(name = "vigencia_desde", nullable = false)
	private LocalDate vigenciaDesde;
	@Column(name = "vigencia_hasta")
	private LocalDate vigenciaHasta;
	@Column(nullable = false)
	private String motivo;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoDescuentoAplicado estado;
	@Column(name = "aplicado_por")
	private UUID aplicadoPor;
	@Column(name = "aplicado_en", nullable = false, updatable = false)
	private Instant aplicadoEn;
	@Column(name = "motivo_revocacion")
	private String motivoRevocacion;
	@Column(name = "revocado_por")
	private UUID revocadoPor;
	@Column(name = "revocado_en")
	private Instant revocadoEn;

	protected DescuentoAplicado() {
	}

	public static DescuentoAplicado nuevo(UUID estudianteId, Descuento descuento, BigDecimal porcentaje,
			LocalDate desde, LocalDate hasta, String motivo, UUID usuarioId) {
		DescuentoAplicado d = new DescuentoAplicado();
		d.estudianteId = estudianteId;
		d.descuentoId = descuento.getId();
		d.nombre = descuento.getNombre();
		d.porcentaje = porcentaje;
		d.vigenciaDesde = desde;
		d.vigenciaHasta = hasta;
		d.motivo = motivo;
		d.estado = EstadoDescuentoAplicado.ACTIVO;
		d.aplicadoPor = usuarioId;
		d.aplicadoEn = Instant.now();
		return d;
	}

	/** RF-88: el descuento rige para las cuotas que vencen dentro de su vigencia. */
	public boolean vigenteEn(LocalDate fecha) {
		return !fecha.isBefore(vigenciaDesde) && (vigenciaHasta == null || !fecha.isAfter(vigenciaHasta));
	}

	public boolean estaActivo() {
		return estado == EstadoDescuentoAplicado.ACTIVO;
	}

	/** RF-87: otro descuento lo reemplaza. */
	public void reemplazar() {
		estado = EstadoDescuentoAplicado.REEMPLAZADO;
	}

	/** RF-90: la revocación exige un motivo. */
	public void revocar(String motivo, UUID usuarioId) {
		estado = EstadoDescuentoAplicado.REVOCADO;
		motivoRevocacion = motivo;
		revocadoPor = usuarioId;
		revocadoEn = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getEstudianteId() {
		return estudianteId;
	}

	public UUID getDescuentoId() {
		return descuentoId;
	}

	public String getNombre() {
		return nombre;
	}

	public BigDecimal getPorcentaje() {
		return porcentaje;
	}

	public LocalDate getVigenciaDesde() {
		return vigenciaDesde;
	}

	public LocalDate getVigenciaHasta() {
		return vigenciaHasta;
	}

	public String getMotivo() {
		return motivo;
	}

	public EstadoDescuentoAplicado getEstado() {
		return estado;
	}

	public Instant getAplicadoEn() {
		return aplicadoEn;
	}

	public UUID getAplicadoPor() {
		return aplicadoPor;
	}

	public String getMotivoRevocacion() {
		return motivoRevocacion;
	}

	public Instant getRevocadoEn() {
		return revocadoEn;
	}
}
