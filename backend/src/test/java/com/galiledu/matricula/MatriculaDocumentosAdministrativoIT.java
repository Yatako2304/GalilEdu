package com.galiledu.matricula;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;

/** HU-72 · El Personal administrativo elimina un documento del expediente. Mateo tiene 1.° A reservada en 2027. */
class MatriculaDocumentosAdministrativoIT extends PruebaIntegracion {
	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private EntityManager em;

	private UUID matriculaDeMateo() {
		return jdbc.sql("""
				SELECT id FROM matricula.matricula
				WHERE estudiante_id = '20000000-0000-0000-0000-000000000001'
				  AND anio_escolar_id = '30000000-0000-0000-0000-000000002027'""").query(UUID.class).single();
	}

	private UUID primerDocumento() {
		return jdbc.sql("""
				SELECT d.id FROM matricula.documento_requerido d
				JOIN matricula.periodo_matricula p ON p.id = d.periodo_matricula_id
				WHERE p.anio_escolar_id = '30000000-0000-0000-0000-000000002027' ORDER BY d.orden""")
				.query(UUID.class).list().getFirst();
	}

	private void subir() throws Exception {
		mvc.perform(conToken(multipart(HttpMethod.PUT, "/matricula/matriculas/{id}/documentos/{d}",
				matriculaDeMateo(), primerDocumento())
				.file(new MockMultipartFile("archivo", "ficha.pdf", "application/pdf", new byte[]{1, 2})),
				token(PERSONAL_ADMINISTRATIVO))).andExpect(status().isOk());
	}

	private ResultActions eliminar() throws Exception {
		em.clear();
		return mvc.perform(conToken(delete("/matricula/matriculas/{id}/documentos/{d}", matriculaDeMateo(),
				primerDocumento()), token(PERSONAL_ADMINISTRATIVO)));
	}

	private void estado(String estado) {
		jdbc.sql("UPDATE matricula.matricula SET estado = ? WHERE id = ?").params(estado, matriculaDeMateo())
				.update();
	}

	@DisplayName("Elimina el documento subido")
	@Test
	void eliminaElDocumentoSubido() throws Exception {
		subir();
		eliminar()
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SECCION_RESERVADA"))
				.andExpect(jsonPath("$.documentos[0].archivo").doesNotExist());
	}

	@DisplayName("Un documento que no se subio es404")
	@Test
	void unDocumentoQueNoSeSubioEs404() throws Exception {
		eliminar().andExpect(status().isNotFound());
	}

	@DisplayName("En validación la matrícula vuelve a esperar documentos")
	@Test
	void enValidacionLaMatriculaVuelveAEsperarDocumentos() throws Exception {
		subir();
		estado("EN_VALIDACION");
		eliminar()
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SECCION_RESERVADA"));
	}

	@DisplayName("Un documento aprobado no se elimina")
	@Test
	void unDocumentoAprobadoNoSeElimina() throws Exception {
		subir();
		jdbc.sql("UPDATE matricula.documento_matricula SET estado = 'APROBADO' WHERE matricula_id = ?")
				.param(matriculaDeMateo()).update();
		eliminar()
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ese documento ya fue aprobado y no se puede eliminar."));
	}

	@DisplayName("Con la matrícula pendiente de pago no se elimina")
	@Test
	void conLaMatriculaPendienteDePagoNoSeElimina() throws Exception {
		subir();
		estado("PENDIENTE_PAGO");
		eliminar().andExpect(status().isConflict());
	}

	@DisplayName("El apoderado no usa este endpoint")
	@Test
	void elApoderadoNoUsaEsteEndpoint() throws Exception {
		mvc.perform(conToken(delete("/matricula/matriculas/{id}/documentos/{d}", matriculaDeMateo(),
				primerDocumento()), token("U20260003")))
				.andExpect(status().isForbidden());
	}
}
