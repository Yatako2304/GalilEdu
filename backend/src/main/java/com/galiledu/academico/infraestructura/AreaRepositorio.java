package com.galiledu.academico.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.galiledu.academico.dominio.Area;

public interface AreaRepositorio extends JpaRepository<Area, UUID> {
	List<Area> findAllByOrderByOrdenAscNombreAsc();

	boolean existsByNombreIgnoreCase(String nombre);

	boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);

	@Query("SELECT COALESCE(MAX(a.orden), 0) FROM Area a")
	short maximoOrden();
}
