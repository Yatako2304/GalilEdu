package com.galiledu.academico.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.academico.aplicacion.DtosCalificaciones.AporteCurso;
import com.galiledu.academico.aplicacion.DtosCalificaciones.ColumnaReporte;
import com.galiledu.academico.aplicacion.DtosCalificaciones.CompetenciaNotas;
import com.galiledu.academico.aplicacion.DtosCalificaciones.CursoCompetencia;
import com.galiledu.academico.aplicacion.DtosCalificaciones.FilaNotasCompetencia;
import com.galiledu.academico.aplicacion.DtosCalificaciones.FilaReporte;
import com.galiledu.academico.aplicacion.DtosCalificaciones.NotaPeriodoCompetencia;
import com.galiledu.academico.aplicacion.DtosCalificaciones.NotaResultado;
import com.galiledu.academico.aplicacion.DtosCalificaciones.NotasCompetencias;
import com.galiledu.academico.aplicacion.DtosCalificaciones.NotasEstudianteCompetencia;
import com.galiledu.academico.aplicacion.DtosCalificaciones.PeriodoNotas;
import com.galiledu.academico.aplicacion.DtosCalificaciones.ReporteCalificaciones;
import com.galiledu.academico.dominio.CalculoNotas;
import com.galiledu.academico.dominio.CalculoNotas.Aporte;
import com.galiledu.academico.dominio.CalculoNotas.AporteFinal;
import com.galiledu.academico.dominio.CalculoNotas.Avance;
import com.galiledu.academico.dominio.CalculoNotas.Calificacion;
import com.galiledu.academico.dominio.CalculoNotas.CursoPeso;
import com.galiledu.academico.dominio.CalculoNotas.Nota;
import com.galiledu.academico.dominio.CalculoNotas.Rango;
import com.galiledu.academico.dominio.CalculoNotas.ResultadoCompetencia;
import com.galiledu.academico.dominio.CalculoNotas.ResultadoPeriodo;
import com.galiledu.academico.infraestructura.ArchivoReporteCalificaciones;
import com.galiledu.academico.infraestructura.ArchivoReporteCalificaciones.TablaPdf;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.CalificacionCompetenciaFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.CompetenciaFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.CursoFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.PeriodoFila;
import com.galiledu.academico.infraestructura.RepositorioPesosPlan;
import com.galiledu.academico.infraestructura.RepositorioPesosPlan.PesoFila;
import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * Nota oficial de cada competencia en una sección: una por periodo y una final, combinando todos los cursos del grado
 * que la evalúan (ver {@link CalculoNotas}). La nota se calcula al consultarla y no se guarda.
 */
@Service
public class ServicioNotasCompetencia {
	private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final RepositorioCalificaciones repositorio;
	private final RepositorioPesosPlan pesos;
	private final Clock reloj;

	public ServicioNotasCompetencia(RepositorioCalificaciones repositorio, RepositorioPesosPlan pesos, Clock reloj) {
		this.repositorio = repositorio;
		this.pesos = pesos;
		this.reloj = reloj;
	}

	/**
	 * Para el docente de un curso y sección: las competencias que evalúa su curso (todas o las elegidas). Ve el aporte
	 * de los cursos de sus colegas en la sección, no sus ítems.
	 */
	@Transactional(readOnly = true)
	public NotasCompetencias notasDocente(UUID docenteId, UUID planCursoId, UUID seccionId,
			List<UUID> competenciaIds) {
		CursoFila curso = cursoDocente(docenteId, planCursoId, seccionId);
		return calcular(curso, seccionId, repositorio.periodos(planCursoId, seccionId),
				elegidas(planCursoId, competenciaIds));
	}

	private CursoFila cursoDocente(UUID docenteId, UUID planCursoId, UUID seccionId) {
		return repositorio.cursos(docenteId).stream()
				.filter(c -> c.planCursoId().equals(planCursoId) && c.seccionId().equals(seccionId)).findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El curso no está asignado al docente."));
	}

	/** Las competencias elegidas del plan del curso, en el orden del plan; sin elegir, todas. */
	private List<CompetenciaFila> elegidas(UUID planCursoId, List<UUID> competenciaIds) {
		List<CompetenciaFila> delCurso = repositorio.competencias(planCursoId);
		Set<UUID> permitidas = delCurso.stream().map(CompetenciaFila::id).collect(Collectors.toSet());
		if (!permitidas.containsAll(competenciaIds)) {
			throw ExcepcionNegocio.invalido("Una competencia no pertenece al plan del curso.");
		}
		return competenciaIds.isEmpty()
				? delCurso
				: delCurso.stream().filter(c -> competenciaIds.contains(c.id())).toList();
	}

