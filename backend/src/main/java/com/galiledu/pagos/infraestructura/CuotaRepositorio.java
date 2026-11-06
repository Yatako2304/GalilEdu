package com.galiledu.pagos.infraestructura;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.galiledu.pagos.dominio.Cuota;

public interface CuotaRepositorio extends JpaRepository<Cuota, UUID> {

	/** Bloquea las cuotas mientras se arma la orden: dos órdenes simultáneas no pueden tomar la misma cuota. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT q FROM Cuota q WHERE q.id IN :ids")
	List<Cuota> bloquear(Collection<UUID> ids);

	/** Cuotas de los compromisos VIGENTES de esos estudiantes. */
	@Query("""
			SELECT q FROM Cuota q JOIN FETCH q.compromiso c
			WHERE c.estudianteId IN :estudianteIds AND c.estado = com.galiledu.pagos.dominio.EstadoCompromiso.VIGENTE
			ORDER BY q.fechaVencimiento""")
	List<Cuota> deEstudiantes(Collection<UUID> estudianteIds);
}
