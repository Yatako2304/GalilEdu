package com.galiledu.academico.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.academico.aplicacion.DtosCalificaciones.ColumnaReporte;
import com.galiledu.academico.aplicacion.DtosCalificaciones.FilaReporte;
import com.galiledu.academico.aplicacion.DtosCalificaciones.NotaResultado;
import com.galiledu.academico.aplicacion.DtosCalificaciones.ReporteCalificaciones;
import com.galiledu.academico.aplicacion.DtosReportesNotas.AportePeriodo;
import com.galiledu.academico.aplicacion.DtosReportesNotas.CompetenciaApoderado;
import com.galiledu.academico.aplicacion.DtosReportesNotas.CompetenciaCurso;
import com.galiledu.academico.aplicacion.DtosReportesNotas.CursoApoderado;
import com.galiledu.academico.aplicacion.DtosReportesNotas.HijoCalificaciones;
import com.galiledu.academico.aplicacion.DtosReportesNotas.ItemApoderado;
import com.galiledu.academico.aplicacion.DtosReportesNotas.MatriculaHijo;
import com.galiledu.academico.aplicacion.DtosReportesNotas.NotasApoderado;
import com.galiledu.academico.aplicacion.DtosReportesNotas.PeriodoApoderado;
import com.galiledu.academico.aplicacion.DtosReportesNotas.ResumenCursoPeriodo;
import com.galiledu.academico.dominio.CalculoNotas;
import com.galiledu.academico.dominio.CalculoNotas.Aporte;
import com.galiledu.academico.dominio.CalculoNotas.Avance;
import com.galiledu.academico.dominio.CalculoNotas.Calificacion;
import com.galiledu.academico.dominio.CalculoNotas.Nota;
import com.galiledu.academico.dominio.CalculoNotas.Rango;
import com.galiledu.academico.dominio.CalculoNotas.ResultadoCompetencia;
import com.galiledu.academico.infraestructura.ArchivoReporteCalificaciones;
import com.galiledu.academico.infraestructura.ArchivoReporteCalificaciones.TablaPdf;
import com.galiledu.academico.infraestructura.RepositorioPesosPlan.PesoFila;
import com.galiledu.academico.infraestructura.RepositorioReportesNotas;
import com.galiledu.academico.infraestructura.RepositorioReportesNotas.DocenteCursoFila;
import com.galiledu.academico.infraestructura.RepositorioReportesNotas.ItemEstudianteFila;
import com.galiledu.academico.infraestructura.RepositorioReportesNotas.MatriculaHijoFila;
import com.galiledu.academico.infraestructura.RepositorioReportesNotas.PeriodoAnioFila;
import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * HU-69: el apoderado consulta las notas de cada hijo matriculado. Solo lectura y solo de sus hijos.
 *
 * <p>
 * La nota oficial es de la competencia (decisión del 16/11): por cada curso se muestra lo que aporta a cada competencia
 * con sus ítems, y un resumen <b>referencial</b> del curso en el periodo. No hay «aprobado»: cada nota se informa con
 * su
 * nivel de logro (decisión del 18/11).
 */
@Service
public class ServicioNotasApoderado {
	private final RepositorioReportesNotas repositorio;
	private final ServicioNotasCompetencia notas;
	private final Clock reloj;

