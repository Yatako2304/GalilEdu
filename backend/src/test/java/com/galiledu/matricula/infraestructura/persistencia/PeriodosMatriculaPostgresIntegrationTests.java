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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The migration-shaped test table and all data are created in a rollback-only transaction. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "GALILEDU_POSTGRES_TEST", matches = "true")
class PeriodosMatriculaPostgresIntegrationTests {
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
	void configuraYActualizaUnPeriodoDistintoDelAnioEscolar() throws Exception {
		// PostgreSQL rolls back DDL too; this does not apply the external migration.
		jdbc.execute("""
			CREATE TABLE IF NOT EXISTS matricula.periodo_matricula (
			  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
			  anio_escolar_id UUID NOT NULL REFERENCES configuracion.anio_escolar (id),
			  fecha_inicio DATE NOT NULL,
			  fecha_fin DATE NOT NULL,
			  CONSTRAINT uq_periodo_matricula_anio UNIQUE (anio_escolar_id),
			  CONSTRAINT ck_periodo_matricula_fechas CHECK (fecha_fin >= fecha_inicio)
			)
			""");
		int anio = ThreadLocalRandom.current().nextInt(5000, 9000);
		UUID anioId = UUID.randomUUID();
		jdbc.update("""
			INSERT INTO configuracion.anio_escolar
			(id, anio, fecha_inicio, fecha_fin, regimen_periodos, estado)
			VALUES (?, ?, ?, ?, 'BIMESTRAL'::configuracion.regimen_periodos,
		            'PLANIFICADO'::configuracion.estado_anio)
			""", anioId, anio, LocalDate.of(anio, 3, 1), LocalDate.of(anio, 12, 31));
		var personal = user("administrativo").roles("PERSONAL_ADMINISTRATIVO");
		String ruta = "/api/matriculas/periodos/" + anioId;
		mvc.perform(put(ruta).with(personal).with(csrf()).contentType("application/json")
			.content("""
				{"inicio":"%s","fin":"%s"}
				""".formatted(LocalDate.of(anio, 1, 10), LocalDate.of(anio, 2, 20))))
			.andExpect(status().isOk());
		mvc.perform(get(ruta).queryParam("fecha", LocalDate.of(anio, 2, 1).toString())
			.with(personal))
			.andExpect(status().isOk()).andExpect(jsonPath("$.habilitado").value(true));
		mvc.perform(get(ruta).queryParam("fecha", LocalDate.of(anio, 3, 1).toString())
			.with(personal))
			.andExpect(status().isOk()).andExpect(jsonPath("$.habilitado").value(false));
		mvc.perform(put(ruta).with(personal).with(csrf()).contentType("application/json")
			.content("""
				{"inicio":"%s","fin":"%s"}
				""".formatted(LocalDate.of(anio, 1, 15), LocalDate.of(anio, 2, 25))))
			.andExpect(status().isOk());
		assertThat(jdbc.queryForObject("""
			SELECT count(*) FROM matricula.periodo_matricula WHERE anio_escolar_id = ?
			""", Integer.class, anioId)).isEqualTo(1);
		assertThat(jdbc.queryForObject("""
			SELECT fecha_inicio FROM matricula.periodo_matricula WHERE anio_escolar_id = ?
			""", LocalDate.class, anioId)).isEqualTo(LocalDate.of(anio, 1, 15));
	}
}
