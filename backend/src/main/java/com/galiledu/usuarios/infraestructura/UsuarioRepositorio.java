package com.galiledu.usuarios.infraestructura;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.galiledu.usuarios.dominio.EstadoUsuario;
import com.galiledu.usuarios.dominio.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {
	Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);

	Optional<Usuario> findByPersonaId(UUID personaId);

	/** HU-4: la cuenta (no eliminada) de la persona con ese correo; el correo es único entre personas. */
	Optional<Usuario> findByPersonaCorreoIgnoreCaseAndEliminadoFalse(String correo);

	boolean existsByPersonaId(UUID personaId);

	boolean existsByIdAndEstadoAndEliminadoFalse(UUID id, EstadoUsuario estado);

	/** Cuenta activa, no eliminada y con el token emitido después del último cambio de contraseña (HU-5). */
	@Query("""
			select count(u) > 0 from Usuario u
			where u.id = :id and u.estado = :estado and u.eliminado = false
			  and (u.sesionesDesde is null or u.sesionesDesde <= :emitidoEn)""")
	boolean sesionVigente(@Param("id") UUID id, @Param("estado") EstadoUsuario estado,
			@Param("emitidoEn") Instant emitidoEn);
}
