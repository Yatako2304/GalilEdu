package com.galiledu.configuracion.dominio;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Receso de una estructura horaria (tabla configuracion.receso, V4): dura {@code duracionMin} tras un bloque. */
@Entity
@Table(schema = "configuracion", name = "receso")
public class Receso {
	public static final int DURACION_MINIMA_MIN = 5;
	public static final int DURACION_MAXIMA_MIN = 60;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "despues_de_bloque", nullable = false, updatable = false)
	private short despuesDeBloque;
	@Column(name = "duracion_min", nullable = false)
	private short duracionMin;

	protected Receso() {
	}

	static Receso desde(DatosReceso datos) {
		Receso receso = new Receso();
		receso.despuesDeBloque = (short) datos.despuesDeBloque();
		receso.duracionMin = (short) datos.duracionMin();
		return receso;
	}

	void cambiarDuracion(int duracionNuevaMin) {
		this.duracionMin = (short) duracionNuevaMin;
	}

	public UUID getId() {
		return id;
	}

	public int getDespuesDeBloque() {
		return despuesDeBloque;
	}

	public int getDuracionMin() {
		return duracionMin;
	}

	public DatosReceso datos() {
		return new DatosReceso(despuesDeBloque, duracionMin);
	}
}
