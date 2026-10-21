package com.galiledu.matricula;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.galiledu.PruebaIntegracion;

/**
 * HU-27 y HU-30: matrícula vista por el apoderado. Datos de prueba: Luis Paredes (U20260003) con Mateo (sección
 * reservada en 1.° A de 2027) y Sofía (pensión de septiembre 2026 vencida).
 */
class MatriculaApoderadoIT extends PruebaIntegracion {
	private static final String APODERADO = "U20260003";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	/** Valeria Sánchez: no es hija de Luis Paredes. */
	private static final String VALERIA = "20000000-0000-0000-0000-000000000003";

	@Test
	void listaLosHijosDelApoderado() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes"), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].nombre",
						containsInAnyOrder("Mateo Alonso Paredes Rojas", "Sofía Paredes Rojas")));
	}

	@Test
	void laDeudaDeLaHermanaBloqueaLaMatriculaDeMateo() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				// HU-27: periodo 2027 y sección reservada
				.andExpect(jsonPath("$.periodo.anio").value(2027))
				.andExpect(jsonPath("$.periodo.estado").value("HABILITADO"))
				.andExpect(jsonPath("$.matricula.estado").value("SECCION_RESERVADA"))
				.andExpect(jsonPath("$.matricula.seccion").value("1.° A"))
				.andExpect(jsonPath("$.pasoActual").value(1))
				// HU-30 (RF-64): la pensión vencida es de Sofía, su hermana
				.andExpect(jsonPath("$.deuda.total").value(greaterThanOrEqualTo(600.0)))
				.andExpect(jsonPath("$.deuda.cuotas[0].estudiante").value("Sofía Paredes Rojas"))
				.andExpect(jsonPath("$.deuda.cuotas[0].concepto").value("Pensión de septiembre"))
				.andExpect(jsonPath("$.impedimentos[*].tipo", hasItem("DEUDA")))
				.andExpect(jsonPath("$.puedeContinuar").value(false));
	}

	@Test
	void noPuedeVerUnEstudianteQueNoEstaASuCargo() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", VALERIA), token(APODERADO)))
				.andExpect(status().isNotFound());
	}

	@Test
	void otroRolNoPuedeUsarLaVistaDelApoderado() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes"), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isForbidden());
	}
}
