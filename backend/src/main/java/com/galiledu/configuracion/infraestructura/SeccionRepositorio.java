package com.galiledu.configuracion.infraestructura;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.galiledu.configuracion.dominio.EstadoSeccion;
import com.galiledu.configuracion.dominio.Seccion;

public interface SeccionRepositorio extends JpaRepository<Seccion, UUID> {
	/** Todas las secciones del año (activas e inactivas), por nombre. */
	List<Seccion> findByAnioEscolarIdOrderByNombre(UUID anioEscolarId);

	List<Seccion> findByAnioEscolarIdAndEstado(UUID anioEscolarId, EstadoSeccion estado);

	/** El nombre debe llegar normalizado (mayúsculas, sin espacios): es como se guarda. */
	boolean existsByAnioEscolarIdAndGradoCodigoAndNombre(UUID anioEscolarId, String gradoCodigo, String nombre);

	/** Una sección del año, distinta de la indicada, que tenga a ese usuario como tutor. */
	Optional<Seccion> findFirstByAnioEscolarIdAndTutorIdAndEstadoAndIdNot(UUID anioEscolarId, UUID tutorId,
			EstadoSeccion estado, UUID seccionExcluidaId);

	/**
	 * Bloquea, hasta el fin de la transacción, las asignaciones de tutor de ese año: si dos peticiones llegan a la vez,
	 * la segunda espera a que la primera termine y recién entonces valida. El resultado no se usa; se convierte a texto
	 * porque pg_advisory_xact_lock devuelve void, que JPA no sabe leer.
	 */
	@Query(value = "SELECT CAST(pg_advisory_xact_lock(hashtextextended(CAST(:anioEscolarId AS text), 0)) AS text)", nativeQuery = true)
	String bloquearAsignacionesDelAnio(@Param("anioEscolarId") UUID anioEscolarId);
}
