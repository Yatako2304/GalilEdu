package com.galiledu.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.galiledu.PruebaIntegracion;

/**
 * El apoderado inicia la matrícula sin esperar al colegio (Figma › Apoderado › HU-28: el paso 1 «Datos y documentos»
 * con la sección «Se asigna al confirmar la matrícula»). La matrícula se crea recién al «Enviar a validación» con todos
 * los documentos y llega así a la bandeja del Personal administrativo. Cada prueba quita la matrícula de Mateo (en los
 * datos de prueba la creó el colegio) y abre el periodo 2027.
 */
class MatriculaIniciadaPorApoderadoIT extends PruebaIntegracion {
	private static final String APODERADO = "U20260003";
	private static final String USUARIO_APODERADO = "10000000-0000-0000-0000-000000000003";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	/** Sofía está inscrita para 2026, no para el periodo 2027. */
	private static final String SOFIA = "20000000-0000-0000-0000-000000000002";
	private static final String VALERIA = "20000000-0000-0000-0000-000000000003";
	private static final String BASE = "/matricula/apoderado/estudiantes/{id}";
	private static final String PERIODO_2027 = """
			(SELECT id FROM matricula.periodo_matricula
			 WHERE anio_escolar_id = '30000000-0000-0000-0000-000000002027')""";

	@Autowired
	private JdbcClient jdbc;

	@BeforeEach
	void mateoSinMatriculaYPeriodoAbierto() {
		jdbc.sql("DELETE FROM matricula.matricula WHERE id = '50000000-0000-0000-0000-000000000002'").update();
		jdbc.sql("UPDATE matricula.periodo_matricula SET estado = 'HABILITADO', cierre = now() + interval '30 days' "
				+ "WHERE id = " + PERIODO_2027).update();
	}

	private List<UUID> documentos() {
		return jdbc.sql("SELECT id FROM matricula.documento_requerido WHERE periodo_matricula_id = " + PERIODO_2027
				+ " ORDER BY orden").query(UUID.class).list();
	}

	private ResultActions enviar(String estudiante, List<UUID> docs) throws Exception {
		MockMultipartHttpServletRequestBuilder pedido = multipart(BASE + "/matricula", estudiante);
		for (UUID d : docs) {
			pedido.file(new MockMultipartFile(d.toString(), "firmado.pdf", "application/pdf", new byte[]{1, 2}));
		}
		return mvc.perform(conToken(pedido, token(APODERADO)));
	}

	private int matriculasDeMateo() {
		return jdbc.sql("SELECT count(*) FROM matricula.matricula WHERE estudiante_id = ?::uuid").param(MATEO)
				.query(Integer.class).single();
	}