	NotasCompetencias calcular(CursoFila curso, UUID seccionId, List<PeriodoFila> periodos,
			List<CompetenciaFila> elegidas) {
		String regla = curso.reglaNota();
		boolean usaPesos = CalculoNotas.usaPesosDeCurso(regla);
		Map<UUID, List<PesoFila>> cursosPorCompetencia = cursosPorCompetencia(curso.anioEscolarId(),
				curso.gradoCodigo());
		List<CompetenciaNotas> competencias = elegidas.stream().map(c -> {
			List<PesoFila> cursos = cursosPorCompetencia.getOrDefault(c.id(), List.of());
			return new CompetenciaNotas(c.id(), c.nombre(), usaPesos,
					CalculoNotas.pesosCompletos(cursos.stream().map(PesoFila::pesoCurso).toList()),
					cursos.stream().map(f -> new CursoCompetencia(f.planCursoId(), f.curso(), f.pesoCurso())).toList());
		}).toList();

		List<UUID> idsPeriodos = periodos.stream().map(PeriodoFila::id).toList();
		// Tabla de conversión vigente al inicio de cada periodo; la nota final usa la del último periodo.
		Map<UUID, List<Rango>> rangos = new LinkedHashMap<>();
		periodos.forEach(p -> rangos.put(p.id(), rangos(p.fechaInicio())));
		List<Rango> rangosFinal = periodos.isEmpty()
				? CalculoNotas.RANGOS_POR_DEFECTO
				: rangos.get(periodos.get(periodos.size() - 1).id());

		Map<UUID, Map<UUID, ResultadoCompetencia>> resultados = resultadosSeccion(seccionId, idsPeriodos,
				cursosPorCompetencia, elegidas.stream().map(CompetenciaFila::id).toList(), regla,
				curso.consolidacion());

		List<FilaNotasCompetencia> filas = repositorio.estudiantes(seccionId).stream().map(estudiante -> {
			Map<UUID, ResultadoCompetencia> suyos = resultados.getOrDefault(estudiante.matriculaId(), Map.of());
			List<NotasEstudianteCompetencia> notas = new ArrayList<>();
			for (CompetenciaNotas competencia : competencias) {
				ResultadoCompetencia resultado = suyos.get(competencia.competenciaId());
				notas.add(new NotasEstudianteCompetencia(competencia.competenciaId(),
						resultado.periodos().stream().map(p -> periodo(p, rangos.get(p.periodoId()))).toList(),
						resultado(resultado.notaFinal(), resultado.avance(), rangosFinal)));
			}
			return new FilaNotasCompetencia(estudiante.matriculaId(), estudiante.estudiante(), notas);
		}).toList();

		return new NotasCompetencias(curso.escala(), regla, curso.consolidacion(),
				periodos.stream().map(p -> new PeriodoNotas(p.id(), p.numero(), p.nombre())).toList(), competencias,
				filas);
	}

	/** Cursos del grado que evalúan cada competencia, con su peso del plan, en el orden del catálogo. */
	Map<UUID, List<PesoFila>> cursosPorCompetencia(UUID anioEscolarId, String gradoCodigo) {
		return pesos.delGrado(anioEscolarId, gradoCodigo).stream()
				.collect(Collectors.groupingBy(PesoFila::competenciaId, LinkedHashMap::new, Collectors.toList()));
	}

