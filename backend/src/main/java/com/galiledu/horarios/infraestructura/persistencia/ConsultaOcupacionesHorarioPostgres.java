package com.galiledu.horarios.infraestructura.persistencia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.galiledu.horarios.aplicacion.puertos.ConsultaOcupacionesHorario;
import com.galiledu.horarios.dominio.DiaSemana;
import com.galiledu.horarios.dominio.OcupacionHoraria;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/**
 * Lee lo ocupado en un bloque. Junto con las demás secciones solo cuenta su versión activa más
 * reciente, para no cruzar a un docente consigo mismo entre versiones de un mismo horario.
 */
@Repository
public class ConsultaOcupacionesHorarioPostgres implements ConsultaOcupacionesHorario {
	private static final String BUSCAR = """
		SELECT a.id, a.horario_seccion_id, g.nombre || ' ' || s.nombre AS seccion, cu.nombre AS curso,
		       ca.docente_id, a.espacio_fisico_id, a.subgrupo_id, a.bloque_horario_id,
		       a.dia::text AS dia, ap.periodo_academico_id
		FROM horarios.asignacion_horaria a
		JOIN horarios.horario_seccion h ON h.id = a.horario_seccion_id
		JOIN configuracion.seccion s ON s.id = h.seccion_id
		JOIN configuracion.grado g ON g.id = s.grado_id
		JOIN configuracion.carga_academica ca ON ca.id = a.carga_academica_id
		JOIN configuracion.oferta_curso oc ON oc.id = ca.oferta_curso_id
		JOIN configuracion.curso cu ON cu.id = oc.curso_id
		JOIN horarios.asignacion_periodo ap ON ap.asignacion_horaria_id = a.id
		WHERE a.activo = TRUE AND h.activo = TRUE
		  AND a.bloque_horario_id = ? AND a.dia = ?::configuracion.dia_semana
		  AND ap.periodo_academico_id IN (%s)
		  AND (h.id = ? OR (
		        h.seccion_id <> (SELECT seccion_id FROM horarios.horario_seccion WHERE id = ?)
		        AND h.version = (SELECT MAX(h2.version) FROM horarios.horario_seccion h2
		                         WHERE h2.seccion_id = h.seccion_id AND h2.activo = TRUE)))
		ORDER BY a.id
		""";

	private final JdbcOperations jdbc;

	public ConsultaOcupacionesHorarioPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<OcupacionHoraria> buscarOcupaciones(UUID horarioSeccionId, UUID bloqueHorarioId,
		DiaSemana dia, Set<UUID> periodos) {
		if (periodos.isEmpty()) {
			return List.of();
		}
		String marcadores = String.join(",", Collections.nCopies(periodos.size(), "?"));
		List<Object> parametros = new ArrayList<>();
		parametros.add(bloqueHorarioId);
		parametros.add(dia.name());
		parametros.addAll(periodos);
		parametros.add(horarioSeccionId);
		parametros.add(horarioSeccionId);

		Map<UUID, Acumulado> porAsignacion = new LinkedHashMap<>();
		jdbc.query(BUSCAR.formatted(marcadores), (RowCallbackHandler) rs -> {
			UUID id = rs.getObject("id", UUID.class);
			Acumulado acumulado = porAsignacion.get(id);
			if (acumulado == null) {
				acumulado = new Acumulado(id, rs.getObject("horario_seccion_id", UUID.class),
					rs.getString("seccion"), rs.getString("curso"),
					rs.getObject("docente_id", UUID.class),
					rs.getObject("espacio_fisico_id", UUID.class),
					rs.getObject("subgrupo_id", UUID.class),
					rs.getObject("bloque_horario_id", UUID.class),
					DiaSemana.valueOf(rs.getString("dia")), new HashSet<>());
				porAsignacion.put(id, acumulado);
			}
			acumulado.periodos().add(rs.getObject("periodo_academico_id", UUID.class));
		}, parametros.toArray());
		return porAsignacion.values().stream().map(Acumulado::aOcupacion).toList();
	}

	private record Acumulado(UUID id, UUID horarioSeccionId, String seccion, String curso,
		UUID docenteId, UUID espacioFisicoId, UUID subgrupoId, UUID bloqueHorarioId,
		DiaSemana dia, Set<UUID> periodos) {
		OcupacionHoraria aOcupacion() {
			return new OcupacionHoraria(id, horarioSeccionId, seccion, curso, docenteId,
				espacioFisicoId, subgrupoId, bloqueHorarioId, dia, periodos);
		}
	}
}
