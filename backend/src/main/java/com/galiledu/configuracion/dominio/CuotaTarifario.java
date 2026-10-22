package com.galiledu.configuracion.dominio;

import java.time.LocalDate;
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

/**
 * Cuota de pensión del calendario de un {@link Tarifario} (tabla pagos.cuota_tarifario, V4). Todas valen
 * {@code tarifario.monto_pension}. Las cuotas de los alumnos (pagos.cuota) no apuntan a esta tabla.
 */
@Entity
@Table(schema = "pagos", name = "cuota_tarifario")
public class CuotaTarifario {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tarifario_id", nullable = false, updatable = false)
	private Tarifario tarifario;

	@Column(nullable = false)
	private short numero;
	@Column(nullable = false)
	private String nombre;
	@Column(name = "fecha_vencimiento", nullable = false)
	private LocalDate fechaVencimiento;

	protected CuotaTarifario() {
	}

	/** Solo la crea {@link Tarifario}, que es quien genera el calendario. */
	CuotaTarifario(Tarifario tarifario, int numero, String nombre, LocalDate fechaVencimiento) {
		this.tarifario = tarifario;
		this.numero = (short) numero;
		this.nombre = nombre;
		this.fechaVencimiento = fechaVencimiento;
	}

	public int getNumero() {
		return numero;
	}
	public String getNombre() {
		return nombre;
	}
	public LocalDate getFechaVencimiento() {
		return fechaVencimiento;
	}
}
