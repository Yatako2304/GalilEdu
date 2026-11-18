package com.galiledu.academico;

import static com.galiledu.academico.AmbientesDePrueba.PREFIJO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * HU-43: dos peticiones {@code POST /ambientes} <b>reales y simultáneas</b>, de dos hilos distintos, contra la base
 * real y sin ninguna simulación del repositorio ni de la transacción. Cada ronda las suelta a la vez con una barrera.
 * <p>
 * Qué se exige, ronda tras ronda:
 * <ul>
 * <li>Dos creaciones del mismo tipo (aula o espacio): las dos dan 201 y nunca repiten código. Los códigos de todas las
 * rondas son consecutivos, sin huecos ni repetidos: un intento que choca se deshace entero y no consume código.</li>
 * <li>Dos creaciones con el mismo nombre (aunque cambien mayúsculas y espacios, y aunque sean de tipos distintos):
 * exactamente una da 201 y la otra 409, y en la base queda una sola fila con ese nombre. Lo mismo con dos renombres
 * al mismo nombre y con un alta contra un renombre.</li>
 * </ul>
 * <p>
 * Qué pasa en cada nivel: la BD no tiene restricción de nombre, y la del código solo frena a dos altas del mismo tipo
 * (A-… contra E-… no chocan). Sin más, una carrera con tipos distintos dejaba dos ambientes con el mismo nombre: las
 * dos
 * transacciones comprobaban el nombre antes de que la otra confirmara, y ninguna veía a la otra. Por eso crear y
 * renombrar toman un bloqueo consultivo de PostgreSQL ({@code pg_advisory_xact_lock}) que se suelta al confirmar: la
 * segunda espera, y cuando entra ya ve el nombre confirmado por la primera. Con ese bloqueo las altas se serializan, y
 * el reintento por {@code ambiente_codigo_key} queda como segunda barrera (la ejercita
 * {@code ServicioAmbientesCrearIT}).
 * <p>
 * Como no hay transacción de prueba (ver {@link AmbienteCrearIT}), todo se confirma de verdad y la limpieza es de
 * {@link AmbientesDePrueba}: solo las filas nuevas, nunca la semilla, y A-206 y E-001 quedan libres.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AmbienteConcurrenciaIT extends PruebaIntegracion {
	private static final int RONDAS = 25;
	private static final String[] USUARIOS = {PERSONAL_ADMINISTRATIVO};

	private record Respuesta(int estado, String cuerpo) {
		String texto(String ruta) {
			return JsonPath.read(cuerpo, ruta);
		}
	}

	@Autowired
	private JdbcClient jdbc;

	private Set<UUID> idsAlEmpezar;
	private List<AmbientesDePrueba.Acceso> accesosAlEmpezar;
	private String personal;
	private ExecutorService hilos;
	private ListAppender<ILoggingEvent> registro;
	private Logger registroDeErroresDeBd;

	// ---------- Preparación y limpieza ----------

	@BeforeEach
	void empezarLimpio() throws Exception {
		idsAlEmpezar = AmbientesDePrueba.empezarLimpio(jdbc);
		accesosAlEmpezar = AmbientesDePrueba.accesos(jdbc, USUARIOS);
		personal = token(PERSONAL_ADMINISTRATIVO);
		hilos = Executors.newFixedThreadPool(2);
		// Hibernate registra cada violación de unicidad que ve PostgreSQL: así se sabe cuántos choques reales hubo.
		registroDeErroresDeBd = (Logger) LoggerFactory.getLogger("org.hibernate.orm.jdbc.error");
		registro = new ListAppender<>();
		registro.start();
		registroDeErroresDeBd.addAppender(registro);
		registroDeErroresDeBd.setLevel(Level.WARN);
	}

	@AfterEach
	void limpiar() {
		registroDeErroresDeBd.detachAppender(registro);
		hilos.shutdownNow();
		AmbientesDePrueba.borrarLoCreadoDesde(jdbc, idsAlEmpezar);
		AmbientesDePrueba.restaurarAccesos(jdbc, accesosAlEmpezar, USUARIOS);
	}

	// ---------- Apoyo ----------

	private static String solicitud(String tipo, String nombre, int aforo) {
		return """
				{"tipo":"%s","nombre":"%s","aforoMaximo":%d,"recursos":[]}""".formatted(tipo, nombre, aforo);
	}

	private Callable<Respuesta> peticion(CyclicBarrier salida, RequestBuilder peticion) {
		return () -> {
			salida.await(10, TimeUnit.SECONDS); // las dos peticiones salen en el mismo instante
			var resultado = mvc.perform(peticion).andReturn().getResponse();
			return new Respuesta(resultado.getStatus(), resultado.getContentAsString());
		};
	}

	/** Dos peticiones reales a la vez, de dos hilos. */
	private List<Respuesta> enParalelo(RequestBuilder peticionA, RequestBuilder peticionB) throws Exception {
		CyclicBarrier salida = new CyclicBarrier(2);
		Future<Respuesta> a = hilos.submit(peticion(salida, peticionA));
		Future<Respuesta> b = hilos.submit(peticion(salida, peticionB));
		return List.of(a.get(60, TimeUnit.SECONDS), b.get(60, TimeUnit.SECONDS));
	}

	/** Dos {@code POST /ambientes} reales a la vez. */
	private List<Respuesta> enParalelo(String jsonA, String jsonB) throws Exception {
		return enParalelo(conToken(post("/ambientes"), personal, jsonA), conToken(post("/ambientes"), personal, jsonB));
	}

	/** Cuántas veces PostgreSQL rechazó un código repetido durante la prueba (choques reales entre las dos). */
	private long choquesDeCodigoObservados() {
		return registro.list.stream().filter(e -> e.getFormattedMessage().contains("ambiente_codigo_key")).count();
	}

	private int filasConNombre(String nombre) {
		return jdbc.sql("SELECT count(*)::int FROM configuracion.ambiente WHERE lower(btrim(nombre)) = lower(:nombre)")
				.param("nombre", nombre).query(Integer.class).single();
	}

	// ---------- Mismo tipo: sin fallos ni códigos repetidos ----------

	@Test
	void dosAulasAlMismoTiempoDan201ConCodigosDistintosYConsecutivos() throws Exception {
		Set<String> codigos = new TreeSet<>();
		for (int ronda = 1; ronda <= RONDAS; ronda++) {
			var respuestas = enParalelo(solicitud("AULA_REGULAR", PREFIJO + "Aula " + ronda + "A", 30),
					solicitud("AULA_REGULAR", PREFIJO + "Aula " + ronda + "B", 30));

			assertThat(respuestas).as("ronda %d", ronda).extracting(Respuesta::estado).containsExactly(201, 201);
			String uno = respuestas.get(0).texto("$.codigo");
			String otro = respuestas.get(1).texto("$.codigo");
			assertThat(uno).as("ronda %d: el código no se repite", ronda).isNotEqualTo(otro);
			codigos.add(uno);
			codigos.add(otro);
		}

		// 50 altas: A-206 a A-255, sin repetidos y sin huecos.
		assertThat(codigos).hasSize(2 * RONDAS);
		List<String> esperados = new ArrayList<>();
		for (int n = 206; n < 206 + 2 * RONDAS; n++) {
			esperados.add("A-%03d".formatted(n));
		}
		assertThat(codigos).containsExactlyElementsOf(esperados);
		assertThat(codigosEnLaBase("A-%")).hasSize(2 * RONDAS + 4); // las 4 aulas de la semilla (A-101, 102, 201 y 205)
		System.out.printf("[concurrencia] aulas: %d rondas, %d choques de código reales reintentados%n", RONDAS,
				choquesDeCodigoObservados());
	}

	@Test
	void dosEspaciosEspecialesAlMismoTiempoDan201ConCodigosDistintosYConsecutivos() throws Exception {
		Set<String> codigos = new TreeSet<>();
		for (int ronda = 1; ronda <= RONDAS; ronda++) {
			var respuestas = enParalelo(solicitud("ESPACIO_ESPECIAL", PREFIJO + "Espacio " + ronda + "A", 100),
					solicitud("ESPACIO_ESPECIAL", PREFIJO + "Espacio " + ronda + "B", 100));

			assertThat(respuestas).as("ronda %d", ronda).extracting(Respuesta::estado).containsExactly(201, 201);
			codigos.add(respuestas.get(0).texto("$.codigo"));
			codigos.add(respuestas.get(1).texto("$.codigo"));
		}

		List<String> esperados = new ArrayList<>();
		for (int n = 1; n <= 2 * RONDAS; n++) {
			esperados.add("E-%03d".formatted(n));
		}
		assertThat(codigos).containsExactlyElementsOf(esperados);
		System.out.printf("[concurrencia] espacios: %d rondas, %d choques de código reales reintentados%n", RONDAS,
				choquesDeCodigoObservados());
	}

	@Test
	void unAulaYUnEspacioAlMismoTiempoNoSeEstorban() throws Exception {
		for (int ronda = 1; ronda <= RONDAS; ronda++) {
			var respuestas = enParalelo(solicitud("AULA_REGULAR", PREFIJO + "Mixta aula " + ronda, 30),
					solicitud("ESPACIO_ESPECIAL", PREFIJO + "Mixta espacio " + ronda, 100));

			assertThat(respuestas).as("ronda %d", ronda).extracting(Respuesta::estado).containsExactly(201, 201);
			assertThat(respuestas.get(0).texto("$.codigo")).isEqualTo("A-%03d".formatted(205 + ronda));
			assertThat(respuestas.get(1).texto("$.codigo")).isEqualTo("E-%03d".formatted(ronda));
		}
	}

	// ---------- Mismo nombre: una 201 y una 409 ----------

	@Test
	void dosConElMismoNombreYTipoDanUna201YUna409() throws Exception {
		for (int ronda = 1; ronda <= RONDAS; ronda++) {
			String nombre = PREFIJO + "Repetida " + ronda;
			var respuestas = enParalelo(solicitud("AULA_REGULAR", nombre, 30), solicitud("AULA_REGULAR", nombre, 30));

			assertThat(respuestas).as("ronda %d", ronda).extracting(Respuesta::estado)
					.containsExactlyInAnyOrder(201, 409);
			var rechazada = respuestas.stream().filter(r -> r.estado() == 409).findFirst().orElseThrow();
			assertThat(rechazada.texto("$.message")).isEqualTo("Ya existe un ambiente llamado \"" + nombre + "\".");
			assertThat(filasConNombre(nombre)).as("ronda %d: una sola fila con ese nombre", ronda).isEqualTo(1);
		}
		System.out.printf("[concurrencia] mismo nombre y tipo: %d rondas, %d choques de código reales%n", RONDAS,
				choquesDeCodigoObservados());
	}

	@Test
	void dosConElMismoNombreYDistintoTipoTambienDanUna201YUna409() throws Exception {
		// Con tipos distintos los códigos no chocan (A-… y E-…): nada de la base obliga a que una espere a la otra.
		for (int ronda = 1; ronda <= RONDAS; ronda++) {
			String nombre = PREFIJO + "Doble tipo " + ronda;
			var respuestas = enParalelo(solicitud("AULA_REGULAR", nombre, 30),
					solicitud("ESPACIO_ESPECIAL", nombre, 30));

			assertThat(respuestas).as("ronda %d", ronda).extracting(Respuesta::estado)
					.containsExactlyInAnyOrder(201, 409);
			assertThat(filasConNombre(nombre)).as("ronda %d: una sola fila con ese nombre", ronda).isEqualTo(1);
		}
	}

	@Test
	void dosConElMismoNombreAunqueCambienMayusculasYEspaciosDanUna201YUna409() throws Exception {
		for (int ronda = 1; ronda <= RONDAS; ronda++) {
			var respuestas = enParalelo(solicitud("AULA_REGULAR", PREFIJO + "Sala norte " + ronda, 30),
					solicitud("AULA_REGULAR", "  " + PREFIJO + "SALA   NORTE " + ronda + "  ", 30));

			assertThat(respuestas).as("ronda %d", ronda).extracting(Respuesta::estado)
					.containsExactlyInAnyOrder(201, 409);
			assertThat(filasConNombre(PREFIJO + "Sala norte " + ronda)).as("ronda %d", ronda).isEqualTo(1);
		}
	}

	// ---------- Renombrar al mismo nombre ----------

	@Test
	void dosRenombresAlMismoNombreAlMismoTiempoDanUna200YUna409() throws Exception {
		for (int ronda = 1; ronda <= RONDAS; ronda++) {
			String idUno = JsonPath
					.read(mvc.perform(conToken(post("/ambientes"), personal,
							solicitud("AULA_REGULAR", PREFIJO + "Origen " + ronda + "A", 30)))
							.andReturn().getResponse().getContentAsString(), "$.id");
			String idOtro = JsonPath
					.read(mvc.perform(conToken(post("/ambientes"), personal,
							solicitud("ESPACIO_ESPECIAL", PREFIJO + "Origen " + ronda + "B", 30)))
							.andReturn().getResponse().getContentAsString(), "$.id");
			String nuevoNombre = PREFIJO + "Renombrada " + ronda;
			String cuerpo = """
					{"nombre":"%s","aforoMaximo":30,"recursos":[]}""".formatted(nuevoNombre);

			var respuestas = enParalelo(conToken(put("/ambientes/" + idUno), personal, cuerpo),
					conToken(put("/ambientes/" + idOtro), personal, cuerpo));

			assertThat(respuestas).as("ronda %d", ronda).extracting(Respuesta::estado)
					.containsExactlyInAnyOrder(200, 409);
			assertThat(filasConNombre(nuevoNombre)).as("ronda %d: una sola fila con ese nombre", ronda).isEqualTo(1);
		}
	}

	@Test
	void unaAltaYUnRenombreAlMismoNombreAlMismoTiempoDanUnGanadorYUnPerdedor() throws Exception {
		for (int ronda = 1; ronda <= RONDAS; ronda++) {
			String id = JsonPath
					.read(mvc.perform(conToken(post("/ambientes"), personal,
							solicitud("AULA_REGULAR", PREFIJO + "Para renombrar " + ronda, 30)))
							.andReturn().getResponse().getContentAsString(), "$.id");
			String nombre = PREFIJO + "Disputado " + ronda;

			var respuestas = enParalelo(
					conToken(put("/ambientes/" + id), personal,
							"""
									{"nombre":"%s","aforoMaximo":30,"recursos":[]}""".formatted(nombre)),
					conToken(post("/ambientes"), personal, solicitud("ESPACIO_ESPECIAL", nombre, 30)));

			// El renombre devuelve 200 y el alta 201; el que pierde da 409. Uno solo gana.
			assertThat(respuestas).as("ronda %d", ronda).extracting(Respuesta::estado).containsAnyOf(409);
			assertThat(respuestas).extracting(Respuesta::estado).containsOnlyOnce(409);
			assertThat(filasConNombre(nombre)).as("ronda %d: una sola fila con ese nombre", ronda).isEqualTo(1);
		}
	}

	private List<String> codigosEnLaBase(String patron) {
		return jdbc.sql("SELECT codigo FROM configuracion.ambiente WHERE codigo LIKE :patron ORDER BY codigo")
				.param("patron", patron).query(String.class).list();
	}
}
