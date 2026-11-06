package com.galiledu.matricula;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;

import com.galiledu.PruebaIntegracion;

/**
 * HU-24 (A3 "Matrículas del periodo"): el listado solo muestra a quienes ya tienen matrícula en el año del periodo.
 * Datos de prueba: 2026 tiene a Sofía matriculada; 2027 tiene a Mateo con la sección reservada en 1.° A.
 */
class MatriculasPeriodoIT extends PruebaIntegracion {
	private static final String MATEO = "Mateo Alonso Paredes Rojas";

	@Autowired
	private JdbcClient jdbc;

	/**
	 * Las pruebas corren en una sola transacción: se vacía su caché para que las secciones cuenten las matrículas
	 * nuevas.
	 */
	@Autowired
	private EntityManager em;

	private UUID periodoDe(int anio) {
		return jdbc.sql("""
				SELECT pm.id FROM matricula.periodo_matricula pm
				JOIN configuracion.anio_escolar a ON a.id = pm.anio_escolar_id WHERE a.anio = ?""")
				.param(anio).query(UUID.class).single();
	}

	@Test
	void elPeriodoTraeSuEstadoYLosIndicadores() throws Exception {
		mvc.perform(conToken(get("/matricula/periodos/{id}", periodoDe(2027)), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(2027))
				.andExpect(jsonPath("$.estado").value(notNullValue()))
				.andExpect(jsonPath("$.cierre").value(notNullValue()))
				.andExpect(jsonPath("$.indicadores.estudiantes").value(1))
				.andExpect(jsonPath("$.indicadores.matriculados").value(0))
				.andExpect(jsonPath("$.indicadores.enProceso").value(1))
				.andExpect(jsonPath("$.indicadores.requierenAccion").value(1));
	}

	@Test
	void elListadoMuestraSoloAQuienesTienenMatricula() throws Exception {
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodoDe(2027)),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(1)))
				.andExpect(jsonPath("$.data[0].nombre").value(MATEO))
				.andExpect(jsonPath("$.data[0].grado").value("1.° de secundaria"))
				.andExpect(jsonPath("$.data[0].seccion").value("1.° A"))
				.andExpect(jsonPath("$.data[0].estado").value("SECCION_RESERVADA"))
				.andExpect(jsonPath("$.meta.total").value(1));
	}

	@Test
	void unEstudianteInscritoSinMatriculaNoApareceNiCuenta() throws Exception {
		// Cualquier estudiante sin fila en matricula.matricula (por ejemplo, el de otro año) no se lista.
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodoDe(2026)),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].nombre", hasItem("Sofía Paredes Rojas")))
				.andExpect(jsonPath("$.data[*].nombre", org.hamcrest.Matchers.not(hasItem(MATEO))))
				.andExpect(jsonPath("$.data[?(@.nombre == 'Sofía Paredes Rojas')].codigo")
						.value(hasItem("MAT-2026-0001")));
	}

	@Test
	void filtraPorTextoGradoYEstado() throws Exception {
		UUID periodo = periodoDe(2027);
		String token = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodo).param("texto", "mateo"), token))
				.andExpect(jsonPath("$.meta.total").value(1));
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodo).param("texto", "zzzz"), token))
				.andExpect(jsonPath("$.meta.total").value(0));
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodo).param("grado", "SEC-1"), token))
				.andExpect(jsonPath("$.meta.total").value(1));
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodo).param("grado", "SEC-5"), token))
				.andExpect(jsonPath("$.meta.total").value(0));
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodo).param("estado", "MATRICULADO"),
				token))
				.andExpect(jsonPath("$.meta.total").value(0));
	}

	@Test
	void ordenaYPaginaConUnaListaCerradaDeColumnas() throws Exception {
		String token = token(PERSONAL_ADMINISTRATIVO);
		UUID periodo = periodoDe(2027);
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodo).param("orden", "grado")
				.param("direccion", "desc").param("size", "1"), token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.size").value(1));
		// Un orden desconocido (o con intento de inyección) cae al orden por estudiante y no rompe.
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodo)
				.param("orden", "nombre; DROP TABLE x"), token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", everyItem(notNullValue())));
	}

	@Test
	void unPeriodoQueNoExisteEs404() throws Exception {
		mvc.perform(conToken(get("/matricula/periodos/{id}", UUID.randomUUID()), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isNotFound());
	}

	// ---------- Nueva matrícula (A6) ----------

	private int contador = 0;

	/** Un estudiante activo inscrito para 2027 en el grado indicado, sin matrícula (se deshace al terminar). */
	private UUID candidato(String gradoCodigo, String nombres) {
		UUID persona = jdbc.sql("""
				INSERT INTO personas.persona (tipo_documento, numero_documento, apellido_paterno, nombres)
				VALUES ('DNI', ?, 'Prueba', ?) RETURNING id""")
				.params("9900%04d".formatted(++contador), nombres).query(UUID.class).single();
		return jdbc.sql("""
				INSERT INTO estudiantes.estudiante (persona_id, anio_academico, grado_codigo)
				VALUES (?, 2027, ?) RETURNING id""").params(persona, gradoCodigo).query(UUID.class).single();
	}

	/** Deja el periodo 2027 abierto ahora (en los datos de prueba su cierre ya pasó). */
	private void abrirPeriodo2027() {
		jdbc.sql("UPDATE matricula.periodo_matricula SET cierre = now() + interval '30 days' WHERE id = ?")
				.param(periodoDe(2027)).update();
	}

	private void limitarCapacidad(String gradoCodigo, int capacidad) {
		jdbc.sql("""
				UPDATE configuracion.seccion SET capacidad_maxima = ?
				WHERE grado_codigo = ? AND anio_escolar_id = '30000000-0000-0000-0000-000000002027'""")
				.params(capacidad, gradoCodigo).update();
	}

	private UUID primerDocumento() {
		return jdbc.sql("SELECT id FROM matricula.documento_requerido WHERE periodo_matricula_id = ? ORDER BY orden")
				.param(periodoDe(2027)).query(UUID.class).list().getFirst();
	}

	private org.springframework.test.web.servlet.ResultActions guardar(UUID estudiante, MockMultipartFile... archivos)
			throws Exception {
		em.clear();
		var peticion = multipart("/matricula/periodos/{id}/matriculas", periodoDe(2027))
				.param("estudianteId", estudiante.toString());
		for (MockMultipartFile archivo : archivos) {
			peticion.file(archivo);
		}
		return mvc.perform(conToken(peticion, token(PERSONAL_ADMINISTRATIVO)));
	}

	@Test
	void elBuscadorMuestraSoloAInscritosSinMatricula() throws Exception {
		candidato("SEC-3", "Camila");
		String token = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(get("/matricula/periodos/{id}/estudiantes", periodoDe(2027)).param("texto", "camila"),
				token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].nombre").value("Camila Prueba"))
				.andExpect(jsonPath("$[0].grado").value("3.° de secundaria"));
		// Mateo ya tiene matrícula en 2027: no se ofrece.
		mvc.perform(conToken(get("/matricula/periodos/{id}/estudiantes", periodoDe(2027)).param("texto", "Mateo"),
				token))
				.andExpect(jsonPath("$", empty()));
	}

	@Test
	void alElegirAlAlumnoMuestraSusDatosLaSeccionYLosDocumentos() throws Exception {
		UUID camila = candidato("SEC-3", "Camila");
		mvc.perform(conToken(get("/matricula/periodos/{id}/estudiantes/{e}/preparacion", periodoDe(2027), camila),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Camila Prueba"))
				.andExpect(jsonPath("$.tipo").value("NUEVO"))
				// HU-24: la primera sección del grado con cupo, en orden alfabético
				.andExpect(jsonPath("$.seccion.etiqueta").value("3.° A"))
				.andExpect(jsonPath("$.documentos", hasSize(4)))
				.andExpect(jsonPath("$.documentos[0].nombre").value("Ficha de matrícula firmada"));
	}

	@Test
	void elPersonalAdministrativoDescargaLaPlantillaDeUnDocumento() throws Exception {
		UUID documento = primerDocumento();
		mvc.perform(conToken(get("/matricula/periodos/{p}/documentos/{d}/plantilla", periodoDe(2027), documento),
				token(PERSONAL_ADMINISTRATIVO))).andExpect(status().isNotFound());

		jdbc.sql("""
				UPDATE matricula.documento_requerido SET plantilla_nombre_archivo = 'ficha.pdf',
				    plantilla_tipo_mime = 'application/pdf', plantilla_contenido = ? WHERE id = ?""")
				.params(new byte[]{'%', 'P', 'D', 'F'}, documento).update();
		mvc.perform(conToken(get("/matricula/periodos/{p}/documentos/{d}/plantilla", periodoDe(2027), documento),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
						.bytes(new byte[]{'%', 'P', 'D', 'F'}));
	}

	@Test
	void conTodosLosDocumentosLaMatriculaNaceEnValidacion() throws Exception {
		abrirPeriodo2027();
		UUID camila = candidato("SEC-3", "Camila");
		var documentos = jdbc.sql(
				"SELECT id FROM matricula.documento_requerido WHERE periodo_matricula_id = ? ORDER BY orden")
				.param(periodoDe(2027)).query(UUID.class).list();
		guardar(camila, documentos.stream()
				.map(d -> new MockMultipartFile(d.toString(), "f.pdf", "application/pdf", new byte[]{1}))
				.toArray(MockMultipartFile[]::new))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("EN_VALIDACION"))
				.andExpect(jsonPath("$.documentosSubidos").value(documentos.size()));
	}

	@Test
	void guardaLaMatriculaConSeccionReservadaYSusDocumentos() throws Exception {
		abrirPeriodo2027();
		UUID camila = candidato("SEC-3", "Camila");
		guardar(camila, new MockMultipartFile(primerDocumento().toString(), "ficha.pdf", "application/pdf",
				new byte[]{1, 2, 3}))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SECCION_RESERVADA"))
				.andExpect(jsonPath("$.seccion").value("3.° A"))
				.andExpect(jsonPath("$.documentosSubidos").value(1));

		org.junit.jupiter.api.Assertions.assertEquals("EN_REVISION", jdbc.sql("""
				SELECT d.estado FROM matricula.documento_matricula d
				JOIN matricula.matricula m ON m.id = d.matricula_id WHERE m.estudiante_id = ?""")
				.param(camila).query(String.class).single());

		String token = token(PERSONAL_ADMINISTRATIVO);
		// Ya aparece en el listado del periodo y deja de ofrecerse en el buscador.
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodoDe(2027)).param("texto", "camila"),
				token))
				.andExpect(jsonPath("$.data[0].nombre").value("Camila Prueba"))
				.andExpect(jsonPath("$.data[0].seccion").value("3.° A"));
		mvc.perform(conToken(get("/matricula/periodos/{id}/estudiantes", periodoDe(2027)).param("texto", "camila"),
				token))
				.andExpect(jsonPath("$", empty()));
	}

	@Test
	void sinDocumentosTambienSeGuarda() throws Exception {
		abrirPeriodo2027();
		guardar(candidato("SEC-2", "Lucía")).andExpect(status().isOk())
				.andExpect(jsonPath("$.documentosSubidos").value(0));
	}

	@Test
	void sinCupoEnLaSeccionLoAsignaALaSiguienteDelGrado() throws Exception {
		abrirPeriodo2027();
		limitarCapacidad("SEC-3", 1);
		guardar(candidato("SEC-3", "Ana")).andExpect(jsonPath("$.seccion").value("3.° A"));
		guardar(candidato("SEC-3", "Beto")).andExpect(jsonPath("$.seccion").value("3.° B"));
		// Ya no queda cupo en A ni en B: no se puede matricular y el modal lo avisa (sin sección).
		UUID carla = candidato("SEC-3", "Carla");
		em.clear();
		guardar(carla).andExpect(status().isConflict());
		mvc.perform(conToken(get("/matricula/periodos/{id}/estudiantes/{e}/preparacion", periodoDe(2027), carla),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(jsonPath("$.seccion").doesNotExist());
	}

	@Test
	void respetaLaSeccionElegidaYSiEstaLlenaLaReasigna() throws Exception {
		abrirPeriodo2027();
		UUID b = jdbc.sql("""
				SELECT id FROM configuracion.seccion WHERE grado_codigo = 'SEC-3' AND nombre = 'B'
				AND anio_escolar_id = '30000000-0000-0000-0000-000000002027'""").query(UUID.class).single();
		UUID ana = candidato("SEC-3", "Ana");
		em.clear();
		mvc.perform(conToken(multipart("/matricula/periodos/{id}/matriculas", periodoDe(2027))
				.param("estudianteId", ana.toString()).param("seccionId", b.toString()),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(jsonPath("$.seccion").value("3.° B"))
				.andExpect(jsonPath("$.seccionReasignada").value(false));
		// Con B llena, la elección se reasigna a la primera con cupo (A).
		limitarCapacidad("SEC-3", 1);
		UUID beto = candidato("SEC-3", "Beto");
		em.clear();
		mvc.perform(conToken(multipart("/matricula/periodos/{id}/matriculas", periodoDe(2027))
				.param("estudianteId", beto.toString()).param("seccionId", b.toString()),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(jsonPath("$.seccion").value("3.° A"))
				.andExpect(jsonPath("$.seccionReasignada").value(true));
		UUID carla = candidato("SEC-3", "Carla");
		em.clear();
		mvc.perform(conToken(get("/matricula/periodos/{id}/estudiantes/{e}/secciones", periodoDe(2027), carla),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.secciones[0].etiqueta").value("3.° A"));
	}

	@Test
	void conElPeriodoCerradoNoSeAsignanSecciones() throws Exception {
		// Los datos de prueba tienen el periodo 2027 ya vencido.
		guardar(candidato("SEC-3", "Camila")).andExpect(status().isConflict());
	}

	@Test
	void noSeMatriculaDosVecesNiAUnEstudianteDeOtroAnio() throws Exception {
		abrirPeriodo2027();
		UUID camila = candidato("SEC-3", "Camila");
		guardar(camila).andExpect(status().isOk());
		guardar(camila).andExpect(status().isConflict());
		jdbc.sql("UPDATE estudiantes.estudiante SET anio_academico = 2026 WHERE id = ?").param(camila).update();
		mvc.perform(conToken(get("/matricula/periodos/{id}/estudiantes/{e}/preparacion", periodoDe(2027), camila),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rechazaArchivosQueNoSonPdfNiImagenOQueNoSonDelPeriodo() throws Exception {
		abrirPeriodo2027();
		UUID camila = candidato("SEC-3", "Camila");
		guardar(camila, new MockMultipartFile(primerDocumento().toString(), "nota.txt", "text/plain",
				new byte[]{1})).andExpect(status().isBadRequest());
		guardar(camila, new MockMultipartFile(UUID.randomUUID().toString(), "ficha.pdf", "application/pdf",
				new byte[]{1})).andExpect(status().isBadRequest());
		// Nada se guardó: sigue sin matrícula.
		mvc.perform(conToken(get("/matricula/periodos/{id}/estudiantes", periodoDe(2027)).param("texto", "camila"),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(jsonPath("$", not(empty())));
	}

	@Test
	void soloElPersonalAdministrativoLoVe() throws Exception {
		UUID periodo = periodoDe(2027);
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodo), token("U20260003")))
				.andExpect(status().isForbidden());
		mvc.perform(get("/matricula/periodos/{id}", periodo)).andExpect(status().isUnauthorized());
	}
}
