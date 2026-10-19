package com.galiledu.estudiantes.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.web.Pagina;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.EstudianteResumen;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.FichaEstudiante;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.InscripcionRegistrada;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.PersonaEncontrada;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.SolicitudInscripcion;
import com.galiledu.estudiantes.aplicacion.ServicioEstudiantes;
import com.galiledu.estudiantes.dominio.EstadoEstudiante;

import jakarta.validation.Valid;

/** Figma > Estudiantes (listado, inscripción en 5 pasos y ficha). HU-7. */
@RestController
@RequestMapping("/estudiantes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PERSONAL_ADMINISTRATIVO')")
public class EstudianteController {
	private final ServicioEstudiantes servicio;

	public EstudianteController(ServicioEstudiantes servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public Pagina<EstudianteResumen> listar(
		@RequestParam(name = "q", required = false) String texto,
		@RequestParam(required = false) EstadoEstudiante estado,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "8") int size) {
		return servicio.listar(texto, estado, page, size);
	}

	@GetMapping("/{id}")
	public FichaEstudiante ficha(@PathVariable UUID id) {
		return servicio.ficha(id);
	}

	@PostMapping("/inscripciones")
	@ResponseStatus(HttpStatus.CREATED)
	public InscripcionRegistrada inscribir(@Valid @RequestBody SolicitudInscripcion solicitud) {
		return servicio.inscribir(solicitud);
	}

	/** GET /api/estudiantes/personas?documento=41234567 → datos de la persona si ya existe (404 si no). */
	@GetMapping("/personas")
	public PersonaEncontrada buscarPersona(@RequestParam String documento) {
		return servicio.buscarPersona(documento);
	}
}
