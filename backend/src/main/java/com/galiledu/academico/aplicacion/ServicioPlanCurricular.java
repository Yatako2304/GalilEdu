package com.galiledu.academico.aplicacion;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.academico.aplicacion.DtosAcademico.AreaDetalle;
import com.galiledu.academico.aplicacion.DtosAcademico.AreaResumen;
import com.galiledu.academico.aplicacion.DtosAcademico.CompetenciaResumen;
import com.galiledu.academico.aplicacion.DtosAcademico.CursoResumen;
import com.galiledu.academico.aplicacion.DtosAcademico.ResumenPlanCurricular;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudArea;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudCompetencia;
import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudCurso;
import com.galiledu.academico.dominio.Area;
import com.galiledu.academico.dominio.Competencia;
import com.galiledu.academico.dominio.Curso;
import com.galiledu.academico.infraestructura.AreaRepositorio;
import com.galiledu.academico.infraestructura.CompetenciaRepositorio;
import com.galiledu.academico.infraestructura.ConsultasCatalogo;
import com.galiledu.academico.infraestructura.CursoRepositorio;
import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * Plan curricular del Personal administrativo (Figma CI-14): áreas (HU-52), competencias (HU-53) y cursos (HU-54).
 * Nada se borra: se desactiva y ya no puede agregarse a planes nuevos (RF-126 a RF-128).
 */
@Service
public class ServicioPlanCurricular {
	private final AreaRepositorio areas;
	private final CompetenciaRepositorio competencias;
	private final CursoRepositorio cursos;
	private final ConsultasCatalogo consultas;

	public ServicioPlanCurricular(AreaRepositorio areas, CompetenciaRepositorio competencias, CursoRepositorio cursos,
			ConsultasCatalogo consultas) {
		this.areas = areas;
		this.competencias = competencias;
		this.cursos = cursos;
		this.consultas = consultas;
	}

	// ---------- Consultas ----------

	@Transactional(readOnly = true)
	public ResumenPlanCurricular resumen() {
		var todasAreas = areas.findAll();
		var todasCompetencias = competencias.findAll();
		var todosCursos = cursos.findAll();
		return new ResumenPlanCurricular(
				todasAreas.stream().filter(Area::estaActiva).count(), todasAreas.size(),
				todasCompetencias.stream().filter(Competencia::estaActiva).count(), todasCompetencias.size(),
				todosCursos.stream().filter(Curso::estaActivo).count(), todosCursos.size(),
				consultas.anioActivo().orElse(null));
	}

	/** Lista de áreas con cuántas competencias y cursos tiene cada una (activos e inactivos). */
	@Transactional(readOnly = true)
	public List<AreaResumen> listarAreas() {
		Map<UUID, Long> competenciasPorArea = competencias.findAll().stream()
				.collect(Collectors.groupingBy(Competencia::getAreaId, Collectors.counting()));
		Map<UUID, Long> cursosPorArea = cursos.findAll().stream()
				.collect(Collectors.groupingBy(Curso::getAreaId, Collectors.counting()));
		return areas.findAllByOrderByOrdenAscNombreAsc().stream()
				.map(a -> AreaResumen.de(a, competenciasPorArea.getOrDefault(a.getId(), 0L),
						cursosPorArea.getOrDefault(a.getId(), 0L)))
				.toList();
	}

	@Transactional(readOnly = true)
	public AreaDetalle detalleArea(UUID id) {
		return detalle(obtenerArea(id));
	}

	// ---------- Áreas (HU-52) ----------

	@Transactional
	public AreaDetalle crearArea(SolicitudArea solicitud) {
		String nombre = solicitud.nombre().strip();
		if (areas.existsByNombreIgnoreCase(nombre)) {
			throw areaDuplicada(nombre);
		}
		short orden = (short) (areas.maximoOrden() + 1);
		return detalle(areas.save(new Area(nombre, texto(solicitud.descripcion()), orden)));
	}

	@Transactional
	public AreaDetalle actualizarArea(UUID id, SolicitudArea solicitud) {
		Area area = obtenerArea(id);
		String nombre = solicitud.nombre().strip();
		if (areas.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
			throw areaDuplicada(nombre);
		}
		area.editar(nombre, texto(solicitud.descripcion()));
		return detalle(area);
	}

	/** Sus competencias y cursos se conservan; el área ya no puede agregarse a planes de evaluación nuevos. */
	@Transactional
	public AreaDetalle desactivarArea(UUID id) {
		Area area = obtenerArea(id);
		area.desactivar();
		return detalle(area);
	}

	@Transactional
	public AreaDetalle reactivarArea(UUID id) {
		Area area = obtenerArea(id);
		area.reactivar();
		return detalle(area);
	}

	// ---------- Competencias (HU-53) ----------

	@Transactional
	public CompetenciaResumen crearCompetencia(UUID areaId, SolicitudCompetencia solicitud) {
		Area area = obtenerAreaActiva(areaId, "competencias");
		String nombre = solicitud.nombre().strip();
		if (competencias.existsByAreaIdAndNombreIgnoreCase(area.getId(), nombre)) {
			throw competenciaDuplicada(area, nombre);
		}
		short orden = (short) (competencias.maximoOrden(area.getId()) + 1);
		return CompetenciaResumen.de(
				competencias.save(new Competencia(area.getId(), nombre, texto(solicitud.descripcion()), orden)));
	}

