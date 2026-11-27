package com.galiledu.pagos.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.pagos.dominio.Pago;

public interface PagoRepositorio extends JpaRepository<Pago, UUID> {
	List<Pago> findByOrdenPagoId(UUID ordenPagoId);

	/** Pago en caja con el mismo medio y número de operación (para no registrarlo dos veces). */
	boolean existsByOrdenPagoIdIsNullAndRegistradoPorIsNotNullAndReferencia(String referencia);
}
