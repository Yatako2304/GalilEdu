package com.galiledu.academico.infraestructura;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Peso de cada curso en una competencia (academico.plan_competencia.peso_curso, V13). Se guarda con SQL y no en
 * {@code CompetenciaEvaluada}: al cambiar un valor de la colección, JPA borraría y volvería a insertar la fila, y eso
 * falla cuando la competencia ya tiene ítems de evaluación que apuntan a ella.
 */
@Repository
public class RepositorioPesosPlan {
	private final JdbcClient jdbc;

	public RepositorioPesosPlan(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record PesoFila(UUID competenciaId, String competencia, String area, UUID planCursoId, String curso,
			BigDecimal pesoCurso) {
	}

	public record PesoCompetencia(UUID competenciaId, BigDecimal pesoCurso) {
	}

	/**
	 * Serializa los cambios de pesos de un grado y año hasta el fin de la transacción. Es un bloqueo consultivo porque
	 * el grado puede no tener todavía ninguna fila que bloquear.
	 */
	public void bloquearGrado(UUID anioEscolarId, String gradoCodigo) {
		jdbc.sql("SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(?, 0))")
				.param("plan-evaluacion:" + anioEscolarId + ":" + gradoCodigo).query(Integer.class).single();
	}

	/** Pesos definidos de un curso, por competencia. Las competencias sin peso no aparecen. */
	public Map<UUID, BigDecimal> delCurso(UUID planCursoId) {
		return jdbc.sql("""
				SELECT competencia_id, peso_curso
				FROM academico.plan_competencia
				WHERE plan_curso_id = ? AND peso_curso IS NOT NULL""").param(planCursoId)
				.query(PesoCompetencia.class).list().stream()
				.collect(Collectors.toMap(PesoCompetencia::competenciaId, PesoCompetencia::pesoCurso));
	}

	/** Suma de la competencia en los demás cursos del grado y año. */
	public BigDecimal deOtrosCursos(UUID anioEscolarId, String gradoCodigo, UUID competenciaId,
			UUID exceptoPlanCursoId) {
		return jdbc.sql("""
				SELECT COALESCE(sum(pco.peso_curso), 0)
				FROM academico.plan_competencia pco
				JOIN academico.plan_curso pc ON pc.id = pco.plan_curso_id
				WHERE pc.anio_escolar_id = ? AND pc.grado_codigo = ? AND pco.competencia_id = ?
				  AND pc.id IS DISTINCT FROM ?""")
				.params(anioEscolarId, gradoCodigo, competenciaId, exceptoPlanCursoId).query(BigDecimal.class)
				.single();
	}

	public void guardar(UUID planCursoId, UUID competenciaId, BigDecimal pesoCurso) {
		jdbc.sql("""
				UPDATE academico.plan_competencia SET peso_curso = ?
				WHERE plan_curso_id = ? AND competencia_id = ?""").params(pesoCurso, planCursoId, competenciaId)
				.update();
	}

	/** Competencias del grado y año con cada curso que las evalúa, en el orden del catálogo. */
	public List<PesoFila> delGrado(UUID anioEscolarId, String gradoCodigo) {
		return jdbc.sql("""
				SELECT pco.competencia_id, co.nombre AS competencia, a.nombre AS area,
				       pc.id AS plan_curso_id, cu.nombre AS curso, pco.peso_curso
				FROM academico.plan_competencia pco
				JOIN academico.plan_curso pc ON pc.id = pco.plan_curso_id
				JOIN academico.curso cu ON cu.id = pc.curso_id
				JOIN academico.competencia co ON co.id = pco.competencia_id
				JOIN academico.area a ON a.id = co.area_id
				WHERE pc.anio_escolar_id = ? AND pc.grado_codigo = ?
				ORDER BY a.orden, a.nombre, co.orden, co.nombre, cu.nombre""")
				.params(anioEscolarId, gradoCodigo).query(PesoFila.class).list();
	}
}
