package com.galiledu.configuracion.infraestructura;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.configuracion.aplicacion.MatriculasDeSecciones;

/**
 * Lee matricula.matricula y matricula.cambio_seccion por SQL de solo lectura. Cuando el módulo de matrícula ofrezca un
 * servicio equivalente, solo se reemplaza esta clase.
 */
@Component
public class MatriculasDeSeccionesSql implements MatriculasDeSecciones {
	private final JdbcClient jdbc;

	public MatriculasDeSeccionesSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public boolean tieneMatriculasOCambios(UUID seccionId) {
		// Cualquier estado de matrícula (incluidos RETIRADO y ANULADA) y la sección como origen o destino de un cambio.
		return jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM matricula.matricula WHERE seccion_id = :seccionId)
				    OR EXISTS (SELECT 1 FROM matricula.cambio_seccion
				               WHERE seccion_origen_id = :seccionId OR seccion_destino_id = :seccionId)""")
				.param("seccionId", seccionId)
				.query(Boolean.class).single();
	}
}
