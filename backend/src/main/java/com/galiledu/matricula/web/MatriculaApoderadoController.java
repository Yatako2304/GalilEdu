package com.galiledu.matricula.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoParaVer;
import com.galiledu.matricula.aplicacion.DtosMatricula.DocumentosApoderado;
import com.galiledu.matricula.aplicacion.DtosMatricula.EstadoMatriculaApoderado;
import com.galiledu.matricula.aplicacion.DtosMatricula.HijoApoderado;
import com.galiledu.matricula.aplicacion.ServicioEstadoMatricula;
import com.galiledu.matricula.aplicacion.ServicioMatriculaApoderado;

/**
 * Figma > Apoderado > Matrícula 2027 y Home: estado del periodo y de la matrícula (HU-27), deuda (HU-30) y documentos
 * (HU-28 descarga de plantillas, HU-29 carga y envío a validación).
 */
@RestController
@RequestMapping("/matricula/apoderado")
@PreAuthorize("hasRole('APODERADO')")
public class MatriculaApoderadoController {
	private final ServicioEstadoMatricula servicio;
	private final ServicioMatriculaApoderado documentos;

	public MatriculaApoderadoController(ServicioEstadoMatricula servicio, ServicioMatriculaApoderado documentos) {
		this.servicio = servicio;
		this.documentos = documentos;
	}

	/** GET /api/matricula/apoderado/estudiantes · hijos del apoderado (selector de Figma). */
	@GetMapping("/estudiantes")
	public List<HijoApoderado> estudiantes(Authentication autenticacion) {
		return servicio.hijos(ServicioJwt.usuarioActual(autenticacion));
	}

	/** GET /api/matricula/apoderado/estudiantes/{id}/estado · periodo, matrícula, deuda e impedimentos. */
	@GetMapping("/estudiantes/{id}/estado")
	public EstadoMatriculaApoderado estado(@PathVariable UUID id, Authentication autenticacion) {
		return servicio.estado(ServicioJwt.usuarioActual(autenticacion), id);
	}

	/** GET /api/matricula/apoderado/estudiantes/{id}/documentos · documentos con su plantilla y lo ya subido. */
	@GetMapping("/estudiantes/{id}/documentos")
	public DocumentosApoderado documentos(@PathVariable UUID id, Authentication autenticacion) {
		return documentos.documentos(ServicioJwt.usuarioActual(autenticacion), id);
	}

	/** GET .../documentos/{documentoId}/plantilla · descarga la plantilla para firmarla. */
	@GetMapping("/estudiantes/{id}/documentos/{documentoId}/plantilla")
	public ResponseEntity<byte[]> plantilla(@PathVariable UUID id, @PathVariable UUID documentoId,
			Authentication autenticacion) {
		ArchivoParaVer a = documentos.plantilla(ServicioJwt.usuarioActual(autenticacion), id, documentoId);
		return archivo(a, ContentDisposition.attachment());
	}

	/** GET .../documentos/{documentoId}/archivo · el archivo ya subido, para verlo. */
	@GetMapping("/estudiantes/{id}/documentos/{documentoId}/archivo")
	public ResponseEntity<byte[]> archivo(@PathVariable UUID id, @PathVariable UUID documentoId,
			Authentication autenticacion) {
		ArchivoParaVer a = documentos.archivo(ServicioJwt.usuarioActual(autenticacion), id, documentoId);
		return archivo(a, ContentDisposition.inline());
	}

	/** PUT .../documentos/{documentoId} (multipart, campo archivo) · sube o reemplaza el documento firmado. */
	@PutMapping(path = "/estudiantes/{id}/documentos/{documentoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public DocumentosApoderado subir(@PathVariable UUID id, @PathVariable UUID documentoId,
			@RequestParam MultipartFile archivo, Authentication autenticacion) {
		return documentos.subir(ServicioJwt.usuarioActual(autenticacion), id, documentoId,
				MatriculaAdministrativoController.leer(archivo));
	}

	/** POST .../enviar-validacion · "Enviar a validación": SECCION_RESERVADA pasa a EN_VALIDACION. */
	@PostMapping("/estudiantes/{id}/enviar-validacion")
	public EstadoMatriculaApoderado enviarAValidacion(@PathVariable UUID id, Authentication autenticacion) {
		return documentos.enviarAValidacion(ServicioJwt.usuarioActual(autenticacion), id);
	}

	/**
	 * POST .../matricula (multipart, un campo por documento: su id con el archivo) · «Enviar a validación» cuando el
	 * hijo aún no tiene matrícula: la crea con la sección que asigna el sistema y la deja EN_VALIDACION.
	 */
	@PostMapping(path = "/estudiantes/{id}/matricula", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public EstadoMatriculaApoderado iniciar(@PathVariable UUID id,
			@RequestParam(required = false) MultiValueMap<String, MultipartFile> archivos,
			Authentication autenticacion) {
		Map<UUID, ArchivoMatricula> leidos = new LinkedHashMap<>();
		if (archivos != null) {
			archivos.forEach((campo, lista) -> {
				if (!lista.isEmpty() && lista.getFirst() instanceof MultipartFile f && !f.isEmpty()) {
					leidos.put(MatriculaAdministrativoController.idDelCampo(campo),
							MatriculaAdministrativoController.leer(f));
				}
			});
		}
		return documentos.iniciar(ServicioJwt.usuarioActual(autenticacion), id, leidos);
	}

	private static ResponseEntity<byte[]> archivo(ArchivoParaVer a, ContentDisposition.Builder tipo) {
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(a.tipoMime()))
				.header(HttpHeaders.CONTENT_DISPOSITION, tipo.filename(a.nombre()).build().toString())
				.body(a.contenido());
	}
}
