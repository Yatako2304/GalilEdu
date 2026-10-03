package com.galiledu.horarios.infraestructura.persistencia;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.galiledu.horarios.aplicacion.puertos.ConsultaHorarioSemanal;
import com.galiledu.horarios.dominio.DiaSemana;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Lee solo versiones activas en estado VIGENTE; un borrador no se muestra a docentes ni familias. */
@Repository
public class ConsultaHorarioSemanalPostgres implements ConsultaHorarioSemanal {
	private static final String BASE = """
		SELECT a.id, a.dia::text AS dia, b.orden, b.hora_inicio, b.hora_fin,
		       g.nombre || ' ' || s.nombre AS seccion, cu.nombre AS curso, ca.docente_id,
		       e.nombre AS espacio, sg.nombre AS subgrupo, a.tipo_sesion
		FROM horarios.asignacion_horaria a
		JOIN horarios.horario_seccion h ON h.id = a.horario_seccion_id
		JOIN configuracion.seccion s ON s.id = h.seccion_id
		JOIN configuracion.grado g ON g.id = s.grado_id
		JOIN configuracion.bloque_horario b ON b.id = a.bloque_horario_id
		JOIN configuracion.carga_academica ca ON ca.id = a.carga_academica_id
		JOIN configuracion.oferta_curso oc ON oc.id = ca.oferta_curso_id
		JOIN configuracion.curso cu ON cu.id = oc.curso_id
		LEFT JOIN configuracion.espacio_fisico e ON e.id = a.espacio_fisico_id
		LEFT JOIN horarios.subgrupo sg ON sg.id = a.subgrupo_id
		WHERE a.activo = TRUE AND h.activo = TRUE AND h.estado = 'VIGENTE'
		  AND EXISTS (SELECT 1 FROM horarios.asignacion_periodo ap
		              WHERE ap.asignacion_horaria_id = a.id AND ap.periodo_academico_id = ?)
		""";
	private static final String ORDEN = " ORDER BY a.dia, b.orden, cu.nombre, a.id";
	private static final RowMapper<EntradaHorario> ENTRADA = (rs, fila) -> new EntradaHorario(
		rs.getObject("id", UUID.class), DiaSemana.valueOf(rs.getString("dia")), rs.getInt("orden"),
		rs.getObject("hora_inicio", LocalTime.class), rs.getObject("hora_fin", LocalTime.class),
		rs.getString("seccion"), rs.getString("curso"), rs.getObject("docente_id", UUID.class),
		rs.getString("espacio"), rs.getString("subgrupo"), rs.getString("tipo_sesion"));

	private final JdbcOperations jdbc;

	public ConsultaHorarioSemanalPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<EntradaHorario> porDocente(UUID docenteId, UUID periodoId) {
		return jdbc.query(BASE + " AND ca.docente_id = ?" + ORDEN, ENTRADA, periodoId, docenteId);
	}

	@Override
	public List<EntradaHorario> porSeccion(UUID seccionId, UUID periodoId) {
		return jdbc.query(BASE + " AND h.seccion_id = ?" + ORDEN, ENTRADA, periodoId, seccionId);
	}

	@Override
	public List<EntradaHorario> porMatricula(UUID matriculaId, UUID periodoId) {
		return jdbc.query(BASE + """
			 AND h.seccion_id = (SELECT seccion_id FROM matricula.matricula WHERE id = ? AND activo = TRUE)
			 AND (a.subgrupo_id IS NULL OR EXISTS (
			     SELECT 1 FROM horarios.miembro_subgrupo ms
			     WHERE ms.subgrupo_id = a.subgrupo_id AND ms.matricula_id = ? AND ms.activo = TRUE))
			""" + ORDEN, ENTRADA, periodoId, matriculaId, matriculaId);
	}
}
