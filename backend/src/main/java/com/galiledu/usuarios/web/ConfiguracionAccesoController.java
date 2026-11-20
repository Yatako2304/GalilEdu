package com.galiledu.usuarios.web;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.usuarios.aplicacion.Dtos.ConfiguracionAccesoDto;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudConfiguracionAcceso;
import com.galiledu.usuarios.aplicacion.ServicioAccesoGoogle;

/** HU-2 · Administrador › Seguridad › Configuración de acceso. Solo el administrador. */
@RestController
@RequestMapping("/seguridad/configuracion-acceso")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ConfiguracionAccesoController {
	private final ServicioAccesoGoogle servicio;

	public ConfiguracionAccesoController(ServicioAccesoGoogle servicio) {
		this.servicio = servicio;
	}

	/** GET /api/seguridad/configuracion-acceso → { googleHabilitado, googleConfigurado }. */
	@GetMapping
	public ConfiguracionAccesoDto ver() {
		return servicio.configuracion();
	}

	/** PUT /api/seguridad/configuracion-acceso { googleHabilitado }. 409 si se activa sin las llaves de Google. */
	@PutMapping
	public ConfiguracionAccesoDto cambiar(@Valid @RequestBody SolicitudConfiguracionAcceso solicitud,
			Authentication autenticacion) {
		return servicio.cambiarGoogle(solicitud.googleHabilitado(), ServicioJwt.usuarioActual(autenticacion));
	}
}
