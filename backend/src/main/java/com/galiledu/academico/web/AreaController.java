package com.galiledu.academico.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.academico.aplicacion.DtosAcademico.AreaDetalle;
import com.galiledu.academico.aplicacion.DtosAcademico.AreaResumen;
import com.galiledu.academico.aplicacion.DtosAcademico.CompetenciaResumen;
import com.galiledu.academico.aplicacion.DtosAcademico.CursoResumen;
import com.galiledu.academico.aplicacion.DtosAcademico.ResumenPlanCurricular;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudArea;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudCompetencia;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudCurso;
import com.galiledu.academico.aplicacion.ServicioPlanCurricular;

/** Figma > Personal Administrativo > CI-14 · Plan curricular (HU-52, HU-53, HU-54). */
@RestController
@RequestMapping("/areas")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class AreaController {
	private final ServicioPlanCurricular servicio;

	public AreaController(ServicioPlanCurricular servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public List<AreaResumen> listar() {
		return servicio.listarAreas();
	}

	/** Tarjetas de la cabecera: áreas, competencias y cursos activos de los registrados. */
	@GetMapping("/resumen")
	public ResumenPlanCurricular resumen() {
		return servicio.resumen();
	}

	@GetMapping("/{id}")
	public AreaDetalle detalle(@PathVariable UUID id) {
		return servicio.detalleArea(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AreaDetalle crear(@Valid @RequestBody SolicitudArea solicitud) {
		return servicio.crearArea(solicitud);
	}

	@PutMapping("/{id}")
	public AreaDetalle actualizar(@PathVariable UUID id, @Valid @RequestBody SolicitudArea solicitud) {
		return servicio.actualizarArea(id, solicitud);
	}

	@PostMapping("/{id}/desactivar")
	public AreaDetalle desactivar(@PathVariable UUID id) {
		return servicio.desactivarArea(id);
	}

	@PostMapping("/{id}/reactivar")
	public AreaDetalle reactivar(@PathVariable UUID id) {
		return servicio.reactivarArea(id);
	}

	@PostMapping("/{id}/competencias")
	@ResponseStatus(HttpStatus.CREATED)
	public CompetenciaResumen crearCompetencia(@PathVariable UUID id,
			@Valid @RequestBody SolicitudCompetencia solicitud) {
		return servicio.crearCompetencia(id, solicitud);
	}

	@PostMapping("/{id}/cursos")
	@ResponseStatus(HttpStatus.CREATED)
	public CursoResumen crearCurso(@PathVariable UUID id, @Valid @RequestBody SolicitudCurso solicitud) {
		return servicio.crearCurso(id, solicitud);
	}
}
