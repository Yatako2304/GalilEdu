package com.galiledu.configuracion.infraestructura.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.configuracion.aplicacion.puertos.RepositorioCatalogoCurricular;
import com.galiledu.configuracion.dominio.DatosAreaCurricular;
import com.galiledu.configuracion.dominio.DatosCompetencia;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioCatalogoCurricularPostgres implements RepositorioCatalogoCurricular {
	private final JdbcOperations jdbc;

	public RepositorioCatalogoCurricularPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public UUID crearArea(DatosAreaCurricular datos) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO configuracion.area_curricular (id, nombre, descripcion) VALUES (?, ?, ?)",
			id, datos.nombre(), datos.descripcion());
		return id;
	}

	@Override
	public Optional<AreaCurricular> buscarArea(UUID id) {
		return jdbc.query("SELECT id, nombre, descripcion, activo FROM configuracion.area_curricular WHERE id = ?",
			(rs, row) -> new AreaCurricular(rs.getObject("id", UUID.class), rs.getString("nombre"),
				rs.getString("descripcion"), rs.getBoolean("activo")), id).stream().findFirst();
	}

	@Override
	public List<AreaCurricular> listarAreas() {
		return jdbc.query("SELECT id, nombre, descripcion, activo FROM configuracion.area_curricular ORDER BY nombre, id",
			(rs, row) -> new AreaCurricular(rs.getObject("id", UUID.class), rs.getString("nombre"),
				rs.getString("descripcion"), rs.getBoolean("activo")));
	}

	@Override
	public boolean actualizarArea(UUID id, DatosAreaCurricular datos) {
		return jdbc.update("UPDATE configuracion.area_curricular SET nombre = ?, descripcion = ? WHERE id = ?",
			datos.nombre(), datos.descripcion(), id) == 1;
	}

	@Override
	public boolean cambiarEstadoArea(UUID id, boolean activo) {
		return jdbc.update("UPDATE configuracion.area_curricular SET activo = ? WHERE id = ?", activo, id) == 1;
	}

	@Override
	public UUID crearCompetencia(DatosCompetencia datos) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
			INSERT INTO configuracion.competencia (id, area_curricular_id, nombre, descripcion)
			VALUES (?, ?, ?, ?)
			""", id, datos.areaCurricularId(), datos.nombre(), datos.descripcion());
		return id;
	}

	@Override
	public Optional<Competencia> buscarCompetencia(UUID id) {
		return jdbc.query("""
			SELECT id, area_curricular_id, nombre, descripcion, activo
			FROM configuracion.competencia WHERE id = ?
			""", (rs, row) -> new Competencia(rs.getObject("id", UUID.class),
			rs.getObject("area_curricular_id", UUID.class), rs.getString("nombre"),
			rs.getString("descripcion"), rs.getBoolean("activo")), id).stream().findFirst();
	}

	@Override
	public List<Competencia> listarCompetencias() {
		return jdbc.query("""
			SELECT id, area_curricular_id, nombre, descripcion, activo
			FROM configuracion.competencia ORDER BY nombre, id
			""", (rs, row) -> new Competencia(rs.getObject("id", UUID.class),
			rs.getObject("area_curricular_id", UUID.class), rs.getString("nombre"),
			rs.getString("descripcion"), rs.getBoolean("activo")));
	}

	@Override
	public boolean actualizarCompetencia(UUID id, DatosCompetencia datos) {
		return jdbc.update("""
			UPDATE configuracion.competencia
			SET area_curricular_id = ?, nombre = ?, descripcion = ? WHERE id = ?
			""", datos.areaCurricularId(), datos.nombre(), datos.descripcion(), id) == 1;
	}

	@Override
	public boolean cambiarEstadoCompetencia(UUID id, boolean activo) {
		return jdbc.update("UPDATE configuracion.competencia SET activo = ? WHERE id = ?", activo, id) == 1;
	}
}
