package com.galiledu.configuracion.web;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudTarifario;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.TarifarioVista;
import com.galiledu.configuracion.aplicacion.ServicioTarifario;

/**
 * Figma > Personal Administrativo > Pensiones > Configuración: "P19 · Configuración › Tarifario y calendario" y el
 * modal
 * "¿Confirmar el tarifario?" (HU-34). No hay Guardar: solo Confirmar tarifario.
 */
@RestController
@RequestMapping("/configuracion/tarifarios")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class TarifarioController {
	private final ServicioTarifario servicio;

	public TarifarioController(ServicioTarifario servicio) {
		this.servicio = servicio;
	}

	/**
	 * GET /api/configuracion/tarifarios?anioEscolarId= · P19 del año: el tarifario guardado o una propuesta
	 * ({@code existe = false}). 404 si el año no existe.
	 */
	@GetMapping
	public TarifarioVista ver(@RequestParam UUID anioEscolarId) {
		return servicio.ver(anioEscolarId);
	}

	/**
	 * POST /api/configuracion/tarifarios/{anioEscolarId}/confirmar · botón "Confirmar tarifario": guarda los montos y
	 * la
	 * morosidad, regenera el calendario y deja el tarifario CONFIRMADO. 409 si es de solo lectura; 400 si un dato no es
	 * válido.
	 */
	@PostMapping("/{anioEscolarId}/confirmar")
	public TarifarioVista confirmar(@PathVariable UUID anioEscolarId, @RequestBody SolicitudTarifario solicitud) {
		return servicio.confirmar(anioEscolarId, solicitud);
	}
}
