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

/** Paso 4: contacto de emergencia (tabla estudiantes.contacto_emergencia). */
@Entity
@Table(schema = "estudiantes", name = "contacto_emergencia")
public class ContactoEmergencia {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estudiante_id", nullable = false)
	private Estudiante estudiante;

	@Column(name = "nombre_completo", nullable = false)
	private String nombreCompleto;
	@Column(nullable = false)
	private String parentesco;
	@Column(name = "telefono_fijo")
	private String telefonoFijo;
	private String celular;
	@Column(nullable = false)
	private short orden;

	protected ContactoEmergencia() {
	}

	public ContactoEmergencia(String nombreCompleto, String parentesco, String telefonoFijo, String celular,
			short orden) {
		this.nombreCompleto = nombreCompleto;
		this.parentesco = parentesco;
		this.telefonoFijo = telefonoFijo;
		this.celular = celular;
		this.orden = orden;
	}

	void asociar(Estudiante estudiante) {
		this.estudiante = estudiante;
	}

	public String getNombreCompleto() {
		return nombreCompleto;
	}
	public String getParentesco() {
		return parentesco;
	}
	public String getTelefonoFijo() {
		return telefonoFijo;
	}
	public String getCelular() {
		return celular;
	}
}
