package com.galiledu.matricula;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.galiledu.PruebaIntegracion;

class MatriculaAdministrativoIT extends PruebaIntegracion {
	private static final String PERIODO_2027 = "30000000-0000-0000-0000-000000002027";
	private static final String MATRICULA_MATEO = "50000000-0000-0000-0000-000000000002";

	@Test
	void listaLasMatriculasRealesDelPeriodo() throws Exception {
		mvc.perform(conToken(get("/matricula/administrativo/periodos/{id}/matriculas", PERIODO_2027),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(MATRICULA_MATEO)))
				.andExpect(jsonPath("$[?(@.id == '%s')].nombre".formatted(MATRICULA_MATEO))
						.value(hasItem("Mateo Alonso Paredes Rojas")))
				.andExpect(jsonPath("$[?(@.id == '%s')].estado".formatted(MATRICULA_MATEO))
						.value(hasItem("SECCION_RESERVADA")))
				.andExpect(jsonPath("$[?(@.id == '%s')].seccion".formatted(MATRICULA_MATEO))
						.value(hasItem(nullValue())));
	}

	@Test
	void abreElDetalleConElMismoIdQueUsaPagos() throws Exception {
		mvc.perform(conToken(get("/matricula/administrativo/matriculas/{id}", MATRICULA_MATEO),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(MATRICULA_MATEO))
				.andExpect(jsonPath("$.numeroDocumento").value("78945612"))
				.andExpect(jsonPath("$.apoderado").isNotEmpty());
	}

	@Test
	void rechazaOtrosRoles() throws Exception {
		mvc.perform(conToken(get("/matricula/administrativo/periodos/{id}/matriculas", PERIODO_2027),
				token("U20260003")))
				.andExpect(status().isForbidden());
	}
}
