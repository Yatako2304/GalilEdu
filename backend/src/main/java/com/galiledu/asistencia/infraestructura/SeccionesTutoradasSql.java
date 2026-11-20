package com.galiledu.asistencia.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.asistencia.aplicacion.SeccionesTutoradas;

/**
 * Lee configuracion.seccion y matricula.matricula por SQL de solo lectura. La condición de "tutor de" es la misma de
 * {@code VinculosUsuarioSql.tutorDe} (Usuarios): sección ACTIVA del año ACTIVO, como tutor o cotutor.
 */
@Component
public class SeccionesTutoradasSql implements SeccionesTutoradas {
	private final JdbcClient jdbc;

	public SeccionesTutoradasSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<SeccionTutorada> delUsuario(UUID usuarioId) {
		return jdbc.sql("""
				SELECT s.id, a.id, a.anio, g.nombre, s.nombre, g.nivel,
				       CASE WHEN s.tutor_id = :usuarioId THEN 'TUTOR' ELSE 'COTUTOR' END
				FROM configuracion.seccion s
				JOIN configuracion.anio_escolar a ON a.id = s.anio_escolar_id
				JOIN configuracion.grado g ON g.codigo = s.grado_codigo
				WHERE a.estado = 'ACTIVO' AND s.estado = 'ACTIVA'
				  AND (s.tutor_id = :usuarioId OR s.cotutor_id = :usuarioId)
				ORDER BY g.orden, s.nombre""")
				.param("usuarioId", usuarioId)
				.query((rs, i) -> new SeccionTutorada(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
						rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)))
				.list();
	}

	@Override
	public List<EstudianteDeSeccion> estudiantes(UUID seccionId) {
		return jdbc.sql("""
				SELECT m.id, e.id, p.nombres, concat_ws(' ', p.apellido_paterno, p.apellido_materno),
				       p.numero_documento
				FROM matricula.matricula m
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				WHERE m.seccion_id = :seccionId AND m.estado = 'MATRICULADO'
				ORDER BY p.apellido_paterno, p.apellido_materno, p.nombres""")
				.param("seccionId", seccionId)
				.query((rs, i) -> new EstudianteDeSeccion(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
						rs.getString(3), rs.getString(4), rs.getString(5)))
				.list();
	}
}
