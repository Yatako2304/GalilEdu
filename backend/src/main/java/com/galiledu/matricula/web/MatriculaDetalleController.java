package com.galiledu.matricula.web;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoParaVer;
import com.galiledu.matricula.aplicacion.DtosMatricula.DetalleMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.SeccionesDelGrado;
import com.galiledu.matricula.aplicacion.ServicioMatriculaAdministrativa;

/** Figma > Personal administrativo > A5, B6, B7 "Detalle de matrícula" y su modal "Cambiar sección" (HU-24). */
@RestController
@RequestMapping("/matricula/matriculas")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class MatriculaDetalleController {
	private final ServicioMatriculaAdministrativa servicio;

	public MatriculaDetalleController(ServicioMatriculaAdministrativa servicio) {
		this.servicio = servicio;
	}

	public record CambioSeccion(@NotNull UUID seccionId) {
	}

	/** GET /api/matricula/matriculas/{id} */
	@GetMapping("/{id}")
	public DetalleMatricula detalle(@PathVariable UUID id) {
		return servicio.detalle(id);
	}

	/** GET /api/matricula/matriculas/{id}/secciones · secciones del grado con su ocupación. */
	@GetMapping("/{id}/secciones")
	public SeccionesDelGrado secciones(@PathVariable UUID id) {
		return servicio.secciones(id);
	}

	/** PUT /api/matricula/matriculas/{id}/seccion · confirma el cambio de sección. */
	@PutMapping("/{id}/seccion")
	public DetalleMatricula cambiarSeccion(@PathVariable UUID id,
			@RequestBody @jakarta.validation.Valid CambioSeccion cuerpo, Authentication autenticacion) {
		servicio.cambiarSeccion(id, cuerpo.seccionId(), ServicioJwt.usuarioActual(autenticacion));
		return servicio.detalle(id);
	}

	/** GET /api/matricula/matriculas/{id}/documentos/{documentoId}/archivo · el archivo, para verlo. */
	@GetMapping("/{id}/documentos/{documentoId}/archivo")
	public ResponseEntity<byte[]> archivo(@PathVariable UUID id, @PathVariable UUID documentoId) {
		ArchivoParaVer a = servicio.archivo(id, documentoId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(a.tipoMime()))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.inline().filename(a.nombre()).build().toString())
				.body(a.contenido());
	}

	public record Observacion(@NotBlank @Size(max = 500) String observacion) {
	}

	/** PUT /api/matricula/matriculas/{id}/documentos/{documentoId}/observacion · observa el documento. */
	@PutMapping("/{id}/documentos/{documentoId}/observacion")
	public DetalleMatricula observar(@PathVariable UUID id, @PathVariable UUID documentoId,
			@RequestBody @jakarta.validation.Valid Observacion cuerpo, Authentication autenticacion) {
		servicio.observarDocumento(id, documentoId, cuerpo.observacion(), ServicioJwt.usuarioActual(autenticacion));
		return servicio.detalle(id);
	}

	/** POST /api/matricula/matriculas/{id}/validar · "¿Aprobar documentación?". */
	@PostMapping("/{id}/validar")
	public DetalleMatricula validar(@PathVariable UUID id, Authentication autenticacion) {
		servicio.validar(id, ServicioJwt.usuarioActual(autenticacion));
		return servicio.detalle(id);
	}

	/** PUT /api/matricula/matriculas/{id}/documentos/{documentoId} (multipart, campo archivo) · sube o reemplaza. */
	@PutMapping(path = "/{id}/documentos/{documentoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public DetalleMatricula subir(@PathVariable UUID id, @PathVariable UUID documentoId,
			@RequestParam MultipartFile archivo, Authentication autenticacion) {
		servicio.subirDocumento(id, documentoId, MatriculaAdministrativoController.leer(archivo),
				ServicioJwt.usuarioActual(autenticacion));
		return servicio.detalle(id);
	}
}
