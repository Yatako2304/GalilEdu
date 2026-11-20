package com.galiledu.matricula;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;

/**
 * HU-25 · Matrícula extemporánea (RF-54, RF-55). En los datos de prueba el periodo 2027 (15/09 – 31/10/2026) ya cerró
 * y permite matrícula extemporánea; Mateo tiene 1.° A reservada en 2027.
 */
class MatriculaExtemporaneaIT extends PruebaIntegracion {
	private static final String APODERADO = "U20260003";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	private static final String MOTIVO = "Se mudó de provincia en noviembre";

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private EntityManager em;

	private int contador = 0;

	private UUID periodo2027() {
		return jdbc.sql("""
				SELECT pm.id FROM matricula.periodo_matricula pm
				JOIN configuracion.anio_escolar a ON a.id = pm.anio_escolar_id WHERE a.anio = 2027""")
				.query(UUID.class).single();
	}

	/** Un estudiante activo inscrito para 2027 en el grado indicado, sin matrícula (se deshace al terminar). */
	private UUID candidato(String gradoCodigo) {
		UUID persona = jdbc.sql("""
				INSERT INTO personas.persona (tipo_documento, numero_documento, apellido_paterno, nombres)
				VALUES ('DNI', ?, 'Tardía', 'Lucía') RETURNING id""")
				.param("9800%04d".formatted(++contador)).query(UUID.class).single();
		return jdbc.sql("""
				INSERT INTO estudiantes.estudiante (persona_id, anio_academico, grado_codigo)
				VALUES (?, 2027, ?) RETURNING id""").params(persona, gradoCodigo).query(UUID.class).single();
	}

	private UUID seccion(String gradoCodigo, String nombre) {
		return jdbc.sql("""
				SELECT id FROM configuracion.seccion
				WHERE anio_escolar_id = '30000000-0000-0000-0000-000000002027' AND grado_codigo = ? AND nombre = ?""")
				.params(gradoCodigo, nombre).query(UUID.class).single();
	}

	private UUID primerDocumento() {
		return jdbc.sql("SELECT id FROM matricula.documento_requerido WHERE periodo_matricula_id = ? ORDER BY orden")
				.param(periodo2027()).query(UUID.class).list().getFirst();
	}

	private ResultActions registrar(UUID estudiante, UUID seccion, String motivo, MockMultipartFile... archivos)
			throws Exception {
		em.clear();
		var peticion = multipart("/matricula/periodos/{id}/matriculas-extemporaneas", periodo2027())
				.param("estudianteId", estudiante.toString());
		if (seccion != null) {
			peticion.param("seccionId", seccion.toString());
		}
		if (motivo != null) {
			peticion.param("motivo", motivo);
		}
		for (MockMultipartFile archivo : archivos) {
			peticion.file(archivo);
		}
		return mvc.perform(conToken(peticion, token(PERSONAL_ADMINISTRATIVO)));
	}

