package com.galiledu.matricula;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * Corrección del PO: la sección es resultado de la matrícula formalizada. Mientras la matrícula está en proceso la
 * sección elegida es solo una selección ({@code seccionSeleccionada}); la oficial ({@code seccionAsignada}) aparece
 * con la matrícula MATRICULADO. Datos de prueba: Mateo con sección reservada en 1.° A de 2027 y Sofía matriculada en
 * 4.° A de 2026.
 */
class SeccionAsignadaMatriculaIT extends PruebaIntegracion {
	private static final String APODERADO = "U20260003";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	private static final String MATRICULA_MATEO = "50000000-0000-0000-0000-000000000002";
	private static final String MATRICULA_SOFIA_2026 = "50000000-0000-0000-0000-000000000001";

	@Autowired
	private JdbcClient jdbc;

	private UUID periodoDe(int anio) {
		return jdbc.sql("""
				SELECT pm.id FROM matricula.periodo_matricula pm
				JOIN configuracion.anio_escolar a ON a.id = pm.anio_escolar_id WHERE a.anio = ?""")
				.param(anio).query(UUID.class).single();
	}

	@Test
	void enProcesoLaSeccionEsSoloSeleccionadaEnElDetalle() throws Exception {
		mvc.perform(conToken(get("/matricula/matriculas/{id}", MATRICULA_MATEO), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SECCION_RESERVADA"))
				.andExpect(jsonPath("$.seccionAsignada").doesNotExist())
				.andExpect(jsonPath("$.seccionSeleccionada.nombre").value("A"))
				.andExpect(jsonPath("$.seccionSeleccionada.etiqueta").value("1.° A"))
				.andExpect(jsonPath("$.seccionSeleccionada.id").isNotEmpty());
	}

	@Test
	void conLaMatriculaFormalizadaLaSeccionQuedaAsignada() throws Exception {
		mvc.perform(conToken(get("/matricula/matriculas/{id}", MATRICULA_SOFIA_2026),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("MATRICULADO"))
				.andExpect(jsonPath("$.seccionAsignada.nombre").value("A"))
				.andExpect(jsonPath("$.seccionAsignada.etiqueta").value("4.° A"))
				.andExpect(jsonPath("$.seccionSeleccionada").doesNotExist());
	}

	@Test
	void unaMatriculaAnuladaNoQuedaRelacionadaConLaSeccion() throws Exception {
		jdbc.sql("UPDATE matricula.matricula SET estado = 'ANULADA' WHERE id = ?::uuid").param(MATRICULA_MATEO)
				.update();
		mvc.perform(conToken(get("/matricula/matriculas/{id}", MATRICULA_MATEO), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.seccionAsignada").doesNotExist())
				.andExpect(jsonPath("$.seccionSeleccionada").doesNotExist());
	}

	@Test
	void laTablaDelPeriodoSeparaAsignadaYSeleccionada() throws Exception {
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodoDe(2027)),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].seccion").value("1.° A"))
				.andExpect(jsonPath("$.data[0].seccionAsignada").doesNotExist())
				.andExpect(jsonPath("$.data[0].seccionSeleccionada.etiqueta").value("1.° A"));
	}

	@Test
	void elApoderadoVeLaSeccionPorAsignarHastaQueSeFormalice() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matricula.seccion").doesNotExist())
				.andExpect(jsonPath("$.matricula.seccionAsignada").doesNotExist())
				.andExpect(jsonPath("$.matricula.seccionSeleccionada.etiqueta").value("1.° A"));

		jdbc.sql("""
				UPDATE matricula.matricula SET estado = 'MATRICULADO', codigo = 'MAT-2027-9001', confirmado_en = now()
				WHERE id = ?::uuid""").param(MATRICULA_MATEO).update();
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matricula.seccion").value("1.° A"))
				.andExpect(jsonPath("$.matricula.seccionAsignada.nombre").value("A"))
				.andExpect(jsonPath("$.matricula.seccionSeleccionada").doesNotExist());
	}
}
