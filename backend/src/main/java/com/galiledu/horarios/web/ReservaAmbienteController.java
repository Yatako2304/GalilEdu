package com.galiledu.horarios.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.compartido.web.Pagina;
import com.galiledu.horarios.aplicacion.DtosReservas.OpcionesDeReserva;
import com.galiledu.horarios.aplicacion.DtosReservas.ReservaDeAmbiente;
import com.galiledu.horarios.aplicacion.DtosReservas.SolicitudEdicionReserva;
import com.galiledu.horarios.aplicacion.DtosReservas.SolicitudReserva;
import com.galiledu.horarios.aplicacion.FiltroDeReservas;
import com.galiledu.horarios.aplicacion.ServicioReservasAmbiente;

/**
 * Figma > Coordinador > «Reserva de espacios especiales» (HU-48, RF-122): historial, nueva reserva y edición. Solo el
 * Coordinador.
 */
@RestController
@RequestMapping("/horarios/reservas")
@PreAuthorize("hasRole('COORDINADOR')")
public class ReservaAmbienteController {
	private final ServicioReservasAmbiente servicio;

	public ReservaAmbienteController(ServicioReservasAmbiente servicio) {
		this.servicio = servicio;
	}

	/**
	 * GET /api/horarios/reservas?filtro=SEMANA|MES|ANIO&page=1&size=10 · las reservas activas del rango, de la más
	 * reciente a la más antigua. Sin {@code filtro}, la semana de hoy. 400 si el filtro no existe.
	 */
	@GetMapping
	public Pagina<ReservaDeAmbiente> listar(@RequestParam(required = false) FiltroDeReservas filtro,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
		return servicio.listar(filtro, page, size);
	}

	/** GET /api/horarios/reservas/opciones · espacios especiales activos y responsables para el modal. */
	@GetMapping("/opciones")
	public OpcionesDeReserva opciones() {
		return servicio.opciones();
	}

	/** GET /api/horarios/reservas/{id} · 404 si no existe o ya fue cancelada. */
	@GetMapping("/{id}")
	public ReservaDeAmbiente obtener(@PathVariable UUID id) {
		return servicio.obtener(id);
	}

	/**
	 * POST /api/horarios/reservas · crea la reserva. 400 por forma (ambiente, aforo, fecha u hora, responsable); 404 si
	 * la sección no existe; 409 por año cerrado o por cruces.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReservaDeAmbiente crear(@Valid @RequestBody SolicitudReserva solicitud, Authentication autenticacion) {
		return servicio.crear(solicitud, ServicioJwt.usuarioActual(autenticacion));
	}

	/**
	 * PUT /api/horarios/reservas/{id} · reemplaza los datos de la reserva. {@code version} es la que devolvió la
	 * lectura. 404 si no existe; 409 si la versión cambió, si ya finalizó o fue cancelada, o por cruces.
	 */
	@PutMapping("/{id}")
	public ReservaDeAmbiente reprogramar(@PathVariable UUID id, @Valid @RequestBody SolicitudEdicionReserva solicitud) {
		return servicio.reprogramar(id, solicitud);
	}

	/**
	 * DELETE /api/horarios/reservas/{id}?version= · «Eliminar»: cancela la reserva. {@code version} es opcional. 404 si
	 * no existe; 409 si ya finalizó, ya fue cancelada o la versión cambió.
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancelar(@PathVariable UUID id, @RequestParam(required = false) Long version) {
		servicio.cancelar(id, version);
	}
}
