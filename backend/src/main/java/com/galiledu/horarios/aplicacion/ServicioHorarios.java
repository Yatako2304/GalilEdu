package com.galiledu.horarios.aplicacion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.ServicioEstructurasHorarias;
import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.AmbientesActivos.AmbienteDisponible;
import com.galiledu.horarios.aplicacion.DatosDeSeccion.DatosSeccion;
import com.galiledu.horarios.aplicacion.DtosHorarios.AmbienteOpcion;
import com.galiledu.horarios.aplicacion.DtosHorarios.AnioParaHorarios;
import com.galiledu.horarios.aplicacion.DtosHorarios.BloqueJornada;
import com.galiledu.horarios.aplicacion.DtosHorarios.CeldaDelHorario;
import com.galiledu.horarios.aplicacion.DtosHorarios.CursoOpcion;
import com.galiledu.horarios.aplicacion.DtosHorarios.EstructuraDelHorario;
import com.galiledu.horarios.aplicacion.DtosHorarios.GradoParaHorarios;
import com.galiledu.horarios.aplicacion.DtosHorarios.HorarioDeSeccion;
import com.galiledu.horarios.aplicacion.DtosHorarios.JornadaDelDia;
import com.galiledu.horarios.aplicacion.DtosHorarios.OpcionesDeHorario;
import com.galiledu.horarios.aplicacion.DtosHorarios.RecesoJornada;
import com.galiledu.horarios.aplicacion.DtosHorarios.Referencia;
import com.galiledu.horarios.aplicacion.DtosHorarios.SeccionDelHorario;
import com.galiledu.horarios.aplicacion.DtosHorarios.SeccionParaHorarios;
import com.galiledu.horarios.aplicacion.DtosHorarios.SolicitudHorario;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.JornadaEstructura;
import com.galiledu.horarios.aplicacion.PlanDeSeccion.CursoDelPlan;
import com.galiledu.horarios.dominio.HorarioBloque;
import com.galiledu.horarios.dominio.HorarioSeccion;
import com.galiledu.horarios.infraestructura.HorarioBloqueRepositorio;
import com.galiledu.horarios.infraestructura.HorarioSeccionRepositorio;

/**
 * Servicio del módulo Horarios (HU-45). DUEÑA: Valeria. Lectura (la matriz de la sección y las opciones del modal
 * "Asignar bloque horario") y escritura (el PUT de la matriz completa). Lo de otros módulos entra por los puertos de
 * solo lectura.
 * <p>
 * Errores de la lectura: sección inexistente → 404; sin horario guardado y sin estructura principal del nivel → 409.
 * Un año CERRADO no es un error al leer: la respuesta sigue siendo 200 con {@code editable = false}.
 */
@Service
public class ServicioHorarios {
	/** Con estos estados del año se puede editar el horario. */
	private static final Set<String> ESTADOS_EDITABLES = Set.of("PLANIFICADO", "ACTIVO");
	static final String MENSAJE_ANIO_CERRADO = "No se puede modificar el horario de un año cerrado.";

	private final DatosDeSeccion secciones;
	private final HorarioSeccionRepositorio horarios;
	private final HorarioBloqueRepositorio bloques;
	private final ResolutorDeJornada resolutor;
	private final PlanDeSeccion plan;
	private final AmbientesActivos ambientes;
	private final ConversorDeBloques conversor;
	private final ValidadorCruces validadorCruces;
	private final ReglasDelHorario reglas;
	private final ServicioEstructurasHorarias estructuras;
	private final AniosYSecciones aniosYSecciones;

	public ServicioHorarios(DatosDeSeccion secciones, HorarioSeccionRepositorio horarios,
			HorarioBloqueRepositorio bloques, ResolutorDeJornada resolutor, PlanDeSeccion plan,
			AmbientesActivos ambientes, ConversorDeBloques conversor, ValidadorCruces validadorCruces,
			ReglasDelHorario reglas, ServicioEstructurasHorarias estructuras, AniosYSecciones aniosYSecciones) {
		this.secciones = secciones;
		this.horarios = horarios;
		this.bloques = bloques;
		this.resolutor = resolutor;
		this.plan = plan;
		this.ambientes = ambientes;
		this.conversor = conversor;
		this.validadorCruces = validadorCruces;
		this.reglas = reglas;
		this.estructuras = estructuras;
		this.aniosYSecciones = aniosYSecciones;
	}

	/** Los años escolares para el selector del Coordinador, del más reciente al más antiguo. */
	@Transactional(readOnly = true)
	public List<AnioParaHorarios> aniosEscolares() {
		return aniosYSecciones.anios().stream().map(a -> new AnioParaHorarios(a.id(), a.anio(), a.estado())).toList();
	}

