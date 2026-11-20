package com.galiledu.asistencia.web;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

import com.galiledu.asistencia.aplicacion.DtosAsistencia.ArchivoJustificacion;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.AsistenciaDelDia;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.SeccionAsistencia;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.SolicitudAsistencia;
import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.ArchivoReporte;
import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.ReporteAsistencia;
import com.galiledu.asistencia.aplicacion.RangoReporte.Tipo;
import com.galiledu.asistencia.aplicacion.ServicioAsistencia;
import com.galiledu.asistencia.aplicacion.ServicioReporteAsistencia;
import com.galiledu.asistencia.dominio.MotivoJustificacion;
import com.galiledu.compartido.seguridad.ServicioJwt;

/** Figma › Tutor › "HU65 · Marcar asistencia de la sección tutorada" (AS-01 a AS-06). */
@RestController
@RequestMapping("/asistencia")
@PreAuthorize("hasRole('TUTOR')")
public class AsistenciaController {
	private final ServicioAsistencia servicio;
	private final ServicioReporteAsistencia reportes;

	public AsistenciaController(ServicioAsistencia servicio, ServicioReporteAsistencia reportes) {
		this.servicio = servicio;
		this.reportes = reportes;
	}

	/**
	 * GET /api/asistencia/secciones/{id}/reporte?tipo=MES&valor=2026-11 · HU-68 (RF-146): consolidado de faltas y
	 * tardanzas por mes o por periodo ({@code tipo=PERIODO&valor=4}). Sin parámetros, el mes actual.
	 */
	@GetMapping("/secciones/{seccionId}/reporte")
	public ReporteAsistencia reporte(@PathVariable UUID seccionId, @RequestParam(required = false) Tipo tipo,
			@RequestParam(required = false) String valor, Authentication autenticacion) {
		return reportes.reporte(ServicioJwt.usuarioActual(autenticacion), seccionId, tipo, valor);
	}

	/** GET /api/asistencia/secciones/{id}/reporte/pdf · HU-68: el mismo reporte en PDF. */
	@GetMapping(value = "/secciones/{seccionId}/reporte/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> reportePdf(@PathVariable UUID seccionId, @RequestParam(required = false) Tipo tipo,
			@RequestParam(required = false) String valor, Authentication autenticacion) {
		ArchivoReporte archivo = reportes.pdf(ServicioJwt.usuarioActual(autenticacion), seccionId, tipo, valor);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
				.body(archivo.contenido());
	}

	/** GET /api/asistencia/secciones · secciones de las que el usuario es tutor o cotutor (año activo). */
	@GetMapping("/secciones")
	public List<SeccionAsistencia> secciones(Authentication autenticacion) {
		return servicio.seccionesDelTutor(ServicioJwt.usuarioActual(autenticacion));
	}

	/** GET /api/asistencia/secciones/{id}/dias/{fecha} · RF-143: la asistencia del día (AS-01, AS-05). */
	@GetMapping("/secciones/{seccionId}/dias/{fecha}")
	public AsistenciaDelDia consultar(@PathVariable UUID seccionId, @PathVariable LocalDate fecha,
			Authentication autenticacion) {
		return servicio.consultar(ServicioJwt.usuarioActual(autenticacion), seccionId, fecha);
	}

	/**
	 * POST /api/asistencia/secciones/{id}/dias/{fecha}/justificaciones · HU-67 (RF-145): justifica la falta o tardanza
	 * de un estudiante (multipart: matriculaId, motivo, detalle y archivo opcional).
	 */
	@PostMapping(value = "/secciones/{seccionId}/dias/{fecha}/justificaciones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public AsistenciaDelDia justificar(@PathVariable UUID seccionId, @PathVariable LocalDate fecha,
			@RequestParam UUID matriculaId, @RequestParam(required = false) MotivoJustificacion motivo,
			@RequestParam(required = false) String detalle, @RequestParam(required = false) MultipartFile archivo,
			Authentication autenticacion) {
		return servicio.justificar(ServicioJwt.usuarioActual(autenticacion), seccionId, fecha, matriculaId, motivo,
				detalle, archivo);
	}

	/** GET /api/asistencia/secciones/{id}/dias/{fecha}/justificaciones/{matriculaId}/sustento · HU-67. */
	@GetMapping("/secciones/{seccionId}/dias/{fecha}/justificaciones/{matriculaId}/sustento")
	public ResponseEntity<byte[]> sustento(@PathVariable UUID seccionId, @PathVariable LocalDate fecha,
			@PathVariable UUID matriculaId, Authentication autenticacion) {
		ArchivoJustificacion archivo = servicio.sustento(ServicioJwt.usuarioActual(autenticacion), seccionId, fecha,
				matriculaId);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(archivo.tipo()))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(archivo.nombre(), StandardCharsets.UTF_8).build()
								.toString())
				.body(archivo.contenido());
	}

	/** PUT /api/asistencia/secciones/{id}/dias/{fecha} · HU-65: registra (AS-04) o corrige (AS-06) la de hoy. */
	@PutMapping("/secciones/{seccionId}/dias/{fecha}")
	public AsistenciaDelDia guardar(@PathVariable UUID seccionId, @PathVariable LocalDate fecha,
			@RequestBody SolicitudAsistencia solicitud, Authentication autenticacion) {
		return servicio.guardar(ServicioJwt.usuarioActual(autenticacion), seccionId, fecha, solicitud);
	}
}
