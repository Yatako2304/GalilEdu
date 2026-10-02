package com.galiledu.usuarios.aplicacion;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.galiledu.usuarios.dominio.EstadoUsuario;
import com.galiledu.usuarios.dominio.Rol;

/** Proyección de la cuenta persistida; no representa credenciales locales. */
public record UsuarioRegistrado(UUID id, UUID personaId, String correo,
	String googleSub, EstadoUsuario estado, Set<Rol> roles) {
	public UsuarioRegistrado {
		Objects.requireNonNull(id, "El id de usuario es obligatorio");
		Objects.requireNonNull(personaId, "El id de persona es obligatorio");
		if (correo == null || correo.isBlank()) {
			throw new IllegalArgumentException("El correo es obligatorio");
		}
		Objects.requireNonNull(estado, "El estado es obligatorio");
		roles = Set.copyOf(Objects.requireNonNull(roles, "Los roles son obligatorios"));
	}
}
