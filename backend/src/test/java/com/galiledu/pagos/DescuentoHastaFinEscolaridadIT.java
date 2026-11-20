package com.galiledu.pagos;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.galiledu.PruebaIntegracion;

/**
 * Corrección del PO: un descuento puede regir "hasta que el estudiante termine el colegio", sin fecha de fin.
 * Datos de prueba: compromiso 2026 de Sofía con las pensiones de septiembre a diciembre pendientes.
 */
class DescuentoHastaFinEscolaridadIT extends PruebaIntegracion {
	private static final String COMPROMISO_SOFIA = "51000000-0000-0000-0000-000000000001";
	private static final String BECA = "52000000-0000-0000-0000-000000000002";

	private static String aplicar(String vigenciaHasta, Boolean hastaFin) {
		return """
				{"descuentoId":"%s","motivo":"Beca de excelencia","vigenciaDesde":"2026-03-01",
				 "vigenciaHasta":%s,"hastaFinEscolaridad":%s,"reemplazar":false}"""
				.formatted(BECA, vigenciaHasta == null ? "null" : "\"" + vigenciaHasta + "\"", hastaFin);
	}

	@DisplayName("Se aplica sin fecha de fin hasta que termine el colegio")
	@Test
	void seAplicaSinFechaDeFinHastaQueTermineElColegio() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA), personal,
				aplicar(null, true)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.descuento.hastaFinEscolaridad").value(true))
				.andExpect(jsonPath("$.descuento.vigenciaHasta").doesNotExist())
				.andExpect(jsonPath("$.cuotasRecalculadas").value(4));
		mvc.perform(conToken(get("/pagos/compromisos/{id}/descuento", COMPROMISO_SOFIA), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo.hastaFinEscolaridad").value(true))
				.andExpect(jsonPath("$.historial[0].hastaFinEscolaridad").value(true));
	}

	@DisplayName("Con fecha de fin la fecha es obligatoria")
	@Test
	void conFechaDeFinLaFechaEsObligatoria() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA),
				token(PERSONAL_ADMINISTRATIVO), aplicar(null, false)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("hasta cuándo")));
	}

	@DisplayName("Hasta fin de escolaridad no admite fecha de fin")
	@Test
	void hastaFinDeEscolaridadNoAdmiteFechaDeFin() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA),
				token(PERSONAL_ADMINISTRATIVO), aplicar("2026-12-31", true)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("no indiques la fecha de fin")));
	}

	@DisplayName("Con fecha de fin guarda la fecha")
	@Test
	void conFechaDeFinGuardaLaFecha() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA),
				token(PERSONAL_ADMINISTRATIVO), aplicar("2026-12-31", false)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.descuento.hastaFinEscolaridad").value(false))
				.andExpect(jsonPath("$.descuento.vigenciaHasta").value("2026-12-31"));
	}

	@DisplayName("Sin el indicador una fecha vacia sigue siendo sin fin")
	@Test
	void sinElIndicadorUnaFechaVaciaSigueSiendoSinFin() throws Exception {
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA),
				token(PERSONAL_ADMINISTRATIVO), """
						{"descuentoId":"%s","motivo":"Beca de excelencia","reemplazar":false}""".formatted(BECA)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.descuento.hastaFinEscolaridad").value(true));
	}
}
