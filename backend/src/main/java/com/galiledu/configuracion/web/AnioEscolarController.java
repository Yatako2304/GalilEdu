package com.galiledu.configuracion.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.configuracion.aplicacion.DtosConfiguracion.AnioEscolarActual;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.AnioEscolarDetalle;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.AnioEscolarResumen;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.RequisitosAnio;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudAnioEscolar;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudEdicionAnio;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudRegimen;
import com.galiledu.configuracion.aplicacion.ServicioAnioEscolar;

/**
 * Figma > Personal Administrativo > Configuraciones > Año escolar: "CI-01", "CI-03", "CI-06", "Modal - Nuevo año
 * escolar" y "Modal - Cambiar régimen de periodos" (HU-74).
 * <p>
 * PENDIENTE(grupo): activar y cerrar el año existen en el servicio pero no se exponen; Figma no tiene botón
 * "Activar".
 */
@RestController
@RequestMapping("/configuracion/anios-escolares")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class AnioEscolarController {
	private final ServicioAnioEscolar servicio;

	public AnioEscolarController(ServicioAnioEscolar servicio) {
		this.servicio = servicio;
	}

	/** GET /api/configuracion/anios-escolares · tabla de años de "CI-01". */
	@GetMapping
	public List<AnioEscolarResumen> listar() {
		return servicio.listar();
	}

	/** GET /api/configuracion/anios-escolares/actual · card del año en curso de "CI-01". 404 si no hay año activo. */
	@GetMapping("/actual")
	public AnioEscolarActual actual() {
		return servicio.actual();
	}

	/**
	 * GET /api/configuracion/anios-escolares/{id} · el año con sus periodos: "CI-03" (activo) y "CI-06" (planificado).
	 */
	@GetMapping("/{id}")
	public AnioEscolarDetalle detalle(@PathVariable UUID id) {
		return servicio.detalle(id);
	}

	/** GET /api/configuracion/anios-escolares/{id}/requisitos · card "Antes de activar el año" de "CI-06". */
	@GetMapping("/{id}/requisitos")
	public RequisitosAnio requisitos(@PathVariable UUID id) {
		return servicio.requisitos(id);
	}

	/**
	 * POST /api/configuracion/anios-escolares · "Modal - Nuevo año escolar": crea el año PLANIFICADO y sus periodos.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AnioEscolarDetalle crear(@Valid @RequestBody SolicitudAnioEscolar solicitud) {
		return servicio.crear(solicitud);
	}

	/**
	 * PUT /api/configuracion/anios-escolares/{id} · botón "Editar" de "CI-06": cambia las fechas y regenera los
	 * periodos.
	 */
	@PutMapping("/{id}")
	public AnioEscolarDetalle editar(@PathVariable UUID id, @Valid @RequestBody SolicitudEdicionAnio solicitud) {
		return servicio.editar(id, solicitud);
	}

	/** PUT /api/configuracion/anios-escolares/{id}/regimen · "Modal - Cambiar régimen de periodos" de "CI-06". */
	@PutMapping("/{id}/regimen")
	public AnioEscolarDetalle cambiarRegimen(@PathVariable UUID id, @Valid @RequestBody SolicitudRegimen solicitud) {
		return servicio.cambiarRegimen(id, solicitud);
	}
}
