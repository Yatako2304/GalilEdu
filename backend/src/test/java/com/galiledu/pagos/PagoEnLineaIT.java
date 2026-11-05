package com.galiledu.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.galiledu.configuracion.PruebaIntegracionConfiguracion;
import com.galiledu.configuracion.RelojPrueba;
import com.galiledu.pagos.aplicacion.PagoEnLineaAprobado;
import com.galiledu.pagos.aplicacion.ServicioPagoEnLinea;
import com.galiledu.pagos.infraestructura.PasarelaSimulada;
import com.jayway.jsonpath.JsonPath;

/**
 * HU-41 · Pago en línea de cuotas pendientes (criterios CAHU41), con la pasarela simulada.
 * Datos de prueba: Luis Paredes (U20260003), papá de Sofía. El reloj está en el 20/10/2026: las pensiones de marzo a
 * agosto de Sofía están pagadas; la de septiembre (S/ 600, vence el 30/09, 5 días de gracia) está vencida con un
 * período de recargo (2 % = S/ 12.00) y la de octubre aún no vence.
 */
@RecordApplicationEvents
class PagoEnLineaIT extends PruebaIntegracionConfiguracion {
	private static final String APODERADO = "U20260003";
	private static final String SOFIA = "20000000-0000-0000-0000-000000000002";
	private static final String SEPTIEMBRE = cuota(7);
	private static final String OCTUBRE = cuota(8);

	@Autowired
	private RelojPrueba reloj;
	@Autowired
	private ServicioPagoEnLinea servicio;
	@Autowired
	private PasarelaSimulada pasarela;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private ApplicationEvents eventos;
	@Autowired
	private EntityManager em;

	private String token;

