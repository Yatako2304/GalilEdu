package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.CuotaVista;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudEdicionAnio;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudTarifario;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.TarifarioVista;
import com.galiledu.configuracion.aplicacion.ServicioAnioEscolar;
import com.galiledu.configuracion.aplicacion.ServicioTarifario;
import com.galiledu.configuracion.dominio.EstadoTarifario;

/**
 * HU-34: reglas del servicio del tarifario con los datos de prueba: 2026 ACTIVO con tarifario CONFIRMADO y el
 * compromiso
 * de Sofía, y 2027 PLANIFICADO con tarifario EN_CONFIGURACION y sin compromisos.
 */
class ServicioTarifarioIT extends PruebaIntegracion {
	private static final UUID ID_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");
	private static final UUID ID_2027 = UUID.fromString("30000000-0000-0000-0000-000000002027");
	private static final UUID TARIFARIO_2026 = UUID.fromString("32000000-0000-0000-0000-000000002026");
	private static final UUID TARIFARIO_2027 = UUID.fromString("32000000-0000-0000-0000-000000002027");
	private static final UUID SOFIA = UUID.fromString("20000000-0000-0000-0000-000000000002");

	@Autowired
	private ServicioTarifario servicio;
	@Autowired
	private ServicioAnioEscolar anios;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager entityManager;

	private static BigDecimal d(String valor) {
		return new BigDecimal(valor);
	}

	private static LocalDate fecha(int anio, int mes, int dia) {
		return LocalDate.of(anio, mes, dia);
	}

	private static SolicitudTarifario solicitud(String ingreso, String matricula, String pension, Integer gracia,
			String recargo, String tope) {
		return new SolicitudTarifario(ingreso == null ? null : d(ingreso), d(matricula), d(pension), gracia,
				recargo == null ? null : d(recargo), tope == null ? null : d(tope));
	}

	private static SolicitudTarifario solicitudValida() {
		return solicitud("400", "460", "650.50", 7, "3", "12");
	}