	/**
	 * La nota de cada competencia para cada estudiante matriculado en la sección, por matrícula y competencia. Es el
	 * mismo cálculo de la tarjeta del docente; lo usan también los reportes (HU-63, HU-64) y el apoderado (HU-69).
	 */
	Map<UUID, Map<UUID, ResultadoCompetencia>> resultadosSeccion(UUID seccionId, List<UUID> idsPeriodos,
			Map<UUID, List<PesoFila>> cursosPorCompetencia, List<UUID> competenciaIds, String regla,
			String consolidacion) {
		Map<UUID, Map<UUID, List<CalificacionCompetenciaFila>>> porEstudiante = repositorio
				.calificacionesCompetencias(seccionId, competenciaIds).stream()
				.collect(Collectors.groupingBy(CalificacionCompetenciaFila::matriculaId,
						Collectors.groupingBy(CalificacionCompetenciaFila::competenciaId)));
		Map<UUID, Map<UUID, ResultadoCompetencia>> resultados = new LinkedHashMap<>();
		for (var estudiante : repositorio.estudiantes(seccionId)) {
			Map<UUID, List<CalificacionCompetenciaFila>> suyas = porEstudiante.getOrDefault(estudiante.matriculaId(),
					Map.of());
			Map<UUID, ResultadoCompetencia> porCompetencia = new LinkedHashMap<>();
			for (UUID competenciaId : competenciaIds) {
				porCompetencia.put(competenciaId, CalculoNotas.notaCompetencia(idsPeriodos,
						cursos(cursosPorCompetencia.getOrDefault(competenciaId, List.of())),
						calificaciones(suyas.getOrDefault(competenciaId, List.of())), regla, consolidacion));
			}
			resultados.put(estudiante.matriculaId(), porCompetencia);
		}
		return resultados;
	}

	static List<CursoPeso> cursos(List<PesoFila> filas) {
		return filas.stream().map(f -> new CursoPeso(f.planCursoId(), f.pesoCurso())).toList();
	}

	private static List<Calificacion> calificaciones(List<CalificacionCompetenciaFila> filas) {
		return filas.stream().map(f -> new Calificacion(f.planCursoId(), f.periodoId(), f.pesoPorcentaje(),
				f.notaNumerica(), f.nivelLogro(), f.ordenItem())).toList();
	}

	/** Tabla de conversión vigente en la fecha; la escala habitual de MINEDU si todavía no hay ninguna versión. */
	List<Rango> rangos(LocalDate fecha) {
		List<Rango> vigentes = repositorio.rangosConversion(fecha);
		return vigentes.isEmpty() ? CalculoNotas.RANGOS_POR_DEFECTO : vigentes;
	}

	static NotaPeriodoCompetencia periodo(ResultadoPeriodo periodo, List<Rango> rangos) {
		NotaResultado nota = resultado(periodo.nota(), periodo.avance(), rangos);
		List<AporteCurso> aportes = periodo.aportes().entrySet().stream().map(e -> aporte(e.getKey(), e.getValue()))
				.toList();
		return new NotaPeriodoCompetencia(periodo.periodoId(), nota.nota(), nota.nivel(), nota.logro(),
				nota.pendiente(), nota.calificadas(), nota.total(), aportes);
	}

	/** El aporte de un curso tal como lo ve el docente: el promedio de lo registrado y cuánto falta. */
	static AporteCurso aporte(UUID planCursoId, Aporte aporte) {
		return new AporteCurso(planCursoId, aporte.avance().map(CalculoNotas::texto).orElse("—"),
				aporte.conteo().pendiente(), aporte.conteo().calificadas(), aporte.conteo().total());
	}

	/**
	 * Una nota oficial para mostrar. Si falta registrar algún ítem queda pendiente, sin número ni letra, con cuántos
	 * ítems ya tienen nota: una nota oficial existe solo cuando está completa.
	 */
	static NotaResultado resultado(Optional<Nota> nota, Avance avance, List<Rango> rangos) {
		if (avance.pendiente() || nota.isEmpty()) {
			return new NotaResultado("—", null, CalculoNotas.PENDIENTE, avance.pendiente(), avance.calificadas(),
					avance.total());
		}
		Nota n = nota.get();
		return new NotaResultado(CalculoNotas.texto(n), CalculoNotas.nivel(n, rangos), CalculoNotas.logro(n, rangos),
				false, avance.calificadas(), avance.total());
	}

	/** «Pendiente · 3 de 4 ítems»: lo que se muestra de una nota que espera registros. */
	static String pendiente(NotaResultado nota) {
		return CalculoNotas.PENDIENTE + " · " + nota.calificadas() + " de " + nota.total() + " ítems";
	}

