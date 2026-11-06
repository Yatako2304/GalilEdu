package com.galiledu.horarios.infraestructura;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.horarios.aplicacion.PlanDeSeccion;

/**
 * Lee academico.plan_curso, curso y docente_seccion por SQL de solo lectura. No importa nada del módulo académico:
 * cuando exista un servicio equivalente, solo se reemplaza esta clase.
 */
@Component
public class PlanDeSeccionSql implements PlanDeSeccion {
	private final JdbcClient jdbc;

	public PlanDeSeccionSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<CursoDelPlan> cursosDelPlan(UUID seccionId) {
		return jdbc
				.sql("""
						SELECT pc.id, c.nombre, pc.horas_semanales, ds.docente_id,
						       concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS docente_nombre
						FROM configuracion.seccion s
						JOIN academico.plan_curso pc ON pc.anio_escolar_id = s.anio_escolar_id AND pc.grado_codigo = s.grado_codigo
						JOIN academico.curso c ON c.id = pc.curso_id
						LEFT JOIN academico.docente_seccion ds ON ds.plan_curso_id = pc.id AND ds.seccion_id = s.id
						LEFT JOIN seguridad.usuario u ON u.id = ds.docente_id
						LEFT JOIN personas.persona p ON p.id = u.persona_id
						WHERE s.id = :seccionId
						ORDER BY c.nombre""")
				.param("seccionId", seccionId).query(PlanDeSeccionSql::aCurso).list();
	}

	@Override
	public List<CursoDelPlan> cursosPorId(UUID seccionId, Collection<UUID> planCursoIds) {
		if (planCursoIds.isEmpty()) {
			return List.of();
		}
		return jdbc.sql("""
				SELECT pc.id, c.nombre, pc.horas_semanales, ds.docente_id,
				       concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS docente_nombre
				FROM academico.plan_curso pc
				JOIN academico.curso c ON c.id = pc.curso_id
				LEFT JOIN academico.docente_seccion ds ON ds.plan_curso_id = pc.id AND ds.seccion_id = :seccionId
				LEFT JOIN seguridad.usuario u ON u.id = ds.docente_id
				LEFT JOIN personas.persona p ON p.id = u.persona_id
				WHERE pc.id IN (:ids)""")
				.param("seccionId", seccionId).param("ids", planCursoIds).query(PlanDeSeccionSql::aCurso).list();
	}

	private static CursoDelPlan aCurso(java.sql.ResultSet r, int fila) throws java.sql.SQLException {
		return new CursoDelPlan(r.getObject("id", UUID.class), r.getString("nombre"), r.getInt("horas_semanales"),
				r.getObject("docente_id", UUID.class), r.getString("docente_nombre"));
	}
}
