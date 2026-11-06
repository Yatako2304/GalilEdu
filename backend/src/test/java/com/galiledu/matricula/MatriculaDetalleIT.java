package com.galiledu.matricula;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;

/** HU-24 (A5 "Detalle de matrícula"): detalle, cambio de sección y documentos. Mateo tiene 1.° A reservada en 2027. */
class MatriculaDetalleIT extends PruebaIntegracion {
	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private EntityManager em;

	private UUID matriculaDeMateo() {
		return jdbc.sql("""
				SELECT m.id FROM matricula.matricula m
				JOIN configuracion.anio_escolar a ON a.id = m.anio_escolar_id
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				WHERE a.anio = 2027 AND p.nombres = 'Mateo Alonso'""")
				.query(UUID.class).single();
	}

	private void abrirPeriodo2027() {
		jdbc.sql("""
				UPDATE matricula.periodo_matricula SET cierre = now() + interval '30 days'
				WHERE anio_escolar_id = '30000000-0000-0000-0000-000000002027'""").update();
	}

	private UUID seccion(String nombre) {
		return jdbc.sql("""
				SELECT id FROM configuracion.seccion
				WHERE anio_escolar_id = '30000000-0000-0000-0000-000000002027' AND grado_codigo = 'SEC-1'
				  AND nombre = ?""").param(nombre).query(UUID.class).single();
	}

	private UUID primerDocumento() {
		return jdbc.sql("""
				SELECT d.id FROM matricula.documento_requerido d
				JOIN matricula.periodo_matricula p ON p.id = d.periodo_matricula_id
				WHERE p.anio_escolar_id = '30000000-0000-0000-0000-000000002027' ORDER BY d.orden""")
				.query(UUID.class).list().getFirst();
	}

	private ResultActions cambiar(UUID destino) throws Exception {
		em.clear();
		return mvc.perform(conToken(put("/matricula/matriculas/{id}/seccion", matriculaDeMateo())
				.contentType(MediaType.APPLICATION_JSON).content("{\"seccionId\":\"" + destino + "\"}"),
				token(PERSONAL_ADMINISTRATIVO)));
	}

	private ResultActions subir(MockMultipartFile archivo) throws Exception {
		return mvc.perform(conToken(multipart(HttpMethod.PUT, "/matricula/matriculas/{id}/documentos/{d}",
				matriculaDeMateo(), primerDocumento()).file(archivo), token(PERSONAL_ADMINISTRATIVO)));
	}

