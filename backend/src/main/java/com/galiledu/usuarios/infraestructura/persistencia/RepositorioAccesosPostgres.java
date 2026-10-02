package com.galiledu.usuarios.infraestructura.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.sql.Timestamp;
import java.util.stream.Collectors;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioAccesos;
import com.galiledu.usuarios.dominio.EstadoAcceso;
import com.galiledu.usuarios.dominio.EstadoUsuario;
import com.galiledu.usuarios.dominio.Rol;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioAccesosPostgres implements RepositorioAccesos {
	private final JdbcOperations jdbc;

	public RepositorioAccesosPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<EstadoAcceso> buscarParaAutenticacion(String nombreUsuario) {
		List<Fila> filas = jdbc.query("""
			SELECT u.username, u.password_hash, u.estado, u.intentos_fallidos, u.bloqueado_hasta,
			       r.nombre AS rol
			FROM seguridad.usuario u
			JOIN personas.persona p ON p.id = u.persona_id AND p.activo = TRUE
			LEFT JOIN seguridad.usuario_rol ur ON ur.usuario_id = u.id
			LEFT JOIN seguridad.rol r ON r.id = ur.rol_id AND r.activo = TRUE
			WHERE u.username = ?
			FOR UPDATE OF u
			""", (rs, row) -> new Fila(rs.getString("username"), rs.getString("password_hash"),
			EstadoUsuario.valueOf(rs.getString("estado")), rs.getInt("intentos_fallidos"),
			rs.getTimestamp("bloqueado_hasta") == null ? null : rs.getTimestamp("bloqueado_hasta").toInstant(),
			rs.getString("rol")), nombreUsuario);
		if (filas.isEmpty()) return Optional.empty();
		Set<Rol> roles = filas.stream().map(Fila::rol).filter(nombre -> nombre != null)
			.map(Rol::new).collect(Collectors.toUnmodifiableSet());
		if (roles.isEmpty()) return Optional.empty();
		Fila primera = filas.getFirst();
		return Optional.of(new EstadoAcceso(primera.username(), primera.hash(), primera.estado(),
			primera.intentos(), primera.bloqueadoHasta(), roles));
	}

	@Override
	public void guardarEstado(EstadoAcceso acceso) {
		jdbc.update("""
			UPDATE seguridad.usuario SET estado = ?::seguridad.estado_usuario,
			intentos_fallidos = ?, bloqueado_hasta = ?,
			ultimo_acceso = CASE WHEN ? = 0 THEN now() ELSE ultimo_acceso END
			WHERE username = ?
			""", acceso.estado().name(), acceso.intentosFallidos(),
			acceso.bloqueadoHasta() == null ? null : Timestamp.from(acceso.bloqueadoHasta()),
			acceso.intentosFallidos(), acceso.nombreUsuario());
	}

	private record Fila(String username, String hash, EstadoUsuario estado,
		int intentos, java.time.Instant bloqueadoHasta, String rol) {}
}
