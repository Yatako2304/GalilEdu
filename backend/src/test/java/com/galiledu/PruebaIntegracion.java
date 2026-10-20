package com.galiledu;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Base de las pruebas de integración (*IT.java): levanta toda la app contra PostgreSQL real.
 * <ul>
 * <li>Base: {@code TEST_DB_URL} (por defecto {@code galiledu_test} en localhost:5433). Créala vacía una vez;
 * Flyway aplica las migraciones y los datos de prueba (usuarios U20260001… con contraseña Galiledu2026).</li>
 * <li>Cada prueba corre en una transacción que se deshace al final: la base queda igual.</li>
 * <li>Se ejecutan con {@code mvnw verify} (no con {@code mvnw test}) y en GitHub Actions.</li>
 * </ul>
 */
@SpringBootTest(properties = {
		"spring.datasource.url=${TEST_DB_URL:jdbc:postgresql://localhost:5433/galiledu_test}",
		"spring.mail.host=",
})
@AutoConfigureMockMvc
@Transactional
public abstract class PruebaIntegracion {
	protected static final String CONTRASENA = "Galiledu2026";
	protected static final String ADMIN = "U20260001";
	protected static final String PERSONAL_ADMINISTRATIVO = "U20260002";
	protected static final String DOCENTE = "U20260004";

	@Autowired
	protected MockMvc mvc;

	/** Inicia sesión y devuelve el JWT. */
	protected String token(String usuario) throws Exception {
		String respuesta = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"%s","password":"%s"}""".formatted(usuario, CONTRASENA)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(respuesta, "$.jwt");
	}

	/** Agrega el token y el cuerpo JSON a una petición. */
	protected static <B extends AbstractMockHttpServletRequestBuilder<B>> B conToken(B peticion, String token,
			String json) {
		peticion.header("Authorization", "Bearer " + token);
		if (json != null) {
			peticion.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return peticion;
	}

	protected static <B extends AbstractMockHttpServletRequestBuilder<B>> B conToken(B peticion, String token) {
		return conToken(peticion, token, null);
	}

	/**
	 * Exige que la BD rechace la operación por la restricción indicada (su nombre en la migración), y no por otra. Así
	 * la prueba no pasa por accidente si el dato falla por un motivo distinto (p. ej. una columna obligatoria vacía).
	 */
	protected static void rechazaPor(String restriccion, ThrowingCallable operacion) {
		assertThatThrownBy(operacion)
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining(restriccion);
	}
}
