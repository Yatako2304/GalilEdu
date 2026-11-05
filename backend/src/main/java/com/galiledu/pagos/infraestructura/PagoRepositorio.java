package com.galiledu.pagos.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.pagos.dominio.Pago;

public interface PagoRepositorio extends JpaRepository<Pago, UUID> {
	List<Pago> findByOrdenPagoId(UUID ordenPagoId);
}
