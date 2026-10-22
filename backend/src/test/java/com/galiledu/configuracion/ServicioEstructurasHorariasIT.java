package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.DetalleEstructuraHoraria;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.EstructuraGuardada;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.EstructuraHorariaResumen;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.EstructurasHorariasDelAnio;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.SolicitudEdicionEstructuraHoraria;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.SolicitudEstructuraHoraria;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.SolicitudReceso;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.VistaPreviaJornada;
import com.galiledu.configuracion.aplicacion.ServicioEstructurasHorarias;

/**
 * HU-44: las reglas del servicio de estructuras horarias, sin pasar por HTTP. 2026 es el año ACTIVO con EH-PRI-26 y
 * EH-SEC-26; 2027 está PLANIFICADO y sin estructuras, y es donde se crean y cambian. Los planes de evaluación se arman
 * por SQL en 2027.
 */
class ServicioEstructurasHorariasIT extends PruebaIntegracion {
	private static final UUID ANIO_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");
	private static final UUID ANIO_2027 = UUID.fromString("30000000-0000-0000-0000-000000002027");
	private static final UUID EH_PRI_26 = UUID.fromString("31000000-0000-0000-0000-000000000001");
	private static final String ANIO_NO_PLANIFICADO = "Solo se puede modificar la estructura horaria de un año planificado.";
	private static final List<String> LUN_A_VIE = List.of("LUN", "MAR", "MIE", "JUE", "VIE");

	@Autowired
	private ServicioEstructurasHorarias servicio;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager entityManager;

	// ---------- Armado de solicitudes y escenarios ----------

	/** Lunes a viernes, entrada 08:00, 6 bloques de 45 min y sin recesos: 30 bloques por semana, salida 12:30. */
	private static SolicitudEstructuraHoraria nueva(String codigo, String nivel, Boolean esPrincipal) {
		return nueva(codigo, nivel, esPrincipal, 6, LUN_A_VIE);
	}

	private static SolicitudEstructuraHoraria nueva(String codigo, String nivel, Boolean esPrincipal,
			int bloquesPorDia, List<String> dias) {
		return new SolicitudEstructuraHoraria(codigo, "Prueba", nivel, esPrincipal, dias, LocalTime.of(8, 0),
				bloquesPorDia, 45, List.of());
	}

	private static SolicitudEstructuraHoraria conRecesos(List<SolicitudReceso> recesos) {
		return new SolicitudEstructuraHoraria("EH-REC", null, "PRIMARIA", null, LUN_A_VIE, LocalTime.of(8, 0), 6, 45,
				recesos);
	}

	private static SolicitudEdicionEstructuraHoraria edicion(String codigo, boolean esPrincipal, int bloquesPorDia,
			List<String> dias) {
		return edicion(codigo, esPrincipal, bloquesPorDia, dias, LocalTime.of(8, 0), 45, List.of());
	}

	private static SolicitudEdicionEstructuraHoraria edicion(String codigo, boolean esPrincipal, int bloquesPorDia,
			List<String> dias, LocalTime horaEntrada, int duracionBloqueMin, List<SolicitudReceso> recesos) {
		return new SolicitudEdicionEstructuraHoraria(codigo, "Prueba", esPrincipal, dias, horaEntrada, bloquesPorDia,
				duracionBloqueMin, recesos);
	}

	private UUID crear(String codigo, String nivel, Boolean esPrincipal) {
		return servicio.crearEstructuraHoraria(ANIO_2027, nueva(codigo, nivel, esPrincipal)).estructura().id();
	}

	private UUID crear(String codigo, String nivel, Boolean esPrincipal, int bloquesPorDia, List<String> dias) {
		return servicio.crearEstructuraHoraria(ANIO_2027, nueva(codigo, nivel, esPrincipal, bloquesPorDia, dias))
				.estructura().id();
	}

	private EstructuraHorariaResumen deLaLista(String codigo) {
		for (EstructuraHorariaResumen resumen : servicio.listarPorAnioEscolar(ANIO_2027).estructuras()) {
			if (resumen.codigo().equals(codigo)) {
				return resumen;
			}
		}
		throw new AssertionError("No está " + codigo + " en la lista de 2027");
	}

	/** Un curso planificado en 2027. 1.° de secundaria con 10 + 10 + 8 horas tiene 28 horas por semana. */
	private void planificarEn2027(String gradoCodigo, String curso, int horas) {
		UUID cursoId = jdbc.sql("SELECT id FROM academico.curso WHERE nombre = :nombre").param("nombre", curso)
				.query(UUID.class).single();
		jdbc.sql("""
				INSERT INTO academico.plan_curso (anio_escolar_id, grado_codigo, curso_id, horas_semanales)
				VALUES (:anioId, :gradoCodigo, :cursoId, :horas)""")
				.param("anioId", ANIO_2027).param("gradoCodigo", gradoCodigo).param("cursoId", cursoId)
				.param("horas", horas).update();
		entityManager.clear();
	}

