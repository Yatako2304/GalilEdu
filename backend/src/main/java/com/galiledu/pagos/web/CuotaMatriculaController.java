package com.galiledu.pagos.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.pagos.aplicacion.DtosPagos.CuotaMatriculaDetalle;
import com.galiledu.pagos.aplicacion.ServicioCuotaMatricula;

/** Figma > Personal Administrativo > Matrícula › Detalle B6 "Pago de matrícula" y B7 (HU-36). */
@RestController
@RequestMapping("/pagos/matriculas/{matriculaId}/cuota-matricula")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class CuotaMatriculaController {
	private final ServicioCuotaMatricula servicio;

	public CuotaMatriculaController(ServicioCuotaMatricula servicio) {
		this.servicio = servicio;
	}

	/** POST /api/pagos/matriculas/{matriculaId}/cuota-matricula · genera la cuota (RF-78, RF-79). */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CuotaMatriculaDetalle generar(@PathVariable UUID matriculaId, Authentication autenticacion) {
		return servicio.generar(matriculaId, ServicioJwt.usuarioActual(autenticacion));
	}

	/** GET /api/pagos/matriculas/{matriculaId}/cuota-matricula · monto, vencimiento, estado y pago (RF-80). */
	@GetMapping
	public CuotaMatriculaDetalle consultar(@PathVariable UUID matriculaId) {
		return servicio.consultar(matriculaId);
	}
}
