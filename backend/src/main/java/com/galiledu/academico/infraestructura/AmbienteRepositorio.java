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
}
