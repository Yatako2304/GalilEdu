package com.galiledu.estudiantes.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.galiledu.estudiantes.dominio.Estudiante;

public interface EstudianteRepositorio extends JpaRepository<Estudiante, UUID>, JpaSpecificationExecutor<Estudiante> {
	boolean existsByPersonaId(UUID personaId);

	/** Estudiantes vinculados a una persona (padre, madre o apoderado): sirve para detectar hermanos. */
	@Query("""
		select distinct v.estudiante from VinculoFamiliar v
		where v.persona.id = :personaId and v.estudiante.eliminado = false
		""")
	List<Estudiante> vinculadosA(@Param("personaId") UUID personaId);
}
