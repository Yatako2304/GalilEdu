package com.galiledu.estudiantes.infraestructura;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.estudiantes.dominio.DocumentoVinculo;

public interface DocumentoVinculoRepositorio extends JpaRepository<DocumentoVinculo, UUID> {
	/** Proyección con los datos para la ficha: no carga el contenido del archivo. */
	interface Resumen {
		String getTipo();

		String getNombreArchivo();

		Instant getSubidoEn();
	}

	Optional<Resumen> findResumenByVinculoId(UUID vinculoId);
}
