package com.galiledu.pagos.web;

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
import com.galiledu.pagos.aplicacion.DtosPagos.CompromisoAnio;
import com.galiledu.pagos.aplicacion.DtosPagos.CompromisoDetalle;
import com.galiledu.pagos.aplicacion.DtosPagos.CronogramaGenerado;
import com.galiledu.pagos.aplicacion.DtosPagos.EstadoCuentaGeneral;
import com.galiledu.pagos.aplicacion.DtosPagos.RetiroRegistrado;
import com.galiledu.pagos.aplicacion.DtosPagos.SolicitudRetiro;
import com.galiledu.pagos.aplicacion.ServicioCompromisos;
import com.galiledu.pagos.aplicacion.ServicioCronograma;

/** Figma > Personal Administrativo > Pensiones > "P8 · Compromiso — Cronograma de cuotas" (HU-37). */
@RestController
@RequestMapping("/pagos")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class CompromisoController {
	private final ServicioCompromisos servicio;
	private final ServicioCronograma cronograma;

	public CompromisoController(ServicioCompromisos servicio, ServicioCronograma cronograma) {
		this.servicio = servicio;
		this.cronograma = cronograma;
	}

	/** GET /api/pagos/estado-cuenta?anio= · Figma "P2 · Estado de cuenta — Todos los alumnos" (HU-37). */
	@GetMapping("/estado-cuenta")
	public EstadoCuentaGeneral estadoCuenta(@RequestParam(required = false) Integer anio) {
		return servicio.estadoCuenta(anio);
	}

	/** GET /api/pagos/estudiantes/{estudianteId}/compromisos */
	@GetMapping("/estudiantes/{estudianteId}/compromisos")
	public List<CompromisoAnio> compromisosDelEstudiante(@PathVariable UUID estudianteId) {
		return servicio.listarPorEstudiante(estudianteId);
	}

	/** GET /api/pagos/compromisos/{id} */
	@GetMapping("/compromisos/{id}")
	public CompromisoDetalle detalle(@PathVariable UUID id) {
		return servicio.detalle(id);
	}

	/** POST /api/pagos/matriculas/{matriculaId}/cronograma · genera las pensiones (RF-81, RF-82, RF-83). */
	@PostMapping("/matriculas/{matriculaId}/cronograma")
	@ResponseStatus(HttpStatus.CREATED)
	public CronogramaGenerado generarCronograma(@PathVariable UUID matriculaId, Authentication autenticacion) {
		return cronograma.generarPensiones(matriculaId, ServicioJwt.usuarioActual(autenticacion));
	}

	/** POST /api/pagos/compromisos/{id}/retiro · anula las pensiones futuras (RF-85). */
	@PostMapping("/compromisos/{id}/retiro")
	public RetiroRegistrado registrarRetiro(@PathVariable UUID id, @Valid @RequestBody SolicitudRetiro solicitud) {
		return cronograma.anularPorRetiro(id, solicitud.fechaRetiro());
	}
}
