package com.galiledu.matricula.infraestructura.persistencia;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** REST and real SQL transaction; test fixtures and reservation are rolled back. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "GALILEDU_POSTGRES_TEST", matches = "true")
class ReservasMatriculaPostgresIntegrationTests {
	@Autowired private MockMvc mvc;
	@Autowired private JdbcOperations jdbc;

	@DynamicPropertySource
	static void postgres(DynamicPropertyRegistry propiedades) {
		propiedades.add("spring.datasource.url", () -> System.getenv("DB_URL"));
		propiedades.add("spring.datasource.username", () -> System.getenv("DB_USER"));
		propiedades.add("spring.datasource.password", () -> System.getenv("DB_PASSWORD"));
		propiedades.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		propiedades.add("spring.jpa.hibernate.ddl-auto", () -> "none");
	}

	@Test
	@Transactional
	void reservaAlternativaSinFormalizarNiDuplicar() throws Exception {
		int anio = ThreadLocalRandom.current().nextInt(5000, 9000);
		UUID anioId = UUID.randomUUID();
		UUID gradoId = UUID.randomUUID();
		UUID seccionA = UUID.randomUUID();
		UUID seccionB = UUID.randomUUID();
		UUID estudianteOcupante = estudianteTemporal();
		UUID estudianteNuevo = estudianteTemporal();
		UUID estudianteSinCupo = estudianteTemporal();
		jdbc.update("""
			INSERT INTO configuracion.anio_escolar
			(id, anio, fecha_inicio, fecha_fin, regimen_periodos, estado)
			VALUES (?, ?, ?, ?, 'BIMESTRAL'::configuracion.regimen_periodos,
		            'PLANIFICADO'::configuracion.estado_anio)
			""", anioId, anio, LocalDate.of(anio, 3, 1), LocalDate.of(anio, 12, 31));
		jdbc.update("""
			INSERT INTO configuracion.grado (id, nivel, numero, nombre)
			VALUES (?, 'PRIMARIA'::configuracion.nivel_educativo, ?, ?)
			""", gradoId, ThreadLocalRandom.current().nextInt(1000, 1_000_000_000),
			"Grado temporal " + gradoId);
		seccion(seccionA, anioId, gradoId, "A");
		seccion(seccionB, anioId, gradoId, "B");
		jdbc.update("""
			INSERT INTO matricula.matricula
			(id, estudiante_id, anio_id, seccion_id, tipo, estado)
			VALUES (?, ?, ?, ?, 'REGULAR'::matricula.tipo_matricula,
		            'SECCION_RESERVADA'::matricula.estado_matricula)
			""", UUID.randomUUID(), estudianteOcupante, anioId, seccionA);

		var personal = user("administrativo").roles("PERSONAL_ADMINISTRATIVO");
		String solicitud = """
			{"estudianteId":"%s","anioEscolarId":"%s","seccionId":"%s"}
			""".formatted(estudianteNuevo, anioId, seccionA);
		var alta = mvc.perform(post("/api/matriculas/reservas").with(personal).with(csrf())
			.contentType("application/json").content(solicitud))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.seccionId").value(seccionB.toString()))
			.andExpect(jsonPath("$.estado").value("SECCION_RESERVADA"))
			.andReturn();
		String location = alta.getResponse().getHeader("Location");
		mvc.perform(get(location).with(personal))
			.andExpect(status().isOk()).andExpect(jsonPath("$.seccionId").value(seccionB.toString()));
		mvc.perform(post("/api/matriculas/reservas").with(personal).with(csrf())
			.contentType("application/json").content(solicitud))
			.andExpect(status().isConflict());
		mvc.perform(post("/api/matriculas/reservas").with(personal).with(csrf())
			.contentType("application/json").content("""
				{"estudianteId":"%s","anioEscolarId":"%s","seccionId":"%s"}
				""".formatted(estudianteSinCupo, anioId, seccionA)))
			.andExpect(status().isConflict());
		assertThat(jdbc.queryForObject("""
			SELECT count(*) FROM matricula.matricula
			WHERE anio_id = ? AND estado = 'SECCION_RESERVADA'::matricula.estado_matricula
			  AND codigo_matricula IS NULL AND fecha_confirmacion IS NULL
			""", Integer.class, anioId)).isEqualTo(2);
	}

	private UUID estudianteTemporal() {
		UUID id = UUID.randomUUID();
		String documento = id.toString().replace("-", "").substring(0, 12);
		jdbc.update("""
			INSERT INTO personas.persona
			(id, tipo_documento, numero_documento, nombres, primer_apellido)
			VALUES (?, 'CARNET_EXTRANJERIA'::personas.tipo_documento, ?, 'Prueba', 'Temporal')
			""", id, documento);
		jdbc.update("INSERT INTO personas.estudiante (id) VALUES (?)", id);
		return id;
	}

	private void seccion(UUID id, UUID anioId, UUID gradoId, String nombre) {
		jdbc.update("""
			INSERT INTO configuracion.seccion
			(id, anio_id, grado_id, nombre, capacidad_maxima)
			VALUES (?, ?, ?, ?, 1)
			""", id, anioId, gradoId, nombre);
	}
}
