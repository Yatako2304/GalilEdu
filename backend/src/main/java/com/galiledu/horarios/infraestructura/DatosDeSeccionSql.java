package com.galiledu.horarios.infraestructura;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.horarios.aplicacion.DatosDeSeccion;

/**
 * Lee configuracion.seccion, grado y anio_escolar (y el nombre del tutor) por SQL de solo lectura. Cuando
 * Configuración ofrezca un servicio equivalente, solo se reemplaza esta clase.
 */
@Component
public class DatosDeSeccionSql implements DatosDeSeccion {
	private final JdbcClient jdbc;

	public DatosDeSeccionSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<DatosSeccion> buscar(UUID seccionId) {
		return jdbc.sql("""
				SELECT s.id, s.nombre, s.grado_codigo, g.nombre AS grado_nombre, g.nivel, s.anio_escolar_id,
				       a.estado AS estado_anio, s.ambiente_id, s.tutor_id,
				       concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS tutor_nombre
				FROM configuracion.seccion s
				JOIN configuracion.grado g ON g.codigo = s.grado_codigo
				JOIN configuracion.anio_escolar a ON a.id = s.anio_escolar_id
				LEFT JOIN seguridad.usuario u ON u.id = s.tutor_id
				LEFT JOIN personas.persona p ON p.id = u.persona_id
				WHERE s.id = :seccionId""")
				.param("seccionId", seccionId)
				.query((r, fila) -> new DatosSeccion(r.getObject("id", UUID.class), r.getString("nombre"),
						r.getString("grado_codigo"), r.getString("grado_nombre"), r.getString("nivel"),
						r.getObject("anio_escolar_id", UUID.class), r.getString("estado_anio"),
						r.getObject("ambiente_id", UUID.class), r.getObject("tutor_id", UUID.class),
						r.getString("tutor_nombre")))
				.optional();
	}
}
