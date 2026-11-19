package com.galiledu.asistencia.infraestructura;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * HU-68 · RF-146: totales de faltas y tardanzas por matrícula en un rango de fechas, separando las justificadas. Solo
 * cuenta los días que tienen asistencia registrada.
 */
@Repository
public class ConsultasReporteAsistencia {
	private final JdbcClient jdbc;

	public ConsultasReporteAsistencia(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Totales de una matrícula. {@code faltas} y {@code tardanzas} incluyen las justificadas. */
	public record Totales(int faltas, int faltasJustificadas, int tardanzas, int tardanzasJustificadas) {
		public static final Totales CERO = new Totales(0, 0, 0, 0);
	}

	public Map<UUID, Totales> totales(UUID seccionId, LocalDate desde, LocalDate hasta) {
		return jdbc.sql("""
				SELECT ae.matricula_id,
				       count(*) FILTER (WHERE ae.estado = 'FALTA'),
				       count(*) FILTER (WHERE ae.estado = 'FALTA' AND j.id IS NOT NULL),
				       count(*) FILTER (WHERE ae.estado = 'TARDANZA'),
				       count(*) FILTER (WHERE ae.estado = 'TARDANZA' AND j.id IS NOT NULL)
				FROM asistencia.registro_diario r
				JOIN asistencia.asistencia_estudiante ae ON ae.registro_id = r.id
				LEFT JOIN asistencia.justificacion j ON j.asistencia_estudiante_id = ae.id
				WHERE r.seccion_id = :seccion AND r.fecha BETWEEN :desde AND :hasta
				GROUP BY ae.matricula_id""")
				.param("seccion", seccionId).param("desde", desde).param("hasta", hasta)
				.query((rs, i) -> Map.entry(rs.getObject(1, UUID.class),
						new Totales(rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5))))
				.list().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/** Cuántos días del rango tienen asistencia registrada. */
	public int diasRegistrados(UUID seccionId, LocalDate desde, LocalDate hasta) {
		return jdbc.sql("""
				SELECT count(*) FROM asistencia.registro_diario
				WHERE seccion_id = :seccion AND fecha BETWEEN :desde AND :hasta""")
				.param("seccion", seccionId).param("desde", desde).param("hasta", hasta)
				.query(Integer.class).single();
	}
}
