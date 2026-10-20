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

import com.galiledu.academico.aplicacion.DtosAcademico.CursoResumen;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudCurso;
import com.galiledu.academico.aplicacion.ServicioPlanCurricular;

/** Cursos de un área y sus grados (HU-54). Se crean desde POST /areas/{id}/cursos. */
@RestController
@RequestMapping("/cursos")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class CursoController {
	private final ServicioPlanCurricular servicio;

	public CursoController(ServicioPlanCurricular servicio) {
		this.servicio = servicio;
	}

	@PutMapping("/{id}")
	public CursoResumen actualizar(@PathVariable UUID id, @Valid @RequestBody SolicitudCurso solicitud) {
		return servicio.actualizarCurso(id, solicitud);
	}

	@PostMapping("/{id}/desactivar")
	public CursoResumen desactivar(@PathVariable UUID id) {
		return servicio.desactivarCurso(id);
	}

	@PostMapping("/{id}/reactivar")
	public CursoResumen reactivar(@PathVariable UUID id) {
		return servicio.reactivarCurso(id);
	}
}
