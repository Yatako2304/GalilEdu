package com.galiledu.horarios.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.horarios.aplicacion.DtosHorarioApoderado.HijoConHorario;
import com.galiledu.horarios.aplicacion.DtosHorarioApoderado.HorarioDelEstudiante;
import com.galiledu.horarios.aplicacion.ServicioHorarioApoderado;

/** Figma > Apoderado > Horario (HU-51): el horario de clases de la sección de cada hijo. Solo lectura. */
@RestController
@RequestMapping("/horarios/apoderado")
@PreAuthorize("hasRole('APODERADO')")
public class HorarioApoderadoController {
	private final ServicioHorarioApoderado servicio;

	public HorarioApoderadoController(ServicioHorarioApoderado servicio) {
		this.servicio = servicio;
	}

	/** GET /api/horarios/apoderado/estudiantes · hijos matriculados del apoderado (selector de Figma). */
	@GetMapping("/estudiantes")
	public List<HijoConHorario> estudiantes(Authentication autenticacion) {
		return servicio.hijos(ServicioJwt.usuarioActual(autenticacion));
	}

	/**
	 * GET /api/horarios/apoderado/estudiantes/{id}/horario?anio=2026 · horario de la sección del hijo ese año (sin
	 * {@code anio}, el más reciente). 404 si no es su hijo o no está matriculado ese año.
	 */
	@GetMapping("/estudiantes/{id}/horario")
	public HorarioDelEstudiante horario(@PathVariable UUID id, @RequestParam(required = false) Integer anio,
			Authentication autenticacion) {
		return servicio.horario(ServicioJwt.usuarioActual(autenticacion), id, anio);
	}
}
