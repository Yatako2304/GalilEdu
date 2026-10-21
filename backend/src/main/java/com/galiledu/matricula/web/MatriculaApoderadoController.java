package com.galiledu.matricula.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.matricula.aplicacion.DtosMatricula.EstadoMatriculaApoderado;
import com.galiledu.matricula.aplicacion.DtosMatricula.HijoApoderado;
import com.galiledu.matricula.aplicacion.ServicioEstadoMatricula;

/** Figma > Apoderado > Matrícula 2027 y Home: estado del periodo y de la matrícula (HU-27) y deuda (HU-30). */
@RestController
@RequestMapping("/matricula/apoderado")
@PreAuthorize("hasRole('APODERADO')")
public class MatriculaApoderadoController {
	private final ServicioEstadoMatricula servicio;

	public MatriculaApoderadoController(ServicioEstadoMatricula servicio) {
		this.servicio = servicio;
	}

	/** GET /api/matricula/apoderado/estudiantes · hijos del apoderado (selector de Figma). */
	@GetMapping("/estudiantes")
	public List<HijoApoderado> estudiantes(Authentication autenticacion) {
		return servicio.hijos(ServicioJwt.usuarioActual(autenticacion));
	}

	/** GET /api/matricula/apoderado/estudiantes/{id}/estado · periodo, matrícula, deuda e impedimentos. */
	@GetMapping("/estudiantes/{id}/estado")
	public EstadoMatriculaApoderado estado(@PathVariable UUID id, Authentication autenticacion) {
		return servicio.estado(ServicioJwt.usuarioActual(autenticacion), id);
	}
}
