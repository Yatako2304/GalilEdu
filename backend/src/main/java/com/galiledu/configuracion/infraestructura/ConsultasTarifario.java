package com.galiledu.configuracion.infraestructura;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Lecturas de solo consulta del tarifario (HU-34) sobre datos de Pagos (P4), que se leen por SQL sin usar sus
 * entidades.
 */
@Repository
public class ConsultasTarifario {
	private final JdbcClient jdbc;

	public ConsultasTarifario(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * ¿Ya se generó algún compromiso de pago con este tarifario? Desde entonces sus montos y su calendario no se pueden
	 * modificar. Temporal: reemplazar por un método de P4 si lo exponen.
	 */
	public boolean tieneCompromisos(UUID tarifarioId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM pagos.compromiso_pago WHERE tarifario_id = ?)")
				.param(tarifarioId)
				.query(Boolean.class)
				.single();
	}
}
