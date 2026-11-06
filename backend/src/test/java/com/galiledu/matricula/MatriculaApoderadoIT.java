package com.galiledu.matricula;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

	@Test
	void listaLosHijosDelApoderado() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes"), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].nombre",
						containsInAnyOrder("Mateo Alonso Paredes Rojas", "Sofía Paredes Rojas")));
	}

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

	@Test
	void conLaDocumentacionValidadaElApoderadoVeLaCuotaDeMatricula() throws Exception {
		jdbc.sql("UPDATE matricula.matricula SET estado = 'PENDIENTE_PAGO' WHERE estudiante_id = ?::uuid")
				.param(MATEO).update();
		jdbc.sql(
				"""
						WITH c AS (
						  INSERT INTO pagos.compromiso_pago (estudiante_id, anio_escolar_id, matricula_id, tarifario_id)
						  SELECT ?::uuid, m.anio_escolar_id, m.id, (SELECT id FROM pagos.tarifario WHERE anio_escolar_id = m.anio_escolar_id)
						  FROM matricula.matricula m WHERE m.estudiante_id = ?::uuid RETURNING id)
						INSERT INTO pagos.cuota (compromiso_id, concepto_codigo, nombre, monto_base, fecha_vencimiento)
						SELECT id, 'MATRICULA', 'Matrícula 2027', 450, '2027-02-28' FROM c""")
				.params(MATEO, MATEO).update();

		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pasoActual").value(3))
				.andExpect(jsonPath("$.cuotaMatricula.concepto").value("Matrícula 2027"))
				.andExpect(jsonPath("$.cuotaMatricula.monto").value(450.0))
				.andExpect(jsonPath("$.cuotaMatricula.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.cuotaMatricula.fechaVencimiento").value("2027-02-28"))
				.andExpect(jsonPath("$.cuotaMatricula.responsable").value("Luis Paredes Silva"));
	}

	@Test
	void sinValidarLosDocumentosNoHayCuotaDeMatricula() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), token(APODERADO)))
				.andExpect(jsonPath("$.cuotaMatricula").doesNotExist());
	}

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
