package com.galiledu.academico.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Curso que se dicta en un grado y año, con sus horas, las competencias que evalúa y el docente de cada sección
 * (tablas academico.plan_curso, plan_competencia y docente_seccion, V6). HU-56.
 */
@Entity
@Table(schema = "academico", name = "plan_curso")
public class PlanCurso {
	public static final int MIN_HORAS = 1;
	public static final int MAX_HORAS = 20;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "anio_escolar_id", nullable = false, updatable = false)
	private UUID anioEscolarId;
	@Column(name = "grado_codigo", nullable = false, updatable = false)
	private String gradoCodigo;
	@Column(name = "curso_id", nullable = false, updatable = false)
	private UUID cursoId;
	@Column(name = "horas_semanales", nullable = false)
	private short horasSemanales;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(schema = "academico", name = "plan_competencia", joinColumns = @JoinColumn(name = "plan_curso_id"))
	private List<CompetenciaEvaluada> competencias = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(schema = "academico", name = "docente_seccion", joinColumns = @JoinColumn(name = "plan_curso_id"))
	private List<DocenteAsignado> docentes = new ArrayList<>();

	protected PlanCurso() {
	}

	public PlanCurso(UUID anioEscolarId, String gradoCodigo, UUID cursoId, int horasSemanales,
			List<CompetenciaEvaluada> competencias) {
		this.anioEscolarId = anioEscolarId;
		this.gradoCodigo = gradoCodigo;
		this.cursoId = cursoId;
		this.horasSemanales = (short) horasSemanales;
		this.competencias = new ArrayList<>(competencias);
	}

	@PrePersist
	void alCrear() {
		creadoEn = Instant.now();
		actualizadoEn = creadoEn;
	}

	@PreUpdate
	void alActualizar() {
		actualizadoEn = Instant.now();
	}

	public void configurar(int horasSemanales, List<CompetenciaEvaluada> competencias) {
		this.horasSemanales = (short) horasSemanales;
		this.competencias.clear();
		this.competencias.addAll(competencias);
		// La colección no cambia la fila de plan_curso; se marca para que "Última configuración" se actualice.
		this.actualizadoEn = Instant.now();
	}

	public void asignarDocentes(List<DocenteAsignado> docentes) {
		this.docentes.clear();
		this.docentes.addAll(docentes);
	}

	public UUID getId() {
		return id;
	}
	public UUID getAnioEscolarId() {
		return anioEscolarId;
	}
	public String getGradoCodigo() {
		return gradoCodigo;
	}
	public UUID getCursoId() {
		return cursoId;
	}
	public int getHorasSemanales() {
		return horasSemanales;
	}
	public Instant getActualizadoEn() {
		return actualizadoEn;
	}
	public List<CompetenciaEvaluada> getCompetencias() {
		return competencias;
	}
	public List<DocenteAsignado> getDocentes() {
		return docentes;
	}
}
