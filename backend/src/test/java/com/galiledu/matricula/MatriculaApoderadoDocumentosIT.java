package com.galiledu.matricula;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;

/**
 * HU-28 y HU-29: el apoderado descarga la plantilla, sube el documento firmado y envía la matrícula a validación.
 * Luis Paredes (U20260003) tiene a Mateo con la sección reservada en 2027; el periodo de los datos de prueba ya venció
 * y cada prueba lo abre cuando lo necesita.
 */
class MatriculaApoderadoDocumentosIT extends PruebaIntegracion {
	private static final String APODERADO = "U20260003";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	/** Valeria Sánchez: no es hija de Luis Paredes. */
	private static final String VALERIA = "20000000-0000-0000-0000-000000000003";
	private static final String BASE = "/matricula/apoderado/estudiantes/{id}";

	@Autowired
	private JdbcClient jdbc;

	private void abrirPeriodo2027() {
		jdbc.sql("""
				UPDATE matricula.periodo_matricula SET cierre = now() + interval '30 days'
				WHERE anio_escolar_id = '30000000-0000-0000-0000-000000002027'""").update();
	}

	private List<UUID> documentos() {
		return jdbc.sql("""
				SELECT d.id FROM matricula.documento_requerido d
				JOIN matricula.periodo_matricula p ON p.id = d.periodo_matricula_id
				WHERE p.anio_escolar_id = '30000000-0000-0000-0000-000000002027' ORDER BY d.orden""")
				.query(UUID.class).list();
	}

	private ResultActions subir(String estudiante, UUID documento, String nombre, String tipo, byte[] bytes)
			throws Exception {
		return mvc.perform(conToken(multipart(HttpMethod.PUT, BASE + "/documentos/{d}", estudiante, documento)
				.file(new MockMultipartFile("archivo", nombre, tipo, bytes)), token(APODERADO)));
	}

	private ResultActions subirTodos() throws Exception {
		ResultActions ultimo = null;
		for (UUID d : documentos()) {
			ultimo = subir(MATEO, d, "firmado.pdf", "application/pdf", new byte[]{1, 2, 3}).andExpect(status().isOk());
		}
		return ultimo;
	}

