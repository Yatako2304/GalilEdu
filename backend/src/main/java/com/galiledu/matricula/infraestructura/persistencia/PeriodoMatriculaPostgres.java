package com.galiledu.matricula.infraestructura.persistencia;

import java.util.Optional;
import java.util.UUID;

import com.galiledu.matricula.aplicacion.puertos.ConsultaPeriodoMatricula;
import com.galiledu.matricula.aplicacion.puertos.RegistroPeriodoMatricula;
import com.galiledu.matricula.dominio.PeriodoMatricula;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class PeriodoMatriculaPostgres implements ConsultaPeriodoMatricula, RegistroPeriodoMatricula {
	private final JdbcOperations jdbc;

	public PeriodoMatriculaPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void guardar(PeriodoMatricula periodo) {
		jdbc.update("""
			INSERT INTO matricula.periodo_matricula (anio_escolar_id, fecha_inicio, fecha_fin)
			VALUES (?, ?, ?)
			ON CONFLICT (anio_escolar_id) DO UPDATE
			SET fecha_inicio = EXCLUDED.fecha_inicio, fecha_fin = EXCLUDED.fecha_fin
			""", periodo.anioEscolarId(), periodo.inicio(), periodo.fin());
	}

	@Override
	public Optional<PeriodoMatricula> buscarPorAnio(UUID anioEscolarId) {
		return jdbc.query("""
			SELECT anio_escolar_id, fecha_inicio, fecha_fin
			FROM matricula.periodo_matricula WHERE anio_escolar_id = ?
			""", (rs, row) -> new PeriodoMatricula(
			rs.getObject("anio_escolar_id", UUID.class),
			rs.getDate("fecha_inicio").toLocalDate(),
			rs.getDate("fecha_fin").toLocalDate()), anioEscolarId).stream().findFirst();
	}
}
