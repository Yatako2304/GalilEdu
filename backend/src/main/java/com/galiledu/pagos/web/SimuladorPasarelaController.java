package com.galiledu.pagos.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.OrdenDetalle;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.SolicitudSimulacion;
import com.galiledu.pagos.aplicacion.ServicioPagoEnLinea;
import com.galiledu.pagos.infraestructura.PasarelaSimulada;

/**
 * Solo existe con la pasarela simulada: hace lo que haría la página de pago real (aprobar o rechazar) y avisa al
 * sistema como lo haría la notificación de la pasarela.
 */
@RestController
@RequestMapping("/pagos/pago-en-linea/simulador")
@PreAuthorize("hasRole('APODERADO')")
@ConditionalOnProperty(name = "galiledu.pagos.pasarela", havingValue = "simulada", matchIfMissing = true)
public class SimuladorPasarelaController {
	private final ServicioPagoEnLinea servicio;
	private final PasarelaSimulada pasarela;

	public SimuladorPasarelaController(ServicioPagoEnLinea servicio, PasarelaSimulada pasarela) {
		this.servicio = servicio;
		this.pasarela = pasarela;
	}

	/** POST /api/pagos/pago-en-linea/simulador/{codigo} · {"aprobar": true|false} */
	@PostMapping("/{codigo}")
	public OrdenDetalle decidir(@PathVariable String codigo, @RequestBody SolicitudSimulacion solicitud,
			Authentication autenticacion) {
		var usuario = ServicioJwt.usuarioActual(autenticacion);
		OrdenDetalle orden = servicio.consultar(usuario, codigo);
		pasarela.decidir("SIM-" + orden.codigo(), solicitud.aprobar());
		servicio.notificar("SIM-" + orden.codigo());
		return servicio.consultar(usuario, codigo);
	}
}
