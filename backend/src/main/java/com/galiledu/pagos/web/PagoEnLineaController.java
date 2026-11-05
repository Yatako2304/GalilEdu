package com.galiledu.pagos.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.CuotasPorPagar;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.OrdenCreada;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.OrdenDetalle;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.SolicitudOrden;
import com.galiledu.pagos.aplicacion.ServicioPagoEnLinea;

/** Figma > Apoderado > "HU-41 Pago en línea de cuotas pendientes y HU-40": barra de selección y pago. */
@RestController
@RequestMapping("/pagos/pago-en-linea")
@PreAuthorize("hasRole('APODERADO')")
public class PagoEnLineaController {
	private final ServicioPagoEnLinea servicio;

	public PagoEnLineaController(ServicioPagoEnLinea servicio) {
		this.servicio = servicio;
	}

	/** GET /api/pagos/pago-en-linea/cuotas · cuotas con saldo de cada hijo, con capital y recargo. */
	@GetMapping("/cuotas")
	public CuotasPorPagar cuotas(Authentication autenticacion) {
		return servicio.cuotasPorPagar(ServicioJwt.usuarioActual(autenticacion));
	}

	/** POST /api/pagos/pago-en-linea/ordenes · crea la orden y devuelve la página de pago de la pasarela. */
	@PostMapping("/ordenes")
	@ResponseStatus(HttpStatus.CREATED)
	public OrdenCreada crearOrden(@Valid @RequestBody SolicitudOrden solicitud, Authentication autenticacion) {
		return servicio.crearOrden(ServicioJwt.usuarioActual(autenticacion), solicitud.cuotaIds());
	}

	/** GET /api/pagos/pago-en-linea/ordenes/{codigo} · resultado del pago al volver de la pasarela. */
	@GetMapping("/ordenes/{codigo}")
	public OrdenDetalle orden(@PathVariable String codigo, Authentication autenticacion) {
		return servicio.consultar(ServicioJwt.usuarioActual(autenticacion), codigo);
	}

	/** POST /api/pagos/pago-en-linea/ordenes/{codigo}/cancelar · el apoderado volvió sin pagar. */
	@PostMapping("/ordenes/{codigo}/cancelar")
	public OrdenDetalle cancelar(@PathVariable String codigo, Authentication autenticacion) {
		return servicio.cancelar(ServicioJwt.usuarioActual(autenticacion), codigo);
	}
}