	@Test
	void sinMatriculaPuedeEmpezarEnElPaso1() throws Exception {
		mvc.perform(conToken(get(BASE + "/estado", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matricula").doesNotExist())
				.andExpect(jsonPath("$.pasoActual").value(1))
				.andExpect(jsonPath("$.puedeContinuar").value(true))
				.andExpect(jsonPath("$.impedimentos", hasSize(0)));
		mvc.perform(conToken(get(BASE + "/documentos", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").doesNotExist())
				.andExpect(jsonPath("$.puedeSubir").value(true))
				.andExpect(jsonPath("$.documentos", hasSize(4)))
				.andExpect(jsonPath("$.documentos[0].archivo").doesNotExist());
	}

	@Test
	void alEnviarTodosLosDocumentosSeCreaLaMatriculaEnValidacion() throws Exception {
		enviar(MATEO, documentos())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matricula.estado").value("EN_VALIDACION"))
				.andExpect(jsonPath("$.matricula.documentosEntregados").value(4))
				// La sección la asigna el sistema, pero el apoderado no la ve hasta que la matrícula se confirme.
				.andExpect(jsonPath("$.matricula.seccion").doesNotExist())
				.andExpect(jsonPath("$.pasoActual").value(2));

		var creada = jdbc.sql("""
				SELECT s.nombre, m.tipo_estudiante, m.creado_por::text
				FROM matricula.matricula m JOIN configuracion.seccion s ON s.id = m.seccion_id
				WHERE m.estudiante_id = ?::uuid""").param(MATEO)
				.query((rs, i) -> List.of(rs.getString(1), rs.getString(2), rs.getString(3))).single();
		// Primera sección de 1.° de secundaria con cupo, igual que «Nueva matrícula».
		assertThat(creada.get(0)).isEqualTo("A");
		assertThat(creada.get(1)).isEqualTo("NUEVO");
		assertThat(creada.get(2)).isEqualTo(USUARIO_APODERADO);

		// Llega a la bandeja del Personal administrativo, lista para validar.
		mvc.perform(conToken(get("/matricula/periodos/{p}/matriculas", periodo2027()).param("texto", "Mateo"),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].estado", hasItem("EN_VALIDACION")));
	}

	@Test
	void sinTodosLosDocumentosNoSeCreaNada() throws Exception {
		enviar(MATEO, documentos().subList(0, 3))
				.andExpect(status().isBadRequest())
				.andExpect(
						jsonPath("$.message").value("Sube los 4 documentos firmados antes de enviarlos a validación."));
		assertThat(matriculasDeMateo()).isZero();
	}

	@Test
	void noSeIniciaDosVeces() throws Exception {
		enviar(MATEO, documentos()).andExpect(status().isOk());
		enviar(MATEO, documentos()).andExpect(status().isConflict());
		assertThat(matriculasDeMateo()).isEqualTo(1);
	}

	@Test
	void sinMatriculaNoSeSubeUnoPorUnoNiSeEnviaPorSeparado() throws Exception {
		mvc.perform(conToken(post(BASE + "/enviar-validacion", MATEO), token(APODERADO)))
				.andExpect(status().isConflict());
		mvc.perform(conToken(multipart(HttpMethod.PUT, BASE + "/documentos/{d}", MATEO,
				documentos().getFirst())
				.file(new MockMultipartFile("archivo", "f.pdf", "application/pdf", new byte[]{1})),
				token(APODERADO)))
				.andExpect(status().isConflict());
		assertThat(matriculasDeMateo()).isZero();
	}

	@Test
	void conElPeriodoCerradoNoPuedeIniciar() throws Exception {
		jdbc.sql("UPDATE matricula.periodo_matricula SET cierre = now() - interval '1 day' WHERE id = " + PERIODO_2027)
				.update();
		mvc.perform(conToken(get(BASE + "/estado", MATEO), token(APODERADO)))
				.andExpect(jsonPath("$.puedeContinuar").value(false))
				.andExpect(jsonPath("$.pasoActual").value(0));
		mvc.perform(conToken(get(BASE + "/documentos", MATEO), token(APODERADO)))
				.andExpect(jsonPath("$.puedeSubir").value(false));
		enviar(MATEO, documentos()).andExpect(status().isConflict());
		assertThat(matriculasDeMateo()).isZero();
	}

	@Test
	void unHijoInscritoParaOtroAnioNoPuedeIniciar() throws Exception {
		mvc.perform(conToken(get(BASE + "/estado", SOFIA), token(APODERADO)))
				.andExpect(jsonPath("$.impedimentos[*].tipo", hasItem("INSCRIPCION")))
				.andExpect(jsonPath("$.puedeContinuar").value(false));
		enviar(SOFIA, documentos()).andExpect(status().isConflict());
	}

	@Test
	void noIniciaLaMatriculaDeUnEstudianteQueNoEstaASuCargo() throws Exception {
		enviar(VALERIA, documentos()).andExpect(status().isNotFound());
	}

	private UUID periodo2027() {
		return jdbc.sql("SELECT " + PERIODO_2027.strip()).query(UUID.class).single();
	}
}
