package com.galiledu.horarios.aplicacion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.AmbientesActivos.AmbienteDisponible;
import com.galiledu.horarios.aplicacion.DatosDeSeccion.DatosSeccion;
import com.galiledu.horarios.aplicacion.DtosHorarios.AmbienteOpcion;
import com.galiledu.horarios.aplicacion.DtosHorarios.BloqueJornada;
import com.galiledu.horarios.aplicacion.DtosHorarios.CeldaDelHorario;
import com.galiledu.horarios.aplicacion.DtosHorarios.CursoOpcion;
import com.galiledu.horarios.aplicacion.DtosHorarios.EstructuraDelHorario;
import com.galiledu.horarios.aplicacion.DtosHorarios.HorarioDeSeccion;
import com.galiledu.horarios.aplicacion.DtosHorarios.JornadaDelDia;
import com.galiledu.horarios.aplicacion.DtosHorarios.OpcionesDeHorario;
import com.galiledu.horarios.aplicacion.DtosHorarios.RecesoJornada;
import com.galiledu.horarios.aplicacion.DtosHorarios.Referencia;
import com.galiledu.horarios.aplicacion.DtosHorarios.SeccionDelHorario;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.JornadaEstructura;
import com.galiledu.horarios.aplicacion.PlanDeSeccion.CursoDelPlan;
import com.galiledu.horarios.dominio.HorarioBloque;
import com.galiledu.horarios.dominio.HorarioSeccion;
import com.galiledu.horarios.infraestructura.HorarioBloqueRepositorio;
import com.galiledu.horarios.infraestructura.HorarioSeccionRepositorio;

/**
 * Servicio del módulo Horarios (HU-45). DUEÑA: Valeria. Fase 1: solo lectura (la matriz de la sección y las opciones
 * del modal "Asignar bloque horario"). Lo de otros módulos entra por los puertos de solo lectura.
 * <p>
 * Errores: sección inexistente → 404; sin horario guardado y sin estructura principal del nivel → 409. Un año CERRADO
 * no es un error: la respuesta sigue siendo 200 con {@code editable = false}.
 */
@Service
public class ServicioHorarios {
	/** Con estos estados del año se puede editar el horario. */
	private static final Set<String> ESTADOS_EDITABLES = Set.of("PLANIFICADO", "ACTIVO");

	private final DatosDeSeccion secciones;
	private final HorarioSeccionRepositorio horarios;
	private final HorarioBloqueRepositorio bloques;
	private final ResolutorDeJornada resolutor;
	private final PlanDeSeccion plan;
	private final AmbientesActivos ambientes;
	private final ConversorDeBloques conversor;
	private final ValidadorCruces validadorCruces;

	public ServicioHorarios(DatosDeSeccion secciones, HorarioSeccionRepositorio horarios,
			HorarioBloqueRepositorio bloques, ResolutorDeJornada resolutor, PlanDeSeccion plan,
			AmbientesActivos ambientes, ConversorDeBloques conversor, ValidadorCruces validadorCruces) {
		this.secciones = secciones;
		this.horarios = horarios;
		this.bloques = bloques;
		this.resolutor = resolutor;
		this.plan = plan;
		this.ambientes = ambientes;
		this.conversor = conversor;
		this.validadorCruces = validadorCruces;
	}

	/**
	 * La matriz de la sección. Con horario guardado usa su estructura y sus bloques; sin él, la estructura principal
	 * del nivel y ninguna celda. No escribe nada.
	 */
	@Transactional(readOnly = true)
	public HorarioDeSeccion horarioDeSeccion(UUID seccionId) {
		DatosSeccion datos = obtenerSeccion(seccionId);
		Optional<HorarioSeccion> cabecera = horarios.findBySeccionId(seccionId);
		JornadaEstructura jornada = resolutor.resolver(datos, cabecera);
		List<HorarioBloque> guardados = cabecera.map(h -> bloques.findByHorarioSeccionId(h.getId()))
				.orElse(List.of()).stream()
				.sorted(Comparator.comparing(HorarioBloque::getDia).thenComparingInt(HorarioBloque::getNumeroBloque))
				.toList();

		Map<UUID, CursoDelPlan> cursos = cursosPorId(seccionId, guardados);
		Map<UUID, String> nombresAmbiente = ambientes
				.nombresPorId(guardados.stream().map(HorarioBloque::getAmbienteId).distinct().toList());
		Map<String, List<CruceDetectado>> cruces = crucesPorCelda(datos, guardados);

		List<CeldaDelHorario> celdas = new ArrayList<>();
		for (HorarioBloque b : guardados) {
			CursoDelPlan curso = cursos.get(b.getPlanCursoId());
			Referencia docente = curso == null || curso.docenteId() == null
					? null
					: new Referencia(curso.docenteId(), curso.docenteNombre());
			celdas.add(new CeldaDelHorario(b.getDia(), b.getNumeroBloque(), b.getPlanCursoId(),
					curso == null ? null : curso.nombre(), docente,
					new Referencia(b.getAmbienteId(), nombresAmbiente.get(b.getAmbienteId())),
					cruces.getOrDefault(clave(b.getDia(), b.getNumeroBloque()), List.of())));
		}
		return new HorarioDeSeccion(seccionDe(datos), cabecera.map(HorarioSeccion::getVersion).orElse(null),
				ESTADOS_EDITABLES.contains(datos.estadoAnio()),
				new EstructuraDelHorario(jornada.estructuraId(), jornada.codigo(), jornada.principal()),
				jornadaDe(jornada), celdas);
	}

