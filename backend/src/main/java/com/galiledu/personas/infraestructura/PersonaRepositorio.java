package com.galiledu.personas.infraestructura;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.galiledu.personas.dominio.Persona;

public interface PersonaRepositorio extends JpaRepository<Persona, UUID> {
	Optional<Persona> findByNumeroDocumento(String numeroDocumento);

	@Query("select p from Persona p where lower(p.correo) = lower(:correo)")
	Optional<Persona> buscarPorCorreo(@Param("correo") String correo);
}
