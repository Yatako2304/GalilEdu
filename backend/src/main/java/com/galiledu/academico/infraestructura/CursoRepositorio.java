package com.galiledu.academico.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.academico.dominio.Curso;

public interface CursoRepositorio extends JpaRepository<Curso, UUID> {
	List<Curso> findByAreaIdOrderByNombreAsc(UUID areaId);

	boolean existsByNombreIgnoreCase(String nombre);

	boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);
}
