package com.galiledu.configuracion.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.configuracion.aplicacion.DtosConfiguracion.AnioEscolarDetalle;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.AnioEscolarResumen;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudAnioEscolar;
import com.galiledu.configuracion.aplicacion.ServicioAnioEscolar;

/**
 * Figma > Personal Administrativo > Configuraciones > Año escolar: "CI-01", "CI-03" y "Modal - Nuevo año escolar"
 * (HU-74).
 */
@RestController
@RequestMapping("/configuracion/anios-escolares")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class AnioEscolarController {
	private final ServicioAnioEscolar servicio;

	public AnioEscolarController(ServicioAnioEscolar servicio) {
		this.servicio = servicio;
	}

	/** GET /api/configuracion/anios-escolares */
	@GetMapping
	public List<AnioEscolarResumen> listar() {
		return servicio.listar();
	}

	/** GET /api/configuracion/anios-escolares/{id} · el año con sus periodos. */
	@GetMapping("/{id}")
	public AnioEscolarDetalle detalle(@PathVariable UUID id) {
		return servicio.detalle(id);
	}

	/** POST /api/configuracion/anios-escolares · crea el año PLANIFICADO y genera sus periodos. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AnioEscolarDetalle crear(@Valid @RequestBody SolicitudAnioEscolar solicitud) {
		return servicio.crear(solicitud);
	}
}
