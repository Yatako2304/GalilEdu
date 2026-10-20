package com.galiledu.estudiantes.web;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.galiledu.compartido.web.Pagina;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.DocumentoAdjunto;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.EstudianteResumen;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.FichaEstudiante;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.InscripcionRegistrada;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.PersonaEncontrada;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.SolicitudInscripcion;
import com.galiledu.estudiantes.aplicacion.ServicioDocumentosApoderado;
import com.galiledu.estudiantes.aplicacion.ServicioEstudiantes;
import com.galiledu.estudiantes.dominio.DocumentoVinculo;
import com.galiledu.estudiantes.dominio.SituacionEstudiante;

/** Figma > Estudiantes (listado, inscripción en 5 pasos y ficha). HU-7. */
@RestController
@RequestMapping("/estudiantes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PERSONAL_ADMINISTRATIVO')")
public class EstudianteController {
	private final ServicioEstudiantes servicio;
	private final ServicioDocumentosApoderado documentos;

	public EstudianteController(ServicioEstudiantes servicio, ServicioDocumentosApoderado documentos) {
		this.servicio = servicio;
		this.documentos = documentos;
	}

	@GetMapping
	public Pagina<EstudianteResumen> listar(
			@RequestParam(name = "q", required = false) String texto,
			@RequestParam(required = false) SituacionEstudiante estado,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "8") int size) {
		return servicio.listar(texto, estado, page, size);
	}

	@GetMapping("/{id}")
	public FichaEstudiante ficha(@PathVariable UUID id) {
		return servicio.ficha(id);
	}

	@PostMapping("/inscripciones")
	@ResponseStatus(HttpStatus.CREATED)
	public InscripcionRegistrada inscribir(@Valid @RequestBody SolicitudInscripcion solicitud) {
		return servicio.inscribir(solicitud);
	}

	/** GET /api/estudiantes/personas?documento=41234567 → datos de la persona si ya existe (404 si no). */
	@GetMapping("/personas")
	public PersonaEncontrada buscarPersona(@RequestParam String documento) {
		return servicio.buscarPersona(documento);
	}

	/** POST /api/estudiantes/{id}/apoderado/documento (multipart: tipo, archivo). PDF, JPG o PNG de hasta 5 MB. */
	@PostMapping(path = "/{id}/apoderado/documento", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public DocumentoAdjunto subirDocumentoApoderado(@PathVariable UUID id, @RequestParam String tipo,
			@RequestParam MultipartFile archivo) {
		return documentos.subir(id, tipo, archivo);
	}

	@GetMapping("/{id}/apoderado/documento")
	public ResponseEntity<byte[]> descargarDocumentoApoderado(@PathVariable UUID id) {
		DocumentoVinculo d = documentos.descargar(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(d.getTipoMime()))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.inline().filename(d.getNombreArchivo(), StandardCharsets.UTF_8).build()
								.toString())
				.body(d.getContenido());
	}
}
