package com.galiledu.academico.infraestructura;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Peso de cada curso en la nota de una competencia (academico.plan_competencia.peso_curso, V16). Se guarda con SQL y
 * no en {@code CompetenciaEvaluada}: la colección de JPA solo sabe qué competencias evalúa el curso, y así una
 * reconfiguración del curso no borra los pesos de las competencias que siguen marcadas.
 */
@Repository
public class RepositorioPesosPlan {
	private final JdbcClient jdbc;

	public RepositorioPesosPlan(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Un curso que evalúa una competencia en el grado y año, con su peso (null = sin definir). */
	public record PesoFila(UUID competenciaId, String competencia, String area, UUID planCursoId, String curso,
			BigDecimal pesoCurso) {
	}

	private record PesoCompetencia(UUID competenciaId, BigDecimal pesoCurso) {
	}

	/**
	 * Serializa los cambios de pesos de un grado y año hasta el fin de la transacción. Es un bloqueo consultivo porque
	 * la suma cruza varios cursos y el grado puede no tener todavía filas que bloquear.
	 */
	public void bloquearGrado(UUID anioEscolarId, String gradoCodigo) {
		jdbc.sql("SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(?, 0))")
				.param("plan-evaluacion:" + anioEscolarId + ":" + gradoCodigo).query(Integer.class).single();
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

	/** Pesos definidos de un curso, por competencia. Las competencias sin peso no aparecen. */
	public Map<UUID, BigDecimal> delCurso(UUID planCursoId) {
		return jdbc.sql("""
				SELECT competencia_id, peso_curso
				FROM academico.plan_competencia
				WHERE plan_curso_id = ? AND peso_curso IS NOT NULL""").param(planCursoId)
				.query(PesoCompetencia.class).list().stream()
				.collect(Collectors.toMap(PesoCompetencia::competenciaId, PesoCompetencia::pesoCurso));
	}

	public void guardar(UUID planCursoId, UUID competenciaId, BigDecimal pesoCurso) {
		jdbc.sql("""
				UPDATE academico.plan_competencia SET peso_curso = ?
				WHERE plan_curso_id = ? AND competencia_id = ?""").params(pesoCurso, planCursoId, competenciaId)
				.update();
	}

	/**
	 * Una competencia que la evalúa un solo curso del grado recibe el 100 %: no hay nada que repartir. Se aplica
	 * después de agregar, configurar, quitar o copiar cursos.
	 */
	public void completarUnicos(UUID anioEscolarId, String gradoCodigo) {
		jdbc.sql("""
				UPDATE academico.plan_competencia pco
				SET peso_curso = 100
				FROM academico.plan_curso pc
				WHERE pc.id = pco.plan_curso_id AND pc.anio_escolar_id = ? AND pc.grado_codigo = ?
				  AND pco.peso_curso IS DISTINCT FROM 100
				  AND (SELECT count(*) FROM academico.plan_competencia otra
				       JOIN academico.plan_curso pc2 ON pc2.id = otra.plan_curso_id
				       WHERE otra.competencia_id = pco.competencia_id
				         AND pc2.anio_escolar_id = pc.anio_escolar_id AND pc2.grado_codigo = pc.grado_codigo) = 1""")
				.params(anioEscolarId, gradoCodigo).update();
	}
}
