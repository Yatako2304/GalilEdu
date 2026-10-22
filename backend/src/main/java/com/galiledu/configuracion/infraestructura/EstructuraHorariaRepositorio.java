package com.galiledu.configuracion.infraestructura;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.galiledu.configuracion.dominio.EstructuraHoraria;

public interface EstructuraHorariaRepositorio extends JpaRepository<EstructuraHoraria, UUID> {
	List<EstructuraHoraria> findByAnioEscolarIdOrderByCodigo(UUID anioEscolarId);

	/** El código se pasa ya normalizado (sin espacios y en mayúsculas): la BD distingue mayúsculas. */
	boolean existsByAnioEscolarIdAndCodigo(UUID anioEscolarId, String codigo);

	/** Igual que la anterior pero sin contar la propia estructura, para validar el código al editarla. */
	boolean existsByAnioEscolarIdAndCodigoAndIdNot(UUID anioEscolarId, String codigo, UUID id);

	/** La estructura principal del nivel en el año; como mucho hay una (índice único parcial de V7). */
	Optional<EstructuraHoraria> findByAnioEscolarIdAndNivelAndEsPrincipalTrue(UUID anioEscolarId, String nivel);

	long countByAnioEscolarIdAndNivel(UUID anioEscolarId, String nivel);

	/**
	 * El año de una estructura, sin cargar la estructura. Sirve para bloquear el año antes de leer la estructura y
	 * así no trabajar con una copia que otra petición ya cambió.
	 */
	@Query("SELECT e.anioEscolarId FROM EstructuraHoraria e WHERE e.id = :id")
	Optional<UUID> findAnioEscolarIdById(@Param("id") UUID id);

	/**
	 * Bloquea, hasta el fin de la transacción, los cambios de estructuras horarias de ese año: si dos peticiones
	 * llegan a la vez, la segunda espera a que la primera termine y recién entonces valida. Es el mismo mecanismo que
	 * usan las secciones, con otra clave para que ambas no se esperen entre sí. El resultado no se usa; se convierte a
	 * texto porque pg_advisory_xact_lock devuelve void, que JPA no sabe leer.
	 */
	@Query(value = "SELECT CAST(pg_advisory_xact_lock(hashtextextended(CAST(:anioEscolarId AS text) || ':estructuras-horarias', 0)) AS text)", nativeQuery = true)
	String bloquearEstructurasDelAnio(@Param("anioEscolarId") UUID anioEscolarId);
}
