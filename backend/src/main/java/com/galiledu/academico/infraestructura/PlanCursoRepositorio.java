package com.galiledu.academico.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.galiledu.academico.dominio.PlanCurso;

public interface PlanCursoRepositorio extends JpaRepository<PlanCurso, UUID> {
	List<PlanCurso> findByAnioEscolarIdAndGradoCodigo(UUID anioEscolarId, String gradoCodigo);

	boolean existsByAnioEscolarIdAndGradoCodigoAndCursoId(UUID anioEscolarId, String gradoCodigo, UUID cursoId);

	@Query(value = """
			SELECT EXISTS (
			    SELECT 1 FROM academico.item_competencia
			    WHERE plan_curso_id = :planCursoId AND competencia_id = :competenciaId)
			""", nativeQuery = true)
	boolean competenciaTieneItems(@Param("planCursoId") UUID planCursoId,
			@Param("competenciaId") UUID competenciaId);
}