	private void planificar28HorasEnPrimeroDeSecundaria() {
		planificarEn2027("SEC-1", "Álgebra", 10);
		planificarEn2027("SEC-1", "Geometría", 10);
		planificarEn2027("SEC-1", "Aritmética", 8);
	}

	private static void assertRechazo(ThrowingCallable operacion, HttpStatus estado, String mensaje) {
		assertThatThrownBy(operacion).isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
			assertThat(e.estado()).isEqualTo(estado);
			assertThat(e).hasMessage(mensaje);
		});
	}

	private int recesosEnLaBase(UUID estructuraId) {
		entityManager.flush();
		return jdbc.sql("SELECT count(*) FROM configuracion.receso WHERE estructura_horaria_id = :id")
				.param("id", estructuraId).query(Integer.class).single();
	}

	// ---------- Listar y detalle ----------

	@Test
	void listaLasEstructurasDeUnAnioConSusIndicadores() {
		EstructurasHorariasDelAnio lista = servicio.listarPorAnioEscolar(ANIO_2026);

		assertThat(lista.anioModificable()).isFalse();
		assertThat(lista.estructuras()).extracting(EstructuraHorariaResumen::codigo).containsExactly("EH-PRI-26",
				"EH-SEC-26");
		EstructuraHorariaResumen primaria = lista.estructuras().get(0);
		assertThat(primaria.nivel()).isEqualTo("PRIMARIA");
		assertThat(primaria.esPrincipal()).isTrue();
		assertThat(primaria.diasLectivos()).containsExactly("LUN", "MAR", "MIE", "JUE", "VIE");
		assertThat(primaria.horaEntrada()).isEqualTo("07:45");
		assertThat(primaria.horaSalida()).isEqualTo("14:20");
		assertThat(primaria.bloquesPorDia()).isEqualTo(8);
		assertThat(primaria.duracionBloqueMin()).isEqualTo(45);
		assertThat(primaria.bloquesPorSemana()).isEqualTo(40);
		assertThat(primaria.recesos()).extracting(r -> r.despuesDeBloque() + ":" + r.duracionMin())
				.containsExactly("3:20", "6:15");
		assertThat(lista.estructuras().get(1).bloquesPorSemana()).isEqualTo(45);
		assertThat(lista.estructuras().get(1).horaSalida()).isEqualTo("14:50");
	}

	@Test
	void unAnioPlanificadoSinEstructurasSeMarcaModificable() {
		EstructurasHorariasDelAnio lista = servicio.listarPorAnioEscolar(ANIO_2027);

		assertThat(lista.anioModificable()).isTrue();
		assertThat(lista.estructuras()).isEmpty();
	}

	@Test
	void listarDeUnAnioQueNoExisteDa404() {
		assertRechazo(() -> servicio.listarPorAnioEscolar(UUID.randomUUID()), HttpStatus.NOT_FOUND,
				"El año escolar no existe.");
	}

	@Test
	void elDetalleTraeLaJornadaCalculadaFilaPorFila() {
		DetalleEstructuraHoraria detalle = servicio.detalleEstructuraHoraria(EH_PRI_26);

		assertThat(detalle.codigo()).isEqualTo("EH-PRI-26");
		assertThat(detalle.anioModificable()).isFalse();
		assertThat(detalle.jornada()).hasSize(10);
		assertThat(detalle.jornada().get(0).tipo()).isEqualTo("BLOQUE");
		assertThat(detalle.jornada().get(0).numero()).isEqualTo(1);
		assertThat(detalle.jornada().get(0).inicio()).isEqualTo("07:45");
		assertThat(detalle.jornada().get(0).fin()).isEqualTo("08:30");
		assertThat(detalle.jornada().get(0).duracionMin()).isEqualTo(45);
		assertThat(detalle.jornada().get(3).tipo()).isEqualTo("RECESO");
		assertThat(detalle.jornada().get(3).numero()).isNull();
		assertThat(detalle.jornada().get(3).inicio()).isEqualTo("10:00");
		assertThat(detalle.jornada().get(3).fin()).isEqualTo("10:20");
		assertThat(detalle.jornada().get(3).duracionMin()).isEqualTo(20);
		assertThat(detalle.jornada().get(9).numero()).isEqualTo(8);
		assertThat(detalle.jornada().get(9).fin()).isEqualTo("14:20");
	}

	@Test
	void elDetalleDeUnaEstructuraQueNoExisteDa404() {
		assertRechazo(() -> servicio.detalleEstructuraHoraria(UUID.randomUUID()), HttpStatus.NOT_FOUND,
				"La estructura horaria no existe.");
	}

	// ---------- Crear ----------

	@Test
	void laPrimeraEstructuraDeUnNivelNaceComoPrincipalAunqueNoSeMarque() {
		EstructuraGuardada guardada = servicio.crearEstructuraHoraria(ANIO_2027,
				nueva("  eh-pri-27 ", "primaria", false));

		assertThat(guardada.estructura().codigo()).isEqualTo("EH-PRI-27");
		assertThat(guardada.estructura().nivel()).isEqualTo("PRIMARIA");
		assertThat(guardada.estructura().esPrincipal()).isTrue();
		assertThat(guardada.estructura().anioModificable()).isTrue();
		assertThat(guardada.advertencias()).isEmpty();
		assertThat(deLaLista("EH-PRI-27").bloquesPorSemana()).isEqualTo(30);
	}

	@Test
	void laSegundaDelNivelNoEsPrincipalSalvoQueSeMarque() {
		crear("EH-A", "PRIMARIA", null);

		EstructuraGuardada segunda = servicio.crearEstructuraHoraria(ANIO_2027, nueva("EH-B", "PRIMARIA", null));

		assertThat(segunda.estructura().esPrincipal()).isFalse();
		assertThat(segunda.advertencias()).isEmpty();
		assertThat(deLaLista("EH-A").esPrincipal()).isTrue();
	}

	@Test
	void marcarComoPrincipalAlCrearBajaLaAnteriorEnLaMismaTransaccionYAvisa() {
		crear("EH-A", "PRIMARIA", null);

		EstructuraGuardada segunda = servicio.crearEstructuraHoraria(ANIO_2027, nueva("EH-B", "PRIMARIA", true));

		assertThat(segunda.estructura().esPrincipal()).isTrue();
		assertThat(deLaLista("EH-A").esPrincipal()).isFalse();
		assertThat(deLaLista("EH-B").esPrincipal()).isTrue();
		assertThat(segunda.advertencias())
				.containsExactly("EH-B pasa a ser la estructura principal de primaria; EH-A deja de serlo.");
	}

	@Test
	void cadaNivelTieneSuPropiaPrincipal() {
		crear("EH-PRI", "PRIMARIA", null);
		crear("EH-SEC", "SECUNDARIA", null);

		assertThat(deLaLista("EH-PRI").esPrincipal()).isTrue();
		assertThat(deLaLista("EH-SEC").esPrincipal()).isTrue();
	}

	@Test
	void guardaLosRecesosYCalculaLaSalida() {
		EstructuraGuardada guardada = servicio.crearEstructuraHoraria(ANIO_2027,
				conRecesos(List.of(new SolicitudReceso(4, 10), new SolicitudReceso(2, 20))));

		assertThat(guardada.estructura().recesos()).extracting(r -> r.despuesDeBloque() + ":" + r.duracionMin())
				.containsExactly("2:20", "4:10");
		// 08:00 + 6 × 45 min (12:30) + 30 min de recesos = 13:00.
		assertThat(guardada.estructura().horaSalida()).isEqualTo("13:00");
	}

	@Test
	void elCodigoRepetidoDa409AunqueVengaConEspaciosOEnMinusculas() {
		crear("EH-A", "PRIMARIA", null);

		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027, nueva(" eh-a ", "SECUNDARIA", null)),
				HttpStatus.CONFLICT, "Ya existe una estructura con el código EH-A en este año.");

		// Si el servicio no normalizara el código para buscar, la restricción de la BD daría el mismo 409 pero tras
		// intentar insertar, y en PostgreSQL eso deja la transacción abortada: esta consulta fallaría.
		assertThat(servicio.listarPorAnioEscolar(ANIO_2027).estructuras()).hasSize(1);
	}

	@Test
	void elMismoCodigoSePuedeUsarEnOtroAnio() {
		// EH-PRI-26 existe en 2026; en 2027 el código está libre.
		EstructuraGuardada guardada = servicio.crearEstructuraHoraria(ANIO_2027, nueva("EH-PRI-26", "PRIMARIA", null));

		assertThat(guardada.estructura().codigo()).isEqualTo("EH-PRI-26");
	}

	@Test
	void rechazaConMensajeElCodigoVacioYElCodigoLargo() {
		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027, nueva("   ", "PRIMARIA", null)),
				HttpStatus.BAD_REQUEST, "Ingresa el código de la estructura.");
		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027, nueva("A".repeat(21), "PRIMARIA", null)),
				HttpStatus.BAD_REQUEST, "El código admite hasta 20 caracteres.");
	}

	@Test
	void rechazaConMensajeLosDiasLectivosInvalidos() {
		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027, nueva("EH-A", "PRIMARIA", null, 6, List.of())),
				HttpStatus.BAD_REQUEST, "Elige al menos un día lectivo.");
		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027,
				nueva("EH-A", "PRIMARIA", null, 6, List.of("LUN", "LUN"))), HttpStatus.BAD_REQUEST,
				"No repitas los días lectivos.");
		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027,
				nueva("EH-A", "PRIMARIA", null, 6, List.of("LUN", "DOM"))), HttpStatus.BAD_REQUEST,
				"Día lectivo no válido: DOM.");
	}

	@Test
	void rechazaConMensajeLosBloquesYLaDuracionFueraDeRango() {
		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027, nueva("EH-A", "PRIMARIA", null, 13, LUN_A_VIE)),
				HttpStatus.BAD_REQUEST, "Los bloques por día van de 1 a 12.");
		SolicitudEstructuraHoraria duracionCorta = new SolicitudEstructuraHoraria("EH-A", null, "PRIMARIA", null,
				LUN_A_VIE, LocalTime.of(8, 0), 6, 19, List.of());
		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027, duracionCorta), HttpStatus.BAD_REQUEST,
				"La duración del bloque va de 20 a 120 minutos.");
	}

	@Test
	void rechazaConMensajeLosRecesosInvalidos() {
		assertRechazo(
				() -> servicio.crearEstructuraHoraria(ANIO_2027, conRecesos(List.of(new SolicitudReceso(3, 4)))),
				HttpStatus.BAD_REQUEST, "El receso dura de 5 a 60 minutos.");
		// Con 6 bloques el último receso posible va después del 5: tras el 6 no tendría sentido.
		assertRechazo(
				() -> servicio.crearEstructuraHoraria(ANIO_2027, conRecesos(List.of(new SolicitudReceso(6, 15)))),
				HttpStatus.BAD_REQUEST, "Un receso va después de un bloque entre 1 y 5.");
		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027,
				conRecesos(List.of(new SolicitudReceso(3, 10), new SolicitudReceso(3, 15)))), HttpStatus.BAD_REQUEST,
				"No puede haber dos recesos después del mismo bloque.");
	}

	@Test
	void laJornadaPuedeTerminarALas1800ExactasPeroNoUnMinutoDespues() {
		SolicitudEstructuraHoraria limite = new SolicitudEstructuraHoraria("EH-TAR-26", null, "SECUNDARIA", false,
				List.of("LUN", "MIE", "VIE"), LocalTime.of(14, 45), 4, 45, List.of(new SolicitudReceso(2, 15)));
		SolicitudEstructuraHoraria unMinutoMas = new SolicitudEstructuraHoraria("EH-TAR-27", null, "SECUNDARIA",
				false, List.of("LUN", "MIE", "VIE"), LocalTime.of(14, 46), 4, 45, List.of(new SolicitudReceso(2, 15)));

		EstructuraGuardada guardada = servicio.crearEstructuraHoraria(ANIO_2027, limite);

		assertThat(guardada.estructura().horaSalida()).isEqualTo("18:00");
		assertThat(guardada.estructura().bloquesPorSemana()).isEqualTo(12);
		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027, unMinutoMas), HttpStatus.BAD_REQUEST,
				"La jornada debe terminar como máximo a las 18:00.");
	}

	@Test
	void rechazaUnNivelQueNoExiste() {
		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027, nueva("EH-A", "INICIAL", null)),
				HttpStatus.BAD_REQUEST, "Elige el nivel: primaria o secundaria.");
	}

	@Test
	void crearEnUnAnioQueNoExisteDa404() {
		assertRechazo(() -> servicio.crearEstructuraHoraria(UUID.randomUUID(), nueva("EH-A", "PRIMARIA", null)),
				HttpStatus.NOT_FOUND, "El año escolar no existe.");
	}

	@Test
	void unAnioActivoRechazaCrearEditarYEliminar() {
		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2026, nueva("EH-NUEVA", "PRIMARIA", null)),
				HttpStatus.CONFLICT, ANIO_NO_PLANIFICADO);
		assertRechazo(
				() -> servicio.editarEstructuraHoraria(EH_PRI_26, edicion("EH-PRI-26", true, 8, LUN_A_VIE)),
				HttpStatus.CONFLICT, ANIO_NO_PLANIFICADO);
		assertRechazo(() -> servicio.eliminarEstructuraHoraria(EH_PRI_26), HttpStatus.CONFLICT, ANIO_NO_PLANIFICADO);
		// Nada cambió.
		assertThat(servicio.listarPorAnioEscolar(ANIO_2026).estructuras()).hasSize(2);
		assertThat(servicio.detalleEstructuraHoraria(EH_PRI_26).bloquesPorDia()).isEqualTo(8);
	}

	// ---------- Editar ----------

	@Test
	void editaLosDatosYRegeneraLosRecesosSinCambiarElNivel() {
		UUID id = servicio.crearEstructuraHoraria(ANIO_2027,
				conRecesos(List.of(new SolicitudReceso(2, 15), new SolicitudReceso(4, 20)))).estructura().id();

		EstructuraGuardada editada = servicio.editarEstructuraHoraria(id, edicion("  eh-rec-2 ", true, 4,
				List.of("LUN", "MAR"), LocalTime.of(9, 0), 50,
				List.of(new SolicitudReceso(2, 30), new SolicitudReceso(1, 10))));

		DetalleEstructuraHoraria detalle = editada.estructura();
		assertThat(detalle.codigo()).isEqualTo("EH-REC-2");
		assertThat(detalle.nivel()).isEqualTo("PRIMARIA");
		assertThat(detalle.bloquesPorSemana()).isEqualTo(8);
		assertThat(detalle.horaEntrada()).isEqualTo("09:00");
		assertThat(detalle.duracionBloqueMin()).isEqualTo(50);
		assertThat(detalle.recesos()).extracting(r -> r.despuesDeBloque() + ":" + r.duracionMin())
				.containsExactly("1:10", "2:30");
		assertThat(recesosEnLaBase(id)).isEqualTo(2);
		assertThat(editada.advertencias()).isEmpty();
	}

	@Test
	void editarConUnCodigoDeOtraEstructuraDa409PeroConservarElPropioNo() {
		UUID a = crear("EH-A", "PRIMARIA", null);
		crear("EH-B", "SECUNDARIA", null);

		assertRechazo(() -> servicio.editarEstructuraHoraria(a, edicion(" eh-b ", true, 6, LUN_A_VIE)),
				HttpStatus.CONFLICT, "Ya existe una estructura con el código EH-B en este año.");
		assertThat(servicio.editarEstructuraHoraria(a, edicion("EH-A", true, 6, LUN_A_VIE)).estructura().codigo())
				.isEqualTo("EH-A");
	}

	@Test
	void editarConDatosInvalidosDa400YNoCambiaNada() {
		UUID id = crear("EH-A", "PRIMARIA", null);

		assertRechazo(() -> servicio.editarEstructuraHoraria(id, edicion("EH-A", true, 13, LUN_A_VIE)),
				HttpStatus.BAD_REQUEST, "Los bloques por día van de 1 a 12.");

		assertThat(servicio.detalleEstructuraHoraria(id).bloquesPorDia()).isEqualTo(6);
	}

	@Test
	void editarUnaEstructuraQueNoExisteDa404() {
		assertRechazo(() -> servicio.editarEstructuraHoraria(UUID.randomUUID(), edicion("EH-A", true, 6, LUN_A_VIE)),
				HttpStatus.NOT_FOUND, "La estructura horaria no existe.");
	}

	@Test
	void noSePuedeDesmarcarLaPrincipalHayQueMarcarOtra() {
		UUID principal = crear("EH-A", "PRIMARIA", null);
		crear("EH-B", "PRIMARIA", null);

		assertRechazo(() -> servicio.editarEstructuraHoraria(principal, edicion("EH-A", false, 6, LUN_A_VIE)),
				HttpStatus.CONFLICT,
				"No se puede quitar la marca de estructura principal. Marca otra estructura de primaria como principal.");

		assertThat(deLaLista("EH-A").esPrincipal()).isTrue();
	}

	@Test
	void marcarOtraComoPrincipalBajaLaAnteriorYAvisaQueLaActualPasaASerlo() {
		crear("EH-A", "PRIMARIA", null);
		UUID otra = crear("EH-B", "PRIMARIA", null);

		EstructuraGuardada editada = servicio.editarEstructuraHoraria(otra, edicion("EH-B", true, 6, LUN_A_VIE));

		assertThat(editada.estructura().esPrincipal()).isTrue();
		assertThat(deLaLista("EH-A").esPrincipal()).isFalse();
		assertThat(deLaLista("EH-B").esPrincipal()).isTrue();
		assertThat(editada.advertencias())
				.containsExactly("EH-B pasa a ser la estructura principal de primaria; EH-A deja de serlo.");
	}

	// ---------- Planes de evaluación ----------

	@Test
	void reducirLaPrincipalPorDebajoDeLasHorasPlanificadasDa409() {
		UUID principal = crear("EH-SEC", "SECUNDARIA", null);
		planificar28HorasEnPrimeroDeSecundaria();

		assertRechazo(() -> servicio.editarEstructuraHoraria(principal, edicion("EH-SEC", true, 5, LUN_A_VIE)),
				HttpStatus.CONFLICT,
				"No se puede reducir la jornada a 25 bloques por semana: 1.° de secundaria ya tiene 28 horas de "
						+ "cursos planificadas. Reduce las horas del plan de evaluación o conserva al menos 28 bloques.");

		assertThat(servicio.detalleEstructuraHoraria(principal).bloquesPorSemana()).isEqualTo(30);
	}

	@Test
	void conVariosGradosPasadosElMensajeNombraAlDeMasHoras() {
		UUID principal = crear("EH-SEC", "SECUNDARIA", null);
		planificarEn2027("SEC-1", "Álgebra", 10);
		planificarEn2027("SEC-1", "Geometría", 8);
		planificarEn2027("SEC-3", "Álgebra", 15);
		planificarEn2027("SEC-3", "Geometría", 12);

		assertRechazo(() -> servicio.editarEstructuraHoraria(principal, edicion("EH-SEC", true, 3, LUN_A_VIE)),
				HttpStatus.CONFLICT,
				"No se puede reducir la jornada a 15 bloques por semana: 3.° de secundaria ya tiene 27 horas de "
						+ "cursos planificadas. Reduce las horas del plan de evaluación o conserva al menos 27 bloques.");
	}

	@Test
	void reducirHastaLasHorasPlanificadasSeAceptaYAvisa() {
		UUID principal = crear("EH-SEC", "SECUNDARIA", null);
		planificar28HorasEnPrimeroDeSecundaria();

		// 4 días × 7 bloques = 28: justo las horas planificadas.
		EstructuraGuardada editada = servicio.editarEstructuraHoraria(principal,
				edicion("EH-SEC", true, 7, List.of("LUN", "MAR", "MIE", "JUE")));

		assertThat(editada.estructura().bloquesPorSemana()).isEqualTo(28);
		assertThat(editada.advertencias()).containsExactly("La jornada de secundaria pasa de 30 a 28 bloques por "
				+ "semana. Revisa el plan de evaluación: 1.° de secundaria tiene 28 horas planificadas.");
	}

	@Test
	void aumentarLosBloquesNuncaSeRechazaPeroAvisaSiElNivelTienePlanes() {
		UUID principal = crear("EH-SEC", "SECUNDARIA", null);
		planificar28HorasEnPrimeroDeSecundaria();

		EstructuraGuardada editada = servicio.editarEstructuraHoraria(principal, edicion("EH-SEC", true, 7, LUN_A_VIE));

		assertThat(editada.estructura().bloquesPorSemana()).isEqualTo(35);
		assertThat(editada.advertencias()).containsExactly("La jornada de secundaria pasa de 30 a 35 bloques por "
				+ "semana. Revisa el plan de evaluación: 1.° de secundaria tiene 28 horas planificadas.");
	}

	@Test
	void sinPlanesEnElNivelNoHayAdvertenciasNiRechazos() {
		UUID principal = crear("EH-PRI", "PRIMARIA", null);
		// Los planes son de secundaria: no cuentan para primaria.
		planificar28HorasEnPrimeroDeSecundaria();

		EstructuraGuardada reducida = servicio.editarEstructuraHoraria(principal,
				edicion("EH-PRI", true, 1, List.of("LUN")));
		EstructuraGuardada aumentada = servicio.editarEstructuraHoraria(principal,
				edicion("EH-PRI", true, 10, LUN_A_VIE));

		assertThat(reducida.advertencias()).isEmpty();
		assertThat(aumentada.advertencias()).isEmpty();
	}

	@Test
	void cambiarEntradaDuracionORecesosNoAvisaPorqueNoMuevenLosBloques() {
		UUID principal = crear("EH-SEC", "SECUNDARIA", null);
		planificar28HorasEnPrimeroDeSecundaria();

		EstructuraGuardada editada = servicio.editarEstructuraHoraria(principal, edicion("EH-SEC", true, 6,
				LUN_A_VIE, LocalTime.of(9, 0), 50, List.of(new SolicitudReceso(3, 20))));

		assertThat(editada.advertencias()).isEmpty();
	}

	@Test
	void unaEstructuraQueNoEsPrincipalNoSeValidaContraLosPlanes() {
		crear("EH-SEC", "SECUNDARIA", null);
		UUID secundaria = crear("EH-TAR", "SECUNDARIA", false);
		planificar28HorasEnPrimeroDeSecundaria();

		EstructuraGuardada editada = servicio.editarEstructuraHoraria(secundaria,
				edicion("EH-TAR", false, 1, List.of("LUN")));

		assertThat(editada.estructura().bloquesPorSemana()).isEqualTo(1);
		assertThat(editada.advertencias()).isEmpty();
	}

	@Test
	void marcarComoPrincipalUnaConMenosBloquesQueLosPlanesDa409YNoCambiaNada() {
		crear("EH-SEC", "SECUNDARIA", null);
		UUID chica = crear("EH-TAR", "SECUNDARIA", false, 4, LUN_A_VIE);
		planificar28HorasEnPrimeroDeSecundaria();

		assertRechazo(() -> servicio.editarEstructuraHoraria(chica, edicion("EH-TAR", true, 4, LUN_A_VIE)),
				HttpStatus.CONFLICT,
				"No se puede reducir la jornada a 20 bloques por semana: 1.° de secundaria ya tiene 28 horas de "
						+ "cursos planificadas. Reduce las horas del plan de evaluación o conserva al menos 28 bloques.");

		// Se valida antes de modificar: la principal anterior sigue siéndolo.
		assertThat(deLaLista("EH-SEC").esPrincipal()).isTrue();
		assertThat(deLaLista("EH-TAR").esPrincipal()).isFalse();
	}

	@Test
	void marcarComoPrincipalUnaConMasBloquesAvisaQueCambiaLaPlanificacion() {
		crear("EH-SEC", "SECUNDARIA", null);
		UUID grande = crear("EH-TAR", "SECUNDARIA", false, 8, LUN_A_VIE);
		planificar28HorasEnPrimeroDeSecundaria();

		EstructuraGuardada editada = servicio.editarEstructuraHoraria(grande, edicion("EH-TAR", true, 8, LUN_A_VIE));

		assertThat(editada.advertencias()).containsExactly(
				"EH-TAR pasa a ser la estructura principal de secundaria; EH-SEC deja de serlo.",
				"La jornada de secundaria pasa de 30 a 40 bloques por semana. Revisa el plan de evaluación: "
						+ "1.° de secundaria tiene 28 horas planificadas.");
	}

	@Test
	void crearUnaPrincipalConMenosBloquesQueLosPlanesDa409YNoCambiaNada() {
		crear("EH-SEC", "SECUNDARIA", null);
		planificar28HorasEnPrimeroDeSecundaria();

		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027,
				nueva("EH-TAR", "SECUNDARIA", true, 4, LUN_A_VIE)), HttpStatus.CONFLICT,
				"No se puede reducir la jornada a 20 bloques por semana: 1.° de secundaria ya tiene 28 horas de "
						+ "cursos planificadas. Reduce las horas del plan de evaluación o conserva al menos 28 bloques.");

		assertThat(servicio.listarPorAnioEscolar(ANIO_2027).estructuras()).extracting(EstructuraHorariaResumen::codigo)
				.containsExactly("EH-SEC");
		assertThat(deLaLista("EH-SEC").esPrincipal()).isTrue();
	}

	@Test
	void laPrimeraPrincipalDeUnNivelTampocoPuedeQuedarPorDebajoDeLosPlanes() {
		planificar28HorasEnPrimeroDeSecundaria();

		assertRechazo(() -> servicio.crearEstructuraHoraria(ANIO_2027,
				nueva("EH-SEC", "SECUNDARIA", null, 4, LUN_A_VIE)), HttpStatus.CONFLICT,
				"No se puede reducir la jornada a 20 bloques por semana: 1.° de secundaria ya tiene 28 horas de "
						+ "cursos planificadas. Reduce las horas del plan de evaluación o conserva al menos 28 bloques.");
	}

	// ---------- Eliminar ----------

	@Test
	void eliminaUnaEstructuraQueNoEsPrincipalConSusRecesos() {
		crear("EH-A", "PRIMARIA", null);
		UUID otra = servicio.crearEstructuraHoraria(ANIO_2027, conRecesos(List.of(new SolicitudReceso(2, 15))))
				.estructura().id();
		assertThat(recesosEnLaBase(otra)).isEqualTo(1);

		servicio.eliminarEstructuraHoraria(otra);

		assertThat(servicio.listarPorAnioEscolar(ANIO_2027).estructuras()).extracting(EstructuraHorariaResumen::codigo)
				.containsExactly("EH-A");
		assertThat(recesosEnLaBase(otra)).isZero();
	}

	@Test
	void laPrincipalConOtrasDelNivelNoSeEliminaHayQueMarcarOtra() {
		UUID principal = crear("EH-A", "PRIMARIA", null);
		crear("EH-B", "PRIMARIA", null);

		assertRechazo(() -> servicio.eliminarEstructuraHoraria(principal), HttpStatus.CONFLICT,
				"No se puede eliminar la estructura principal de primaria: quedan otras estructuras del nivel. "
						+ "Marca otra como principal primero.");

		assertThat(servicio.listarPorAnioEscolar(ANIO_2027).estructuras()).hasSize(2);
	}

	@Test
	void laUnicaDelNivelSinPlanesSePuedeEliminar() {
		UUID unica = crear("EH-A", "PRIMARIA", null);

		servicio.eliminarEstructuraHoraria(unica);

		assertThat(servicio.listarPorAnioEscolar(ANIO_2027).estructuras()).isEmpty();
	}

	@Test
	void laUnicaDelNivelConPlanesNoSePuedeEliminar() {
		UUID unica = crear("EH-SEC", "SECUNDARIA", null);
		planificar28HorasEnPrimeroDeSecundaria();

		assertRechazo(() -> servicio.eliminarEstructuraHoraria(unica), HttpStatus.CONFLICT,
				"No se puede eliminar la estructura principal de secundaria: hay cursos planificados que dependen "
						+ "de ella. Marca otra como principal primero.");

		assertThat(servicio.listarPorAnioEscolar(ANIO_2027).estructuras()).hasSize(1);
	}

	@Test
	void eliminarUnaEstructuraQueNoExisteDa404() {
		assertRechazo(() -> servicio.eliminarEstructuraHoraria(UUID.randomUUID()), HttpStatus.NOT_FOUND,
				"La estructura horaria no existe.");
	}

	// ---------- Vista previa ----------

	@Test
	void laVistaPreviaDelModalDeFigmaCalculaLaJornadaYNoGuardaNada() {
		SolicitudEstructuraHoraria modal = new SolicitudEstructuraHoraria("EH-TAR-26", "Secundaria · talleres de tarde",
				"SECUNDARIA", false, List.of("LUN", "MIE", "VIE"), LocalTime.of(14, 45), 4, 45,
				List.of(new SolicitudReceso(2, 15)));

		VistaPreviaJornada vistaPrevia = servicio.vistaPreviaEstructuraHoraria(ANIO_2027, modal);

		assertThat(vistaPrevia.valida()).isTrue();
		assertThat(vistaPrevia.errores()).isEmpty();
		assertThat(vistaPrevia.jornada().horaSalida()).isEqualTo("18:00");
		assertThat(vistaPrevia.jornada().bloquesPorSemana()).isEqualTo(12);
		assertThat(vistaPrevia.jornada().filas()).hasSize(5);
		assertThat(vistaPrevia.jornada().filas().get(2).tipo()).isEqualTo("RECESO");
		assertThat(vistaPrevia.jornada().filas().get(2).inicio()).isEqualTo("16:15");
		assertThat(vistaPrevia.jornada().filas().get(2).fin()).isEqualTo("16:30");
		assertThat(servicio.listarPorAnioEscolar(ANIO_2027).estructuras()).isEmpty();
	}

	@Test
	void laVistaPreviaPasadaLas1800MuestraLaSalidaRealYElError() {
		SolicitudEstructuraHoraria tarde = new SolicitudEstructuraHoraria("EH-TAR-26", null, "SECUNDARIA", false,
				List.of("LUN"), LocalTime.of(14, 46), 4, 45, List.of(new SolicitudReceso(2, 15)));

		VistaPreviaJornada vistaPrevia = servicio.vistaPreviaEstructuraHoraria(ANIO_2027, tarde);

		assertThat(vistaPrevia.valida()).isFalse();
		assertThat(vistaPrevia.errores()).containsExactly("La jornada debe terminar como máximo a las 18:00.");
		assertThat(vistaPrevia.jornada().horaSalida()).isEqualTo("18:01");
	}

	@Test
	void laVistaPreviaDeUnFormularioVacioListaLosErroresSinFallar() {
		SolicitudEstructuraHoraria vacia = new SolicitudEstructuraHoraria(null, null, null, null, null, null, null,
				null, null);

		VistaPreviaJornada vistaPrevia = servicio.vistaPreviaEstructuraHoraria(ANIO_2027, vacia);

		assertThat(vistaPrevia.valida()).isFalse();
		assertThat(vistaPrevia.jornada()).isNull();
		assertThat(vistaPrevia.errores()).containsExactly(
				"Ingresa el código de la estructura.",
				"Elige al menos un día lectivo.",
				"Ingresa la hora de entrada.",
				"Los bloques por día van de 1 a 12.",
				"La duración del bloque va de 20 a 120 minutos.",
				"Elige el nivel: primaria o secundaria.");
	}

	@Test
	void laVistaPreviaAvisaDelCodigoRepetidoYDelNivelInvalido() {
		crear("EH-A", "PRIMARIA", null);

		VistaPreviaJornada vistaPrevia = servicio.vistaPreviaEstructuraHoraria(ANIO_2027,
				nueva(" eh-a ", "INICIAL", null));

		assertThat(vistaPrevia.valida()).isFalse();
		assertThat(vistaPrevia.errores()).containsExactly("Elige el nivel: primaria o secundaria.",
				"Ya existe una estructura con el código EH-A en este año.");
		assertThat(vistaPrevia.jornada()).isNotNull();
	}

	@Test
	void laVistaPreviaNoMiraElEstadoDelAnioNiLosPlanes() {
		crear("EH-SEC", "SECUNDARIA", null);
		planificar28HorasEnPrimeroDeSecundaria();

		// 2 bloques por semana son menos que las 28 horas planificadas: la vista previa no lo mira.
		VistaPreviaJornada conPlanes = servicio.vistaPreviaEstructuraHoraria(ANIO_2027,
				nueva("EH-OTRA", "SECUNDARIA", true, 2, List.of("LUN")));
		// 2026 es un año ACTIVO: solo se calcula, no se guarda.
		VistaPreviaJornada enAnioActivo = servicio.vistaPreviaEstructuraHoraria(ANIO_2026,
				nueva("EH-NUEVA", "PRIMARIA", null));

		assertThat(conPlanes.valida()).isTrue();
		assertThat(enAnioActivo.valida()).isTrue();
	}

	@Test
	void laVistaPreviaDeUnAnioQueNoExisteDa404() {
		assertRechazo(() -> servicio.vistaPreviaEstructuraHoraria(UUID.randomUUID(), nueva("EH-A", "PRIMARIA", null)),
				HttpStatus.NOT_FOUND, "El año escolar no existe.");
	}
}
