package com.galiledu.pagos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.galiledu.PruebaIntegracion;

/** HU-37: compromiso de pago y cronograma de cuotas. Datos de prueba: compromiso 2026 de Sofía Paredes. */
class CompromisoPagoIT extends PruebaIntegracion {
	private static final String SOFIA = "20000000-0000-0000-0000-000000000002";
	private static final String COMPROMISO_SOFIA = "51000000-0000-0000-0000-000000000001";
	private static final String APODERADO = "U20260003";
	/** Valeria Sánchez: matriculada en 5.° A de 2026 (confirmada el 10/02/2026) y sin compromiso de pago. */
	private static final String VALERIA = "20000000-0000-0000-0000-000000000003";
	private static final String MATRICULA_VALERIA = "50000000-0000-0000-0000-000000000003";
	/** Mateo Paredes: solo tiene la sección reservada para 2027. */
	private static final String MATRICULA_MATEO = "50000000-0000-0000-0000-000000000002";

	@Test
	void listaLosCompromisosDelEstudiante() throws Exception {
		mvc.perform(conToken(get("/pagos/estudiantes/{id}/compromisos", SOFIA), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].anio").value(2026))
				.andExpect(jsonPath("$[0].estado").value("VIGENTE"));
	}

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

	@Test
	void compromisoInexistenteDevuelve404() throws Exception {
		mvc.perform(conToken(get("/pagos/compromisos/{id}", "00000000-0000-0000-0000-000000000000"),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isNotFound());
	}

	@Test
	void otroRolNoPuedeVerElCompromiso() throws Exception {
		mvc.perform(conToken(get("/pagos/compromisos/{id}", COMPROMISO_SOFIA), token(APODERADO)))
				.andExpect(status().isForbidden());
	}

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

	@Test
	void noGeneraCronogramaSiLaMatriculaNoEstaFormalizada() throws Exception {
		mvc.perform(conToken(post("/pagos/matriculas/{id}/cronograma", MATRICULA_MATEO),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isConflict());
	}

	@Test
	void elRetiroAnulaSoloLasPensionesQueVencenDespues() throws Exception {
		// Retiro el 15/10/2026: se anulan octubre, noviembre y diciembre; septiembre (vencida) se mantiene.
		mvc.perform(conToken(post("/pagos/compromisos/{id}/retiro", COMPROMISO_SOFIA), token(PERSONAL_ADMINISTRATIVO),
				"""
						{"fechaRetiro":"2026-10-15"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cuotasAnuladas").value(3));
	}

	@Test
	void otroRolNoPuedeGenerarElCronograma() throws Exception {
		mvc.perform(conToken(post("/pagos/matriculas/{id}/cronograma", MATRICULA_VALERIA), token(APODERADO)))
				.andExpect(status().isForbidden());
	}
}
