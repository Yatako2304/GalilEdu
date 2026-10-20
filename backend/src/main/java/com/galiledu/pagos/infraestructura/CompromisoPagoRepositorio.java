package com.galiledu.pagos.infraestructura;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.pagos.dominio.CompromisoPago;

public interface CompromisoPagoRepositorio extends JpaRepository<CompromisoPago, UUID> {
	List<CompromisoPago> findByEstudianteId(UUID estudianteId);

	Optional<CompromisoPago> findByMatriculaId(UUID matriculaId);

	Optional<CompromisoPago> findByEstudianteIdAndAnioEscolarId(UUID estudianteId, UUID anioEscolarId);
}
