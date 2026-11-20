package com.galiledu.pagos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * Corrección del PO: el descuento por hermanos beneficia al hermano menor. Datos de prueba: Sofía (2011) está
 * matriculada en 2026; aquí se matricula también a su hermano Mateo (2014) en 2026, con su compromiso. Con 2 hermanos
 * el tramo es 10 %.
 */
class DescuentoHermanoMenorIT extends PruebaIntegracion {
	private static final String COMPROMISO_SOFIA = "51000000-0000-0000-0000-000000000001";
	private static final String COMPROMISO_MATEO = "59000000-0000-0000-0000-000000000002";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	private static final String SOFIA = "20000000-0000-0000-0000-000000000002";
	private static final String HERMANOS = "52000000-0000-0000-0000-000000000001";
	private static final String BECA = "52000000-0000-0000-0000-000000000002";

	@Autowired
	private JdbcClient jdbc;

	@BeforeEach
	void matricularAMateoEn2026() {
		jdbc.sql("""
				INSERT INTO matricula.matricula (id, estudiante_id, anio_escolar_id, grado_codigo, seccion_id,
				    tipo_estudiante, estado, codigo, extemporanea, confirmado_en, creado_en, actualizado_en)
				SELECT '59000000-0000-0000-0000-000000000001', ?::uuid, m.anio_escolar_id, 'SEC-1', s.id, 'NUEVO',
				       'MATRICULADO', 'MAT-2026-9001', FALSE, now(), now(), now()
				FROM matricula.matricula m
				JOIN configuracion.seccion s ON s.anio_escolar_id = m.anio_escolar_id AND s.grado_codigo = 'SEC-1'
				WHERE m.id = '50000000-0000-0000-0000-000000000001'
				ORDER BY s.nombre LIMIT 1""").param(MATEO).update();
		jdbc.sql("""
				INSERT INTO pagos.compromiso_pago (id, estudiante_id, anio_escolar_id, matricula_id, tarifario_id,
				    estado, creado_en, actualizado_en)
				SELECT ?::uuid, ?::uuid, c.anio_escolar_id, '59000000-0000-0000-0000-000000000001', c.tarifario_id,
				       'VIGENTE', now(), now()
				FROM pagos.compromiso_pago c WHERE c.id = ?::uuid""")
				.params(COMPROMISO_MATEO, MATEO, COMPROMISO_SOFIA).update();
	}

	private static String aplicar(String descuentoId) {
		return """
				{"descuentoId":"%s","motivo":"Hermanos en el colegio","vigenciaDesde":"2026-03-01",
				 "reemplazar":false}""".formatted(descuentoId);
	}

	@Test
	void alHermanoMayorNoLeCorrespondeElDescuentoPorHermanos() throws Exception {
		mvc.perform(conToken(get("/pagos/compromisos/{id}/descuentos-aplicables", COMPROMISO_SOFIA),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hermanosMatriculados").value(2))
				.andExpect(jsonPath("$.descuentos[?(@.tipo == 'POR_HERMANOS')].aplicable").value(false))
				.andExpect(jsonPath("$.descuentos[?(@.tipo == 'POR_HERMANOS')].porcentaje").value(10.0))
				.andExpect(jsonPath("$.descuentos[?(@.tipo == 'POR_HERMANOS')].cantidadHermanosMatriculados")
						.value(2))
				// El filtro [?(...)] devuelve una lista: se busca el texto dentro de ella
				.andExpect(jsonPath("$.descuentos[?(@.tipo == 'POR_HERMANOS')].motivo",
						hasItem(containsString("hermano menor: Mateo Alonso Paredes Rojas"))))
				.andExpect(jsonPath("$.descuentos[?(@.tipo == 'POR_HERMANOS')].beneficiarios[0].estudianteId")
						.value(MATEO))
				// Los descuentos de porcentaje único no dependen de los hermanos
				.andExpect(jsonPath("$.descuentos[?(@.nombre == 'Beca institucional')].aplicable").value(true));
	}

	@Test
	void alHermanoMenorSiLeCorresponde() throws Exception {
		mvc.perform(conToken(get("/pagos/compromisos/{id}/descuentos-aplicables", COMPROMISO_MATEO),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.descuentos[?(@.tipo == 'POR_HERMANOS')].aplicable").value(true))
				.andExpect(jsonPath("$.descuentos[?(@.tipo == 'POR_HERMANOS')].porcentaje").value(10.0));
	}

	@Test
	void elBackendRechazaAplicarloAlHermanoQueNoCorresponde() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA), personal,
				aplicar(HERMANOS)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message", containsString("hermano menor")));
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_MATEO), personal,
				aplicar(HERMANOS)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.descuento.porcentaje").value(10.0));
		// Un descuento de porcentaje único se le sigue pudiendo aplicar al mayor
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA), personal, aplicar(BECA)))
				.andExpect(status().isCreated());
	}

	@Test
	void siLosMenoresSonGemelosAmbosLoReciben() throws Exception {
		jdbc.sql("""
				UPDATE personas.persona SET fecha_nacimiento = (
				    SELECT p.fecha_nacimiento FROM personas.persona p
				    JOIN estudiantes.estudiante e ON e.persona_id = p.id WHERE e.id = ?::uuid)
				WHERE id = (SELECT persona_id FROM estudiantes.estudiante WHERE id = ?::uuid)""")
				.params(MATEO, SOFIA).update();
		mvc.perform(conToken(get("/pagos/compromisos/{id}/descuentos-aplicables", COMPROMISO_SOFIA),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.descuentos[?(@.tipo == 'POR_HERMANOS')].aplicable").value(true))
				.andExpect(jsonPath("$.descuentos[?(@.tipo == 'POR_HERMANOS')].beneficiarios[*].estudianteId",
						hasSize(2)));
		mvc.perform(conToken(post("/pagos/compromisos/{id}/descuentos", COMPROMISO_SOFIA),
				token(PERSONAL_ADMINISTRATIVO), aplicar(HERMANOS)))
				.andExpect(status().isCreated());
	}
}
