package com.galiledu.academica.aplicacion;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Cuenta activa que ejecuta un caso de uso académico; personaId identifica al docente. */
public record ActorAcademico(UUID usuarioId, UUID personaId, Set<String> roles) {
	public static final String ADMINISTRADOR = "ADMINISTRADOR";

	public ActorAcademico {
		Objects.requireNonNull(usuarioId, "El id de usuario es obligatorio");
		Objects.requireNonNull(personaId, "El id de persona es obligatorio");
		roles = Set.copyOf(Objects.requireNonNull(roles, "Los roles son obligatorios"));
	}

	public boolean esAdministrador() {
		return roles.contains(ADMINISTRADOR);
	}
}
