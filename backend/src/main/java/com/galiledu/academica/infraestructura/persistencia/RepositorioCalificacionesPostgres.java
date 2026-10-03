package com.galiledu.academica.infraestructura.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.academica.aplicacion.SolicitudAcademicaInvalidaException;
import com.galiledu.academica.aplicacion.puertos.RepositorioCalificaciones;
import com.galiledu.academica.dominio.CalculadoraNotas;
import com.galiledu.academica.dominio.CalificacionEstudiante;
import com.galiledu.academica.dominio.EvidenciaPedagogica;
import com.galiledu.academica.dominio.TipoItemEvaluacion;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioCalificacionesPostgres implements RepositorioCalificaciones {
	private static final String COLUMNAS = """
		c.id, c.item_evaluacion_id, c.matricula_id, c.valor_numerico, c.valor_cualitativo,
		c.fecha_registro, c.fecha_modificacion
		""";
	private final JdbcOperations jdbc;

	public RepositorioCalificacionesPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<CalificacionEstudiante> buscarParaActualizar(UUID itemEvaluacionId, UUID matriculaId) {
		return jdbc.query("SELECT " + COLUMNAS + """
			FROM gestion_academica.calificacion c
			WHERE c.item_evaluacion_id = ? AND c.matricula_id = ?
			FOR UPDATE
			""", (rs, row) -> calificacion(rs), itemEvaluacionId, matriculaId).stream().findFirst();
	}

	@Override
	public Optional<CalificacionEstudiante> buscar(UUID id) {
		return jdbc.query("SELECT " + COLUMNAS + """
			FROM gestion_academica.calificacion c
			WHERE c.id = ? AND c.activo = TRUE
			""", (rs, row) -> calificacion(rs), id).stream().findFirst();
	}

	@Override
	public void crear(CalificacionEstudiante calificacion) {
		try {
			jdbc.update("""
				INSERT INTO gestion_academica.calificacion
				(id, item_evaluacion_id, matricula_id, valor_numerico, valor_cualitativo, fecha_registro)
				VALUES (?, ?, ?, ?, ?::configuracion.valor_cualitativo, ?)
				""", calificacion.id(), calificacion.itemEvaluacionId(), calificacion.matriculaId(),
				ColumnasCalificacion.numerico(calificacion.valor()),
				ColumnasCalificacion.cualitativo(calificacion.valor()),
				ColumnasCalificacion.timestamp(calificacion.fechaRegistro()));
		} catch (DuplicateKeyException e) {
			throw new SolicitudAcademicaInvalidaException(
				"La calificación se registró al mismo tiempo desde otra sesión; vuelva a intentarlo");
		}
	}

	/** Una fila retirada lógicamente vuelve a estar activa al recibir una corrección. */
	@Override
	public boolean actualizar(CalificacionEstudiante calificacion) {
		return jdbc.update("""
			UPDATE gestion_academica.calificacion
			SET valor_numerico = ?, valor_cualitativo = ?::configuracion.valor_cualitativo,
			    fecha_modificacion = ?, activo = TRUE
			WHERE id = ?
			""", ColumnasCalificacion.numerico(calificacion.valor()),
			ColumnasCalificacion.cualitativo(calificacion.valor()),
			ColumnasCalificacion.timestamp(calificacion.fechaModificacion()), calificacion.id()) == 1;
	}

	@Override
	public void guardarEvidencia(UUID calificacionId, EvidenciaPedagogica evidencia) {
		jdbc.update("""
			INSERT INTO gestion_academica.evidencia_pedagogica
			(id, calificacion_id, nombre_archivo, archivo_url, mime_type, tamanio_bytes)
			VALUES (?, ?, ?, ?, ?, ?)
			ON CONFLICT (calificacion_id) DO UPDATE
			SET nombre_archivo = EXCLUDED.nombre_archivo, archivo_url = EXCLUDED.archivo_url,
			    mime_type = EXCLUDED.mime_type, tamanio_bytes = EXCLUDED.tamanio_bytes,
			    fecha_carga = now(), activo = TRUE
			""", UUID.randomUUID(), calificacionId, evidencia.nombreArchivo(), evidencia.archivoUrl(),
			evidencia.mimeType(), evidencia.tamanioBytes());
	}

	@Override
	public List<CalificacionEstudiante> listarPorItem(UUID itemEvaluacionId) {
		return jdbc.query("SELECT " + COLUMNAS + """
			FROM gestion_academica.calificacion c
			WHERE c.item_evaluacion_id = ? AND c.activo = TRUE
			ORDER BY c.matricula_id
			""", (rs, row) -> calificacion(rs), itemEvaluacionId);
	}

	@Override
	public List<NotaDeMatricula> listarParaConsolidar(UUID cargaAcademicaId, UUID configuracionCompetenciaId,
		UUID periodoAcademicoId) {
		return jdbc.query("""
			SELECT c.matricula_id, c.valor_numerico, c.valor_cualitativo, c.fecha_registro,
			       i.peso, i.fecha_evaluacion
			FROM gestion_academica.calificacion c
			JOIN gestion_academica.item_evaluacion i ON i.id = c.item_evaluacion_id
			WHERE i.carga_academica_id = ? AND i.configuracion_competencia_id = ?
			  AND i.periodo_academico_id = ? AND i.activo = TRUE AND c.activo = TRUE
			""", (rs, row) -> new NotaDeMatricula(rs.getObject("matricula_id", UUID.class),
			new CalculadoraNotas.NotaItem(ColumnasCalificacion.leer(rs, "valor_numerico", "valor_cualitativo"),
				rs.getBigDecimal("peso"), rs.getObject("fecha_evaluacion", LocalDate.class),
				ColumnasCalificacion.instante(rs, "fecha_registro"))),
			cargaAcademicaId, configuracionCompetenciaId, periodoAcademicoId);
	}

	@Override
	public List<CalificacionDetallada> listarPorMatricula(UUID matriculaId) {
		return jdbc.query("SELECT " + COLUMNAS + """
			     , i.nombre AS item, i.tipo, i.carga_academica_id, i.configuracion_competencia_id,
			       i.periodo_academico_id, e.nombre_archivo, e.archivo_url, e.mime_type, e.tamanio_bytes
			FROM gestion_academica.calificacion c
			JOIN gestion_academica.item_evaluacion i ON i.id = c.item_evaluacion_id AND i.activo = TRUE
			LEFT JOIN gestion_academica.evidencia_pedagogica e ON e.calificacion_id = c.id AND e.activo = TRUE
			WHERE c.matricula_id = ? AND c.activo = TRUE
			ORDER BY i.periodo_academico_id, i.configuracion_competencia_id, i.fecha_evaluacion NULLS LAST, i.nombre
			""", (rs, row) -> new CalificacionDetallada(calificacion(rs), rs.getString("item"),
			TipoItemEvaluacion.valueOf(rs.getString("tipo")), rs.getObject("carga_academica_id", UUID.class),
			rs.getObject("configuracion_competencia_id", UUID.class),
			rs.getObject("periodo_academico_id", UUID.class), evidencia(rs)), matriculaId);
	}

	private static CalificacionEstudiante calificacion(ResultSet rs) throws SQLException {
		return new CalificacionEstudiante(rs.getObject("id", UUID.class),
			rs.getObject("item_evaluacion_id", UUID.class), rs.getObject("matricula_id", UUID.class),
			ColumnasCalificacion.leer(rs, "valor_numerico", "valor_cualitativo"),
			ColumnasCalificacion.instante(rs, "fecha_registro"),
			ColumnasCalificacion.instante(rs, "fecha_modificacion"));
	}

	private static EvidenciaPedagogica evidencia(ResultSet rs) throws SQLException {
		String archivo = rs.getString("nombre_archivo");
		if (archivo == null) return null;
		long tamanio = rs.getLong("tamanio_bytes");
		return new EvidenciaPedagogica(archivo, rs.getString("archivo_url"), rs.getString("mime_type"),
			rs.wasNull() ? null : tamanio);
	}
}
