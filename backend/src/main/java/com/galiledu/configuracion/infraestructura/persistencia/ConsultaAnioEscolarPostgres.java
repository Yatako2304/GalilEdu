package com.galiledu.configuracion.infraestructura.persistencia;

import java.util.Optional;
import java.util.UUID;

import com.galiledu.configuracion.aplicacion.puertos.ConsultaAnioEscolar;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultaAnioEscolarPostgres implements ConsultaAnioEscolar {
	private final JdbcOperations jdbc;

	public ConsultaAnioEscolarPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<AnioEscolar> buscar(UUID id) {
		return jdbc.query("""
			SELECT id, fecha_inicio, fecha_fin, estado::text AS estado
			FROM configuracion.anio_escolar WHERE id = ? AND activo = TRUE
			""", (rs, row) -> new AnioEscolar(rs.getObject("id", UUID.class),
			rs.getDate("fecha_inicio").toLocalDate(), rs.getDate("fecha_fin").toLocalDate(),
			rs.getString("estado")), id).stream().findFirst();
	}
}
