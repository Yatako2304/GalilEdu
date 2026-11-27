package com.galiledu.pagos;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/** HU-37: compromiso de pago y cronograma de cuotas. Datos de prueba: compromiso 2026 de Sofía Paredes. */
class CompromisoPagoIT extends PruebaIntegracion {
	@Autowired
	private JdbcClient jdbc;
	private static final String SOFIA = "20000000-0000-0000-0000-000000000002";
	private static final String COMPROMISO_SOFIA = "51000000-0000-0000-0000-000000000001";
	private static final String APODERADO = "U20260003";
	/** Valeria Sánchez: matriculada en 5.° A de 2026 (confirmada el 10/02/2026) y sin compromiso de pago. */
	private static final String VALERIA = "20000000-0000-0000-0000-000000000003";
	private static final String MATRICULA_VALERIA = "50000000-0000-0000-0000-000000000003";
	/** Mateo Paredes: solo tiene la sección reservada para 2027. */
	private static final String MATRICULA_MATEO = "50000000-0000-0000-0000-000000000002";

	@DisplayName("Lista los compromisos del estudiante")
	@Test
	void listaLosCompromisosDelEstudiante() throws Exception {
		mvc.perform(conToken(get("/pagos/estudiantes/{id}/compromisos", SOFIA), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].anio").value(2026))
				.andExpect(jsonPath("$[0].estado").value("VIGENTE"));
	}

	@DisplayName("Muestra el cronograma con resumen y cuota vencida")
	@Test
	void muestraElCronogramaConResumenYCuotaVencida() throws Exception {
		mvc.perform(conToken(get("/pagos/compromisos/{id}", COMPROMISO_SOFIA), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(2026))
				.andExpect(jsonPath("$.estudiante.nombre").value("Sofía Paredes Rojas"))
				.andExpect(jsonPath("$.estudiante.seccion").value("4.° A"))
				// matrícula + 10 pensiones
				.andExpect(jsonPath("$.cuotas", hasSize(11)))
				.andExpect(jsonPath("$.resumen.totalAnual").value(6420.0))
				.andExpect(jsonPath("$.resumen.capitalPagado").value(4020.0))
				.andExpect(jsonPath("$.resumen.saldoPendiente").value(2400.0))
				.andExpect(jsonPath("$.resumen.pensionesPagadas").value(6))
				.andExpect(jsonPath("$.resumen.pensionesTotal").value(10))
				.andExpect(jsonPath("$.resumen.pensionMensual").value(600.0))
				// la pensión 7 (septiembre) está impaga y vencida
				.andExpect(jsonPath("$.cuotas[?(@.numero == 7)].vencida").value(true));
	}

	@DisplayName("Compromiso inexistente devuelve404")
	@Test
	void compromisoInexistenteDevuelve404() throws Exception {
		mvc.perform(conToken(get("/pagos/compromisos/{id}", "00000000-0000-0000-0000-000000000000"),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isNotFound());
	}

	@DisplayName("Otro rol no puede ver el compromiso")
	@Test
	void otroRolNoPuedeVerElCompromiso() throws Exception {
		mvc.perform(conToken(get("/pagos/compromisos/{id}", COMPROMISO_SOFIA), token(APODERADO)))
				.andExpect(status().isForbidden());
	}

	@DisplayName("Genera las diez pensiones al matricular y no las duplica")
	@Test
	void generaLasDiezPensionesAlMatricularYNoLasDuplica() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(post("/pagos/matriculas/{id}/cronograma", MATRICULA_VALERIA), personal))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.anio").value(2026))
				.andExpect(jsonPath("$.cuotasGeneradas").value(10));

		mvc.perform(conToken(get("/pagos/estudiantes/{id}/compromisos", VALERIA), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		// RF-83: un reintento no crea otro cronograma
		mvc.perform(conToken(post("/pagos/matriculas/{id}/cronograma", MATRICULA_VALERIA), personal))
				.andExpect(status().isConflict());
	}

	@DisplayName("No genera cronograma si la matrícula no está formalizada")
	@Test
	void noGeneraCronogramaSiLaMatriculaNoEstaFormalizada() throws Exception {
		mvc.perform(conToken(post("/pagos/matriculas/{id}/cronograma", MATRICULA_MATEO),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isConflict());
	}

	@DisplayName("El retiro anula solo las pensiones que vencen después")
	@Test
	void elRetiroAnulaSoloLasPensionesQueVencenDespues() throws Exception {
		// Retiro el 15/10/2026: se anulan octubre, noviembre y diciembre; septiembre (vencida) se mantiene.
		mvc.perform(conToken(post("/pagos/compromisos/{id}/retiro", COMPROMISO_SOFIA), token(PERSONAL_ADMINISTRATIVO),
				"""
						{"fechaRetiro":"2026-10-15"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cuotasAnuladas").value(3));
	}

	@DisplayName("Otro rol no puede generar el cronograma")
	@Test
	void otroRolNoPuedeGenerarElCronograma() throws Exception {
		mvc.perform(conToken(post("/pagos/matriculas/{id}/cronograma", MATRICULA_VALERIA), token(APODERADO)))
				.andExpect(status().isForbidden());
	}

	@DisplayName("Estado de cuenta del año lista los compromisos con su deuda")
	@Test
	void estadoDeCuentaDelAnioListaLosCompromisosConSuDeuda() throws Exception {
		mvc.perform(conToken(get("/pagos/estado-cuenta").param("anio", "2026"), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(2026))
				.andExpect(jsonPath("$.alumnos[?(@.nombre == 'Sofía Paredes Rojas')].seccion").value("4.° A"))
				.andExpect(jsonPath("$.alumnos[?(@.nombre == 'Sofía Paredes Rojas')].antiguedad").value(1))
				.andExpect(jsonPath("$.alumnos[?(@.nombre == 'Sofía Paredes Rojas')].capitalPagado").value(4020.0))
				.andExpect(jsonPath("$.deudaVencida").value(greaterThanOrEqualTo(600.0)))
				.andExpect(jsonPath("$.alumnosConDeuda").value(greaterThanOrEqualTo(1)));
	}

	@DisplayName("Sin año el estado de cuenta lista a todos los alumnos con su compromiso más reciente")
	@Test
	void sinAnioElEstadoDeCuentaListaATodosLosAlumnosConSuCompromisoMasReciente() throws Exception {
		int activos = jdbc.sql("SELECT count(*) FROM estudiantes.estudiante WHERE NOT eliminado")
				.query(Integer.class).single();
		mvc.perform(conToken(get("/pagos/estado-cuenta"), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").doesNotExist())
				.andExpect(jsonPath("$.alumnos", hasSize(activos)))
				.andExpect(jsonPath("$.alumnos[?(@.nombre == 'Sofía Paredes Rojas')].compromisoId")
						.value(COMPROMISO_SOFIA))
				.andExpect(jsonPath("$.alumnos[?(@.nombre == 'Sofía Paredes Rojas')].anioCompromiso").value(2026))
				// Valeria está matriculada pero sin compromiso: también sale, con montos en cero.
				.andExpect(jsonPath("$.alumnos[?(@.estudianteId == '%s')].compromisoId".formatted(VALERIA))
						.value(contains(nullValue())))
				.andExpect(jsonPath("$.alumnos[?(@.compromisoId == null)].totalAnual").value(everyItem(is(0))));
	}

	@DisplayName("El estado de cuenta muestra a los retirados por si dejaron deuda")
	@Test
	void elEstadoDeCuentaMuestraALosRetiradosPorSiDejaronDeuda() throws Exception {
		jdbc.sql("UPDATE estudiantes.estudiante SET estado = 'RETIRADO' WHERE id = ?::uuid").param(SOFIA).update();
		mvc.perform(conToken(get("/pagos/estado-cuenta"), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.alumnos[?(@.nombre == 'Sofía Paredes Rojas')].retirado").value(contains(true)));
	}

	@DisplayName("El estado de cuenta ordena por año del compromiso y nombre, y deja al final a los que no tienen")
	@Test
	void elEstadoDeCuentaOrdenaPorAnioYNombreYDejaAlFinalALosQueNoTienen() throws Exception {
		String json = mvc.perform(conToken(get("/pagos/estado-cuenta"), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		java.util.List<java.util.Map<String, Object>> alumnos = com.jayway.jsonpath.JsonPath.read(json, "$.alumnos");
		java.util.Comparator<java.util.Map<String, Object>> orden = java.util.Comparator
				.comparing((java.util.Map<String, Object> a) -> (Integer) a.get("anioCompromiso"),
						java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
		org.assertj.core.api.Assertions.assertThat(alumnos)
				.isSortedAccordingTo(orden)
				.extracting(a -> a.get("anioCompromiso")).contains(2026);
	}

	@DisplayName("La morosidad muestra la pensión vencida con su recargo separado")
	@Test
	void laMorosidadMuestraLaPensionVencidaConSuRecargoSeparado() throws Exception {
		mvc.perform(
				conToken(get("/pagos/compromisos/{id}/morosidad", COMPROMISO_SOFIA), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(2026))
				.andExpect(jsonPath("$.diasGracia").value(5))
				.andExpect(jsonPath("$.recargoPorcentaje").value(2.0))
				.andExpect(jsonPath("$.topeRecargoPorcentaje").value(10.0))
				// la más antigua primero: septiembre (S/ 600), vencida desde el 30/09/2026
				.andExpect(jsonPath("$.cuotas[0].concepto").value("Pensión de septiembre"))
				.andExpect(jsonPath("$.cuotas[0].fechaVencimiento").value("2026-09-30"))
				.andExpect(jsonPath("$.cuotas[0].capital").value(600.0))
				.andExpect(jsonPath("$.cuotas[0].diasAtraso").value(greaterThan(5)))
				// RF-92: hay recargo y nunca pasa del tope (10 % de 600)
				.andExpect(jsonPath("$.cuotas[0].recargo").value(greaterThan(0.0)))
				.andExpect(jsonPath("$.cuotas[0].recargo").value(lessThanOrEqualTo(60.0)))
				.andExpect(jsonPath("$.capital").value(greaterThanOrEqualTo(600.0)))
				.andExpect(jsonPath("$.recargo").value(greaterThan(0.0)));
	}

	@DisplayName("Morosidad de un compromiso inexistente devuelve404")
	@Test
	void morosidadDeUnCompromisoInexistenteDevuelve404() throws Exception {
		mvc.perform(conToken(get("/pagos/compromisos/{id}/morosidad", "99999999-0000-0000-0000-000000000000"),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isNotFound());
	}
}
