package com.galiledu.asistencia.infraestructura;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Nombre visible de quien registró o corrigió la asistencia ("Selene Curo"): primer nombre y apellido paterno. */
@Component
public class NombresDeUsuario {
	private final JdbcClient jdbc;

	public NombresDeUsuario(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public String de(UUID usuarioId) {
		if (usuarioId == null) {
			return null;
		}
		Optional<String> nombre = jdbc.sql("""
				SELECT split_part(p.nombres, ' ', 1) || ' ' || p.apellido_paterno
				FROM seguridad.usuario u JOIN personas.persona p ON p.id = u.persona_id
				WHERE u.id = ?""").param(usuarioId).query(String.class).optional();
		return nombre.orElse(null);
	}
}
