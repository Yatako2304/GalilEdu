package com.galiledu.pagos.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.pagos.dominio.Descuento;
import com.galiledu.pagos.dominio.EstadoDescuento;

public interface DescuentoRepositorio extends JpaRepository<Descuento, UUID> {
	List<Descuento> findAllByOrderByNombreAsc();

	List<Descuento> findByEstadoOrderByNombreAsc(EstadoDescuento estado);

	boolean existsByNombreIgnoreCase(String nombre);

	boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);
}
