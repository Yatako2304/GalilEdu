package com.galiledu.horarios.infraestructura;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.horarios.dominio.HorarioSeccion;

public interface HorarioSeccionRepositorio extends JpaRepository<HorarioSeccion, UUID> {
	/** El horario de la sección; como mucho hay uno (uq_horario_seccion_seccion). */
	Optional<HorarioSeccion> findBySeccionId(UUID seccionId);
}
