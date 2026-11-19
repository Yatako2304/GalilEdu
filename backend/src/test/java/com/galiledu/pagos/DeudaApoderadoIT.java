package com.galiledu.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;

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

	@Autowired
	private JdbcClient jdbc;

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

	@Test
	void muestraElCronogramaConElRecargoDeLaCuotaVencida() throws Exception {
		mvc.perform(conToken(get("/pagos/apoderado/compromisos/{id}", COMPROMISO_SOFIA), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cuotas", hasSize(11)))
				.andExpect(jsonPath("$.cuotas[?(@.nombre == 'Pensión de septiembre')].vencida").value(true))
				.andExpect(jsonPath("$.cuotas[?(@.nombre == 'Pensión de marzo')].recargo").value(0));
	}

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

	@Test
	void listaLosCompromisosDeSuHija() throws Exception {
		mvc.perform(conToken(get("/pagos/apoderado/estudiantes/{id}/compromisos", SOFIA), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].anio").value(2026));
	}

	@Test
	void noVeLaDeudaDeUnEstudianteAjeno() throws Exception {
		String apoderado = token(APODERADO);
		mvc.perform(conToken(get("/pagos/apoderado/estudiantes/{id}/compromisos", VALERIA), apoderado))
				.andExpect(status().isNotFound());
	}

	@Test
	void unCompromisoInexistenteDevuelve404() throws Exception {
		mvc.perform(conToken(get("/pagos/apoderado/compromisos/{id}", VALERIA), token(APODERADO)))
				.andExpect(status().isNotFound());
	}

	/** HU-42 · RF-107/108: el apoderado descarga la constancia de un pago de su hija en PDF. */
	@Test
	void descargaLaConstanciaDeUnPagoEnPdf() throws Exception {
		byte[] archivo = mvc.perform(conToken(get("/pagos/apoderado/pagos/{id}/constancia", pago("C-2026-00007")),
				token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						containsString("constancia-C-2026-00007.pdf")))
				.andReturn().getResponse().getContentAsByteArray();
		try (PDDocument pdf = Loader.loadPDF(archivo)) {
			assertThat(new PDFTextStripper().getText(pdf)).contains("Constancia de pago", "N.° C-2026-00007",
					"Sofía Paredes Rojas", "DNI 74125896", "4.° de secundaria A", "Pensión de agosto",
					"Total pagado", "S/ 600.00");
		}
	}

	@Test
	void noDescargaLaConstanciaDeOtraFamilia() throws Exception {
		jdbc.sql("""
				INSERT INTO pagos.pago (id, estudiante_id, numero_constancia, fecha_pago, monto_total)
				VALUES ('99000000-0000-0000-0000-000000000001', ?::uuid, 'C-2026-99999', now(), 10)""")
				.param(VALERIA).update();
		mvc.perform(conToken(get("/pagos/apoderado/pagos/{id}/constancia", "99000000-0000-0000-0000-000000000001"),
				token(APODERADO)))
				.andExpect(status().isNotFound());
	}

	@Test
	void unPagoInexistenteDevuelve404() throws Exception {
		mvc.perform(conToken(get("/pagos/apoderado/pagos/{id}/constancia", VALERIA), token(APODERADO)))
				.andExpect(status().isNotFound());
	}

	@Test
	void elPersonalNoDescargaConstanciasDesdeLaVistaDelApoderado() throws Exception {
		mvc.perform(conToken(get("/pagos/apoderado/pagos/{id}/constancia", pago("C-2026-00007")),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isForbidden());
	}

	private String pago(String constancia) {
		return jdbc.sql("SELECT id::text FROM pagos.pago WHERE numero_constancia = ?").param(constancia)
				.query(String.class).single();
	}

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
