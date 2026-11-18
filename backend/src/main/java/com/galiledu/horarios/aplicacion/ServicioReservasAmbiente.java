package com.galiledu.horarios.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.compartido.web.Pagina;
import com.galiledu.configuracion.aplicacion.ServicioEstructurasHorarias;
import com.galiledu.horarios.aplicacion.AmbientesActivos.AmbienteDisponible;
import com.galiledu.horarios.aplicacion.CalendarioDelAnio.AnioConPeriodos;
import com.galiledu.horarios.aplicacion.ConsultaDeReservas.ReservaLeida;
import com.galiledu.horarios.aplicacion.DatosDeSeccion.DatosSeccion;
import com.galiledu.horarios.aplicacion.DtosHorarios.AmbienteOpcion;
import com.galiledu.horarios.aplicacion.DtosHorarios.Referencia;
import com.galiledu.horarios.aplicacion.DtosReservas.AmbienteDeReserva;
import com.galiledu.horarios.aplicacion.DtosReservas.EstadoDeReserva;
import com.galiledu.horarios.aplicacion.DtosReservas.OpcionesDeReserva;
import com.galiledu.horarios.aplicacion.DtosReservas.ReservaDeAmbiente;
import com.galiledu.horarios.aplicacion.DtosReservas.SeccionDeReserva;
import com.galiledu.horarios.aplicacion.DtosReservas.SolicitudEdicionReserva;
import com.galiledu.horarios.aplicacion.DtosReservas.SolicitudReserva;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.JornadaEstructura;
import com.galiledu.horarios.aplicacion.ReglasDeReserva.DatosDeReserva;
import com.galiledu.horarios.aplicacion.ReglasDeReserva.ReservaVigente;
import com.galiledu.horarios.dominio.ReservaAmbiente;
import com.galiledu.horarios.infraestructura.HorarioSeccionRepositorio;
import com.galiledu.horarios.infraestructura.ReservaAmbienteRepositorio;

/**
 * Reserva de espacios especiales para una sección (HU-48, RF-122), para el Coordinador. Las reglas están en
 * {@link ReglasDeReserva}; aquí se cargan los datos y se decide el orden.
 * <p>
 * Orden al crear o reprogramar: (1) validaciones de forma, que no necesitan el candado y responden 400 o 409 de estado;
 * (2) el bloqueo del año ({@code bloquearAnioParaHorarios}, el mismo del PUT del horario), que serializa las
 * escrituras de horarios y reservas del año; (3) recién entonces se cargan las reservas cercanas y los bloques de los
 * horarios, y se buscan los cruces (409 con el primero, con título). Cancelar no crea ocupación nueva, así que no toma
 * el bloqueo: lo protege la versión de la reserva.
 * <p>
 * Limitación conocida: el PUT del horario semanal no mira las reservas, así que un horario guardado después puede
 * superponerse a una reserva existente.
 */
@Service
public class ServicioReservasAmbiente {
	static final String MENSAJE_CAMBIO = "La reserva cambió mientras la editabas. Vuelve a cargarla.";
	static final int TAMANO_POR_DEFECTO = 10;
	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

	private final DatosDeSeccion secciones;
	private final HorarioSeccionRepositorio horarios;
	private final ResolutorDeJornada resolutor;
	private final AmbientesActivos ambientes;
	private final MatriculadosDeSeccion matriculados;
	private final ResponsablesElegibles responsables;
	private final CalendarioDelAnio calendario;
	private final OcupacionHorarios ocupacion;
	private final ReservaAmbienteRepositorio repositorio;
	private final ConsultaDeReservas consulta;
	private final ReglasDeReserva reglas;
	private final ServicioEstructurasHorarias estructuras;
	private final Clock reloj;

	public ServicioReservasAmbiente(DatosDeSeccion secciones, HorarioSeccionRepositorio horarios,
			ResolutorDeJornada resolutor, AmbientesActivos ambientes, MatriculadosDeSeccion matriculados,
			ResponsablesElegibles responsables, CalendarioDelAnio calendario, OcupacionHorarios ocupacion,
			ReservaAmbienteRepositorio repositorio, ConsultaDeReservas consulta, ReglasDeReserva reglas,
			ServicioEstructurasHorarias estructuras, Clock reloj) {
		this.secciones = secciones;
		this.horarios = horarios;
		this.resolutor = resolutor;
		this.ambientes = ambientes;
		this.matriculados = matriculados;
		this.responsables = responsables;
		this.calendario = calendario;
		this.ocupacion = ocupacion;
		this.repositorio = repositorio;
		this.consulta = consulta;
		this.reglas = reglas;
		this.estructuras = estructuras;
		this.reloj = reloj;
	}

	// ---------- Lectura ----------

