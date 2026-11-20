package com.galiledu.academico.aplicacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.academico.aplicacion.DtosCalificaciones.ColumnaReporte;
import com.galiledu.academico.aplicacion.DtosCalificaciones.FilaReporte;
import com.galiledu.academico.aplicacion.DtosCalificaciones.ReporteCalificaciones;
import com.galiledu.academico.aplicacion.DtosReportesNotas.AnioReporte;
import com.galiledu.academico.aplicacion.DtosReportesNotas.AreaInstitucional;
import com.galiledu.academico.aplicacion.DtosReportesNotas.CeldaResumen;
import com.galiledu.academico.aplicacion.DtosReportesNotas.CursoOpcion;
import com.galiledu.academico.aplicacion.DtosReportesNotas.Distribucion;
import com.galiledu.academico.aplicacion.DtosReportesNotas.FilaInstitucional;
import com.galiledu.academico.aplicacion.DtosReportesNotas.FilaReporteGrado;
import com.galiledu.academico.aplicacion.DtosReportesNotas.GradoOpcion;
import com.galiledu.academico.aplicacion.DtosReportesNotas.NotasSeccion;
import com.galiledu.academico.aplicacion.DtosReportesNotas.PeriodoOpcion;
import com.galiledu.academico.aplicacion.DtosReportesNotas.ReporteGrado;
import com.galiledu.academico.aplicacion.DtosReportesNotas.ReporteInstitucional;
import com.galiledu.academico.aplicacion.DtosReportesNotas.SeccionReporte;
import com.galiledu.academico.dominio.CalculoNotas;
import com.galiledu.academico.dominio.CalculoNotas.Aporte;
import com.galiledu.academico.dominio.CalculoNotas.AporteFinal;
import com.galiledu.academico.dominio.CalculoNotas.Avance;
import com.galiledu.academico.dominio.CalculoNotas.Rango;
import com.galiledu.academico.dominio.CalculoNotas.ResultadoCompetencia;
import com.galiledu.academico.dominio.CalculoNotas.ResultadoPeriodo;
import com.galiledu.academico.dominio.ResumenNotas;
import com.galiledu.academico.infraestructura.ArchivoReporteCalificaciones;
import com.galiledu.academico.infraestructura.ArchivoReporteCalificaciones.TablaPdf;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.CompetenciaFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.CursoFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.PeriodoFila;
import com.galiledu.academico.infraestructura.RepositorioPesosPlan.PesoFila;
import com.galiledu.academico.infraestructura.RepositorioReportesNotas;
import com.galiledu.academico.infraestructura.RepositorioReportesNotas.AnioFila;
import com.galiledu.academico.infraestructura.RepositorioReportesNotas.GradoCursoFila;
import com.galiledu.academico.infraestructura.RepositorioReportesNotas.PeriodoAnioFila;
import com.galiledu.academico.infraestructura.RepositorioReportesNotas.SeccionContextoFila;
import com.galiledu.academico.infraestructura.RepositorioReportesNotas.SeccionFila;
import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * Reportes de notas para el Coordinador (HU-63, por curso y grado) y para el Coordinador y el Administrador (HU-64,
 * institucional). Usan la nota oficial de cada competencia de {@link ServicioNotasCompetencia}; con un curso elegido,
 * lo que ese curso aporta a cada competencia.
 */
@Service
public class ServicioReportesNotas {
	static final String NOTA_FINAL = "Nota final";

	private final RepositorioReportesNotas repositorio;
	private final ServicioNotasCompetencia notas;

	public ServicioReportesNotas(RepositorioReportesNotas repositorio, ServicioNotasCompetencia notas) {
		this.repositorio = repositorio;
		this.notas = notas;
	}

