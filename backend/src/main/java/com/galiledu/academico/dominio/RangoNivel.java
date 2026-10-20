package com.galiledu.academico.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Rango de notas vigesimales de un nivel de logro (tabla academico.tabla_conversion_nivel, V6). */
@Embeddable
public class RangoNivel {
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NivelLogro nivel;
	@Column(nullable = false)
	private String significado;
	@Column(name = "nota_desde", nullable = false)
	private short notaDesde;
	@Column(name = "nota_hasta", nullable = false)
	private short notaHasta;

	protected RangoNivel() {
	}

	public RangoNivel(NivelLogro nivel, int notaDesde, int notaHasta) {
		this.nivel = nivel;
		this.significado = nivel.significado();
		this.notaDesde = (short) notaDesde;
		this.notaHasta = (short) notaHasta;
	}

	public NivelLogro getNivel() {
		return nivel;
	}
	public String getSignificado() {
		return significado;
	}
	public int getNotaDesde() {
		return notaDesde;
	}
	public int getNotaHasta() {
		return notaHasta;
	}
}
