package com.galiledu.horarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.galiledu.compartido.RelojConfig;
import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.compartido.web.Pagina;
import com.galiledu.configuracion.aplicacion.ServicioEstructurasHorarias;
import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.AmbientesActivos.AmbienteDisponible;
import com.galiledu.horarios.aplicacion.CalendarioDelAnio.AnioConPeriodos;
import com.galiledu.horarios.aplicacion.CalendarioDelAnio.PeriodoDelAnio;
import com.galiledu.horarios.aplicacion.ConsultaDeReservas.ReservaLeida;
import com.galiledu.horarios.aplicacion.DatosDeSeccion.DatosSeccion;
import com.galiledu.horarios.aplicacion.DtosHorarios.Referencia;
import com.galiledu.horarios.aplicacion.DtosReservas.EstadoDeReserva;
import com.galiledu.horarios.aplicacion.DtosReservas.ReservaDeAmbiente;
import com.galiledu.horarios.aplicacion.DtosReservas.SolicitudEdicionReserva;
import com.galiledu.horarios.aplicacion.DtosReservas.SolicitudReserva;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.BloqueDeJornada;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.JornadaEstructura;
import com.galiledu.horarios.dominio.ReservaAmbiente;
import com.galiledu.horarios.infraestructura.HorarioSeccionRepositorio;
import com.galiledu.horarios.infraestructura.ReservaAmbienteRepositorio;

/**
 * HU-48: el orden de las operaciones del servicio, sin Spring ni BD (las reglas en sí están en ReglasDeReservaTests).
 */
class ServicioReservasAmbienteTests {
	private static final UUID ANIO = UUID.randomUUID();
	private static final UUID SECCION = UUID.randomUUID();
	private static final UUID OTRA_SECCION = UUID.randomUUID();
	private static final UUID LAB = UUID.randomUUID();
	private static final UUID AULA = UUID.randomUUID();
	private static final UUID RESPONSABLE = UUID.randomUUID();
	private static final UUID USUARIO = UUID.randomUUID();
	private static final UUID ID = UUID.randomUUID();
	/** Un lunes del IV bimestre de 2026. */
	private static final LocalDate LUNES = LocalDate.of(2026, 11, 23);

	/** 1 de marzo de 2026, 10:00 en Lima. */
	private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-01T15:00:00Z"), RelojConfig.LIMA);

