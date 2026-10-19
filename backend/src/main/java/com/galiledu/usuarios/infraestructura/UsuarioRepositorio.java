package com.galiledu.usuarios.infraestructura;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.galiledu.usuarios.dominio.EstadoUsuario;
import com.galiledu.usuarios.dominio.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {
	Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);

	Optional<Usuario> findByPersonaId(UUID personaId);

	boolean existsByPersonaId(UUID personaId);

	boolean existsByIdAndEstadoAndEliminadoFalse(UUID id, EstadoUsuario estado);
}
