package com.galiledu.estudiantes.infraestructura;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.galiledu.estudiantes.dominio.Estudiante;

public interface EstudianteRepositorio extends JpaRepository<Estudiante, UUID>, JpaSpecificationExecutor<Estudiante> {
	boolean existsByPersonaId(UUID personaId);

	Optional<Estudiante> findByPersonaId(UUID personaId);

	/** Estudiantes vinculados a una persona (padre, madre o apoderado): sirve para detectar hermanos. */
	@Query("""
			select distinct v.estudiante from VinculoFamiliar v
			where v.persona.id = :personaId and v.estudiante.eliminado = false
			""")
	List<Estudiante> vinculadosA(@Param("personaId") UUID personaId);

	/** Si la persona es responsable de matrícula de otro estudiante (no eliminado) además del indicado. */
	@Query("""
			select count(v) > 0 from VinculoFamiliar v
			where v.persona.id = :personaId and v.esResponsableMatricula = true
			  and v.estudiante.eliminado = false and v.estudiante.id <> :estudianteId
			""")
	boolean esResponsableDeOtro(@Param("personaId") UUID personaId, @Param("estudianteId") UUID estudianteId);
}
