package com.galiledu.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/**
 * HU-38 · P10 Descuento aplicado. Datos de prueba: compromiso 2026 de Sofía, con matrícula y pensiones de marzo a
 * agosto pagadas y de septiembre a diciembre pendientes (S/ 600 cada una). Sofía es la única hija matriculada en 2026.
 */
class DescuentoAplicadoIT extends PruebaIntegracion {
	private static final String COMPROMISO_SOFIA = "51000000-0000-0000-0000-000000000001";
	private static final String HERMANOS = "52000000-0000-0000-0000-000000000001";
	private static final String BECA = "52000000-0000-0000-0000-000000000002";
	private static final String ACUERDO = "52000000-0000-0000-0000-000000000003";
	private static final String DEPORTISTA = "52000000-0000-0000-0000-000000000004";

	@Autowired
	private JdbcClient jdbc;

	/** Descuento de cada cuota del compromiso de Sofía, en orden de vencimiento. */
	private List<BigDecimal> descuentos() {
		return jdbc.sql("""
				SELECT monto_descuento FROM pagos.cuota WHERE compromiso_id = ?::uuid ORDER BY fecha_vencimiento""")
				.param(COMPROMISO_SOFIA).query(BigDecimal.class).list();
	}

	private static String aplicar(String descuentoId, boolean reemplazar) {
		return """
				{"descuentoId":"%s","motivo":"Convenio con la familia","vigenciaDesde":"2026-03-01",
				 "reemplazar":%s}""".formatted(descuentoId, reemplazar);
	}

	@Test
	void listaLosDescuentosAplicablesSegunLosHermanosMatriculados() throws Exception {
		mvc.perform(conToken(get("/pagos/compromisos/{id}/descuentos-aplicables", COMPROMISO_SOFIA),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hermanosMatriculados").value(1))
				.andExpect(jsonPath("$.actual").isEmpty())
				// El inactivo (Deportista destacado) no se ofrece
				.andExpect(jsonPath("$.descuentos", hasSize(3)))
				.andExpect(jsonPath("$.descuentos[1].nombre").value("Beca institucional"))
				.andExpect(jsonPath("$.descuentos[1].porcentaje").value(50.0))
				.andExpect(jsonPath("$.descuentos[2].nombre").value("Hermanos matriculados"))
				.andExpect(jsonPath("$.descuentos[2].aplicable").value(false));
	}

	@Test
	void aplicaElDescuentoSoloALasCuotasPendientes() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA),
				token(PERSONAL_ADMINISTRATIVO), aplicar(ACUERDO, false)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cuotasRecalculadas").value(4))
				.andExpect(jsonPath("$.descuento.nombre").value("Acuerdo particular"))
				.andExpect(jsonPath("$.descuento.porcentaje").value(20.0))
				.andExpect(jsonPath("$.descuento.motivo").value("Convenio con la familia"))
				.andExpect(jsonPath("$.descuento.aplicadoPor").isNotEmpty());
		// RF-88: matrícula y marzo a agosto (pagadas) no cambian; septiembre a diciembre: 20 % de 600
		assertThat(descuentos()).extracting(BigDecimal::doubleValue)
				.containsExactly(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 120.0, 120.0, 120.0, 120.0);
	}

	@Test
	void reemplazarElDescuentoVigenteExigeConfirmacion() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA), personal,
				aplicar(ACUERDO, false)))
				.andExpect(status().isCreated());
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA), personal,
				aplicar(BECA, false)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message", containsString("Acuerdo particular")));
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA), personal,
				aplicar(BECA, true)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cuotasRecalculadas").value(4));
		assertThat(descuentos().get(10)).isEqualByComparingTo("300.00");

		mvc.perform(conToken(get("/pagos/compromisos/{id}/descuento", COMPROMISO_SOFIA), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo.nombre").value("Beca institucional"))
				.andExpect(jsonPath("$.cuotasConDescuento").value(4))
				.andExpect(jsonPath("$.historial", hasSize(2)))
				.andExpect(jsonPath("$.historial[?(@.nombre == 'Acuerdo particular')].estado").value("REEMPLAZADO"));
	}

	@Test
	void revocarRestituyeElMontoOriginal() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		String respuesta = mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA),
				personal, aplicar(BECA, false)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(respuesta, "$.descuento.id");

		mvc.perform(conToken(post("/pagos/descuentos-aplicados/{id}/revocacion", id), personal, """
				{"motivo":""}"""))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(post("/pagos/descuentos-aplicados/{id}/revocacion", id), personal, """
				{"motivo":"Perdió la beca"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.descuento.estado").value("REVOCADO"))
				.andExpect(jsonPath("$.cuotasRecalculadas").value(4));
		assertThat(descuentos()).allSatisfy(d -> assertThat(d).isEqualByComparingTo("0"));
		mvc.perform(conToken(post("/pagos/descuentos-aplicados/{id}/revocacion", id), personal, """
				{"motivo":"Otra vez"}"""))
				.andExpect(status().isConflict());
	}

	@Test
	void noAplicaUnDescuentoInactivoNiUnoSinTramo() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA), personal,
				aplicar(DEPORTISTA, false)))
				.andExpect(status().isConflict());
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA), personal,
				aplicar(HERMANOS, false)))
				.andExpect(status().isConflict());
	}

	@Test
	void elMotivoEsObligatorio() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA),
				token(PERSONAL_ADMINISTRATIVO), """
						{"descuentoId":"%s","motivo":"  "}""".formatted(ACUERDO)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void elApoderadoNoPuedeAplicarDescuentos() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA), token("U20260003"),
				aplicar(ACUERDO, false)))
				.andExpect(status().isForbidden());
	}
}
