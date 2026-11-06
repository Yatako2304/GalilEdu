package com.galiledu.academico.web;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.galiledu.academico.aplicacion.DtosCalificaciones.ArchivoEvidencia;
import com.galiledu.academico.aplicacion.DtosCalificaciones.CompetenciaOpcion;
import com.galiledu.academico.aplicacion.DtosCalificaciones.CursoDocente;
import com.galiledu.academico.aplicacion.DtosCalificaciones.EvidenciaResumen;
import com.galiledu.academico.aplicacion.DtosCalificaciones.FilaCalificacion;
import com.galiledu.academico.aplicacion.DtosCalificaciones.ItemEvaluacion;
import com.galiledu.academico.aplicacion.DtosCalificaciones.NotaFinalCompetencia;
import com.galiledu.academico.aplicacion.DtosCalificaciones.ReporteCalificaciones;
import com.galiledu.academico.aplicacion.DtosCalificaciones.SolicitudCalificaciones;
import com.galiledu.academico.aplicacion.DtosCalificaciones.SolicitudItem;
import com.galiledu.academico.aplicacion.ServicioCalificaciones;
import com.galiledu.academico.aplicacion.ServicioNotaCompetencia;
import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.compartido.web.ExcepcionNegocio;

/** API del flujo de Calificaciones del usuario Docente (HU-57, HU-59 y HU-62). */
@RestController
@RequestMapping("/calificaciones/docente")
@PreAuthorize("hasRole('DOCENTE')")
public class CalificacionController {
	private final ServicioCalificaciones servicio;
	private final ServicioNotaCompetencia notaCompetencia;

	public CalificacionController(ServicioCalificaciones servicio, ServicioNotaCompetencia notaCompetencia) {
		this.servicio = servicio;
		this.notaCompetencia = notaCompetencia;
	}

	@GetMapping("/cursos")
	public List<CursoDocente> cursos(Authentication autenticacion) {
		return servicio.cursos(ServicioJwt.usuarioActual(autenticacion));
	}

	@GetMapping("/cursos/{planCursoId}/secciones/{seccionId}/competencias")
	public List<CompetenciaOpcion> competencias(@PathVariable UUID planCursoId, @PathVariable UUID seccionId,
			Authentication autenticacion) {
		return servicio.competencias(ServicioJwt.usuarioActual(autenticacion), planCursoId, seccionId);
	}

	/** Nota final de la competencia en la sección, entre todos los cursos del grado que la evalúan. */
	@GetMapping("/cursos/{planCursoId}/secciones/{seccionId}/competencias/{competenciaId}/nota-final")
	public NotaFinalCompetencia notaFinal(@PathVariable UUID planCursoId, @PathVariable UUID seccionId,
			@PathVariable UUID competenciaId, Authentication autenticacion) {
		return notaCompetencia.notaFinal(ServicioJwt.usuarioActual(autenticacion), planCursoId, seccionId,
				competenciaId);
	}

	@GetMapping("/cursos/{planCursoId}/secciones/{seccionId}/periodos/{periodoId}/items")
	public List<ItemEvaluacion> items(@PathVariable UUID planCursoId, @PathVariable UUID seccionId,
			@PathVariable UUID periodoId, Authentication autenticacion) {
		return servicio.items(ServicioJwt.usuarioActual(autenticacion), planCursoId, seccionId, periodoId);
	}

	@PostMapping("/cursos/{planCursoId}/secciones/{seccionId}/periodos/{periodoId}/items")
	@ResponseStatus(HttpStatus.CREATED)
	public ItemEvaluacion crearItem(@PathVariable UUID planCursoId, @PathVariable UUID seccionId,
			@PathVariable UUID periodoId, @Valid @RequestBody SolicitudItem solicitud, Authentication autenticacion) {
		return servicio.crearItem(ServicioJwt.usuarioActual(autenticacion), planCursoId, seccionId, periodoId,
				solicitud);
	}

	@PutMapping("/items/{itemId}")
	public ItemEvaluacion editarItem(@PathVariable UUID itemId, @Valid @RequestBody SolicitudItem solicitud,
			Authentication autenticacion) {
		return servicio.editarItem(ServicioJwt.usuarioActual(autenticacion), itemId, solicitud);
	}

