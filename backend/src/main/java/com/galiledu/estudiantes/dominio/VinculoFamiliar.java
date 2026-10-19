package com.galiledu.estudiantes.dominio;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.galiledu.personas.dominio.Persona;

/**
 * Paso 3: padre, madre o apoderado del estudiante (tabla estudiantes.vinculo_familiar).
 * Si el padre o la madre "no registra / fallecido", persona es null y se guarda el motivo.
 */
@Entity
@Table(schema = "estudiantes", name = "vinculo_familiar")
public class VinculoFamiliar {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estudiante_id", nullable = false)
	private Estudiante estudiante;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "persona_id")
	private Persona persona;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Parentesco parentesco;
	private String relacion;
	@Column(name = "vive_con_estudiante", nullable = false)
	private boolean viveConEstudiante;
	@Column(name = "tiene_patria_potestad", nullable = false)
	private boolean tienePatriaPotestad;
	@Column(name = "es_responsable_matricula", nullable = false)
	private boolean esResponsableMatricula;
	@Column(name = "motivo_no_registra")
	private String motivoNoRegistra;
	private String observacion;

	protected VinculoFamiliar() {
	}

	public static VinculoFamiliar conPersona(Parentesco parentesco, Persona persona, String relacion,
			boolean viveConEstudiante, boolean tienePatriaPotestad) {
		VinculoFamiliar v = new VinculoFamiliar();
		v.parentesco = parentesco;
		v.persona = persona;
		v.relacion = relacion;
		v.viveConEstudiante = viveConEstudiante;
		v.tienePatriaPotestad = tienePatriaPotestad;
		return v;
	}

	public static VinculoFamiliar noRegistra(Parentesco parentesco, String motivo, String observacion) {
		VinculoFamiliar v = new VinculoFamiliar();
		v.parentesco = parentesco;
		v.motivoNoRegistra = motivo;
		v.observacion = observacion;
		return v;
	}

	void asociar(Estudiante estudiante) {
		this.estudiante = estudiante;
	}

	public void marcarResponsable() {
		this.esResponsableMatricula = true;
	}

	public UUID getId() {
		return id;
	}
	public Estudiante getEstudiante() {
		return estudiante;
	}
	public Persona getPersona() {
		return persona;
	}
	public Parentesco getParentesco() {
		return parentesco;
	}
	public String getRelacion() {
		return relacion;
	}
	public boolean isViveConEstudiante() {
		return viveConEstudiante;
	}
	public boolean isTienePatriaPotestad() {
		return tienePatriaPotestad;
	}
	public boolean isEsResponsableMatricula() {
		return esResponsableMatricula;
	}
	public String getMotivoNoRegistra() {
		return motivoNoRegistra;
	}
	public String getObservacion() {
		return observacion;
	}
}
