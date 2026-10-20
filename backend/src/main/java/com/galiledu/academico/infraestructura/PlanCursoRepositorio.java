package com.galiledu.academico.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.academico.dominio.PlanCurso;

public interface PlanCursoRepositorio extends JpaRepository<PlanCurso, UUID> {
	List<PlanCurso> findByAnioEscolarIdAndGradoCodigo(UUID anioEscolarId, String gradoCodigo);

	boolean existsByAnioEscolarIdAndGradoCodigoAndCursoId(UUID anioEscolarId, String gradoCodigo, UUID cursoId);
}
