package com.galiledu.matricula.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.matricula.aplicacion.DtosMatricula.MatriculaAdministrativa;
import com.galiledu.matricula.aplicacion.ServicioMatriculasAdministrativo;

/** Navegación administrativa con datos reales del despliegue para llegar a HU-36. */
@RestController
@RequestMapping("/matricula/administrativo")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class MatriculaAdministrativoController {
	private final ServicioMatriculasAdministrativo servicio;

	public MatriculaAdministrativoController(ServicioMatriculasAdministrativo servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/periodos/{periodoId}/matriculas")
	public List<MatriculaAdministrativa> listar(@PathVariable UUID periodoId) {
		return servicio.listar(periodoId);
	}

	@GetMapping("/matriculas/{matriculaId}")
	public MatriculaAdministrativa detalle(@PathVariable UUID matriculaId) {
		return servicio.detalle(matriculaId);
	}
}
