package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.AnioEscolarDetalle;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.PeriodoDetalle;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.Requisito;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.RequisitosAnio;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudEdicionAnio;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudPeriodo;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudRegimen;
import com.galiledu.configuracion.aplicacion.ServicioAnioEscolar;
import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.EstadoPeriodo;
import com.galiledu.configuracion.dominio.Regimen;

/**
 * HU-74: reglas del servicio, incluidas las que aún no tienen endpoint (activar y cerrar). El reloj está fijo en
 * 15/09/2026: el III Bimestre de 2026 está en curso.
 */
class ServicioAnioEscolarIT extends PruebaIntegracionConfiguracion {
	private static final UUID ID_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");
	private static final UUID ID_2027 = UUID.fromString("30000000-0000-0000-0000-000000002027");

	@Autowired
	private ServicioAnioEscolar servicio;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private RelojPrueba reloj;

	private static void rechaza(HttpStatus estado, String mensaje, ThrowingCallable operacion) {
		assertThatThrownBy(operacion).isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
			assertThat(e.estado()).isEqualTo(estado);
			assertThat(e.getMessage()).isEqualTo(mensaje);
		});
	}

	private static LocalDate fecha(int anio, int mes, int dia) {
		return LocalDate.of(anio, mes, dia);
	}

	private UUID periodo(UUID anioId, int numero) {
		return servicio.detalle(anioId).periodos().get(numero - 1).id();
	}

	private SolicitudPeriodo solicitud(PeriodoDetalle p, LocalDate limite) {
		return new SolicitudPeriodo(p.nombre(), p.fechaInicio(), p.fechaFin(), limite);
	}

	/** Cierra el 2026 por SQL para poder probar la activación del 2027; limpia el contexto de JPA para que lo relea. */
	private void cerrarElAnio2026PorSql() {
		jdbc.sql("UPDATE configuracion.anio_escolar SET estado = 'CERRADO' WHERE id = :id").param("id", ID_2026)
				.update();
		entityManager.clear();
	}

	private void registrarEstructura2027PorSql() {
		jdbc.sql("""
				INSERT INTO configuracion.estructura_horaria (anio_escolar_id, codigo, dias_lectivos, hora_entrada,
				                                              bloques_por_dia, duracion_bloque_min, nivel)
				VALUES (:id, 'EH-SEC-27', ARRAY['LUN'], '07:30', 8, 45, 'SECUNDARIA')""").param("id", ID_2027).update();
	}

	@Test
	void elDetalleTraeLasCantidadesYLosPeriodosConEstadoDuracionYRegistroDeNotas() {
		AnioEscolarDetalle detalle = servicio.detalle(ID_2026);

		assertThat(detalle.cantidadSecciones()).isEqualTo(10);
		assertThat(detalle.cantidadEstructuras()).isEqualTo(2);
		assertThat(detalle.periodos()).extracting(PeriodoDetalle::estado).containsExactly(EstadoPeriodo.CERRADO,
				EstadoPeriodo.CERRADO, EstadoPeriodo.EN_CURSO, EstadoPeriodo.PLANIFICADO);
		assertThat(detalle.periodos()).extracting(PeriodoDetalle::duracionSemanas).containsExactly(10, 10, 10, 9);
		assertThat(detalle.periodos()).extracting(PeriodoDetalle::registroNotasAbierto)
				.containsExactly(false, false, true, false);
		assertThat(servicio.detalle(ID_2027).periodos()).extracting(PeriodoDetalle::duracionSemanas)
				.containsExactly(13, 13, 13);
	}

	@Test
	void elAnioActualTraeElPeriodoEnCursoLosConteosYElAvance() {
		var actual = servicio.actual();

		assertThat(actual.anio()).isEqualTo(2026);
		assertThat(actual.periodoEnCurso().nombre()).isEqualTo("III Bimestre");
		assertThat(actual.periodoEnCurso().fechaLimiteNotas()).isEqualTo(fecha(2026, 10, 16));
		assertThat(actual.periodoEnCurso().diasParaLimiteNotas()).isEqualTo(31);
		assertThat(actual.seccionesActivas()).isEqualTo(10);
		assertThat(actual.alumnosMatriculados()).isEqualTo(2);
		assertThat(actual.estructurasHorarias()).isEqualTo(2);
		assertThat(actual.codigosEstructuras()).containsExactly("EH-PRI-26", "EH-SEC-26");
		assertThat(actual.avance()).isEqualTo(67); // 198 de 292 días
		assertThat(actual.periodoNotasAbiertas()).isNull(); // el periodo en curso se muestra aparte
		assertThat(actual.proximoPeriodo().nombre()).isEqualTo("IV Bimestre");
		assertThat(actual.proximoPeriodo().fechaInicio()).isEqualTo(fecha(2026, 10, 19));
	}

	@Test
	void ampliarElPlazoDeNotasDeUnPeriodoTerminadoLoMuestraAparteDelPeriodoEnCurso() {
		reloj.fijarEn(fecha(2026, 10, 21)); // el III terminó el 09/10; el IV está en curso desde el 19/10
		PeriodoDetalle tercero = servicio.detalle(ID_2026).periodos().get(2);
		assertThat(servicio.actual().periodoNotasAbiertas()).isNull(); // su límite venció el 16/10

		servicio.editarPeriodo(tercero.id(), solicitud(tercero, fecha(2026, 10, 27)));
		var actual = servicio.actual();

		assertThat(actual.periodoEnCurso().nombre()).isEqualTo("IV Bimestre");
		assertThat(actual.periodoNotasAbiertas().nombre()).isEqualTo("III Bimestre");
		assertThat(actual.periodoNotasAbiertas().fechaLimiteNotas()).isEqualTo(fecha(2026, 10, 27));
		assertThat(actual.periodoNotasAbiertas().diasParaLimiteNotas()).isEqualTo(6);
		assertThat(actual.proximoPeriodo()).isNull();
	}

	@Test
	void entreDosPeriodosLaCardMuestraElPlazoDeNotasAbiertoYElProximoPeriodo() {
		reloj.fijarEn(fecha(2026, 10, 12)); // el III terminó el 09/10; sus notas siguen abiertas hasta el 16/10

		var actual = servicio.actual();

		assertThat(actual.periodoEnCurso()).isNull();
		assertThat(actual.periodoNotasAbiertas().nombre()).isEqualTo("III Bimestre");
		assertThat(actual.periodoNotasAbiertas().fechaFin()).isEqualTo(fecha(2026, 10, 9));
		assertThat(actual.periodoNotasAbiertas().fechaLimiteNotas()).isEqualTo(fecha(2026, 10, 16));
		assertThat(actual.periodoNotasAbiertas().diasParaLimiteNotas()).isEqualTo(4);
		assertThat(actual.proximoPeriodo().nombre()).isEqualTo("IV Bimestre");
		assertThat(actual.proximoPeriodo().fechaInicio()).isEqualTo(fecha(2026, 10, 19));
	}

	@Test
	void despuesDelUltimoPeriodoSoloQuedaElPlazoDeNotasYLuegoNada() {
		reloj.fijarEn(fecha(2026, 12, 20)); // el IV terminó el 18/12; sus notas siguen abiertas hasta el 23/12

		var actual = servicio.actual();

		assertThat(actual.periodoEnCurso()).isNull();
		assertThat(actual.periodoNotasAbiertas().nombre()).isEqualTo("IV Bimestre");
		assertThat(actual.periodoNotasAbiertas().diasParaLimiteNotas()).isEqualTo(3);
		assertThat(actual.proximoPeriodo()).isNull();

		reloj.fijarEn(fecha(2026, 12, 24));
		var despues = servicio.actual();
		assertThat(despues.periodoNotasAbiertas()).isNull();
		assertThat(despues.proximoPeriodo()).isNull();
		assertThat(despues.avance()).isEqualTo(100);
	}

	@Test
	void sinAnioActivoNoHayAnioActual() {
		cerrarElAnio2026PorSql();

		rechaza(HttpStatus.NOT_FOUND, "No hay un año escolar activo.", servicio::actual);
	}

	@Test
	void editarLasFechasDeUnAnioPlanificadoRegeneraSusPeriodos() {
		var editado = servicio.editar(ID_2027, new SolicitudEdicionAnio(fecha(2027, 3, 8), fecha(2027, 12, 10)));

		assertThat(editado.fechaInicio()).isEqualTo(fecha(2027, 3, 8));
		assertThat(editado.periodos()).hasSize(3);
		assertThat(editado.periodos().getFirst().fechaInicio()).isEqualTo(fecha(2027, 3, 8));
		assertThat(editado.periodos().getLast().fechaFin()).isEqualTo(fecha(2027, 12, 10));
		assertThat(editado.periodos()).extracting(PeriodoDetalle::id).doesNotContainNull();
		assertThat(jdbc.sql("SELECT count(*) FROM configuracion.periodo_academico WHERE anio_escolar_id = :id")
				.param("id", ID_2027).query(Integer.class).single()).isEqualTo(3);
	}

	@Test
	void editarLasFechasValidaElRangoYElEstadoDelAnio() {
		rechaza(HttpStatus.BAD_REQUEST, "La fecha de fin debe ser posterior a la fecha de inicio.",
				() -> servicio.editar(ID_2027, new SolicitudEdicionAnio(fecha(2027, 12, 10), fecha(2027, 3, 8))));
		rechaza(HttpStatus.BAD_REQUEST, "El año escolar es muy corto para dividirlo en 3 periodos.",
				() -> servicio.editar(ID_2027, new SolicitudEdicionAnio(fecha(2027, 3, 1), fecha(2027, 3, 3))));
		rechaza(HttpStatus.CONFLICT, "Solo se puede modificar un año planificado.",
				() -> servicio.editar(ID_2026, new SolicitudEdicionAnio(fecha(2026, 3, 2), fecha(2026, 12, 18))));
		rechaza(HttpStatus.NOT_FOUND, "El año escolar no existe.",
				() -> servicio.editar(UUID.randomUUID(),
						new SolicitudEdicionAnio(fecha(2027, 3, 1), fecha(2027, 12, 1))));
	}

	@Test
	void cambiarElRegimenDeUnAnioPlanificadoRegeneraLosPeriodosSinChocarConLaRestriccionDeNumero() {
		var cambiado = servicio.cambiarRegimen(ID_2027, new SolicitudRegimen(Regimen.BIMESTRAL));

		assertThat(cambiado.regimen()).isEqualTo(Regimen.BIMESTRAL);
		assertThat(cambiado.periodos()).extracting(PeriodoDetalle::nombre)
				.containsExactly("I Bimestre", "II Bimestre", "III Bimestre", "IV Bimestre");
		assertThat(cambiado.periodos().getFirst().fechaInicio()).isEqualTo(fecha(2027, 3, 1));
		assertThat(cambiado.periodos().getLast().fechaFin()).isEqualTo(fecha(2027, 12, 17));
		assertThat(jdbc.sql("SELECT count(*) FROM configuracion.periodo_academico WHERE anio_escolar_id = :id")
				.param("id", ID_2027).query(Integer.class).single()).isEqualTo(4);
	}

	@Test
	void cambiarElRegimenSoloSePuedeEnUnAnioPlanificado() {
		rechaza(HttpStatus.CONFLICT, "Solo se puede modificar un año planificado.",
				() -> servicio.cambiarRegimen(ID_2026, new SolicitudRegimen(Regimen.TRIMESTRAL)));
		rechaza(HttpStatus.NOT_FOUND, "El año escolar no existe.",
				() -> servicio.cambiarRegimen(UUID.randomUUID(), new SolicitudRegimen(Regimen.TRIMESTRAL)));
		assertThat(servicio.detalle(ID_2026).periodos()).hasSize(4);
	}

	@Test
	void losRequisitosDelAnioPlanificadoMuestranLoQueFalta() {
		RequisitosAnio r = servicio.requisitos(ID_2027);

		assertThat(r.puedeActivar()).isFalse();
		assertThat(r.obligatorios()).extracting(Requisito::texto, Requisito::cumplido, Requisito::detalle)
				.containsExactly(
						tuple("Periodos académicos definidos", true,
								"3 trimestres del 01/03/2027 al 17/12/2027"),
						tuple("Secciones creadas", true, "10 secciones activas en 5 grados"),
						tuple("Estructura horaria registrada", false,
								"Aún no hay estructuras horarias."));
		assertThat(r.recomendados()).extracting(Requisito::texto, Requisito::cumplido, Requisito::detalle)
				.containsExactly(
						tuple("Tutores asignados (recomendado)", false,
								"10 de 10 secciones aún no tienen tutor"),
						tuple("Plan de evaluación por grado (recomendado)", false,
								"0 de 5 grados con cursos asignados"));
	}

	@Test
	void losRequisitosDelAnioActivoSeCumplenLosObligatorios() {
		RequisitosAnio r = servicio.requisitos(ID_2026);

		assertThat(r.puedeActivar()).isTrue();
		assertThat(r.obligatorios()).extracting(Requisito::cumplido).containsOnly(true);
		assertThat(r.obligatorios().get(2).detalle()).isEqualTo("EH-PRI-26 · EH-SEC-26");
		assertThat(r.recomendados().get(0).detalle()).isEqualTo("9 de 10 secciones aún no tienen tutor");
		assertThat(r.recomendados().get(1).detalle()).isEqualTo("1 de 5 grados con cursos asignados");
	}

	@Test
	void losRequisitosDeUnAnioQueNoExisteDan404() {
		rechaza(HttpStatus.NOT_FOUND, "El año escolar no existe.", () -> servicio.requisitos(UUID.randomUUID()));
	}

	@Test
	void editarUnPeriodoFuturoCambiaSusFechasYDevuelveElPeriodoCalculado() {
		PeriodoDetalle segundo = servicio.detalle(ID_2027).periodos().get(1);

		var editado = servicio.editarPeriodo(segundo.id(),
				new SolicitudPeriodo("II Trimestre", fecha(2027, 6, 9), fecha(2027, 9, 6), fecha(2027, 9, 20)));

		assertThat(editado.id()).isEqualTo(segundo.id());
		assertThat(editado.fechaInicio()).isEqualTo(fecha(2027, 6, 9));
		assertThat(editado.fechaLimiteNotas()).isEqualTo(fecha(2027, 9, 20));
		assertThat(editado.estado()).isEqualTo(EstadoPeriodo.PLANIFICADO);
		assertThat(editado.registroNotasAbierto()).isFalse();
	}

	@Test
	void unPeriodoQueYaEmpezoNoCambiaSuFechaDeInicio() {
		PeriodoDetalle tercero = servicio.detalle(ID_2026).periodos().get(2);

		rechaza(HttpStatus.CONFLICT, "El periodo ya empezó: no se puede cambiar.",
				() -> servicio.editarPeriodo(tercero.id(),
						new SolicitudPeriodo(tercero.nombre(), fecha(2026, 8, 4), tercero.fechaFin(),
								tercero.fechaLimiteNotas())));
	}

	@Test
	void unPeriodoNoPuedeCruzarseConOtro() {
		PeriodoDetalle cuarto = servicio.detalle(ID_2026).periodos().get(3);

		rechaza(HttpStatus.BAD_REQUEST, "Se cruza con el III Bimestre, que empieza el 03/08/2026.",
				() -> servicio.editarPeriodo(cuarto.id(),
						new SolicitudPeriodo(cuarto.nombre(), fecha(2026, 10, 5), cuarto.fechaFin(),
								cuarto.fechaLimiteNotas())));
	}

	@Test
	void ampliarLaFechaLimiteDeUnPeriodoTerminadoReabreElRegistroDeNotas() {
		PeriodoDetalle primero = servicio.detalle(ID_2026).periodos().getFirst();
		assertThat(primero.estado()).isEqualTo(EstadoPeriodo.CERRADO);
		assertThat(primero.registroNotasAbierto()).isFalse();
		UUID periodoId = primero.id();

		var ampliado = servicio.editarPeriodo(periodoId, solicitud(primero, fecha(2026, 10, 31)));

		assertThat(ampliado.estado()).isEqualTo(EstadoPeriodo.CERRADO);
		assertThat(ampliado.fechaLimiteNotas()).isEqualTo(fecha(2026, 10, 31));
		assertThat(ampliado.registroNotasAbierto()).isTrue();
		assertThat(servicio.registroNotasAbierto(periodoId)).isTrue();
	}

	@Test
	void unPeriodoTerminadoNoCambiaNombreNiFechas() {
		PeriodoDetalle primero = servicio.detalle(ID_2026).periodos().getFirst();

		rechaza(HttpStatus.CONFLICT, "El periodo ya terminó: solo se puede cambiar la fecha límite de notas.",
				() -> servicio.editarPeriodo(primero.id(),
						new SolicitudPeriodo("Otro nombre", primero.fechaInicio(), primero.fechaFin(),
								primero.fechaLimiteNotas())));
	}

	@Test
	void editarUnPeriodoQueNoExisteDa404() {
		rechaza(HttpStatus.NOT_FOUND, "El periodo académico no existe.",
				() -> servicio.editarPeriodo(UUID.randomUUID(),
						new SolicitudPeriodo("I Bimestre", fecha(2026, 3, 2), fecha(2026, 5, 8), fecha(2026, 5, 15))));
		rechaza(HttpStatus.NOT_FOUND, "El periodo académico no existe.",
				() -> servicio.registroNotasAbierto(UUID.randomUUID()));
	}

	@Test
	void elPeriodoEnCursoSoloExisteEnElAnioActivo() {
		assertThat(servicio.periodoEnCurso(ID_2026)).get().extracting(PeriodoDetalle::nombre)
				.isEqualTo("III Bimestre");
		assertThat(servicio.periodoEnCurso(ID_2027)).isEmpty();
		rechaza(HttpStatus.NOT_FOUND, "El año escolar no existe.", () -> servicio.periodoEnCurso(UUID.randomUUID()));
	}

	@Test
	void elRegistroDeNotasEstaAbiertoSoloEnElPeriodoEnCursoDelAnioActivo() {
		var periodos = servicio.detalle(ID_2026).periodos();

		assertThat(servicio.registroNotasAbierto(periodos.get(0).id())).isFalse(); // límite 15/05
		assertThat(servicio.registroNotasAbierto(periodos.get(2).id())).isTrue(); // 03/08 al 16/10
		assertThat(servicio.registroNotasAbierto(periodos.get(3).id())).isFalse(); // aún no empieza
		assertThat(servicio.registroNotasAbierto(periodo(ID_2027, 1))).isFalse(); // año planificado
	}

	@Test
	void noSeActivaUnAnioMientrasOtroSigueActivo() {
		rechaza(HttpStatus.CONFLICT, "El año 2026 sigue activo. Ciérralo antes de activar este.",
				() -> servicio.activar(ID_2027));
	}

	@Test
	void noSeActivaUnAnioSinSusRequisitosObligatorios() {
		cerrarElAnio2026PorSql();

		rechaza(HttpStatus.CONFLICT, "Faltan requisitos obligatorios: Estructura horaria registrada.",
				() -> servicio.activar(ID_2027));
	}

	@Test
	void seActivaUnAnioPlanificadoConSusRequisitos() {
		cerrarElAnio2026PorSql();
		registrarEstructura2027PorSql();

		AnioEscolarDetalle activado = servicio.activar(ID_2027);

		assertThat(activado.estado()).isEqualTo(EstadoAnioEscolar.ACTIVO);
		assertThat(activado.periodos()).hasSize(3);
		assertThat(servicio.actual().anio()).isEqualTo(2027);
	}

	@Test
	void soloSePuedeActivarUnAnioPlanificado() {
		rechaza(HttpStatus.CONFLICT, "Solo se puede activar un año planificado.", () -> servicio.activar(ID_2026));
		rechaza(HttpStatus.NOT_FOUND, "El año escolar no existe.", () -> servicio.activar(UUID.randomUUID()));
	}

	@Test
	void noSeCierraElAnioAntesDeQueVenzaElPlazoDeNotasDelUltimoPeriodo() {
		rechaza(HttpStatus.CONFLICT, "Los docentes pueden registrar notas hasta el 23/12/2026.",
				() -> servicio.cerrar(ID_2026));
	}

	@Test
	void soloSePuedeCerrarUnAnioActivo() {
		rechaza(HttpStatus.CONFLICT, "Solo se puede cerrar un año activo.", () -> servicio.cerrar(ID_2027));
		rechaza(HttpStatus.NOT_FOUND, "El año escolar no existe.", () -> servicio.cerrar(UUID.randomUUID()));
	}
}
