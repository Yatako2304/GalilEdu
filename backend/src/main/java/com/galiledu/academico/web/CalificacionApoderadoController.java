package com.galiledu.academico.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.academico.aplicacion.DtosReportesNotas.HijoCalificaciones;
import com.galiledu.academico.aplicacion.DtosReportesNotas.NotasApoderado;
import com.galiledu.academico.aplicacion.ServicioNotasApoderado;
import com.galiledu.compartido.seguridad.ServicioJwt;

/** HU-69 · Figma > Apoderado > «HU-69 Consulta de notas»: notas de cada hijo matriculado. Solo lectura. */
@RestController
@RequestMapping("/calificaciones/apoderado")
@PreAuthorize("hasRole('APODERADO')")
public class CalificacionApoderadoController {
	private final ServicioNotasApoderado servicio;

	public CalificacionApoderadoController(ServicioNotasApoderado servicio) {
		this.servicio = servicio;
	}

	/** Hijos matriculados del apoderado, con sus años (selector del estudiante y del año). */
	@GetMapping("/estudiantes")
	public List<HijoCalificaciones> estudiantes(Authentication autenticacion) {
		return servicio.hijos(ServicioJwt.usuarioActual(autenticacion));
	}

	/** Notas del hijo ese año (sin {@code anio}, el más reciente). 404 si no es su hijo o no está matriculado. */
	@GetMapping("/estudiantes/{id}/notas")
	public NotasApoderado notas(@PathVariable UUID id, @RequestParam(required = false) Integer anio,
			Authentication autenticacion) {
		return servicio.notas(ServicioJwt.usuarioActual(autenticacion), id, anio);
	}

	/** Libreta del año en PDF: nota oficial de cada competencia por periodo y la final. */
	@GetMapping("/estudiantes/{id}/libreta")
	public ResponseEntity<byte[]> libreta(@PathVariable UUID id, @RequestParam(required = false) Integer anio,
			Authentication autenticacion) {
		return ReporteNotasController.pdf(servicio.libreta(ServicioJwt.usuarioActual(autenticacion), id, anio),
				"libreta-notas.pdf");
	}
}
