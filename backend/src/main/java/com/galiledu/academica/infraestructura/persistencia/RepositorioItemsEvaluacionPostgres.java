package com.galiledu.academica.infraestructura.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.academica.aplicacion.puertos.RepositorioItemsEvaluacion;
import com.galiledu.academica.dominio.ItemEvaluacion;
import com.galiledu.academica.dominio.TipoItemEvaluacion;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioItemsEvaluacionPostgres implements RepositorioItemsEvaluacion {
	private static final String COLUMNAS = """
		id, carga_academica_id, configuracion_competencia_id, periodo_academico_id,
		nombre, tipo, fecha_evaluacion, peso, activo
		""";
	private final JdbcOperations jdbc;

	public RepositorioItemsEvaluacionPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void crear(ItemEvaluacion item) {
		jdbc.update("""
			INSERT INTO gestion_academica.item_evaluacion
			(id, carga_academica_id, configuracion_competencia_id, periodo_academico_id,
			 nombre, tipo, fecha_evaluacion, peso, activo)
			VALUES (?, ?, ?, ?, ?, ?::gestion_academica.tipo_item_evaluacion, ?, ?, ?)
			""", item.id(), item.cargaAcademicaId(), item.configuracionCompetenciaId(),
			item.periodoAcademicoId(), item.nombre(), item.tipo().name(), item.fechaEvaluacion(),
			item.peso(), item.activo());
	}

	@Override
	public Optional<ItemEvaluacion> buscar(UUID id) {
		return jdbc.query("SELECT " + COLUMNAS + " FROM gestion_academica.item_evaluacion WHERE id = ?",
			(rs, row) -> item(rs), id).stream().findFirst();
	}

	@Override
	public boolean actualizar(ItemEvaluacion item) {
		return jdbc.update("""
			UPDATE gestion_academica.item_evaluacion
			SET nombre = ?, tipo = ?::gestion_academica.tipo_item_evaluacion,
			    fecha_evaluacion = ?, peso = ?, activo = ?
			WHERE id = ?
			""", item.nombre(), item.tipo().name(), item.fechaEvaluacion(), item.peso(), item.activo(),
			item.id()) == 1;
	}

	@Override
	public List<ItemEvaluacion> listarActivos(UUID cargaAcademicaId, UUID periodoAcademicoId) {
		return jdbc.query("SELECT " + COLUMNAS + """
			FROM gestion_academica.item_evaluacion
			WHERE carga_academica_id = ? AND activo = TRUE
			  AND (?::uuid IS NULL OR periodo_academico_id = ?::uuid)
			ORDER BY fecha_evaluacion NULLS LAST, nombre
			""", (rs, row) -> item(rs), cargaAcademicaId, periodoAcademicoId, periodoAcademicoId);
	}

	@Override
	public boolean tieneCalificaciones(UUID itemId) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
			SELECT EXISTS (SELECT 1 FROM gestion_academica.calificacion
			WHERE item_evaluacion_id = ? AND activo = TRUE)
			""", Boolean.class, itemId));
	}

	private static ItemEvaluacion item(ResultSet rs) throws SQLException {
		return new ItemEvaluacion(rs.getObject("id", UUID.class), rs.getObject("carga_academica_id", UUID.class),
			rs.getObject("configuracion_competencia_id", UUID.class),
			rs.getObject("periodo_academico_id", UUID.class), rs.getString("nombre"),
			TipoItemEvaluacion.valueOf(rs.getString("tipo")), rs.getObject("fecha_evaluacion", LocalDate.class),
			rs.getBigDecimal("peso"), rs.getBoolean("activo"));
	}
}
