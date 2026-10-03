package com.galiledu.academica.infraestructura.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.galiledu.academica.aplicacion.puertos.RepositorioNotasConsolidadas;
import com.galiledu.academica.dominio.NotaConsolidada;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioNotasConsolidadasPostgres implements RepositorioNotasConsolidadas {
	private final JdbcOperations jdbc;

	public RepositorioNotasConsolidadasPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public UUID guardarFinal(UUID matriculaId, UUID configuracionCompetenciaId, NotaConsolidada nota, Instant ahora) {
		return jdbc.queryForObject("""
			INSERT INTO gestion_academica.nota_final_competencia
			(id, matricula_id, configuracion_competencia_id, valor_numerico, valor_cualitativo,
			 equivalente_cualitativo, fecha_calculo)
			VALUES (?, ?, ?, ?, ?::configuracion.valor_cualitativo, ?::configuracion.valor_cualitativo, ?)
			ON CONFLICT (matricula_id, configuracion_competencia_id) DO UPDATE
			SET valor_numerico = EXCLUDED.valor_numerico, valor_cualitativo = EXCLUDED.valor_cualitativo,
			    equivalente_cualitativo = EXCLUDED.equivalente_cualitativo,
			    fecha_calculo = EXCLUDED.fecha_calculo, activo = TRUE
			RETURNING id
			""", UUID.class, UUID.randomUUID(), matriculaId, configuracionCompetenciaId,
			ColumnasCalificacion.numerico(nota.valor()), ColumnasCalificacion.cualitativo(nota.valor()),
			equivalente(nota), ColumnasCalificacion.timestamp(ahora));
	}

	@Override
	public void guardarPeriodo(UUID notaFinalId, UUID matriculaId, UUID periodoAcademicoId, NotaConsolidada nota,
		Instant ahora) {
		jdbc.update("""
			INSERT INTO gestion_academica.nota_periodo_competencia
			(id, nota_final_competencia_id, matricula_id, periodo_academico_id, valor_numerico,
			 valor_cualitativo, equivalente_cualitativo, fecha_calculo)
			VALUES (?, ?, ?, ?, ?, ?::configuracion.valor_cualitativo, ?::configuracion.valor_cualitativo, ?)
			ON CONFLICT (nota_final_competencia_id, periodo_academico_id) DO UPDATE
			SET valor_numerico = EXCLUDED.valor_numerico, valor_cualitativo = EXCLUDED.valor_cualitativo,
			    equivalente_cualitativo = EXCLUDED.equivalente_cualitativo,
			    fecha_calculo = EXCLUDED.fecha_calculo, activo = TRUE
			""", UUID.randomUUID(), notaFinalId, matriculaId, periodoAcademicoId,
			ColumnasCalificacion.numerico(nota.valor()), ColumnasCalificacion.cualitativo(nota.valor()),
			equivalente(nota), ColumnasCalificacion.timestamp(ahora));
	}

	@Override
	public List<NotaPeriodoGuardada> listarPeriodos(UUID matriculaId, UUID configuracionCompetenciaId) {
		return jdbc.query("""
			SELECT f.configuracion_competencia_id, p.periodo_academico_id, p.valor_numerico,
			       p.valor_cualitativo, p.equivalente_cualitativo, p.fecha_calculo
			FROM gestion_academica.nota_periodo_competencia p
			JOIN gestion_academica.nota_final_competencia f ON f.id = p.nota_final_competencia_id
			WHERE p.matricula_id = ? AND f.configuracion_competencia_id = ? AND p.activo = TRUE
			""", (rs, row) -> periodo(rs), matriculaId, configuracionCompetenciaId);
	}

	@Override
	public List<NotaPeriodoGuardada> listarPeriodosDeMatricula(UUID matriculaId) {
		return jdbc.query("""
			SELECT f.configuracion_competencia_id, p.periodo_academico_id, p.valor_numerico,
			       p.valor_cualitativo, p.equivalente_cualitativo, p.fecha_calculo
			FROM gestion_academica.nota_periodo_competencia p
			JOIN gestion_academica.nota_final_competencia f ON f.id = p.nota_final_competencia_id
			WHERE p.matricula_id = ? AND p.activo = TRUE
			ORDER BY f.configuracion_competencia_id, p.periodo_academico_id
			""", (rs, row) -> periodo(rs), matriculaId);
	}

	@Override
	public List<NotaFinalGuardada> listarFinales(UUID matriculaId) {
		return jdbc.query("""
			SELECT id, configuracion_competencia_id, valor_numerico, valor_cualitativo,
			       equivalente_cualitativo, fecha_calculo
			FROM gestion_academica.nota_final_competencia
			WHERE matricula_id = ? AND activo = TRUE
			ORDER BY configuracion_competencia_id
			""", (rs, row) -> new NotaFinalGuardada(rs.getObject("id", UUID.class),
			rs.getObject("configuracion_competencia_id", UUID.class), nota(rs),
			ColumnasCalificacion.instante(rs, "fecha_calculo")), matriculaId);
	}

	private static NotaPeriodoGuardada periodo(ResultSet rs) throws SQLException {
		return new NotaPeriodoGuardada(rs.getObject("configuracion_competencia_id", UUID.class),
			rs.getObject("periodo_academico_id", UUID.class), nota(rs),
			ColumnasCalificacion.instante(rs, "fecha_calculo"));
	}

	private static NotaConsolidada nota(ResultSet rs) throws SQLException {
		return new NotaConsolidada(ColumnasCalificacion.leer(rs, "valor_numerico", "valor_cualitativo"),
			ColumnasCalificacion.nivel(rs, "equivalente_cualitativo"));
	}

	private static String equivalente(NotaConsolidada nota) {
		return nota.equivalenteCualitativo() == null ? null : nota.equivalenteCualitativo().name();
	}
}
