package com.galiledu.academico.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudTablaConversion;
import com.galiledu.academico.aplicacion.DtosAcademico.TablaConversionDetalle;
import com.galiledu.academico.aplicacion.ServicioEscalaCalificacion;
import com.galiledu.compartido.seguridad.ServicioJwt;

/** Figma > Personal Administrativo > CI-23 · Escala de calificación (HU-55). */
@RestController
@RequestMapping("/tablas-conversion")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class TablaConversionController {
	private final ServicioEscalaCalificacion servicio;

	public TablaConversionController(ServicioEscalaCalificacion servicio) {
		this.servicio = servicio;
	}

	/** Todas las versiones (programadas, vigente y anteriores), de la más nueva a la más antigua. */
	@GetMapping
	public List<TablaConversionDetalle> listar() {
		return servicio.listar();
	}

	@GetMapping("/vigente")
	public TablaConversionDetalle vigente() {
		return servicio.vigente();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TablaConversionDetalle crear(@Valid @RequestBody SolicitudTablaConversion solicitud,
			Authentication autenticacion) {
		return servicio.crear(solicitud, ServicioJwt.usuarioActual(autenticacion));
	}
}