	@DisplayName("Con el período cerrado registra la matrícula extemporanea con su motivo")
	@Test
	void conElPeriodoCerradoRegistraLaMatriculaExtemporaneaConSuMotivo() throws Exception {
		UUID lucia = candidato("SEC-3");
		registrar(lucia, seccion("SEC-3", "B"), "  " + MOTIVO + " ",
				new MockMultipartFile(primerDocumento().toString(), "ficha.pdf", "application/pdf", new byte[]{1}))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SECCION_RESERVADA"))
				// RF-55: la sección la elige el colegio
				.andExpect(jsonPath("$.seccion").value("3.° B"))
				.andExpect(jsonPath("$.documentosSubidos").value(1));

		UUID matricula = jdbc.sql("SELECT id FROM matricula.matricula WHERE estudiante_id = ?").param(lucia)
				.query(UUID.class).single();
		mvc.perform(conToken(get("/matricula/matriculas/{id}", matricula), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.extemporanea").value(true))
				.andExpect(jsonPath("$.motivoExtemporanea").value(MOTIVO))
				// con el periodo cerrado, una extemporánea sí puede cambiar de sección
				.andExpect(jsonPath("$.puedeCambiarSeccion").value(true));
		// y aparece en el listado del periodo
		mvc.perform(conToken(get("/matricula/periodos/{id}/matriculas", periodo2027()).param("texto", "Tardía"),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(jsonPath("$.meta.total").value(1));
	}

	@DisplayName("El motivo y la sección son obligatorios")
	@Test
	void elMotivoYLaSeccionSonObligatorios() throws Exception {
		UUID lucia = candidato("SEC-3");
		registrar(lucia, seccion("SEC-3", "A"), "   ")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Indica el motivo de la matrícula extemporánea."));
		registrar(lucia, null, MOTIVO)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Elige la sección del estudiante."));
		registrar(lucia, seccion("SEC-3", "A"), "x".repeat(501))
				.andExpect(status().isBadRequest());
	}

	@DisplayName("La sección debe ser del grado y tener cupo")
	@Test
	void laSeccionDebeSerDelGradoYTenerCupo() throws Exception {
		UUID lucia = candidato("SEC-1");
		registrar(lucia, seccion("SEC-3", "A"), MOTIVO)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La sección elegida no es de 1.° de secundaria."));
		// 1.° A ya tiene a Mateo: con capacidad 1 queda llena
		jdbc.sql("UPDATE configuracion.seccion SET capacidad_maxima = 1 WHERE id = ?").param(seccion("SEC-1", "A"))
				.update();
		registrar(lucia, seccion("SEC-1", "A"), MOTIVO)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("La sección 1.° A no tiene vacantes."));
	}

	@DisplayName("Con el período abierto no se registra extemporanea")
	@Test
	void conElPeriodoAbiertoNoSeRegistraExtemporanea() throws Exception {
		jdbc.sql("UPDATE matricula.periodo_matricula SET cierre = now() + interval '30 days' WHERE id = ?")
				.param(periodo2027()).update();
		registrar(candidato("SEC-3"), seccion("SEC-3", "A"), MOTIVO)
				.andExpect(status().isConflict())
				.andExpect(
						jsonPath("$.message").value("La matrícula extemporánea se habilita cuando el periodo cierre."));
	}

	@DisplayName("Si el período no la permite se rechaza")
	@Test
	void siElPeriodoNoLaPermiteSeRechaza() throws Exception {
		jdbc.sql("UPDATE matricula.periodo_matricula SET permitir_extemporanea = FALSE WHERE id = ?")
				.param(periodo2027()).update();
		registrar(candidato("SEC-3"), seccion("SEC-3", "A"), MOTIVO)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("El periodo de matrícula 2027 no permite matrículas extemporáneas."));
	}

	@DisplayName("No duplica la matrícula del año")
	@Test
	void noDuplicaLaMatriculaDelAnio() throws Exception {
		registrar(UUID.fromString(MATEO), seccion("SEC-1", "B"), MOTIVO)
				.andExpect(status().isConflict());
	}

	@DisplayName("El apoderado completa una extemporanea aunque el período este cerrado")
	@Test
	void elApoderadoCompletaUnaExtemporaneaAunqueElPeriodoEsteCerrado() throws Exception {
		jdbc.sql("""
				UPDATE matricula.matricula SET extemporanea = TRUE, motivo_extemporanea = ?
				WHERE estudiante_id = ?::uuid AND anio_escolar_id = '30000000-0000-0000-0000-000000002027'""")
				.params(MOTIVO, MATEO).update();
		String apoderado = token(APODERADO);
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), apoderado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matricula.extemporanea").value(true))
				.andExpect(jsonPath("$.impedimentos[*].tipo", not(hasItem("PERIODO"))));
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/documentos", MATEO), apoderado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.puedeSubir").value(true));
	}

	@DisplayName("Una matrícula regular con el período cerrado sigue bloqueada")
	@Test
	void unaMatriculaRegularConElPeriodoCerradoSigueBloqueada() throws Exception {
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), token(APODERADO)))
				.andExpect(jsonPath("$.matricula.extemporanea").value(false))
				.andExpect(jsonPath("$.impedimentos[*].tipo", hasItem("PERIODO")));
		Assertions.assertFalse(jdbc.sql("""
				SELECT extemporanea FROM matricula.matricula
				WHERE estudiante_id = ?::uuid AND anio_escolar_id = '30000000-0000-0000-0000-000000002027'""")
				.param(MATEO).query(Boolean.class).single());
	}

	@DisplayName("Otro rol no registra extemporaneas")
	@Test
	void otroRolNoRegistraExtemporaneas() throws Exception {
		mvc.perform(conToken(multipart("/matricula/periodos/{id}/matriculas-extemporaneas", periodo2027())
				.param("estudianteId", MATEO).param("motivo", MOTIVO), token(APODERADO)))
				.andExpect(status().isForbidden());
	}
}