	@DeleteMapping("/items/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminarItem(@PathVariable UUID itemId, Authentication autenticacion) {
		servicio.eliminarItem(ServicioJwt.usuarioActual(autenticacion), itemId);
	}

	@GetMapping("/items/{itemId}/calificaciones")
	public List<FilaCalificacion> calificaciones(@PathVariable UUID itemId, Authentication autenticacion) {
		return servicio.calificaciones(ServicioJwt.usuarioActual(autenticacion), itemId);
	}

	@PutMapping("/items/{itemId}/calificaciones")
	public List<FilaCalificacion> guardarCalificaciones(@PathVariable UUID itemId,
			@Valid @RequestBody SolicitudCalificaciones solicitud, Authentication autenticacion) {
		return servicio.guardarCalificaciones(ServicioJwt.usuarioActual(autenticacion), itemId, solicitud);
	}

	@PostMapping(path = "/items/{itemId}/calificaciones/{matriculaId}/evidencia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public EvidenciaResumen guardarEvidencia(@PathVariable UUID itemId, @PathVariable UUID matriculaId,
			@RequestParam MultipartFile archivo, Authentication autenticacion) {
		return servicio.guardarEvidencia(ServicioJwt.usuarioActual(autenticacion), itemId, matriculaId, archivo);
	}

	@GetMapping("/items/{itemId}/calificaciones/{matriculaId}/evidencia")
	public ResponseEntity<byte[]> descargarEvidencia(@PathVariable UUID itemId, @PathVariable UUID matriculaId,
			Authentication autenticacion) {
		ArchivoEvidencia archivo = servicio.descargarEvidencia(ServicioJwt.usuarioActual(autenticacion), itemId,
				matriculaId);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(archivo.tipoMime()))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
						.filename(archivo.nombreArchivo(), StandardCharsets.UTF_8).build().toString())
				.body(archivo.contenido());
	}

	@DeleteMapping("/items/{itemId}/calificaciones/{matriculaId}/evidencia")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminarEvidencia(@PathVariable UUID itemId, @PathVariable UUID matriculaId,
			Authentication autenticacion) {
		servicio.eliminarEvidencia(ServicioJwt.usuarioActual(autenticacion), itemId, matriculaId);
	}

	@GetMapping("/reportes")
	public ReporteCalificaciones reporte(@RequestParam String tipo, @RequestParam UUID planCursoId,
			@RequestParam UUID seccionId, @RequestParam UUID periodoId,
			@RequestParam(required = false) UUID objetivoId,
			@RequestParam(required = false) String itemIds,
			@RequestParam(defaultValue = "true") boolean incluirPromedio, Authentication autenticacion) {
		return servicio.reporte(ServicioJwt.usuarioActual(autenticacion), tipo, planCursoId, seccionId, periodoId,
				objetivoId, ids(itemIds), incluirPromedio);
	}

	@GetMapping(value = "/reportes/exportar", produces = "text/csv")
	public ResponseEntity<byte[]> exportar(@RequestParam String tipo, @RequestParam UUID planCursoId,
			@RequestParam UUID seccionId, @RequestParam UUID periodoId,
			@RequestParam(required = false) UUID objetivoId,
			@RequestParam(required = false) String itemIds,
			@RequestParam(defaultValue = "true") boolean incluirPromedio, Authentication autenticacion) {
		byte[] contenido = servicio.exportarCsv(ServicioJwt.usuarioActual(autenticacion), tipo, planCursoId,
				seccionId, periodoId, objetivoId, ids(itemIds), incluirPromedio);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("consolidado-notas.csv").build().toString())
				.body(contenido);
	}

	private static List<UUID> ids(String valor) {
		if (valor == null || valor.isBlank()) {
			return List.of();
		}
		try {
			return Arrays.stream(valor.split(",")).map(String::strip).filter(id -> !id.isEmpty()).map(UUID::fromString)
					.toList();
		} catch (IllegalArgumentException excepcion) {
			throw ExcepcionNegocio.invalido("Los ítems seleccionados no son válidos.");
		}
	}
}
