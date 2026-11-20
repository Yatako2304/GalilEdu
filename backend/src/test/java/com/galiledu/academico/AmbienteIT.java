package com.galiledu.academico;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * HU-43, Figma CI-26 (857:10215): API de ambientes físicos con los datos de prueba (A-101 en uso por 1.° A, A-205
 * inactiva, AUD-01 de 180). Todo corre en la transacción de la prueba, que se deshace.
 * <p>
 * Los {@code POST /ambientes} no están aquí: {@code crear} confirma de verdad (una transacción por intento) y el
 * rollback de la prueba no lo deshace. Están en {@link AmbienteCrearIT}, que limpia lo que crea.
 * <p>
 * Los rechazos de la API se comprueban con {@code status()} y el mensaje; {@code rechazaPor} se usa solo para los de
 * la base de datos, que es la segunda barrera si algo llegara a pasar la API.
 */
class AmbienteIT extends PruebaIntegracion {
	@Autowired
	private JdbcClient jdbc;

	private String personal;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	private UUID idDe(String codigo) {
		return jdbc.sql("SELECT id FROM configuracion.ambiente WHERE codigo = :codigo").param("codigo", codigo)
				.query(UUID.class).single();
	}

	private static String edicion(String nombre, Object aforo, String recursosJson) {
		return """
				{"nombre":"%s","aforoMaximo":%s,"recursos":%s}""".formatted(nombre, aforo, recursosJson);
	}

	/** Matriculados de 1.° A que usan A-101 en los datos de prueba (los que ocupan vacante). */
	private int matriculadosDeA101() {
		return jdbc.sql("""
				SELECT count(*)::int FROM matricula.matricula m
				JOIN configuracion.seccion s ON s.id = m.seccion_id
				JOIN configuracion.ambiente a ON a.id = s.ambiente_id
				WHERE a.codigo = 'A-101' AND m.estado NOT IN ('RETIRADO', 'ANULADA')""").query(Integer.class).single();
	}

	// ---------- Permisos ----------

	@DisplayName("Sin sesión da401")
	@Test
	void sinSesionDa401() throws Exception {
		UUID id = idDe("A-101");
		mvc.perform(get("/ambientes/resumen")).andExpect(status().isUnauthorized());
		mvc.perform(get("/ambientes")).andExpect(status().isUnauthorized());
		mvc.perform(put("/ambientes/" + id).contentType("application/json").content(edicion("Aula", 30, "[]")))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/ambientes/" + id + "/desactivar")).andExpect(status().isUnauthorized());
		mvc.perform(post("/ambientes/" + id + "/reactivar")).andExpect(status().isUnauthorized());
	}

