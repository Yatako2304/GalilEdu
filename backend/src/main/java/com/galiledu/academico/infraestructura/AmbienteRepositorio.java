package com.galiledu.academico.infraestructura;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.galiledu.academico.dominio.Ambiente;

public interface AmbienteRepositorio extends JpaRepository<Ambiente, UUID> {
	List<Ambiente> findAllByOrderByNombreAsc();

	/** Incluye los inactivos: un ambiente desactivado se puede reactivar, así que su nombre sigue ocupado. */
	boolean existsByNombreIgnoreCase(String nombre);

	boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);

	/**
	 * Códigos que empiezan con "{prefijo}-" (p. ej. "A" trae A-101 y A-205, pero no AUD-01). Puede traer alguno que no
	 * sea numérico (A-12X): {@code CodigoAmbiente.siguiente} descarta los que no siguen el formato.
	 */
	@Query("SELECT a.codigo FROM Ambiente a WHERE a.codigo LIKE CONCAT(:prefijo, '-%')")
	List<String> codigosConPrefijo(@Param("prefijo") String prefijo);

	/**
	 * Serializa, hasta el fin de la transacción, a quien crea o renombra ambientes: la segunda petición espera a que la
	 * primera confirme y recién entonces valida el nombre. Hace falta porque la BD no tiene restricción de nombre, y la
	 * del código solo frena a dos altas del mismo tipo (A-… contra E-… no chocan). Es el mismo mecanismo que usan las
	 * secciones y las estructuras horarias, con otra clave. El resultado no se usa; se convierte a texto porque
	 * pg_advisory_xact_lock devuelve void, que JPA no sabe leer.
	 */
	@Query(value = "SELECT CAST(pg_advisory_xact_lock(hashtextextended('configuracion.ambiente:nombre', 0)) AS text)", nativeQuery = true)
	String bloquearNombres();
}
