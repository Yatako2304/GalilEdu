package com.galiledu.usuarios.infraestructura;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.usuarios.aplicacion.VinculosUsuario;

/**
 * Lee los vínculos familiares (estudiantes.*) y las secciones (configuracion.seccion) por SQL de solo lectura. La
 * situación del estudiante repite la fórmula de {@code Estudiante.situacion}: retirado, matriculado en su año académico
 * o inscrito.
 */
@Component
public class VinculosUsuarioSql implements VinculosUsuario {
	private final JdbcClient jdbc;

	public VinculosUsuarioSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<EstudianteACargo> estudiantesACargo(UUID personaId) {
		return jdbc.sql("""
				SELECT e.id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS nombre,
				       p.numero_documento, g.nombre AS grado, e.anio_academico,
				       CASE WHEN e.estado = 'RETIRADO' THEN 'RETIRADO'
				            WHEN EXISTS (SELECT 1 FROM matricula.matricula m
				                         JOIN configuracion.anio_escolar a ON a.id = m.anio_escolar_id
				                         WHERE m.estudiante_id = e.id AND a.anio = e.anio_academico
				                           AND m.estado = 'MATRICULADO') THEN 'MATRICULADO'
				            ELSE 'INSCRITO' END AS situacion,
				       v.parentesco, v.relacion, v.vive_con_estudiante, v.tiene_patria_potestad,
				       v.es_responsable_matricula, d.tipo, d.nombre_archivo, d.subido_en
				FROM estudiantes.vinculo_familiar v
				JOIN estudiantes.estudiante e ON e.id = v.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				JOIN configuracion.grado g ON g.codigo = e.grado_codigo
				LEFT JOIN estudiantes.documento_vinculo d ON d.vinculo_id = v.id
				WHERE v.persona_id = :personaId AND NOT e.eliminado
				ORDER BY e.anio_academico DESC, nombre""")
				.param("personaId", personaId)
				.query((rs, fila) -> {
					Timestamp subidoEn = rs.getTimestamp("subido_en");
					var acreditacion = rs.getString("tipo") == null
							? null
							: new Acreditacion(rs.getString("tipo"), rs.getString("nombre_archivo"),
									subidoEn.toInstant());
					return new EstudianteACargo(rs.getObject("id", UUID.class), rs.getString("nombre"),
							rs.getString("numero_documento"), rs.getString("grado"), rs.getInt("anio_academico"),
							rs.getString("situacion"), rs.getString("parentesco"), rs.getString("relacion"),
							rs.getBoolean("vive_con_estudiante"), rs.getBoolean("tiene_patria_potestad"),
							rs.getBoolean("es_responsable_matricula"), acreditacion);
				})
				.list();
	}

	@Override
	public List<SeccionTutoria> tutorDe(UUID usuarioId) {
		return jdbc.sql("""
				SELECT s.id AS seccion_id, g.nombre || ' ' || s.nombre AS seccion,
				       CASE WHEN s.tutor_id = :usuarioId THEN 'TUTOR' ELSE 'COTUTOR' END AS cargo
				FROM configuracion.seccion s
				JOIN configuracion.anio_escolar a ON a.id = s.anio_escolar_id
				JOIN configuracion.grado g ON g.codigo = s.grado_codigo
				WHERE a.estado = 'ACTIVO' AND s.estado = 'ACTIVA'
				  AND (s.tutor_id = :usuarioId OR s.cotutor_id = :usuarioId)
				ORDER BY g.orden, s.nombre""")
				.param("usuarioId", usuarioId)
				.query(SeccionTutoria.class)
				.list();
	}
}
