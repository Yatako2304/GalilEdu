package com.galiledu.configuracion.infraestructura.persistencia;

import java.util.UUID;

import com.galiledu.configuracion.dominio.DatosAreaCurricular;
import com.galiledu.configuracion.dominio.DatosCompetencia;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositorioCatalogoCurricularPostgresTests {
	@Test
	void areasYCompetenciasUsanLasTablasYBorradoLogicoDelScript() {
		var jdbc = nuevaBaseAislada();
		var repositorio = new RepositorioCatalogoCurricularPostgres(jdbc);
		UUID areaId = repositorio.crearArea(new DatosAreaCurricular("Matemática", "Área curricular"));
		UUID competenciaId = repositorio.crearCompetencia(
			new DatosCompetencia(areaId, "Resuelve problemas", "Competencia"));

		assertThat(repositorio.buscarArea(areaId).orElseThrow().activo()).isTrue();
		assertThat(repositorio.listarCompetencias()).hasSize(1);
		assertThat(repositorio.actualizarCompetencia(competenciaId,
			new DatosCompetencia(areaId, "Resuelve problemas de cantidad", "Competencia"))).isTrue();
		assertThat(repositorio.buscarCompetencia(competenciaId).orElseThrow().nombre())
			.isEqualTo("Resuelve problemas de cantidad");
		assertThat(repositorio.cambiarEstadoArea(areaId, false)).isTrue();
		assertThat(repositorio.buscarArea(areaId).orElseThrow().activo()).isFalse();
		assertThat(repositorio.cambiarEstadoArea(areaId, true)).isTrue();
		assertThat(repositorio.cambiarEstadoCompetencia(competenciaId, false)).isTrue();
		assertThat(repositorio.buscarCompetencia(competenciaId).orElseThrow().activo()).isFalse();
		assertThat(repositorio.cambiarEstadoCompetencia(competenciaId, true)).isTrue();
		assertThat(repositorio.buscarCompetencia(competenciaId).orElseThrow().activo()).isTrue();
		assertThat(repositorio.listarAreas()).hasSize(1);
	}

	@Test
	void noAceptaCompetenciaConAreaInexistenteNiNombreDuplicado() {
		var repositorio = new RepositorioCatalogoCurricularPostgres(nuevaBaseAislada());
		assertThatThrownBy(() -> repositorio.crearCompetencia(
			new DatosCompetencia(UUID.randomUUID(), "Nombre", "Descripción")))
			.isInstanceOf(DataIntegrityViolationException.class);
		repositorio.crearArea(new DatosAreaCurricular("Matemática", "Área curricular"));
		assertThatThrownBy(() -> repositorio.crearArea(new DatosAreaCurricular("Matemática", "Otra")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private static JdbcTemplate nuevaBaseAislada() {
		var fuente = new DriverManagerDataSource(
			"jdbc:h2:mem:catalogo_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
		var jdbc = new JdbcTemplate(fuente);
		jdbc.execute("CREATE SCHEMA configuracion");
		jdbc.execute("""
			CREATE TABLE configuracion.area_curricular (
			  id UUID PRIMARY KEY, nombre VARCHAR(100) NOT NULL UNIQUE,
			  descripcion VARCHAR(255), activo BOOLEAN NOT NULL DEFAULT TRUE)
			""");
		jdbc.execute("""
			CREATE TABLE configuracion.competencia (
			  id UUID PRIMARY KEY, area_curricular_id UUID NOT NULL REFERENCES configuracion.area_curricular(id),
			  nombre VARCHAR(150) NOT NULL, descripcion VARCHAR(255), activo BOOLEAN NOT NULL DEFAULT TRUE,
			  UNIQUE (area_curricular_id, nombre))
			""");
		return jdbc;
	}
}
