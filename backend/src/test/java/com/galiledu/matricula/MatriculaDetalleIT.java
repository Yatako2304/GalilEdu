package com.galiledu.matricula;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
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
		return cambiarConCuerpo("{\"seccionId\":\"" + destino + "\",\"motivo\":\"REORGANIZACION\"}");
	}

	private ResultActions cambiarConCuerpo(String json) throws Exception {
		em.clear();
		return mvc.perform(conToken(put("/matricula/matriculas/{id}/seccion", matriculaDeMateo())
				.contentType(MediaType.APPLICATION_JSON).content(json), token(PERSONAL_ADMINISTRATIVO)));
	}

	private ResultActions historial() throws Exception {
		return mvc.perform(conToken(get("/matricula/matriculas/{id}/cambios-seccion", matriculaDeMateo()),
				token(PERSONAL_ADMINISTRATIVO)));
	}

	private ResultActions subir(MockMultipartFile archivo) throws Exception {
		return mvc.perform(conToken(multipart(HttpMethod.PUT, "/matricula/matriculas/{id}/documentos/{d}",
				matriculaDeMateo(), primerDocumento()).file(archivo), token(PERSONAL_ADMINISTRATIVO)));
	}

	@DisplayName("El detalle trae alumno sección paso y documentos")
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
				.andExpect(jsonPath("$.puedeEditarDocumentos").value(true))
				.andExpect(jsonPath("$.puedeValidar").value(false));
	}

	@DisplayName("Una matrícula que no existe es404")
	@Test
	void unaMatriculaQueNoExisteEs404() throws Exception {
		mvc.perform(conToken(get("/matricula/matriculas/{id}", UUID.randomUUID()), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isNotFound());
	}

	@DisplayName("Lista las secciones del grado con su ocupación")
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

	@DisplayName("Cambia de sección libera la vacante y deja el historial")
	@Test
	void cambiaDeSeccionLiberaLaVacanteYDejaElHistorial() throws Exception {
		abrirPeriodo2027();
		cambiar(seccion("B")).andExpect(status().isOk()).andExpect(jsonPath("$.seccion.etiqueta").value("1.° B"));
		Assertions.assertEquals(1, jdbc.sql("""
				SELECT count(*) FROM matricula.cambio_seccion WHERE matricula_id = ? AND seccion_destino_id = ?
				AND motivo = 'Reorganización por cambio de año escolar'""").params(matriculaDeMateo(), seccion("B"))
				.query(Integer.class).single());
		em.clear();
		mvc.perform(conToken(get("/matricula/matriculas/{id}/secciones", matriculaDeMateo()),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(jsonPath("$.secciones[0].ocupadas").value(0))
				.andExpect(jsonPath("$.secciones[1].ocupadas").value(1));
	}

	/** HU-26 (RF-56): el motivo es obligatorio y debe ser uno de los dos conocidos. */
	@DisplayName("El cambio de sección exige un motivo válido")
	@Test
	void elCambioDeSeccionExigeUnMotivoValido() throws Exception {
		abrirPeriodo2027();
		String destino = seccion("B").toString();
		cambiarConCuerpo("{\"seccionId\":\"" + destino + "\"}").andExpect(status().isBadRequest());
		cambiarConCuerpo("{\"seccionId\":\"" + destino + "\",\"motivo\":\"CAPRICHO\"}")
				.andExpect(status().isBadRequest());
		cambiarConCuerpo("{\"seccionId\":\"" + destino + "\",\"motivo\":\"SOLICITUD_APODERADO\",\"observacion\":\""
				+ "x".repeat(301) + "\"}").andExpect(status().isBadRequest());
		Assertions.assertEquals(0, jdbc.sql("SELECT count(*) FROM matricula.cambio_seccion")
				.query(Integer.class).single());
		cambiarConCuerpo("{\"seccionId\":\"" + destino + "\",\"motivo\":\"SOLICITUD_APODERADO\"}")
				.andExpect(status().isOk());
	}

	/** HU-26: el historial muestra origen, destino, motivo, observación y quién lo hizo, el más reciente primero. */
	@DisplayName("El historial lista los cambios del más reciente al más antiguo")
	@Test
	void elHistorialListaLosCambiosDelMasRecienteAlMasAntiguo() throws Exception {
		abrirPeriodo2027();
		historial().andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

		cambiarConCuerpo("{\"seccionId\":\"" + seccion("B") + "\",\"motivo\":\"SOLICITUD_APODERADO\","
				+ "\"observacion\":\"La familia vive cerca del aula\"}").andExpect(status().isOk());
		cambiarConCuerpo("{\"seccionId\":\"" + seccion("A") + "\",\"motivo\":\"REORGANIZACION\"}")
				.andExpect(status().isOk());
		// Los dos cambios caen en el mismo instante de la transacción de prueba: se fuerza el orden por fecha.
		jdbc.sql("""
				UPDATE matricula.cambio_seccion SET realizado_en = now() - interval '1 day'
				WHERE seccion_destino_id = ?""").param(seccion("B")).update();

		historial().andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].origen").value("1.° B"))
				.andExpect(jsonPath("$[0].destino").value("1.° A"))
				.andExpect(jsonPath("$[0].motivo").value("Reorganización por cambio de año escolar"))
				.andExpect(jsonPath("$[0].observacion").doesNotExist())
				.andExpect(jsonPath("$[1].origen").value("1.° A"))
				.andExpect(jsonPath("$[1].destino").value("1.° B"))
				.andExpect(jsonPath("$[1].motivo").value("Solicitud del apoderado"))
				.andExpect(jsonPath("$[1].observacion").value("La familia vive cerca del aula"))
				.andExpect(jsonPath("$[1].realizadoPor").isNotEmpty());
	}

	@DisplayName("El historial solo lo ve el personal administrativo y da no encontrado si no existe")
	@Test
	void elHistorialSoloLoVeElPersonalAdministrativoYDaNoEncontradoSiNoExiste() throws Exception {
		mvc.perform(conToken(get("/matricula/matriculas/{id}/cambios-seccion", matriculaDeMateo()), token(DOCENTE)))
				.andExpect(status().isForbidden());
		mvc.perform(conToken(get("/matricula/matriculas/{id}/cambios-seccion", UUID.randomUUID()),
				token(PERSONAL_ADMINISTRATIVO))).andExpect(status().isNotFound());
	}

	@DisplayName("No cambia a una sección llena ni a la misma ni con el período cerrado")
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

	@DisplayName("Sube y luego ve el documento y al reemplazarlo vuelve a revisión")
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

	@DisplayName("Si el colegio completa todos los documentos la matrícula pasa a validación")
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

	private java.util.List<UUID> documentosDelPeriodo() {
		return jdbc.sql("""
				SELECT d.id FROM matricula.documento_requerido d
				JOIN matricula.periodo_matricula p ON p.id = d.periodo_matricula_id
				WHERE p.anio_escolar_id = '30000000-0000-0000-0000-000000002027' ORDER BY d.orden""")
				.query(UUID.class).list();
	}

	/** El colegio sube todos los documentos de Mateo: la matrícula queda EN_VALIDACION. */
	private void dejarEnValidacion() throws Exception {
		for (UUID documento : documentosDelPeriodo()) {
			mvc.perform(conToken(multipart(HttpMethod.PUT, "/matricula/matriculas/{id}/documentos/{d}",
					matriculaDeMateo(), documento).file(
							new MockMultipartFile("archivo", "f.pdf", "application/pdf",
									new byte[]{1})),
					token(PERSONAL_ADMINISTRATIVO))).andExpect(status().isOk());
		}
	}

	private ResultActions observar(UUID documento, String observacion) throws Exception {
		return mvc.perform(conToken(put("/matricula/matriculas/{id}/documentos/{d}/observacion", matriculaDeMateo(),
				documento).contentType(MediaType.APPLICATION_JSON)
				.content("{\"observacion\":\"" + observacion + "\"}"), token(PERSONAL_ADMINISTRATIVO)));
	}

	/** HU-34: la cuota de matrícula sale del tarifario, que en los datos de prueba aún no está confirmado. */
	private void confirmarTarifario2027() throws Exception {
		mvc.perform(conToken(post("/configuracion/tarifarios/{id}/confirmar", "30000000-0000-0000-0000-000000002027"),
				token(PERSONAL_ADMINISTRATIVO),
				"""
						{"montoCuotaIngreso":500,"montoMatricula":450,"montoPension":620,"diasGracia":5,\n				"recargoPorcentaje":2,"topeRecargoPorcentaje":10}"""))
				.andExpect(status().isOk());
	}

	private ResultActions validar() throws Exception {
		return mvc.perform(conToken(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.post("/matricula/matriculas/{id}/validar", matriculaDeMateo()), token(PERSONAL_ADMINISTRATIVO)));
	}

	@DisplayName("Observar un documento devuelve la matrícula al apoderado y solo ese documento queda observado")
	@Test
	void observarUnDocumentoDevuelveLaMatriculaAlApoderadoYSoloEseDocumentoQuedaObservado() throws Exception {
		dejarEnValidacion();
		mvc.perform(conToken(get("/matricula/matriculas/{id}", matriculaDeMateo()), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(jsonPath("$.estado").value("EN_VALIDACION"))
				.andExpect(jsonPath("$.puedeValidar").value(true));
		observar(documentosDelPeriodo().getFirst(), "La firma no se ve")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SECCION_RESERVADA"))
				.andExpect(jsonPath("$.puedeValidar").value(false))
				.andExpect(jsonPath("$.documentos[0].archivo.estado").value("OBSERVADO"))
				.andExpect(jsonPath("$.documentos[0].archivo.observacion").value("La firma no se ve"))
				.andExpect(jsonPath("$.documentos[1].archivo.estado").value("EN_REVISION"));
	}

	@DisplayName("Al reemplazar el documento observado la matrícula vuelve a validación")
	@Test
	void alReemplazarElDocumentoObservadoLaMatriculaVuelveAValidacion() throws Exception {
		dejarEnValidacion();
		observar(documentosDelPeriodo().getFirst(), "Borroso").andExpect(status().isOk());
		subir(new MockMultipartFile("archivo", "nuevo.pdf", "application/pdf", new byte[]{7}))
				.andExpect(jsonPath("$.estado").value("EN_VALIDACION"))
				.andExpect(jsonPath("$.documentos[0].archivo.estado").value("EN_REVISION"))
				.andExpect(jsonPath("$.puedeValidar").value(true));
	}

	@DisplayName("No se observa sin motivo ni fuera de validación")
	@Test
	void noSeObservaSinMotivoNiFueraDeValidacion() throws Exception {
		observar(primerDocumento(), "Algo").andExpect(status().isConflict());
		dejarEnValidacion();
		observar(primerDocumento(), "  ").andExpect(status().isBadRequest());
	}

	@DisplayName("Validar aprueba los documentos y deja la matrícula pendiente de pago")
	@Test
	void validarApruebaLosDocumentosYDejaLaMatriculaPendienteDePago() throws Exception {
		confirmarTarifario2027();
		dejarEnValidacion();
		validar().andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
				.andExpect(jsonPath("$.pasoActual").value(3))
				.andExpect(jsonPath("$.documentos[0].archivo.estado").value("APROBADO"))
				.andExpect(jsonPath("$.documentos[3].archivo.estado").value("APROBADO"));
		// La cuota de matrícula se genera sola al validar: nadie tiene que acordarse de crearla.
		mvc.perform(conToken(get("/pagos/matriculas/{id}/cuota-matricula", matriculaDeMateo()),
				token(PERSONAL_ADMINISTRATIVO))).andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"));
		validar().andExpect(status().isConflict());
	}

	@DisplayName("No se válida una matrícula con documentos observados o no en validación")
	@Test
	void noSeValidaUnaMatriculaConDocumentosObservadosONoEnValidacion() throws Exception {
		validar().andExpect(status().isConflict());
		dejarEnValidacion();
		jdbc.sql("UPDATE matricula.documento_matricula SET estado = 'OBSERVADO', observacion = 'x' "
				+ "WHERE documento_requerido_id = ?").param(primerDocumento()).update();
		validar().andExpect(status().isConflict());
	}

	@DisplayName("Sin tarifario confirmado no se válida y se explica el motivo")
	@Test
	void sinTarifarioConfirmadoNoSeValidaYSeExplicaElMotivo() throws Exception {
		dejarEnValidacion();
		validar().andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El tarifario 2027 aún no está confirmado."));
		// Nada quedó a medias: la matrícula sigue en validación y puede validarse al confirmar el tarifario.
		mvc.perform(conToken(get("/matricula/matriculas/{id}", matriculaDeMateo()), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(jsonPath("$.estado").value("EN_VALIDACION"));
	}

	@DisplayName("Un documento aprobado no se reemplaza")
	@Test
	void unDocumentoAprobadoNoSeReemplaza() throws Exception {
		confirmarTarifario2027();
		dejarEnValidacion();
		validar().andExpect(status().isOk());
		subir(new MockMultipartFile("archivo", "otro.pdf", "application/pdf", new byte[]{5}))
				.andExpect(status().isConflict());
	}

	@DisplayName("Ver un documento no subido es404 y un archivo inválido es400")
	@Test
	void verUnDocumentoNoSubidoEs404YUnArchivoInvalidoEs400() throws Exception {
		mvc.perform(conToken(get("/matricula/matriculas/{id}/documentos/{d}/archivo", matriculaDeMateo(),
				primerDocumento()), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isNotFound());
		subir(new MockMultipartFile("archivo", "nota.txt", "text/plain", new byte[]{1}))
				.andExpect(status().isBadRequest());
	}

	@DisplayName("La ficha del estudiante ubica su matrícula vigente")
	@Test
	void laFichaDelEstudianteUbicaSuMatriculaVigente() throws Exception {
		UUID estudiante = jdbc.sql("SELECT estudiante_id FROM matricula.matricula WHERE id = ?")
				.param(matriculaDeMateo()).query(UUID.class).single();
		mvc.perform(conToken(get("/matricula/matriculas/de-estudiante/{id}", estudiante),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matriculaId").value(matriculaDeMateo().toString()));
	}

	@DisplayName("Un estudiante sin matrícula devuelve matrícula id nulo")
	@Test
	void unEstudianteSinMatriculaDevuelveMatriculaIdNulo() throws Exception {
		mvc.perform(conToken(get("/matricula/matriculas/de-estudiante/{id}", UUID.randomUUID()),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matriculaId").doesNotExist());
		mvc.perform(conToken(get("/matricula/matriculas/de-estudiante/{id}", UUID.randomUUID()), token("U20260003")))
				.andExpect(status().isForbidden());
	}

	@DisplayName("Solo el personal administrativo lo ve")
	@Test
	void soloElPersonalAdministrativoLoVe() throws Exception {
		mvc.perform(conToken(get("/matricula/matriculas/{id}", matriculaDeMateo()), token("U20260003")))
				.andExpect(status().isForbidden());
	}
}
