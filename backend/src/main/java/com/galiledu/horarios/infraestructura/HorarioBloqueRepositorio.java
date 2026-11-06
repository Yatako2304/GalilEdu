package com.galiledu.horarios.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.horarios.dominio.HorarioBloque;

public interface HorarioBloqueRepositorio extends JpaRepository<HorarioBloque, UUID> {
	List<HorarioBloque> findByHorarioSeccionId(UUID horarioSeccionId);

	long countByHorarioSeccionIdAndPlanCursoId(UUID horarioSeccionId, UUID planCursoId);
}
