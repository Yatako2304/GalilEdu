package com.galiledu.configuracion.infraestructura;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.galiledu.configuracion.dominio.Tarifario;

public interface TarifarioRepositorio extends JpaRepository<Tarifario, UUID> {
	/** Hay uno por año (pagos.tarifario.anio_escolar_id es UNIQUE). */
	Optional<Tarifario> findByAnioEscolarId(UUID anioEscolarId);

	/** El tarifario del año con ese número (por ejemplo, el del año anterior, para proponer los valores de P19). */
	@Query("SELECT t FROM Tarifario t WHERE t.anioEscolarId = (SELECT a.id FROM AnioEscolar a WHERE a.anio = :anio)")
	Optional<Tarifario> findByAnio(int anio);
}
