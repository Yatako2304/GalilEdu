package com.galiledu.academico.aplicacion;

import java.time.LocalDate;
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
import com.galiledu.academico.dominio.CalculoNotas.Calificacion;
import com.galiledu.academico.dominio.CalculoNotas.CursoPeso;
import com.galiledu.academico.dominio.CalculoNotas.Nota;
import com.galiledu.academico.dominio.CalculoNotas.Rango;
import com.galiledu.academico.dominio.CalculoNotas.ResultadoCompetencia;
import com.galiledu.academico.dominio.CalculoNotas.ResultadoPeriodo;
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
	private final RepositorioCalificaciones repositorio;
	private final RepositorioPesosPlan pesos;

	public ServicioNotasCompetencia(RepositorioCalificaciones repositorio, RepositorioPesosPlan pesos) {
		this.repositorio = repositorio;
		this.pesos = pesos;
	}

	/**
	 * Para el docente de un curso y sección: las competencias que evalúa su curso (todas o las elegidas). Ve el aporte
	 * de los cursos de sus colegas en la sección, no sus ítems.
	 */
	@Transactional(readOnly = true)
	public NotasCompetencias notasDocente(UUID docenteId, UUID planCursoId, UUID seccionId,
			List<UUID> competenciaIds) {
		CursoFila curso = repositorio.cursos(docenteId).stream()
				.filter(c -> c.planCursoId().equals(planCursoId) && c.seccionId().equals(seccionId)).findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El curso no está asignado al docente."));
		List<CompetenciaFila> delCurso = repositorio.competencias(planCursoId);
		Set<UUID> permitidas = delCurso.stream().map(CompetenciaFila::id).collect(Collectors.toSet());
		if (!permitidas.containsAll(competenciaIds)) {
			throw ExcepcionNegocio.invalido("Una competencia no pertenece al plan del curso.");
		}
		List<CompetenciaFila> elegidas = competenciaIds.isEmpty()
				? delCurso
				: delCurso.stream().filter(c -> competenciaIds.contains(c.id())).toList();
		return calcular(curso, seccionId, repositorio.periodos(planCursoId, seccionId), elegidas);
	}

	private NotasCompetencias calcular(CursoFila curso, UUID seccionId, List<PeriodoFila> periodos,
			List<CompetenciaFila> elegidas) {
		String regla = curso.reglaNota();
		boolean usaPesos = CalculoNotas.usaPesosDeCurso(regla);
		Map<UUID, List<PesoFila>> cursosPorCompetencia = pesos.delGrado(curso.anioEscolarId(), curso.gradoCodigo())
				.stream()
				.collect(Collectors.groupingBy(PesoFila::competenciaId, LinkedHashMap::new, Collectors.toList()));
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

		Map<UUID, Map<UUID, List<CalificacionCompetenciaFila>>> porEstudiante = repositorio
				.calificacionesCompetencias(seccionId, elegidas.stream().map(CompetenciaFila::id).toList()).stream()
				.collect(Collectors.groupingBy(CalificacionCompetenciaFila::matriculaId,
						Collectors.groupingBy(CalificacionCompetenciaFila::competenciaId)));

		List<FilaNotasCompetencia> filas = repositorio.estudiantes(seccionId).stream().map(estudiante -> {
			Map<UUID, List<CalificacionCompetenciaFila>> suyas = porEstudiante.getOrDefault(estudiante.matriculaId(),
					Map.of());
			List<NotasEstudianteCompetencia> notas = new ArrayList<>();
			for (CompetenciaNotas competencia : competencias) {
				List<CursoPeso> cursos = competencia.cursos().stream()
						.map(c -> new CursoPeso(c.planCursoId(), c.pesoCurso())).toList();
				List<Calificacion> calificaciones = suyas.getOrDefault(competencia.competenciaId(), List.of())
						.stream()
						.map(f -> new Calificacion(f.planCursoId(), f.periodoId(), f.pesoPorcentaje(),
								f.notaNumerica(), f.nivelLogro(), f.ordenItem()))
						.toList();
				ResultadoCompetencia resultado = CalculoNotas.notaCompetencia(idsPeriodos, cursos, calificaciones,
						regla, curso.consolidacion());
				notas.add(new NotasEstudianteCompetencia(competencia.competenciaId(),
						resultado.periodos().stream().map(p -> periodo(p, rangos.get(p.periodoId()))).toList(),
						resultado(resultado.notaFinal(), resultado.parcial(), rangosFinal)));
			}
			return new FilaNotasCompetencia(estudiante.matriculaId(), estudiante.estudiante(), notas);
		}).toList();

		return new NotasCompetencias(curso.escala(), regla, curso.consolidacion(),
				periodos.stream().map(p -> new PeriodoNotas(p.id(), p.numero(), p.nombre())).toList(), competencias,
				filas);
	}

	private List<Rango> rangos(LocalDate fecha) {
		List<Rango> vigentes = repositorio.rangosConversion(fecha);
		return vigentes.isEmpty() ? CalculoNotas.RANGOS_POR_DEFECTO : vigentes;
	}

	private static NotaPeriodoCompetencia periodo(ResultadoPeriodo periodo, List<Rango> rangos) {
		NotaResultado nota = resultado(periodo.nota(), periodo.parcial(), rangos);
		List<AporteCurso> aportes = periodo.aportes().entrySet().stream()
				.map(e -> new AporteCurso(e.getKey(), e.getValue().map(CalculoNotas::texto).orElse("—"))).toList();
		return new NotaPeriodoCompetencia(periodo.periodoId(), nota.nota(), nota.nivel(), nota.logro(),
				nota.parcial(), aportes);
	}

	private static NotaResultado resultado(Optional<Nota> nota, boolean parcial, List<Rango> rangos) {
		return nota.map(n -> new NotaResultado(CalculoNotas.texto(n), CalculoNotas.nivel(n, rangos),
				CalculoNotas.logro(n, rangos), parcial))
				.orElse(new NotaResultado("—", null, CalculoNotas.PENDIENTE, false));
	}

	/**
	 * Las mismas notas como tabla para el final del PDF del consolidado: por competencia, una columna por periodo y la
	 * nota final con su nivel de logro (p. ej. «A · 13,9/20») y su significado («Logro esperado»).
	 */
	@Transactional(readOnly = true)
	public ReporteCalificaciones reporteNotasFinales(UUID docenteId, UUID planCursoId, UUID seccionId,
			List<UUID> competenciaIds) {
		NotasCompetencias notas = notasDocente(docenteId, planCursoId, seccionId, competenciaIds);
		boolean vigesimal = "VIGESIMAL".equals(notas.escala());
		List<ColumnaReporte> columnas = new ArrayList<>();
		for (CompetenciaNotas c : notas.competencias()) {
			notas.periodos().forEach(p -> columnas.add(new ColumnaReporte(c.competenciaId() + ":" + p.id(),
					p.nombre(), c.competenciaId(), c.competencia(), false)));
			columnas.add(new ColumnaReporte(c.competenciaId() + ":final", "Nota final", c.competenciaId(),
					c.competencia(), true));
		}
		List<FilaReporte> filas = notas.filas().stream().map(fila -> {
			Map<String, String> valores = new LinkedHashMap<>();
			for (NotasEstudianteCompetencia c : fila.competencias()) {
				c.periodos().forEach(p -> valores.put(c.competenciaId() + ":" + p.periodoId(), p.nota()));
				NotaResultado f = c.notaFinal();
				String clave = c.competenciaId() + ":final";
				valores.put(clave, f.nivel() == null
						? f.nota()
						: vigesimal ? f.nivel() + " · " + f.nota().replace('.', ',') + "/20" : f.nivel());
				valores.put(clave + "_estado", f.parcial() ? f.logro() + " · Parcial" : f.logro());
			}
			return new FilaReporte(fila.matriculaId(), fila.estudiante(), valores);
		}).toList();
		return new ReporteCalificaciones("Nota final de la competencia", "Nota oficial por competencia", columnas,
				filas);
	}
}
