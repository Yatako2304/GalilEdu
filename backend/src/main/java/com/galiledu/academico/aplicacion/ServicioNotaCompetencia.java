package com.galiledu.academico.aplicacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.academico.aplicacion.DtosCalificaciones.CursoNotaFinal;
import com.galiledu.academico.aplicacion.DtosCalificaciones.FilaNotaFinal;
import com.galiledu.academico.aplicacion.DtosCalificaciones.NotaFinalCompetencia;
import com.galiledu.academico.dominio.CalculoNotas;
import com.galiledu.academico.dominio.CalculoNotas.Nota;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.EstudianteFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.NotaCompetenciaFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.ParametrosPlan;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.PeriodoAnio;
import com.galiledu.academico.infraestructura.RepositorioPesosPlan;
import com.galiledu.academico.infraestructura.RepositorioPesosPlan.PesoFila;
import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * Nota final de una competencia entre los cursos del grado, para una sección. Primero consolida los periodos de cada
 * curso y después combina los cursos con su peso del plan de evaluación (academico.plan_competencia.peso_curso).
 */
@Service
public class ServicioNotaCompetencia {
	private static final int NOTA_APROBATORIA_POR_DEFECTO = 14;
	private static final String SIN_NOTA = "—";

	private final RepositorioCalificaciones repositorio;
	private final RepositorioPesosPlan pesos;

	public ServicioNotaCompetencia(RepositorioCalificaciones repositorio, RepositorioPesosPlan pesos) {
		this.repositorio = repositorio;
		this.pesos = pesos;
	}

	@Transactional(readOnly = true)
	public NotaFinalCompetencia notaFinal(UUID docenteId, UUID planCursoId, UUID seccionId, UUID competenciaId) {
		if (!repositorio.tieneAcceso(docenteId, planCursoId, seccionId)) {
			throw ExcepcionNegocio.noEncontrado("El curso no está asignado al docente.");
		}
		String competencia = repositorio.competencias(planCursoId).stream().filter(c -> c.id().equals(competenciaId))
				.findFirst().map(c -> c.nombre())
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La competencia no pertenece al plan del curso."));
		ParametrosPlan plan = repositorio.parametrosPlan(planCursoId);
		List<PesoFila> cursos = pesos.delGrado(plan.anioEscolarId(), plan.gradoCodigo()).stream()
				.filter(c -> c.competenciaId().equals(competenciaId)).toList();
		List<PeriodoAnio> periodos = repositorio.periodosDelAnio(plan.anioEscolarId());
		boolean pesosCompletos = CalculoNotas.pesosCompletos(cursos.stream().map(PesoFila::pesoCurso).toList());
		boolean calculable = pesosCompletos || !"PROMEDIO_PONDERADO".equals(plan.reglaNota());
		int notaAprobatoria = periodos.isEmpty()
				? NOTA_APROBATORIA_POR_DEFECTO
				: repositorio.notaAprobatoria(periodos.get(periodos.size() - 1).fechaInicio())
						.orElse(NOTA_APROBATORIA_POR_DEFECTO);
		Map<UUID, List<NotaCompetenciaFila>> notas = repositorio
				.notasDeCompetencia(plan.anioEscolarId(), plan.gradoCodigo(), seccionId, competenciaId).stream()
				.collect(Collectors.groupingBy(NotaCompetenciaFila::matriculaId));

		List<FilaNotaFinal> filas = repositorio.estudiantes(seccionId).stream()
				.map(estudiante -> fila(estudiante, notas.getOrDefault(estudiante.matriculaId(), List.of()), cursos,
						periodos, plan, calculable, notaAprobatoria))
				.toList();
		return new NotaFinalCompetencia(competenciaId, competencia, plan.escala(), plan.reglaNota(),
				plan.consolidacion(), pesosCompletos,
				cursos.stream().map(c -> new CursoNotaFinal(c.planCursoId(), c.curso(), c.pesoCurso())).toList(),
				filas);
	}

	private static FilaNotaFinal fila(EstudianteFila estudiante, List<NotaCompetenciaFila> notas, List<PesoFila> cursos,
			List<PeriodoAnio> periodos, ParametrosPlan plan, boolean calculable, int notaAprobatoria) {
		Map<String, String> porCurso = new LinkedHashMap<>();
		List<Nota> resultados = new ArrayList<>();
		boolean parcial = false;
		for (PesoFila curso : cursos) {
			List<Nota> porPeriodo = new ArrayList<>();
			for (PeriodoAnio periodo : periodos) {
				List<NotaCompetenciaFila> delPeriodo = notas.stream()
						.filter(n -> n.planCursoId().equals(curso.planCursoId()) && n.periodoId().equals(periodo.id()))
						.toList();
				CalculoNotas.combinar(delPeriodo.stream().map(ServicioNotaCompetencia::nota).toList(),
						plan.reglaNota()).ifPresent(porPeriodo::add);
				parcial = parcial || (cuenta(periodo, periodos, plan) && incompleto(delPeriodo));
			}
			Optional<Nota> resultado = CalculoNotas.consolidar(porPeriodo, plan.consolidacion(), plan.reglaNota());
			porCurso.put(curso.planCursoId().toString(), resultado.map(CalculoNotas::texto).orElse(SIN_NOTA));
			resultado.ifPresent(r -> resultados.add(new Nota(pesoDe(curso, plan), r.numerica(), r.nivel(), r.orden())));
		}
		Optional<Nota> notaFinal = calculable ? CalculoNotas.combinar(resultados, plan.reglaNota()) : Optional.empty();
		return new FilaNotaFinal(estudiante.matriculaId(), estudiante.estudiante(), porCurso,
				notaFinal.map(CalculoNotas::texto).orElse(SIN_NOTA),
				notaFinal.map(n -> CalculoNotas.estado(n, notaAprobatoria)).orElse("Pendiente"),
				notaFinal.isPresent() && parcial);
	}

	/** Con ULTIMO_PERIODO, los periodos anteriores no cambian la nota: una falta allí no la vuelve parcial. */
	private static boolean cuenta(PeriodoAnio periodo, List<PeriodoAnio> periodos, ParametrosPlan plan) {
		return !"ULTIMO_PERIODO".equals(plan.consolidacion()) || periodo.equals(periodos.get(periodos.size() - 1));
	}

	private static boolean incompleto(List<NotaCompetenciaFila> delPeriodo) {
		return delPeriodo.isEmpty()
				|| delPeriodo.stream().anyMatch(n -> n.notaNumerica() == null && n.nivelLogro() == null);
	}

	/** Sin promedio ponderado los cursos pesan igual: el peso del plan puede estar vacío y no se usa. */
	private static BigDecimal pesoDe(PesoFila curso, ParametrosPlan plan) {
		return "PROMEDIO_PONDERADO".equals(plan.reglaNota()) && curso.pesoCurso() != null
				? curso.pesoCurso()
				: BigDecimal.ONE;
	}

	private static Nota nota(NotaCompetenciaFila fila) {
		return new Nota(fila.pesoPorcentaje(), fila.notaNumerica(), fila.nivelLogro(), fila.ordenItem());
	}
}
