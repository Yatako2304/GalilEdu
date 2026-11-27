package com.galiledu.academico.web;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.academico.aplicacion.DtosAmbientes.AmbienteResumen;
import com.galiledu.academico.aplicacion.DtosAmbientes.ResumenAmbientes;
import com.galiledu.academico.aplicacion.DtosAmbientes.SolicitudAmbiente;
import com.galiledu.academico.aplicacion.DtosAmbientes.SolicitudEdicionAmbiente;
import com.galiledu.academico.aplicacion.ServicioAmbientes;
import com.galiledu.academico.dominio.EstadoAmbiente;
import com.galiledu.academico.dominio.TipoAmbiente;

/**
 * Figma > Personal Administrativo > "CI-26 · Ambientes físicos (HU-43)" (nodo 857:10215) y modal "Nuevo ambiente
 * físico" (nodo 857:11500). El Administrador tiene los mismos permisos que el Personal administrativo (decisión del
 * 26/11: el plan de pruebas pone los espacios en el rol Administrador).
 */
@RestController
@RequestMapping("/ambientes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PERSONAL_ADMINISTRATIVO')")
public class AmbienteController {
	private final ServicioAmbientes servicio;

	public AmbienteController(ServicioAmbientes servicio) {
		this.servicio = servicio;
	}

	/**
	 * CI-26 (857:10215), tarjetas: "Ambientes activos · de N registrados", "Aulas regulares", "Espacios especiales" y
	 * "Aforo total". Solo cuentan los activos y no dependen de los filtros de la lista.
	 */
	@GetMapping("/resumen")
	public ResumenAmbientes resumen() {
		return servicio.resumen();
	}

	/**
	 * CI-26 (857:10215), tabla: lista simple, sin paginar, por nombre. Filtros opcionales por tipo y estado; {@code q}
	 * busca en el nombre. El código viaja en la respuesta, pero la pantalla no lo muestra ni lo pide.
	 */
	@GetMapping
	public List<AmbienteResumen> listar(@RequestParam(required = false) TipoAmbiente tipo,
			@RequestParam(required = false) EstadoAmbiente estado, @RequestParam(required = false) String q) {
		return servicio.listar(tipo, estado, q);
	}

	/**
	 * Modal "Nuevo ambiente físico" (857:11500), botón "Registrar ambiente". A diferencia de Figma, el código no se
	 * ingresa: lo genera el sistema (A-001, E-001).
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AmbienteResumen crear(@Valid @RequestBody SolicitudAmbiente solicitud) {
		return servicio.crear(solicitud);
	}

	/** CI-26 (857:10215), editar una fila: nombre, aforo y recursos. El código y el tipo no cambian. */
	@PutMapping("/{id}")
	public AmbienteResumen actualizar(@PathVariable UUID id, @Valid @RequestBody SolicitudEdicionAmbiente solicitud) {
		return servicio.actualizar(id, solicitud);
	}

	/**
	 * CI-26 (857:10215), estado de la fila. Se permite aunque el ambiente esté en uso; si ya estaba inactivo, no hace
	 * nada.
	 */
	@PostMapping("/{id}/desactivar")
	public AmbienteResumen desactivar(@PathVariable UUID id) {
		return servicio.desactivar(id);
	}

	/** CI-26 (857:10215), estado de la fila. Si ya estaba activo, no hace nada. */
	@PostMapping("/{id}/reactivar")
	public AmbienteResumen reactivar(@PathVariable UUID id) {
		return servicio.reactivar(id);
	}
}
