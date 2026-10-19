package com.galiledu.usuarios.infraestructura;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.seguridad.VerificadorSesion;
import com.galiledu.usuarios.dominio.EstadoUsuario;

/** Una sesión sigue vigente solo si la cuenta existe, está activa y no fue eliminada. */
@Component
public class VerificadorSesionUsuarios implements VerificadorSesion {
	private final UsuarioRepositorio usuarios;

	public VerificadorSesionUsuarios(UsuarioRepositorio usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean sesionVigente(UUID usuarioId) {
		return usuarios.existsByIdAndEstadoAndEliminadoFalse(usuarioId, EstadoUsuario.ACTIVO);
	}
}