	/** Ids de las cuotas de Sofía en los datos de prueba: md5('galiledu-cuota-sofia-' || numero). */
	private static String cuota(int numero) {
		try {
			byte[] h = java.security.MessageDigest.getInstance("MD5")
					.digest(("galiledu-cuota-sofia-" + numero).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			String hex = java.util.HexFormat.of().formatHex(h);
			return hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-"
					+ hex.substring(16, 20) + "-" + hex.substring(20);
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	@BeforeEach
	void prepararDia() throws Exception {
		reloj.fijarEn(LocalDate.of(2026, 10, 20));
		token = token(APODERADO);
	}

	private String crearOrden(String... cuotas) throws Exception {
		String ids = String.join("\",\"", cuotas);
		String respuesta = mvc.perform(conToken(post("/pagos/pago-en-linea/ordenes"), token,
				"{\"cuotaIds\":[\"" + ids + "\"]}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(respuesta, "$.codigo");
	}

	private String estadoCuota(String id) {
		em.flush();
		return jdbc.sql("SELECT estado FROM pagos.cuota WHERE id = ?::uuid").param(id).query(String.class).single();
	}

	@Test
	void muestraLasCuotasPorPagarConCapitalYRecargoSeparados() throws Exception {
		mvc.perform(conToken(get("/pagos/pago-en-linea/cuotas"), token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estudiantes[?(@.estudianteId == '%s')].nombre".formatted(SOFIA))
						.value("Sofía Paredes Rojas"))
				.andExpect(jsonPath("$..cuotas[?(@.cuotaId == '%s')].capital".formatted(SEPTIEMBRE)).value(600.0))
				.andExpect(jsonPath("$..cuotas[?(@.cuotaId == '%s')].recargo".formatted(SEPTIEMBRE)).value(12.0))
				.andExpect(jsonPath("$..cuotas[?(@.cuotaId == '%s')].total".formatted(SEPTIEMBRE)).value(612.0))
				.andExpect(jsonPath("$..cuotas[?(@.cuotaId == '%s')].vencida".formatted(SEPTIEMBRE)).value(true))
				.andExpect(jsonPath("$..cuotas[?(@.cuotaId == '%s')].recargo".formatted(OCTUBRE)).value(0.0))
				// CAHU41-3: octubre se marca junto con septiembre
				.andExpect(jsonPath("$..cuotas[?(@.cuotaId == '%s')].requiere[0]".formatted(OCTUBRE))
						.value(SEPTIEMBRE));
	}

	@Test
	void creaLaOrdenConElRecargoYDejaLaCuotaEnProceso() throws Exception {
		mvc.perform(conToken(post("/pagos/pago-en-linea/ordenes"), token,
				"{\"cuotaIds\":[\"%s\"]}".formatted(SEPTIEMBRE)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.codigo").value(startsWith("OP-2026-")))
				.andExpect(jsonPath("$.total").value(612.0))
				.andExpect(jsonPath("$.urlPago").value(containsString("/app/apoderado/pensiones/pasarela-simulada")));

		assertThat(estadoCuota(SEPTIEMBRE)).isEqualTo("EN_PROCESO");
		// CAHU41-4 y CAHU41-12: no se puede volver a elegir y no cuenta como vencida mientras se resuelve.
		mvc.perform(conToken(get("/pagos/pago-en-linea/cuotas"), token))
				.andExpect(jsonPath("$..cuotas[?(@.cuotaId == '%s')].seleccionable".formatted(SEPTIEMBRE))
						.value(false))
				.andExpect(jsonPath("$..cuotas[?(@.cuotaId == '%s')].vencida".formatted(SEPTIEMBRE)).value(false));
	}

	@Test
	void exigePagarLasPensionesEnOrden() throws Exception {
		mvc.perform(conToken(post("/pagos/pago-en-linea/ordenes"), token,
				"{\"cuotaIds\":[\"%s\"]}".formatted(OCTUBRE)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("Primero paga la «Pensión de septiembre»")));
	}

	@Test
	void unaCuotaEnProcesoNoEntraAOtraOrden() throws Exception {
		crearOrden(SEPTIEMBRE);
		mvc.perform(conToken(post("/pagos/pago-en-linea/ordenes"), token,
				"{\"cuotaIds\":[\"%s\"]}".formatted(SEPTIEMBRE)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("ya tiene un pago en proceso")));
	}

	@Test
	void pagoAprobadoRegistraElPagoConConstanciaYRecargo() throws Exception {
		String codigo = crearOrden(SEPTIEMBRE, OCTUBRE);

		mvc.perform(conToken(post("/pagos/pago-en-linea/simulador/{codigo}", codigo), token, "{\"aprobar\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("APROBADA"))
				.andExpect(jsonPath("$.total").value(1212.0))
				.andExpect(jsonPath("$.constancias", hasSize(1)))
				.andExpect(jsonPath("$.constancias[0].numero").value(startsWith("C-2026-")))
				.andExpect(jsonPath("$.constancias[0].estado").value("CONFIRMADO"));

		assertThat(estadoCuota(SEPTIEMBRE)).isEqualTo("PAGADA");
		assertThat(estadoCuota(OCTUBRE)).isEqualTo("PAGADA");
		em.flush();
		BigDecimal recargo = jdbc.sql("SELECT recargo FROM pagos.pago_detalle WHERE cuota_id = ?::uuid")
				.param(SEPTIEMBRE).query(BigDecimal.class).single();
		assertThat(recargo).isEqualByComparingTo("12.00");
		// HU-31 escucha este evento para matricular cuando la cuota pagada es la de matrícula.
		assertThat(eventos.stream(PagoEnLineaAprobado.class))
				.singleElement()
				.satisfies(e -> assertThat(e.cuotaIds()).hasSize(2));
	}

	@Test
	void unaNotificacionRepetidaNoDuplicaElPago() throws Exception {
		String codigo = crearOrden(SEPTIEMBRE);
		pasarela.decidir("SIM-" + codigo, true);
		servicio.notificar("SIM-" + codigo);
		servicio.notificar("SIM-" + codigo);
		mvc.perform(conToken(get("/pagos/pago-en-linea/ordenes/{codigo}", codigo), token))
				.andExpect(jsonPath("$.estado").value("APROBADA"))
				.andExpect(jsonPath("$.constancias", hasSize(1)));
	}

	@Test
	void pagoRechazadoDevuelveLasCuotasASuEstado() throws Exception {
		String codigo = crearOrden(SEPTIEMBRE);
		mvc.perform(conToken(post("/pagos/pago-en-linea/simulador/{codigo}", codigo), token, "{\"aprobar\":false}"))
				.andExpect(jsonPath("$.estado").value("RECHAZADA"))
				.andExpect(jsonPath("$.motivo").value("Tarjeta rechazada (simulación)."))
				.andExpect(jsonPath("$.constancias", hasSize(0)));
		assertThat(estadoCuota(SEPTIEMBRE)).isEqualTo("PENDIENTE");
	}

	@Test
	void cancelarLiberaLasCuotas() throws Exception {
		String codigo = crearOrden(SEPTIEMBRE);
		mvc.perform(conToken(post("/pagos/pago-en-linea/ordenes/{codigo}/cancelar", codigo), token))
				.andExpect(jsonPath("$.estado").value("RECHAZADA"))
				.andExpect(jsonPath("$.motivo").value("Cancelaste el pago."));
		assertThat(estadoCuota(SEPTIEMBRE)).isEqualTo("PENDIENTE");
	}

	@Test
	void laOrdenSinPagarExpiraALosTreintaMinutos() throws Exception {
		String codigo = crearOrden(SEPTIEMBRE);
		assertThat(servicio.ordenesVencidas()).doesNotContain(codigo);

		reloj.fijarEn(LocalDate.of(2026, 10, 21));
		assertThat(servicio.ordenesVencidas()).contains(codigo);
		servicio.resolverSiVencio(codigo);

		mvc.perform(conToken(get("/pagos/pago-en-linea/ordenes/{codigo}", codigo), token))
				.andExpect(jsonPath("$.estado").value("EXPIRADA"));
		assertThat(estadoCuota(SEPTIEMBRE)).isEqualTo("PENDIENTE");
	}

	@Test
	void siLaPasarelaConfirmaOtroMontoElPagoQuedaObservado() throws Exception {
		String codigo = crearOrden(SEPTIEMBRE);
		pasarela.aprobarPor("SIM-" + codigo, new BigDecimal("600.00"));
		servicio.notificar("SIM-" + codigo);

		mvc.perform(conToken(get("/pagos/pago-en-linea/ordenes/{codigo}", codigo), token))
				.andExpect(jsonPath("$.estado").value("APROBADA"))
				.andExpect(jsonPath("$.montoConfirmado").value(600.0))
				.andExpect(jsonPath("$.constancias[0].estado").value("OBSERVADO"));
		em.flush();
		String observacion = jdbc.sql("""
				SELECT observacion FROM pagos.pago p JOIN pagos.orden_pago o ON o.id = p.orden_pago_id
				WHERE o.codigo = ?""").param(codigo).query(String.class).single();
		assertThat(observacion).contains("S/ 600.00").contains("S/ 612.00");
	}

	@Test
	void noSePuedenPagarCuotasAjenasNiInexistentes() throws Exception {
		mvc.perform(conToken(post("/pagos/pago-en-linea/ordenes"), token,
				"{\"cuotaIds\":[\"00000000-0000-0000-0000-000000000000\"]}"))
				.andExpect(status().isNotFound());
		// Otra familia: el personal administrativo no es apoderado de nadie.
		mvc.perform(conToken(get("/pagos/pago-en-linea/cuotas"), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isForbidden());
	}

	@Test
	void laOrdenDeOtroUsuarioNoSeVe() throws Exception {
		String codigo = crearOrden(SEPTIEMBRE);
		jdbc.sql("UPDATE pagos.orden_pago SET usuario_id = '10000000-0000-0000-0000-000000000001' WHERE codigo = ?")
				.param(codigo).update();
		em.clear();
		mvc.perform(conToken(get("/pagos/pago-en-linea/ordenes/{codigo}", codigo), token))
				.andExpect(status().isNotFound());
	}
}
