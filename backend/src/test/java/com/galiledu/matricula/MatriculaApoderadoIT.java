package com.galiledu.matricula;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * HU-27 y HU-30: matrícula vista por el apoderado. Datos de prueba: Luis Paredes (U20260003) con Mateo (sección
 * reservada en 1.° A de 2027) y Sofía (pensión de septiembre 2026 vencida).
 */
class MatriculaApoderadoIT extends PruebaIntegracion {
	@org.springframework.beans.factory.annotation.Autowired
	private JdbcClient jdbc;

	private static final String APODERADO = "U20260003";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	private static final String SOFIA = "20000000-0000-0000-0000-000000000002";
	/** Valeria Sánchez: no es hija de Luis Paredes. */
	private static final String VALERIA = "20000000-0000-0000-0000-000000000003";

	@DisplayName("Lista los hijos del apoderado")
	@Test
	void listaLosHijosDelApoderado() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes"), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].nombre",
						containsInAnyOrder("Mateo Alonso Paredes Rojas", "Sofía Paredes Rojas")));
	}

	@DisplayName("La deuda de la hermana no bloquea la matrícula de mateo")
	@Test
	void laDeudaDeLaHermanaNoBloqueaLaMatriculaDeMateo() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				// HU-27: periodo 2027 y sección reservada. El estado visible del periodo depende de la fecha.
				.andExpect(jsonPath("$.periodo.anio").value(2027))
				.andExpect(jsonPath("$.matricula.estado").value("SECCION_RESERVADA"))
				// La sección no se muestra al apoderado hasta que la matrícula esté confirmada.
				.andExpect(jsonPath("$.matricula.seccion").doesNotExist())
				.andExpect(jsonPath("$.pasoActual").value(1))
				// HU-30 (RF-64, corregido): la pensión vencida es de Sofía, no de Mateo
				.andExpect(jsonPath("$.deuda.total").value(0))
				.andExpect(jsonPath("$.deuda.cuotas", hasSize(0)))
				.andExpect(jsonPath("$.impedimentos[*].tipo", not(hasItem("DEUDA"))));
	}

	@DisplayName("La sección solo se muestra con la matrícula confirmada")
	@Test
	void laSeccionSoloSeMuestraConLaMatriculaConfirmada() throws Exception {
		jdbc.sql("""
				UPDATE matricula.matricula SET estado = 'MATRICULADO', codigo = 'MAT-2027-9001', confirmado_en = now()
				WHERE estudiante_id = ?::uuid""").param(MATEO).update();
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matricula.seccion").value("1.° A"))
				.andExpect(jsonPath("$.matricula.confirmadoEn").isNotEmpty());
	}

	@DisplayName("La deuda de Sofía bloquea su propia matrícula")
	@Test
	void laDeudaDeSofiaBloqueaSuPropiaMatricula() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", SOFIA), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deuda.total").value(greaterThanOrEqualTo(600.0)))
				.andExpect(jsonPath("$.deuda.cuotas[0].estudiante").value("Sofía Paredes Rojas"))
				.andExpect(jsonPath("$.deuda.cuotas[0].concepto").value("Pensión de septiembre"))
				.andExpect(jsonPath("$.impedimentos[*].tipo", hasItem("DEUDA")))
				.andExpect(jsonPath("$.puedeContinuar").value(false));
	}

	@DisplayName("No puede ver un estudiante que no está a su cargo")
	@Test
	void noPuedeVerUnEstudianteQueNoEstaASuCargo() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", VALERIA), token(APODERADO)))
				.andExpect(status().isNotFound());
	}

	@DisplayName("Otro rol no puede usar la vista del apoderado")
	@Test
	void otroRolNoPuedeUsarLaVistaDelApoderado() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes"), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isForbidden());
	}
}
