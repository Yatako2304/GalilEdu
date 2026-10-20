package com.galiledu.academico.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.galiledu.academico.dominio.Competencia;

public interface CompetenciaRepositorio extends JpaRepository<Competencia, UUID> {
	List<Competencia> findByAreaIdOrderByOrdenAscNombreAsc(UUID areaId);

	boolean existsByAreaIdAndNombreIgnoreCase(UUID areaId, String nombre);

	boolean existsByAreaIdAndNombreIgnoreCaseAndIdNot(UUID areaId, String nombre, UUID id);

	@Query("SELECT COALESCE(MAX(c.orden), 0) FROM Competencia c WHERE c.areaId = ?1")
	short maximoOrden(UUID areaId);
}
