package com.galiledu.pagos.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.pagos.aplicacion.DtosPagoEnCaja.PagoCajaRegistrado;
import com.galiledu.pagos.aplicacion.DtosPagoEnCaja.SolicitudPagoCaja;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.CuotaPorPagar;
import com.galiledu.pagos.aplicacion.ServicioPagoEnCaja;

/** Pago en caja: botón «Registrar pago» del cronograma del compromiso (P8). */
@RestController
@RequestMapping("/pagos/compromisos/{id}")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class PagoEnCajaController {
	private final ServicioPagoEnCaja servicio;

	public PagoEnCajaController(ServicioPagoEnCaja servicio) {
		this.servicio = servicio;
	}

	/** GET /api/pagos/compromisos/{id}/cuotas-por-pagar?fecha= · cuotas con saldo y su recargo a esa fecha. */
	@GetMapping("/cuotas-por-pagar")
	public List<CuotaPorPagar> cuotasPorPagar(@PathVariable UUID id, @RequestParam(required = false) LocalDate fecha) {
		return servicio.cuotasPorPagar(id, fecha);
	}

	/** POST /api/pagos/compromisos/{id}/pagos · registra el pago en caja y emite su constancia. */
	@PostMapping("/pagos")
	@ResponseStatus(HttpStatus.CREATED)
	public PagoCajaRegistrado registrar(@PathVariable UUID id, @Valid @RequestBody SolicitudPagoCaja solicitud,
			Authentication autenticacion) {
		return servicio.registrar(ServicioJwt.usuarioActual(autenticacion), id, solicitud);
	}
}