	private static final AnioConPeriodos ANIO_2026 = new AnioConPeriodos(ANIO, 2026, "ACTIVO",
			LocalDate.of(2026, 3, 2), LocalDate.of(2026, 12, 18),
			List.of(new PeriodoDelAnio(4, "IV Bimestre", LocalDate.of(2026, 10, 19), LocalDate.of(2026, 12, 18))));
	private static final JornadaEstructura JORNADA = new JornadaEstructura(UUID.randomUUID(), "EH-SEC-26", true,
			List.of(DiaSemana.LUN, DiaSemana.MAR, DiaSemana.MIE, DiaSemana.JUE, DiaSemana.VIE),
			List.of(new BloqueDeJornada(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
					new BloqueDeJornada(2, LocalTime.of(8, 45), LocalTime.of(9, 30))),
			List.of());

	private final DatosDeSeccion secciones = mock(DatosDeSeccion.class);
	private final HorarioSeccionRepositorio horarios = mock(HorarioSeccionRepositorio.class);
	private final ResolutorDeJornada resolutor = mock(ResolutorDeJornada.class);
	private final AmbientesActivos ambientes = mock(AmbientesActivos.class);
	private final MatriculadosDeSeccion matriculados = mock(MatriculadosDeSeccion.class);
	private final ResponsablesElegibles responsables = mock(ResponsablesElegibles.class);
	private final CalendarioDelAnio calendario = mock(CalendarioDelAnio.class);
	private final OcupacionHorarios ocupacion = mock(OcupacionHorarios.class);
	private final ReservaAmbienteRepositorio repositorio = mock(ReservaAmbienteRepositorio.class);
	private final ConsultaDeReservas consulta = mock(ConsultaDeReservas.class);
	private final ServicioEstructurasHorarias estructuras = mock(ServicioEstructurasHorarias.class);

	private ServicioReservasAmbiente servicio = armar(RELOJ);

	private ServicioReservasAmbiente armar(Clock reloj) {
		var servicio = new ServicioReservasAmbiente(secciones, horarios, resolutor, ambientes, matriculados,
				responsables, calendario, ocupacion, repositorio, consulta, new ReglasDeReserva(), estructuras, reloj);
		// Un escenario válido: lo que cada prueba quiere romper, lo rompe ella.
		when(secciones.buscar(SECCION)).thenReturn(Optional.of(new DatosSeccion(SECCION, "B", "SEC-4",
				"4.° de secundaria", "SECUNDARIA", ANIO, "ACTIVO", null, null, null)));
		when(ambientes.activos())
				.thenReturn(List.of(new AmbienteDisponible(AULA, "Aula 101", "A-101", "AULA_REGULAR", 30),
						new AmbienteDisponible(LAB, "Laboratorio de ciencias", "LAB-01", "ESPACIO_ESPECIAL", 30)));
		when(calendario.deAnio(ANIO)).thenReturn(Optional.of(ANIO_2026));
		when(matriculados.de(SECCION)).thenReturn(3);
		when(responsables.existe(RESPONSABLE)).thenReturn(true);
		when(horarios.findBySeccionId(SECCION)).thenReturn(Optional.empty());
		when(resolutor.resolver(any(), any())).thenReturn(JORNADA);
		when(ocupacion.bloquesOcupados(ANIO, null)).thenReturn(List.of());
		when(repositorio.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
		when(consulta.porId(any())).thenAnswer(i -> Optional.of(leida(ID, "ACTIVA", LUNES, 0)));
		return servicio;
	}

	private static ReservaLeida leida(UUID id, String estado, LocalDate fecha, long version) {
		return new ReservaLeida(id, LAB, "LAB-01", "Laboratorio de ciencias", "Experimento", ANIO, "SEC-4", SECCION,
				"4.° B", RESPONSABLE, "María Fernández Ríos", fecha, LocalTime.of(8, 0), LocalTime.of(9, 0), estado,
				version);
	}

	private static SolicitudReserva solicitud(UUID ambiente) {
		return new SolicitudReserva(ambiente, SECCION, RESPONSABLE, "Experimento", LUNES, LocalTime.of(8, 0),
				LocalTime.of(9, 0));
	}

	private static SolicitudEdicionReserva edicion(long version) {
		return new SolicitudEdicionReserva(version, LAB, SECCION, RESPONSABLE, "Experimento", LUNES,
				LocalTime.of(8, 0), LocalTime.of(9, 0));
	}

	/** Una reserva ya guardada, con id y versión como si viniera de la BD. */
	private static ReservaAmbiente guardada(LocalDate fecha, long version) {
		ReservaAmbiente reserva = ReservaAmbiente.crear(LAB, SECCION, RESPONSABLE, "Experimento", fecha,
				LocalTime.of(8, 0), LocalTime.of(9, 0), USUARIO);
		ReflectionTestUtils.setField(reserva, "id", ID);
		ReflectionTestUtils.setField(reserva, "version", version);
		return reserva;
	}

	private static void assertEstado(Runnable accion, HttpStatus estado, String mensaje) {
		assertThatThrownBy(accion::run).isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
			assertThat(e.estado()).isEqualTo(estado);
			assertThat(e.getMessage()).startsWith(mensaje);
		});
	}

	// ---------- Crear: orden de operaciones ----------

	@DisplayName("Crea con el creador y devuelve la reserva leida")
	@Test
	void creaConElCreadorYDevuelveLaReservaLeida() {
		ReservaDeAmbiente creada = servicio.crear(solicitud(LAB), USUARIO);

		var capturada = org.mockito.ArgumentCaptor.forClass(ReservaAmbiente.class);
		verify(repositorio).saveAndFlush(capturada.capture());
		assertThat(capturada.getValue().getCreadoPor()).isEqualTo(USUARIO);
		assertThat(capturada.getValue().getSeccionId()).isEqualTo(SECCION);
		assertThat(creada.seccion().etiqueta()).isEqualTo("4.° B");
		assertThat(creada.horaInicio()).isEqualTo("08:00");
		assertThat(creada.estado()).isEqualTo(EstadoDeReserva.PENDIENTE);
		assertThat(creada.editable()).isTrue();
	}