	/** Los espacios especiales activos (por código) y quienes pueden ser responsables (por nombre). */
	@Transactional(readOnly = true)
	public OpcionesDeReserva opciones() {
		List<AmbienteOpcion> espacios = ambientes.activos().stream()
				.filter(a -> ReglasDeReserva.ESPACIO_ESPECIAL.equals(a.tipo()))
				.sorted(Comparator.comparing(AmbienteDisponible::codigo))
				.map(a -> new AmbienteOpcion(a.id(), a.nombre(), a.codigo(), a.tipo(), a.aforoMaximo())).toList();
		return new OpcionesDeReserva(espacios, responsables.elegibles());
	}

	/**
	 * El historial de reservas ACTIVAS cuya fecha cae en el rango del filtro (por defecto, la semana de hoy), de la más
	 * reciente a la más antigua. {@code pagina} empieza en 1.
	 */
	@Transactional(readOnly = true)
	public Pagina<ReservaDeAmbiente> listar(FiltroDeReservas filtro, int pagina, int tamano) {
		LocalDateTime ahora = ahora();
		LocalDate hoy = ahora.toLocalDate();
		FiltroDeReservas rango = filtro == null ? FiltroDeReservas.SEMANA : filtro;
		int numero = Math.max(1, pagina);
		int tam = Math.clamp(tamano, 1, 100);
		LocalDate desde = rango.desde(hoy);
		LocalDate hasta = rango.hasta(hoy);
		long total = consulta.contarActivasEntre(desde, hasta);
		List<ReservaLeida> filas = consulta.activasEntre(desde, hasta, tam, (long) (numero - 1) * tam);
		return Pagina.de(new PageImpl<>(filas, PageRequest.of(numero - 1, tam), total), f -> aDto(f, ahora));
	}

	/** Una reserva activa. 404 si no existe o ya fue cancelada («Eliminar» la quita de todas las vistas). */
	@Transactional(readOnly = true)
	public ReservaDeAmbiente obtener(UUID id) {
		return consulta.porId(id).filter(r -> "ACTIVA".equals(r.estado()))
				.map(r -> aDto(r, ahora())).orElseThrow(ServicioReservasAmbiente::reservaInexistente);
	}

	// ---------- Escritura ----------

	@Transactional
	public ReservaDeAmbiente crear(SolicitudReserva solicitud, UUID usuarioId) {
		DatosDeReserva datos = new DatosDeReserva(solicitud.ambienteId(), solicitud.seccionId(),
				solicitud.responsableId(), solicitud.fecha(), solicitud.horaInicio(), solicitud.horaFin());
		validarYBuscarCruces(datos, null);
		ReservaAmbiente reserva = ReservaAmbiente.crear(datos.ambienteId(), datos.seccionId(), datos.responsableId(),
				solicitud.actividad(), datos.fecha(), datos.horaInicio(), datos.horaFin(), usuarioId);
		return guardar(reserva);
	}

	/** 404 si no existe; 409 si la versión es vieja, o si está cancelada o finalizada; el resto, como al crear. */
	@Transactional
	public ReservaDeAmbiente reprogramar(UUID id, SolicitudEdicionReserva solicitud) {
		ReservaAmbiente reserva = repositorio.findById(id).orElseThrow(ServicioReservasAmbiente::reservaInexistente);
		comprobarVersion(reserva, solicitud.version());
		reglas.validarEditable(reserva, ahora());
		DatosDeReserva datos = new DatosDeReserva(solicitud.ambienteId(), solicitud.seccionId(),
				solicitud.responsableId(), solicitud.fecha(), solicitud.horaInicio(), solicitud.horaFin());
		validarYBuscarCruces(datos, id);
		reserva.reprogramar(datos.ambienteId(), datos.seccionId(), datos.responsableId(), solicitud.actividad(),
				datos.fecha(), datos.horaInicio(), datos.horaFin());
		return guardar(reserva);
	}

	/**
	 * «Eliminar»: la reserva queda CANCELADA y deja de contar para los cruces. {@code version} es opcional; si llega,
	 * debe ser la actual. 404 si no existe; 409 si ya está cancelada o finalizada.
	 */
	@Transactional
	public void cancelar(UUID id, Long version) {
		ReservaAmbiente reserva = repositorio.findById(id).orElseThrow(ServicioReservasAmbiente::reservaInexistente);
		if (version != null) {
			comprobarVersion(reserva, version);
		}
		reglas.validarEditable(reserva, ahora());
		reserva.cancelar();
		guardarCambios(reserva);
	}

	// ---------- Interno ----------

