package com.galiledu.usuarios.infraestructura;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.seguridad.VerificadorSesion;
import com.galiledu.usuarios.dominio.EstadoUsuario;
import com.galiledu.usuarios.dominio.Usuario;

/**
 * Una sesión sigue vigente solo si la cuenta existe, está activa, no fue eliminada y el token se emitió después del
 * último cambio de contraseña.
 */
@Component
public class VerificadorSesionUsuarios implements VerificadorSesion {
	private final UsuarioRepositorio usuarios;

	public VerificadorSesionUsuarios(UsuarioRepositorio usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean sesionVigente(UUID usuarioId, Instant emitidoEn) {
		return usuarios.sesionVigente(usuarioId, EstadoUsuario.ACTIVO, emitidoEn == null ? Instant.EPOCH : emitidoEn);
	}

	@Override
	@Transactional(readOnly = true)
	public Set<String> rolesVigentes(UUID usuarioId) {
		return usuarios.findById(usuarioId)
				.filter(u -> !u.isEliminado() && u.getEstado() == EstadoUsuario.ACTIVO)
				.map(Usuario::getRoles)
				.map(roles -> roles.stream().map(Enum::name).collect(Collectors.toSet()))
				.orElse(Set.of());
	}
}
