package com.galiledu.academico.infraestructura;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.academico.dominio.TablaConversion;

public interface TablaConversionRepositorio extends JpaRepository<TablaConversion, UUID> {
	List<TablaConversion> findAllByOrderByVigenteDesdeDesc();

	/** La vigente en una fecha: la última que empezó en o antes de esa fecha. */
	Optional<TablaConversion> findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(LocalDate fecha);

	boolean existsByVigenteDesde(LocalDate vigenteDesde);
}