	@Test
	void elDetalleTraeAlumnoSeccionPasoYDocumentos() throws Exception {
		mvc.perform(conToken(get("/matricula/matriculas/{id}", matriculaDeMateo()), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Mateo Alonso Paredes Rojas"))
				.andExpect(jsonPath("$.estado").value("SECCION_RESERVADA"))
				.andExpect(jsonPath("$.pasoActual").value(2))
				.andExpect(jsonPath("$.seccion.etiqueta").value("1.° A"))
				.andExpect(jsonPath("$.periodoId").value(notNullValue()))
				.andExpect(jsonPath("$.documentos", hasSize(4)))
				.andExpect(jsonPath("$.documentos[0].archivo").doesNotExist())
				// Los datos de prueba tienen el periodo ya vencido: solo se cambia sección a los matriculados.
				.andExpect(jsonPath("$.puedeCambiarSeccion").value(false))
				.andExpect(jsonPath("$.puedeEditarDocumentos").value(true));
	}

	@Test
	void unaMatriculaQueNoExisteEs404() throws Exception {
		mvc.perform(conToken(get("/matricula/matriculas/{id}", UUID.randomUUID()), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isNotFound());
	}

	@Test
	void listaLasSeccionesDelGradoConSuOcupacion() throws Exception {
		abrirPeriodo2027();
		mvc.perform(conToken(get("/matricula/matriculas/{id}/secciones", matriculaDeMateo()),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.grado").value("1.° de secundaria"))
				.andExpect(jsonPath("$.seleccionadaId").value(seccion("A").toString()))
				.andExpect(jsonPath("$.secciones[0].etiqueta").value("1.° A"))
				.andExpect(jsonPath("$.secciones[0].ocupadas").value(1));
	}

	@Test
	void cambiaDeSeccionLiberaLaVacanteYDejaElHistorial() throws Exception {
		abrirPeriodo2027();
		cambiar(seccion("B")).andExpect(status().isOk()).andExpect(jsonPath("$.seccion.etiqueta").value("1.° B"));
		Assertions.assertEquals(1, jdbc.sql("""
				SELECT count(*) FROM matricula.cambio_seccion WHERE matricula_id = ? AND seccion_destino_id = ?
				AND motivo IS NULL""").params(matriculaDeMateo(), seccion("B")).query(Integer.class).single());
		em.clear();
		mvc.perform(conToken(get("/matricula/matriculas/{id}/secciones", matriculaDeMateo()),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(jsonPath("$.secciones[0].ocupadas").value(0))
				.andExpect(jsonPath("$.secciones[1].ocupadas").value(1));
	}

	@Test
	void noCambiaAUnaSeccionLlenaNiALaMismaNiConElPeriodoCerrado() throws Exception {
		cambiar(seccion("B")).andExpect(status().isConflict());
		abrirPeriodo2027();
		cambiar(seccion("A")).andExpect(status().isBadRequest());
		jdbc.sql("UPDATE configuracion.seccion SET capacidad_maxima = 1 WHERE id = ?").param(seccion("B")).update();
		jdbc.sql("""
				INSERT INTO personas.persona (tipo_documento, numero_documento, apellido_paterno, nombres)
				VALUES ('DNI', '99001234', 'Prueba', 'Otro')""").update();
		jdbc.sql("""
				INSERT INTO estudiantes.estudiante (persona_id, anio_academico, grado_codigo)
				SELECT id, 2027, 'SEC-1' FROM personas.persona WHERE numero_documento = '99001234'""").update();
		jdbc.sql("""
				INSERT INTO matricula.matricula (estudiante_id, anio_escolar_id, grado_codigo, seccion_id,
				                                 tipo_estudiante, estado)
				SELECT e.id, '30000000-0000-0000-0000-000000002027', 'SEC-1', ?, 'NUEVO', 'SECCION_RESERVADA'
				FROM estudiantes.estudiante e JOIN personas.persona p ON p.id = e.persona_id
				WHERE p.numero_documento = '99001234'""").param(seccion("B")).update();
		cambiar(seccion("B")).andExpect(status().isConflict());
	}

	@Test
	void subeYLuegoVeElDocumentoYAlReemplazarloVuelveARevision() throws Exception {
		subir(new MockMultipartFile("archivo", "ficha.pdf", "application/pdf", new byte[]{1, 2, 3}))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.documentos[0].archivo.nombre").value("ficha.pdf"))
				.andExpect(jsonPath("$.documentos[0].archivo.estado").value("EN_REVISION"));
		mvc.perform(conToken(get("/matricula/matriculas/{id}/documentos/{d}/archivo", matriculaDeMateo(),
				primerDocumento()), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/pdf"))
				.andExpect(content().bytes(new byte[]{1, 2, 3}));
		jdbc.sql("UPDATE matricula.documento_matricula SET estado = 'OBSERVADO', observacion = 'Borroso'").update();
		subir(new MockMultipartFile("archivo", "ficha2.png", "image/png", new byte[]{9}))
				.andExpect(jsonPath("$.documentos[0].archivo.estado").value("EN_REVISION"))
				.andExpect(jsonPath("$.documentos[0].archivo.observacion").doesNotExist());
	}

	@Test
	void siElColegioCompletaTodosLosDocumentosLaMatriculaPasaAValidacion() throws Exception {
		UUID matricula = matriculaDeMateo();
		var documentos = jdbc.sql("""
				SELECT d.id FROM matricula.documento_requerido d
				JOIN matricula.periodo_matricula p ON p.id = d.periodo_matricula_id
				WHERE p.anio_escolar_id = '30000000-0000-0000-0000-000000002027' ORDER BY d.orden""")
				.query(UUID.class).list();
		for (UUID documento : documentos.subList(0, documentos.size() - 1)) {
			mvc.perform(conToken(multipart(HttpMethod.PUT, "/matricula/matriculas/{id}/documentos/{d}", matricula,
					documento).file(new MockMultipartFile("archivo", "f.pdf", "application/pdf", new byte[]{1})),
					token(PERSONAL_ADMINISTRATIVO))).andExpect(jsonPath("$.estado").value("SECCION_RESERVADA"));
		}
		mvc.perform(conToken(multipart(HttpMethod.PUT, "/matricula/matriculas/{id}/documentos/{d}", matricula,
				documentos.getLast()).file(new MockMultipartFile("archivo", "f.pdf", "application/pdf", new byte[]{1})),
				token(PERSONAL_ADMINISTRATIVO))).andExpect(jsonPath("$.estado").value("EN_VALIDACION"));
	}

	@Test
	void verUnDocumentoNoSubidoEs404YUnArchivoInvalidoEs400() throws Exception {
		mvc.perform(conToken(get("/matricula/matriculas/{id}/documentos/{d}/archivo", matriculaDeMateo(),
				primerDocumento()), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isNotFound());
		subir(new MockMultipartFile("archivo", "nota.txt", "text/plain", new byte[]{1}))
				.andExpect(status().isBadRequest());
	}

	@Test
	void soloElPersonalAdministrativoLoVe() throws Exception {
		mvc.perform(conToken(get("/matricula/matriculas/{id}", matriculaDeMateo()), token("U20260003")))
				.andExpect(status().isForbidden());
	}
}