	/**
	 * Las validaciones de forma, el bloqueo del año y los cruces. {@code excluirId} es la reserva que se edita (null al
	 * crear).
	 */
	private void validarYBuscarCruces(DatosDeReserva datos, UUID excluirId) {
		DatosSeccion seccion = secciones.buscar(datos.seccionId())
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La sección no existe."));
		List<AmbienteDisponible> activos = ambientes.activos();
		AmbienteDisponible ambiente = activos.stream().filter(a -> a.id().equals(datos.ambienteId())).findFirst()
				.orElse(null);
		reglas.validarAmbiente(ambiente);
		AnioConPeriodos anio = calendario.deAnio(seccion.anioEscolarId())
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El año escolar no existe."));
		reglas.validarAnio(anio);
		reglas.validarAforo(ambiente, matriculados.de(seccion.id()));
		if (!responsables.existe(datos.responsableId())) {
			throw ExcepcionNegocio.invalido("Elige un docente o tutor activo como responsable.");
		}
		JornadaEstructura jornada = resolutor.resolver(seccion, horarios.findBySeccionId(seccion.id()));
		reglas.validarFechaYHora(datos, ahora(), anio, jornada);

		// Con el bloqueo tomado, lo que se lee ya incluye lo que confirmó cualquier escritura anterior del año.
		estructuras.bloquearAnioParaHorarios(seccion.anioEscolarId());
		List<ReservaVigente> vigentes = reservasCercanas(datos, excluirId);
		List<BloqueOcupado> bloques = ocupacion.bloquesOcupados(seccion.anioEscolarId(), null);
		Map<UUID, String> tipos = new HashMap<>();
		activos.forEach(a -> tipos.put(a.id(), a.tipo()));
		reglas.buscarCruces(datos, excluirId, vigentes, bloques, tipos).stream().findFirst().ifPresent(c -> {
			throw c.aExcepcion();
		});
	}

	/** Las reservas ACTIVAS que se superponen por ambiente, sección o responsable, con sus nombres. */
	private List<ReservaVigente> reservasCercanas(DatosDeReserva datos, UUID excluirId) {
		Map<UUID, ReservaAmbiente> porId = new LinkedHashMap<>();
		LocalDate fecha = datos.fecha();
		LocalTime inicio = datos.horaInicio();
		LocalTime fin = datos.horaFin();
		repositorio.superpuestasPorAmbiente(datos.ambienteId(), fecha, inicio, fin, excluirId)
				.forEach(r -> porId.put(r.getId(), r));
		repositorio.superpuestasPorSeccion(datos.seccionId(), fecha, inicio, fin, excluirId)
				.forEach(r -> porId.put(r.getId(), r));
		repositorio.superpuestasPorResponsable(datos.responsableId(), fecha, inicio, fin, excluirId)
				.forEach(r -> porId.put(r.getId(), r));
		Map<UUID, ReservaLeida> nombres = new HashMap<>();
		consulta.porIds(porId.keySet()).forEach(l -> nombres.put(l.id(), l));
		return porId.values().stream().map(r -> {
			ReservaLeida l = nombres.get(r.getId());
			return new ReservaVigente(r.getId(), r.getAmbienteId(), r.getSeccionId(), r.getResponsableId(),
					r.getFecha(), r.getHoraInicio(), r.getHoraFin(), l == null ? "" : l.seccion(),
					l == null ? "" : l.ambienteNombre(), l == null ? "" : l.responsable(), r.getActividad());
		}).toList();
	}

	private void comprobarVersion(ReservaAmbiente reserva, long versionRecibida) {
		if (reserva.getVersion() != versionRecibida) {
			throw ExcepcionNegocio.conflicto(MENSAJE_CAMBIO);
		}
	}

	private ReservaDeAmbiente guardar(ReservaAmbiente reserva) {
		guardarCambios(reserva);
		return aDto(consulta.porId(reserva.getId()).orElseThrow(ServicioReservasAmbiente::reservaInexistente),
				ahora());
	}

	/**
	 * Guarda y fuerza la escritura: si otra persona cambió la fila, el conflicto sale aquí y no al cerrar la
	 * transacción.
	 */
	private void guardarCambios(ReservaAmbiente reserva) {
		try {
			repositorio.saveAndFlush(reserva);
		} catch (ObjectOptimisticLockingFailureException e) {
			throw ExcepcionNegocio.conflicto(MENSAJE_CAMBIO);
		}
	}

	private LocalDateTime ahora() {
		return LocalDateTime.now(reloj);
	}

	private static ExcepcionNegocio reservaInexistente() {
		return ExcepcionNegocio.noEncontrado("La reserva no existe.");
	}

	private static ReservaDeAmbiente aDto(ReservaLeida r, LocalDateTime ahora) {
		boolean finalizada = !r.fecha().atTime(r.horaFin()).isAfter(ahora);
		boolean activa = "ACTIVA".equals(r.estado());
		return new ReservaDeAmbiente(r.id(),
				new AmbienteDeReserva(r.ambienteId(), r.ambienteCodigo(), r.ambienteNombre()),
				r.actividad(), r.anioId(), r.gradoCodigo(), new SeccionDeReserva(r.seccionId(), r.seccion()),
				new Referencia(r.responsableId(), r.responsable()), r.fecha(), r.horaInicio().format(HORA),
				r.horaFin().format(HORA), finalizada ? EstadoDeReserva.FINALIZADO : EstadoDeReserva.PENDIENTE,
				activa && !finalizada, r.version());
	}
}
