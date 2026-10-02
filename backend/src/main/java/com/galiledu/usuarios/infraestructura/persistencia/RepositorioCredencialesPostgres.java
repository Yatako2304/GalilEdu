package com.galiledu.usuarios.infraestructura.persistencia;

import java.util.Optional;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioCredenciales;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioCredencialesPostgres implements RepositorioCredenciales {
	private final JdbcOperations jdbc;

	public RepositorioCredencialesPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<String> buscarHashActivoParaActualizar(String nombreUsuario) {
		return jdbc.query("""
			SELECT u.password_hash
			FROM seguridad.usuario u
			JOIN personas.persona p ON p.id = u.persona_id AND p.activo = TRUE
			WHERE u.username = ? AND u.estado = 'ACTIVO'::seguridad.estado_usuario
			FOR UPDATE OF u
			""", (rs, row) -> rs.getString("password_hash"), nombreUsuario)
			.stream().findFirst();
	}

	@Override
	public boolean actualizarHash(String nombreUsuario, String hashAnterior, String hashNuevo) {
		return jdbc.update("""
			UPDATE seguridad.usuario SET password_hash = ?
			WHERE username = ? AND password_hash = ?
			  AND estado = 'ACTIVO'::seguridad.estado_usuario
			""", hashNuevo, nombreUsuario, hashAnterior) == 1;
	}
}
