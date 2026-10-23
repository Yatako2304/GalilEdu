package com.galiledu.configuracion;

import static com.galiledu.configuracion.AyudasEntradasHostiles.cuerpoDeCrear;
import static com.galiledu.configuracion.AyudasEntradasHostiles.cuerpoDeEditar;
import static com.galiledu.configuracion.AyudasEntradasHostiles.rastrosEn;
import static com.galiledu.configuracion.AyudasEntradasHostiles.texto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/**
 * HU-44, concurrencia: varias peticiones a la vez sobre las mismas estructuras. Estas pruebas NO usan la transacción
 * de prueba (cada petición confirma de verdad, como en producción, y es la única forma de que el bloqueo por año y las
 * restricciones de la BD actúen), así que todo lo que crean se borra por SQL en {@code @AfterEach}. Todo ocurre en el
 * año 2027 (PLANIFICADO y sin estructuras de partida). Cada prueba tiene un tiempo máximo para que nunca se cuelgue.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class EstructuraHorariaConcurrenciaIT extends PruebaIntegracion {
	private static final String ANIO_2027 = "30000000-0000-0000-0000-000000002027";
	private static final int HILOS = 6;
	private static final long ESPERA_MAXIMA_SEGUNDOS = 60;

	@Autowired
	private JdbcClient jdbc;

	private String personalAdministrativo;

	@BeforeEach
	void prepararYLimpiar() throws Exception {
		limpiar();
		personalAdministrativo = token(PERSONAL_ADMINISTRATIVO);
	}

	@AfterEach
	void limpiar() {
		// Los recesos se borran en cascada con su estructura.
		jdbc.sql("DELETE FROM configuracion.estructura_horaria WHERE anio_escolar_id = :anioId")
				.param("anioId", UUID.fromString(ANIO_2027)).update();
	}

	// ---------- Ayudas ----------

	private static String rutaDelAnio() {
		return "/configuracion/anios-escolares/" + ANIO_2027 + "/estructuras-horarias";
	}

	private static String rutaEstructura(String estructuraId) {
		return "/configuracion/estructuras-horarias/" + estructuraId;
	}

	private MvcResult crear(String cuerpo) throws Exception {
		return mvc.perform(conToken(post(rutaDelAnio()), personalAdministrativo, cuerpo)).andReturn();
	}

	private String crearValida(String... clavesYLiterales) throws Exception {
		MvcResult resultado = crear(cuerpoDeCrear(clavesYLiterales));
		assertThat(resultado.getResponse().getStatus()).as(cuerpoDe(resultado)).isEqualTo(201);
		return JsonPath.read(cuerpoDe(resultado), "$.estructura.id");
	}

	private static String cuerpoDe(MvcResult resultado) throws Exception {
		return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	/**
	 * Lanza todas las tareas a la vez (con una compuerta de salida común) y devuelve sus respuestas. Cada espera tiene
	 * tope y el grupo de hilos siempre se cierra, así que la prueba no puede colgarse.
	 */
	private static List<MvcResult> enParalelo(List<Callable<MvcResult>> tareas) throws Exception {
		ExecutorService hilos = Executors.newFixedThreadPool(tareas.size());
		try {
			CountDownLatch listos = new CountDownLatch(tareas.size());
			CountDownLatch salida = new CountDownLatch(1);
			List<Future<MvcResult>> futuros = new ArrayList<>();
			for (Callable<MvcResult> tarea : tareas) {
				futuros.add(hilos.submit(() -> {
					listos.countDown();
					salida.await(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS);
					return tarea.call();
				}));
			}
			assertThat(listos.await(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)).as("los hilos no arrancaron").isTrue();
			salida.countDown();
			List<MvcResult> resultados = new ArrayList<>();
			for (Future<MvcResult> futuro : futuros) {
				resultados.add(futuro.get(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS));
			}
			return resultados;
		} finally {
			hilos.shutdownNow();
		}
	}

	private static List<Integer> estados(List<MvcResult> resultados) {
		return resultados.stream().map(resultado -> resultado.getResponse().getStatus()).toList();
	}

	private static void exigeSinErroresDelServidorNiFugas(List<MvcResult> resultados) throws Exception {
		for (MvcResult resultado : resultados) {
			assertThat(resultado.getResponse().getStatus()).as("cuerpo: %s", cuerpoDe(resultado)).isLessThan(500);
			assertThat(rastrosEn(cuerpoDe(resultado))).isEmpty();
		}
	}

	private int cantidadDeEstructuras() {
		return jdbc.sql("SELECT count(*) FROM configuracion.estructura_horaria WHERE anio_escolar_id = :anioId")
				.param("anioId", UUID.fromString(ANIO_2027)).query(Integer.class).single();
	}

	private int cantidadDePrincipalesDe(String nivel) {
		return jdbc.sql("""
				SELECT count(*) FROM configuracion.estructura_horaria
				WHERE anio_escolar_id = :anioId AND nivel = :nivel AND es_principal""")
				.param("anioId", UUID.fromString(ANIO_2027)).param("nivel", nivel).query(Integer.class).single();
	}

	private int recesosHuerfanos() {
		return jdbc
				.sql("""
						SELECT count(*) FROM configuracion.receso r
						WHERE NOT EXISTS (SELECT 1 FROM configuracion.estructura_horaria e WHERE e.id = r.estructura_horaria_id)""")
				.query(Integer.class).single();
	}

	// ---------- 10. Concurrencia ----------

	@Test
	void variasCreacionesSimultaneasConElMismoCodigoCreanUnaSolaYLasDemasRecibenConflicto() throws Exception {
		List<Callable<MvcResult>> tareas = new ArrayList<>();
		for (int hilo = 0; hilo < HILOS; hilo++) {
			tareas.add(() -> crear(cuerpoDeCrear("codigo", texto("EH-CARRERA"))));
		}

		List<MvcResult> resultados = enParalelo(tareas);

		exigeSinErroresDelServidorNiFugas(resultados);
		List<Integer> estados = estados(resultados);
		assertThat(estados).as("estados %s", estados).isSubsetOf(201, 409);
		assertThat(estados.stream().filter(estado -> estado == 201).count()).isEqualTo(1);
		assertThat(cantidadDeEstructuras()).isEqualTo(1);
		assertThat(cantidadDePrincipalesDe("PRIMARIA")).isEqualTo(1);
		for (MvcResult resultado : resultados) {
			if (resultado.getResponse().getStatus() == 409) {
				assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message"))
						.isEqualTo("Ya existe una estructura con el código EH-CARRERA en este año.");
			}
		}
	}

	@Test
	void variasPeticionesQueMarcanComoPrincipalEstructurasDistintasDejanUnaSolaPrincipal() throws Exception {
		crearValida("codigo", texto("EH-A"));
		String b = crearValida("codigo", texto("EH-B"));
		String c = crearValida("codigo", texto("EH-C"));
		List<Callable<MvcResult>> tareas = new ArrayList<>();
		for (int hilo = 0; hilo < HILOS; hilo++) {
			String objetivo = hilo % 2 == 0 ? b : c;
			String codigo = hilo % 2 == 0 ? "EH-B" : "EH-C";
			tareas.add(() -> mvc.perform(conToken(put(rutaEstructura(objetivo)), personalAdministrativo,
					cuerpoDeEditar("codigo", texto(codigo), "esPrincipal", "true"))).andReturn());
		}

		List<MvcResult> resultados = enParalelo(tareas);

		exigeSinErroresDelServidorNiFugas(resultados);
		assertThat(estados(resultados)).as("estados %s", estados(resultados)).isSubsetOf(200, 409);
		assertThat(cantidadDeEstructuras()).isEqualTo(3);
		assertThat(cantidadDePrincipalesDe("PRIMARIA")).as("principales al final").isEqualTo(1);
	}

	@Test
	void crearDosEstructurasPrincipalesALaVezDejaUnaSolaPrincipal() throws Exception {
		crearValida("codigo", texto("EH-A"));
		List<Callable<MvcResult>> tareas = List.of(
				() -> crear(cuerpoDeCrear("codigo", texto("EH-B"), "esPrincipal", "true")),
				() -> crear(cuerpoDeCrear("codigo", texto("EH-C"), "esPrincipal", "true")));

		List<MvcResult> resultados = enParalelo(tareas);

		exigeSinErroresDelServidorNiFugas(resultados);
		assertThat(estados(resultados)).as("estados %s", estados(resultados)).isSubsetOf(201, 409);
		assertThat(cantidadDePrincipalesDe("PRIMARIA")).as("principales al final").isEqualTo(1);
		assertThat(cantidadDeEstructuras()).isEqualTo(1 + (int) estados(resultados).stream()
				.filter(estado -> estado == 201).count());
	}

	@Test
	void eliminarYEditarALaVezLaMismaEstructuraNoDejaNadaACedias() throws Exception {
		crearValida("codigo", texto("EH-A"));
		for (int ronda = 0; ronda < 5; ronda++) {
			String id = crearValida("codigo", texto("EH-B" + ronda), "recesos",
					"[{\"despuesDeBloque\":2,\"duracionMin\":15}]");
			List<Callable<MvcResult>> tareas = List.of(
					() -> mvc.perform(conToken(delete(rutaEstructura(id)), personalAdministrativo)).andReturn(),
					() -> mvc.perform(conToken(put(rutaEstructura(id)), personalAdministrativo,
							cuerpoDeEditar("codigo", texto("EH-EDITADA"), "recesos",
									"[{\"despuesDeBloque\":3,\"duracionMin\":20}]")))
							.andReturn());

			List<MvcResult> resultados = enParalelo(tareas);

			exigeSinErroresDelServidorNiFugas(resultados);
			assertThat(resultados.get(0).getResponse().getStatus()).as("eliminar, ronda %d", ronda).isEqualTo(204);
			assertThat(resultados.get(1).getResponse().getStatus()).as("editar, ronda %d", ronda).isIn(200, 404);
			assertThat(jdbc.sql("SELECT count(*) FROM configuracion.estructura_horaria WHERE id = :id")
					.param("id", UUID.fromString(id)).query(Integer.class).single()).isZero();
			assertThat(recesosHuerfanos()).isZero();
		}
		assertThat(cantidadDeEstructuras()).isEqualTo(1);
	}

	// ---------- Con transacción real: una escritura rechazada no deja nada a medias ----------

	/**
	 * Corregido: el NUL en el código se rechaza con 400 antes de tocar la base (antes daba 500). Con transacción real
	 * se
	 * comprueba además que no quedó nada escrito.
	 */
	@Test
	void unNulEnElCodigoDa400YNoDejaNadaEscrito() throws Exception {
		MvcResult resultado = crear(cuerpoDeCrear("codigo", texto("A\u0000B")));

		assertThat(resultado.getResponse().getStatus()).as("cuerpo: %s", cuerpoDe(resultado)).isEqualTo(400);
		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message"))
				.isEqualTo("El texto contiene caracteres no válidos.");
		assertThat(cantidadDeEstructuras()).isZero();
	}

	/**
	 * Una creación como principal que se rechaza no deja a la anterior desmarcada. Hoy el NUL en la descripción se
	 * rechaza al validar, antes de bajar a la anterior; el camino "falla el guardado después de bajarla" ya no se puede
	 * provocar desde la API (se probó antes de la corrección: la transacción real lo deshacía).
	 */
	@Test
	void unaCreacionPrincipalRechazadaNoDejaDesmarcadaALaAnterior() throws Exception {
		String principal = crearValida("codigo", texto("EH-A"));

		MvcResult resultado = crear(cuerpoDeCrear("codigo", texto("EH-B"), "esPrincipal", "true", "descripcion",
				texto("x\u0000y")));

		assertThat(resultado.getResponse().getStatus()).as("cuerpo: %s", cuerpoDe(resultado)).isEqualTo(400);
		assertThat(jdbc.sql("SELECT es_principal FROM configuracion.estructura_horaria WHERE id = :id")
				.param("id", UUID.fromString(principal)).query(Boolean.class).single()).isTrue();
		assertThat(cantidadDeEstructuras()).isEqualTo(1);
	}

	/**
	 * Corregido: un NUL en la descripción ya no da el 409 "Otra persona acaba de cambiar…" (culpaba a otra persona sin
	 * motivo): da 400 con el mensaje del texto.
	 */
	@Test
	void unNulEnLaDescripcionDa400YNoCulpaAOtraPersona() throws Exception {
		MvcResult resultado = crear(cuerpoDeCrear("descripcion", texto("x\u0000y")));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
		assertThat(cuerpoDe(resultado)).doesNotContain("Otra persona");
	}

	@Test
	void editarConUnNulEnLaDescripcionNoCambiaNadaEnLaBase() throws Exception {
		String id = crearValida("codigo", texto("EH-A"));
		String antes = AyudasEntradasHostiles.fotoDeEstructuras(jdbc, true);

		MvcResult resultado = mvc.perform(conToken(put(rutaEstructura(id)), personalAdministrativo,
				cuerpoDeEditar("esPrincipal", "true", "descripcion", texto("x\u0000y")))).andReturn();

		assertThat(resultado.getResponse().getStatus()).as("cuerpo: %s", cuerpoDe(resultado)).isBetween(400, 499);
		assertThat(AyudasEntradasHostiles.fotoDeEstructuras(jdbc, true)).isEqualTo(antes);
	}
}