	@DisplayName("Otros roles reciben403")
	@Test
	void otrosRolesReciben403() throws Exception {
		UUID id = idDe("A-101");
		for (String usuario : new String[]{ADMIN, DOCENTE, COORDINADOR}) {
			String token = token(usuario);
			mvc.perform(conToken(get("/ambientes/resumen"), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(get("/ambientes"), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(put("/ambientes/" + id), token, edicion("Aula", 30, "[]")))
					.andExpect(status().isForbidden());
			mvc.perform(conToken(post("/ambientes/" + id + "/desactivar"), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(post("/ambientes/" + id + "/reactivar"), token)).andExpect(status().isForbidden());
		}
	}

	// ---------- Resumen (tarjetas) ----------

	@DisplayName("El resumen trae4 tarjetas con solo los activos y el total de registrados")
	@Test
	void elResumenTrae4TarjetasConSoloLosActivosYElTotalDeRegistrados() throws Exception {
		var esperado = jdbc.sql("""
				SELECT count(*) FILTER (WHERE estado = 'ACTIVO') AS activos, count(*) AS total,
				       count(*) FILTER (WHERE estado = 'ACTIVO' AND tipo = 'AULA_REGULAR') AS aulas,
				       count(*) FILTER (WHERE estado = 'ACTIVO' AND tipo = 'ESPACIO_ESPECIAL') AS espacios,
				       coalesce(sum(aforo_maximo) FILTER (WHERE estado = 'ACTIVO'), 0) AS aforo
				FROM configuracion.ambiente""").query().singleRow();

		mvc.perform(conToken(get("/ambientes/resumen"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activos").value(((Number) esperado.get("activos")).longValue()))
				.andExpect(jsonPath("$.total").value(((Number) esperado.get("total")).longValue()))
				.andExpect(jsonPath("$.aulasRegulares").value(((Number) esperado.get("aulas")).longValue()))
				.andExpect(jsonPath("$.espaciosEspeciales").value(((Number) esperado.get("espacios")).longValue()))
				.andExpect(jsonPath("$.aforoTotal").value(((Number) esperado.get("aforo")).longValue()));
	}

	@DisplayName("El resumen no cambia con los filtros de la lista")
	@Test
	void elResumenNoCambiaConLosFiltrosDeLaLista() throws Exception {
		String sinFiltros = mvc.perform(conToken(get("/ambientes/resumen"), personal)).andReturn().getResponse()
				.getContentAsString();

		mvc.perform(
				conToken(get("/ambientes").param("tipo", "AULA_REGULAR").param("estado", "INACTIVO").param("q", "x"),
						personal))
				.andExpect(status().isOk());

		mvc.perform(conToken(get("/ambientes/resumen"), personal)).andExpect(status().isOk())
				.andExpect(content().json(sinFiltros, true));
	}

	// ---------- Listado y filtros ----------

	@DisplayName("Sin filtros lista todos con su código generado de solo lectura")
	@Test
	void sinFiltrosListaTodosConSuCodigoGeneradoDeSoloLectura() throws Exception {
		mvc.perform(conToken(get("/ambientes"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].nombre", hasItem("Aula 101")))
				.andExpect(jsonPath("$[*].nombre", hasItem("Auditorio")))
				.andExpect(jsonPath("$[?(@.codigo=='A-101')].nombre", contains("Aula 101")))
				.andExpect(jsonPath("$[?(@.codigo=='A-101')].tipo", contains("AULA_REGULAR")))
				.andExpect(jsonPath("$[?(@.codigo=='A-101')].aforoMaximo", contains(30)))
				.andExpect(jsonPath("$[?(@.codigo=='A-101')].recursos",
						contains(contains("Pizarra acrílica", "Proyector"))))
				.andExpect(jsonPath("$[?(@.codigo=='A-101')].estado", contains("ACTIVO")))
				.andExpect(jsonPath("$[?(@.codigo=='A-101')].id").isNotEmpty());
	}

	@DisplayName("Filtra por tipo")
	@Test
	void filtraPorTipo() throws Exception {
		mvc.perform(conToken(get("/ambientes").param("tipo", "AULA_REGULAR"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].tipo", everyItem(equalTo("AULA_REGULAR"))))
				.andExpect(jsonPath("$[*].nombre", hasItem("Aula 101")))
				.andExpect(jsonPath("$[*].nombre", not(hasItem("Auditorio"))));
		mvc.perform(conToken(get("/ambientes").param("tipo", "ESPACIO_ESPECIAL"), personal))
				.andExpect(jsonPath("$[*].tipo", everyItem(equalTo("ESPACIO_ESPECIAL"))))
				.andExpect(jsonPath("$[*].nombre", hasItem("Auditorio")));
	}

	@DisplayName("Filtra por estado")
	@Test
	void filtraPorEstado() throws Exception {
		mvc.perform(conToken(get("/ambientes").param("estado", "INACTIVO"), personal))
				.andExpect(jsonPath("$[*].estado", everyItem(equalTo("INACTIVO"))))
				.andExpect(jsonPath("$[*].nombre", hasItem("Aula 205")))
				.andExpect(jsonPath("$[*].nombre", not(hasItem("Aula 101"))));
		mvc.perform(conToken(get("/ambientes").param("estado", "ACTIVO"), personal))
				.andExpect(jsonPath("$[*].estado", everyItem(equalTo("ACTIVO"))))
				.andExpect(jsonPath("$[*].nombre", not(hasItem("Aula 205"))));
	}

	@DisplayName("La búsqueda es por nombre sin distinguir mayúsculas y se combina con los otros filtros")
	@Test
	void laBusquedaEsPorNombreSinDistinguirMayusculasYSeCombinaConLosOtrosFiltros() throws Exception {
		mvc.perform(conToken(get("/ambientes").param("q", "  AULA   10 "), personal))
				.andExpect(jsonPath("$[*].nombre", contains("Aula 101", "Aula 102")));
		mvc.perform(
				conToken(get("/ambientes").param("q", "aula").param("tipo", "AULA_REGULAR").param("estado", "INACTIVO"),
						personal))
				.andExpect(jsonPath("$[*].nombre", contains("Aula 205")));
		mvc.perform(conToken(get("/ambientes").param("q", "aula").param("tipo", "ESPACIO_ESPECIAL"), personal))
				.andExpect(jsonPath("$.length()").value(0));
		// No busca por código ni por recursos.
		mvc.perform(conToken(get("/ambientes").param("q", "A-101"), personal))
				.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(conToken(get("/ambientes").param("q", "proyector"), personal))
				.andExpect(jsonPath("$.length()").value(0));
	}

	@DisplayName("Un tipo o un estado desconocido da400")
	@Test
	void unTipoOUnEstadoDesconocidoDa400() throws Exception {
		mvc.perform(conToken(get("/ambientes").param("tipo", "AULA"), personal))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La solicitud tiene un formato inválido"));
		mvc.perform(conToken(get("/ambientes").param("estado", "BORRADO"), personal))
				.andExpect(status().isBadRequest());
	}

	// ---------- Editar ----------

	@DisplayName("Edita nombre aforo y recursos y no cambia el código ni el tipo")
	@Test
	void editaNombreAforoYRecursosYNoCambiaElCodigoNiElTipo() throws Exception {
		UUID id = idDe("A-101");

		mvc.perform(conToken(put("/ambientes/" + id), personal,
				"""
						{"nombre":"  Aula   101 renovada ","aforoMaximo":40,"recursos":[" Pizarra digital ","","Proyector","proyector"],
						 "codigo":"A-999","tipo":"ESPACIO_ESPECIAL","estado":"INACTIVO"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.nombre").value("Aula 101 renovada"))
				.andExpect(jsonPath("$.aforoMaximo").value(40))
				.andExpect(jsonPath("$.recursos", contains("Pizarra digital", "Proyector")))
				// El código, el tipo y el estado que vengan en el cuerpo se ignoran.
				.andExpect(jsonPath("$.codigo").value("A-101"))
				.andExpect(jsonPath("$.tipo").value("AULA_REGULAR"))
				.andExpect(jsonPath("$.estado").value("ACTIVO"));

		// El listado de la misma transacción ya lo ve cambiado.
		mvc.perform(conToken(get("/ambientes").param("q", "renovada"), personal))
				.andExpect(jsonPath("$[0].codigo").value("A-101"))
				.andExpect(jsonPath("$[0].aforoMaximo").value(40));
	}

	@DisplayName("Un espacio especial puede superar45")
	@Test
	void unEspacioEspecialPuedeSuperar45() throws Exception {
		mvc.perform(conToken(put("/ambientes/" + idDe("AUD-01")), personal, edicion("Auditorio", 300, "[\"Sonido\"]")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aforoMaximo").value(300));
	}

	@DisplayName("Editar con nombre de otro ambiente da409 sin distinguir mayúsculas ni espacios")
	@Test
	void editarConNombreDeOtroAmbienteDa409SinDistinguirMayusculasNiEspacios() throws Exception {
		UUID id = idDe("A-101");

		mvc.perform(conToken(put("/ambientes/" + id), personal, edicion("  aula   102 ", 30, "[]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ya existe un ambiente llamado \"aula 102\"."));
		// También el de un ambiente inactivo (A-205): se puede reactivar.
		mvc.perform(conToken(put("/ambientes/" + id), personal, edicion("AULA 205", 30, "[]")))
				.andExpect(status().isConflict());
		// Y su propio nombre, aunque cambie la capitalización, sí se puede.
		mvc.perform(conToken(put("/ambientes/" + id), personal, edicion("AULA 101", 30, "[]")))
				.andExpect(status().isOk());
	}

	@DisplayName("Un aula de46 da400 con el texto de figma")
	@Test
	void unAulaDe46Da400ConElTextoDeFigma() throws Exception {
		mvc.perform(conToken(put("/ambientes/" + idDe("A-101")), personal, edicion("Aula 101", 46, "[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Un aula regular admite como máximo 45."));
	}

	@DisplayName("Los datos invalidos dan400 con su mensaje")
	@Test
	void losDatosInvalidosDan400ConSuMensaje() throws Exception {
		String url = "/ambientes/" + idDe("A-101");

		mvc.perform(conToken(put(url), personal, """
				{"nombre":"   ","aforoMaximo":30,"recursos":[]}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa el nombre del ambiente."))
				.andExpect(jsonPath("$.errores.nombre").value("Ingresa el nombre del ambiente."));
		mvc.perform(conToken(put(url), personal, """
				{"nombre":"Aula","recursos":[]}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.aforoMaximo").value("Ingresa el aforo máximo."));
		mvc.perform(conToken(put(url), personal, edicion("Aula", 0, "[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.aforoMaximo").value("El aforo debe ser mayor que 0."));
		mvc.perform(conToken(put(url), personal, edicion("Aula", -5, "[]")))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(put(url), personal, edicion("Aula", 30, "[\"" + "r".repeat(61) + "\"]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Cada recurso admite como máximo 60 caracteres."));
		mvc.perform(conToken(put(url), personal, edicion("a".repeat(81), 30, "[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El nombre admite como máximo 80 caracteres."));
		mvc.perform(conToken(put(url), personal, "{no es json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La solicitud tiene un formato inválido"));
	}

	@DisplayName("Un dato inválido no deja nada cambiado")
	@Test
	void unDatoInvalidoNoDejaNadaCambiado() throws Exception {
		UUID id = idDe("A-101");

		mvc.perform(conToken(put("/ambientes/" + id), personal, edicion("Nombre nuevo", 46, "[\"Parlantes\"]")))
				.andExpect(status().isBadRequest());

		mvc.perform(conToken(get("/ambientes").param("q", "aula 101"), personal))
				.andExpect(jsonPath("$[0].nombre").value("Aula 101"))
				.andExpect(jsonPath("$[0].aforoMaximo").value(30))
				.andExpect(jsonPath("$[0].recursos", contains("Pizarra acrílica", "Proyector")));
	}

	@DisplayName("Bajar el aforo por debajo de la matrícula da409 y hasta la matrícula se permite")
	@Test
	void bajarElAforoPorDebajoDeLaMatriculaDa409YHastaLaMatriculaSePermite() throws Exception {
		UUID id = idDe("A-101");
		int matriculados = matriculadosDeA101();

		mvc.perform(conToken(put("/ambientes/" + id), personal, edicion("Aula 101", matriculados - 1, "[]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"No puedes bajar el aforo a %d: la sección A de 1.° de secundaria tiene %d estudiantes matriculados."
								.formatted(matriculados - 1, matriculados)));
		mvc.perform(conToken(get("/ambientes").param("q", "aula 101"), personal))
				.andExpect(jsonPath("$[0].aforoMaximo").value(30)); // el rechazo no cambió nada

		mvc.perform(conToken(put("/ambientes/" + id), personal, edicion("Aula 101", matriculados, "[]")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aforoMaximo").value(matriculados));
	}

	@DisplayName("Editar un ambiente que no existe da404")
	@Test
	void editarUnAmbienteQueNoExisteDa404() throws Exception {
		mvc.perform(conToken(put("/ambientes/" + UUID.randomUUID()), personal, edicion("Aula", 30, "[]")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El ambiente no existe."));
	}

	// ---------- Desactivar y reactivar ----------

	@DisplayName("Desactivar y reactivar son idempotentes y se permiten con el ambiente en uso")
	@Test
	void desactivarYReactivarSonIdempotentesYSePermitenConElAmbienteEnUso() throws Exception {
		UUID id = idDe("A-101"); // lo usa 1.° A, con alumnos

		mvc.perform(conToken(post("/ambientes/" + id + "/desactivar"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INACTIVO"))
				.andExpect(jsonPath("$.codigo").value("A-101"));
		mvc.perform(conToken(post("/ambientes/" + id + "/desactivar"), personal)) // ya estaba inactivo
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INACTIVO"));
		mvc.perform(conToken(post("/ambientes/" + id + "/reactivar"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVO"));
		mvc.perform(conToken(post("/ambientes/" + id + "/reactivar"), personal)) // ya estaba activo
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVO"));
	}

	@DisplayName("Un ambiente desactivado sigue en la lista y sale de las tarjetas activas")
	@Test
	void unAmbienteDesactivadoSigueEnLaListaYSaleDeLasTarjetasActivas() throws Exception {
		UUID id = idDe("A-101");
		mvc.perform(conToken(post("/ambientes/" + id + "/desactivar"), personal)).andExpect(status().isOk());

		mvc.perform(conToken(get("/ambientes").param("estado", "INACTIVO").param("q", "aula 101"), personal))
				.andExpect(jsonPath("$.length()").value(1));
		mvc.perform(conToken(get("/ambientes").param("estado", "ACTIVO").param("q", "aula 101"), personal))
				.andExpect(jsonPath("$.length()").value(0));
	}

	@DisplayName("Desactivar o reactivar un ambiente que no existe da404")
	@Test
	void desactivarOReactivarUnAmbienteQueNoExisteDa404() throws Exception {
		UUID inexistente = UUID.randomUUID();
		mvc.perform(conToken(post("/ambientes/" + inexistente + "/desactivar"), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El ambiente no existe."));
		mvc.perform(conToken(post("/ambientes/" + inexistente + "/reactivar"), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El ambiente no existe."));
	}

	// ---------- La base de datos como segunda barrera ----------
	// Cada una es la última sentencia de su prueba: PostgreSQL deja abortada la transacción tras un error.

	@DisplayName("La base rechaza un aula de46 aunque no pase la api")
	@Test
	void laBaseRechazaUnAulaDe46AunqueNoPaseLaApi() {
		rechazaPor("ck_ambiente_aforo", () -> jdbc.sql("""
				INSERT INTO configuracion.ambiente (codigo, nombre, tipo, aforo_maximo)
				VALUES ('A-946', 'Aula de 46', 'AULA_REGULAR', 46)""").update());
	}

	@DisplayName("La base rechaza un aforo de cero aunque no pase la api")
	@Test
	void laBaseRechazaUnAforoDeCeroAunqueNoPaseLaApi() {
		rechazaPor("ck_ambiente_aforo", () -> jdbc.sql("""
				INSERT INTO configuracion.ambiente (codigo, nombre, tipo, aforo_maximo)
				VALUES ('E-946', 'Espacio sin aforo', 'ESPACIO_ESPECIAL', 0)""").update());
	}
}
