package com.galiledu.pagos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/**
 * HU-35 · P21 Catálogo de descuentos. Datos de prueba: Hermanos matriculados (2: 10 %, 3+: 15 %), Beca institucional
 * (50 %), Acuerdo particular (20 %) y Deportista destacado (25 %, inactivo). Límite de la institución: 50 %.
 */
class CatalogoDescuentosIT extends PruebaIntegracion {
	private static final String HERMANOS = "52000000-0000-0000-0000-000000000001";

	@DisplayName("Lista el catálogo ordenado por denominación")
	@Test
	void listaElCatalogoOrdenadoPorDenominacion() throws Exception {
		mvc.perform(conToken(get("/pagos/descuentos"), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(4)))
				.andExpect(jsonPath("$[0].nombre").value("Acuerdo particular"))
				.andExpect(jsonPath("$[1].nombre").value("Beca institucional"))
				.andExpect(jsonPath("$[1].porcentaje").value(50.0))
				.andExpect(jsonPath("$[1].conceptos", hasSize(2)))
				.andExpect(jsonPath("$[3].nombre").value("Hermanos matriculados"))
				.andExpect(jsonPath("$[3].tipo").value("POR_HERMANOS"))
				.andExpect(jsonPath("$[3].tramos", hasSize(2)))
				.andExpect(jsonPath("$[3].tramos[1].hermanosHasta").isEmpty());
	}

	@DisplayName("Registra un descuento de porcentaje único")
	@Test
	void registraUnDescuentoDePorcentajeUnico() throws Exception {
		mvc.perform(conToken(post("/pagos/descuentos"), token(PERSONAL_ADMINISTRATIVO), """
				{"nombre":"Hijo de docente","tipo":"PORCENTAJE_UNICO","aplicacion":"MANUAL","porcentaje":30,
				 "conceptos":["PENSION","MATRICULA"],"condicion":"Padre o madre trabaja en el colegio"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.porcentaje").value(30))
				.andExpect(jsonPath("$.conceptos", hasSize(2)));
	}

	@DisplayName("Rechaza un descuento sin conceptos o sobre el limite")
	@Test
	void rechazaUnDescuentoSinConceptosOSobreElLimite() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(post("/pagos/descuentos"), personal, """
				{"nombre":"Sin conceptos","tipo":"PORCENTAJE_UNICO","aplicacion":"MANUAL","porcentaje":10,
				 "conceptos":[]}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Asocia al menos un concepto de pago al descuento."));
		mvc.perform(conToken(post("/pagos/descuentos"), personal, """
				{"nombre":"Muy alto","tipo":"PORCENTAJE_UNICO","aplicacion":"MANUAL","porcentaje":60,
				 "conceptos":["PENSION"]}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("El porcentaje debe estar entre 1 % y 50 % (límite de la institución)."));
	}

	@DisplayName("No repite la denominación")
	@Test
	void noRepiteLaDenominacion() throws Exception {
		mvc.perform(conToken(post("/pagos/descuentos"), token(PERSONAL_ADMINISTRATIVO), """
				{"nombre":"beca institucional","tipo":"PORCENTAJE_UNICO","aplicacion":"MANUAL","porcentaje":10,
				 "conceptos":["PENSION"]}"""))
				.andExpect(status().isConflict());
	}

	@DisplayName("El descuento por hermanos exige dos tramos que no se superpongan")
	@Test
	void elDescuentoPorHermanosExigeDosTramosQueNoSeSuperpongan() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(post("/pagos/descuentos"), personal, """
				{"nombre":"Familia numerosa","tipo":"POR_HERMANOS","aplicacion":"AUTOMATICO",
				 "tramos":[{"hermanosDesde":2,"porcentaje":10}],"conceptos":["PENSION"]}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El descuento por hermanos necesita al menos dos tramos."));
		mvc.perform(conToken(post("/pagos/descuentos"), personal, """
				{"nombre":"Familia numerosa","tipo":"POR_HERMANOS","aplicacion":"AUTOMATICO",
				 "tramos":[{"hermanosDesde":2,"hermanosHasta":3,"porcentaje":10},
				           {"hermanosDesde":3,"porcentaje":15}],"conceptos":["PENSION"]}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Los tramos de 2 y 3 hermanos se superponen."));
	}

	@DisplayName("Edita los tramos del descuento por hermanos")
	@Test
	void editaLosTramosDelDescuentoPorHermanos() throws Exception {
		mvc.perform(conToken(put("/pagos/descuentos/{id}", HERMANOS), token(PERSONAL_ADMINISTRATIVO), """
				{"nombre":"Hermanos matriculados","tipo":"POR_HERMANOS","aplicacion":"AUTOMATICO",
				 "tramos":[{"hermanosDesde":3,"porcentaje":18},{"hermanosDesde":2,"hermanosHasta":2,"porcentaje":12}],
				 "conceptos":["PENSION"],"condicion":"Dos o más hermanos matriculados"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tramos", hasSize(2)))
				.andExpect(jsonPath("$.tramos[0].hermanosDesde").value(2))
				.andExpect(jsonPath("$.tramos[0].porcentaje").value(12))
				.andExpect(jsonPath("$.tramos[1].porcentaje").value(18));
	}

	@DisplayName("Desactiva y reactiva un descuento")
	@Test
	void desactivaYReactivaUnDescuento() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		String creado = mvc.perform(conToken(post("/pagos/descuentos"), personal, """
				{"nombre":"Convenio empresa","tipo":"PORCENTAJE_UNICO","aplicacion":"MANUAL","porcentaje":15,
				 "conceptos":["PENSION"]}"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(creado, "$.id");
		mvc.perform(conToken(post("/pagos/descuentos/{id}/desactivacion", id), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INACTIVO"));
		mvc.perform(conToken(post("/pagos/descuentos/{id}/activacion", id), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVO"));
	}

	@DisplayName("El limite de la institución se puede cambiar")
	@Test
	void elLimiteDeLaInstitucionSePuedeCambiar() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(get("/pagos/descuentos/configuracion"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.porcentajeMaximo").value(50.0));
		mvc.perform(conToken(put("/pagos/descuentos/configuracion"), personal, """
				{"porcentajeMaximo":60}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.porcentajeMaximo").value(60));
		mvc.perform(conToken(post("/pagos/descuentos"), personal, """
				{"nombre":"Muy alto","tipo":"PORCENTAJE_UNICO","aplicacion":"MANUAL","porcentaje":60,
				 "conceptos":["PENSION"]}"""))
				.andExpect(status().isCreated());
		mvc.perform(conToken(put("/pagos/descuentos/configuracion"), personal, """
				{"porcentajeMaximo":120}"""))
				.andExpect(status().isBadRequest());
	}

	@DisplayName("Otro rol no puede ver el catálogo")
	@Test
	void otroRolNoPuedeVerElCatalogo() throws Exception {
		mvc.perform(conToken(get("/pagos/descuentos"), token("U20260003")))
				.andExpect(status().isForbidden());
	}
}
