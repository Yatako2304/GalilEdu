package com.galiledu.horarios.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.horarios.aplicacion.HijosDelApoderado;

/**
 * Lee seguridad.usuario, estudiantes.vinculo_familiar, matricula.matricula y configuracion.seccion por SQL de solo
 * lectura (como {@code DatosDeSeccionSql}). Solo cuentan las matrículas MATRICULADO de estudiantes no eliminados, y
 * los vínculos de una persona no fallecida.
 */
@Component
public class HijosDelApoderadoSql implements HijosDelApoderado {
	private final JdbcClient jdbc;

	public HijosDelApoderadoSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<MatriculaDeHijo> matriculasDeHijos(UUID usuarioId) {
		return jdbc.sql("""
				SELECT DISTINCT e.id AS estudiante_id,
				       concat_ws(' ', pe.nombres, pe.apellido_paterno, pe.apellido_materno) AS nombre,
				       a.anio, s.id AS seccion_id, s.nombre AS seccion, g.nombre AS grado_nombre, g.nivel,
				       concat_ws(' ', pt.nombres, pt.apellido_paterno, pt.apellido_materno) AS tutor_nombre,
				       pt.celular AS tutor_celular, pt.correo AS tutor_correo, s.tutor_id
				FROM seguridad.usuario u
				JOIN personas.persona pa ON pa.id = u.persona_id AND NOT pa.fallecido
				JOIN estudiantes.vinculo_familiar v ON v.persona_id = pa.id
				JOIN estudiantes.estudiante e ON e.id = v.estudiante_id AND NOT e.eliminado
				JOIN personas.persona pe ON pe.id = e.persona_id
				JOIN matricula.matricula m ON m.estudiante_id = e.id AND m.estado = 'MATRICULADO'
				JOIN configuracion.seccion s ON s.id = m.seccion_id
				JOIN configuracion.grado g ON g.codigo = s.grado_codigo
				JOIN configuracion.anio_escolar a ON a.id = s.anio_escolar_id
				LEFT JOIN seguridad.usuario ut ON ut.id = s.tutor_id
				LEFT JOIN personas.persona pt ON pt.id = ut.persona_id
				WHERE u.id = :usuarioId
				ORDER BY nombre, a.anio DESC""")
				.param("usuarioId", usuarioId)
				.query((r, fila) -> new MatriculaDeHijo(r.getObject("estudiante_id", UUID.class), r.getString("nombre"),
						r.getInt("anio"), r.getObject("seccion_id", UUID.class), r.getString("seccion"),
						r.getString("grado_nombre"), r.getString("nivel"),
						r.getObject("tutor_id") == null ? null : r.getString("tutor_nombre"),
						r.getString("tutor_celular"), r.getString("tutor_correo")))
				.list();
	}
}
