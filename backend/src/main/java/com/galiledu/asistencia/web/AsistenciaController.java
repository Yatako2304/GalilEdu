package com.galiledu.asistencia.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.asistencia.aplicacion.DtosAsistencia.AsistenciaDelDia;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.SeccionAsistencia;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.SolicitudAsistencia;
import com.galiledu.asistencia.aplicacion.ServicioAsistencia;
import com.galiledu.compartido.seguridad.ServicioJwt;

/** Figma › Tutor › "HU65 · Marcar asistencia de la sección tutorada" (AS-01 a AS-06). */
@RestController
@RequestMapping("/asistencia")
@PreAuthorize("hasRole('TUTOR')")
public class AsistenciaController {
	private final ServicioAsistencia servicio;

	public AsistenciaController(ServicioAsistencia servicio) {
		this.servicio = servicio;
	}

	/** GET /api/asistencia/secciones · secciones de las que el usuario es tutor o cotutor (año activo). */
	@GetMapping("/secciones")
	public List<SeccionAsistencia> secciones(Authentication autenticacion) {
		return servicio.seccionesDelTutor(ServicioJwt.usuarioActual(autenticacion));
	}

	/** GET /api/asistencia/secciones/{id}/dias/{fecha} · RF-143: la asistencia del día (AS-01, AS-05). */
	@GetMapping("/secciones/{seccionId}/dias/{fecha}")
	public AsistenciaDelDia consultar(@PathVariable UUID seccionId, @PathVariable LocalDate fecha,
			Authentication autenticacion) {
		return servicio.consultar(ServicioJwt.usuarioActual(autenticacion), seccionId, fecha);
	}

	/** PUT /api/asistencia/secciones/{id}/dias/{fecha} · HU-65: registra (AS-04) o corrige (AS-06) la de hoy. */
	@PutMapping("/secciones/{seccionId}/dias/{fecha}")
	public AsistenciaDelDia guardar(@PathVariable UUID seccionId, @PathVariable LocalDate fecha,
			@RequestBody SolicitudAsistencia solicitud, Authentication autenticacion) {
		return servicio.guardar(ServicioJwt.usuarioActual(autenticacion), seccionId, fecha, solicitud);
	}
}
