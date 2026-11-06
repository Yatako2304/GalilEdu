package com.galiledu.horarios.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.horarios.aplicacion.CruceDetectado;
import com.galiledu.horarios.aplicacion.ServicioCrucesHorario;
import com.galiledu.horarios.aplicacion.SolicitudCruces;

/** Aviso de cruces antes de guardar el horario de una sección (HU-46), para el Coordinador. */
@RestController
@RequestMapping("/horarios")
@PreAuthorize("hasRole('COORDINADOR')")
public class CrucesHorarioController {
	private final ServicioCrucesHorario servicio;

	public CrucesHorarioController(ServicioCrucesHorario servicio) {
		this.servicio = servicio;
	}

	/**
	 * POST /api/horarios/secciones/{seccionId}/cruces
	 * Cruces de docente y de ambiente de las celdas enviadas con lo guardado en las demás secciones del año; lista
	 * vacía si no hay. No guarda nada. Ignora las celdas que no puede resolver (curso sin docente, bloque fuera de la
	 * jornada): el PUT da ese error. 404 si la sección no existe; 400 si el cuerpo no es válido.
	 */
	@PostMapping("/secciones/{seccionId}/cruces")
	public List<CruceDetectado> cruces(@PathVariable UUID seccionId, @Valid @RequestBody SolicitudCruces solicitud) {
		return servicio.buscarCruces(seccionId, solicitud);
	}
}
