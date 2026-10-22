package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.compartido.web.Pagina;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudTarifario;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.DocumentoSolicitud;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.DocumentoVista;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.FilaPeriodoMatricula;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.FormularioPeriodoMatricula;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.SeccionVacante;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.SolicitudPeriodoMatricula;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.VacanteSeccion;
import com.galiledu.configuracion.aplicacion.ServicioPeriodoMatricula;
import com.galiledu.configuracion.aplicacion.ServicioTarifario;
import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.EstadoVisiblePeriodoMatricula;

/**
 * HU-23: reglas del servicio del periodo de matrícula con los datos de prueba. El reloj empieza el 15/09/2026 a las
 * 10:00 (Lima): el periodo 2027 (15/09 08:00 al 31/10 23:59) está ABIERTO. Antes del 15/09 está PROGRAMADO y después
 * del
 * 31/10, CERRADO. El tarifario 2027 de los datos de prueba está EN_CONFIGURACION: las pruebas que lo necesitan lo
 * confirman dentro de la prueba. Para crear se usan años nuevos (2028, 2029) insertados por SQL.
 */
class ServicioPeriodoMatriculaIT extends PruebaIntegracionConfiguracion {
	private static final UUID ANIO_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");
	private static final UUID ANIO_2027 = UUID.fromString("30000000-0000-0000-0000-000000002027");
	private static final UUID MATRICULA_2027 = UUID.fromString("50000000-0000-0000-0000-000000000002");
	private static final UUID SOFIA = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final LocalTime HORA = LocalTime.of(10, 0);

	@Autowired
	private ServicioPeriodoMatricula servicio;
	@Autowired
	private ServicioTarifario tarifarios;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private RelojPrueba reloj;

	private static LocalDate fecha(int anio, int mes, int dia) {
		return LocalDate.of(anio, mes, dia);
	}

	private static Instant instante(int anio, int mes, int dia, int hora) {
		return fecha(anio, mes, dia).atTime(hora, 0).atZone(com.galiledu.compartido.RelojConfig.LIMA).toInstant();
	}