	@Transactional(readOnly = true)
	public List<AnioReporte> anios() {
		return repositorio.anios().stream().map(anio -> {
			Map<String, List<GradoCursoFila>> porGrado = repositorio.gradosYCursos(anio.id()).stream()
					.collect(Collectors.groupingBy(GradoCursoFila::gradoCodigo, LinkedHashMap::new,
							Collectors.toList()));
			List<GradoOpcion> grados = porGrado.values().stream()
					.map(cursos -> new GradoOpcion(cursos.get(0).gradoCodigo(), cursos.get(0).grado(),
							cursos.stream().map(c -> new CursoOpcion(c.planCursoId(), c.curso())).toList()))
					.toList();
			return new AnioReporte(anio.id(), anio.anio(), anio.estado(), anio.escala(), anio.reglaNota(),
					anio.consolidacion(),
					repositorio.periodos(anio.id()).stream()
							.map(p -> new PeriodoOpcion(p.id(), p.numero(), p.nombre())).toList(),
					grados);
		}).toList();
	}

	/**
	 * HU-63: una fila por competencia y una columna por sección del grado, más el grado completo. Sin curso, la nota
	 * oficial de la competencia; con curso, el aporte de ese curso. Sin periodo, la nota final del año.
	 */
	@Transactional(readOnly = true)
	public ReporteGrado reporteGrado(UUID anioEscolarId, String gradoCodigo, UUID planCursoId, UUID periodoId) {
		Contexto contexto = contexto(anioEscolarId, periodoId);
		GradoCursoFila grado = repositorio.gradosYCursos(anioEscolarId).stream()
				.filter(g -> g.gradoCodigo().equals(gradoCodigo)).findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El grado no tiene plan de evaluación en el año."));
		String curso = null;
		if (planCursoId != null) {
			curso = repositorio.gradosYCursos(anioEscolarId).stream()
					.filter(g -> g.gradoCodigo().equals(gradoCodigo) && g.planCursoId().equals(planCursoId))
					.map(GradoCursoFila::curso).findFirst()
					.orElseThrow(() -> ExcepcionNegocio.invalido("El curso no pertenece al plan del grado."));
		}
		Map<UUID, List<PesoFila>> cursosPorCompetencia = notas.cursosPorCompetencia(anioEscolarId, gradoCodigo);
		Map<UUID, List<PesoFila>> competencias = planCursoId == null
				? cursosPorCompetencia
				: cursosPorCompetencia.entrySet().stream()
						.filter(e -> e.getValue().stream().anyMatch(f -> f.planCursoId().equals(planCursoId)))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a,
								LinkedHashMap::new));
		List<UUID> idsCompetencias = new ArrayList<>(competencias.keySet());

		List<SeccionFila> secciones = repositorio.secciones(anioEscolarId, gradoCodigo);
		List<SeccionReporte> seccionesReporte = new ArrayList<>();
		// Por competencia, las notas de cada sección (en orden) para resumirlas por sección y en el grado.
		Map<UUID, List<List<ResumenNotas.Valor>>> porCompetencia = new LinkedHashMap<>();
		idsCompetencias.forEach(id -> porCompetencia.put(id, new ArrayList<>()));
		for (SeccionFila seccion : secciones) {
			Map<UUID, Map<UUID, ResultadoCompetencia>> resultados = notas.resultadosSeccion(seccion.id(),
					contexto.idsPeriodos(), cursosPorCompetencia, idsCompetencias, contexto.anio().reglaNota(),
					contexto.anio().consolidacion());
			seccionesReporte.add(new SeccionReporte(seccion.id(), seccion.nombre(), resultados.size()));
			for (UUID competenciaId : idsCompetencias) {
				porCompetencia.get(competenciaId).add(resultados.values().stream()
						.map(r -> nota(r.get(competenciaId), contexto, planCursoId)).toList());
			}
		}

		boolean usaPesos = CalculoNotas.usaPesosDeCurso(contexto.anio().reglaNota());
		List<FilaReporteGrado> filas = competencias.entrySet().stream().map(e -> {
			List<List<ResumenNotas.Valor>> porSeccion = porCompetencia.get(e.getKey());
			PesoFila primera = e.getValue().get(0);
			boolean calculable = planCursoId != null || !usaPesos
					|| CalculoNotas.pesosCompletos(e.getValue().stream().map(PesoFila::pesoCurso).toList());
			return new FilaReporteGrado(e.getKey(), primera.competencia(), primera.area(), calculable,
					porSeccion.stream().map(n -> celda(n, contexto.rangos())).toList(),
					celda(porSeccion.stream().flatMap(List::stream).toList(), contexto.rangos()));
		}).toList();
		return new ReporteGrado(contexto.anio().anio(), grado.grado(), curso, contexto.periodo(),
				contexto.anio().escala(), contexto.anio().reglaNota(), seccionesReporte, filas);
	}

	/**
	 * HU-64: distribución de niveles de logro de todo el colegio: por sección, por grado, por área y total. Cada nota
	 * oficial de competencia de cada estudiante cuenta una vez.
	 */
	@Transactional(readOnly = true)
	public ReporteInstitucional reporteInstitucional(UUID anioEscolarId, UUID periodoId) {
		Contexto contexto = contexto(anioEscolarId, periodoId);
		Map<String, String> grados = repositorio.gradosYCursos(anioEscolarId).stream()
				.collect(Collectors.toMap(GradoCursoFila::gradoCodigo, GradoCursoFila::grado, (a, b) -> a,
						LinkedHashMap::new));
		List<FilaInstitucional> filas = new ArrayList<>();
		Map<String, ResumenNotas.Distribucion> porArea = new LinkedHashMap<>();
		ResumenNotas.Distribucion total = ResumenNotas.Distribucion.VACIA;
		int estudiantes = 0;
		for (Map.Entry<String, String> grado : grados.entrySet()) {
			Map<UUID, List<PesoFila>> cursosPorCompetencia = notas.cursosPorCompetencia(anioEscolarId,
					grado.getKey());
			List<UUID> competencias = new ArrayList<>(cursosPorCompetencia.keySet());
			ResumenNotas.Distribucion delGrado = ResumenNotas.Distribucion.VACIA;
			int estudiantesGrado = 0;
			List<FilaInstitucional> deSecciones = new ArrayList<>();
			for (SeccionFila seccion : repositorio.secciones(anioEscolarId, grado.getKey())) {
				Map<UUID, Map<UUID, ResultadoCompetencia>> resultados = notas.resultadosSeccion(seccion.id(),
						contexto.idsPeriodos(), cursosPorCompetencia, competencias, contexto.anio().reglaNota(),
						contexto.anio().consolidacion());
				ResumenNotas.Distribucion deSeccion = ResumenNotas.Distribucion.VACIA;
				for (UUID competenciaId : competencias) {
					ResumenNotas.Distribucion d = ResumenNotas.distribucion(resultados.values().stream()
							.map(r -> nota(r.get(competenciaId), contexto, null)).toList(), contexto.rangos());
					deSeccion = deSeccion.mas(d);
					porArea.merge(cursosPorCompetencia.get(competenciaId).get(0).area(), d,
							ResumenNotas.Distribucion::mas);
				}
				deSecciones.add(new FilaInstitucional(grado.getKey(), grado.getValue(), seccion.nombre(),
						resultados.size(), distribucion(deSeccion)));
				delGrado = delGrado.mas(deSeccion);
				estudiantesGrado += resultados.size();
			}
			if (deSecciones.isEmpty()) {
				continue;
			}
			filas.addAll(deSecciones);
			filas.add(new FilaInstitucional(grado.getKey(), grado.getValue(), null, estudiantesGrado,
					distribucion(delGrado)));
			total = total.mas(delGrado);
			estudiantes += estudiantesGrado;
		}
		return new ReporteInstitucional(contexto.anio().anio(), contexto.periodo(), contexto.anio().escala(),
				estudiantes, filas,
				porArea.entrySet().stream().map(e -> new AreaInstitucional(e.getKey(), distribucion(e.getValue())))
						.toList(),
				distribucion(total));
	}

	/**
	 * Detalle de una sección (desde HU-63, solo el Coordinador): por estudiante, la nota oficial de cada competencia
	 * del
	 * grado en cada periodo y la final. Solo lectura. 404 si la sección no existe o su año no ha empezado.
	 */
	@Transactional(readOnly = true)
	public NotasSeccion notasSeccion(UUID seccionId) {
		SeccionContextoFila s = repositorio.seccion(seccionId).orElseThrow(
				() -> ExcepcionNegocio.noEncontrado("La sección no existe o su año escolar aún no ha empezado."));
		CursoFila contexto = new CursoFila(null, s.id(), s.anioEscolarId(), s.anio(), s.estadoAnio(), null, null,
				s.gradoCodigo(), s.grado(), s.seccion(), s.escala(), s.reglaNota(), s.consolidacion());
		List<PeriodoFila> periodos = repositorio.periodos(s.anioEscolarId()).stream()
				.map(p -> new PeriodoFila(p.id(), p.numero(), p.nombre(), p.fechaInicio(), p.fechaFin(), p.fechaFin(),
						0, 0))
				.toList();
		List<CompetenciaFila> competencias = notas.cursosPorCompetencia(s.anioEscolarId(), s.gradoCodigo())
				.entrySet().stream().map(e -> new CompetenciaFila(e.getKey(), e.getValue().get(0).competencia()))
				.toList();
		return new NotasSeccion(s.id(), s.anio(), s.grado(), s.seccion(),
				notas.calcular(contexto, s.id(), periodos, competencias));
	}

	/** El año, los ids de sus periodos, el periodo elegido (o null = nota final) y la tabla de conversión a usar. */
	record Contexto(AnioFila anio, List<UUID> idsPeriodos, int indicePeriodo, String periodo, List<Rango> rangos) {
	}

	Contexto contexto(UUID anioEscolarId, UUID periodoId) {
		AnioFila anio = repositorio.anios().stream().filter(a -> a.id().equals(anioEscolarId)).findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El año escolar no existe o aún no ha empezado."));
		List<PeriodoAnioFila> periodos = repositorio.periodos(anioEscolarId);
		if (periodos.isEmpty()) {
			throw ExcepcionNegocio.conflicto("El año escolar no tiene periodos.");
		}
		List<UUID> ids = periodos.stream().map(PeriodoAnioFila::id).toList();
		if (periodoId == null) {
			// La nota final usa la tabla de conversión vigente al inicio del último periodo.
			return new Contexto(anio, ids, -1, NOTA_FINAL,
					notas.rangos(periodos.get(periodos.size() - 1).fechaInicio()));
		}
		int indice = ids.indexOf(periodoId);
		if (indice < 0) {
			throw ExcepcionNegocio.invalido("El periodo no pertenece al año escolar.");
		}
		PeriodoAnioFila periodo = periodos.get(indice);
		return new Contexto(anio, ids, indice, periodo.nombre(), notas.rangos(periodo.fechaInicio()));
	}

	/**
	 * La nota de un estudiante en la competencia según el reporte: la oficial o, con un curso, su aporte; en el periodo
	 * elegido o la final (para el aporte del curso, sus periodos se consolidan con la regla del año). Si falta
	 * registrar
	 * algún ítem, queda pendiente: una nota oficial existe solo cuando está completa.
	 */
	static ResumenNotas.Valor nota(ResultadoCompetencia resultado, Contexto contexto, UUID planCursoId) {
		if (resultado == null) {
			return ResumenNotas.Valor.SIN_NOTA;
		}
		if (planCursoId == null) {
			if (contexto.indicePeriodo() < 0) {
				return new ResumenNotas.Valor(resultado.notaFinal(), resultado.pendiente());
			}
			ResultadoPeriodo periodo = resultado.periodos().get(contexto.indicePeriodo());
			return new ResumenNotas.Valor(periodo.nota(), periodo.pendiente());
		}
		Function<ResultadoPeriodo, Aporte> aporte = p -> p.aportes().getOrDefault(planCursoId,
				new Aporte(Optional.empty(), Avance.VACIO));
		if (contexto.indicePeriodo() >= 0) {
			Aporte delPeriodo = aporte.apply(resultado.periodos().get(contexto.indicePeriodo()));
			return new ResumenNotas.Valor(delPeriodo.nota(), delPeriodo.conteo().pendiente());
		}
		AporteFinal aporteFinal = CalculoNotas.aporteFinal(resultado.periodos().stream().map(aporte).toList(),
				contexto.anio().consolidacion(), contexto.anio().reglaNota());
		return new ResumenNotas.Valor(aporteFinal.nota(), aporteFinal.pendiente());
	}

	private static CeldaResumen celda(List<ResumenNotas.Valor> notas, List<Rango> rangos) {
		ResumenNotas.Celda c = ResumenNotas.celda(notas, rangos);
		return new CeldaResumen(c.promedio(), c.nivel(), c.logro(), c.conNota(), c.logroEsperado(),
				c.porcentajeLogro(), c.pendientes());
	}

	private static Distribucion distribucion(ResumenNotas.Distribucion d) {
		return new Distribucion(d.ad(), d.a(), d.b(), d.c(), d.pendientes(), d.sinNotas(), d.porcentajeLogro());
	}

	// ---------- PDF ----------

	/** HU-63 en PDF: la misma tabla, una columna por sección y el grado completo. */
	@Transactional(readOnly = true)
	public byte[] pdfGrado(UUID anioEscolarId, String gradoCodigo, UUID planCursoId, UUID periodoId) {
		ReporteGrado reporte = reporteGrado(anioEscolarId, gradoCodigo, planCursoId, periodoId);
		String grupo = reporte.curso() == null ? "Nota oficial de la competencia" : "Aporte de " + reporte.curso();
		List<ColumnaReporte> columnas = new ArrayList<>();
		reporte.secciones().forEach(s -> columnas
				.add(new ColumnaReporte(s.id().toString(), "Sección " + s.nombre(), null, null, true)));
		columnas.add(new ColumnaReporte("general", "Todo el grado", null, null, true));
		List<FilaReporte> filas = reporte.filas().stream().map(f -> {
			Map<String, String> valores = new LinkedHashMap<>();
			for (int i = 0; i < reporte.secciones().size(); i++) {
				valorCelda(valores, reporte.secciones().get(i).id().toString(), f.secciones().get(i));
			}
			valorCelda(valores, "general", f.general());
			return new FilaReporte(f.competenciaId(), f.competencia(), valores);
		}).toList();
		String contexto = reporte.grado() + (reporte.curso() == null ? "" : " · " + reporte.curso())
				+ " · Año escolar " + reporte.anio() + " · " + reporte.periodo();
		String parametros = parametros(reporte.escala(), reporte.reglaNota())
				+ " · Promedio de los estudiantes con nota (en letras, el nivel más frecuente) y su nivel de logro."
				+ " % = estudiantes con A o AD. Pendientes: estudiantes sin nota porque falta registrar algún ítem.";
		return ArchivoReporteCalificaciones.pdfResumen("Reporte de notas por curso y grado", "Competencia",
				"Notas calculadas al generar el reporte. Solo cuentan las notas completas. —: sin notas todavía.",
				"El grado no tiene competencias.",
				reporte.escala(), List.of(new TablaPdf("Reporte de notas por curso y grado", contexto, parametros,
						new ReporteCalificaciones(grupo, "Reporte por curso y grado", columnas, filas))));
	}

	/** HU-64 en PDF: secciones y grados, y luego las áreas. */
	@Transactional(readOnly = true)
	public byte[] pdfInstitucional(UUID anioEscolarId, UUID periodoId) {
		ReporteInstitucional reporte = reporteInstitucional(anioEscolarId, periodoId);
		List<FilaReporte> filas = new ArrayList<>(reporte.filas().stream()
				.map(f -> fila(f.seccion() == null ? f.grado() + " (total)" : f.grado() + " · Sección " + f.seccion(),
						f.estudiantes(), f.distribucion()))
				.toList());
		filas.add(fila("Todo el colegio", reporte.estudiantes(), reporte.total()));
		String contexto = "Año escolar " + reporte.anio() + " · " + reporte.periodo() + " · " + reporte.estudiantes()
				+ " estudiantes evaluados (grados con plan de evaluación)";
		String parametros = "Cada nota oficial de competencia de cada estudiante cuenta una vez."
				+ " AD: logro destacado · A: logro esperado · B: en proceso · C: en inicio · % = notas con A o AD.";
		List<TablaPdf> tablas = List.of(
				new TablaPdf("Reporte institucional de notas", contexto, parametros,
						new ReporteCalificaciones("Notas por nivel de logro", "Por grado y sección",
								columnasDistribucion(true), filas)),
				new TablaPdf("Reporte institucional por área", contexto, parametros,
						new ReporteCalificaciones("Notas por nivel de logro", "Por área", columnasDistribucion(false),
								reporte.areas().stream().map(a -> fila(a.area(), null, a.distribucion())).toList())));
		return ArchivoReporteCalificaciones.pdfResumen("Reporte institucional de notas", "Grado y sección",
				"Notas calculadas al generar el reporte. Pendientes: falta registrar algún ítem. Sin notas: aún no hay"
						+ " ítems que cuenten.",
				// Son conteos, no notas: sin el sufijo «/20» de la escala vigesimal.
				"No hay secciones con estudiantes.", "CONTEO", tablas);
	}

	private static void valorCelda(Map<String, String> valores, String clave, CeldaResumen celda) {
		valores.put(clave, celda.promedio());
		String estado = celda.porcentajeLogro() == null
				? celda.logro()
				: celda.logro() + " · " + celda.porcentajeLogro() + " %";
		valores.put(clave + "_estado",
				celda.pendientes() > 0 ? estado + " · " + celda.pendientes() + " pend." : estado);
	}

	private static List<ColumnaReporte> columnasDistribucion(boolean conEstudiantes) {
		List<ColumnaReporte> columnas = new ArrayList<>();
		if (conEstudiantes) {
			columnas.add(new ColumnaReporte("estudiantes", "Estudiantes", null, null, false));
		}
		for (String nivel : List.of("AD", "A", "B", "C", "Pendientes", "Sin notas", "% A o AD")) {
			columnas.add(new ColumnaReporte(nivel, nivel, null, null, false));
		}
		return columnas;
	}

	private static FilaReporte fila(String nombre, Integer estudiantes, Distribucion d) {
		Map<String, String> valores = new LinkedHashMap<>();
		if (estudiantes != null) {
			valores.put("estudiantes", String.valueOf(estudiantes));
		}
		valores.put("AD", String.valueOf(d.ad()));
		valores.put("A", String.valueOf(d.a()));
		valores.put("B", String.valueOf(d.b()));
		valores.put("C", String.valueOf(d.c()));
		valores.put("Pendientes", String.valueOf(d.pendientes()));
		valores.put("Sin notas", String.valueOf(d.sinNotas()));
		valores.put("% A o AD", d.porcentajeLogro() == null ? "—" : d.porcentajeLogro() + " %");
		return new FilaReporte(null, nombre, valores);
	}

	static String parametros(String escala, String reglaNota) {
		String regla = switch (reglaNota) {
			case "PROMEDIO_PONDERADO" -> "Promedio ponderado";
			case "PROMEDIO_SIMPLE" -> "Promedio simple";
			case "NIVEL_MAS_FRECUENTE" -> "Nivel más frecuente";
			case "ULTIMO_NIVEL" -> "Último nivel";
			default -> reglaNota;
		};
		return "Escala: " + ("VIGESIMAL".equals(escala) ? "0 a 20" : "AD, A, B, C") + " · Regla del año escolar: "
				+ regla;
	}
}