	/** Lo que el modal "Asignar bloque horario" ofrece: los cursos del plan con sus horas y los ambientes activos. */
	@Transactional(readOnly = true)
	public OpcionesDeHorario opcionesDeSeccion(UUID seccionId) {
		DatosSeccion datos = obtenerSeccion(seccionId);
		// Un bloque asignado = una hora semanal del plan (ver la ficha de Catálogo académico).
		Map<UUID, Long> asignadas = horarios.findBySeccionId(seccionId)
				.map(h -> bloques.findByHorarioSeccionId(h.getId())).orElse(List.of()).stream()
				.collect(Collectors.groupingBy(HorarioBloque::getPlanCursoId, Collectors.counting()));
		List<CursoOpcion> cursos = plan.cursosDelPlan(seccionId).stream()
				.map(c -> new CursoOpcion(c.planCursoId(), c.nombre(), c.horasSemanales(),
						asignadas.getOrDefault(c.planCursoId(), 0L),
						c.docenteId() == null ? null : new Referencia(c.docenteId(), c.docenteNombre())))
				.toList();
		List<AmbienteOpcion> opcionesAmbiente = ambientes.activos().stream()
				.map((AmbienteDisponible a) -> new AmbienteOpcion(a.id(), a.nombre(), a.tipo(), a.aforoMaximo()))
				.toList();
		return new OpcionesDeHorario(datos.ambienteId(), cursos, opcionesAmbiente);
	}

	/**
	 * Cuántos bloques tiene asignados el curso del plan en el horario de la sección. Lo usará Académico. TODO
	 * (Valeria, HU-45): contar las filas de horarios.horario_bloque; hoy devuelve 0.
	 */
	public int bloquesDeCurso(UUID planCursoId, UUID seccionId) {
		return 0;
	}

	// ---------- Apoyo ----------

	private DatosSeccion obtenerSeccion(UUID seccionId) {
		return secciones.buscar(seccionId).orElseThrow(() -> ExcepcionNegocio.noEncontrado("La sección no existe."));
	}

	private Map<UUID, CursoDelPlan> cursosPorId(UUID seccionId, Collection<HorarioBloque> guardados) {
		Map<UUID, CursoDelPlan> cursos = new HashMap<>();
		plan.cursosPorId(seccionId, guardados.stream().map(HorarioBloque::getPlanCursoId).distinct().toList())
				.forEach(c -> cursos.put(c.planCursoId(), c));
		return cursos;
	}

	/** Pasa las celdas guardadas por el conversor y el validador, y reparte los cruces por (día, bloque). */
	private Map<String, List<CruceDetectado>> crucesPorCelda(DatosSeccion datos, List<HorarioBloque> guardados) {
		if (guardados.isEmpty()) {
			return Map.of();
		}
		List<CeldaHorario> celdas = guardados.stream()
				.map(b -> new CeldaHorario(b.getDia(), b.getNumeroBloque(), b.getPlanCursoId(), b.getAmbienteId()))
				.toList();
		List<BloqueSolicitado> solicitados = conversor.aBloquesSolicitados(datos.id(), celdas);
		Map<String, List<CruceDetectado>> porCelda = new HashMap<>();
		for (CruceDetectado cruce : validadorCruces.buscarCruces(datos.anioEscolarId(), datos.id(), solicitados)) {
			porCelda.computeIfAbsent(clave(cruce.dia(), cruce.numeroBloque()), k -> new ArrayList<>()).add(cruce);
		}
		return porCelda;
	}

	private static String clave(DiaSemana dia, int numeroBloque) {
		return dia + ":" + numeroBloque;
	}

	private static SeccionDelHorario seccionDe(DatosSeccion d) {
		Referencia tutor = d.tutorId() == null ? null : new Referencia(d.tutorId(), d.tutorNombre());
		return new SeccionDelHorario(d.id(), d.nombre(), d.gradoNombre(), d.nivel(), d.anioEscolarId(),
				d.ambienteId(), tutor);
	}

	private static JornadaDelDia jornadaDe(JornadaEstructura j) {
		return new JornadaDelDia(j.dias(),
				j.bloques().stream().map(b -> new BloqueJornada(b.numero(), b.inicio().toString(), b.fin().toString()))
						.toList(),
				j.recesos().stream()
						.map(r -> new RecesoJornada(r.despuesDeBloque(), r.inicio().toString(), r.fin().toString()))
						.toList());
	}
}
