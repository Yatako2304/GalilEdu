package com.galiledu.usuarios.infraestructura.persistencia;

import java.util.UUID;

import com.galiledu.usuarios.aplicacion.puertos.ConsultaEstudianteActivo;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultaEstudianteActivoPostgres implements ConsultaEstudianteActivo {
	private final JdbcOperations jdbc;

	public ConsultaEstudianteActivoPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public boolean existe(UUID estudianteId) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
			SELECT EXISTS (
			  SELECT 1 FROM personas.estudiante e
			  JOIN personas.persona p ON p.id = e.id
			  WHERE e.id = ? AND p.activo = TRUE
			)
			""", Boolean.class, estudianteId));
	}
}
