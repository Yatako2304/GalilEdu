package com.galiledu.pagos;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.galiledu.PruebaIntegracion;

/**
 * HU-40: estado de deuda del apoderado. Datos de prueba: Luis Paredes (U20260003), padre de Mateo (sin compromiso) y
 * Sofía (compromiso 2026 con septiembre vencida y octubre a diciembre pendientes).
 */
class DeudaApoderadoIT extends PruebaIntegracion {
	private static final String APODERADO = "U20260003";
	private static final String SOFIA = "20000000-0000-0000-0000-000000000002";
	private static final String COMPROMISO_SOFIA = "51000000-0000-0000-0000-000000000001";
	/** Valeria Sánchez: no es hija de Luis Paredes. */
	private static final String VALERIA = "20000000-0000-0000-0000-000000000003";

	@DisplayName("Lista los hijos con su saldo ordenados por apellido")
	@Test
	void listaLosHijosConSuSaldoOrdenadosPorApellido() throws Exception {
		mvc.perform(conToken(get("/pagos/apoderado/estudiantes"), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].nombres").value("Mateo Alonso"))
				.andExpect(jsonPath("$[0].apellidos").value("Paredes Rojas"))
				.andExpect(jsonPath("$[0].saldo").value(0))
				.andExpect(jsonPath("$[1].nombres").value("Sofía"))
				.andExpect(jsonPath("$[1].codigo").value("74125896"))
				.andExpect(jsonPath("$[1].grado").value("4.° de secundaria"))
				.andExpect(jsonPath("$[1].seccion").value("A"))
				.andExpect(jsonPath("$[1].saldo").value(2400.0))
				.andExpect(jsonPath("$[1].cuotasVencidas").value(greaterThanOrEqualTo(1)));
	}

	@DisplayName("Muestra el cronograma con el recargo de la cuota vencida")
	@Test
	void muestraElCronogramaConElRecargoDeLaCuotaVencida() throws Exception {
		mvc.perform(conToken(get("/pagos/apoderado/compromisos/{id}", COMPROMISO_SOFIA), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cuotas", hasSize(11)))
				.andExpect(jsonPath("$.cuotas[?(@.nombre == 'Pensión de septiembre')].vencida").value(true))
				.andExpect(jsonPath("$.cuotas[?(@.nombre == 'Pensión de marzo')].recargo").value(0));
	}

	@DisplayName("Muestra el historial de pagos del más reciente al más antiguo")
	@Test
	void muestraElHistorialDePagosDelMasRecienteAlMasAntiguo() throws Exception {
		mvc.perform(conToken(get("/pagos/apoderado/compromisos/{id}/pagos", COMPROMISO_SOFIA), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(7)))
				.andExpect(jsonPath("$[0].numeroConstancia").value("C-2026-00007"))
				.andExpect(jsonPath("$[0].cuotas[0]").value("Pensión de agosto"))
				.andExpect(jsonPath("$[6].numeroConstancia").value("C-2026-00001"))
				.andExpect(jsonPath("$[6].monto").value(420.0));
	}

	@DisplayName("Lista los compromisos de su hija")
	@Test
	void listaLosCompromisosDeSuHija() throws Exception {
		mvc.perform(conToken(get("/pagos/apoderado/estudiantes/{id}/compromisos", SOFIA), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].anio").value(2026));
	}

	@DisplayName("No ve la deuda de un estudiante ajeno")
	@Test
	void noVeLaDeudaDeUnEstudianteAjeno() throws Exception {
		String apoderado = token(APODERADO);
		mvc.perform(conToken(get("/pagos/apoderado/estudiantes/{id}/compromisos", VALERIA), apoderado))
				.andExpect(status().isNotFound());
	}

	@DisplayName("Un compromiso inexistente devuelve404")
	@Test
	void unCompromisoInexistenteDevuelve404() throws Exception {
		mvc.perform(conToken(get("/pagos/apoderado/compromisos/{id}", VALERIA), token(APODERADO)))
				.andExpect(status().isNotFound());
	}

	@DisplayName("El personal usa su propia vista")
	@Test
	void elPersonalUsaSuPropiaVista() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(get("/pagos/apoderado/estudiantes"), personal))
				.andExpect(status().isForbidden());
		mvc.perform(conToken(get("/pagos/compromisos/{id}/pagos", COMPROMISO_SOFIA), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(7)));
	}
}
