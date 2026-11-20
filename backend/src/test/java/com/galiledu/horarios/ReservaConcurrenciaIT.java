package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-48, concurrencia: dos reservas que chocan se piden a la vez. El bloqueo del año hace que la segunda vea lo que
 * confirmó la primera, así que una responde 201 y la otra 409. Estas pruebas NO usan la transacción de prueba (cada
 * petición confirma de verdad), así que lo que crean se borra por SQL en {@code @AfterEach}. Todo ocurre en 2027.
 */
@Import(RelojDeReservasConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class ReservaConcurrenciaIT extends PruebaReservas {
	private static final long ESPERA_MAXIMA_SEGUNDOS = 60;
	private static final String LUNES = "2027-03-08";

	private UUID seccionUno;
	private UUID seccionDos;
	private UUID lab;
	private UUID labDos;
	private UUID maria;
	private UUID otroResponsable;
	private String coordinador;

	@BeforeEach
	void preparar() throws Exception {
		limpiar();
		estructura("EH-T-27", "SECUNDARIA", true, 6);
		lab = espacio("TMP-LAB", "ACTIVO", 30);
		labDos = espacio("TMP-LAB2", "ACTIVO", 30);
		seccionUno = seccion("SEC-1", "C1", null, null);
		seccionDos = seccion("SEC-1", "C2", null, null);
		maria = docente();
		otroResponsable = usuario("79999951", "U20269951", "ACTIVO", false, "TUTOR");
		coordinador = token(COORDINADOR);
	}

	@AfterEach
	void limpiar() {
		jdbc.sql("DELETE FROM horarios.reserva_ambiente WHERE ambiente_id IN "
				+ "(SELECT id FROM configuracion.ambiente WHERE codigo LIKE 'TMP-%')").update();
		jdbc.sql("DELETE FROM configuracion.seccion WHERE anio_escolar_id = :anio AND nombre IN ('C1', 'C2')")
				.param("anio", ANIO_2027).update();
		jdbc.sql("DELETE FROM configuracion.estructura_horaria WHERE anio_escolar_id = :anio")
				.param("anio", ANIO_2027).update();
		jdbc.sql("DELETE FROM configuracion.ambiente WHERE codigo LIKE 'TMP-%'").update();
		jdbc.sql("DELETE FROM seguridad.usuario_rol WHERE usuario_id IN "
				+ "(SELECT id FROM seguridad.usuario WHERE nombre_usuario = 'U20269951')").update();
		jdbc.sql("DELETE FROM seguridad.usuario WHERE nombre_usuario = 'U20269951'").update();
		jdbc.sql("DELETE FROM personas.persona WHERE numero_documento = '79999951'").update();
	}

	private MvcResult reservar(UUID ambiente, UUID seccion, UUID responsable) throws Exception {
		return mvc.perform(conToken(post("/horarios/reservas"), coordinador,
				cuerpoReserva(ambiente, seccion, responsable, "Experimento", LUNES, "08:00", "09:00")))
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

	private void verUnoGanaYElOtroPierde(List<MvcResult> resultados, String fragmentoDelMensaje) throws Exception {
		assertThat(resultados.stream().map(r -> r.getResponse().getStatus()).sorted().toList())
				.containsExactly(201, 409);
		MvcResult perdedor = resultados.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst()
				.orElseThrow();
		assertThat(cuerpoDe(perdedor)).contains(fragmentoDelMensaje);
		assertThat(jdbc.sql("""
				SELECT count(*) FROM horarios.reserva_ambiente
				WHERE estado = 'ACTIVA' AND seccion_id IN (:uno, :dos)""").param("uno", seccionUno)
				.param("dos", seccionDos).query(Long.class).single()).isEqualTo(1L);
	}

	@DisplayName("Dos secciones piden el mismo laboratorio a la misma hora una gana y la otra recibe cruce de ambiente")
	@Test
	void dosSeccionesPidenElMismoLaboratorioALaMismaHoraUnaGanaYLaOtraRecibeCruceDeAmbiente() throws Exception {
		List<MvcResult> resultados = enParalelo(
				List.of(() -> reservar(lab, seccionUno, maria), () -> reservar(lab, seccionDos, otroResponsable)));

		verUnoGanaYElOtroPierde(resultados, "Ambiente no disponible: Espacio TMP-LAB ya está reservado por 1.° C");
	}

	@DisplayName("El mismo responsable en dos ambientes a la misma hora una gana y la otra recibe cruce de responsable")
	@Test
	void elMismoResponsableEnDosAmbientesALaMismaHoraUnaGanaYLaOtraRecibeCruceDeResponsable() throws Exception {
		List<MvcResult> resultados = enParalelo(
				List.of(() -> reservar(lab, seccionUno, maria), () -> reservar(labDos, seccionDos, maria)));

		verUnoGanaYElOtroPierde(resultados,
				"Responsable no disponible: María Fernández Ríos ya es responsable de Experimento con 1.° C");
	}
}
