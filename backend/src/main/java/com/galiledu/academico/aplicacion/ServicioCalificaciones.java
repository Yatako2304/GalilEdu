package com.galiledu.academico.aplicacion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import com.galiledu.academico.aplicacion.DtosCalificaciones.ModificacionCalificacion;
import com.galiledu.academico.aplicacion.DtosCalificaciones.PeriodoCurso;
import com.galiledu.academico.aplicacion.DtosCalificaciones.RegistroCalificacion;
import com.galiledu.academico.aplicacion.DtosCalificaciones.ReporteCalificaciones;
import com.galiledu.academico.aplicacion.DtosCalificaciones.SolicitudCalificaciones;
import com.galiledu.academico.aplicacion.DtosCalificaciones.SolicitudCompetenciaItem;
import com.galiledu.academico.aplicacion.DtosCalificaciones.SolicitudItem;
import com.galiledu.academico.aplicacion.DtosCalificaciones.SolicitudModificacion;
import com.galiledu.academico.dominio.CalculoNotas;
import com.galiledu.academico.infraestructura.ArchivoReporteCalificaciones;
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
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.ValorCalificacion;
import com.galiledu.compartido.web.ExcepcionNegocio;

/** Casos de uso del docente para HU-57, HU-59, HU-60 y HU-62. */
@Service
public class ServicioCalificaciones {
	private static final long TAMANO_MAXIMO = 10L * 1024 * 1024;
	private static final BigDecimal PESO_TOTAL = new BigDecimal("100.00");
	private static final Set<String> TIPOS_EVIDENCIA = Set.of("application/pdf", "application/msword",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/jpeg", "image/png");
	private static final Map<String, Integer> ORDEN_NIVEL = Map.of("C", 0, "B", 1, "A", 2, "AD", 3);
	static final int MOTIVO_MINIMO = 10;

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
						p.fechaLimiteNotas(), estado(p, curso, hoy), abierto(p, curso, hoy),
						modificable(curso.estadoAnio(), p.fechaInicio(), hoy), p.competencias(), p.items()))
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

	/**
	 * HU-60: una nota ya registrada se puede modificar, con motivo, desde que empieza el periodo y mientras el año esté
	 * ACTIVO, también después de la fecha límite de registro. Con el año cerrado ya no se modifica.
	 */
	private static boolean modificable(String estadoAnio, LocalDate fechaInicio, LocalDate hoy) {
		return "ACTIVO".equals(estadoAnio) && !hoy.isBefore(fechaInicio);
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
		SolicitudItem limpia = validarItem(planCursoId, seccionId, periodoId, solicitud, null, periodo.reglaNota());
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
				id, item.reglaNota());
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

	/**
	 * El lote solo <b>registra</b> celdas vacías. Una celda ya resuelta (con nota o «No evaluado») se cambia únicamente
	 * con {@link #modificarCalificacion}, que pide motivo y deja historial: así cada cambio tiene un motivo escrito por
	 * el docente. Repetir el mismo valor o enviar vacía una celda ya resuelta no cambia nada.
	 */
	private void guardarCalificacion(UUID docenteId, ItemContexto item, RegistroCalificacion registro) {
		BigDecimal nota = registro.notaNumerica();
		String nivel = registro.nivelLogro();
		Optional<ValorCalificacion> anterior = repositorio.valorCalificacion(item.id(), registro.matriculaId());
		if (nota == null && nivel == null) {
			return;
		}
		exigirValor(item.escala(), nota, nivel);
		ValorCalificacion nueva = new ValorCalificacion(nota, nivel);
		if (anterior.isPresent()) {
			if (iguales(anterior.get(), nueva)) {
				return;
			}
			throw ExcepcionNegocio.conflicto(
					"Una de las notas ya está registrada: para cambiarla usa «Modificar» e indica el motivo.");
		}
		repositorio.guardarCalificacion(item.id(), registro.matriculaId(), nota, nivel, docenteId);
	}

	private static void exigirValor(String escala, BigDecimal nota, String nivel) {
		if ("VIGESIMAL".equals(escala)) {
			if (nota == null || nivel != null) {
				throw ExcepcionNegocio.invalido("La escala vigesimal requiere una calificación numérica de 0 a 20.");
			}
		} else if (nivel == null || nota != null) {
			throw ExcepcionNegocio.invalido("La escala cualitativa requiere un nivel AD, A, B o C.");
		}
	}

	private static boolean iguales(ValorCalificacion a, ValorCalificacion b) {
		boolean notas = a.notaNumerica() == null
				? b.notaNumerica() == null
				: b.notaNumerica() != null && a.notaNumerica().compareTo(b.notaNumerica()) == 0;
		return notas && Objects.equals(a.nivelLogro(), b.nivelLogro()) && a.noEvaluado() == b.noEvaluado();
	}

	/**
	 * HU-60: resuelve una celda fuera del registro en lote. Sirve para:
	 * <ul>
	 * <li><b>modificar</b> una nota ya registrada;</li>
	 * <li><b>registrar fuera de plazo</b> una nota que falta, después de la fecha límite;</li>
	 * <li>marcar la celda como <b>«No evaluado»</b> (el ítem no cuenta para el estudiante).</li>
	 * </ul>
	 * Se permite desde que empieza el periodo y mientras el año esté ACTIVO. Exige motivo (10 a 500 caracteres) salvo
	 * para registrar una nota en una celda vacía dentro del plazo, y deja el cambio en el historial.
	 */
	@Transactional
	public FilaCalificacion modificarCalificacion(UUID docenteId, UUID itemId, UUID matriculaId,
			SolicitudModificacion solicitud) {
		ItemContexto item = itemConAcceso(docenteId, itemId);
		LocalDate hoy = LocalDate.now(reloj);
		if (!modificable(item.estadoAnio(), item.fechaInicio(), hoy)) {
			throw ExcepcionNegocio.conflicto(
					"Solo se modifican calificaciones de un periodo ya iniciado y de un año escolar activo.");
		}
		if (!repositorio.matriculaPertenece(matriculaId, item.seccionId())) {
			throw ExcepcionNegocio.invalido("El estudiante no pertenece a la sección del curso.");
		}
		Optional<ValorCalificacion> anterior = repositorio.valorCalificacion(itemId, matriculaId);
		boolean enPlazo = !hoy.isAfter(item.fechaLimiteNotas());
		ValorCalificacion nueva;
		if (solicitud.marcaNoEvaluado()) {
			if (solicitud.notaNumerica() != null || solicitud.nivelLogro() != null) {
				throw ExcepcionNegocio.invalido("«No evaluado» no lleva calificación.");
			}
			nueva = ValorCalificacion.NO_EVALUADO;
		} else {
			exigirValor(item.escala(), solicitud.notaNumerica(), solicitud.nivelLogro());
			nueva = new ValorCalificacion(solicitud.notaNumerica(), solicitud.nivelLogro());
		}
		if (anterior.isPresent() && iguales(anterior.get(), nueva)) {
			throw ExcepcionNegocio.invalido(nueva.noEvaluado()
					? "La celda ya está marcada como «No evaluado»."
					: "La nueva calificación es igual a la actual.");
		}
		// Registrar por primera vez una nota dentro del plazo es parte del registro: no pide motivo ni deja historial.
		boolean conHistorial = anterior.isPresent() || !enPlazo || nueva.noEvaluado();
		String motivo = solicitud.motivo() == null ? "" : solicitud.motivo().strip();
		if (conHistorial && motivo.length() < MOTIVO_MINIMO) {
			throw ExcepcionNegocio.invalido(
					"Explica el motivo (al menos " + MOTIVO_MINIMO + " caracteres): queda en el historial.");
		}
		if (nueva.noEvaluado()) {
			repositorio.marcarNoEvaluado(itemId, matriculaId, motivo, docenteId);
		} else {
			repositorio.guardarCalificacion(itemId, matriculaId, nueva.notaNumerica(), nueva.nivelLogro(), docenteId);
		}
		if (conHistorial) {
			repositorio.registrarModificacion(itemId, matriculaId, anterior.orElse(null), nueva, motivo, docenteId);
		}
		return calificaciones(docenteId, itemId).stream().filter(f -> f.matriculaId().equals(matriculaId))
				.findFirst().orElseThrow();
	}

	/** HU-60: los cambios de la nota de un estudiante en un ítem, del más reciente al más antiguo. */
	@Transactional(readOnly = true)
	public List<ModificacionCalificacion> historial(UUID docenteId, UUID itemId, UUID matriculaId) {
		ItemContexto item = itemConAcceso(docenteId, itemId);
		if (!repositorio.matriculaPertenece(matriculaId, item.seccionId())) {
			throw ExcepcionNegocio.noEncontrado("El estudiante no pertenece a la sección del curso.");
		}
		return repositorio.historial(itemId, matriculaId).stream()
				.map(f -> new ModificacionCalificacion(f.notaAnterior(), f.nivelAnterior(), f.noEvaluadoAnterior(),
						f.notaNueva(), f.nivelNuevo(), f.noEvaluadoNuevo(), f.motivo(), f.modificadoPor(),
						f.modificadoEn()))
				.toList();
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
		return reporte(docenteId, tipo, planCursoId, seccionId, periodoId, objetivoId, itemIds, incluirPromedio,
				List.of());
	}

	@Transactional(readOnly = true)
	public ReporteCalificaciones reporte(UUID docenteId, String tipo, UUID planCursoId, UUID seccionId,
			UUID periodoId, UUID objetivoId, List<UUID> itemIds, boolean incluirPromedio, List<UUID> competenciaIds) {
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
			case "COMPETENCIA" -> competenciaIds.isEmpty()
					? reporteCompetencia(detalles, exigirObjetivo(objetivoId, "Elige una competencia."), periodo,
							itemIds, incluirPromedio)
					: reporteCompetencias(planCursoId, seccionId, detalles, periodo, competenciaIds, itemIds,
							incluirPromedio);
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
		return exportarCsv(docenteId, tipo, planCursoId, seccionId, periodoId, objetivoId, itemIds,
				incluirPromedio, List.of());
	}

	@Transactional(readOnly = true)
	public byte[] exportarCsv(UUID docenteId, String tipo, UUID planCursoId, UUID seccionId, UUID periodoId,
			UUID objetivoId, List<UUID> itemIds, boolean incluirPromedio, List<UUID> competenciaIds) {
		ReporteCalificaciones reporte = reporte(docenteId, tipo, planCursoId, seccionId, periodoId, objetivoId,
				itemIds, incluirPromedio, competenciaIds);
		StringBuilder csv = new StringBuilder("\uFEFFEstudiante");
		reporte.columnas().forEach(c -> csv.append(',').append(csv(c.competencia() == null
				? c.titulo()
				: c.competencia() + " · " + c.titulo())));
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

	@Transactional(readOnly = true)
	public byte[] exportarPdf(UUID docenteId, String tipo, UUID planCursoId, UUID seccionId, UUID periodoId,
			UUID objetivoId, List<UUID> itemIds, boolean incluirPromedio, List<UUID> competenciaIds) {
		return exportarPdf(docenteId, tipo, planCursoId, seccionId, periodoId, objetivoId, itemIds, incluirPromedio,
				competenciaIds, null);
	}

	/**
	 * notasPeriodo: la nota oficial de cada competencia en el mismo periodo, que va al final del PDF (null para no
	 * incluirla).
	 */
	@Transactional(readOnly = true)
	public byte[] exportarPdf(UUID docenteId, String tipo, UUID planCursoId, UUID seccionId, UUID periodoId,
			UUID objetivoId, List<UUID> itemIds, boolean incluirPromedio, List<UUID> competenciaIds,
			ReporteCalificaciones notasPeriodo) {
		ReporteCalificaciones reporte = reporte(docenteId, tipo, planCursoId, seccionId, periodoId, objetivoId,
				itemIds, incluirPromedio, competenciaIds);
		CursoFila curso = repositorio.cursos(docenteId).stream()
				.filter(c -> c.planCursoId().equals(planCursoId) && c.seccionId().equals(seccionId)).findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El curso no está asignado al docente."));
		PeriodoFila periodo = repositorio.periodos(planCursoId, seccionId).stream()
				.filter(p -> p.id().equals(periodoId)).findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El periodo no pertenece al año del curso."));
		String contexto = curso.curso() + " · " + curso.grado() + " · Sección " + curso.seccion()
				+ " · Año escolar " + curso.anio() + " · " + periodo.nombre() + " · Generado el "
				+ LocalDateTime.now(reloj).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
		String regla = switch (curso.reglaNota()) {
			case "PROMEDIO_PONDERADO" -> "Promedio ponderado";
			case "PROMEDIO_SIMPLE" -> "Promedio simple";
			case "NIVEL_MAS_FRECUENTE" -> "Nivel más frecuente";
			case "ULTIMO_NIVEL" -> "Último nivel";
			default -> curso.reglaNota();
		};
		String parametros = "Escala: " + ("VIGESIMAL".equals(curso.escala()) ? "0 a 20" : "AD, A, B, C")
				+ " · Regla del año escolar: " + regla
				+ " · El promedio del periodo es el aporte de este curso a cada competencia en el periodo; la nota"
				+ " oficial de la competencia combina todos los cursos del grado que la evalúan.";
		return ArchivoReporteCalificaciones.pdf(reporte, contexto, parametros, curso.escala(), notasPeriodo);
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
						f.noEvaluado()
								? "No evaluado"
								: f.notaNumerica() != null || f.nivelLogro() != null ? "Calificado" : "Pendiente")))
						.toList());
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
			columnas.add(new ColumnaReporte("resultado", "Promedio del periodo"));
		}
		List<CalculoNotas.Rango> rangos = rangosConversion(periodo.fechaInicio());
		Map<UUID, List<DetalleReporteFila>> porEstudiante = competencia.stream()
				.collect(Collectors.groupingBy(DetalleReporteFila::matriculaId, LinkedHashMap::new,
						Collectors.toList()));
		List<FilaReporte> filas = porEstudiante.values().stream().map(lista -> {
			Map<String, String> valores = new LinkedHashMap<>();
			lista.forEach(d -> valores.put(d.itemId().toString(), valor(d)));
			if (incluirPromedio) {
				registrarResultado(valores, "resultado", resultado(lista, periodo.reglaNota()), lista, false, periodo,
						rangos);
			}
			return new FilaReporte(lista.get(0).matriculaId(), lista.get(0).estudiante(), valores);
		}).toList();
		return new ReporteCalificaciones(competencia.get(0).competencia(), "Consolidado de notas", columnas,
				filas);
	}

	private ReporteCalificaciones reporteCompetencias(UUID planCursoId, UUID seccionId,
			List<DetalleReporteFila> detalles, PeriodoContexto periodo, List<UUID> competenciaIds,
			List<UUID> itemIds, boolean incluirPromedio) {
		List<UUID> seleccionadas = competenciaIds.stream().distinct().toList();
		Map<UUID, String> permitidas = repositorio.competencias(planCursoId).stream()
				.collect(Collectors.toMap(c -> c.id(), c -> c.nombre()));
		if (!permitidas.keySet().containsAll(seleccionadas)) {
			throw ExcepcionNegocio.invalido("Una competencia no pertenece al plan del curso.");
		}
		List<ItemFila> itemsCurso = repositorio.items(planCursoId, seccionId, periodo.id());
		List<ItemCompetenciaFila> asociaciones = repositorio
				.competenciasDeItems(itemsCurso.stream().map(ItemFila::id).toList()).stream()
				.filter(c -> seleccionadas.contains(c.competenciaId())).toList();
		Set<UUID> disponibles = asociaciones.stream().map(ItemCompetenciaFila::itemEvaluacionId)
				.collect(Collectors.toSet());
		if (!disponibles.containsAll(itemIds)) {
			throw ExcepcionNegocio.invalido("Uno de los ítems no pertenece a las competencias seleccionadas.");
		}
		Set<UUID> elegidos = itemIds.isEmpty() ? disponibles : new HashSet<>(itemIds);
		List<ItemFila> itemsElegidos = itemsCurso.stream().filter(i -> elegidos.contains(i.id())).toList();
		if (!itemIds.isEmpty()) {
			Map<UUID, Integer> orden = new LinkedHashMap<>();
			for (int indice = 0; indice < itemIds.size(); indice++) {
				orden.putIfAbsent(itemIds.get(indice), indice);
			}
			itemsElegidos = itemsElegidos.stream().sorted(Comparator.comparingInt(i -> orden.get(i.id()))).toList();
		}
		List<ColumnaReporte> columnas = new ArrayList<>();
		for (UUID competenciaId : seleccionadas) {
			Set<UUID> deCompetencia = asociaciones.stream().filter(c -> c.competenciaId().equals(competenciaId))
					.map(ItemCompetenciaFila::itemEvaluacionId).collect(Collectors.toSet());
			for (ItemFila item : itemsElegidos) {
				if (deCompetencia.contains(item.id())) {
					columnas.add(new ColumnaReporte(claveReporte(competenciaId, item.id().toString()), item.nombre(),
							competenciaId, permitidas.get(competenciaId), false));
				}
			}
			if (incluirPromedio) {
				columnas.add(new ColumnaReporte(claveReporte(competenciaId, "resultado"), "Promedio del periodo",
						competenciaId, permitidas.get(competenciaId), true));
			}
		}
		boolean promedioGeneral = incluirPromedio && seleccionadas.size() > 1;
		if (promedioGeneral) {
			columnas.add(new ColumnaReporte("resultado_general", "Promedio general", null, null, true));
		}
		List<DetalleReporteFila> filtrados = detalles.stream().filter(d -> seleccionadas.contains(d.competenciaId())
				&& elegidos.contains(d.itemId())).toList();
		Map<UUID, List<DetalleReporteFila>> porEstudiante = filtrados.stream()
				.collect(Collectors.groupingBy(DetalleReporteFila::matriculaId));
		List<CalculoNotas.Rango> rangos = rangosConversion(periodo.fechaInicio());
		List<FilaReporte> filas = repositorio.estudiantes(seccionId).stream().map(estudiante -> {
			List<DetalleReporteFila> lista = porEstudiante.getOrDefault(estudiante.matriculaId(), List.of());
			Map<String, String> valores = new LinkedHashMap<>();
			lista.forEach(d -> valores.put(claveReporte(d.competenciaId(), d.itemId().toString()), valor(d)));
			if (incluirPromedio) {
				for (UUID competenciaId : seleccionadas) {
					List<DetalleReporteFila> deCompetencia = lista.stream()
							.filter(d -> d.competenciaId().equals(competenciaId)).toList();
					registrarResultado(valores, claveReporte(competenciaId, "resultado"),
							resultado(deCompetencia, periodo.reglaNota()), deCompetencia, false, periodo, rangos);
				}
				if (promedioGeneral) {
					registrarResultado(valores, "resultado_general", resultadoDeItemsUnicos(lista, periodo.reglaNota()),
							lista, true, periodo, rangos);
				}
			}
			return new FilaReporte(estudiante.matriculaId(), estudiante.estudiante(), valores);
		}).toList();
		return new ReporteCalificaciones(seleccionadas.size() == 1
				? permitidas.get(seleccionadas.get(0))
				: "Competencias seleccionadas", "Consolidado de notas", columnas, filas);
	}

	private static String claveReporte(UUID competenciaId, String columna) {
		return competenciaId + ":" + columna;
	}

	/** Lo que se muestra debajo de un promedio al que le faltan registros: sin letra ni significado. */
	static final String FALTAN_REGISTROS = "Faltan registros";

	/**
	 * El promedio del periodo es la vista de trabajo del docente. Si falta registrar algún ítem, en vigesimal lo que
	 * falta cuenta como 0 (ver {@link #resultado}); no lleva nivel de logro (una letra se leería como nota) y dice
	 * cuánto está calificado: en ponderado, «Faltan registros (cuentan como 0) · 70 % del peso calificado»; en las
	 * demás reglas, «… · 2 de 3 ítems». La nota oficial de la competencia, en cambio, queda pendiente.
	 *
	 * @param unicos
	 *            el promedio general cuenta cada ítem una vez aunque apunte a varias competencias
	 */
	private static void registrarResultado(Map<String, String> valores, String clave, String resultado,
			List<DetalleReporteFila> detalles, boolean unicos, PeriodoContexto periodo,
			List<CalculoNotas.Rango> rangos) {
		valores.put(clave, resultado);
		List<DetalleReporteFila> cuentan = detalles.stream().filter(DetalleReporteFila::cuenta).toList();
		List<DetalleReporteFila> calificados = cuentan.stream()
				.filter(d -> d.notaNumerica() != null || d.nivelLogro() != null).toList();
		long total = unicos ? cuentan.stream().map(DetalleReporteFila::itemId).distinct().count() : cuentan.size();
		long conNota = unicos
				? calificados.stream().map(DetalleReporteFila::itemId).distinct().count()
				: calificados.size();
		if (conNota == total) {
			valores.put(clave + "_estado", logroResultado(resultado, periodo.escala(), rangos));
			return;
		}
		String respaldo;
		if ("PROMEDIO_PONDERADO".equals(periodo.reglaNota())) {
			// Un ítem compartido suma su peso en cada competencia, igual que en el promedio general.
			BigDecimal peso = suma(cuentan);
			int porcentaje = peso.signum() == 0
					? 0
					: suma(calificados).multiply(BigDecimal.valueOf(100)).divide(peso, 0, RoundingMode.HALF_UP)
							.intValue();
			respaldo = porcentaje + " % del peso calificado";
		} else {
			respaldo = conNota + " de " + total + " ítems";
		}
		// En vigesimal lo que falta cuenta como 0 en este promedio (un piso); en letras no hay 0.
		valores.put(clave + "_estado", FALTAN_REGISTROS
				+ ("VIGESIMAL".equals(periodo.escala()) ? " (cuentan como 0)" : "") + " · " + respaldo);
	}

	private static BigDecimal suma(List<DetalleReporteFila> detalles) {
		return detalles.stream().map(d -> d.pesoPorcentaje() == null ? BigDecimal.ZERO : d.pesoPorcentaje())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static String resultadoDeItemsUnicos(List<DetalleReporteFila> detalles, String regla) {
		Map<UUID, DetalleReporteFila> unicos = new LinkedHashMap<>();
		for (DetalleReporteFila detalle : detalles) {
			unicos.merge(detalle.itemId(), detalle, (anterior, actual) -> !"PROMEDIO_PONDERADO".equals(regla)
					? anterior
					: new DetalleReporteFila(
							anterior.matriculaId(), anterior.estudianteId(), anterior.estudiante(), anterior.itemId(),
							anterior.item(), anterior.competenciaId(), anterior.competencia(),
							anterior.pesoPorcentaje().add(actual.pesoPorcentaje()), anterior.notaNumerica(),
							anterior.nivelLogro(), anterior.evidenciaNombre(), anterior.ordenItem(),
							anterior.noEvaluado(), anterior.itemUsado()));
		}
		return resultado(new ArrayList<>(unicos.values()), regla);
	}

	/**
	 * Nivel de logro del resultado, como se informa en la evaluación por competencias: en vigesimal «A · Logro
	 * esperado» (la nota se redondea al entero, 0,5 sube, y se ubica en la tabla de conversión); en la cualitativa, el
	 * significado del nivel. No hay «aprobado» ni «desaprobado» por competencia.
	 */
	private static String logroResultado(String resultado, String escala, List<CalculoNotas.Rango> rangos) {
		if ("—".equals(resultado)) {
			return CalculoNotas.PENDIENTE;
		}
		if ("VIGESIMAL".equals(escala)) {
			var nota = new CalculoNotas.Nota(BigDecimal.ONE, new BigDecimal(resultado), null, null);
			String nivel = CalculoNotas.nivel(nota, rangos);
			return nivel == null ? CalculoNotas.PENDIENTE : nivel + " · " + CalculoNotas.logro(nota, rangos);
		}
		return CalculoNotas.logro(new CalculoNotas.Nota(BigDecimal.ONE, null, resultado, null), rangos);
	}

	/** Tabla de conversión vigente en la fecha; la escala habitual si todavía no hay ninguna versión. */
	private List<CalculoNotas.Rango> rangosConversion(LocalDate fecha) {
		List<CalculoNotas.Rango> vigentes = repositorio.rangosConversion(fecha);
		return vigentes.isEmpty() ? CalculoNotas.RANGOS_POR_DEFECTO : vigentes;
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

	/**
	 * El promedio del periodo de un estudiante (vista de trabajo del docente). Sin ninguna nota, «—». En la escala
	 * vigesimal, un ítem que cuenta y aún no tiene nota entra como <b>0</b>: así el promedio es un piso que solo puede
	 * subir al registrar lo que falta. En la escala de letras no hay un 0, así que se usan solo los niveles
	 * registrados.
	 * La nota oficial no se calcula así: queda pendiente hasta que no falte nada.
	 */
	private static String resultado(List<DetalleReporteFila> detalles, String regla) {
		List<DetalleReporteFila> cuentan = detalles.stream().filter(DetalleReporteFila::cuenta).toList();
		List<DetalleReporteFila> calificados = cuentan.stream()
				.filter(d -> d.notaNumerica() != null || d.nivelLogro() != null).toList();
		if (calificados.isEmpty()) {
			return "—";
		}
		if (calificados.get(0).notaNumerica() != null) {
			calificados = cuentan.stream().map(ServicioCalificaciones::faltanteComoCero).toList();
			BigDecimal suma;
			BigDecimal divisor;
			if ("PROMEDIO_PONDERADO".equals(regla)) {
				suma = calificados.stream().map(d -> d.notaNumerica().multiply(d.pesoPorcentaje()))
						.reduce(BigDecimal.ZERO, BigDecimal::add);
				divisor = calificados.stream().map(DetalleReporteFila::pesoPorcentaje).reduce(BigDecimal.ZERO,
						BigDecimal::add);
			} else {
				suma = calificados.stream().map(DetalleReporteFila::notaNumerica).reduce(BigDecimal.ZERO,
						BigDecimal::add);
				divisor = BigDecimal.valueOf(calificados.size());
			}
			return suma.divide(divisor, 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
		}
		if ("ULTIMO_NIVEL".equals(regla)) {
			return calificados.stream().max(Comparator.comparing(DetalleReporteFila::ordenItem)).orElseThrow()
					.nivelLogro();
		}
		Map<String, Long> conteo = calificados.stream()
				.collect(Collectors.groupingBy(DetalleReporteFila::nivelLogro, Collectors.counting()));
		return conteo.entrySet().stream().max(Map.Entry.<String, Long>comparingByValue()
				.thenComparing(e -> ORDEN_NIVEL.getOrDefault(e.getKey(), -1))).orElseThrow().getKey();
	}

	private SolicitudItem validarItem(UUID planCursoId, UUID seccionId, UUID periodoId, SolicitudItem solicitud,
			UUID excepto, String reglaNota) {
		boolean ponderado = "PROMEDIO_PONDERADO".equals(reglaNota);
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
			if (ponderado) {
				if (c.pesoPorcentaje() == null || c.pesoPorcentaje().compareTo(new BigDecimal("0.01")) < 0
						|| c.pesoPorcentaje().compareTo(PESO_TOTAL) > 0) {
					throw ExcepcionNegocio.invalido("Ingresa un peso entre 0,01 y 100 % para cada competencia.");
				}
				BigDecimal asignado = repositorio.pesoAsignado(planCursoId, seccionId, periodoId,
						c.competenciaId(), excepto);
				if (asignado.add(c.pesoPorcentaje()).compareTo(PESO_TOTAL) > 0) {
					throw ExcepcionNegocio.invalido("El peso total de la competencia «"
							+ permitidas.get(c.competenciaId()) + "» no puede superar el 100 %.");
				}
			}
		});
		return new SolicitudItem(nombre, descripcion, solicitud.tipo(), solicitud.competencias().stream()
				.map(c -> new SolicitudCompetenciaItem(c.competenciaId(), ponderado ? c.pesoPorcentaje() : null))
				.toList());
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
				fila.notaNumerica(), fila.nivelLogro(), evidencia,
				fila.id() == null ? "PENDIENTE" : fila.noEvaluado() ? "NO_EVALUADO" : "CALIFICADO",
				fila.modificaciones(), fila.noEvaluado(), fila.motivoNoEvaluado());
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

	private static DetalleReporteFila faltanteComoCero(DetalleReporteFila d) {
		if (d.notaNumerica() != null || d.nivelLogro() != null) {
			return d;
		}
		return new DetalleReporteFila(d.matriculaId(), d.estudianteId(), d.estudiante(), d.itemId(), d.item(),
				d.competenciaId(), d.competencia(), d.pesoPorcentaje(), BigDecimal.ZERO, null, d.evidenciaNombre(),
				d.ordenItem(), d.noEvaluado(), d.itemUsado());
	}

	/** Lo que se muestra de una celda que aún no tiene nota pero cuenta: falta registrarla. */
	static final String POR_REGISTRAR = "Por registrar";

	/**
	 * Valor de una celda del consolidado: la nota, «NE» si es «No evaluado», «Por registrar» si falta y el ítem cuenta,
	 * o
	 * «—» si nadie tiene registrado el ítem todavía.
	 */
	private static String valor(DetalleReporteFila fila) {
		if (fila.noEvaluado()) {
			return "NE";
		}
		if (fila.notaNumerica() != null) {
			return fila.notaNumerica().stripTrailingZeros().toPlainString();
		}
		if (fila.nivelLogro() != null) {
			return fila.nivelLogro();
		}
		return fila.itemUsado() ? POR_REGISTRAR : "—";
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
