package com.galiledu.pagos.dominio;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** RF-77: porcentaje máximo de descuento que admite la institución (una sola fila, id = 1). */
@Entity
@Table(schema = "pagos", name = "configuracion")
public class ConfiguracionPagos {
	public static final short ID = 1;

	@Id
	private Short id;
	@Column(name = "porcentaje_maximo_descuento", nullable = false)
	private BigDecimal porcentajeMaximoDescuento;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	protected ConfiguracionPagos() {
	}

	public void cambiarPorcentajeMaximo(BigDecimal porcentaje) {
		this.porcentajeMaximoDescuento = porcentaje;
		this.actualizadoEn = Instant.now();
	}

	public BigDecimal getPorcentajeMaximoDescuento() {
		return porcentajeMaximoDescuento;
	}
}
