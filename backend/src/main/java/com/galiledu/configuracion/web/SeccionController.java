package com.galiledu.configuracion.web;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosSecciones.DocenteElegible;
import com.galiledu.configuracion.aplicacion.DtosSecciones.GradoConSecciones;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SeccionResumen;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SolicitudCapacidad;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SolicitudSeccion;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SolicitudTutores;
import com.galiledu.configuracion.aplicacion.ServicioSecciones;

/**
 * Figma > Personal Administrativo > Configuraciones > Año escolar: "CI-09 · Secciones por grado y tutores", "Modal -
 * Nueva sección" y "Modal - Asignar tutor" (HU-75).
 */
@RestController
@RequestMapping("/configuracion")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class SeccionController {
	private final ServicioSecciones servicio;

	public SeccionController(ServicioSecciones servicio) {
		this.servicio = servicio;
	}

	/**
	 * GET /api/configuracion/anios-escolares/{anioId}/secciones?nivel=
	 * Grados del año con sus secciones, también los que no tienen. Sin nivel, los 11.
	 */
	@GetMapping("/anios-escolares/{anioId}/secciones")
	public List<GradoConSecciones> listarGradosConSecciones(@PathVariable UUID anioId,
			@RequestParam(required = false) String nivel) {
		return servicio.listarGradosConSecciones(anioId, nivel);
	}

	/**
	 * POST /api/configuracion/anios-escolares/{anioId}/secciones
	 * Crea una sección ACTIVA y sin tutores. 409 si el nombre ya existe en el grado.
	 */
	@PostMapping("/anios-escolares/{anioId}/secciones")
	@ResponseStatus(HttpStatus.CREATED)
	public SeccionResumen crear(@PathVariable UUID anioId, @Valid @RequestBody SolicitudSeccion solicitud) {
		return servicio.crear(anioId, solicitud);
	}

	/**
	 * PUT /api/configuracion/secciones/{id}
	 * Cambia solo la capacidad, que no puede ser menor que los alumnos que ocupan vacante.
	 */
	@PutMapping("/secciones/{id}")
	public SeccionResumen cambiarCapacidad(@PathVariable UUID id, @Valid @RequestBody SolicitudCapacidad solicitud) {
		return servicio.cambiarCapacidad(id, solicitud);
	}

	/**
	 * PUT /api/configuracion/secciones/{id}/tutores
	 * Asigna el tutor y, opcional, el cotutor. Un docente es tutor de una sola sección activa por año.
	 */
	@PutMapping("/secciones/{id}/tutores")
	public SeccionResumen asignarTutores(@PathVariable UUID id, @Valid @RequestBody SolicitudTutores solicitud) {
		return servicio.asignarTutores(id, solicitud);
	}

	/**
	 * DELETE /api/configuracion/secciones/{id}
	 * Elimina la sección solo si nunca tuvo matrículas ni cambios de sección; si no, 409 (conviene inactivarla).
	 */
	@DeleteMapping("/secciones/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@PathVariable UUID id) {
		servicio.eliminar(id);
	}

	/**
	 * POST /api/configuracion/secciones/{id}/inactivar
	 * Deja de recibir matrículas; conserva a sus alumnos y su historial.
	 */
	@PostMapping("/secciones/{id}/inactivar")
	public SeccionResumen inactivar(@PathVariable UUID id) {
		return servicio.inactivar(id);
	}

	/**
	 * POST /api/configuracion/secciones/{id}/reactivar
	 * Vuelve a ACTIVA. 409 si su tutor ya lo es de otra sección activa del año.
	 */
	@PostMapping("/secciones/{id}/reactivar")
	public SeccionResumen reactivar(@PathVariable UUID id) {
		return servicio.reactivar(id);
	}

	/**
	 * GET /api/configuracion/secciones/tutores-disponibles?anioEscolarId=
	 * Docentes y tutores activos, marcando a quienes ya son tutores ese año. Alimenta los selectores del modal.
	 */
	@GetMapping("/secciones/tutores-disponibles")
	public List<DocenteElegible> listarDocentesElegibles(@RequestParam(required = false) UUID anioEscolarId) {
		// El parámetro se exige aquí y no con required=true porque el manejador de errores compartido respondería 500.
		if (anioEscolarId == null) {
			throw ExcepcionNegocio.invalido("Indica el año escolar.");
		}
		return servicio.listarDocentesElegibles(anioEscolarId);
	}
}
