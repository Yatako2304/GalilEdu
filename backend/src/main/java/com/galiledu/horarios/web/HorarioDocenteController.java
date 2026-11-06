package com.galiledu.horarios.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.horarios.aplicacion.DtosHorarioDocente.AnioDelDocente;
import com.galiledu.horarios.aplicacion.DtosHorarioDocente.HorarioDocente;
import com.galiledu.horarios.aplicacion.ServicioHorarioDocente;

/**
 * Figma > Docente / Tutor > Horario (HU-49): el horario de dictado del usuario que inició sesión, con solo las clases
 * donde es titular. Solo lectura. El docente nunca viene en la URL: es siempre el usuario autenticado.
 */
@RestController
@RequestMapping("/horarios/docente")
@PreAuthorize("hasAnyRole('DOCENTE', 'TUTOR')")
public class HorarioDocenteController {
	private final ServicioHorarioDocente servicio;

	public HorarioDocenteController(ServicioHorarioDocente servicio) {
		this.servicio = servicio;
	}

	/** GET /api/horarios/docente/anios · años en los que tiene clases, del más reciente al más antiguo. */
	@GetMapping("/anios")
	public List<AnioDelDocente> anios(Authentication autenticacion) {
		return servicio.aniosDelDocente(ServicioJwt.usuarioActual(autenticacion));
	}

	/**
	 * GET /api/horarios/docente/horario?anioId= · sus clases separadas por estructura horaria. Sin {@code anioId}, el
	 * año ACTIVO si tiene clases en él y, si no, el más reciente con clases. 404 si el año no existe.
	 */
	@GetMapping("/horario")
	public HorarioDocente horario(@RequestParam(required = false) UUID anioId, Authentication autenticacion) {
		return servicio.horarioDelDocente(ServicioJwt.usuarioActual(autenticacion), anioId);
	}
}
