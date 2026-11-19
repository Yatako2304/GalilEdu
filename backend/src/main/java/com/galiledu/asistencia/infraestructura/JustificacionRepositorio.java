package com.galiledu.asistencia.infraestructura;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.asistencia.dominio.Justificacion;

public interface JustificacionRepositorio extends JpaRepository<Justificacion, UUID> {
	List<Justificacion> findByAsistenciaEstudianteIdIn(Collection<UUID> asistencias);

	Optional<Justificacion> findByAsistenciaEstudianteId(UUID asistenciaEstudianteId);

	boolean existsByAsistenciaEstudianteIdIn(Collection<UUID> asistencias);
}
