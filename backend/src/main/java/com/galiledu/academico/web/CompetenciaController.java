package com.galiledu.academico.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.academico.aplicacion.DtosAcademico.CompetenciaResumen;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudCompetencia;
import com.galiledu.academico.aplicacion.ServicioPlanCurricular;

/** Competencias de un área (HU-53). Se crean desde POST /areas/{id}/competencias. */
@RestController
@RequestMapping("/competencias")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class CompetenciaController {
	private final ServicioPlanCurricular servicio;

	public CompetenciaController(ServicioPlanCurricular servicio) {
		this.servicio = servicio;
	}

	@PutMapping("/{id}")
	public CompetenciaResumen actualizar(@PathVariable UUID id, @Valid @RequestBody SolicitudCompetencia solicitud) {
		return servicio.actualizarCompetencia(id, solicitud);
	}

	@PostMapping("/{id}/desactivar")
	public CompetenciaResumen desactivar(@PathVariable UUID id) {
		return servicio.desactivarCompetencia(id);
	}

	@PostMapping("/{id}/reactivar")
	public CompetenciaResumen reactivar(@PathVariable UUID id) {
		return servicio.reactivarCompetencia(id);
	}
}
