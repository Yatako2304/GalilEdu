package com.galiledu.pagos.infraestructura;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.pagos.dominio.DescuentoAplicado;
import com.galiledu.pagos.dominio.EstadoDescuentoAplicado;

public interface DescuentoAplicadoRepositorio extends JpaRepository<DescuentoAplicado, UUID> {
	Optional<DescuentoAplicado> findByEstudianteIdAndEstado(UUID estudianteId, EstadoDescuentoAplicado estado);

	List<DescuentoAplicado> findByEstudianteIdOrderByAplicadoEnDesc(UUID estudianteId);
}