	/**
	 * La nota oficial de cada competencia en <b>un</b> periodo, para el final del PDF del consolidado de ese periodo:
	 * una
	 * columna por competencia con el nivel de logro y la nota (p. ej. «A · 13,6/20») y su significado. El PDF de un
	 * periodo no trae periodos que todavía no terminan ni la nota del año: esa va en el resumen del año.
	 */
	@Transactional(readOnly = true)
	public ReporteCalificaciones reporteNotasPeriodo(UUID docenteId, UUID planCursoId, UUID seccionId, UUID periodoId,
			List<UUID> competenciaIds) {
		NotasCompetencias notas = notasDocente(docenteId, planCursoId, seccionId, competenciaIds);
		int indice = notas.periodos().stream().map(PeriodoNotas::id).toList().indexOf(periodoId);
		if (indice < 0) {
			throw ExcepcionNegocio.noEncontrado("El periodo no pertenece al año del curso.");
		}
		PeriodoNotas periodo = notas.periodos().get(indice);
		boolean vigesimal = "VIGESIMAL".equals(notas.escala());
		List<ColumnaReporte> columnas = notas.competencias().stream()
				.map(c -> new ColumnaReporte(c.competenciaId() + ":" + periodo.id(), periodo.nombre(),
						c.competenciaId(), c.competencia(), true))
				.toList();
		List<FilaReporte> filas = notas.filas().stream().map(fila -> {
			Map<String, String> valores = new LinkedHashMap<>();
			for (NotasEstudianteCompetencia c : fila.competencias()) {
				NotaPeriodoCompetencia p = c.periodos().get(indice);
				String clave = c.competenciaId() + ":" + periodo.id();
				valores.put(clave, p.nivel() == null
						? p.nota()
						: vigesimal ? p.nivel() + " · " + p.nota().replace('.', ',') + "/20" : p.nivel());
				valores.put(clave + "_estado", p.pendiente()
						? CalculoNotas.PENDIENTE + " · " + p.calificadas() + " de " + p.total() + " ítems"
						: p.logro());
			}
			return new FilaReporte(fila.matriculaId(), fila.estudiante(), valores);
		}).toList();
		return new ReporteCalificaciones("Nota de la competencia en el periodo", "Nota oficial por competencia",
				columnas, filas);
	}

	/**
	 * Resumen del año del curso del docente. Por competencia, dos grupos de columnas:
	 * <ul>
	 * <li><b>Aporte del curso:</b> lo que aporta en cada periodo y su aporte final, que consolida sus periodos con la
	 * regla del año. Es referencial: el curso no tiene nota final propia.</li>
	 * <li><b>Nota de la competencia:</b> la oficial en cada periodo y la final, combinando todos los cursos.</li>
	 * </ul>
	 * La nota final de la competencia <b>no</b> es el promedio de los aportes finales de los cursos: combina los cursos
	 * en cada periodo y después junta los periodos.
	 */
	@Transactional(readOnly = true)
	public ReporteCalificaciones resumenAnual(UUID docenteId, UUID planCursoId, UUID seccionId,
			List<UUID> competenciaIds) {
		CursoFila curso = cursoDocente(docenteId, planCursoId, seccionId);
		List<CompetenciaFila> elegidas = elegidas(planCursoId, competenciaIds);
		List<PeriodoFila> periodos = repositorio.periodos(planCursoId, seccionId);
		List<UUID> idsPeriodos = periodos.stream().map(PeriodoFila::id).toList();
		List<Rango> rangosFinal = periodos.isEmpty()
				? CalculoNotas.RANGOS_POR_DEFECTO
				: rangos(periodos.get(periodos.size() - 1).fechaInicio());
		String regla = curso.reglaNota();
		String consolidacion = curso.consolidacion();
		boolean vigesimal = "VIGESIMAL".equals(curso.escala());

		List<ColumnaReporte> columnas = new ArrayList<>();
		for (CompetenciaFila c : elegidas) {
			String aporte = "Aporte de " + curso.curso() + " · " + c.nombre();
			String oficial = "Nota de la competencia · " + c.nombre();
			periodos.forEach(p -> columnas
					.add(new ColumnaReporte(c.id() + ":aporte:" + p.id(), p.nombre(), c.id(), aporte, false)));
			columnas.add(new ColumnaReporte(c.id() + ":aporte:final", "Aporte final", c.id(), aporte, true));
			periodos.forEach(p -> columnas
					.add(new ColumnaReporte(c.id() + ":oficial:" + p.id(), p.nombre(), c.id(), oficial, false)));
			columnas.add(new ColumnaReporte(c.id() + ":oficial:final", "Nota final", c.id(), oficial, true));
		}

		Map<UUID, Map<UUID, ResultadoCompetencia>> resultados = resultadosSeccion(seccionId, idsPeriodos,
				cursosPorCompetencia(curso.anioEscolarId(), curso.gradoCodigo()),
				elegidas.stream().map(CompetenciaFila::id).toList(), regla, consolidacion);
		List<FilaReporte> filas = repositorio.estudiantes(seccionId).stream().map(estudiante -> {
			Map<String, String> valores = new LinkedHashMap<>();
			Map<UUID, ResultadoCompetencia> suyos = resultados.getOrDefault(estudiante.matriculaId(), Map.of());
			for (CompetenciaFila c : elegidas) {
				ResultadoCompetencia resultado = suyos.get(c.id());
				List<Aporte> aportes = resultado.periodos().stream()
						.map(p -> p.aportes().getOrDefault(planCursoId, SIN_APORTE)).toList();
				for (int i = 0; i < periodos.size(); i++) {
					UUID periodoId = periodos.get(i).id();
					Aporte aporte = aportes.get(i);
					valores.put(c.id() + ":aporte:" + periodoId,
							valorPeriodo(aporte.nota(), aporte.conteo()));
					ResultadoPeriodo oficial = resultado.periodos().get(i);
					valores.put(c.id() + ":oficial:" + periodoId, valorPeriodo(oficial.nota(), oficial.avance()));
				}
				AporteFinal aporteFinal = CalculoNotas.aporteFinal(aportes, consolidacion, regla);
				columnaFinal(valores, c.id() + ":aporte:final", aporteFinal.nota(), aporteFinal.avance(), rangosFinal,
						vigesimal);
				columnaFinal(valores, c.id() + ":oficial:final", resultado.notaFinal(), resultado.avance(),
						rangosFinal, vigesimal);
			}
			return new FilaReporte(estudiante.matriculaId(), estudiante.estudiante(), valores);
		}).toList();
		return new ReporteCalificaciones("Resumen del año", "Aporte del curso y nota de la competencia", columnas,
				filas);
	}