	@DisplayName("Toma el bloqueo del año antes de cargar reservas y bloques y guarda después")
	@Test
	void tomaElBloqueoDelAnioAntesDeCargarReservasYBloquesYGuardaDespues() {
		servicio.crear(solicitud(LAB), USUARIO);

		InOrder orden = inOrder(estructuras, repositorio, ocupacion);
		orden.verify(estructuras).bloquearAnioParaHorarios(ANIO);
		orden.verify(repositorio).superpuestasPorAmbiente(eq(LAB), eq(LUNES), any(), any(), eq(null));
		orden.verify(ocupacion).bloquesOcupados(ANIO, null);
		orden.verify(repositorio).saveAndFlush(any());
	}

	@DisplayName("Las reglas de forma fallan antes de tomar el bloqueo y no guardan nada")
	@Test
	void lasReglasDeFormaFallanAntesDeTomarElBloqueoYNoGuardanNada() {
		assertEstado(() -> servicio.crear(solicitud(AULA), USUARIO), HttpStatus.BAD_REQUEST,
				"Elige un espacio especial activo.");

		verifyNoInteractions(estructuras, ocupacion);
		verify(repositorio, never()).saveAndFlush(any());
	}

	@DisplayName("Un responsable no elegible se rechaza con un400 antes del bloqueo")
	@Test
	void unResponsableNoElegibleSeRechazaConUn400AntesDelBloqueo() {
		when(responsables.existe(RESPONSABLE)).thenReturn(false);

		assertEstado(() -> servicio.crear(solicitud(LAB), USUARIO), HttpStatus.BAD_REQUEST,
				"Elige un docente o tutor activo como responsable.");
		verifyNoInteractions(estructuras);
	}

	@DisplayName("Una sección inexistente responde404")
	@Test
	void unaSeccionInexistenteResponde404() {
		var otra = new SolicitudReserva(LAB, OTRA_SECCION, RESPONSABLE, "Experimento", LUNES, LocalTime.of(8, 0),
				LocalTime.of(9, 0));
		when(secciones.buscar(OTRA_SECCION)).thenReturn(Optional.empty());

		assertEstado(() -> servicio.crear(otra, USUARIO), HttpStatus.NOT_FOUND, "La sección no existe.");
	}

	@DisplayName("Un año cerrado responde409 sin bloquear")
	@Test
	void unAnioCerradoResponde409SinBloquear() {
		when(calendario.deAnio(ANIO)).thenReturn(Optional.of(new AnioConPeriodos(ANIO, 2026, "CERRADO",
				ANIO_2026.inicio(), ANIO_2026.fin(), ANIO_2026.periodos())));

		assertEstado(() -> servicio.crear(solicitud(LAB), USUARIO), HttpStatus.CONFLICT,
				"No se puede reservar un espacio en un año cerrado.");
		verifyNoInteractions(estructuras);
	}

