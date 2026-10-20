package com.galiledu.academico.aplicacion;

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

import com.galiledu.academico.aplicacion.DtosAcademico.AnioEscolarOpcion;
import com.galiledu.academico.aplicacion.DtosAcademico.AsignacionDocente;
import com.galiledu.academico.aplicacion.DtosAcademico.CompetenciaPlan;
import com.galiledu.academico.aplicacion.DtosAcademico.CompetenciaResumen;
import com.galiledu.academico.aplicacion.DtosAcademico.CursoDisponible;
import com.galiledu.academico.aplicacion.DtosAcademico.DocenteOpcion;
import com.galiledu.academico.aplicacion.DtosAcademico.DocenteSeccion;
import com.galiledu.academico.aplicacion.DtosAcademico.OrigenPlan;
import com.galiledu.academico.aplicacion.DtosAcademico.PlanCursoDetalle;
import com.galiledu.academico.aplicacion.DtosAcademico.PlanCursoResumen;
import com.galiledu.academico.aplicacion.DtosAcademico.PlanGrado;
import com.galiledu.academico.aplicacion.DtosAcademico.SeccionPlan;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudCompetenciaPlan;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudConfigurarCurso;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudCopiaPlan;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudDocentes;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudPlanCurso;
import com.galiledu.academico.dominio.Area;
import com.galiledu.academico.dominio.Competencia;
import com.galiledu.academico.dominio.CompetenciaEvaluada;
import com.galiledu.academico.dominio.Consolidacion;
import com.galiledu.academico.dominio.Curso;
import com.galiledu.academico.dominio.DocenteAsignado;
import com.galiledu.academico.dominio.Escala;
import com.galiledu.academico.dominio.PlanCurso;
import com.galiledu.academico.dominio.ReglaNota;
import com.galiledu.academico.infraestructura.AreaRepositorio;
import com.galiledu.academico.infraestructura.CompetenciaRepositorio;
import com.galiledu.academico.infraestructura.ConsultasCatalogo;
import com.galiledu.academico.infraestructura.ConsultasCatalogo.AnioEscolar;
import com.galiledu.academico.infraestructura.ConsultasCatalogo.SeccionGrado;
import com.galiledu.academico.infraestructura.CursoRepositorio;
import com.galiledu.academico.infraestructura.PlanCursoRepositorio;
import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * Plan de evaluación de cada grado y año (Figma CI-17 y CI-20, HU-56): qué cursos se dictan y cuántas horas, qué
 * competencias evalúan y cómo, y qué docente los dicta en cada sección. RF-130 a RF-132.
 */
@Service
public class ServicioPlanEvaluacion {
	/** Valores con los que aparece una competencia no marcada en el modal (Figma: "Vigesimal (0–20)"…). */
	private static final Escala ESCALA_POR_DEFECTO = Escala.VIGESIMAL;
	private static final ReglaNota REGLA_POR_DEFECTO = ReglaNota.PROMEDIO_SIMPLE;
	private static final Consolidacion CONSOLIDACION_POR_DEFECTO = Consolidacion.PROMEDIO_PERIODOS;

	private final PlanCursoRepositorio planes;
	private final CursoRepositorio cursos;
	private final AreaRepositorio areas;
	private final CompetenciaRepositorio competencias;
	private final ConsultasCatalogo consultas;

	public ServicioPlanEvaluacion(PlanCursoRepositorio planes, CursoRepositorio cursos, AreaRepositorio areas,
			CompetenciaRepositorio competencias, ConsultasCatalogo consultas) {
		this.planes = planes;
		this.cursos = cursos;
		this.areas = areas;
		this.competencias = competencias;
		this.consultas = consultas;
	}

	// ---------- Consultas ----------

