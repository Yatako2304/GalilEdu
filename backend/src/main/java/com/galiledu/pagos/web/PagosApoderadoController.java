package com.galiledu.pagos.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.pagos.aplicacion.DtosPagos.CompromisoAnio;
import com.galiledu.pagos.aplicacion.DtosPagos.CompromisoDetalle;
import com.galiledu.pagos.aplicacion.DtosPagos.HijoConDeuda;
import com.galiledu.pagos.aplicacion.DtosPagos.PagoRealizado;
import com.galiledu.pagos.aplicacion.ServicioDeudaApoderado;

/** Figma > Apoderado > Pensiones (HU-40): estado de deuda, cronograma de cuotas y pagos de los hijos. */
@RestController
@RequestMapping("/pagos/apoderado")
@PreAuthorize("hasRole('APODERADO')")
public class PagosApoderadoController {
	private final ServicioDeudaApoderado servicio;

	public PagosApoderadoController(ServicioDeudaApoderado servicio) {
		this.servicio = servicio;
	}

	/** GET /api/pagos/apoderado/estudiantes · RF-95. */
	@GetMapping("/estudiantes")
	public List<HijoConDeuda> hijos(Authentication autenticacion) {
		return servicio.hijos(ServicioJwt.usuarioActual(autenticacion));
	}

	/** GET /api/pagos/apoderado/estudiantes/{id}/compromisos · selector "Compromiso 2026". */
	@GetMapping("/estudiantes/{estudianteId}/compromisos")
	public List<CompromisoAnio> compromisos(@PathVariable UUID estudianteId, Authentication autenticacion) {
		return servicio.compromisos(ServicioJwt.usuarioActual(autenticacion), estudianteId);
	}

	/** GET /api/pagos/apoderado/compromisos/{id} · RF-96: cronograma con recargo por cuota. */
	@GetMapping("/compromisos/{id}")
	public CompromisoDetalle detalle(@PathVariable UUID id, Authentication autenticacion) {
		return servicio.detalle(ServicioJwt.usuarioActual(autenticacion), id);
	}

	/** GET /api/pagos/apoderado/compromisos/{id}/pagos · RF-97. */
	@GetMapping("/compromisos/{id}/pagos")
	public List<PagoRealizado> pagos(@PathVariable UUID id, Authentication autenticacion) {
		return servicio.pagos(ServicioJwt.usuarioActual(autenticacion), id);
	}
}
