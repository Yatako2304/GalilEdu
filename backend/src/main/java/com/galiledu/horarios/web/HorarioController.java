package com.galiledu.horarios.web;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.horarios.aplicacion.DtosHorarios.HorarioDeSeccion;
import com.galiledu.horarios.aplicacion.DtosHorarios.OpcionesDeHorario;
import com.galiledu.horarios.aplicacion.ServicioHorarios;

/** Horario semanal de una sección (HU-45), para el Coordinador. Fase 1: solo lectura. */
@RestController
@RequestMapping("/horarios")
@PreAuthorize("hasRole('COORDINADOR')")
public class HorarioController {
	private final ServicioHorarios servicio;

	public HorarioController(ServicioHorarios servicio) {
		this.servicio = servicio;
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
}
