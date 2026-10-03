package com.galiledu.pagos.infraestructura.persistencia;

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

/** Writes a school-year fixture and a tariff to PostgreSQL, then rolls all records back. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "GALILEDU_POSTGRES_TEST", matches = "true")
class TarifariosPostgresIntegrationTests {
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
	void registraTarifarioCompletoYConsultaSinCrearUnDuplicado() throws Exception {
		int anio = ThreadLocalRandom.current().nextInt(5000, 9000);
		UUID anioId = UUID.randomUUID();
		jdbc.update("""
			INSERT INTO configuracion.anio_escolar
			(id, anio, fecha_inicio, fecha_fin, regimen_periodos, estado)
			VALUES (?, ?, ?, ?, 'BIMESTRAL'::configuracion.regimen_periodos,
			        'PLANIFICADO'::configuracion.estado_anio)
			""", anioId, anio, LocalDate.of(anio, 3, 1), LocalDate.of(anio, 12, 31));
		String contenido = """
			{"anioEscolarId":"%s","montoMatricula":850.00,"montoPension":450.00,
			 "cantidadCuotas":2,"vencimientoMatricula":"%s",
			 "vencimientosPensiones":["%s","%s"],
			 "diasGracia":5,"tasaMensual":1.50,"topePorcentaje":10.00}
			""".formatted(anioId, LocalDate.of(anio, 2, 10),
			LocalDate.of(anio, 4, 10), LocalDate.of(anio, 5, 10));
		var administrativo = user("administrativo").roles("PERSONAL_ADMINISTRATIVO");

		mvc.perform(post("/api/pagos/tarifarios").with(administrativo).with(csrf())
			.contentType("application/json").content(contenido))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("VIGENTE"))
			.andExpect(jsonPath("$.cantidadCuotas").value(2))
			.andExpect(jsonPath("$.vencimientoMatricula").value(LocalDate.of(anio, 2, 10).toString()));
		mvc.perform(get("/api/pagos/tarifarios/" + anioId).with(administrativo))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.vencimientosPensiones.length()").value(2))
			.andExpect(jsonPath("$.vencimientoMatricula").value(LocalDate.of(anio, 2, 10).toString()));
		assertThat(jdbc.queryForObject("""
			SELECT count(*) FROM pagos.detalle_tarifa d
			JOIN pagos.tarifario_escolar t ON t.id = d.tarifario_escolar_id
			WHERE t.anio_escolar_id = ?
			""", Integer.class, anioId)).isEqualTo(2);
		assertThat(jdbc.queryForObject("""
			SELECT d.numero FROM pagos.detalle_calendario d
			JOIN pagos.calendario_pagos c ON c.id = d.calendario_pagos_id
			JOIN pagos.detalle_tarifa t ON t.id = d.detalle_tarifa_id
			JOIN pagos.categoria_tarifa ct ON ct.id = t.categoria_tarifa_id
			WHERE c.anio_escolar_id = ? AND ct.codigo = 'MATRICULA'
			""", Integer.class, anioId)).isEqualTo(3);
		assertThat(jdbc.queryForObject("""
			SELECT count(*) FROM pagos.detalle_calendario d
			JOIN pagos.calendario_pagos c ON c.id = d.calendario_pagos_id
			WHERE c.anio_escolar_id = ?
			""", Integer.class, anioId)).isEqualTo(3);
		mvc.perform(post("/api/pagos/tarifarios").with(administrativo).with(csrf())
			.contentType("application/json").content(contenido))
			.andExpect(status().isConflict());
	}
}
