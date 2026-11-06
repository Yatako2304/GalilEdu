package com.galiledu.matricula.web;

import java.io.IOException;
import java.io.UncheckedIOException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.compartido.web.Pagina;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoParaVer;
import com.galiledu.matricula.aplicacion.DtosMatricula.EstudianteSinMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.FilaMatriculaPeriodo;
import com.galiledu.matricula.aplicacion.DtosMatricula.MatriculaCreada;
import com.galiledu.matricula.aplicacion.DtosMatricula.PeriodoMatriculas;
import com.galiledu.matricula.aplicacion.DtosMatricula.PreparacionMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.SeccionesDelGrado;
import com.galiledu.matricula.aplicacion.ServicioMatriculasPeriodo;

/** Figma > Personal administrativo > A3 "Matrículas del periodo" y A6 "Nueva matrícula" (HU-24, HU-71, HU-72). */
@RestController
@RequestMapping("/matricula/periodos")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class MatriculaAdministrativoController {
	private final ServicioMatriculasPeriodo servicio;

	public MatriculaAdministrativoController(ServicioMatriculasPeriodo servicio) {
		this.servicio = servicio;
	}

	/** GET /api/matricula/periodos/{periodoId} · aviso del periodo (abierto o cerrado) e indicadores. */
	@GetMapping("/{periodoId}")
	public PeriodoMatriculas periodo(@PathVariable UUID periodoId) {
		return servicio.periodo(periodoId);
	}

	/**
	 * GET /api/matricula/periodos/{periodoId}/matriculas?texto=&grado=&estado=&orden=&direccion=&page=&size= · tabla:
	 * solo quienes ya tienen matrícula. {@code orden}: estudiante, grado, seccion o estado; {@code direccion}: asc o
	 * desc.
	 */
	@GetMapping("/{periodoId}/matriculas")
	public Pagina<FilaMatriculaPeriodo> matriculas(@PathVariable UUID periodoId,
			@RequestParam(required = false) String texto,
			@RequestParam(required = false) String grado,
			@RequestParam(required = false) String estado,
			@RequestParam(defaultValue = "estudiante") String orden,
			@RequestParam(defaultValue = "asc") String direccion,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return servicio.listar(periodoId, texto, grado, estado, orden, "desc".equalsIgnoreCase(direccion), page, size);
	}

	/** GET /api/matricula/periodos/{periodoId}/estudiantes?texto= · buscador de "Nueva matrícula" (hasta 8). */
	@GetMapping("/{periodoId}/estudiantes")
	public List<EstudianteSinMatricula> estudiantes(@PathVariable UUID periodoId,
			@RequestParam(required = false) String texto) {
		return servicio.buscarEstudiantes(periodoId, texto);
	}

	/** GET /api/matricula/periodos/{periodoId}/estudiantes/{id}/preparacion · datos, sección y documentos. */
	@GetMapping("/{periodoId}/estudiantes/{estudianteId}/preparacion")
	public PreparacionMatricula preparacion(@PathVariable UUID periodoId, @PathVariable UUID estudianteId) {
		return servicio.preparar(periodoId, estudianteId);
	}

	/**
	 * POST /api/matricula/periodos/{periodoId}/matriculas (multipart) · "Guardar" de Nueva matrícula. Campo
	 * {@code estudianteId} y, por cada documento ya subido, un archivo cuyo nombre de campo es el id del documento
	 * requerido. Solo con el periodo abierto.
	 */
	@PostMapping(path = "/{periodoId}/matriculas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public MatriculaCreada crear(@PathVariable UUID periodoId, @RequestParam UUID estudianteId,
			@RequestParam(required = false) UUID seccionId,
			@RequestParam(required = false) MultiValueMap<String, MultipartFile> archivos,
			Authentication autenticacion) {
		Map<UUID, ArchivoMatricula> leidos = new LinkedHashMap<>();
		if (archivos != null) {
			archivos.forEach((campo, lista) -> {
				if (!lista.isEmpty() && lista.getFirst() instanceof MultipartFile f && !f.isEmpty()) {
					leidos.put(idDelCampo(campo), leer(lista.getFirst()));
				}
			});
		}
		return servicio.crear(periodoId, estudianteId, seccionId, leidos, ServicioJwt.usuarioActual(autenticacion));
	}

	/** GET /api/matricula/periodos/{periodoId}/estudiantes/{id}/secciones · secciones del grado para elegir. */
	@GetMapping("/{periodoId}/estudiantes/{estudianteId}/secciones")
	public SeccionesDelGrado secciones(@PathVariable UUID periodoId, @PathVariable UUID estudianteId) {
		return servicio.seccionesDelAlumno(periodoId, estudianteId);
	}

	/** GET /api/matricula/periodos/{periodoId}/documentos/{documentoId}/plantilla · descarga la plantilla. */
	@GetMapping("/{periodoId}/documentos/{documentoId}/plantilla")
	public ResponseEntity<byte[]> plantilla(@PathVariable UUID periodoId, @PathVariable UUID documentoId) {
		ArchivoParaVer a = servicio.plantilla(periodoId, documentoId);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(a.tipoMime()))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(a.nombre()).build().toString())
				.body(a.contenido());
	}

	static UUID idDelCampo(String campo) {
		try {
			return UUID.fromString(campo);
		} catch (IllegalArgumentException e) {
			throw ExcepcionNegocio.invalido("El campo \"" + campo + "\" no corresponde a un documento.");
		}
	}

	static ArchivoMatricula leer(MultipartFile archivo) {
		String original = archivo.getOriginalFilename() == null ? "documento" : archivo.getOriginalFilename();
		// Sin rutas ni caracteres raros en el nombre que se guarda.
		String nombre = original.replaceAll("^.*[\\\\/]", "").replaceAll("[^\\p{L}\\p{N}._ -]", "_").strip();
		if (nombre.isEmpty()) {
			nombre = "documento";
		}
		try {
			return new ArchivoMatricula(nombre.substring(0, Math.min(nombre.length(), 200)),
					archivo.getContentType(), archivo.getBytes());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