	public ServicioNotasApoderado(RepositorioReportesNotas repositorio, ServicioNotasCompetencia notas, Clock reloj) {
		this.repositorio = repositorio;
		this.notas = notas;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public List<HijoCalificaciones> hijos(UUID usuarioId) {
		Map<UUID, List<MatriculaHijoFila>> porHijo = repositorio.matriculasDeHijos(usuarioId).stream()
				.collect(Collectors.groupingBy(MatriculaHijoFila::estudianteId, LinkedHashMap::new,
						Collectors.toList()));
		return porHijo.values().stream()
				.map(m -> new HijoCalificaciones(m.get(0).estudianteId(), m.get(0).estudiante(),
						m.stream().map(f -> new MatriculaHijo(f.anio(), f.grado(), f.seccion())).toList()))
				.toList();
	}

	/** Notas del hijo en el año (sin año, el más reciente). 404 si no es su hijo o no está matriculado ese año. */
	@Transactional(readOnly = true)
	public NotasApoderado notas(UUID usuarioId, UUID estudianteId, Integer anio) {
		return notas(matricula(usuarioId, estudianteId, anio));
	}

	private NotasApoderado notas(MatriculaHijoFila matricula) {
		UUID estudianteId = matricula.estudianteId();
		LocalDate hoy = LocalDate.now(reloj);
		List<PeriodoAnioFila> periodos = repositorio.periodos(matricula.anioEscolarId());
		List<UUID> idsPeriodos = periodos.stream().map(PeriodoAnioFila::id).toList();
		Map<UUID, List<Rango>> rangos = new LinkedHashMap<>();
		periodos.forEach(p -> rangos.put(p.id(), notas.rangos(p.fechaInicio())));
		List<Rango> rangosFinal = periodos.isEmpty()
				? CalculoNotas.RANGOS_POR_DEFECTO
				: rangos.get(periodos.get(periodos.size() - 1).id());
		String regla = matricula.reglaNota();
		boolean ponderado = CalculoNotas.PROMEDIO_PONDERADO.equals(regla);

		Map<UUID, List<PesoFila>> cursosPorCompetencia = notas.cursosPorCompetencia(matricula.anioEscolarId(),
				matricula.gradoCodigo());
		List<ItemEstudianteFila> items = repositorio.itemsDelEstudiante(matricula.matriculaId(),
				matricula.seccionId());
		Map<UUID, List<ItemEstudianteFila>> itemsPorCompetencia = items.stream()
				.collect(Collectors.groupingBy(ItemEstudianteFila::competenciaId));

		// Nota oficial de cada competencia del grado, con lo que aporta cada curso en cada periodo.
		Map<UUID, ResultadoCompetencia> resultados = new LinkedHashMap<>();
		List<CompetenciaApoderado> competencias = new ArrayList<>();
		boolean usaPesos = CalculoNotas.usaPesosDeCurso(regla);
		cursosPorCompetencia.forEach((competenciaId, cursos) -> {
			ResultadoCompetencia resultado = CalculoNotas.notaCompetencia(idsPeriodos,
					ServicioNotasCompetencia.cursos(cursos),
					itemsPorCompetencia.getOrDefault(competenciaId, List.of()).stream()
							// «No evaluado»: ese ítem no cuenta para este estudiante.
							.filter(f -> !f.noEvaluado())
							.map(f -> new Calificacion(f.planCursoId(), f.periodoId(), f.pesoPorcentaje(),
									f.notaNumerica(), f.nivelLogro(), f.ordenItem()))
							.toList(),
					regla, matricula.consolidacion());
			resultados.put(competenciaId, resultado);
			competencias.add(new CompetenciaApoderado(competenciaId, cursos.get(0).competencia(),
					cursos.get(0).area(), cursos.stream().map(PesoFila::curso).toList(),
					!usaPesos || CalculoNotas.pesosCompletos(cursos.stream().map(PesoFila::pesoCurso).toList()),
					resultado.periodos().stream()
							.map(p -> ServicioNotasCompetencia.periodo(p, rangos.get(p.periodoId()))).toList(),
					ServicioNotasCompetencia.resultado(resultado.notaFinal(), resultado.avance(), rangosFinal)));
		});

		// Por curso: sus competencias con el aporte e ítems de cada periodo, y un resumen referencial del periodo.
		Map<UUID, String> docentes = repositorio.docentes(matricula.seccionId()).stream()
				.collect(Collectors.toMap(DocenteCursoFila::planCursoId, DocenteCursoFila::docente, (a, b) -> a));
		Map<UUID, List<PesoFila>> porCurso = cursosPorCompetencia.values().stream().flatMap(List::stream)
				.collect(Collectors.groupingBy(PesoFila::planCursoId, LinkedHashMap::new, Collectors.toList()));
		List<CursoApoderado> cursos = porCurso.entrySet().stream()
				.sorted(Map.Entry.comparingByValue((a, b) -> a.get(0).curso().compareToIgnoreCase(b.get(0).curso())))
				.map(e -> {
					UUID planCursoId = e.getKey();
					List<ItemEstudianteFila> delCurso = items.stream()
							.filter(i -> i.planCursoId().equals(planCursoId)).toList();
					List<ResumenCursoPeriodo> resumen = periodos.stream().map(p -> {
						Aporte delPeriodo = resumenCurso(delCurso, p.id(), regla, ponderado);
						return new ResumenCursoPeriodo(p.id(), ServicioNotasCompetencia.resultado(delPeriodo.nota(),
								delPeriodo.conteo(), rangos.get(p.id())));
					}).toList();
					List<CompetenciaCurso> deCurso = e.getValue().stream().map(peso -> {
						ResultadoCompetencia resultado = resultados.get(peso.competenciaId());
						List<AportePeriodo> aportes = resultado.periodos().stream().map(p -> {
							List<ItemApoderado> itemsPeriodo = delCurso.stream()
									.filter(i -> i.periodoId().equals(p.periodoId())
											&& i.competenciaId().equals(peso.competenciaId()))
									.map(i -> item(i, rangos.get(p.periodoId()))).toList();
							Aporte aporte = p.aportes().getOrDefault(planCursoId,
									new Aporte(Optional.empty(), Avance.VACIO));
							return new AportePeriodo(p.periodoId(), ServicioNotasCompetencia.resultado(aporte.nota(),
									aporte.conteo(), rangos.get(p.periodoId())), itemsPeriodo);
						}).toList();
						return new CompetenciaCurso(peso.competenciaId(), peso.competencia(), aportes);
					}).toList();
					PesoFila primera = e.getValue().get(0);
					return new CursoApoderado(planCursoId, primera.curso(), primera.area(),
							docentes.get(planCursoId), resumen, deCurso);
				}).toList();

		boolean cerrado = "CERRADO".equals(matricula.estadoAnio());
		boolean finalizado = cerrado
				|| (!periodos.isEmpty() && hoy.isAfter(periodos.get(periodos.size() - 1).fechaFin()));
		return new NotasApoderado(estudianteId, matricula.estudiante(), matricula.anio(), matricula.estadoAnio(),
				finalizado, matricula.grado(), matricula.seccion(), matricula.escala(), regla,
				matricula.consolidacion(),
				periodos.stream().map(p -> new PeriodoApoderado(p.id(), p.numero(), p.nombre(), p.fechaInicio(),
						p.fechaFin(), estadoPeriodo(p, cerrado, hoy))).toList(),
				cursos, competencias);
	}

	/** La libreta del año en PDF: la nota oficial de cada competencia en cada periodo y la final, en nivel de logro. */
	@Transactional(readOnly = true)
	public byte[] libreta(UUID usuarioId, UUID estudianteId, Integer anio) {
		return libreta(notas(usuarioId, estudianteId, anio));
	}

	/** La misma libreta para el Coordinador, de cualquier estudiante matriculado. 404 si la matrícula no existe. */
	@Transactional(readOnly = true)
	public byte[] libretaMatricula(UUID matriculaId) {
		return libreta(notas(repositorio.matricula(matriculaId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La matrícula no existe."))));
	}

	private byte[] libreta(NotasApoderado datos) {
		boolean vigesimal = "VIGESIMAL".equals(datos.escala());
		List<ColumnaReporte> columnas = new ArrayList<>();
		datos.periodos().forEach(p -> columnas.add(new ColumnaReporte(p.id().toString(), p.nombre(), null, null,
				true)));
		columnas.add(new ColumnaReporte("final", "Nota final", null, null, true));
		List<FilaReporte> filas = datos.competencias().stream().map(c -> {
			Map<String, String> valores = new LinkedHashMap<>();
			c.periodos().forEach(p -> valor(valores, p.periodoId().toString(),
					new NotaResultado(p.nota(), p.nivel(), p.logro(), p.pendiente(), p.calificadas(), p.total()),
					vigesimal));
			valor(valores, "final", c.notaFinal(), vigesimal);
			return new FilaReporte(c.competenciaId(), c.area() + " · " + c.competencia(), valores);
		}).toList();
		String contexto = datos.estudiante() + " · " + datos.grado() + " · Sección " + datos.seccion()
				+ " · Año escolar " + datos.anio() + (datos.finalizado() ? " · Finalizado" : " · En curso");
		String parametros = ServicioReportesNotas.parametros(datos.escala(), datos.reglaNota())
				+ " · La nota de cada competencia combina todos los cursos del grado que la evalúan. Pendiente: falta"
				+ " registrar algún ítem; la nota aparece cuando el registro está completo.";
		return ArchivoReporteCalificaciones.pdfResumen("Libreta de notas", "Área · Competencia",
				"Notas calculadas al generar la libreta. —: sin notas todavía.", "El grado no tiene competencias.",
				"LIBRETA", List.of(new TablaPdf("Libreta de notas", contexto, parametros,
						new ReporteCalificaciones("Nota de la competencia", "Libreta", columnas, filas))));
	}

	private static void valor(Map<String, String> valores, String clave, NotaResultado nota, boolean vigesimal) {
		valores.put(clave, nota.nivel() == null
				? "—"
				: vigesimal ? nota.nivel() + " · " + nota.nota().replace('.', ',') : nota.nivel());
		valores.put(clave + "_estado", nota.pendiente() ? ServicioNotasCompetencia.pendiente(nota) : nota.logro());
	}

	private MatriculaHijoFila matricula(UUID usuarioId, UUID estudianteId, Integer anio) {
		List<MatriculaHijoFila> delHijo = repositorio.matriculasDeHijos(usuarioId).stream()
				.filter(m -> m.estudianteId().equals(estudianteId)).toList();
		return delHijo.stream().filter(m -> anio == null || m.anio() == anio).findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El estudiante no está matriculado ese año."));
	}

	/**
	 * Resumen referencial de un curso en el periodo: sus ítems (cada uno una vez) con la regla del año; en ponderado,
	 * el peso de un ítem es la suma de sus pesos en las competencias. Es el «Promedio general» del consolidado. Si
	 * falta
	 * registrar algún ítem, queda pendiente como las notas oficiales.
	 */
	private static Aporte resumenCurso(List<ItemEstudianteFila> items, UUID periodoId, String regla,
			boolean ponderado) {
		Map<UUID, Calificacion> unicos = new LinkedHashMap<>();
		items.stream().filter(i -> i.periodoId().equals(periodoId) && !i.noEvaluado())
				.forEach(i -> unicos.merge(i.itemId(),
						new Calificacion(i.planCursoId(), i.periodoId(),
								i.pesoPorcentaje() == null ? BigDecimal.ONE : i.pesoPorcentaje(), i.notaNumerica(),
								i.nivelLogro(), i.ordenItem()),
						(a, b) -> ponderado
								? new Calificacion(a.planCursoId(), a.periodoId(), a.pesoItem().add(b.pesoItem()),
										a.numerica(), a.nivel(), a.orden())
								: a));
		return CalculoNotas.aporte(new ArrayList<>(unicos.values()), regla, ponderado);
	}

	private static ItemApoderado item(ItemEstudianteFila fila, List<Rango> rangos) {
		if (fila.noEvaluado()) {
			return new ItemApoderado(fila.itemId(), fila.item(), fila.tipo(), "No evaluado", null);
		}
		if (fila.notaNumerica() == null && fila.nivelLogro() == null) {
			return new ItemApoderado(fila.itemId(), fila.item(), fila.tipo(), "—", null);
		}
		Nota nota = new Nota(BigDecimal.ONE, fila.notaNumerica(), fila.nivelLogro(), fila.ordenItem());
		return new ItemApoderado(fila.itemId(), fila.item(), fila.tipo(), CalculoNotas.texto(nota),
				CalculoNotas.nivel(nota, rangos));
	}

	private static String estadoPeriodo(PeriodoAnioFila periodo, boolean anioCerrado, LocalDate hoy) {
		if (anioCerrado || hoy.isAfter(periodo.fechaFin())) {
			return "CERRADO";
		}
		return hoy.isBefore(periodo.fechaInicio()) ? "PLANIFICADO" : "EN_CURSO";
	}
}