	@Transactional(readOnly = true)
	public List<AnioEscolarOpcion> anios() {
		return consultas.aniosEscolares().stream().map(a -> new AnioEscolarOpcion(a.id(), a.anio(), a.estado()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<DocenteOpcion> docentes() {
		return consultas.docentes().stream().map(d -> new DocenteOpcion(d.id(), d.nombre())).toList();
	}

	@Transactional(readOnly = true)
	public PlanGrado plan(UUID anioEscolarId, String gradoCodigo) {
		AnioEscolar anio = obtenerAnio(anioEscolarId);
		validarGrado(gradoCodigo);
		List<SeccionGrado> secciones = consultas.secciones(anioEscolarId, gradoCodigo);
		Set<UUID> idsSecciones = secciones.stream().map(SeccionGrado::id).collect(Collectors.toSet());
		Map<UUID, Curso> cursosPorId = porId(cursos.findAll(), Curso::getId);
		Map<UUID, Area> areasPorId = porId(areas.findAll(), Area::getId);

		List<PlanCurso> delGrado = planes.findByAnioEscolarIdAndGradoCodigo(anioEscolarId, gradoCodigo);
		List<PlanCursoResumen> filas = delGrado.stream()
				.map(p -> {
					Curso curso = cursosPorId.get(p.getCursoId());
					int asignados = (int) p.getDocentes().stream()
							.filter(d -> idsSecciones.contains(d.getSeccionId())).count();
					return new PlanCursoResumen(p.getId(), curso.getId(), curso.getNombre(),
							areasPorId.get(curso.getAreaId()).getNombre(), p.getHorasSemanales(),
							p.getCompetencias().size(), escala(p), asignados);
				})
				.sorted(Comparator.comparing((PlanCursoResumen f) -> areaOrden(f, cursosPorId, areasPorId))
						.thenComparing(PlanCursoResumen::curso))
				.toList();

		return new PlanGrado(anio.id(), anio.anio(), anio.estado(), gradoCodigo, consultas.nombreDeGrado(gradoCodigo),
				consultas.bloquesSemanales(anioEscolarId, gradoCodigo).orElse(null),
				filas.stream().mapToInt(PlanCursoResumen::horasSemanales).sum(),
				(int) delGrado.stream().flatMap(p -> p.getCompetencias().stream())
						.map(CompetenciaEvaluada::getCompetenciaId).distinct().count(),
				(int) filas.stream().filter(f -> f.competencias() > 0).count(),
				filas.stream().mapToInt(PlanCursoResumen::docentesAsignados).sum(),
				secciones.size() * filas.size(),
				secciones.stream().map(s -> new SeccionPlan(s.id(), s.nombre(), s.tutor())).toList(),
				filas);
	}

	/** Competencias del área del curso: las activas y las que el plan ya usa aunque se hayan desactivado. */
	@Transactional(readOnly = true)
	public PlanCursoDetalle detalle(UUID id) {
		PlanCurso plan = obtenerPlan(id);
		Curso curso = obtenerCurso(plan.getCursoId());
		Map<UUID, CompetenciaEvaluada> elegidas = porId(plan.getCompetencias(),
				CompetenciaEvaluada::getCompetenciaId);
		List<CompetenciaPlan> filas = competencias.findByAreaIdOrderByOrdenAscNombreAsc(curso.getAreaId()).stream()
				.filter(c -> c.estaActiva() || elegidas.containsKey(c.getId()))
				.map(c -> {
					CompetenciaEvaluada e = elegidas.get(c.getId());
					return e == null
							? new CompetenciaPlan(c.getId(), c.getNombre(), c.getEstado(), false,
									ESCALA_POR_DEFECTO, REGLA_POR_DEFECTO, CONSOLIDACION_POR_DEFECTO)
							: new CompetenciaPlan(c.getId(), c.getNombre(), c.getEstado(), true, e.getEscala(),
									e.getReglaNota(), e.getConsolidacion());
				})
				.toList();
		return new PlanCursoDetalle(plan.getId(), curso.getId(), curso.getNombre(),
				obtenerArea(curso.getAreaId()).getNombre(), obtenerAnio(plan.getAnioEscolarId()).anio(),
				consultas.nombreDeGrado(plan.getGradoCodigo()), plan.getHorasSemanales(), plan.getActualizadoEn(),
				filas);
	}

	/** Figma: "Solo aparecen cursos activos que aún no están en el plan del grado." */
	@Transactional(readOnly = true)
	public List<CursoDisponible> cursosDisponibles(UUID anioEscolarId, String gradoCodigo) {
		obtenerAnio(anioEscolarId);
		validarGrado(gradoCodigo);
		Set<UUID> enPlan = planes.findByAnioEscolarIdAndGradoCodigo(anioEscolarId, gradoCodigo).stream()
				.map(PlanCurso::getCursoId).collect(Collectors.toSet());
		Map<UUID, Area> areasPorId = porId(areas.findAll(), Area::getId);
		return cursos.findAll().stream()
				.filter(c -> c.estaActivo() && areasPorId.get(c.getAreaId()).estaActiva())
				.filter(c -> c.getGrados().contains(gradoCodigo) && !enPlan.contains(c.getId()))
				.sorted(Comparator.comparing(Curso::getNombre))
				.map(c -> new CursoDisponible(c.getId(), c.getNombre(), areasPorId.get(c.getAreaId()).getNombre(),
						competencias.findByAreaIdOrderByOrdenAscNombreAsc(c.getAreaId()).stream()
								.filter(Competencia::estaActiva).map(CompetenciaResumen::de).toList()))
				.toList();
	}

	/** Planes con cursos que se pueden copiar a este grado y año (todos menos el propio). */
	@Transactional(readOnly = true)
	public List<OrigenPlan> origenes(UUID anioEscolarId, String gradoCodigo) {
		Map<UUID, AnioEscolar> anios = porId(consultas.aniosEscolares(), AnioEscolar::id);
		Map<List<Object>, List<PlanCurso>> porPlan = planes.findAll().stream()
				.filter(p -> !(p.getAnioEscolarId().equals(anioEscolarId) && p.getGradoCodigo().equals(gradoCodigo)))
				.collect(Collectors.groupingBy(p -> List.of(p.getAnioEscolarId(), p.getGradoCodigo()),
						LinkedHashMap::new, Collectors.toList()));
		List<String> ordenGrados = consultas.codigosDeGrado();
		return porPlan.values().stream()
				.map(lista -> {
					PlanCurso uno = lista.get(0);
					return new OrigenPlan(uno.getAnioEscolarId(), anios.get(uno.getAnioEscolarId()).anio(),
							uno.getGradoCodigo(), consultas.nombreDeGrado(uno.getGradoCodigo()), lista.size(),
							lista.stream().mapToInt(PlanCurso::getHorasSemanales).sum(),
							lista.stream().mapToInt(p -> p.getCompetencias().size()).sum());
				})
				.sorted(Comparator.comparing(OrigenPlan::anio).reversed()
						.thenComparing(o -> ordenGrados.indexOf(o.gradoCodigo())))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<DocenteSeccion> docentesDelCurso(UUID id) {
		PlanCurso plan = obtenerPlan(id);
		Map<UUID, UUID> asignados = plan.getDocentes().stream()
				.collect(Collectors.toMap(DocenteAsignado::getSeccionId, DocenteAsignado::getDocenteId));
		return consultas.secciones(plan.getAnioEscolarId(), plan.getGradoCodigo()).stream()
				.map(s -> new DocenteSeccion(s.id(), s.nombre(), s.tutor(), asignados.get(s.id())))
				.toList();
	}

	// ---------- Cambios ----------

	@Transactional
	public PlanCursoDetalle agregarCurso(SolicitudPlanCurso solicitud) {
		AnioEscolar anio = obtenerAnioEditable(solicitud.anioEscolarId());
		String grado = solicitud.gradoCodigo();
		validarGrado(grado);
		Curso curso = obtenerCurso(solicitud.cursoId());
		Area area = obtenerArea(curso.getAreaId());
		if (!curso.estaActivo() || !area.estaActiva()) {
			throw ExcepcionNegocio.conflicto(
					"%s está inactivo en el plan curricular: ya no puede agregarse a planes nuevos."
							.formatted(curso.getNombre()));
		}
		if (!curso.getGrados().contains(grado)) {
			throw ExcepcionNegocio.conflicto("%s no se dicta en %s. Agrega el grado al curso en el plan curricular."
					.formatted(curso.getNombre(), consultas.nombreDeGrado(grado)));
		}
		if (planes.existsByAnioEscolarIdAndGradoCodigoAndCursoId(anio.id(), grado, curso.getId())) {
			throw ExcepcionNegocio.conflicto("%s ya está en el plan de %s %d."
					.formatted(curso.getNombre(), consultas.nombreDeGrado(grado), anio.anio()));
		}
		validarHoras(anio.id(), grado, null, solicitud.horasSemanales());
		var evaluadas = competenciasEvaluadas(curso, solicitud.competencias(), Set.of());
		PlanCurso plan = planes.save(new PlanCurso(anio.id(), grado, curso.getId(), solicitud.horasSemanales(),
				evaluadas));
		return detalle(plan.getId());
	}

	@Transactional
	public PlanCursoDetalle configurarCurso(UUID id, SolicitudConfigurarCurso solicitud) {
		PlanCurso plan = obtenerPlan(id);
		obtenerAnioEditable(plan.getAnioEscolarId());
		validarHoras(plan.getAnioEscolarId(), plan.getGradoCodigo(), plan.getId(), solicitud.horasSemanales());
		Set<UUID> yaElegidas = plan.getCompetencias().stream().map(CompetenciaEvaluada::getCompetenciaId)
				.collect(Collectors.toSet());
		plan.configurar(solicitud.horasSemanales(),
				competenciasEvaluadas(obtenerCurso(plan.getCursoId()), solicitud.competencias(), yaElegidas));
		planes.flush();
		return detalle(plan.getId());
	}

	/** Quita el curso del plan del grado, con sus competencias y docentes. */
	@Transactional
	public void quitarCurso(UUID id) {
		PlanCurso plan = obtenerPlan(id);
		obtenerAnioEditable(plan.getAnioEscolarId());
		planes.delete(plan);
	}

	@Transactional
	public List<DocenteSeccion> asignarDocentes(UUID id, SolicitudDocentes solicitud) {
		PlanCurso plan = obtenerPlan(id);
		obtenerAnioEditable(plan.getAnioEscolarId());
		Set<UUID> secciones = consultas.secciones(plan.getAnioEscolarId(), plan.getGradoCodigo()).stream()
				.map(SeccionGrado::id).collect(Collectors.toSet());
		Set<UUID> docentes = consultas.docentes().stream().map(ConsultasCatalogo.Docente::id)
				.collect(Collectors.toSet());
		List<DocenteAsignado> asignaciones = new ArrayList<>();
		Set<UUID> vistas = new HashSet<>();
		for (AsignacionDocente a : solicitud.asignaciones()) {
			if (!secciones.contains(a.seccionId())) {
				throw ExcepcionNegocio.invalido("La sección no es de %s %d."
						.formatted(consultas.nombreDeGrado(plan.getGradoCodigo()),
								obtenerAnio(plan.getAnioEscolarId()).anio()));
			}
			if (!vistas.add(a.seccionId())) {
				throw ExcepcionNegocio.invalido("Cada sección se asigna una sola vez.");
			}
			if (a.docenteId() == null) {
				continue;
			}
			if (!docentes.contains(a.docenteId())) {
				throw ExcepcionNegocio.invalido("Elige un usuario activo con rol Docente.");
			}
			asignaciones.add(new DocenteAsignado(a.seccionId(), a.docenteId()));
		}
		plan.asignarDocentes(asignaciones);
		planes.flush();
		return docentesDelCurso(id);
	}

	/**
	 * Copia al grado vacío los cursos de otro plan que siguen activos (y se dictan en el grado), sus horas y sus
	 * competencias activas. Los docentes no se copian (Figma: "Los docentes se asignan después").
	 */
	@Transactional
	public PlanGrado copiarPlan(SolicitudCopiaPlan solicitud) {
		AnioEscolar anio = obtenerAnioEditable(solicitud.anioEscolarId());
		String grado = solicitud.gradoCodigo();
		validarGrado(grado);
		obtenerAnio(solicitud.origenAnioEscolarId());
		if (!planes.findByAnioEscolarIdAndGradoCodigo(anio.id(), grado).isEmpty()) {
			throw ExcepcionNegocio.conflicto("%s ya tiene cursos en %d: solo se copia a un plan vacío."
					.formatted(consultas.nombreDeGrado(grado), anio.anio()));
		}
		List<PlanCurso> origen = planes.findByAnioEscolarIdAndGradoCodigo(solicitud.origenAnioEscolarId(),
				solicitud.origenGradoCodigo());
		if (origen.isEmpty()) {
			throw ExcepcionNegocio.invalido("El plan elegido no tiene cursos.");
		}
		Map<UUID, Curso> cursosPorId = porId(cursos.findAll(), Curso::getId);
		Map<UUID, Area> areasPorId = porId(areas.findAll(), Area::getId);
		Map<UUID, Competencia> competenciasPorId = porId(competencias.findAll(), Competencia::getId);
		List<PlanCurso> copias = new ArrayList<>();
		for (PlanCurso p : origen) {
			Curso curso = cursosPorId.get(p.getCursoId());
			if (!curso.estaActivo() || !areasPorId.get(curso.getAreaId()).estaActiva()
					|| !curso.getGrados().contains(grado)) {
				continue;
			}
			List<CompetenciaEvaluada> activas = p.getCompetencias().stream()
					.filter(c -> competenciasPorId.get(c.getCompetenciaId()).estaActiva())
					.map(c -> new CompetenciaEvaluada(c.getCompetenciaId(), c.getEscala(), c.getReglaNota(),
							c.getConsolidacion()))
					.toList();
			copias.add(new PlanCurso(anio.id(), grado, curso.getId(), p.getHorasSemanales(), activas));
		}
		if (copias.isEmpty()) {
			throw ExcepcionNegocio.conflicto(
					"Ningún curso del plan elegido sigue activo o se dicta en %s.".formatted(
							consultas.nombreDeGrado(grado)));
		}
		int horas = copias.stream().mapToInt(PlanCurso::getHorasSemanales).sum();
		consultas.bloquesSemanales(anio.id(), grado).ifPresent(bloques -> {
			if (horas > bloques) {
				throw horasExcedidas(horas, bloques);
			}
		});
		planes.saveAll(copias);
		planes.flush();
		return plan(anio.id(), grado);
	}

	// ---------- Auxiliares ----------

	/**
	 * Convierte las competencias marcadas. Deben ser del área del curso, sin repetirse, con una regla que corresponda a
	 * su escala; una competencia inactiva solo se acepta si el plan ya la tenía.
	 */
	private List<CompetenciaEvaluada> competenciasEvaluadas(Curso curso, List<SolicitudCompetenciaPlan> pedidas,
			Set<UUID> yaElegidas) {
		Map<UUID, Competencia> delArea = porId(competencias.findByAreaIdOrderByOrdenAscNombreAsc(curso.getAreaId()),
				Competencia::getId);
		Set<UUID> vistas = new HashSet<>();
		List<CompetenciaEvaluada> resultado = new ArrayList<>();
		for (SolicitudCompetenciaPlan p : pedidas) {
			Competencia c = delArea.get(p.competenciaId());
			if (c == null) {
				throw ExcepcionNegocio.invalido("La competencia no es del área de %s.".formatted(curso.getNombre()));
			}
			if (!vistas.add(c.getId())) {
				throw ExcepcionNegocio.invalido("La competencia \"%s\" está repetida.".formatted(c.getNombre()));
			}
			if (!c.estaActiva() && !yaElegidas.contains(c.getId())) {
				throw ExcepcionNegocio.conflicto(
						"La competencia \"%s\" está inactiva: ya no puede agregarse.".formatted(c.getNombre()));
			}
			if (!p.reglaNota().sirvePara(p.escala())) {
				throw ExcepcionNegocio.invalido("La agregación de ítems de \"%s\" no corresponde a su escala."
						.formatted(c.getNombre()));
			}
			resultado.add(new CompetenciaEvaluada(c.getId(), p.escala(), p.reglaNota(), p.consolidacion()));
		}
		return resultado;
	}

	/** Las horas del grado no superan los bloques semanales de su estructura horaria (Figma: "Disponibles: 45"). */
	private void validarHoras(UUID anioEscolarId, String grado, UUID excepto, int horas) {
		consultas.bloquesSemanales(anioEscolarId, grado).ifPresent(bloques -> {
			int total = horas + planes.findByAnioEscolarIdAndGradoCodigo(anioEscolarId, grado).stream()
					.filter(p -> !p.getId().equals(excepto)).mapToInt(PlanCurso::getHorasSemanales).sum();
			if (total > bloques) {
				throw horasExcedidas(total, bloques);
			}
		});
	}

	private static ExcepcionNegocio horasExcedidas(int horas, int bloques) {
		return ExcepcionNegocio.conflicto(
				"El grado tendría %d horas semanales y la estructura horaria solo tiene %d bloques."
						.formatted(horas, bloques));
	}

	/** VIGESIMAL, CUALITATIVA, MIXTA o null si el curso aún no tiene competencias. */
	private static String escala(PlanCurso p) {
		var escalas = p.getCompetencias().stream().map(CompetenciaEvaluada::getEscala).distinct().toList();
		if (escalas.isEmpty()) {
			return null;
		}
		return escalas.size() == 1 ? escalas.get(0).name() : "MIXTA";
	}

	private static int areaOrden(PlanCursoResumen f, Map<UUID, Curso> cursos, Map<UUID, Area> areas) {
		return areas.get(cursos.get(f.cursoId()).getAreaId()).getOrden();
	}

	private AnioEscolar obtenerAnio(UUID id) {
		return consultas.anioEscolar(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("El año escolar no existe."));
	}

	/** Un año cerrado conserva su plan tal como quedó. */
	private AnioEscolar obtenerAnioEditable(UUID id) {
		AnioEscolar anio = obtenerAnio(id);
		if (anio.estaCerrado()) {
			throw ExcepcionNegocio.conflicto(
					"El año escolar %d está cerrado: su plan de evaluación ya no se modifica.".formatted(anio.anio()));
		}
		return anio;
	}

	private void validarGrado(String codigo) {
		if (!consultas.codigosDeGrado().contains(codigo)) {
			throw ExcepcionNegocio.invalido("El grado \"%s\" no existe.".formatted(codigo));
		}
	}

	private PlanCurso obtenerPlan(UUID id) {
		return planes.findById(id)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El curso no está en el plan de evaluación."));
	}

	private Curso obtenerCurso(UUID id) {
		return cursos.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("El curso no existe."));
	}

	private Area obtenerArea(UUID id) {
		return areas.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("El área no existe."));
	}

	private static <T> Map<UUID, T> porId(List<T> lista, Function<T, UUID> id) {
		return lista.stream().collect(Collectors.toMap(id, Function.identity(), (a, b) -> a, LinkedHashMap::new));
	}

}
