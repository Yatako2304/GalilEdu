package com.galiledu.academico.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.academico.aplicacion.DtosAcademico.AnioEscolarOpcion;
import com.galiledu.academico.aplicacion.DtosAcademico.CursoDisponible;
import com.galiledu.academico.aplicacion.DtosAcademico.DocenteOpcion;
import com.galiledu.academico.aplicacion.DtosAcademico.DocenteSeccion;
import com.galiledu.academico.aplicacion.DtosAcademico.OrigenPlan;
import com.galiledu.academico.aplicacion.DtosAcademico.PlanCursoDetalle;
import com.galiledu.academico.aplicacion.DtosAcademico.PlanGrado;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudConfigurarCurso;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudCopiaPlan;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudDocentes;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudPlanCurso;
import com.galiledu.academico.aplicacion.ServicioPlanEvaluacion;

/**
 * Figma > Personal Administrativo > CI-17 y CI-20 · Plan de evaluación (HU-56). Cada recurso es un curso del plan de
 * un grado y año (academico.plan_curso). También lo usa el Coordinador (RF-130 a RF-132), aunque en Figma solo está
 * en el menú del Personal administrativo.
 */
@RestController
@RequestMapping("/planes-evaluacion")
@PreAuthorize("hasAnyRole('PERSONAL_ADMINISTRATIVO', 'COORDINADOR')")
public class PlanEvaluacionController {
	private final ServicioPlanEvaluacion servicio;

	public PlanEvaluacionController(ServicioPlanEvaluacion servicio) {
		this.servicio = servicio;
	}

	/** GET /api/planes-evaluacion?anioEscolarId=&grado=SEC-1 */
	@GetMapping
	public PlanGrado plan(@RequestParam UUID anioEscolarId, @RequestParam String grado) {
		return servicio.plan(anioEscolarId, grado);
	}

	@GetMapping("/anios")
	public List<AnioEscolarOpcion> anios() {
		return servicio.anios();
	}

	@GetMapping("/docentes")
	public List<DocenteOpcion> docentes() {
		return servicio.docentes();
	}

	@GetMapping("/cursos-disponibles")
	public List<CursoDisponible> cursosDisponibles(@RequestParam UUID anioEscolarId, @RequestParam String grado) {
		return servicio.cursosDisponibles(anioEscolarId, grado);
	}

	@GetMapping("/origenes")
	public List<OrigenPlan> origenes(@RequestParam UUID anioEscolarId, @RequestParam String grado) {
		return servicio.origenes(anioEscolarId, grado);
	}

	@PostMapping("/copias")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanGrado copiar(@Valid @RequestBody SolicitudCopiaPlan solicitud) {
		return servicio.copiarPlan(solicitud);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PlanCursoDetalle agregar(@Valid @RequestBody SolicitudPlanCurso solicitud) {
		return servicio.agregarCurso(solicitud);
	}

	@GetMapping("/{id}")
	public PlanCursoDetalle detalle(@PathVariable UUID id) {
		return servicio.detalle(id);
	}

	@PutMapping("/{id}")
	public PlanCursoDetalle configurar(@PathVariable UUID id, @Valid @RequestBody SolicitudConfigurarCurso solicitud) {
		return servicio.configurarCurso(id, solicitud);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void quitar(@PathVariable UUID id) {
		servicio.quitarCurso(id);
	}

	@GetMapping("/{id}/docentes")
	public List<DocenteSeccion> docentesDelCurso(@PathVariable UUID id) {
		return servicio.docentesDelCurso(id);
	}

	@PutMapping("/{id}/docentes")
	public List<DocenteSeccion> asignarDocentes(@PathVariable UUID id,
			@Valid @RequestBody SolicitudDocentes solicitud) {
		return servicio.asignarDocentes(id, solicitud);
	}
}
