package com.galiledu.pagos.infraestructura;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.galiledu.pagos.dominio.OrdenPago;

public interface OrdenPagoRepositorio extends JpaRepository<OrdenPago, UUID> {
	Optional<OrdenPago> findByCodigo(String codigo);

	/**
	 * Bloquea la orden mientras se resuelve: si la pasarela avisa dos veces a la vez, la segunda espera y encuentra la
	 * orden ya resuelta (RF-103).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT o FROM OrdenPago o WHERE o.codigo = :codigo")
	Optional<OrdenPago> bloquearPorCodigo(String codigo);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT o FROM OrdenPago o WHERE o.referenciaPasarela = :referencia")
	Optional<OrdenPago> bloquearPorReferencia(String referencia);

	@Query("""
			SELECT o.codigo FROM OrdenPago o
			WHERE o.estado = com.galiledu.pagos.dominio.EstadoOrdenPago.EN_PROCESO AND o.expiraEn < :ahora""")
	List<String> vencidas(OffsetDateTime ahora);
}
