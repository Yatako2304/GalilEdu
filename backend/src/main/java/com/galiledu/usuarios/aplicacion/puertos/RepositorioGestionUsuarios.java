package com.galiledu.usuarios.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.galiledu.usuarios.dominio.DatosPersonales;

public interface RepositorioGestionUsuarios {
	UUID crearPersona(DatosPersonales datos, Set<String> perfiles, List<UUID> apoderados);
	String crearCuenta(UUID personaId, String hashContrasena, Set<String> roles);
	Optional<PersonaRegistrada> buscarPersona(UUID id);
	boolean actualizarPersona(UUID id, DatosPersonales datos);
	boolean desactivarPersona(UUID id);
	boolean reactivarPersona(UUID id);
	UUID vincularApoderado(UUID estudianteId, UUID apoderadoId, String parentesco);

	record PersonaRegistrada(UUID id, DatosPersonales datos, boolean activa,
		Set<String> perfiles, List<UUID> apoderados) {}
}
