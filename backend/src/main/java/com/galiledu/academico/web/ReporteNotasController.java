package com.galiledu.academico.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.academico.aplicacion.DtosReportesNotas.AnioReporte;
import com.galiledu.academico.aplicacion.DtosReportesNotas.NotasSeccion;
import com.galiledu.academico.aplicacion.DtosReportesNotas.ReporteGrado;
import com.galiledu.academico.aplicacion.DtosReportesNotas.ReporteInstitucional;
import com.galiledu.academico.aplicacion.ServicioNotasApoderado;
import com.galiledu.academico.aplicacion.ServicioReportesNotas;

/**
 * Reportes de notas: por curso y grado (HU-63, Coordinador) e institucional (HU-64, Coordinador y Administrador). Sin
 * {@code periodoId}, la nota final del año.
 */
@RestController
@RequestMapping("/calificaciones/reportes")
@PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
public class ReporteNotasController {
	private final ServicioReportesNotas servicio;
	private final ServicioNotasApoderado apoderado;

	public ReporteNotasController(ServicioReportesNotas servicio, ServicioNotasApoderado apoderado) {
		this.servicio = servicio;
		this.apoderado = apoderado;
	}

	/** Años ACTIVO o CERRADO con sus periodos y los grados y cursos de su plan (filtros de los reportes). */
	@GetMapping("/anios")
	public List<AnioReporte> anios() {
		return servicio.anios();
	}

	/** HU-63. Sin planCursoId, la nota oficial de cada competencia; con él, lo que aporta ese curso. */
	@GetMapping("/grado")
	@PreAuthorize("hasRole('COORDINADOR')")
	public ReporteGrado grado(@RequestParam UUID anioEscolarId, @RequestParam String grado,
			@RequestParam(required = false) UUID planCursoId, @RequestParam(required = false) UUID periodoId) {
		return servicio.reporteGrado(anioEscolarId, grado, planCursoId, periodoId);
	}

	@GetMapping(value = "/grado/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	@PreAuthorize("hasRole('COORDINADOR')")
	public ResponseEntity<byte[]> gradoPdf(@RequestParam UUID anioEscolarId, @RequestParam String grado,
			@RequestParam(required = false) UUID planCursoId, @RequestParam(required = false) UUID periodoId) {
		return pdf(servicio.pdfGrado(anioEscolarId, grado, planCursoId, periodoId), "reporte-notas-grado.pdf");
	}

	/** Detalle de una sección: la nota oficial de cada competencia por estudiante. Solo el Coordinador. */
	@GetMapping("/secciones/{seccionId}/notas")
	@PreAuthorize("hasRole('COORDINADOR')")
	public NotasSeccion notasSeccion(@PathVariable UUID seccionId) {
		return servicio.notasSeccion(seccionId);
	}

	/** Libreta de un estudiante (la misma del apoderado) para el Coordinador. */
	@GetMapping(value = "/matriculas/{matriculaId}/libreta", produces = MediaType.APPLICATION_PDF_VALUE)
	@PreAuthorize("hasRole('COORDINADOR')")
	public ResponseEntity<byte[]> libreta(@PathVariable UUID matriculaId) {
		return pdf(apoderado.libretaMatricula(matriculaId), "libreta-notas.pdf");
	}

	/** HU-64. */
	@GetMapping("/institucional")
	public ReporteInstitucional institucional(@RequestParam UUID anioEscolarId,
			@RequestParam(required = false) UUID periodoId) {
		return servicio.reporteInstitucional(anioEscolarId, periodoId);
	}

	@GetMapping(value = "/institucional/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> institucionalPdf(@RequestParam UUID anioEscolarId,
			@RequestParam(required = false) UUID periodoId) {
		return pdf(servicio.pdfInstitucional(anioEscolarId, periodoId), "reporte-institucional-notas.pdf");
	}

	static ResponseEntity<byte[]> pdf(byte[] contenido, String nombre) {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(nombre).build().toString())
				.body(contenido);
	}
}
