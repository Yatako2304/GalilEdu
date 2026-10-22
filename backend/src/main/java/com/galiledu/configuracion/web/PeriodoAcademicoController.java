package com.galiledu.configuracion.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.configuracion.aplicacion.DtosConfiguracion.PeriodoDetalle;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudPeriodo;
import com.galiledu.configuracion.aplicacion.ServicioAnioEscolar;

/**
 * Figma > Personal Administrativo > Configuraciones > Año escolar: "Modal - Editar periodo" (HU-74). No hay abrir ni
 * cerrar periodo: su estado se calcula con las fechas.
 */
@RestController
@RequestMapping("/configuracion/periodos-academicos")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class PeriodoAcademicoController {
	private final ServicioAnioEscolar servicio;

	public PeriodoAcademicoController(ServicioAnioEscolar servicio) {
		this.servicio = servicio;
	}

	/**
	 * PUT /api/configuracion/periodos-academicos/{id} · "Modal - Editar periodo" de "CI-03" y "CI-06": nombre, fechas y
	 * fecha límite de notas. Para ampliar el plazo de notas de un periodo terminado, solo cambia la fecha límite.
	 */
	@PutMapping("/{id}")
	public PeriodoDetalle editar(@PathVariable UUID id, @Valid @RequestBody SolicitudPeriodo solicitud) {
		return servicio.editarPeriodo(id, solicitud);
	}
}
