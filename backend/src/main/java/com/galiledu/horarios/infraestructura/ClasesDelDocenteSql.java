package com.galiledu.horarios.infraestructura;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.ClasesDelDocente;

/**
 * Lee con una sola consulta los bloques guardados de las secciones del año donde el docente es el titular del curso
 * (docente_seccion). Es de solo lectura y no calcula horas: devuelve la estructura de cada sección para que quien la
 * use pida la jornada con {@code JornadaDeSeccion.deEstructura}.
 */
@Component
public class ClasesDelDocenteSql implements ClasesDelDocente {
	private final JdbcClient jdbc;

	public ClasesDelDocenteSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<AnioConClases> aniosDe(UUID docenteId) {
		return jdbc
				.sql("""
						SELECT DISTINCT ae.id, ae.anio, ae.estado
						FROM horarios.horario_bloque hb
						JOIN horarios.horario_seccion hs ON hs.id = hb.horario_seccion_id
						JOIN configuracion.seccion s ON s.id = hs.seccion_id
						JOIN configuracion.anio_escolar ae ON ae.id = s.anio_escolar_id
						JOIN academico.docente_seccion ds ON ds.plan_curso_id = hb.plan_curso_id AND ds.seccion_id = hs.seccion_id
						WHERE ds.docente_id = :docenteId
						ORDER BY ae.anio DESC""")
				.param("docenteId", docenteId)
				.query((r, n) -> new AnioConClases(r.getObject("id", UUID.class), r.getInt("anio"),
						r.getString("estado")))
				.list();
	}

	@Override
	public List<ClaseDelDocente> clasesDe(UUID docenteId, UUID anioEscolarId) {
		// "5.° B": número del grado y nombre de la sección.
		List<ClaseDelDocente> clases = jdbc
				.sql("""
						SELECT hs.seccion_id, g.numero || '.° ' || s.nombre AS seccion, g.nivel, hs.estructura_horaria_id,
						       hb.dia, hb.numero_bloque, hb.plan_curso_id, c.nombre AS curso, hb.ambiente_id,
						       a.nombre AS ambiente
						FROM horarios.horario_bloque hb
						JOIN horarios.horario_seccion hs ON hs.id = hb.horario_seccion_id
						JOIN configuracion.seccion s ON s.id = hs.seccion_id
						JOIN configuracion.grado g ON g.codigo = s.grado_codigo
						JOIN academico.plan_curso pc ON pc.id = hb.plan_curso_id
						JOIN academico.curso c ON c.id = pc.curso_id
						JOIN configuracion.ambiente a ON a.id = hb.ambiente_id
						JOIN academico.docente_seccion ds ON ds.plan_curso_id = hb.plan_curso_id AND ds.seccion_id = hs.seccion_id
						WHERE s.anio_escolar_id = :anioId AND ds.docente_id = :docenteId""")
				.param("anioId", anioEscolarId).param("docenteId", docenteId)
				.query((r, n) -> new ClaseDelDocente(r.getObject("seccion_id", UUID.class), r.getString("seccion"),
						r.getString("nivel"), r.getObject("estructura_horaria_id", UUID.class),
						DiaSemana.valueOf(r.getString("dia")), r.getInt("numero_bloque"),
						r.getObject("plan_curso_id", UUID.class), r.getString("curso"),
						r.getObject("ambiente_id", UUID.class), r.getString("ambiente")))
				.list();
		// El día se guarda como texto (LUN…SAB): se ordena por la posición del enum, no alfabéticamente.
		return clases.stream().sorted(Comparator.comparing(ClaseDelDocente::dia)
				.thenComparingInt(ClaseDelDocente::numeroBloque).thenComparing(ClaseDelDocente::seccion)).toList();
	}
}
