package com.galiledu.configuracion.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.DetalleEstructuraHoraria;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.EstructuraGuardada;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.EstructurasHorariasDelAnio;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.SolicitudEdicionEstructuraHoraria;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.SolicitudEstructuraHoraria;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.VistaPreviaJornada;
import com.galiledu.configuracion.aplicacion.ServicioEstructurasHorarias;

/**
 * Figma > Personal Administrativo > Configuraciones > Año escolar: "CI-12 · Estructura horaria" y "Modal - Nueva
 * estructura horaria" (HU-44).
 */
@RestController
@RequestMapping("/configuracion")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class EstructuraHorariaController {
	private final ServicioEstructurasHorarias servicio;

	public EstructuraHorariaController(ServicioEstructurasHorarias servicio) {
		this.servicio = servicio;
	}

	/**
	 * GET /api/configuracion/anios-escolares/{anioId}/estructuras-horarias
	 * CI-12, lista "Estructuras del año" y los indicadores de cada tarjeta. Incluye {@code anioModificable} para que
	 * el frontend deshabilite "+ Nueva", Editar y Eliminar cuando el año no está planificado. 404 si el año no existe.
	 */
	@GetMapping("/anios-escolares/{anioId}/estructuras-horarias")
	public EstructurasHorariasDelAnio listarPorAnioEscolar(@PathVariable UUID anioId) {
		return servicio.listarPorAnioEscolar(anioId);
	}

	/**
	 * GET /api/configuracion/estructuras-horarias/{id}
	 * CI-12, "Jornada semanal": los datos de la estructura y su grilla calculada (bloques y recesos en orden).
	 */
	@GetMapping("/estructuras-horarias/{id}")
	public DetalleEstructuraHoraria detalleEstructuraHoraria(@PathVariable UUID id) {
		return servicio.detalleEstructuraHoraria(id);
	}

	/**
	 * POST /api/configuracion/anios-escolares/{anioId}/estructuras-horarias
	 * Modal "Nueva estructura horaria", botón "Crear estructura". Solo con el año planificado. 409 si el código ya
	 * existe en el año. La respuesta trae las advertencias para el usuario (por ejemplo, que cambia la planificación).
	 */
	@PostMapping("/anios-escolares/{anioId}/estructuras-horarias")
	@ResponseStatus(HttpStatus.CREATED)
	public EstructuraGuardada crearEstructuraHoraria(@PathVariable UUID anioId,
			@Valid @RequestBody SolicitudEstructuraHoraria solicitud) {
		return servicio.crearEstructuraHoraria(anioId, solicitud);
	}

	/**
	 * POST /api/configuracion/anios-escolares/{anioId}/estructuras-horarias/vista-previa
	 * Modal "Nueva estructura horaria", "Vista previa de la jornada". No guarda: calcula la jornada y devuelve las
	 * validaciones, para no repetir las reglas en el frontend. No mira el estado del año ni los planes.
	 */
	@PostMapping("/anios-escolares/{anioId}/estructuras-horarias/vista-previa")
	public VistaPreviaJornada vistaPreviaEstructuraHoraria(@PathVariable UUID anioId,
			@Valid @RequestBody SolicitudEstructuraHoraria solicitud) {
		return servicio.vistaPreviaEstructuraHoraria(anioId, solicitud);
	}

	/**
	 * PUT /api/configuracion/estructuras-horarias/{id}
	 * Figma todavía no dibuja el botón Editar de "CI-12": usa los mismos campos del "Modal - Nueva estructura
	 * horaria", sin el nivel. Edita la estructura y regenera sus recesos. El nivel no se cambia: para eso se elimina
	 * y se crea de nuevo. Solo con el año planificado.
	 */
	@PutMapping("/estructuras-horarias/{id}")
	public EstructuraGuardada editarEstructuraHoraria(@PathVariable UUID id,
			@Valid @RequestBody SolicitudEdicionEstructuraHoraria solicitud) {
		return servicio.editarEstructuraHoraria(id, solicitud);
	}

	/**
	 * DELETE /api/configuracion/estructuras-horarias/{id}
	 * Figma todavía no dibuja el botón Eliminar de "CI-12" (la estructura que se elimina es la seleccionada en la
	 * lista "Estructuras del año"). Solo con el año planificado. 409 si es la principal y quedan otras del nivel, o
	 * si es la única del nivel y hay cursos planificados.
	 */
	@DeleteMapping("/estructuras-horarias/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminarEstructuraHoraria(@PathVariable UUID id) {
		servicio.eliminarEstructuraHoraria(id);
	}
}
