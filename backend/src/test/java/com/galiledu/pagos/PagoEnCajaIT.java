package com.galiledu.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.galiledu.configuracion.PruebaIntegracionConfiguracion;
import com.galiledu.configuracion.RelojPrueba;
import com.galiledu.pagos.aplicacion.PagoRegistrado;

/**
 * Pago en caja (fila 29 del plan de pruebas): el Personal administrativo registra el pago de cuotas desde el cronograma
 * del compromiso. Datos de prueba: compromiso 2026 de Sofía Paredes. El reloj está en el 01/11/2026: la pensión de
 * septiembre (S/ 600, vence el 30/09, 5 días de gracia) lleva un período de recargo (S/ 12.00); la de octubre (vence el
 * 31/10) sigue en gracia.
 */
@RecordApplicationEvents
class PagoEnCajaIT extends PruebaIntegracionConfiguracion {
	private static final String APODERADO = "U20260003";
	private static final String COMPROMISO_SOFIA = "51000000-0000-0000-0000-000000000001";
	private static final String SEPTIEMBRE = cuota(7);
	private static final String OCTUBRE = cuota(8);

	@Autowired
	private RelojPrueba reloj;
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
		reloj.fijarEn(LocalDate.of(2026, 11, 1));
		token = token(PERSONAL_ADMINISTRATIVO);
	}

	private static String solicitud(String fecha, String medio, String operacion, String... cuotas) {
		String op = operacion == null ? "null" : "\"" + operacion + "\"";
		return "{\"cuotaIds\":[\"%s\"],\"fechaPago\":\"%s\",\"medioPago\":\"%s\",\"numeroOperacion\":%s}"
				.formatted(String.join("\",\"", cuotas), fecha, medio, op);
	}

	private String uno(String sql, String id) {
		em.flush();
		return jdbc.sql(sql).param(id).query(String.class).single();
	}

	@DisplayName("Lista las cuotas del compromiso con el recargo a la fecha elegida")
	@Test
	void listaLasCuotasDelCompromisoConElRecargoALaFechaElegida() throws Exception {
		mvc.perform(conToken(get("/pagos/compromisos/{id}/cuotas-por-pagar", COMPROMISO_SOFIA), token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.cuotaId == '%s')].recargo".formatted(SEPTIEMBRE)).value(12.0))
				.andExpect(jsonPath("$[?(@.cuotaId == '%s')].requiere[0]".formatted(OCTUBRE)).value(SEPTIEMBRE));
		// Pagado el 03/10 todavía estaba en gracia: sin recargo.
		mvc.perform(conToken(get("/pagos/compromisos/{id}/cuotas-por-pagar", COMPROMISO_SOFIA)
				.param("fecha", "2026-10-03"), token))
				.andExpect(jsonPath("$[?(@.cuotaId == '%s')].recargo".formatted(SEPTIEMBRE)).value(0.0));
	}

	@DisplayName("Registra un pago por transferencia con su constancia y deja la cuota pagada")
	@Test
	void registraUnPagoPorTransferenciaConSuConstanciaYDejaLaCuotaPagada() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token,
				solicitud("2026-11-01", "TRANSFERENCIA", "00123456", SEPTIEMBRE)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.numeroConstancia").value(startsWith("C-2026-")))
				.andExpect(jsonPath("$.capital").value(600.0))
				.andExpect(jsonPath("$.recargo").value(12.0))
				.andExpect(jsonPath("$.total").value(612.0));

		assertThat(uno("SELECT estado FROM pagos.cuota WHERE id = ?::uuid", SEPTIEMBRE)).isEqualTo("PAGADA");
		assertThat(uno("""
				SELECT pg.referencia FROM pagos.pago pg JOIN pagos.pago_detalle d ON d.pago_id = pg.id
				WHERE d.cuota_id = ?::uuid AND pg.orden_pago_id IS NULL AND pg.registrado_por IS NOT NULL""",
				SEPTIEMBRE)).isEqualTo("Transferencia · 00123456");
		// Aparece en la pestaña «Pagos» con su medio, y Matrícula recibe el mismo evento que con el pago en línea.
		mvc.perform(conToken(get("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token))
				.andExpect(jsonPath("$[0].medio").value("Transferencia · 00123456"))
				.andExpect(jsonPath("$[0].recargo").value(12.0))
				.andExpect(jsonPath("$[1].medio").value("En línea"));
		assertThat(eventos.stream(PagoRegistrado.class))
				.singleElement()
				.satisfies(e -> assertThat(e.cuotaIds()).containsExactly(java.util.UUID.fromString(SEPTIEMBRE)));
	}

	@DisplayName("Con una fecha anterior cobra el recargo de esa fecha")
	@Test
	void conUnaFechaAnteriorCobraElRecargoDeEsaFecha() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token,
				solicitud("2026-10-03", "EFECTIVO", null, SEPTIEMBRE)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.recargo").value(0.0))
				.andExpect(jsonPath("$.total").value(600.0));
		assertThat(uno("""
				SELECT pg.referencia FROM pagos.pago pg JOIN pagos.pago_detalle d ON d.pago_id = pg.id
				WHERE d.cuota_id = ?::uuid""", SEPTIEMBRE)).isEqualTo("Efectivo");
	}

	@DisplayName("Exige pagar las pensiones en orden")
	@Test
	void exigePagarLasPensionesEnOrden() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token,
				solicitud("2026-11-01", "EFECTIVO", null, OCTUBRE)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("Pensión de septiembre")));
		mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token,
				solicitud("2026-11-01", "EFECTIVO", null, SEPTIEMBRE, OCTUBRE)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.total").value(1212.0));
	}

	@DisplayName("Rechaza una fecha futura y pide el número de operación salvo en efectivo")
	@Test
	void rechazaUnaFechaFuturaYPideElNumeroDeOperacionSalvoEnEfectivo() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token,
				solicitud("2026-11-02", "EFECTIVO", null, SEPTIEMBRE)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("posterior a hoy")));
		mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token,
				solicitud("2026-11-01", "YAPE_PLIN", " ", SEPTIEMBRE)))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token,
				solicitud("2026-11-01", "YAPE_PLIN", null, SEPTIEMBRE)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("número de operación")));
		assertThat(uno("SELECT estado FROM pagos.cuota WHERE id = ?::uuid", SEPTIEMBRE)).isEqualTo("PENDIENTE");
	}

	@DisplayName("No registra dos veces la misma operación ni una cuota ya pagada")
	@Test
	void noRegistraDosVecesLaMismaOperacionNiUnaCuotaYaPagada() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token,
				solicitud("2026-11-01", "DEPOSITO", "OP-77", SEPTIEMBRE)))
				.andExpect(status().isCreated());
		mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token,
				solicitud("2026-11-01", "DEPOSITO", "OP-77", OCTUBRE)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("OP-77")));
		mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token,
				solicitud("2026-11-01", "EFECTIVO", null, SEPTIEMBRE)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("ya no tiene saldo")));
	}

	@DisplayName("Una cuota con un pago en línea en proceso no se registra en caja")
	@Test
	void unaCuotaConUnPagoEnLineaEnProcesoNoSeRegistraEnCaja() throws Exception {
		mvc.perform(conToken(post("/pagos/pago-en-linea/ordenes"), token(APODERADO),
				"{\"cuotaIds\":[\"%s\"]}".formatted(SEPTIEMBRE)))
				.andExpect(status().isCreated());
		mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token,
				solicitud("2026-11-01", "EFECTIVO", null, SEPTIEMBRE)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("ya tiene un pago en proceso")));
	}

	@DisplayName("Un compromiso que no existe responde 404")
	@Test
	void unCompromisoQueNoExisteResponde404() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", "51000000-0000-0000-0000-0000000000ff"), token,
				solicitud("2026-11-01", "EFECTIVO", null, SEPTIEMBRE)))
				.andExpect(status().isNotFound());
	}

	@DisplayName("Solo el Personal administrativo registra pagos en caja")
	@Test
	void soloElPersonalAdministrativoRegistraPagosEnCaja() throws Exception {
		for (String usuario : new String[]{APODERADO, DOCENTE, COORDINADOR}) {
			mvc.perform(conToken(post("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), token(usuario),
					solicitud("2026-11-01", "EFECTIVO", null, SEPTIEMBRE)))
					.andExpect(status().isForbidden());
		}
	}
}