	@Transactional
	public CompetenciaResumen actualizarCompetencia(UUID id, SolicitudCompetencia solicitud) {
		Competencia competencia = obtenerCompetencia(id);
		String nombre = solicitud.nombre().strip();
		if (competencias.existsByAreaIdAndNombreIgnoreCaseAndIdNot(competencia.getAreaId(), nombre, id)) {
			throw competenciaDuplicada(obtenerArea(competencia.getAreaId()), nombre);
		}
		competencia.editar(nombre, texto(solicitud.descripcion()));
		return CompetenciaResumen.de(competencia);
	}

	@Transactional
	public CompetenciaResumen desactivarCompetencia(UUID id) {
		Competencia competencia = obtenerCompetencia(id);
		competencia.desactivar();
		return CompetenciaResumen.de(competencia);
	}

	@Transactional
	public CompetenciaResumen reactivarCompetencia(UUID id) {
		Competencia competencia = obtenerCompetencia(id);
		competencia.reactivar();
		return CompetenciaResumen.de(competencia);
	}

	// ---------- Cursos (HU-54) ----------

	@Transactional
	public CursoResumen crearCurso(UUID areaId, SolicitudCurso solicitud) {
		Area area = obtenerAreaActiva(areaId, "cursos");
		String nombre = solicitud.nombre().strip();
		if (cursos.existsByNombreIgnoreCase(nombre)) {
			throw cursoDuplicado(nombre);
		}
		List<String> catalogo = consultas.codigosDeGrado();
		Set<String> grados = validarGrados(solicitud.grados(), catalogo);
		return CursoResumen.de(cursos.save(new Curso(area.getId(), nombre, grados)), catalogo);
	}

	/** Un grado en el que el curso ya está planificado no se puede quitar (fk_plan_curso_grado). */
	@Transactional
	public CursoResumen actualizarCurso(UUID id, SolicitudCurso solicitud) {
		Curso curso = obtenerCurso(id);
		String nombre = solicitud.nombre().strip();
		if (cursos.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
			throw cursoDuplicado(nombre);
		}
		List<String> catalogo = consultas.codigosDeGrado();
		Set<String> grados = validarGrados(solicitud.grados(), catalogo);
		for (String planificado : consultas.gradosPlanificados(id)) {
			if (!grados.contains(planificado)) {
				throw ExcepcionNegocio.conflicto("No puedes quitar %s: el curso ya está en su plan de evaluación."
						.formatted(consultas.nombreDeGrado(planificado)));
			}
		}
		curso.editar(nombre, grados);
		return CursoResumen.de(curso, catalogo);
	}

	@Transactional
	public CursoResumen desactivarCurso(UUID id) {
		Curso curso = obtenerCurso(id);
		curso.desactivar();
		return CursoResumen.de(curso, consultas.codigosDeGrado());
	}

	@Transactional
	public CursoResumen reactivarCurso(UUID id) {
		Curso curso = obtenerCurso(id);
		curso.reactivar();
		return CursoResumen.de(curso, consultas.codigosDeGrado());
	}

	// ---------- Auxiliares ----------

	private AreaDetalle detalle(Area area) {
		List<String> catalogo = consultas.codigosDeGrado();
		return new AreaDetalle(area.getId(), area.getNombre(), area.getDescripcion(), area.getEstado(),
				competencias.findByAreaIdOrderByOrdenAscNombreAsc(area.getId()).stream()
						.map(CompetenciaResumen::de).toList(),
				cursos.findByAreaIdOrderByNombreAsc(area.getId()).stream()
						.map(c -> CursoResumen.de(c, catalogo)).toList());
	}

	private Area obtenerArea(UUID id) {
		return areas.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("El área no existe."));
	}

	/** A un área inactiva no se le agregan competencias ni cursos: primero hay que reactivarla. */
	private Area obtenerAreaActiva(UUID id, String que) {
		Area area = obtenerArea(id);
		if (!area.estaActiva()) {
			throw ExcepcionNegocio.conflicto(
					"El área %s está inactiva. Reactívala para agregarle %s.".formatted(area.getNombre(), que));
		}
		return area;
	}

	private Competencia obtenerCompetencia(UUID id) {
		return competencias.findById(id)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La competencia no existe."));
	}

	private Curso obtenerCurso(UUID id) {
		return cursos.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("El curso no existe."));
	}

	private static Set<String> validarGrados(Set<String> grados, List<String> catalogo) {
		Set<String> validos = new LinkedHashSet<>();
		for (String grado : grados) {
			if (grado == null || !catalogo.contains(grado)) {
				throw ExcepcionNegocio.invalido("El grado \"%s\" no existe.".formatted(grado));
			}
			validos.add(grado);
		}
		return validos;
	}

	private static ExcepcionNegocio areaDuplicada(String nombre) {
		return ExcepcionNegocio.conflicto("Ya existe un área con el nombre \"%s\".".formatted(nombre));
	}

	private static ExcepcionNegocio competenciaDuplicada(Area area, String nombre) {
		return ExcepcionNegocio.conflicto(
				"El área %s ya tiene la competencia \"%s\".".formatted(area.getNombre(), nombre));
	}

	private static ExcepcionNegocio cursoDuplicado(String nombre) {
		return ExcepcionNegocio.conflicto("Ya existe un curso con el nombre \"%s\".".formatted(nombre));
	}

	/** La descripción es opcional: vacía se guarda como NULL. */
	private static String texto(String valor) {
		return valor == null || valor.isBlank() ? null : valor.strip();
	}
}
