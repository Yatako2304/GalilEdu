package com.galiledu.horarios.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.horarios.aplicacion.DtosHorarios.Referencia;
import com.galiledu.horarios.aplicacion.ResponsablesElegibles;

/**
 * Lee seguridad.usuario, usuario_rol y personas.persona por SQL de solo lectura. Cuando el módulo de usuarios ofrezca
 * un servicio equivalente, solo se reemplaza esta clase.
 */
@Component
public class ResponsablesElegiblesSql implements ResponsablesElegibles {
	/** EXISTS en vez de JOIN: quien tiene los roles DOCENTE y TUTOR a la vez aparece una sola vez. */
	private static final String ELEGIBLES = """
			FROM seguridad.usuario u
			JOIN personas.persona p ON p.id = u.persona_id
			WHERE u.estado = 'ACTIVO' AND NOT u.eliminado
			  AND EXISTS (SELECT 1 FROM seguridad.usuario_rol r
			              WHERE r.usuario_id = u.id AND r.rol_codigo IN ('DOCENTE', 'TUTOR'))""";

	private final JdbcClient jdbc;

	public ResponsablesElegiblesSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<Referencia> elegibles() {
		return jdbc.sql("""
				SELECT u.id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS nombre
				""" + ELEGIBLES + " ORDER BY nombre")
				.query((r, fila) -> new Referencia(r.getObject("id", UUID.class), r.getString("nombre"))).list();
	}

	@Override
	public boolean existe(UUID usuarioId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 " + ELEGIBLES + " AND u.id = :id)").param("id", usuarioId)
				.query(Boolean.class).single();
	}
}
