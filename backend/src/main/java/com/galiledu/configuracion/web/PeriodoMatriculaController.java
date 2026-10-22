package com.galiledu.configuracion.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.compartido.web.Pagina;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.AnioDisponible;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.DocumentoVista;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.FilaPeriodoMatricula;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.FormularioPeriodoMatricula;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.PeriodoVigente;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.SolicitudPeriodoMatricula;
import com.galiledu.configuracion.aplicacion.ServicioPeriodoMatricula;
import com.galiledu.configuracion.dominio.EstadoVisiblePeriodoMatricula;
import com.galiledu.configuracion.infraestructura.ConsultasPeriodoMatricula.PlantillaGuardada;

/**
 * Figma > Personal Administrativo > Matrícula > Configuración de matrícula: pantalla 13 "Periodos de matrícula"
 * (listado), "Nuevo periodo de matrícula" (formulario), la vista de solo lectura y el modal "Configuración · Matrícula"
 * (HU-23). Primero se guarda el periodo y después se suben las plantillas con los ids de documento que devuelve.
 */
@RestController
@RequestMapping("/configuracion/periodos-matricula")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class PeriodoMatriculaController {
	private final ServicioPeriodoMatricula servicio;

	public PeriodoMatriculaController(ServicioPeriodoMatricula servicio) {
		this.servicio = servicio;
	}

	/**
	 * GET /api/configuracion/periodos-matricula?anio=&estado=&page=&size= · listado de la pantalla 13: tabla con los
	 * filtros "Año" y "Estado" (PROGRAMADO, ABIERTO o CERRADO), del periodo más reciente al más antiguo.
	 */
	@GetMapping
	public Pagina<FilaPeriodoMatricula> listar(
			@RequestParam(required = false) Integer anio,
			@RequestParam(required = false) EstadoVisiblePeriodoMatricula estado,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "8") int size) {
		return servicio.listar(anio, estado, page, size);
	}

	/**
	 * GET /api/configuracion/periodos-matricula/vigente · tarjeta "Vigente" del listado: el periodo abierto ahora o, si
	 * no hay, el próximo programado. 404 si no hay ninguno.
	 */
	@GetMapping("/vigente")
	public PeriodoVigente vigente() {
		return servicio.vigente()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No hay un periodo de matrícula vigente."));
	}

	/**
	 * GET /api/configuracion/periodos-matricula/anios-disponibles · selector "Año escolar" de "Nuevo periodo de
	 * matrícula": los años planificados o activos que aún no tienen periodo.
	 */
	@GetMapping("/anios-disponibles")
	public List<AnioDisponible> aniosDisponibles() {
		return servicio.aniosDisponibles();
	}

	/**
	 * GET /api/configuracion/periodos-matricula/nuevo?anioEscolarId= · formulario "Nuevo periodo de matrícula" vacío
	 * para el año elegido: secciones con sus vacantes, tarjeta de monto y resumen. 409 si el año no admite periodo.
	 */
	@GetMapping("/nuevo")
	public FormularioPeriodoMatricula nuevo(@RequestParam(required = false) UUID anioEscolarId) {
		// El parámetro se exige aquí y no con required=true porque el manejador de errores compartido respondería 500.
		if (anioEscolarId == null) {
			throw ExcepcionNegocio.invalido("Indica el año escolar.");
		}
		return servicio.formularioNuevo(anioEscolarId);
	}

	/**
	 * GET /api/configuracion/periodos-matricula/{id} · formulario de un periodo guardado: editable si está programado y
	 * de solo lectura (con el resumen del modal "Configuración · Matrícula") si ya abrió o cerró.
	 */
	@GetMapping("/{id}")
	public FormularioPeriodoMatricula ver(@PathVariable UUID id) {
		return servicio.ver(id);
	}

	/**
	 * POST /api/configuracion/periodos-matricula · botón "Guardar configuración" de "Nuevo periodo de matrícula": crea
	 * el periodo habilitado con sus fechas, vacantes y documentos. 409 si el tarifario no está confirmado o ya hay otro
	 * periodo sin cerrar; 400 si un dato no es válido (no se guarda nada).
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public FormularioPeriodoMatricula crear(@RequestBody SolicitudPeriodoMatricula solicitud) {
		return servicio.crear(solicitud);
	}

	/**
	 * PUT /api/configuracion/periodos-matricula/{id} · "Guardar configuración" al editar. Solo un periodo programado:
	 * 409
	 * si ya abrió o cerró.
	 */
	@PutMapping("/{id}")
	public FormularioPeriodoMatricula editar(@PathVariable UUID id, @RequestBody SolicitudPeriodoMatricula solicitud) {
		return servicio.editar(id, solicitud);
	}

	/**
	 * DELETE /api/configuracion/periodos-matricula/{id} · opción "Eliminar" del menú de la fila. Solo un periodo
	 * programado, y 409 si el año ya tiene matrículas o documentos entregados.
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@PathVariable UUID id) {
		servicio.eliminar(id);
	}

	/**
	 * POST /api/configuracion/periodos-matricula/{id}/documentos/{documentoId}/plantilla (multipart, parte
	 * {@code archivo}) · "Subir plantilla" de "Documentos requeridos". XLSX, XLS o CSV de hasta 10 MB; solo con el
	 * periodo programado.
	 */
	@PostMapping(path = "/{id}/documentos/{documentoId}/plantilla", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public DocumentoVista subirPlantilla(@PathVariable UUID id, @PathVariable UUID documentoId,
			@RequestParam(name = "archivo", required = false) MultipartFile archivo) throws IOException {
		if (archivo == null || archivo.isEmpty()) {
			throw ExcepcionNegocio.invalido("Selecciona un archivo para la plantilla.");
		}
		return servicio.subirPlantilla(id, documentoId, archivo.getOriginalFilename(), archivo.getBytes());
	}

	/**
	 * GET /api/configuracion/periodos-matricula/{id}/documentos/{documentoId}/plantilla · descarga la plantilla (con su
	 * nombre y tipo) desde el formulario y la vista de solo lectura, en cualquier estado del periodo. 404 si el
	 * documento no tiene plantilla.
	 */
	@GetMapping("/{id}/documentos/{documentoId}/plantilla")
	public ResponseEntity<byte[]> descargarPlantilla(@PathVariable UUID id, @PathVariable UUID documentoId) {
		PlantillaGuardada plantilla = servicio.descargarPlantilla(id, documentoId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(plantilla.tipoMime()))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(plantilla.nombreArchivo(), StandardCharsets.UTF_8).build().toString())
				.body(plantilla.contenido());
	}

	/**
	 * DELETE /api/configuracion/periodos-matricula/{id}/documentos/{documentoId}/plantilla · quitar la plantilla de un
	 * documento. Solo con el periodo programado.
	 */
	@DeleteMapping("/{id}/documentos/{documentoId}/plantilla")
	public DocumentoVista quitarPlantilla(@PathVariable UUID id, @PathVariable UUID documentoId) {
		return servicio.quitarPlantilla(id, documentoId);
	}
}
