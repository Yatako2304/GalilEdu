package com.galiledu.academico.aplicacion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.galiledu.academico.aplicacion.DtosCalificaciones.ArchivoEvidencia;
import com.galiledu.academico.aplicacion.DtosCalificaciones.ColumnaReporte;
import com.galiledu.academico.aplicacion.DtosCalificaciones.CompetenciaItem;
import com.galiledu.academico.aplicacion.DtosCalificaciones.CompetenciaOpcion;
import com.galiledu.academico.aplicacion.DtosCalificaciones.CursoDocente;
import com.galiledu.academico.aplicacion.DtosCalificaciones.EvidenciaResumen;
import com.galiledu.academico.aplicacion.DtosCalificaciones.FilaCalificacion;
import com.galiledu.academico.aplicacion.DtosCalificaciones.FilaReporte;
import com.galiledu.academico.aplicacion.DtosCalificaciones.ItemEvaluacion;
import com.galiledu.academico.aplicacion.DtosCalificaciones.PeriodoCurso;
import com.galiledu.academico.aplicacion.DtosCalificaciones.RegistroCalificacion;
import com.galiledu.academico.aplicacion.DtosCalificaciones.ReporteCalificaciones;
import com.galiledu.academico.aplicacion.DtosCalificaciones.SolicitudCalificaciones;
import com.galiledu.academico.aplicacion.DtosCalificaciones.SolicitudItem;
import com.galiledu.academico.dominio.CalculoNotas;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.CalificacionFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.CompetenciaPeso;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.CursoFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.DetalleReporteFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.ItemCompetenciaFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.ItemContexto;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.ItemFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.PeriodoContexto;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.PeriodoFila;
import com.galiledu.compartido.web.ExcepcionNegocio;

