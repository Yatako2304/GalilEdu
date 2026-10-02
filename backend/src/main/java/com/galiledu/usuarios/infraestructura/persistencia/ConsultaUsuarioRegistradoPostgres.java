package com.galiledu.usuarios.infraestructura.persistencia;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.galiledu.usuarios.aplicacion.UsuarioRegistrado;
import com.galiledu.usuarios.aplicacion.puertos.ConsultaUsuarioRegistrado;
import com.galiledu.usuarios.dominio.EstadoUsuario;
import com.galiledu.usuarios.dominio.Rol;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

/** Adaptador de lectura: el correo pertenece a Persona, no a Usuario. */
@Repository
public class ConsultaUsuarioRegistradoPostgres implements ConsultaUsuarioRegistrado {
	private static final String BUSCAR_POR_CORREO = """
		SELECT u.id, u.persona_id, p.correo, u.google_sub, u.estado,
		       r.nombre AS rol_nombre
		FROM seguridad.usuario u
		JOIN personas.persona p ON p.id = u.persona_id
		LEFT JOIN seguridad.usuario_rol ur ON ur.usuario_id = u.id
		LEFT JOIN seguridad.rol r ON r.id = ur.rol_id AND r.activo = TRUE
		WHERE p.correo = ?
		ORDER BY r.nombre
		""";

	private final JdbcOperations jdbc;

	public ConsultaUsuarioRegistradoPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<UsuarioRegistrado> buscarPorCorreo(String correo) {
		if (correo == null || correo.isBlank()) {
			return Optional.empty();
		}
		var filas = jdbc.query(BUSCAR_POR_CORREO, (resultado, fila) -> new FilaUsuario(
			resultado.getObject("id", UUID.class),
			resultado.getObject("persona_id", UUID.class),
			resultado.getString("correo"),
			resultado.getString("google_sub"),
			EstadoUsuario.valueOf(resultado.getString("estado")),
			resultado.getString("rol_nombre")), correo);
		if (filas.isEmpty()) {
			return Optional.empty();
		}
		FilaUsuario primera = filas.getFirst();
		Set<Rol> roles = filas.stream().map(FilaUsuario::nombreRol)
			.filter(nombre -> nombre != null && !nombre.isBlank())
			.map(Rol::new).collect(Collectors.toUnmodifiableSet());
		return Optional.of(new UsuarioRegistrado(primera.id(), primera.personaId(),
			primera.correo(), primera.googleSub(), primera.estado(), roles));
	}

	private record FilaUsuario(UUID id, UUID personaId, String correo,
		String googleSub, EstadoUsuario estado, String nombreRol) {}
}