	/** El resumen del año en PDF, con la fecha y hora en que se generó: las notas cambian mientras haya registro. */
	@Transactional(readOnly = true)
	public byte[] pdfResumenAnual(UUID docenteId, UUID planCursoId, UUID seccionId, List<UUID> competenciaIds) {
		ReporteCalificaciones reporte = resumenAnual(docenteId, planCursoId, seccionId, competenciaIds);
		CursoFila curso = cursoDocente(docenteId, planCursoId, seccionId);
		String contexto = curso.curso() + " · " + curso.grado() + " · Sección " + curso.seccion()
				+ " · Año escolar " + curso.anio() + " · Generado el " + generado();
		String parametros = ServicioReportesNotas.parametros(curso.escala(), curso.reglaNota())
				+ " · El aporte final consolida los periodos de este curso y es referencial. La nota de la"
				+ " competencia combina todos los cursos en cada periodo: no es el promedio de los aportes finales."
				+ " La consolida la coordinación. Pendiente: falta registrar algún ítem; la nota aparece cuando el"
				+ " registro está completo.";
		return ArchivoReporteCalificaciones.pdfResumen("Resumen del año", "Estudiante",
				"Notas calculadas al generar el reporte. —: sin notas todavía.",
				"No hay estudiantes matriculados para mostrar.", curso.escala(),
				List.of(new TablaPdf("Resumen del año", contexto, parametros, reporte)));
	}

	/** Fecha y hora de generación de un PDF («19/11/2026 10:30»). */
	public String generado() {
		return LocalDateTime.now(reloj).format(GENERADO);
	}

	private static final Aporte SIN_APORTE = new Aporte(Optional.empty(), Avance.VACIO);

	/** Una celda de periodo del resumen: la nota si está completa o «Pendiente (3 de 4)». */
	private static String valorPeriodo(Optional<Nota> nota, Avance avance) {
		if (avance.pendiente()) {
			return CalculoNotas.PENDIENTE + " (" + avance.calificadas() + " de " + avance.total() + ")";
		}
		return nota.map(CalculoNotas::texto).orElse("—");
	}

	/**
	 * Valor y estado de una columna final: «14.2» y «A · Logro esperado» (en letras, solo el significado); pendiente,
	 * «—» y «Pendiente · 3 de 4 ítems».
	 */
	private static void columnaFinal(Map<String, String> valores, String clave, Optional<Nota> nota, Avance avance,
			List<Rango> rangos, boolean vigesimal) {
		NotaResultado r = resultado(nota, avance, rangos);
		valores.put(clave, r.nota());
		String logro = vigesimal && r.nivel() != null ? r.nivel() + " · " + r.logro() : r.logro();
		valores.put(clave + "_estado", r.pendiente() ? pendiente(r) : logro);
	}
}
