package com.galiledu.horarios.infraestructura;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.horarios.aplicacion.CalendarioDelAnio;

/**
 * Lee configuracion.anio_escolar y periodo_academico por SQL de solo lectura. Cuando Configuración ofrezca un servicio
 * equivalente, solo se reemplaza esta clase.
 */
@Component
public class CalendarioDelAnioSql implements CalendarioDelAnio {
	private final JdbcClient jdbc;

	public CalendarioDelAnioSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<AnioConPeriodos> deAnio(UUID anioEscolarId) {
		List<PeriodoDelAnio> periodos = jdbc.sql("""
				SELECT numero, nombre, fecha_inicio, fecha_fin
				FROM configuracion.periodo_academico
				WHERE anio_escolar_id = :anioId
				ORDER BY numero""").param("anioId", anioEscolarId)
				.query((r, fila) -> new PeriodoDelAnio(r.getInt("numero"), r.getString("nombre"),
						r.getObject("fecha_inicio", LocalDate.class), r.getObject("fecha_fin", LocalDate.class)))
				.list();
		return jdbc.sql("""
				SELECT id, anio, estado, fecha_inicio, fecha_fin
				FROM configuracion.anio_escolar
				WHERE id = :anioId""").param("anioId", anioEscolarId)
				.query((r, fila) -> new AnioConPeriodos(r.getObject("id", UUID.class), r.getInt("anio"),
						r.getString("estado"), r.getObject("fecha_inicio", LocalDate.class),
						r.getObject("fecha_fin", LocalDate.class), periodos))
				.optional();
	}
}
