package com.galiledu.pagos.dominio;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** RF-75: tramo del descuento por hermanos ("2 hermanos: 10 % · 3+: 15 %"). {@code hasta} null = "o más". */
@Entity
@Table(schema = "pagos", name = "descuento_tramo")
public class DescuentoTramo {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "descuento_id", nullable = false)
	private Descuento descuento;

	@Column(name = "hermanos_desde", nullable = false)
	private short hermanosDesde;
	@Column(name = "hermanos_hasta")
	private Short hermanosHasta;
	@Column(nullable = false)
	private BigDecimal porcentaje;

	protected DescuentoTramo() {
	}

	DescuentoTramo(Descuento descuento, int desde, Integer hasta, BigDecimal porcentaje) {
		this.descuento = descuento;
		this.hermanosDesde = (short) desde;
		this.hermanosHasta = hasta == null ? null : hasta.shortValue();
		this.porcentaje = porcentaje;
	}

	/** ¿El tramo cubre esa cantidad de hermanos matriculados? */
	public boolean cubre(int hermanos) {
		return hermanos >= hermanosDesde && (hermanosHasta == null || hermanos <= hermanosHasta);
	}

	public int getHermanosDesde() {
		return hermanosDesde;
	}

	public Integer getHermanosHasta() {
		return hermanosHasta == null ? null : hermanosHasta.intValue();
	}

	public BigDecimal getPorcentaje() {
		return porcentaje;
	}
}