	private static void rechaza(HttpStatus estado, String mensaje, ThrowingCallable operacion) {
		assertThatThrownBy(operacion).isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
			assertThat(e.estado()).isEqualTo(estado);
			assertThat(e.getMessage()).isEqualTo(mensaje);
		});
	}

	private static DocumentoSolicitud doc(String nombre) {
		return new DocumentoSolicitud(null, nombre, null);
	}

	private static SolicitudPeriodoMatricula solicitud(UUID anio, LocalDate inicio, LocalDate cierre,
			List<VacanteSeccion> vacantes, List<DocumentoSolicitud> documentos) {
		return new SolicitudPeriodoMatricula(anio, inicio, HORA, cierre, HORA, vacantes, documentos);
	}

	/** Un año PLANIFICADO sin periodo, con secciones A de 1.° a 5.° de secundaria (capacidad 30). */
	private UUID anioConSecciones(int anio) {
		UUID id = anioSinSecciones(anio, "PLANIFICADO");
		jdbc.sql("""
				INSERT INTO configuracion.seccion (anio_escolar_id, grado_codigo, nombre, capacidad_maxima)
				SELECT :id, g.codigo, 'A', 30 FROM configuracion.grado g WHERE g.nivel = 'SECUNDARIA'""")
				.param("id", id).update();
		return id;
	}

	private UUID anioSinSecciones(int anio, String estado) {
		UUID id = UUID.randomUUID();
		jdbc.sql("""
				INSERT INTO configuracion.anio_escolar (id, anio, fecha_inicio, fecha_fin, regimen, estado)
				VALUES (:id, :anio, :inicio, :fin, 'TRIMESTRAL', :estado)""")
				.param("id", id).param("anio", anio).param("inicio", fecha(anio, 3, 1))
				.param("fin", fecha(anio, 12, 15))
				.param("estado", estado).update();
		entityManager.clear();
		return id;
	}

	private void confirmarTarifario(UUID anio) {
		tarifarios.confirmar(anio, new SolicitudTarifario(new BigDecimal("500"), new BigDecimal("450"),
				new BigDecimal("620"), 5, new BigDecimal("2"), new BigDecimal("10")));
	}

	/** Las secciones de un año que todavía no tiene periodo. */
	private List<SeccionVacante> seccionesDeAnioNuevo(UUID anio) {
		return servicio.formularioNuevo(anio).secciones();
	}

	private UUID periodo2027() {
		return periodoDe(ANIO_2027);
	}

	private List<SeccionVacante> secciones2027() {
		return servicio.ver(periodo2027()).secciones();
	}

	/** Los documentos de 2027 como los manda el formulario: con su id, en su orden. */
	private List<DocumentoSolicitud> documentos2027() {
		return servicio.ver(periodo2027()).documentos().stream()
				.map(d -> new DocumentoSolicitud(d.id(), d.nombre(), d.descripcion())).toList();
	}

	private void matriculasPorSql(UUID anio, UUID seccion, int cantidad) {
		UUID[] estudiantes = {UUID.fromString("20000000-0000-0000-0000-000000000001"), SOFIA};
		for (int i = 0; i < cantidad; i++) {
			jdbc.sql("""
					INSERT INTO matricula.matricula (estudiante_id, anio_escolar_id, grado_codigo, seccion_id,
					                                 tipo_estudiante, estado)
					VALUES (:estudiante, :anio, 'SEC-1', :seccion, 'NUEVO', 'SECCION_RESERVADA')""")
					.param("estudiante", estudiantes[i]).param("anio", anio).param("seccion", seccion).update();
		}
		entityManager.clear(); // el conteo de alumnos de la sección es una fórmula que JPA ya pudo haber cargado
	}

	private UUID periodoDe(UUID anio) {
		return jdbc.sql("SELECT id FROM matricula.periodo_matricula WHERE anio_escolar_id = ?").param(anio)
				.query(UUID.class).single();
	}

	private int capacidad(UUID seccion) {
		return jdbc.sql("SELECT capacidad_maxima FROM configuracion.seccion WHERE id = ?").param(seccion)
				.query(Integer.class).single();
	}

	private int periodosDelAnio(UUID anio) {
		return jdbc.sql("SELECT count(*) FROM matricula.periodo_matricula WHERE anio_escolar_id = ?").param(anio)
				.query(Integer.class).single();
	}

	private String estadoGuardado(UUID anio) {
		return jdbc.sql("SELECT estado FROM matricula.periodo_matricula WHERE anio_escolar_id = ?").param(anio)
				.query(String.class).single();
	}

	// ---------- Estado, vigente y listado ----------

	@Test
	void vigenteEsElAbiertoAhoraOElProximoProgramadoOVacio() {
		// reloj por defecto: 15/09/2026 10:00, el periodo 2027 acaba de abrir
		var abierto = servicio.vigente().orElseThrow();
		assertThat(abierto.anio()).isEqualTo(2027);
		assertThat(abierto.nombre()).isEqualTo("Matrícula 2027");
		assertThat(abierto.estado()).isEqualTo(EstadoVisiblePeriodoMatricula.ABIERTO);
		assertThat(abierto.bloquearPorMorosidad()).isTrue();
		assertThat(abierto.permitirExtemporanea()).isTrue();
		assertThat(abierto.vacantesTotales()).isEqualTo(300);
		assertThat(abierto.matriculasRegistradas()).isEqualTo(1);
		assertThat(abierto.cierre()).isEqualTo(Instant.parse("2026-11-01T04:59:00Z"));

		reloj.fijarEn(fecha(2026, 9, 1));
		var proximo = servicio.vigente().orElseThrow();
		assertThat(proximo.anio()).isEqualTo(2027);
		assertThat(proximo.estado()).isEqualTo(EstadoVisiblePeriodoMatricula.PROGRAMADO);

		reloj.fijarEn(fecha(2026, 11, 15));
		assertThat(servicio.vigente()).isEmpty();
	}

	@Test
	void estadoEnCalculaElEstadoDeUnPeriodoEnCualquierInstante() {
		UUID p2027 = periodo2027();

		assertThat(servicio.estadoEn(p2027, instante(2026, 9, 14, 12)))
				.isEqualTo(EstadoVisiblePeriodoMatricula.PROGRAMADO);
		assertThat(servicio.estadoEn(p2027, instante(2026, 10, 1, 12)))
				.isEqualTo(EstadoVisiblePeriodoMatricula.ABIERTO);
		assertThat(servicio.estadoEn(p2027, instante(2026, 11, 2, 12)))
				.isEqualTo(EstadoVisiblePeriodoMatricula.CERRADO);
		// el de 2026 está guardado como CERRADO
		assertThat(servicio.estadoEn(periodoDe(ANIO_2026), instante(2025, 12, 1, 12)))
				.isEqualTo(EstadoVisiblePeriodoMatricula.CERRADO);
		rechaza(HttpStatus.NOT_FOUND, "El periodo de matrícula no existe.",
				() -> servicio.estadoEn(UUID.randomUUID(), Instant.now()));
	}

	@Test
	void elListadoTraeElEstadoCalculadoLasMatriculasYElVigente() {
		Pagina<FilaPeriodoMatricula> pagina = servicio.listar(null, null, 1, 10);

		assertThat(pagina.meta().total()).isEqualTo(2);
		assertThat(pagina.data()).extracting(FilaPeriodoMatricula::anio).containsExactly(2027, 2026);
		FilaPeriodoMatricula f2027 = pagina.data().get(0);
		assertThat(f2027.nombre()).isEqualTo("Matrícula 2027");
		assertThat(f2027.estado()).isEqualTo(EstadoVisiblePeriodoMatricula.ABIERTO);
		assertThat(f2027.vigente()).isTrue();
		assertThat(f2027.matriculas()).isEqualTo(1);
		assertThat(pagina.data().get(1).estado()).isEqualTo(EstadoVisiblePeriodoMatricula.CERRADO);
		assertThat(pagina.data().get(1).vigente()).isFalse();
	}

	@Test
	void elListadoSeFiltraPorAnioYPorEstadoYSePagina() {
		assertThat(servicio.listar(2026, null, 1, 10).data()).extracting(FilaPeriodoMatricula::anio)
				.containsExactly(2026);
		assertThat(servicio.listar(null, EstadoVisiblePeriodoMatricula.ABIERTO, 1, 10).data())
				.extracting(FilaPeriodoMatricula::anio).containsExactly(2027);
		assertThat(servicio.listar(null, EstadoVisiblePeriodoMatricula.PROGRAMADO, 1, 10).data()).isEmpty();
		reloj.fijarEn(fecha(2026, 9, 1));
		assertThat(servicio.listar(null, EstadoVisiblePeriodoMatricula.PROGRAMADO, 1, 10).data())
				.extracting(FilaPeriodoMatricula::anio).containsExactly(2027);

		Pagina<FilaPeriodoMatricula> segunda = servicio.listar(null, null, 2, 1);
		assertThat(segunda.data()).extracting(FilaPeriodoMatricula::anio).containsExactly(2026);
		assertThat(segunda.meta().total()).isEqualTo(2);
		assertThat(segunda.meta().totalPages()).isEqualTo(2);
	}

	@Test
	void losAniosDisponiblesSonLosPlanificadosOActivosSinPeriodo() {
		assertThat(servicio.aniosDisponibles()).isEmpty();
		UUID id2028 = anioConSecciones(2028);
		anioSinSecciones(2029, "CERRADO");

		assertThat(servicio.aniosDisponibles()).extracting(a -> a.anio()).containsExactly(2028);
		assertThat(servicio.aniosDisponibles().getFirst().id()).isEqualTo(id2028);
		assertThat(servicio.aniosDisponibles().getFirst().estado()).isEqualTo(EstadoAnioEscolar.PLANIFICADO);
	}

	// ---------- Formularios ----------

	@Test
	void elPeriodoAbiertoSeVeEnSoloLecturaConElResumenDelModal() {
		FormularioPeriodoMatricula f = servicio.ver(periodo2027());

		assertThat(f.titulo()).isEqualTo("Matrícula 2027");
		assertThat(f.estado()).isEqualTo(EstadoVisiblePeriodoMatricula.ABIERTO);
		assertThat(f.editable()).isFalse();
		assertThat(f.eliminable()).isFalse();
		assertThat(f.fechaInicio()).isEqualTo(fecha(2026, 9, 15));
		assertThat(f.horaInicio()).isEqualTo(LocalTime.of(8, 0));
		assertThat(f.fechaCierre()).isEqualTo(fecha(2026, 10, 31));
		assertThat(f.horaCierre()).isEqualTo(LocalTime.of(23, 59));
		assertThat(f.diasDelPeriodo()).isEqualTo(46);
		assertThat(f.vacantesTotales()).isEqualTo(300);
		assertThat(f.cantidadSecciones()).isEqualTo(10);
		assertThat(f.resumenSecciones()).isEqualTo("1.° a 5.°, A y B");
		assertThat(f.matriculasRegistradas()).isEqualTo(1);
		assertThat(f.bloquearPorMorosidad()).isTrue();
		assertThat(f.permitirExtemporanea()).isTrue();
		assertThat(f.secciones().getFirst().etiqueta()).isEqualTo("1.° A");
		assertThat(f.secciones().getFirst().gradoNombre()).isEqualTo("1.° de secundaria");
		assertThat(f.secciones().getFirst().capacidad()).isEqualTo(30);
		assertThat(f.documentos()).extracting(DocumentoVista::nombre).containsExactly("Ficha de matrícula firmada",
				"DNI del estudiante", "DNI del apoderado", "Declaración jurada del apoderado");
		assertThat(f.documentos()).noneMatch(DocumentoVista::tienePlantilla);
	}

	@Test
	void elMontoDelFormularioSaleDelTarifarioConfirmado() {
		FormularioPeriodoMatricula sin = servicio.ver(periodo2027()); // el tarifario 2027 está EN_CONFIGURACION
		assertThat(sin.tarifarioConfirmado()).isFalse();
		assertThat(sin.montoMatricula()).isNull();

		confirmarTarifario(ANIO_2027);

		FormularioPeriodoMatricula con = servicio.ver(periodo2027());
		assertThat(con.tarifarioConfirmado()).isTrue();
		assertThat(con.montoMatricula()).isEqualByComparingTo("450");
	}

	@Test
	void elFormularioNuevoTraeLasSeccionesDelAnioConSuCapacidad() {
		UUID id2028 = anioConSecciones(2028);

		FormularioPeriodoMatricula f = servicio.formularioNuevo(id2028);

		assertThat(f.id()).isNull();
		assertThat(f.estado()).isNull();
		assertThat(f.editable()).isTrue();
		assertThat(f.fechaInicio()).isNull();
		assertThat(f.diasDelPeriodo()).isNull();
		assertThat(f.anio()).isEqualTo(2028);
		assertThat(f.secciones()).extracting(SeccionVacante::etiqueta).containsExactly("1.° A", "2.° A", "3.° A",
				"4.° A", "5.° A");
		assertThat(f.vacantesTotales()).isEqualTo(150);
		assertThat(f.tarifarioConfirmado()).isFalse();
		assertThat(f.documentos()).isEmpty();
	}

	@Test
	void elFormularioNuevoRechazaUnAnioQueYaTienePeriodoOEstaCerrado() {
		rechaza(HttpStatus.CONFLICT, "Ya existe un periodo de matrícula para el año 2027.",
				() -> servicio.formularioNuevo(ANIO_2027));
		UUID id2025 = anioSinSecciones(2025, "CERRADO");
		rechaza(HttpStatus.CONFLICT, "Solo se puede habilitar la matrícula de un año planificado o activo.",
				() -> servicio.formularioNuevo(id2025));
		rechaza(HttpStatus.NOT_FOUND, "El año escolar no existe.", () -> servicio.formularioNuevo(UUID.randomUUID()));
		rechaza(HttpStatus.NOT_FOUND, "El periodo de matrícula no existe.", () -> servicio.ver(UUID.randomUUID()));
	}

	// ---------- Crear ----------

	@Test
	void creaElPeriodoDeUnAnioNuevoConVacantesYDocumentosYCierraLosVencidos() {
		reloj.fijarEn(fecha(2026, 11, 15)); // el 2027 ya cerró (31/10), aunque sigue guardado como HABILITADO
		assertThat(estadoGuardado(ANIO_2027)).isEqualTo("HABILITADO");
		UUID id2028 = anioConSecciones(2028);
		confirmarTarifario(id2028);
		List<SeccionVacante> secciones = seccionesDeAnioNuevo(id2028);

		FormularioPeriodoMatricula f = servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15),
				List.of(new VacanteSeccion(secciones.get(0).seccionId(), 25),
						new VacanteSeccion(secciones.get(1).seccionId(), 40)),
				List.of(doc("Ficha de matrícula"), new DocumentoSolicitud(null, "DNI del estudiante", "Ambas caras"))));

		assertThat(f.id()).isNotNull();
		assertThat(f.estado()).isEqualTo(EstadoVisiblePeriodoMatricula.PROGRAMADO);
		assertThat(f.editable()).isTrue();
		assertThat(f.eliminable()).isTrue();
		assertThat(f.fechaInicio()).isEqualTo(fecha(2026, 12, 1));
		assertThat(f.horaCierre()).isEqualTo(HORA);
		assertThat(f.diasDelPeriodo()).isEqualTo(76);
		assertThat(f.montoMatricula()).isEqualByComparingTo("450");
		assertThat(f.bloquearPorMorosidad()).isTrue();
		assertThat(f.permitirExtemporanea()).isTrue();
		assertThat(f.vacantesTotales()).isEqualTo(25 + 40 + 30 + 30 + 30);
		assertThat(f.secciones()).extracting(SeccionVacante::capacidad).containsExactly(25, 40, 30, 30, 30);
		assertThat(f.documentos()).extracting(DocumentoVista::nombre).containsExactly("Ficha de matrícula",
				"DNI del estudiante");
		assertThat(f.documentos()).allSatisfy(d -> assertThat(d.id()).isNotNull());
		assertThat(capacidad(secciones.get(0).seccionId())).isEqualTo(25);
		assertThat(estadoGuardado(id2028)).isEqualTo("HABILITADO");
		assertThat(estadoGuardado(ANIO_2027)).isEqualTo("CERRADO"); // al guardar se cierran los HABILITADO vencidos
		assertThat(servicio.vigente().orElseThrow().anio()).isEqualTo(2028);
	}

	@Test
	void sinTarifarioConfirmadoNoSeHabilitaLaMatricula() {
		reloj.fijarEn(fecha(2026, 11, 15));
		UUID id2028 = anioConSecciones(2028);

		rechaza(HttpStatus.CONFLICT, "Confirma el tarifario 2028 antes de habilitar la matrícula.",
				() -> servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15), List.of(), List.of())));
		assertThat(periodosDelAnio(id2028)).isZero();
	}

	@Test
	void unAnioQueYaTienePeriodoNoSeHabilitaOtraVez() {
		reloj.fijarEn(fecha(2026, 9, 1));
		rechaza(HttpStatus.CONFLICT, "Ya existe un periodo de matrícula para el año 2027.", () -> servicio
				.crear(solicitud(ANIO_2027, fecha(2026, 12, 1), fecha(2027, 2, 15), List.of(), List.of())));
	}

	@Test
	void sinSeccionesActivasNoSeHabilitaLaMatricula() {
		reloj.fijarEn(fecha(2026, 11, 15));
		UUID id2029 = anioSinSecciones(2029, "PLANIFICADO");
		confirmarTarifario(id2029);

		rechaza(HttpStatus.BAD_REQUEST, "Crea las secciones de 2029 antes de habilitar la matrícula.",
				() -> servicio.crear(solicitud(id2029, fecha(2026, 12, 1), fecha(2027, 2, 15), List.of(), List.of())));
		assertThat(periodosDelAnio(id2029)).isZero();
	}

	@Test
	void noSeCreaUnPeriodoMientrasOtroSigueSinCerrar() {
		UUID id2028 = anioConSecciones(2028);
		confirmarTarifario(id2028);

		// reloj por defecto: el 2027 está ABIERTO
		rechaza(HttpStatus.CONFLICT, "Ya existe un periodo de matrícula 2027 sin cerrar.",
				() -> servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15), List.of(), List.of())));
		reloj.fijarEn(fecha(2026, 9, 1)); // y también si está PROGRAMADO
		rechaza(HttpStatus.CONFLICT, "Ya existe un periodo de matrícula 2027 sin cerrar.",
				() -> servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15), List.of(), List.of())));
		assertThat(periodosDelAnio(id2028)).isZero();
	}

	@Test
	void soloSeHabilitaLaMatriculaDeUnAnioPlanificadoOActivo() {
		UUID id2025 = anioSinSecciones(2025, "CERRADO");
		rechaza(HttpStatus.CONFLICT, "Solo se puede habilitar la matrícula de un año planificado o activo.",
				() -> servicio.crear(solicitud(id2025, fecha(2026, 12, 1), fecha(2027, 2, 15), List.of(), List.of())));
		rechaza(HttpStatus.BAD_REQUEST, "Elige el año académico.",
				() -> servicio.crear(solicitud(null, fecha(2026, 12, 1), fecha(2027, 2, 15), List.of(), List.of())));
		rechaza(HttpStatus.NOT_FOUND, "El año escolar no existe.", () -> servicio.crear(
				solicitud(UUID.randomUUID(), fecha(2026, 12, 1), fecha(2027, 2, 15), List.of(), List.of())));
	}

	@Test
	void lasFechasYLosDocumentosInvalidosSeRechazanSinGuardarNada() {
		reloj.fijarEn(fecha(2026, 11, 15));
		UUID id2028 = anioConSecciones(2028);
		confirmarTarifario(id2028);

		rechaza(HttpStatus.BAD_REQUEST, "La fecha de inicio no puede ser anterior a la fecha y hora actuales.",
				() -> servicio.crear(solicitud(id2028, fecha(2026, 11, 1), fecha(2027, 2, 15), List.of(), List.of())));
		rechaza(HttpStatus.BAD_REQUEST, "La fecha de cierre debe ser posterior a la de inicio.",
				() -> servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2026, 12, 1), List.of(), List.of())));
		rechaza(HttpStatus.BAD_REQUEST, "Ingresa la fecha y la hora de inicio.",
				() -> servicio.crear(solicitud(id2028, null, fecha(2027, 2, 15), List.of(), List.of())));
		rechaza(HttpStatus.BAD_REQUEST, "Ingresa el nombre del documento.", () -> servicio.crear(
				solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15), List.of(), List.of(doc("  ")))));
		rechaza(HttpStatus.BAD_REQUEST, "Ya hay un documento llamado \"dni\" en este periodo.", () -> servicio.crear(
				solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15), List.of(), List.of(doc("DNI"), doc("dni")))));
		assertThat(periodosDelAnio(id2028)).isZero();
	}

	// ---------- Vacantes ----------

	@Test
	void unaVacanteInvalidaNoCambiaNadaNiGuardaElPeriodo() {
		reloj.fijarEn(fecha(2026, 11, 15));
		UUID id2028 = anioConSecciones(2028);
		confirmarTarifario(id2028);
		List<SeccionVacante> s = seccionesDeAnioNuevo(id2028);
		matriculasPorSql(id2028, s.get(0).seccionId(), 2);
		UUID deOtroAnio = secciones2027().getFirst().seccionId();

		// la primera vacante es válida y la segunda no: tampoco se aplica la primera
		rechaza(HttpStatus.BAD_REQUEST, "Las vacantes de la sección 2.° A deben estar entre 1 y 45.",
				() -> servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15),
						List.of(new VacanteSeccion(s.get(4).seccionId(), 20),
								new VacanteSeccion(s.get(1).seccionId(), 46)),
						List.of(doc("Ficha")))));
		rechaza(HttpStatus.BAD_REQUEST, "Las vacantes de la sección 2.° A deben estar entre 1 y 45.",
				() -> servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15),
						List.of(new VacanteSeccion(s.get(1).seccionId(), 0)), List.of())));
		rechaza(HttpStatus.BAD_REQUEST,
				"Las vacantes de la sección 1.° A no pueden ser menos que los alumnos que ocupan vacante (2).",
				() -> servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15),
						List.of(new VacanteSeccion(s.get(4).seccionId(), 20),
								new VacanteSeccion(s.get(0).seccionId(), 1)),
						List.of())));
		rechaza(HttpStatus.BAD_REQUEST, "La sección no pertenece a las secciones activas de 2028.",
				() -> servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15),
						List.of(new VacanteSeccion(s.get(4).seccionId(), 20), new VacanteSeccion(deOtroAnio, 20)),
						List.of())));
		rechaza(HttpStatus.BAD_REQUEST, "La sección 5.° A está repetida.",
				() -> servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15),
						List.of(new VacanteSeccion(s.get(4).seccionId(), 20),
								new VacanteSeccion(s.get(4).seccionId(), 21)),
						List.of())));
		rechaza(HttpStatus.BAD_REQUEST, "Ingresa las vacantes de la sección 5.° A.",
				() -> servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15),
						List.of(new VacanteSeccion(s.get(4).seccionId(), null)), List.of())));
		rechaza(HttpStatus.BAD_REQUEST, "Indica la sección de cada vacante.",
				() -> servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15),
						List.of(new VacanteSeccion(null, 20)), List.of())));

		assertThat(periodosDelAnio(id2028)).isZero();
		assertThat(capacidad(s.get(4).seccionId())).isEqualTo(30);
		assertThat(capacidad(s.get(1).seccionId())).isEqualTo(30);
		assertThat(estadoGuardado(ANIO_2027)).isEqualTo("HABILITADO"); // ni siquiera se cerraron los vencidos
		assertThat(servicio.ver(periodo2027()).documentos()).hasSize(4);
	}

	@Test
	void unaVacanteIgualAlTopeOAlNumeroDeOcupantesEsValida() {
		reloj.fijarEn(fecha(2026, 11, 15));
		UUID id2028 = anioConSecciones(2028);
		confirmarTarifario(id2028);
		List<SeccionVacante> s = seccionesDeAnioNuevo(id2028);
		matriculasPorSql(id2028, s.get(0).seccionId(), 2);

		FormularioPeriodoMatricula f = servicio.crear(solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15),
				List.of(new VacanteSeccion(s.get(0).seccionId(), 2), new VacanteSeccion(s.get(1).seccionId(), 45)),
				List.of()));

		assertThat(f.secciones()).extracting(SeccionVacante::capacidad).containsExactly(2, 45, 30, 30, 30);
		assertThat(f.matriculasRegistradas()).isEqualTo(2);
	}

	// ---------- Editar ----------

	@Test
	void editaUnPeriodoProgramadoSusFechasVacantesYDocumentos() {
		reloj.fijarEn(fecha(2026, 9, 1)); // el 2027 está PROGRAMADO
		confirmarTarifario(ANIO_2027);
		List<SeccionVacante> s = secciones2027();
		List<DocumentoSolicitud> actuales = documentos2027();
		List<DocumentoSolicitud> nuevos = List.of(
				new DocumentoSolicitud(actuales.get(2).id(), "DNI del apoderado", "Ambas caras, nítido"),
				new DocumentoSolicitud(actuales.get(0).id(), "Ficha de matrícula", null),
				doc("Certificado de estudios"));

		FormularioPeriodoMatricula f = servicio.editar(periodo2027(),
				solicitud(null, fecha(2026, 9, 20), fecha(2026, 11, 5),
						List.of(new VacanteSeccion(s.get(0).seccionId(), 35),
								new VacanteSeccion(s.get(9).seccionId(), 28)),
						nuevos));

		assertThat(f.estado()).isEqualTo(EstadoVisiblePeriodoMatricula.PROGRAMADO);
		assertThat(f.fechaInicio()).isEqualTo(fecha(2026, 9, 20));
		assertThat(f.fechaCierre()).isEqualTo(fecha(2026, 11, 5));
		assertThat(f.documentos()).extracting(DocumentoVista::nombre).containsExactly("DNI del apoderado",
				"Ficha de matrícula", "Certificado de estudios");
		assertThat(f.documentos().get(0).id()).isEqualTo(actuales.get(2).id()); // el mismo documento, no uno nuevo
		assertThat(f.documentos().get(0).descripcion()).isEqualTo("Ambas caras, nítido");
		assertThat(f.documentos()).extracting(DocumentoVista::orden).containsExactly(1, 2, 3);
		assertThat(f.vacantesTotales()).isEqualTo(300 + 5 - 2);
		assertThat(capacidad(s.get(0).seccionId())).isEqualTo(35);
		assertThat(capacidad(s.get(9).seccionId())).isEqualTo(28);
		assertThat(capacidad(s.get(5).seccionId())).isEqualTo(30); // las que no vienen conservan su capacidad
		assertThat(jdbc.sql("SELECT count(*) FROM matricula.documento_requerido WHERE periodo_matricula_id = ?")
				.param(periodo2027()).query(Integer.class).single()).isEqualTo(3);
	}

	@Test
	void editarPuedeRetirarUnDocumentoYAgregarOtroConElMismoNombre() {
		reloj.fijarEn(fecha(2026, 9, 1));
		confirmarTarifario(ANIO_2027);
		List<DocumentoSolicitud> actuales = documentos2027();

		FormularioPeriodoMatricula f = servicio.editar(periodo2027(), solicitud(null, fecha(2026, 9, 20),
				fecha(2026, 10, 31), List.of(), List.of(actuales.get(0), doc("DNI del estudiante"))));

		assertThat(f.documentos()).extracting(DocumentoVista::nombre).containsExactly("Ficha de matrícula firmada",
				"DNI del estudiante");
		assertThat(f.documentos().get(1).id()).isNotEqualTo(actuales.get(1).id());
	}

	@Test
	void unPeriodoAbiertoNoSeEditaYUnoCerradoTampoco() {
		confirmarTarifario(ANIO_2027);
		var solicitud = solicitud(null, fecha(2026, 12, 1), fecha(2027, 2, 15), List.of(), List.of());

		rechaza(HttpStatus.CONFLICT, "Una vez habilitado, el periodo no se puede editar; solo deshabilitar.",
				() -> servicio.editar(periodo2027(), solicitud));
		rechaza(HttpStatus.CONFLICT, "El periodo de matrícula ya cerró: no se puede editar.",
				() -> servicio.editar(periodoDe(ANIO_2026), solicitud));
		rechaza(HttpStatus.NOT_FOUND, "El periodo de matrícula no existe.",
				() -> servicio.editar(UUID.randomUUID(), solicitud));
	}

	@Test
	void editarConDatosInvalidosNoCambiaNada() {
		reloj.fijarEn(fecha(2026, 9, 1));
		confirmarTarifario(ANIO_2027);
		List<SeccionVacante> s = secciones2027();
		List<DocumentoSolicitud> actuales = documentos2027();

		rechaza(HttpStatus.BAD_REQUEST, "Las vacantes de la sección 1.° A deben estar entre 1 y 45.",
				() -> servicio.editar(periodo2027(),
						solicitud(null, fecha(2026, 9, 20), fecha(2026, 11, 5),
								List.of(new VacanteSeccion(s.get(1).seccionId(), 33),
										new VacanteSeccion(s.get(0).seccionId(), 99)),
								List.of(actuales.get(0)))));
		rechaza(HttpStatus.BAD_REQUEST, "El documento no pertenece a este periodo.",
				() -> servicio.editar(periodo2027(),
						solicitud(null, fecha(2026, 9, 20), fecha(2026, 11, 5), List.of(),
								List.of(new DocumentoSolicitud(UUID.randomUUID(), "Ajeno", null)))));
		rechaza(HttpStatus.BAD_REQUEST, "La fecha de cierre debe ser posterior a la de inicio.",
				() -> servicio.editar(periodo2027(),
						solicitud(null, fecha(2026, 9, 20), fecha(2026, 9, 20), List.of(), actuales)));

		FormularioPeriodoMatricula f = servicio.ver(periodo2027());
		assertThat(f.fechaInicio()).isEqualTo(fecha(2026, 9, 15));
		assertThat(f.documentos()).hasSize(4);
		assertThat(capacidad(s.get(1).seccionId())).isEqualTo(30);
	}

	@Test
	void noSeQuitaUnDocumentoQueYaTieneEntregas() {
		reloj.fijarEn(fecha(2026, 9, 1));
		confirmarTarifario(ANIO_2027);
		List<DocumentoSolicitud> actuales = documentos2027();
		jdbc.sql("""
				INSERT INTO matricula.documento_matricula (matricula_id, documento_requerido_id, nombre_archivo,
				                                           tipo_mime, tamano_bytes, contenido)
				VALUES (?, ?, 'ficha.pdf', 'application/pdf', 3, '\\x010203'::bytea)""")
				.params(MATRICULA_2027, actuales.get(0).id()).update();
		entityManager.clear();

		rechaza(HttpStatus.CONFLICT,
				"No se puede quitar el documento \"Ficha de matrícula firmada\": ya tiene entregas.",
				() -> servicio.editar(periodo2027(), solicitud(null, fecha(2026, 9, 20), fecha(2026, 10, 31),
						List.of(), actuales.subList(1, 4))));

		assertThat(servicio.ver(periodo2027()).documentos()).hasSize(4);
		assertThat(servicio.ver(periodo2027()).documentos().getFirst().tieneEntregas()).isTrue();
		// conservándolo sí se puede editar
		servicio.editar(periodo2027(), solicitud(null, fecha(2026, 9, 20), fecha(2026, 10, 31), List.of(), actuales));
	}

	// ---------- Eliminar ----------

	@Test
	void eliminaUnPeriodoProgramadoSinMatriculas() {
		reloj.fijarEn(fecha(2026, 11, 15));
		UUID id2028 = anioConSecciones(2028);
		confirmarTarifario(id2028);
		UUID periodo = servicio.crear(
				solicitud(id2028, fecha(2026, 12, 1), fecha(2027, 2, 15), List.of(), List.of(doc("Ficha")))).id();

		servicio.eliminar(periodo);

		assertThat(periodosDelAnio(id2028)).isZero();
		assertThat(jdbc.sql("SELECT count(*) FROM matricula.documento_requerido WHERE periodo_matricula_id = ?")
				.param(periodo).query(Integer.class).single()).isZero();
		assertThat(servicio.aniosDisponibles()).extracting(a -> a.anio()).containsExactly(2028);
	}

	@Test
	void noSeEliminaUnPeriodoProgramadoSiElAnioYaTieneMatriculas() {
		reloj.fijarEn(fecha(2026, 9, 1)); // el 2027 está PROGRAMADO y tiene una matrícula reservada

		assertThat(servicio.ver(periodo2027()).eliminable()).isFalse();
		rechaza(HttpStatus.CONFLICT, "No se puede eliminar el periodo de matrícula: el año 2027 ya tiene matrículas.",
				() -> servicio.eliminar(periodo2027()));
		assertThat(periodosDelAnio(ANIO_2027)).isEqualTo(1);
	}

	@Test
	void unaMatriculaAnuladaTambienImpideEliminar() {
		reloj.fijarEn(fecha(2026, 9, 1));
		jdbc.sql("UPDATE matricula.matricula SET estado = 'ANULADA' WHERE id = ?").param(MATRICULA_2027).update();
		entityManager.clear();

		rechaza(HttpStatus.CONFLICT, "No se puede eliminar el periodo de matrícula: el año 2027 ya tiene matrículas.",
				() -> servicio.eliminar(periodo2027()));
	}

	@Test
	void unPeriodoAbiertoOCerradoNoSeElimina() {
		rechaza(HttpStatus.CONFLICT, "Solo se puede eliminar un periodo de matrícula programado.",
				() -> servicio.eliminar(periodo2027()));
		rechaza(HttpStatus.CONFLICT, "Solo se puede eliminar un periodo de matrícula programado.",
				() -> servicio.eliminar(periodoDe(ANIO_2026)));
		rechaza(HttpStatus.NOT_FOUND, "El periodo de matrícula no existe.", () -> servicio.eliminar(UUID.randomUUID()));
	}

	// ---------- Plantillas ----------

	private static final byte[] CSV = "nombre,dni\nAna,1\n".getBytes();

	@Test
	void subeReemplazaYQuitaLaPlantillaDeUnDocumentoDeUnPeriodoProgramado() {
		reloj.fijarEn(fecha(2026, 9, 1));
		UUID documento = documentos2027().getFirst().id();

		DocumentoVista subida = servicio.subirPlantilla(periodo2027(), documento, "C:\\docs\\ficha.CSV", CSV);
		assertThat(subida.tienePlantilla()).isTrue();
		assertThat(subida.plantillaNombreArchivo()).isEqualTo("ficha.CSV");
		var descargada = servicio.descargarPlantilla(periodo2027(), documento);
		assertThat(descargada.nombreArchivo()).isEqualTo("ficha.CSV");
		assertThat(descargada.tipoMime()).isEqualTo("text/csv");
		assertThat(descargada.contenido()).isEqualTo(CSV);
		assertThat(servicio.ver(periodo2027()).documentos().getFirst().tienePlantilla()).isTrue();

		servicio.subirPlantilla(periodo2027(), documento, "ficha.xls", new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11,
				(byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0});
		assertThat(servicio.descargarPlantilla(periodo2027(), documento).tipoMime())
				.isEqualTo("application/vnd.ms-excel");

		assertThat(servicio.quitarPlantilla(periodo2027(), documento).tienePlantilla()).isFalse();
		rechaza(HttpStatus.NOT_FOUND, "El documento no tiene plantilla.",
				() -> servicio.descargarPlantilla(periodo2027(), documento));
		rechaza(HttpStatus.NOT_FOUND, "El documento no tiene plantilla.",
				() -> servicio.quitarPlantilla(periodo2027(), documento));
	}

	@Test
	void laPlantillaSoloAdmiteXlsxXlsOCsvDeHasta10Mb() {
		reloj.fijarEn(fecha(2026, 9, 1));
		UUID documento = documentos2027().getFirst().id();

		rechaza(HttpStatus.BAD_REQUEST, "La plantilla debe ser un archivo XLSX, XLS o CSV.",
				() -> servicio.subirPlantilla(periodo2027(), documento, "ficha.pdf", CSV));
		rechaza(HttpStatus.BAD_REQUEST, "El contenido del archivo no corresponde a un XLSX válido.",
				() -> servicio.subirPlantilla(periodo2027(), documento, "ficha.xlsx", CSV));
		rechaza(HttpStatus.BAD_REQUEST, "Selecciona un archivo para la plantilla.",
				() -> servicio.subirPlantilla(periodo2027(), documento, "ficha.csv", new byte[0]));
		byte[] grande = new byte[10 * 1024 * 1024 + 1];
		java.util.Arrays.fill(grande, (byte) 'a');
		rechaza(HttpStatus.BAD_REQUEST, "La plantilla no puede pesar más de 10 MB.",
				() -> servicio.subirPlantilla(periodo2027(), documento, "grande.csv", grande));
		assertThat(servicio.ver(periodo2027()).documentos().getFirst().tienePlantilla()).isFalse();
	}

	@Test
	void laPlantillaSoloSeCambiaConElPeriodoProgramadoPeroSeDescargaSiempre() {
		reloj.fijarEn(fecha(2026, 9, 1));
		UUID documento = documentos2027().getFirst().id();
		servicio.subirPlantilla(periodo2027(), documento, "ficha.csv", CSV);

		reloj.fijarEn(fecha(2026, 10, 1)); // ahora está ABIERTO: solo lectura
		rechaza(HttpStatus.CONFLICT, "Solo se puede cambiar la plantilla de un periodo programado.",
				() -> servicio.subirPlantilla(periodo2027(), documento, "otra.csv", CSV));
		rechaza(HttpStatus.CONFLICT, "Solo se puede cambiar la plantilla de un periodo programado.",
				() -> servicio.quitarPlantilla(periodo2027(), documento));
		assertThat(servicio.descargarPlantilla(periodo2027(), documento).contenido()).isEqualTo(CSV);

		reloj.fijarEn(fecha(2026, 11, 15)); // y CERRADO
		assertThat(servicio.descargarPlantilla(periodo2027(), documento).nombreArchivo()).isEqualTo("ficha.csv");
	}

	@Test
	void laPlantillaDeUnDocumentoAjenoOInexistenteDa404() {
		reloj.fijarEn(fecha(2026, 9, 1));
		UUID documento2026 = servicio.ver(periodoDe(ANIO_2026)).documentos().getFirst().id();

		rechaza(HttpStatus.NOT_FOUND, "El documento no existe.",
				() -> servicio.subirPlantilla(periodo2027(), documento2026, "ficha.csv", CSV));
		rechaza(HttpStatus.NOT_FOUND, "El documento no existe.",
				() -> servicio.descargarPlantilla(periodo2027(), UUID.randomUUID()));
		rechaza(HttpStatus.NOT_FOUND, "El periodo de matrícula no existe.",
				() -> servicio.subirPlantilla(UUID.randomUUID(), documento2026, "ficha.csv", CSV));
	}

	@Test
	void editarElPeriodoConservaLaPlantillaDeLosDocumentosQueSeMantienen() {
		reloj.fijarEn(fecha(2026, 9, 1));
		confirmarTarifario(ANIO_2027);
		List<DocumentoSolicitud> actuales = documentos2027();
		servicio.subirPlantilla(periodo2027(), actuales.get(0).id(), "ficha.csv", CSV);

		FormularioPeriodoMatricula f = servicio.editar(periodo2027(),
				solicitud(null, fecha(2026, 9, 20), fecha(2026, 10, 31), List.of(), actuales));

		assertThat(f.documentos().getFirst().tienePlantilla()).isTrue();
		assertThat(servicio.descargarPlantilla(periodo2027(), actuales.get(0).id()).contenido()).isEqualTo(CSV);
	}
}