	private static void rechaza(HttpStatus estado, String mensaje, ThrowingCallable operacion) {
		assertThatThrownBy(operacion).isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
			assertThat(e.estado()).isEqualTo(estado);
			assertThat(e.getMessage()).isEqualTo(mensaje);
		});
	}

	/** Un año sin tarifario, del 01/03 al 15/12 (trimestral). */
	private UUID anioPorSql(int anio, String estado) {
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

	/** Un compromiso de pago de Sofía en 2027, con el tarifario 2027 (que sigue EN_CONFIGURACION). */
	private void compromisoDelTarifario2027PorSql() {
		jdbc.sql("""
				INSERT INTO pagos.compromiso_pago (estudiante_id, anio_escolar_id, tarifario_id)
				VALUES (:estudiante, :anio, :tarifario)""")
				.param("estudiante", SOFIA).param("anio", ID_2027).param("tarifario", TARIFARIO_2027).update();
	}

	private int cuotasGuardadas(UUID tarifarioId) {
		return jdbc.sql("SELECT count(*) FROM pagos.cuota_tarifario WHERE tarifario_id = ?").param(tarifarioId)
				.query(Integer.class).single();
	}

	// --- GET ---

	@Test
	void sinTarifarioProponeLosValoresDelAnioAnteriorYElCalendarioDelAnio() {
		UUID id2028 = anioPorSql(2028, "PLANIFICADO"); // el año anterior es 2027, con tarifario

		TarifarioVista vista = servicio.ver(id2028);

		assertThat(vista.existe()).isFalse();
		assertThat(vista.estado()).isEqualTo(EstadoTarifario.EN_CONFIGURACION);
		assertThat(vista.soloLectura()).isFalse();
		assertThat(vista.motivoSoloLectura()).isNull();
		assertThat(vista.calendarioDesactualizado()).isFalse();
		assertThat(vista.conceptos().montoCuotaIngreso()).isEqualByComparingTo("500");
		assertThat(vista.conceptos().montoMatricula()).isEqualByComparingTo("450");
		assertThat(vista.conceptos().montoPension()).isEqualByComparingTo("620");
		assertThat(vista.conceptos().vencimientoMatricula()).isEqualTo(fecha(2028, 2, 29));
		assertThat(vista.conceptos().cantidadCuotas()).isEqualTo(10);
		assertThat(vista.morosidad().diasGracia()).isEqualTo(5);
		assertThat(vista.morosidad().recargoPorcentaje()).isEqualByComparingTo("2");
		assertThat(vista.morosidad().topeRecargoPorcentaje()).isEqualByComparingTo("10");
		assertThat(vista.calendario().cuotas()).hasSize(10);
		assertThat(vista.calendario().cuotas().getLast().fechaVencimiento()).isEqualTo(fecha(2028, 12, 15));
		assertThat(vista.calendario().totalPensiones()).isEqualByComparingTo("6200");
		assertThat(jdbc.sql("SELECT count(*) FROM pagos.tarifario WHERE anio_escolar_id = ?").param(id2028)
				.query(Integer.class).single()).isZero(); // la propuesta no se guarda
	}

	@Test
	void sinTarifarioNiAnioAnteriorLaPropuestaVieneVaciaConElCalendarioGenerado() {
		UUID id2030 = anioPorSql(2030, "PLANIFICADO"); // no existe 2029

		TarifarioVista vista = servicio.ver(id2030);

		assertThat(vista.existe()).isFalse();
		assertThat(vista.conceptos().montoCuotaIngreso()).isNull();
		assertThat(vista.conceptos().montoMatricula()).isNull();
		assertThat(vista.conceptos().montoPension()).isNull();
		assertThat(vista.morosidad().diasGracia()).isNull();
		assertThat(vista.morosidad().recargoPorcentaje()).isNull();
		assertThat(vista.morosidad().topeRecargoPorcentaje()).isNull();
		assertThat(vista.calendario().cuotas()).hasSize(10).extracting(CuotaVista::monto).containsOnlyNulls();
		assertThat(vista.calendario().totalPensiones()).isNull();
	}

	@Test
	void elTarifario2026EsDeSoloLecturaPorTenerCompromisos() {
		TarifarioVista vista = servicio.ver(ID_2026);

		assertThat(vista.existe()).isTrue();
		assertThat(vista.anio()).isEqualTo(2026);
		assertThat(vista.estado()).isEqualTo(EstadoTarifario.CONFIRMADO);
		assertThat(vista.soloLectura()).isTrue();
		assertThat(vista.motivoSoloLectura())
				.isEqualTo(
						"Ya se generaron compromisos de pago 2026: los montos y el calendario no se pueden modificar.");
		assertThat(vista.calendarioDesactualizado()).isFalse();
		assertThat(vista.conceptos().montoMatricula()).isEqualByComparingTo("420");
		assertThat(vista.conceptos().vencimientoMatricula()).isEqualTo(fecha(2026, 2, 28));
		assertThat(vista.calendario().totalPensiones()).isEqualByComparingTo("6000");
	}

	@Test
	void elTarifario2027EstaEnConfiguracionYSePuedeEditar() {
		TarifarioVista vista = servicio.ver(ID_2027);

		assertThat(vista.existe()).isTrue();
		assertThat(vista.estado()).isEqualTo(EstadoTarifario.EN_CONFIGURACION);
		assertThat(vista.soloLectura()).isFalse();
		assertThat(vista.calendarioDesactualizado()).isFalse();
		assertThat(vista.calendario().cuotas()).extracting(CuotaVista::numero).containsExactly(1, 2, 3, 4, 5, 6, 7, 8,
				9, 10);
		assertThat(vista.calendario().cuotas().getFirst().nombre()).isEqualTo("Pensión de marzo");
	}

	@Test
	void verUnAnioQueNoExisteDa404() {
		rechaza(HttpStatus.NOT_FOUND, "El año escolar no existe.", () -> servicio.ver(UUID.randomUUID()));
		rechaza(HttpStatus.NOT_FOUND, "El año escolar no existe.",
				() -> servicio.confirmar(UUID.randomUUID(), solicitudValida()));
	}

	// --- Confirmar ---

	@Test
	void confirmaElTarifario2027GuardandoLosDatosYDejandoloConfirmado() {
		TarifarioVista vista = servicio.confirmar(ID_2027, solicitudValida());

		assertThat(vista.existe()).isTrue();
		assertThat(vista.estado()).isEqualTo(EstadoTarifario.CONFIRMADO);
		assertThat(vista.conceptos().montoCuotaIngreso()).isEqualByComparingTo("400");
		assertThat(vista.conceptos().montoMatricula()).isEqualByComparingTo("460");
		assertThat(vista.conceptos().montoPension()).isEqualByComparingTo("650.50");
		assertThat(vista.calendario().totalPensiones()).isEqualByComparingTo("6505");
		assertThat(vista.soloLectura()).isFalse();
		assertThat(jdbc.sql("SELECT estado FROM pagos.tarifario WHERE id = ?").param(TARIFARIO_2027)
				.query(String.class).single()).isEqualTo("CONFIRMADO");
		assertThat(jdbc.sql("SELECT monto_pension FROM pagos.tarifario WHERE id = ?").param(TARIFARIO_2027)
				.query(BigDecimal.class).single()).isEqualByComparingTo("650.50");
		assertThat(jdbc.sql("SELECT dias_gracia FROM pagos.tarifario WHERE id = ?").param(TARIFARIO_2027)
				.query(Integer.class).single()).isEqualTo(7);
		assertThat(cuotasGuardadas(TARIFARIO_2027)).isEqualTo(10);
	}

	@Test
	void confirmarCreaElTarifarioDeUnAnioQueNoLoTenia() {
		UUID id2028 = anioPorSql(2028, "PLANIFICADO");

		TarifarioVista vista = servicio.confirmar(id2028, solicitudValida());

		assertThat(vista.existe()).isTrue();
		assertThat(vista.estado()).isEqualTo(EstadoTarifario.CONFIRMADO);
		assertThat(jdbc.sql("SELECT count(*) FROM pagos.tarifario WHERE anio_escolar_id = ? AND estado = 'CONFIRMADO'")
				.param(id2028).query(Integer.class).single()).isEqualTo(1);
		assertThat(jdbc.sql("""
				SELECT count(*) FROM pagos.cuota_tarifario c JOIN pagos.tarifario t ON t.id = c.tarifario_id
				WHERE t.anio_escolar_id = ?""").param(id2028).query(Integer.class).single()).isEqualTo(10);
	}

	@Test
	void confirmarOtraVezUnTarifarioGuardadoRegeneraElCalendarioSinChocarConLosUnique() {
		servicio.confirmar(ID_2027, solicitudValida()); // el 2027 ya tenía sus 10 cuotas: se quitan y se vuelven a crear

		TarifarioVista segunda = servicio.confirmar(ID_2027, solicitud("500", "450", "620", 5, "2", "10"));

		assertThat(segunda.estado()).isEqualTo(EstadoTarifario.CONFIRMADO);
		assertThat(segunda.calendario().cuotas()).hasSize(10);
		assertThat(cuotasGuardadas(TARIFARIO_2027)).isEqualTo(10);
		assertThat(servicio.vigente(ID_2027)).get().extracting(v -> v.montoPension()).isEqualTo(d("620.00"));
	}

	@Test
	void confirmarRechazaLosDatosInvalidosConSuMensaje() {
		rechaza(HttpStatus.BAD_REQUEST, "Ingresa el tope de recargo.",
				() -> servicio.confirmar(ID_2027, solicitud("500", "450", "620", 5, "2", null)));
		rechaza(HttpStatus.BAD_REQUEST, "El recargo no puede ser mayor que el tope de recargo.",
				() -> servicio.confirmar(ID_2027, solicitud("500", "450", "620", 5, "11", "10")));
		rechaza(HttpStatus.BAD_REQUEST, "Los días de gracia deben estar entre 0 y 60.",
				() -> servicio.confirmar(ID_2027, solicitud("500", "450", "620", 61, "2", "10")));
		rechaza(HttpStatus.BAD_REQUEST, "El monto de la pensión debe ser mayor que cero.",
				() -> servicio.confirmar(ID_2027, solicitud("500", "450", "0", 5, "2", "10")));
	}

	@Test
	void unDatoInvalidoNoBorraNiCambiaNadaDelTarifarioGuardado() {
		rechaza(HttpStatus.BAD_REQUEST, "Ingresa el tope de recargo.",
				() -> servicio.confirmar(ID_2027, solicitud("500", "450", "620", 5, "2", null)));

		assertThat(cuotasGuardadas(TARIFARIO_2027)).isEqualTo(10);
		assertThat(servicio.ver(ID_2027).calendario().cuotas()).hasSize(10);
		assertThat(servicio.ver(ID_2027).estado()).isEqualTo(EstadoTarifario.EN_CONFIGURACION);
	}

	// --- Solo lectura ---

	@Test
	void unTarifarioConfirmadoConCompromisosNoSePuedeVolverAConfirmar() {
		rechaza(HttpStatus.CONFLICT,
				"Ya se generaron compromisos de pago 2026: los montos y el calendario no se pueden modificar.",
				() -> servicio.confirmar(ID_2026, solicitudValida()));
	}

	@Test
	void unTarifarioEnConfiguracionConUnCompromisoSiSePuedeConfirmar() {
		compromisoDelTarifario2027PorSql();

		TarifarioVista antes = servicio.ver(ID_2027);
		assertThat(antes.estado()).isEqualTo(EstadoTarifario.EN_CONFIGURACION);
		assertThat(antes.soloLectura()).isFalse();

		TarifarioVista vista = servicio.confirmar(ID_2027, solicitudValida());

		assertThat(vista.estado()).isEqualTo(EstadoTarifario.CONFIRMADO);
		// ahora sí: CONFIRMADO con compromisos es de solo lectura
		assertThat(servicio.ver(ID_2027).soloLectura()).isTrue();
		rechaza(HttpStatus.CONFLICT,
				"Ya se generaron compromisos de pago 2027: los montos y el calendario no se pueden modificar.",
				() -> servicio.confirmar(ID_2027, solicitudValida()));
	}

	@Test
	void unAnioCerradoConservaSuTarifarioComoRegistroHistorico() {
		jdbc.sql("UPDATE configuracion.anio_escolar SET estado = 'CERRADO' WHERE id = ?").param(ID_2027).update();
		entityManager.clear();

		TarifarioVista vista = servicio.ver(ID_2027);

		assertThat(vista.soloLectura()).isTrue();
		assertThat(vista.motivoSoloLectura())
				.isEqualTo("Año escolar cerrado: el tarifario se conserva como registro histórico.");
		rechaza(HttpStatus.CONFLICT, "Año escolar cerrado: el tarifario se conserva como registro histórico.",
				() -> servicio.confirmar(ID_2027, solicitudValida()));
	}

	@Test
	void unAnioCerradoSinTarifarioTampocoPermiteCrearlo() {
		UUID id2025 = anioPorSql(2025, "CERRADO");

		assertThat(servicio.ver(id2025).soloLectura()).isTrue();
		rechaza(HttpStatus.CONFLICT, "Año escolar cerrado: el tarifario se conserva como registro histórico.",
				() -> servicio.confirmar(id2025, solicitudValida()));
	}

	// --- Para otros módulos ---

	@Test
	void vigenteYCalendarioSoloExistenParaUnTarifarioConfirmado() {
		var vigente = servicio.vigente(ID_2026).orElseThrow();
		assertThat(vigente.tarifarioId()).isEqualTo(TARIFARIO_2026);
		assertThat(vigente.montoCuotaIngreso()).isEqualByComparingTo("500");
		assertThat(vigente.montoMatricula()).isEqualByComparingTo("420");
		assertThat(vigente.vencimientoMatricula()).isEqualTo(fecha(2026, 2, 28));
		assertThat(vigente.montoPension()).isEqualByComparingTo("600");
		assertThat(vigente.diasGracia()).isEqualTo(5);
		assertThat(vigente.recargoPorcentaje()).isEqualByComparingTo("2");
		assertThat(vigente.topeRecargoPorcentaje()).isEqualByComparingTo("10");

		var calendario = servicio.calendario(ID_2026).orElseThrow();
		assertThat(calendario.montoPension()).isEqualByComparingTo("600");
		assertThat(calendario.cuotas()).hasSize(10);
		assertThat(calendario.cuotas().getFirst().numero()).isEqualTo(1);
		assertThat(calendario.cuotas().getFirst().nombre()).isEqualTo("Pensión de marzo");
		assertThat(calendario.cuotas().getFirst().fechaVencimiento()).isEqualTo(fecha(2026, 3, 31));
		assertThat(calendario.cuotas().getLast().fechaVencimiento()).isEqualTo(fecha(2026, 12, 15));

		assertThat(servicio.vigente(ID_2027)).isEmpty(); // EN_CONFIGURACION
		assertThat(servicio.calendario(ID_2027)).isEmpty();
		assertThat(servicio.vigente(UUID.randomUUID())).isEmpty();

		servicio.confirmar(ID_2027, solicitudValida());
		assertThat(servicio.vigente(ID_2027)).get().extracting(v -> v.tarifarioId()).isEqualTo(TARIFARIO_2027);
		assertThat(servicio.calendario(ID_2027)).isPresent();
	}

	// --- Editar las fechas del año (HU-74) ---

	@Test
	void editarLasFechasDelAnioRegeneraElCalendarioDelTarifarioConfirmadoSinTocarMontosNiEstado() {
		servicio.confirmar(ID_2027, solicitudValida());

		anios.editar(ID_2027, new SolicitudEdicionAnio(fecha(2027, 4, 5), fecha(2027, 11, 26)));

		TarifarioVista vista = servicio.ver(ID_2027);
		assertThat(vista.estado()).isEqualTo(EstadoTarifario.CONFIRMADO);
		assertThat(vista.calendarioDesactualizado()).isFalse();
		assertThat(vista.conceptos().cantidadCuotas()).isEqualTo(8);
		assertThat(vista.conceptos().vencimientoMatricula()).isEqualTo(fecha(2027, 3, 31));
		assertThat(vista.calendario().cuotas().getFirst().nombre()).isEqualTo("Pensión de abril");
		assertThat(vista.calendario().cuotas().getLast().fechaVencimiento()).isEqualTo(fecha(2027, 11, 15));
		assertThat(vista.conceptos().montoPension()).isEqualByComparingTo("650.50");
		assertThat(vista.morosidad().diasGracia()).isEqualTo(7);
		assertThat(cuotasGuardadas(TARIFARIO_2027)).isEqualTo(8);
		assertThat(jdbc.sql("SELECT cantidad_cuotas FROM pagos.tarifario WHERE id = ?").param(TARIFARIO_2027)
				.query(Integer.class).single()).isEqualTo(8);
	}

	@Test
	void editarLasFechasTambienRegeneraElCalendarioDeUnTarifarioEnConfiguracion() {
		anios.editar(ID_2027, new SolicitudEdicionAnio(fecha(2027, 3, 1), fecha(2027, 11, 26)));

		TarifarioVista vista = servicio.ver(ID_2027);
		assertThat(vista.estado()).isEqualTo(EstadoTarifario.EN_CONFIGURACION);
		assertThat(vista.conceptos().cantidadCuotas()).isEqualTo(9);
		assertThat(vista.calendario().cuotas().getLast().fechaVencimiento()).isEqualTo(fecha(2027, 11, 15));
	}

	@Test
	void conCompromisosNoSeRegeneraElCalendarioYElTarifarioAvisaQueQuedoDesactualizado() {
		compromisoDelTarifario2027PorSql();

		anios.editar(ID_2027, new SolicitudEdicionAnio(fecha(2027, 4, 5), fecha(2027, 11, 26)));

		TarifarioVista vista = servicio.ver(ID_2027);
		assertThat(vista.conceptos().cantidadCuotas()).isEqualTo(10); // el calendario guardado no cambió
		assertThat(vista.calendario().cuotas().getLast().fechaVencimiento()).isEqualTo(fecha(2027, 12, 15));
		assertThat(vista.conceptos().vencimientoMatricula()).isEqualTo(fecha(2027, 2, 28));
		assertThat(vista.calendarioDesactualizado()).isTrue();
	}

	@Test
	void editarLasFechasRechazaUnCalendarioQueNoSeriaValidoExplicandoLaRelacionConElTarifario() {
		rechaza(HttpStatus.BAD_REQUEST,
				"No se pueden cambiar las fechas: el calendario del tarifario 2027 no sería válido con ellas. "
						+ "El calendario debe tener entre 1 y 12 cuotas, pero el año escolar abarca 13 meses.",
				() -> anios.editar(ID_2027, new SolicitudEdicionAnio(fecha(2027, 3, 1), fecha(2028, 3, 20))));

		// el 400 no tocó nada: ni las cuotas del tarifario ni las fechas ni los periodos del año
		assertThat(cuotasGuardadas(TARIFARIO_2027)).isEqualTo(10);
		assertThat(servicio.ver(ID_2027).calendario().cuotas()).hasSize(10);
		var anio = anios.detalle(ID_2027);
		assertThat(anio.fechaFin()).isEqualTo(fecha(2027, 12, 17));
		assertThat(anio.periodos()).hasSize(3);
		assertThat(anio.periodos().getLast().fechaFin()).isEqualTo(fecha(2027, 12, 17));
	}

	@Test
	void editarLasFechasDeUnAnioSinTarifarioNoCreaNingunoNiFalla() {
		UUID id2028 = anioPorSql(2028, "PLANIFICADO");

		anios.editar(id2028, new SolicitudEdicionAnio(fecha(2028, 3, 6), fecha(2028, 12, 8)));

		assertThat(servicio.ver(id2028).existe()).isFalse();
	}
}
