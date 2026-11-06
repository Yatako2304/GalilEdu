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
 * HU-45, concurrencia del PUT: dos guardados a la vez sobre la misma sección. Estas pruebas NO usan la transacción de
 * prueba (cada petición confirma de verdad: es la única forma de que el bloqueo del año y la versión actúen), así que
 * lo que crean se borra por SQL en {@code @AfterEach}. Todo ocurre en 2027.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class HorarioConcurrenciaIT extends PruebaHorarios {
	private static final long ESPERA_MAXIMA_SEGUNDOS = 60;

	private UUID seccion;
	private UUID algebra;
	private UUID aritmetica;
	private UUID aula;
	private String coordinador;

	@BeforeEach
	void preparar() throws Exception {
		limpiar();
		estructura("EH-T-27", "SECUNDARIA", true, 6);
		aula = ambiente("TMP-A1", "ACTIVO");
		seccion = seccion("SEC-1", "T1", aula, null);
		algebra = plan("Álgebra", 5);
		aritmetica = plan("Aritmética", 2);
		asignarDocente(algebra, seccion, docente());
		asignarDocente(aritmetica, seccion, docente());
		coordinador = token(COORDINADOR);
	}

	@AfterEach
	void limpiar() {
		jdbc.sql("DELETE FROM horarios.horario_seccion WHERE seccion_id IN "
				+ "(SELECT id FROM configuracion.seccion WHERE anio_escolar_id = :anio AND nombre = 'T1')")
				.param("anio", ANIO_2027).update();
		// docente_seccion se borra en cascada con el plan.
		jdbc.sql("DELETE FROM academico.plan_curso WHERE anio_escolar_id = :anio").param("anio", ANIO_2027).update();
		jdbc.sql("DELETE FROM configuracion.seccion WHERE anio_escolar_id = :anio AND nombre = 'T1'")
				.param("anio", ANIO_2027).update();
		jdbc.sql("DELETE FROM configuracion.estructura_horaria WHERE anio_escolar_id = :anio")
				.param("anio", ANIO_2027).update();
		jdbc.sql("DELETE FROM configuracion.ambiente WHERE codigo LIKE 'TMP-%'").update();
	}

	private MvcResult enviar(Long version, Celda... celdas) throws Exception {
		return mvc.perform(conToken(put("/horarios/secciones/" + seccion), coordinador, cuerpo(version, celdas)))
				.andReturn();
	}

	private static List<Integer> estados(List<MvcResult> resultados) {
		return resultados.stream().map(r -> r.getResponse().getStatus()).sorted().toList();
	}

	private static String mensaje(MvcResult resultado) throws Exception {
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

	@Test
	void dosPutConLaMismaVersionUnoGanaYElOtroRecibe409() throws Exception {
		assertThat(enviar(null, new Celda("LUN", 1, algebra, aula)).getResponse().getStatus()).isEqualTo(200);

		List<MvcResult> resultados = enParalelo(List.of(
				() -> enviar(0L, new Celda("LUN", 1, algebra, aula), new Celda("MAR", 2, algebra, aula)),
				() -> enviar(0L, new Celda("LUN", 1, algebra, aula), new Celda("MIE", 3, aritmetica, aula))));

		assertThat(estados(resultados)).containsExactly(200, 409);
		MvcResult perdedor = resultados.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst()
				.orElseThrow();
		assertThat(mensaje(perdedor)).contains("Otra persona modificó el horario de 1.° de secundaria T1");
		// Solo ganó una escritura: la versión pasó de 0 a 1 y hay 2 bloques (el de antes más uno).
		assertThat(jdbc.sql("SELECT version FROM horarios.horario_seccion WHERE seccion_id = :s").param("s", seccion)
				.query(Long.class).single()).isEqualTo(1L);
		assertThat(bloquesGuardados()).isEqualTo(2);
	}

	@Test
	void dosPrimerosGuardadosALaVezUnoCreaLaCabeceraYElOtroRecibe409SinErrorInterno() throws Exception {
		List<MvcResult> resultados = enParalelo(List.of(() -> enviar(null, new Celda("LUN", 1, algebra, aula)),
				() -> enviar(null, new Celda("MAR", 2, aritmetica, aula))));

		assertThat(estados(resultados)).containsExactly(200, 409);
		assertThat(cabeceras()).isEqualTo(1);
		assertThat(bloquesGuardados()).isEqualTo(1);
	}
}