	@DisplayName("Un cruce responde409 con título y no guarda")
	@Test
	void unCruceResponde409ConTituloYNoGuarda() {
		var otraSeccionConElLab = new BloqueOcupado(OTRA_SECCION, DiaSemana.LUN, LocalTime.of(8, 0),
				LocalTime.of(8, 45), null, LAB, "5.° A", "Física", "Luis Paredes", "LAB-01");
		when(ocupacion.bloquesOcupados(ANIO, null)).thenReturn(List.of(otraSeccionConElLab));

		assertThatThrownBy(() -> servicio.crear(solicitud(LAB), USUARIO)).isInstanceOfSatisfying(
				ExcepcionNegocio.class, e -> {
					assertThat(e.estado()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(e.getMessage())
							.isEqualTo(
									"Ambiente no disponible: 5.° A ya usa LAB-01 para Física el lunes de 8:00 a 8:45.");
				});
		verify(repositorio, never()).saveAndFlush(any());
	}

	@DisplayName("El cruce de la misma sección con el mismo ambiente se rechaza con un400")
	@Test
	void elCruceDeLaMismaSeccionConElMismoAmbienteSeRechazaConUn400() {
		var propia = new BloqueOcupado(SECCION, DiaSemana.LUN, LocalTime.of(8, 0), LocalTime.of(8, 45), null, LAB,
				"4.° B", "Química", "Luis Paredes", "LAB-01");
		when(ocupacion.bloquesOcupados(ANIO, null)).thenReturn(List.of(propia));

		assertEstado(() -> servicio.crear(solicitud(LAB), USUARIO), HttpStatus.BAD_REQUEST,
				"4.° B ya tiene LAB-01 en su horario el lunes de 8:00 a 8:45; no necesita reserva.");
	}

	@DisplayName("Ahora se toma del reloj inyectado para la regla3")
	@Test
	void ahoraSeTomaDelRelojInyectadoParaLaRegla3() {
		var pasada = Clock.fixed(Instant.parse("2026-11-23T14:00:00Z"), RelojConfig.LIMA); // lunes 09:00 en Lima
		servicio = armar(pasada);

		assertEstado(() -> servicio.crear(solicitud(LAB), USUARIO), HttpStatus.BAD_REQUEST,
				"No se puede reservar en una fecha u hora que ya pasó.");
	}

	// ---------- Reprogramar ----------

	@DisplayName("Reprogramar con una versión vieja409 y no bloquea el año")
	@Test
	void reprogramarConUnaVersionVieja409YNoBloqueaElAnio() {
		when(repositorio.findById(ID)).thenReturn(Optional.of(guardada(LUNES, 3)));

		assertEstado(() -> servicio.reprogramar(ID, edicion(2)), HttpStatus.CONFLICT,
				"La reserva cambió mientras la editabas. Vuelve a cargarla.");
		verifyNoInteractions(estructuras);
	}

	@DisplayName("Reprogramar excluye la propia reserva en las tres busquedas")
	@Test
	void reprogramarExcluyeLaPropiaReservaEnLasTresBusquedas() {
		when(repositorio.findById(ID)).thenReturn(Optional.of(guardada(LUNES, 0)));

		servicio.reprogramar(ID, edicion(0));

		verify(repositorio).superpuestasPorAmbiente(eq(LAB), eq(LUNES), any(), any(), eq(ID));
		verify(repositorio).superpuestasPorSeccion(eq(SECCION), eq(LUNES), any(), any(), eq(ID));
		verify(repositorio).superpuestasPorResponsable(eq(RESPONSABLE), eq(LUNES), any(), any(), eq(ID));
	}

	@DisplayName("Reprogramar una reserva finalizada o cancelada responde409 sin bloquear")
	@Test
	void reprogramarUnaReservaFinalizadaOCanceladaResponde409SinBloquear() {
		var finalizada = guardada(LocalDate.of(2026, 2, 16), 0);
		when(repositorio.findById(ID)).thenReturn(Optional.of(finalizada));
		assertEstado(() -> servicio.reprogramar(ID, edicion(0)), HttpStatus.CONFLICT,
				"La reserva ya finalizó y no se puede modificar.");

		var cancelada = guardada(LUNES, 0);
		cancelada.cancelar();
		when(repositorio.findById(ID)).thenReturn(Optional.of(cancelada));
		assertEstado(() -> servicio.reprogramar(ID, edicion(0)), HttpStatus.CONFLICT, "La reserva ya fue cancelada.");

		verifyNoInteractions(estructuras);
	}

	@DisplayName("Un conflicto de versión al guardar se convierte en409")
	@Test
	void unConflictoDeVersionAlGuardarSeConvierteEn409() {
		when(repositorio.findById(ID)).thenReturn(Optional.of(guardada(LUNES, 0)));
		when(repositorio.saveAndFlush(any()))
				.thenThrow(new ObjectOptimisticLockingFailureException(ReservaAmbiente.class, ID));

		assertEstado(() -> servicio.reprogramar(ID, edicion(0)), HttpStatus.CONFLICT,
				"La reserva cambió mientras la editabas. Vuelve a cargarla.");
	}

	@DisplayName("Reprogramar una reserva inexistente responde404")
	@Test
	void reprogramarUnaReservaInexistenteResponde404() {
		when(repositorio.findById(ID)).thenReturn(Optional.empty());

		assertEstado(() -> servicio.reprogramar(ID, edicion(0)), HttpStatus.NOT_FOUND, "La reserva no existe.");
	}

	// ---------- Cancelar ----------

	@DisplayName("Cancelar deja la reserva cancelada sin tomar el bloqueo")
	@Test
	void cancelarDejaLaReservaCanceladaSinTomarElBloqueo() {
		var reserva = guardada(LUNES, 0);
		when(repositorio.findById(ID)).thenReturn(Optional.of(reserva));

		servicio.cancelar(ID, null);

		assertThat(reserva.estaCancelada()).isTrue();
		verify(repositorio).saveAndFlush(reserva);
		verifyNoInteractions(estructuras);
	}

	@DisplayName("Cancelar con una versión vieja409")
	@Test
	void cancelarConUnaVersionVieja409() {
		when(repositorio.findById(ID)).thenReturn(Optional.of(guardada(LUNES, 2)));

		assertEstado(() -> servicio.cancelar(ID, 1L), HttpStatus.CONFLICT,
				"La reserva cambió mientras la editabas. Vuelve a cargarla.");
		verify(repositorio, never()).saveAndFlush(any());
	}

	@DisplayName("Cancelar con la versión actual se permite")
	@Test
	void cancelarConLaVersionActualSePermite() {
		when(repositorio.findById(ID)).thenReturn(Optional.of(guardada(LUNES, 2)));

		servicio.cancelar(ID, 2L);

		verify(repositorio).saveAndFlush(any());
	}

	@DisplayName("Cancelar una finalizada o ya cancelada responde409")
	@Test
	void cancelarUnaFinalizadaOYaCanceladaResponde409() {
		when(repositorio.findById(ID)).thenReturn(Optional.of(guardada(LocalDate.of(2026, 2, 16), 0)));
		assertEstado(() -> servicio.cancelar(ID, null), HttpStatus.CONFLICT,
				"La reserva ya finalizó y no se puede modificar.");

		var cancelada = guardada(LUNES, 0);
		cancelada.cancelar();
		when(repositorio.findById(ID)).thenReturn(Optional.of(cancelada));
		assertEstado(() -> servicio.cancelar(ID, null), HttpStatus.CONFLICT, "La reserva ya fue cancelada.");

		verify(repositorio, never()).saveAndFlush(any());
	}

	@DisplayName("Cancelar una reserva inexistente responde404")
	@Test
	void cancelarUnaReservaInexistenteResponde404() {
		when(repositorio.findById(ID)).thenReturn(Optional.empty());

		assertEstado(() -> servicio.cancelar(ID, null), HttpStatus.NOT_FOUND, "La reserva no existe.");
	}

	// ---------- Listar: filtros con el reloj ----------

	/** Miércoles 18 de noviembre de 2026, 10:00 en Lima. */
	private ServicioReservasAmbiente conHoyMiercoles() {
		return armar(Clock.fixed(Instant.parse("2026-11-18T15:00:00Z"), RelojConfig.LIMA));
	}

	@DisplayName("Sin filtro lista la semana de hoy de lunes a domingo")
	@Test
	void sinFiltroListaLaSemanaDeHoyDeLunesADomingo() {
		servicio = conHoyMiercoles();

		servicio.listar(null, 1, 10);

		verify(consulta).contarActivasEntre(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 22));
		verify(consulta).activasEntre(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 22), 10, 0);
	}

	@DisplayName("El filtro mes cubre el mes calendario de hoy")
	@Test
	void elFiltroMesCubreElMesCalendarioDeHoy() {
		servicio = conHoyMiercoles();

		servicio.listar(FiltroDeReservas.MES, 1, 10);

		verify(consulta).activasEntre(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 10, 0);
	}

	@DisplayName("El filtro año cubre el año calendario de hoy")
	@Test
	void elFiltroAnioCubreElAnioCalendarioDeHoy() {
		servicio = conHoyMiercoles();

		servicio.listar(FiltroDeReservas.ANIO, 1, 10);

		verify(consulta).activasEntre(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 10, 0);
	}

	@DisplayName("La semana en domingo y en lunes queda completa")
	@Test
	void laSemanaEnDomingoYEnLunesQuedaCompleta() {
		var domingo = LocalDate.of(2026, 11, 22);
		var lunes = LocalDate.of(2026, 11, 16);

		assertThat(FiltroDeReservas.SEMANA.desde(domingo)).isEqualTo(lunes);
		assertThat(FiltroDeReservas.SEMANA.hasta(domingo)).isEqualTo(domingo);
		assertThat(FiltroDeReservas.SEMANA.desde(lunes)).isEqualTo(lunes);
		assertThat(FiltroDeReservas.SEMANA.hasta(lunes)).isEqualTo(domingo);
	}

	@DisplayName("La página se convierte en desplazamiento y el tamano se acota")
	@Test
	void laPaginaSeConvierteEnDesplazamientoYElTamanoSeAcota() {
		servicio = conHoyMiercoles();

		servicio.listar(FiltroDeReservas.ANIO, 3, 20);
		servicio.listar(FiltroDeReservas.ANIO, 0, 1000);
		servicio.listar(FiltroDeReservas.ANIO, 1, 0);

		verify(consulta).activasEntre(any(), any(), eq(20), eq(40L));
		verify(consulta).activasEntre(any(), any(), eq(100), eq(0L));
		verify(consulta).activasEntre(any(), any(), eq(1), eq(0L));
	}

	@DisplayName("El estado y el editable se calculan con el reloj")
	@Test
	void elEstadoYElEditableSeCalculanConElReloj() {
		servicio = conHoyMiercoles();
		var finalizada = leida(UUID.randomUUID(), "ACTIVA", LocalDate.of(2026, 11, 17), 4);
		var pendiente = leida(UUID.randomUUID(), "ACTIVA", LocalDate.of(2026, 11, 20), 0);
		when(consulta.contarActivasEntre(any(), any())).thenReturn(2L);
		when(consulta.activasEntre(any(), any(), eq(10), eq(0L))).thenReturn(List.of(pendiente, finalizada));

		Pagina<ReservaDeAmbiente> pagina = servicio.listar(FiltroDeReservas.SEMANA, 1, 10);

		assertThat(pagina.meta().total()).isEqualTo(2);
		assertThat(pagina.meta().page()).isEqualTo(1);
		assertThat(pagina.data()).extracting(ReservaDeAmbiente::estado, ReservaDeAmbiente::editable)
				.containsExactly(org.assertj.core.api.Assertions.tuple(EstadoDeReserva.PENDIENTE, true),
						org.assertj.core.api.Assertions.tuple(EstadoDeReserva.FINALIZADO, false));
		assertThat(pagina.data().get(1).version()).isEqualTo(4);
	}

	@DisplayName("Una reserva que termina justo ahora ya está finalizada")
	@Test
	void unaReservaQueTerminaJustoAhoraYaEstaFinalizada() {
		// Hoy 18/11 a las 10:00; termina a las 09:00 el mismo día y otra a las 10:00 exactas.
		servicio = conHoyMiercoles();
		var termino = new ReservaLeida(ID, LAB, "LAB-01", "Laboratorio", "Experimento", ANIO, "SEC-4", SECCION, "4.° B",
				RESPONSABLE, "María", LocalDate.of(2026, 11, 18), LocalTime.of(9, 0), LocalTime.of(10, 0), "ACTIVA", 0);
		when(consulta.porId(ID)).thenReturn(Optional.of(termino));

		assertThat(servicio.obtener(ID).estado()).isEqualTo(EstadoDeReserva.FINALIZADO);
	}

	// ---------- Obtener y opciones ----------

	@DisplayName("Obtener una reserva cancelada o inexistente responde404")
	@Test
	void obtenerUnaReservaCanceladaOInexistenteResponde404() {
		when(consulta.porId(ID)).thenReturn(Optional.of(leida(ID, "CANCELADA", LUNES, 1)));
		assertEstado(() -> servicio.obtener(ID), HttpStatus.NOT_FOUND, "La reserva no existe.");

		when(consulta.porId(ID)).thenReturn(Optional.empty());
		assertEstado(() -> servicio.obtener(ID), HttpStatus.NOT_FOUND, "La reserva no existe.");
	}

	@DisplayName("Las opciones solo traen espacios especiales por código y los responsables")
	@Test
	void lasOpcionesSoloTraenEspaciosEspecialesPorCodigoYLosResponsables() {
		var labDos = new AmbienteDisponible(UUID.randomUUID(), "Auditorio", "AUD-01", "ESPACIO_ESPECIAL", 180);
		when(ambientes.activos())
				.thenReturn(List.of(new AmbienteDisponible(LAB, "Laboratorio", "LAB-01", "ESPACIO_ESPECIAL", 30),
						new AmbienteDisponible(AULA, "Aula 101", "A-101", "AULA_REGULAR", 30), labDos));
		when(responsables.elegibles()).thenReturn(List.of(new Referencia(RESPONSABLE, "María Fernández Ríos")));

		var opciones = servicio.opciones();

		assertThat(opciones.ambientes()).extracting(a -> a.codigo()).containsExactly("AUD-01", "LAB-01");
		assertThat(opciones.responsables()).extracting(Referencia::nombre).containsExactly("María Fernández Ríos");
	}
}