	/** Los grados del año con sus secciones para los selectores del Coordinador. 404 si el año no existe. */
	@Transactional(readOnly = true)
	public List<GradoParaHorarios> gradosDelAnio(UUID anioId) {
		return aniosYSecciones.gradosDelAnio(anioId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El año escolar no existe.")).stream()
				.map(g -> new GradoParaHorarios(g.codigo(), g.nombre(), g.nivel(), g.numero(),
						(int) g.secciones().stream().filter(x -> "ACTIVA".equals(x.estado())).count(),
						g.secciones().stream().map(x -> new SeccionParaHorarios(x.id(), x.nombre(), x.estado()))
								.toList()))
				.toList();
	}

	/**
	 * La matriz de la sección. Con horario guardado usa su estructura y sus bloques; sin él, la estructura principal
	 * del nivel y ninguna celda. No escribe nada.
	 */
	@Transactional(readOnly = true)
	public HorarioDeSeccion horarioDeSeccion(UUID seccionId) {
		DatosSeccion datos = obtenerSeccion(seccionId);
		return construirHorario(datos, horarios.findBySeccionId(seccionId));
	}

	/**
	 * Guarda la matriz COMPLETA de la sección (no un delta) y devuelve el estado resultante. Todo ocurre en una
	 * transacción: se bloquea el año ANTES de leer la versión, se valida y se aplica por diferencia, de modo que los
	 * bloques sin cambios conservan su id. La versión sube exactamente 1 solo si algo cambió.
	 * <p>
	 * Errores: 404 sección inexistente; 409 año cerrado, versión desactualizada, curso sin docente, horas semanales
	 * excedidas, cruces o falta de estructura principal; 400 por las reglas de forma de las celdas.
	 */
	@Transactional
	public HorarioDeSeccion guardarHorario(UUID seccionId, SolicitudHorario solicitud) {
		DatosSeccion datos = obtenerSeccion(seccionId);
		if ("CERRADO".equals(datos.estadoAnio())) {
			throw ExcepcionNegocio.conflicto(MENSAJE_ANIO_CERRADO);
		}
		// Primero el bloqueo y luego la lectura de la cabecera: así la versión que se compara no es una lectura vieja.
		estructuras.bloquearAnioParaHorarios(datos.anioEscolarId());
		Optional<HorarioSeccion> cabecera = horarios.findBySeccionId(seccionId);
		validarVersion(datos, cabecera, solicitud.version());

		List<CeldaHorario> celdas = solicitud.celdas().stream()
				.map(c -> new CeldaHorario(c.dia(), c.numeroBloque(), c.planCursoId(), c.ambienteId())).toList();
		if (cabecera.isEmpty() && celdas.isEmpty()) {
			return construirHorario(datos, cabecera);
		}

		JornadaEstructura jornada = resolutor.resolver(datos, cabecera);
		Map<UUID, CursoDelPlan> cursosDelPlan = new HashMap<>();
		plan.cursosDelPlan(seccionId).forEach(c -> cursosDelPlan.put(c.planCursoId(), c));
		Set<UUID> ambientesActivos = new HashSet<>();
		ambientes.activos().forEach(a -> ambientesActivos.add(a.id()));
		reglas.validar(datos, jornada, celdas, cursosDelPlan, ambientesActivos);
		validarCruces(datos, celdas);

		HorarioSeccion horario = aplicar(datos, cabecera, jornada, celdas);
		return construirHorario(datos, Optional.of(horario));
	}

	private HorarioDeSeccion construirHorario(DatosSeccion datos, Optional<HorarioSeccion> cabecera) {
		UUID seccionId = datos.id();
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

	/** Cuántos bloques tiene asignados el curso del plan en el horario de la sección. Lo usa Académico. */
	@Transactional(readOnly = true)
	public int bloquesDeCurso(UUID planCursoId, UUID seccionId) {
		return horarios.findBySeccionId(seccionId)
				.map(h -> (int) bloques.countByHorarioSeccionIdAndPlanCursoId(h.getId(), planCursoId)).orElse(0);
	}

	// ---------- Escritura ----------

	private static ExcepcionNegocio otraPersona(DatosSeccion datos) {
		return ExcepcionNegocio.conflicto("Otra persona modificó el horario de " + datos.gradoNombre() + " "
				+ datos.nombre() + ". Recarga para ver los cambios.");
	}

	/** La versión que llegó debe ser la actual; null significa "no había horario cuando se leyó". */
	private void validarVersion(DatosSeccion datos, Optional<HorarioSeccion> cabecera, Long versionRecibida) {
		boolean coincide = cabecera.isPresent()
				? versionRecibida != null && versionRecibida == cabecera.get().getVersion()
				: versionRecibida == null;
		if (!coincide) {
			throw otraPersona(datos);
		}
	}

	private void validarCruces(DatosSeccion datos, List<CeldaHorario> celdas) {
		List<BloqueSolicitado> solicitados = conversor.aBloquesSolicitados(datos.id(), celdas);
		if (solicitados.size() != celdas.size()) {
			// Las reglas ya garantizaron docente y bloque existentes: el conversor no debería omitir nada.
			throw new IllegalStateException("El conversor omitió " + (celdas.size() - solicitados.size())
					+ " celda(s) que ya habían pasado las reglas del horario.");
		}
		List<CruceDetectado> cruces = validadorCruces.buscarCruces(datos.anioEscolarId(), datos.id(), solicitados);
		if (!cruces.isEmpty()) {
			// Con varios cruces se muestra el primero: el mensaje se presenta tal cual en una sola notificación.
			throw ExcepcionNegocio.conflicto(MensajesCruce.conTitulo(cruces.get(0)));
		}
	}

	/**
	 * Aplica la matriz por diferencia con clave (día, bloque): elimina los que ya no están, actualiza los que cambiaron
	 * de curso o ambiente e inserta los nuevos. Los que no cambian no se tocan y conservan su id. Los DELETE se
	 * envían (flush) antes de los INSERT para no chocar con uq_horario_bloque_posicion.
	 */
	private HorarioSeccion aplicar(DatosSeccion datos, Optional<HorarioSeccion> cabecera, JornadaEstructura jornada,
			List<CeldaHorario> celdas) {
		boolean nueva = cabecera.isEmpty();
		HorarioSeccion horario = cabecera.orElseGet(() -> crearCabecera(datos, jornada));

		Map<String, HorarioBloque> existentes = new LinkedHashMap<>();
		if (!nueva) {
			bloques.findByHorarioSeccionId(horario.getId())
					.forEach(b -> existentes.put(clave(b.getDia(), b.getNumeroBloque()), b));
		}
		Map<String, CeldaHorario> deseadas = new LinkedHashMap<>();
		celdas.forEach(c -> deseadas.put(clave(c.dia(), c.numeroBloque()), c));

		List<HorarioBloque> aEliminar = existentes.entrySet().stream().filter(e -> !deseadas.containsKey(e.getKey()))
				.map(Map.Entry::getValue).toList();
		List<HorarioBloque> aInsertar = new ArrayList<>();
		int modificados = 0;
		for (Map.Entry<String, CeldaHorario> entrada : deseadas.entrySet()) {
			CeldaHorario celda = entrada.getValue();
			HorarioBloque actual = existentes.get(entrada.getKey());
			if (actual == null) {
				aInsertar.add(HorarioBloque.crear(horario.getId(), celda.dia(), celda.numeroBloque(),
						celda.planCursoId(), celda.ambienteId()));
			} else if (actual.reasignar(celda.planCursoId(), celda.ambienteId())) {
				modificados++;
			}
		}

		if (!aEliminar.isEmpty()) {
			bloques.deleteAll(aEliminar);
			bloques.flush();
		}
		bloques.saveAll(aInsertar);
		bloques.flush();

		boolean huboCambios = !aEliminar.isEmpty() || !aInsertar.isEmpty() || modificados > 0;
		// Una cabecera nueva nace con la versión 0; una existente sube exactamente 1 si algo cambió. Se toca la
		// cabecera (y no OPTIMISTIC_FORCE_INCREMENT) para que la versión nueva ya esté en la entidad al responder.
		if (!nueva && huboCambios) {
			horario.marcarModificado();
			try {
				horarios.saveAndFlush(horario);
			} catch (ObjectOptimisticLockingFailureException versionCambiada) {
				throw otraPersona(datos);
			}
		}
		return horario;
	}

	private HorarioSeccion crearCabecera(DatosSeccion datos, JornadaEstructura jornada) {
		try {
			return horarios.saveAndFlush(HorarioSeccion.crear(datos.id(), jornada.estructuraId()));
		} catch (DataIntegrityViolationException violacion) {
			// Dos primeros guardados a la vez: el segundo choca con la UNIQUE de seccion_id.
			if (String.valueOf(violacion.getMessage()).contains("uq_horario_seccion_seccion")) {
				throw otraPersona(datos);
			}
			throw violacion;
		}
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
