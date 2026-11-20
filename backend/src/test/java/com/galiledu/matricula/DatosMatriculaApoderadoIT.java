package com.galiledu.matricula;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * HU-32 · Figma "HU-32 Confirmación de matrícula" › "Datos del estudiante y apoderado" en la pantalla del apoderado.
 * Datos de prueba: Luis Paredes (U20260003), responsable de Mateo (sección reservada en 1.° A de 2027).
 */
class DatosMatriculaApoderadoIT extends PruebaIntegracion {
	private static final String APODERADO = "U20260003";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";

	@Autowired
	private JdbcClient jdbc;

	@Test
	void muestraLosDatosDelEstudianteYDelApoderado() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.datos.documento").value("78945612"))
				.andExpect(jsonPath("$.datos.apoderado").value("Luis Paredes Silva"))
				.andExpect(jsonPath("$.datos.contacto").isNotEmpty())
				.andExpect(jsonPath("$.datos.informacionMedica").isNotEmpty())
				// La sección es resultado de la matrícula formalizada (corrección del PO)
				.andExpect(jsonPath("$.datos.seccion").doesNotExist())
				.andExpect(jsonPath("$.datos.tutor").doesNotExist());
	}

	@Test
	void conLaMatriculaConfirmadaMuestraLaSeccion() throws Exception {
		jdbc.sql("""
				UPDATE matricula.matricula SET estado = 'MATRICULADO', codigo = 'MAT-2027-9001', confirmado_en = now()
				WHERE estudiante_id = ?::uuid""").param(MATEO).update();
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.datos.seccion").value("1.° A"));
	}
}
