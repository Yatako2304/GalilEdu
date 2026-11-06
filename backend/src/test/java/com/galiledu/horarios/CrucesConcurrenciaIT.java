package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-46, concurrencia: dos secciones distintas guardan a la vez un bloque que choca entre ellas. El bloqueo del año
 * hace que el segundo guardado vea lo que confirmó el primero, así que uno responde 200 y el otro 409. Estas pruebas
 * NO usan la transacción de prueba (cada petición confirma de verdad), así que lo que crean se borra por SQL en
 * {@code @AfterEach}. Todo ocurre en 2027.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class CrucesConcurrenciaIT extends PruebaHorarios {
	private static final long ESPERA_MAXIMA_SEGUNDOS = 60;

	private UUID seccionUno;
	private UUID seccionDos;
	private UUID algebra;
	private UUID aulaUno;
	private UUID aulaDos;
	private String coordinador;

	@BeforeEach
	void preparar() throws Exception {
		limpiar();
		estructura("EH-T-27", "SECUNDARIA", true, 6);
		aulaUno = ambiente("TMP-A1", "ACTIVO");
		aulaDos = ambiente("TMP-A2", "ACTIVO");
		algebra = plan("Álgebra", 5);
		coordinador = token(COORDINADOR);
	}

	@AfterEach
	void limpiar() {
		jdbc.sql("DELETE FROM horarios.horario_seccion WHERE seccion_id IN "
				+ "(SELECT id FROM configuracion.seccion WHERE anio_escolar_id = :anio AND nombre IN ('C1', 'C2'))")
				.param("anio", ANIO_2027).update();
		// docente_seccion se borra en cascada con el plan.
		jdbc.sql("DELETE FROM academico.plan_curso WHERE anio_escolar_id = :anio").param("anio", ANIO_2027).update();
		jdbc.sql("DELETE FROM configuracion.seccion WHERE anio_escolar_id = :anio AND nombre IN ('C1', 'C2')")
				.param("anio", ANIO_2027).update();
		jdbc.sql("DELETE FROM configuracion.estructura_horaria WHERE anio_escolar_id = :anio")
				.param("anio", ANIO_2027).update();
		jdbc.sql("DELETE FROM configuracion.ambiente WHERE codigo LIKE 'TMP-%'").update();
	}

	private UUID otroDocente() {
		return jdbc.sql("SELECT id FROM seguridad.usuario WHERE nombre_usuario = :u").param("u", COORDINADOR)
				.query(UUID.class).single();
	}

	private MvcResult enviar(UUID seccion, Celda celda) throws Exception {
		return mvc.perform(conToken(put("/horarios/secciones/" + seccion), coordinador, cuerpo(null, celda)))
				.andReturn();
	}

	private static String cuerpoDe(MvcResult resultado) throws Exception {
		return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	/** Lanza las tareas a la vez (con una compuerta de salida común); cada espera tiene tope. */
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

	private void verUnoGanaYElOtroPierde(List<MvcResult> resultados, String... fragmentosDelMensaje)
			throws Exception {
		assertThat(resultados.stream().map(r -> r.getResponse().getStatus()).sorted().toList())
				.containsExactly(200, 409);
		MvcResult perdedor = resultados.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst()
				.orElseThrow();
		assertThat(cuerpoDe(perdedor)).contains(fragmentosDelMensaje);
	}

	@Test
	void dosSeccionesConElMismoDocenteAlMismoBloqueUnaGanaYLaOtraRecibeCruceDeDocente() throws Exception {
		seccionUno = seccion("SEC-1", "C1", aulaUno, null);
		seccionDos = seccion("SEC-1", "C2", aulaDos, null);
		asignarDocente(algebra, seccionUno, docente());
		asignarDocente(algebra, seccionDos, docente());

		List<MvcResult> resultados = enParalelo(List.of(() -> enviar(seccionUno, new Celda("LUN", 1, algebra, aulaUno)),
				() -> enviar(seccionDos, new Celda("LUN", 1, algebra, aulaDos))));

		verUnoGanaYElOtroPierde(resultados, "Docente no disponible: María Fernández Ríos ya dicta Álgebra con 1.° C");
		assertThat(bloquesGuardados()).isEqualTo(1);
		assertThat(jdbc
				.sql("""
						SELECT count(*) FROM horarios.horario_bloque hb
						JOIN horarios.horario_seccion hs ON hs.id = hb.horario_seccion_id
						JOIN academico.docente_seccion ds ON ds.plan_curso_id = hb.plan_curso_id AND ds.seccion_id = hs.seccion_id
						WHERE ds.docente_id = :docente AND hb.dia = 'LUN' AND hb.numero_bloque = 1""")
				.param("docente", docente()).query(Long.class).single()).isEqualTo(1L);
	}

	@Test
	void dosSeccionesConElMismoAmbienteAlMismoBloqueUnaGanaYLaOtraRecibeCruceDeAmbiente() throws Exception {
		seccionUno = seccion("SEC-1", "C1", aulaUno, null);
		seccionDos = seccion("SEC-1", "C2", aulaUno, null);
		asignarDocente(algebra, seccionUno, docente());
		asignarDocente(algebra, seccionDos, otroDocente());

		List<MvcResult> resultados = enParalelo(List.of(() -> enviar(seccionUno, new Celda("LUN", 1, algebra, aulaUno)),
				() -> enviar(seccionDos, new Celda("LUN", 1, algebra, aulaUno))));

		verUnoGanaYElOtroPierde(resultados, "Ambiente no disponible: 1.° C",
				" ya usa Aula TMP-A1 para Álgebra el lunes de 8:00 a 8:45.");
		assertThat(bloquesGuardados()).isEqualTo(1);
		assertThat(jdbc.sql("""
				SELECT count(*) FROM horarios.horario_bloque
				WHERE ambiente_id = :ambiente AND dia = 'LUN' AND numero_bloque = 1""")
				.param("ambiente", aulaUno).query(Long.class).single()).isEqualTo(1L);
	}
}
