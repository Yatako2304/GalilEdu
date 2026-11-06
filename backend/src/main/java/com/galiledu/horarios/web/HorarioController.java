package com.galiledu.horarios.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.horarios.aplicacion.DtosHorarios.AnioParaHorarios;
import com.galiledu.horarios.aplicacion.DtosHorarios.GradoParaHorarios;
import com.galiledu.horarios.aplicacion.DtosHorarios.HorarioDeSeccion;
import com.galiledu.horarios.aplicacion.DtosHorarios.OpcionesDeHorario;
import com.galiledu.horarios.aplicacion.DtosHorarios.SolicitudHorario;
import com.galiledu.horarios.aplicacion.ServicioHorarios;

/** Horario semanal de una sección (HU-45), para el Coordinador. Lectura y escritura. */
@RestController
@RequestMapping("/horarios")
@PreAuthorize("hasRole('COORDINADOR')")
public class HorarioController {
	private final ServicioHorarios servicio;

	public HorarioController(ServicioHorarios servicio) {
		this.servicio = servicio;
	}

	/**
	 * GET /api/horarios/anios-escolares
	 * Selector de año de la pantalla de asignación: id, año y estado. Configuración los ofrece solo al Personal
	 * administrativo; el Coordinador los lee aquí.
	 */
	@GetMapping("/anios-escolares")
	public List<AnioParaHorarios> aniosEscolares() {
		return servicio.aniosEscolares();
	}

	/**
	 * GET /api/horarios/anios-escolares/{anioId}/secciones
	 * Selectores de grado y sección: los grados del catálogo con las secciones que tienen en el año. 404 si el año no
	 * existe.
	 */
	@GetMapping("/anios-escolares/{anioId}/secciones")
	public List<GradoParaHorarios> seccionesDelAnio(@PathVariable UUID anioId) {
		return servicio.gradosDelAnio(anioId);
	}

	/**
	 * GET /api/horarios/secciones/{seccionId}
	 * La matriz de la sección para verla o editarla. No crea nada: sin horario guardado devuelve {@code version = null}
	 * y la jornada de la estructura principal del nivel. 404 si la sección no existe; 409 si no hay horario ni
	 * estructura principal.
	 */
	@GetMapping("/secciones/{seccionId}")
	public HorarioDeSeccion horarioDeSeccion(@PathVariable UUID seccionId) {
		return servicio.horarioDeSeccion(seccionId);
	}

	/**
	 * GET /api/horarios/secciones/{seccionId}/opciones
	 * Cursos del plan y ambientes activos para el modal "Asignar bloque horario". 404 si la sección no existe.
	 */
	@GetMapping("/secciones/{seccionId}/opciones")
	public OpcionesDeHorario opcionesDeSeccion(@PathVariable UUID seccionId) {
		return servicio.opcionesDeSeccion(seccionId);
	}

	/**
	 * PUT /api/horarios/secciones/{seccionId}
	 * Guarda la matriz COMPLETA de la sección y responde con el mismo cuerpo que el GET. {@code version} es la que
	 * devolvió el GET (null si no había horario). 404 si la sección no existe; 409 si el año está cerrado, la versión
	 * cambió, falta docente, se exceden las horas semanales o hay cruces; 400 si una celda no es válida.
	 */
	@PutMapping("/secciones/{seccionId}")
	public HorarioDeSeccion guardarHorario(@PathVariable UUID seccionId,
			@Valid @RequestBody SolicitudHorario solicitud) {
		return servicio.guardarHorario(seccionId, solicitud);
	}
}