/** Casos de uso del docente para HU-57, HU-59 y HU-62. */
@Service
public class ServicioCalificaciones {
	private static final long TAMANO_MAXIMO = 10L * 1024 * 1024;
	private static final BigDecimal PESO_TOTAL = new BigDecimal("100.00");
	private static final Set<String> TIPOS_EVIDENCIA = Set.of("application/pdf", "application/msword",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/jpeg", "image/png");

	private final RepositorioCalificaciones repositorio;
	private final Clock reloj;

	public ServicioCalificaciones(RepositorioCalificaciones repositorio, Clock reloj) {
		this.repositorio = repositorio;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public List<CursoDocente> cursos(UUID docenteId) {
		return repositorio.cursos(docenteId).stream().map(c -> new CursoDocente(c.planCursoId(), c.seccionId(),
				c.anioEscolarId(), c.anio(), c.curso(), c.area(), c.gradoCodigo(), c.grado(), c.seccion(), c.escala(),
				c.reglaNota(), c.consolidacion(), periodos(c))).toList();
	}

	private List<PeriodoCurso> periodos(CursoFila curso) {
		LocalDate hoy = LocalDate.now(reloj);
		return repositorio.periodos(curso.planCursoId(), curso.seccionId()).stream()
				.map(p -> new PeriodoCurso(p.id(), p.numero(), p.nombre(), p.fechaInicio(), p.fechaFin(),
						p.fechaLimiteNotas(), estado(p, curso, hoy), abierto(p, curso, hoy), p.competencias(),
						p.items()))
				.toList();
	}

	private static String estado(PeriodoFila periodo, CursoFila curso, LocalDate hoy) {
		if (!"ACTIVO".equals(curso.estadoAnio())) {
			return curso.estadoAnio();
		}
		return hoy.isBefore(periodo.fechaInicio())
				? "PLANIFICADO"
				: hoy.isAfter(periodo.fechaFin()) ? "CERRADO" : "EN_CURSO";
	}

	private static boolean abierto(PeriodoFila periodo, CursoFila curso, LocalDate hoy) {
		return "ACTIVO".equals(curso.estadoAnio()) && !hoy.isBefore(periodo.fechaInicio())
				&& !hoy.isAfter(periodo.fechaLimiteNotas());
	}

	@Transactional(readOnly = true)
	public List<CompetenciaOpcion> competencias(UUID docenteId, UUID planCursoId, UUID seccionId) {
		validarAcceso(docenteId, planCursoId, seccionId);
		return repositorio.competencias(planCursoId).stream().map(c -> new CompetenciaOpcion(c.id(), c.nombre()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ItemEvaluacion> items(UUID docenteId, UUID planCursoId, UUID seccionId, UUID periodoId) {
		validarAcceso(docenteId, planCursoId, seccionId);
		PeriodoContexto periodo = periodo(planCursoId, periodoId);
		List<ItemFila> items = repositorio.items(planCursoId, seccionId, periodoId);
		Map<UUID, List<ItemCompetenciaFila>> competencias = repositorio
				.competenciasDeItems(items.stream().map(ItemFila::id).toList()).stream()
				.collect(Collectors.groupingBy(ItemCompetenciaFila::itemEvaluacionId, LinkedHashMap::new,
						Collectors.toList()));
		boolean editable = abierto(periodo);
		return items.stream().map(item -> mapear(item, competencias.getOrDefault(item.id(), List.of()), editable))
				.toList();
	}

	@Transactional
	public ItemEvaluacion crearItem(UUID docenteId, UUID planCursoId, UUID seccionId, UUID periodoId,
			SolicitudItem solicitud) {
		validarAcceso(docenteId, planCursoId, seccionId);
		PeriodoContexto periodo = periodo(planCursoId, periodoId);
		exigirAbierto(periodo);
		repositorio.bloquearPlanCurso(planCursoId);
		SolicitudItem limpia = validarItem(planCursoId, seccionId, periodoId, solicitud, null);
		UUID id = repositorio.crearItem(planCursoId, seccionId, periodoId, limpia.nombre(), limpia.descripcion(),
				limpia.tipo(), docenteId);
		repositorio.reemplazarCompetencias(id, planCursoId, limpia.competencias().stream()
				.map(c -> new CompetenciaPeso(c.competenciaId(), c.pesoPorcentaje())).toList());
		return buscarItem(id, true);
	}

	@Transactional
	public ItemEvaluacion editarItem(UUID docenteId, UUID id, SolicitudItem solicitud) {
		ItemContexto item = itemConAcceso(docenteId, id);
		exigirAbierto(item);
		repositorio.bloquearPlanCurso(item.planCursoId());
		SolicitudItem limpia = validarItem(item.planCursoId(), item.seccionId(), item.periodoAcademicoId(), solicitud,
				id);
		repositorio.actualizarItem(id, limpia.nombre(), limpia.descripcion(), limpia.tipo());
		repositorio.reemplazarCompetencias(id, item.planCursoId(), limpia.competencias().stream()
				.map(c -> new CompetenciaPeso(c.competenciaId(), c.pesoPorcentaje())).toList());
		return buscarItem(id, true);
	}

	@Transactional
	public void eliminarItem(UUID docenteId, UUID id) {
		ItemContexto item = itemConAcceso(docenteId, id);
		exigirAbierto(item);
		if (repositorio.tieneCalificaciones(id)) {
			throw ExcepcionNegocio.conflicto("No se puede eliminar un ítem que ya tiene calificaciones.");
		}
		repositorio.eliminarItem(id);
	}

	@Transactional(readOnly = true)
	public List<FilaCalificacion> calificaciones(UUID docenteId, UUID itemId) {
		ItemContexto item = itemConAcceso(docenteId, itemId);
		return repositorio.calificaciones(itemId, item.seccionId()).stream().map(ServicioCalificaciones::mapear)
				.toList();
	}

	@Transactional
	public List<FilaCalificacion> guardarCalificaciones(UUID docenteId, UUID itemId,
			SolicitudCalificaciones solicitud) {
		ItemContexto item = itemConAcceso(docenteId, itemId);
		exigirAbierto(item);
		Set<UUID> vistas = new HashSet<>();
		for (RegistroCalificacion registro : solicitud.calificaciones()) {
			if (!vistas.add(registro.matriculaId())) {
				throw ExcepcionNegocio.invalido("Cada estudiante debe aparecer una sola vez.");
			}
			if (!repositorio.matriculaPertenece(registro.matriculaId(), item.seccionId())) {
				throw ExcepcionNegocio.invalido("Uno de los estudiantes no pertenece a la sección del curso.");
			}
			guardarCalificacion(docenteId, item, registro);
		}
		return calificaciones(docenteId, itemId);
	}

	private void guardarCalificacion(UUID docenteId, ItemContexto item, RegistroCalificacion registro) {
		BigDecimal nota = registro.notaNumerica();
		String nivel = registro.nivelLogro();
		if (nota == null && nivel == null) {
			repositorio.quitarCalificacion(item.id(), registro.matriculaId());
			return;
		}
		if ("VIGESIMAL".equals(item.escala())) {
			if (nota == null || nivel != null) {
				throw ExcepcionNegocio.invalido("La escala vigesimal requiere una calificación numérica de 0 a 20.");
			}
		} else if (nivel == null || nota != null) {
			throw ExcepcionNegocio.invalido("La escala cualitativa requiere un nivel AD, A, B o C.");
		}
		repositorio.guardarCalificacion(item.id(), registro.matriculaId(), nota, nivel, docenteId);
	}

	@Transactional
	public EvidenciaResumen guardarEvidencia(UUID docenteId, UUID itemId, UUID matriculaId, MultipartFile archivo) {
		ItemContexto item = itemConAcceso(docenteId, itemId);
		exigirAbierto(item);
		if (!repositorio.matriculaPertenece(matriculaId, item.seccionId())) {
			throw ExcepcionNegocio.invalido("El estudiante no pertenece a la sección del curso.");
		}
		if (archivo == null || archivo.isEmpty()) {
			throw ExcepcionNegocio.invalido("Selecciona una evidencia pedagógica.");
		}
		if (!TIPOS_EVIDENCIA.contains(archivo.getContentType())) {
			throw ExcepcionNegocio.invalido("La evidencia debe ser PDF, DOC, DOCX, JPG o PNG.");
		}
		if (archivo.getSize() > TAMANO_MAXIMO) {
			throw ExcepcionNegocio.invalido("La evidencia no puede pesar más de 10 MB.");
		}
		UUID calificacionId = repositorio.calificacionId(itemId, matriculaId)
				.orElseThrow(() -> ExcepcionNegocio.conflicto("Guarda primero la calificación del estudiante."));
		repositorio.guardarEvidencia(calificacionId, nombreSeguro(archivo.getOriginalFilename()),
				archivo.getContentType(), bytes(archivo));
		return calificaciones(docenteId, itemId).stream().filter(f -> f.matriculaId().equals(matriculaId)).findFirst()
				.map(FilaCalificacion::evidencia).orElseThrow();
	}

	@Transactional(readOnly = true)
	public ArchivoEvidencia descargarEvidencia(UUID docenteId, UUID itemId, UUID matriculaId) {
		ItemContexto item = itemConAcceso(docenteId, itemId);
		if (!repositorio.matriculaPertenece(matriculaId, item.seccionId())) {
			throw ExcepcionNegocio.noEncontrado("La evidencia no existe.");
		}
		UUID calificacionId = repositorio.calificacionId(itemId, matriculaId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La evidencia no existe."));
		return repositorio.evidencia(calificacionId)
				.map(e -> new ArchivoEvidencia(e.nombreArchivo(), e.tipoMime(), e.contenido()))
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La evidencia no existe."));
	}

	@Transactional
	public void eliminarEvidencia(UUID docenteId, UUID itemId, UUID matriculaId) {
		ItemContexto item = itemConAcceso(docenteId, itemId);
		exigirAbierto(item);
		UUID calificacionId = repositorio.calificacionId(itemId, matriculaId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La evidencia no existe."));
		repositorio.quitarEvidencia(calificacionId);
	}

	@Transactional(readOnly = true)
	public ReporteCalificaciones reporte(UUID docenteId, String tipo, UUID planCursoId, UUID seccionId,
			UUID periodoId, UUID objetivoId) {
		return reporte(docenteId, tipo, planCursoId, seccionId, periodoId, objetivoId, List.of(), true);
	}

	@Transactional(readOnly = true)
	public ReporteCalificaciones reporte(UUID docenteId, String tipo, UUID planCursoId, UUID seccionId,
			UUID periodoId, UUID objetivoId, List<UUID> itemIds, boolean incluirPromedio) {
		validarAcceso(docenteId, planCursoId, seccionId);
		PeriodoContexto periodo = periodo(planCursoId, periodoId);
		List<DetalleReporteFila> detalles = repositorio.detallesReporte(planCursoId, seccionId, periodoId);
		return switch (tipo == null ? "" : tipo.toUpperCase()) {
			case "ITEM" -> {
				UUID itemId = exigirObjetivo(objetivoId, "Elige un ítem de evaluación.");
				ItemFila item = repositorio.items(planCursoId, seccionId, periodoId).stream()
						.filter(i -> i.id().equals(itemId)).findFirst()
						.orElseThrow(() -> ExcepcionNegocio
								.noEncontrado("El ítem no pertenece al curso y periodo seleccionados."));
				yield reporteItem(detalles, item);
			}
			case "COMPETENCIA" -> reporteCompetencia(detalles,
					exigirObjetivo(objetivoId, "Elige una competencia."), periodo, itemIds, incluirPromedio);
			case "PERIODO" -> reportePeriodo(detalles, periodo.reglaNota());
			default -> throw ExcepcionNegocio.invalido("El tipo de reporte debe ser ITEM, COMPETENCIA o PERIODO.");
		};
	}

	@Transactional(readOnly = true)
	public byte[] exportarCsv(UUID docenteId, String tipo, UUID planCursoId, UUID seccionId, UUID periodoId,
			UUID objetivoId) {
		return exportarCsv(docenteId, tipo, planCursoId, seccionId, periodoId, objetivoId, List.of(), true);
	}

	@Transactional(readOnly = true)
	public byte[] exportarCsv(UUID docenteId, String tipo, UUID planCursoId, UUID seccionId, UUID periodoId,
			UUID objetivoId, List<UUID> itemIds, boolean incluirPromedio) {
		ReporteCalificaciones reporte = reporte(docenteId, tipo, planCursoId, seccionId, periodoId, objetivoId,
				itemIds, incluirPromedio);
		StringBuilder csv = new StringBuilder("\uFEFFEstudiante");
		reporte.columnas().forEach(c -> csv.append(',').append(csv(c.titulo())));
		csv.append('\n');
		for (FilaReporte fila : reporte.filas()) {
			csv.append(csv(fila.estudiante()));
			for (ColumnaReporte columna : reporte.columnas()) {
				csv.append(',').append(csv(fila.valores().getOrDefault(columna.clave(), "")));
			}
			csv.append('\n');
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private ReporteCalificaciones reporteItem(List<DetalleReporteFila> detalles, ItemFila item) {
		List<DetalleReporteFila> filas = detalles.stream().filter(d -> d.itemId().equals(item.id()))
				.collect(Collectors.toMap(DetalleReporteFila::matriculaId, Function.identity(), (a, b) -> a,
						LinkedHashMap::new))
				.values().stream().toList();
		List<ColumnaReporte> columnas = List.of(new ColumnaReporte("nota", "Calificación"),
				new ColumnaReporte("evidencia", "Evidencia"), new ColumnaReporte("estado", "Estado"));
		return new ReporteCalificaciones(item.nombre(), "Calificaciones por ítem", columnas,
				filas.stream().map(f -> new FilaReporte(f.matriculaId(), f.estudiante(), Map.of("nota", valor(f),
						"evidencia", f.evidenciaNombre() == null ? "—" : f.evidenciaNombre(), "estado",
						valor(f).equals("—") ? "Pendiente" : "Calificado"))).toList());
	}

	private ReporteCalificaciones reporteCompetencia(List<DetalleReporteFila> detalles, UUID competenciaId,
			PeriodoContexto periodo, List<UUID> itemIds, boolean incluirPromedio) {
		List<DetalleReporteFila> competencia = detalles.stream().filter(d -> d.competenciaId().equals(competenciaId))
				.toList();
		if (competencia.isEmpty()) {
			throw ExcepcionNegocio.noEncontrado("La competencia no tiene ítems en el periodo seleccionado.");
		}
		if (!itemIds.isEmpty()) {
			Set<UUID> disponibles = competencia.stream().map(DetalleReporteFila::itemId).collect(Collectors.toSet());
			if (!disponibles.containsAll(itemIds)) {
				throw ExcepcionNegocio.invalido("Uno de los ítems no pertenece a la competencia seleccionada.");
			}
			Set<UUID> seleccionados = new HashSet<>(itemIds);
			competencia = competencia.stream().filter(d -> seleccionados.contains(d.itemId())).toList();
		}
		List<DetalleReporteFila> items = competencia.stream()
				.collect(Collectors.toMap(DetalleReporteFila::itemId, Function.identity(), (a, b) -> a,
						LinkedHashMap::new))
				.values().stream().toList();
		if (!itemIds.isEmpty()) {
			Map<UUID, Integer> orden = new LinkedHashMap<>();
			for (int indice = 0; indice < itemIds.size(); indice++) {
				orden.put(itemIds.get(indice), indice);
			}
			items = items.stream().sorted(Comparator.comparingInt(i -> orden.get(i.itemId()))).toList();
		}
		List<ColumnaReporte> columnas = new ArrayList<>(items.stream()
				.map(i -> new ColumnaReporte(i.itemId().toString(), i.item())).toList());
		if (incluirPromedio) {
			columnas.add(new ColumnaReporte("resultado", "Promedio final"));
		}
		int notaAprobatoria = repositorio.notaAprobatoria(periodo.fechaInicio()).orElse(14);
		Map<UUID, List<DetalleReporteFila>> porEstudiante = competencia.stream()
				.collect(Collectors.groupingBy(DetalleReporteFila::matriculaId, LinkedHashMap::new,
						Collectors.toList()));
		List<FilaReporte> filas = porEstudiante.values().stream().map(lista -> {
			Map<String, String> valores = new LinkedHashMap<>();
			lista.forEach(d -> valores.put(d.itemId().toString(), valor(d)));
			if (incluirPromedio) {
				String resultado = resultado(lista, periodo.reglaNota());
				valores.put("resultado", resultado);
				valores.put("resultado_estado", estadoResultado(resultado, periodo.escala(), notaAprobatoria));
			}
			return new FilaReporte(lista.get(0).matriculaId(), lista.get(0).estudiante(), valores);
		}).toList();
		return new ReporteCalificaciones(competencia.get(0).competencia(), "Consolidado de notas", columnas,
				filas);
	}

	private static String estadoResultado(String resultado, String escala, int notaAprobatoria) {
		if ("—".equals(resultado)) {
			return "Pendiente";
		}
		if ("VIGESIMAL".equals(escala)) {
			return new BigDecimal(resultado).compareTo(BigDecimal.valueOf(notaAprobatoria)) >= 0
					? "Aprobado"
					: "Desaprobado";
		}
		return "A".equals(resultado) || "AD".equals(resultado) ? "Aprobado" : "Desaprobado";
	}

	private ReporteCalificaciones reportePeriodo(List<DetalleReporteFila> detalles, String regla) {
		List<DetalleReporteFila> competencias = detalles.stream()
				.collect(Collectors.toMap(DetalleReporteFila::competenciaId, Function.identity(), (a, b) -> a,
						LinkedHashMap::new))
				.values().stream().toList();
		List<ColumnaReporte> columnas = competencias.stream()
				.map(c -> new ColumnaReporte(c.competenciaId().toString(), c.competencia())).toList();
		Map<UUID, List<DetalleReporteFila>> porEstudiante = detalles.stream()
				.collect(Collectors.groupingBy(DetalleReporteFila::matriculaId, LinkedHashMap::new,
						Collectors.toList()));
		List<FilaReporte> filas = porEstudiante.values().stream().map(lista -> {
			Map<String, String> valores = new LinkedHashMap<>();
			for (DetalleReporteFila competencia : competencias) {
				valores.put(competencia.competenciaId().toString(), resultado(lista.stream()
						.filter(d -> d.competenciaId().equals(competencia.competenciaId())).toList(), regla));
			}
			return new FilaReporte(lista.get(0).matriculaId(), lista.get(0).estudiante(), valores);
		}).toList();
		return new ReporteCalificaciones("Reporte del periodo", "Resultados por competencia", columnas, filas);
	}

	private static String resultado(List<DetalleReporteFila> detalles, String regla) {
		return CalculoNotas.combinar(detalles.stream()
				.map(d -> new CalculoNotas.Nota(d.pesoPorcentaje(), d.notaNumerica(), d.nivelLogro(), d.ordenItem()))
				.toList(), regla).map(CalculoNotas::texto).orElse("—");
	}

	private SolicitudItem validarItem(UUID planCursoId, UUID seccionId, UUID periodoId, SolicitudItem solicitud,
			UUID excepto) {
		String nombre = solicitud.nombre().strip();
		String descripcion = solicitud.descripcion().strip();
		if (repositorio.existeNombre(planCursoId, seccionId, periodoId, nombre, excepto)) {
			throw ExcepcionNegocio.conflicto("Ya existe un ítem con ese nombre en el periodo.");
		}
		Map<UUID, String> permitidas = repositorio.competencias(planCursoId).stream()
				.collect(Collectors.toMap(c -> c.id(), c -> c.nombre()));
		Set<UUID> vistas = new HashSet<>();
		solicitud.competencias().forEach(c -> {
			if (!permitidas.containsKey(c.competenciaId())) {
				throw ExcepcionNegocio.invalido("Una competencia no pertenece al plan del curso.");
			}
			if (!vistas.add(c.competenciaId())) {
				throw ExcepcionNegocio.invalido("Cada competencia debe asociarse una sola vez.");
			}
			BigDecimal asignado = repositorio.pesoAsignado(planCursoId, seccionId, periodoId,
					c.competenciaId(), excepto);
			if (asignado.add(c.pesoPorcentaje()).compareTo(PESO_TOTAL) > 0) {
				throw ExcepcionNegocio.invalido("El peso total de la competencia «"
						+ permitidas.get(c.competenciaId()) + "» no puede superar el 100 %.");
			}
		});
		return new SolicitudItem(nombre, descripcion, solicitud.tipo(), solicitud.competencias());
	}

	private ItemEvaluacion buscarItem(UUID id, boolean editable) {
		ItemFila item = repositorio.itemsDeIds(List.of(id)).stream().findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El ítem de evaluación no existe."));
		return mapear(item, repositorio.competenciasDeItems(List.of(id)), editable);
	}

	private static ItemEvaluacion mapear(ItemFila item, List<ItemCompetenciaFila> competencias, boolean editable) {
		return new ItemEvaluacion(item.id(), item.nombre(), item.descripcion(), item.tipo(), item.actualizadoEn(),
				competencias.stream().map(c -> new CompetenciaItem(c.competenciaId(), c.competencia(),
						c.pesoPorcentaje())).toList(),
				editable);
	}

	private static FilaCalificacion mapear(CalificacionFila fila) {
		EvidenciaResumen evidencia = fila.evidenciaNombreArchivo() == null
				? null
				: new EvidenciaResumen(fila.evidenciaNombreArchivo(), fila.evidenciaTipoMime(),
						fila.evidenciaTamanoBytes(), fila.evidenciaSubidoEn());
		return new FilaCalificacion(fila.id(), fila.matriculaId(), fila.estudianteId(), fila.estudiante(),
				fila.notaNumerica(), fila.nivelLogro(), evidencia, fila.id() == null ? "PENDIENTE" : "CALIFICADO");
	}

	private ItemContexto itemConAcceso(UUID docenteId, UUID itemId) {
		ItemContexto item = repositorio.item(itemId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El ítem de evaluación no existe."));
		validarAcceso(docenteId, item.planCursoId(), item.seccionId());
		return item;
	}

	private PeriodoContexto periodo(UUID planCursoId, UUID periodoId) {
		return repositorio.periodo(planCursoId, periodoId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El periodo no pertenece al año del curso."));
	}

	private void validarAcceso(UUID docenteId, UUID planCursoId, UUID seccionId) {
		if (!repositorio.tieneAcceso(docenteId, planCursoId, seccionId)) {
			throw ExcepcionNegocio.noEncontrado("El curso no está asignado al docente.");
		}
	}

	private boolean abierto(PeriodoContexto periodo) {
		LocalDate hoy = LocalDate.now(reloj);
		return "ACTIVO".equals(periodo.estadoAnio()) && !hoy.isBefore(periodo.fechaInicio())
				&& !hoy.isAfter(periodo.fechaLimiteNotas());
	}

	private void exigirAbierto(ItemContexto item) {
		LocalDate hoy = LocalDate.now(reloj);
		if (!"ACTIVO".equals(item.estadoAnio()) || hoy.isBefore(item.fechaInicio())
				|| hoy.isAfter(item.fechaLimiteNotas())) {
			throw ExcepcionNegocio.conflicto("El periodo no está habilitado para registrar calificaciones.");
		}
	}

	private void exigirAbierto(PeriodoContexto periodo) {
		if (!abierto(periodo)) {
			throw ExcepcionNegocio.conflicto("El periodo no está habilitado para registrar calificaciones.");
		}
	}

	private static UUID exigirObjetivo(UUID objetivoId, String mensaje) {
		if (objetivoId == null) {
			throw ExcepcionNegocio.invalido(mensaje);
		}
		return objetivoId;
	}

	private static String valor(DetalleReporteFila fila) {
		if (fila.notaNumerica() != null) {
			return fila.notaNumerica().stripTrailingZeros().toPlainString();
		}
		return fila.nivelLogro() == null ? "—" : fila.nivelLogro();
	}

	private static String csv(String valor) {
		return '"' + valor.replace("\"", "\"\"") + '"';
	}

	private static String nombreSeguro(String original) {
		String nombre = original == null ? "evidencia" : original.replaceAll("^.*[\\\\/]", "");
		nombre = nombre.replaceAll("[^\\p{L}\\p{N}._ -]", "_").strip();
		return nombre.isEmpty() ? "evidencia" : nombre.substring(0, Math.min(nombre.length(), 200));
	}

	private static byte[] bytes(MultipartFile archivo) {
		try {
			return archivo.getBytes();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
