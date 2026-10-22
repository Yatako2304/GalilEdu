package com.galiledu.configuracion.infraestructura;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.configuracion.aplicacion.DirectorioDocentes;

/**
 * Lee los usuarios (seguridad.usuario, usuario_rol y personas.persona) por SQL de solo lectura. Cuando el módulo de
 * usuarios ofrezca un servicio equivalente, solo se reemplaza esta clase.
 */
@Component
public class DirectorioDocentesSql implements DirectorioDocentes {
	private final JdbcClient jdbc;

	public DirectorioDocentesSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<Docente> elegiblesParaTutoria() {
		// EXISTS en vez de JOIN: quien tiene los roles DOCENTE y TUTOR a la vez aparece una sola vez.
		return jdbc.sql("""
				SELECT u.id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS nombre
				FROM seguridad.usuario u
				JOIN personas.persona p ON p.id = u.persona_id
				WHERE u.estado = 'ACTIVO' AND NOT u.eliminado
				  AND EXISTS (SELECT 1 FROM seguridad.usuario_rol r
				              WHERE r.usuario_id = u.id AND r.rol_codigo IN ('DOCENTE', 'TUTOR'))
				ORDER BY nombre""")
				.query(Docente.class).list();
	}

	@Override
	public Map<UUID, String> nombresDe(Collection<UUID> usuarioIds) {
		Map<UUID, String> nombresPorUsuarioId = new HashMap<>();
		if (usuarioIds.isEmpty()) {
			return nombresPorUsuarioId;
		}
		List<Docente> usuarios = jdbc.sql("""
				SELECT u.id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS nombre
				FROM seguridad.usuario u
				JOIN personas.persona p ON p.id = u.persona_id
				WHERE u.id IN (:usuarioIds)""")
				.param("usuarioIds", usuarioIds)
				.query(Docente.class).list();
		for (Docente usuario : usuarios) {
			nombresPorUsuarioId.put(usuario.id(), usuario.nombre());
		}
		return nombresPorUsuarioId;
	}
}
