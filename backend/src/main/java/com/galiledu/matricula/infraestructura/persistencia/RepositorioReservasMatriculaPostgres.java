package com.galiledu.matricula.infraestructura.persistencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.matricula.aplicacion.puertos.RepositorioReservasMatricula;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioReservasMatriculaPostgres implements RepositorioReservasMatricula {
	private final JdbcOperations jdbc;

	public RepositorioReservasMatriculaPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public boolean existe(UUID estudianteId, UUID anioEscolarId) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
			SELECT EXISTS (SELECT 1 FROM matricula.matricula
			WHERE estudiante_id = ? AND anio_id = ?)
			""", Boolean.class, estudianteId, anioEscolarId));
	}

	@Override
	public int ocupados(UUID seccionId) {
		return jdbc.queryForObject("""
			SELECT count(*) FROM matricula.matricula
			WHERE seccion_id = ? AND activo = TRUE
			  AND estado IN ('SECCION_RESERVADA'::matricula.estado_matricula,
		                'PENDIENTE_PAGO'::matricula.estado_matricula,
		                'MATRICULADA'::matricula.estado_matricula)
			""", Integer.class, seccionId);
	}

	@Override
	public UUID reservar(UUID estudianteId, UUID anioEscolarId, UUID seccionId,
		LocalDate fechaRegistro) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
			INSERT INTO matricula.matricula
			(id, estudiante_id, anio_id, seccion_id, fecha_registro, tipo, estado)
			VALUES (?, ?, ?, ?, ?, 'REGULAR'::matricula.tipo_matricula,
		            'SECCION_RESERVADA'::matricula.estado_matricula)
			""", id, estudianteId, anioEscolarId, seccionId, fechaRegistro);
		return id;
	}

	@Override
	public Optional<Reserva> buscar(UUID reservaId) {
		List<Reserva> encontradas = jdbc.query("""
			SELECT id, estudiante_id, anio_id, seccion_id,
			       estado::text AS estado, tipo::text AS tipo, fecha_registro
			FROM matricula.matricula
			WHERE id = ? AND activo = TRUE
			  AND estado = 'SECCION_RESERVADA'::matricula.estado_matricula
			""", (rs, row) -> new Reserva(rs.getObject("id", UUID.class),
			rs.getObject("estudiante_id", UUID.class),
			rs.getObject("anio_id", UUID.class),
			rs.getObject("seccion_id", UUID.class),
			rs.getString("estado"), rs.getString("tipo"),
			rs.getDate("fecha_registro").toLocalDate()), reservaId);
		return encontradas.stream().findFirst();
	}
}