	@Test
	void listaLosDocumentosConSuPlantillaYLoYaSubido() throws Exception {
		abrirPeriodo2027();
		mvc.perform(conToken(get(BASE + "/documentos", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SECCION_RESERVADA"))
				.andExpect(jsonPath("$.puedeSubir").value(true))
				.andExpect(jsonPath("$.documentos", hasSize(4)))
				.andExpect(jsonPath("$.documentos[0].tienePlantilla").value(false))
				.andExpect(jsonPath("$.documentos[0].archivo").doesNotExist());
	}

	@Test
	void conElPeriodoCerradoNoPuedeSubir() throws Exception {
		mvc.perform(conToken(get(BASE + "/documentos", MATEO), token(APODERADO)))
				.andExpect(jsonPath("$.puedeSubir").value(false));
		subir(MATEO, documentos().getFirst(), "firmado.pdf", "application/pdf", new byte[]{1})
				.andExpect(status().isConflict());
	}

	@Test
	void descargaLaPlantillaDelDocumento() throws Exception {
		abrirPeriodo2027();
		UUID documento = documentos().getFirst();
		mvc.perform(conToken(get(BASE + "/documentos/{d}/plantilla", MATEO, documento), token(APODERADO)))
				.andExpect(status().isNotFound());

		jdbc.sql("""
				UPDATE matricula.documento_requerido SET plantilla_nombre_archivo = 'ficha.pdf',
				    plantilla_tipo_mime = 'application/pdf', plantilla_contenido = ? WHERE id = ?""")
				.params(new byte[]{'%', 'P', 'D', 'F'}, documento).update();
		mvc.perform(conToken(get(BASE + "/documentos/{d}/plantilla", MATEO, documento), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/pdf"))
				.andExpect(content().bytes(new byte[]{'%', 'P', 'D', 'F'}));
		mvc.perform(conToken(get(BASE + "/documentos", MATEO), token(APODERADO)))
				.andExpect(jsonPath("$.documentos[0].tienePlantilla").value(true));
	}

	@Test
	void subeYLuegoVeSuDocumentoYRechazaOtrosFormatos() throws Exception {
		abrirPeriodo2027();
		UUID documento = documentos().getFirst();
		subir(MATEO, documento, "firmado.pdf", "application/pdf", new byte[]{1, 2, 3})
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.documentos[0].archivo.nombre").value("firmado.pdf"))
				.andExpect(jsonPath("$.documentos[0].archivo.estado").value("EN_REVISION"));
		mvc.perform(conToken(get(BASE + "/documentos/{d}/archivo", MATEO, documento), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(content().bytes(new byte[]{1, 2, 3}));
		subir(MATEO, documento, "nota.txt", "text/plain", new byte[]{1}).andExpect(status().isBadRequest());
		subir(MATEO, UUID.randomUUID(), "firmado.pdf", "application/pdf", new byte[]{1})
				.andExpect(status().isBadRequest());
	}

	@Test
	void conUnDocumentoObservadoSoloReemplazaEseYLosAprobadosQuedanBloqueados() throws Exception {
		abrirPeriodo2027();
		List<UUID> docs = documentos();
		for (UUID d : docs.subList(0, 3)) {
			subir(MATEO, d, "firmado.pdf", "application/pdf", new byte[]{1}).andExpect(status().isOk());
		}
		jdbc.sql("UPDATE matricula.documento_matricula SET estado = 'APROBADO' WHERE documento_requerido_id = ?")
				.param(docs.get(0)).update();
		jdbc.sql("UPDATE matricula.documento_matricula SET estado = 'OBSERVADO', observacion = 'Borroso' "
				+ "WHERE documento_requerido_id = ?").param(docs.get(1)).update();
		subir(MATEO, docs.get(0), "otro.pdf", "application/pdf", new byte[]{2}).andExpect(status().isConflict());
		subir(MATEO, docs.get(1), "nuevo.pdf", "application/pdf", new byte[]{3})
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.documentos[1].archivo.estado").value("EN_REVISION"))
				.andExpect(jsonPath("$.documentos[1].archivo.observacion").doesNotExist());
	}

	@Test
	void noPuedeEnviarAValidacionSinTodosLosDocumentos() throws Exception {
		abrirPeriodo2027();
		subir(MATEO, documentos().getFirst(), "firmado.pdf", "application/pdf", new byte[]{1})
				.andExpect(status().isOk());
		mvc.perform(conToken(post(BASE + "/enviar-validacion", MATEO), token(APODERADO)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Faltan 3 documento(s) firmado(s) por subir."));
	}

	@Test
	void conTodosLosDocumentosLaEnviaAValidacionYYaNoSePuedeEditar() throws Exception {
		abrirPeriodo2027();
		subirTodos();
		mvc.perform(conToken(post(BASE + "/enviar-validacion", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matricula.estado").value("EN_VALIDACION"))
				.andExpect(jsonPath("$.pasoActual").value(2));
		mvc.perform(conToken(get(BASE + "/documentos", MATEO), token(APODERADO)))
				.andExpect(jsonPath("$.estado").value("EN_VALIDACION"))
				.andExpect(jsonPath("$.puedeSubir").value(false));
		subir(MATEO, documentos().getFirst(), "otro.pdf", "application/pdf", new byte[]{9})
				.andExpect(status().isConflict());
		mvc.perform(conToken(post(BASE + "/enviar-validacion", MATEO), token(APODERADO)))
				.andExpect(status().isConflict());
	}

	@Test
	void noTocaLosDocumentosDeUnEstudianteQueNoEstaASuCargo() throws Exception {
		abrirPeriodo2027();
		mvc.perform(conToken(get(BASE + "/documentos", VALERIA), token(APODERADO))).andExpect(status().isNotFound());
		mvc.perform(conToken(post(BASE + "/enviar-validacion", VALERIA), token(APODERADO)))
				.andExpect(status().isNotFound());
		subir(VALERIA, documentos().getFirst(), "firmado.pdf", "application/pdf", new byte[]{1})
				.andExpect(status().isNotFound());
	}

	@Test
	void soloElApoderadoUsaEstosEndpoints() throws Exception {
		mvc.perform(conToken(get(BASE + "/documentos", MATEO), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isForbidden());
	}
}
