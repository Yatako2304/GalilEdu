package com.galiledu.configuracion.infraestructura.persistencia;

import java.util.UUID;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** REST + PostgreSQL real; Spring revierte los registros al finalizar la prueba. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "GALILEDU_POSTGRES_TEST", matches = "true")
class CatalogoCurricularPostgresIntegrationTests {
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
	void creaEditaDesactivaYReactivaAreaYCompetenciaDesdeRest() throws Exception {
		String sufijo = UUID.randomUUID().toString().substring(0, 8);
		String areaBase = "/api/configuracion/areas-curriculares";
		String competenciaBase = "/api/configuracion/competencias";
		var administrativo = user("administrativo").roles("PERSONAL_ADMINISTRATIVO");

		var altaArea = mvc.perform(post(areaBase).with(administrativo).with(csrf())
			.contentType("application/json")
			.content("{\"nombre\":\"Área %s\",\"descripcion\":\"Área de prueba\"}".formatted(sufijo)))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.activo").value(true)).andReturn();
		UUID areaId = idDelLocation(altaArea.getResponse().getHeader("Location"));

		var altaCompetencia = mvc.perform(post(competenciaBase).with(administrativo).with(csrf())
			.contentType("application/json")
			.content("""
				{"areaCurricularId":"%s","nombre":"Competencia %s","descripcion":"Competencia de prueba"}
				""".formatted(areaId, sufijo)))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.areaCurricularId").value(areaId.toString()))
			.andReturn();
		UUID competenciaId = idDelLocation(altaCompetencia.getResponse().getHeader("Location"));

		mvc.perform(put(areaBase + "/" + areaId).with(administrativo).with(csrf())
			.contentType("application/json")
			.content("{\"nombre\":\"Área editada %s\",\"descripcion\":\"Descripción nueva\"}".formatted(sufijo)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Área editada " + sufijo));
		mvc.perform(delete(competenciaBase + "/" + competenciaId).with(administrativo).with(csrf()))
			.andExpect(status().isNoContent());
		mvc.perform(get(competenciaBase + "/" + competenciaId).with(administrativo))
			.andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
		mvc.perform(post(competenciaBase + "/" + competenciaId + "/reactivacion")
			.with(administrativo).with(csrf())).andExpect(status().isNoContent());
		assertThat(jdbc.queryForObject("SELECT activo FROM configuracion.competencia WHERE id = ?",
			Boolean.class, competenciaId)).isTrue();
	}

	private static UUID idDelLocation(String location) {
		return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
	}
}
