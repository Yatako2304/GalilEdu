package com.galiledu.asistencia.infraestructura;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.galiledu.asistencia.dominio.CorreccionAsistencia;

public interface CorreccionAsistenciaRepositorio extends JpaRepository<CorreccionAsistencia, UUID> {
	/** Etiqueta "Modificado" de AS-06: las asistencias que tienen al menos una corrección. */
	@Query("SELECT DISTINCT c.asistenciaEstudianteId FROM CorreccionAsistencia c WHERE c.asistenciaEstudianteId IN :ids")
	List<UUID> conCorrecciones(Collection<UUID> ids);
}
