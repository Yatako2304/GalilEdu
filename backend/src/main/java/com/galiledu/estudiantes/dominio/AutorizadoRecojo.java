package com.galiledu.estudiantes.dominio;

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

/** Paso 4: persona autorizada a recoger al estudiante (tabla estudiantes.autorizado_recojo). */
@Entity
@Table(schema = "estudiantes", name = "autorizado_recojo")
public class AutorizadoRecojo {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estudiante_id", nullable = false)
	private Estudiante estudiante;

	@Column(name = "nombre_completo", nullable = false)
	private String nombreCompleto;
	@Column(nullable = false)
	private String relacion;

	protected AutorizadoRecojo() {
	}

	public AutorizadoRecojo(String nombreCompleto, String relacion) {
		this.nombreCompleto = nombreCompleto;
		this.relacion = relacion;
	}

	void asociar(Estudiante estudiante) {
		this.estudiante = estudiante;
	}

	public String getNombreCompleto() {
		return nombreCompleto;
	}
	public String getRelacion() {
		return relacion;
	}
}
