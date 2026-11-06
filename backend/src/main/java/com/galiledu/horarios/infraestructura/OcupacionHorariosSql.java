package com.galiledu.horarios.infraestructura;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.BloqueOcupado;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.BloqueDeJornada;
import com.galiledu.horarios.aplicacion.OcupacionHorarios;

/**
 * Lee los bloques guardados de las demás secciones del año con una sola consulta y calcula sus horas con la jornada
 * de la estructura de CADA sección (la de su cabecera), no con la de la sección que se edita. La jornada se pide una
 * vez por estructura dentro de cada llamada. Es de solo lectura.
 */
@Component
public class OcupacionHorariosSql implements OcupacionHorarios {
	private final JdbcClient jdbc;
	private final JornadaDeSeccion jornadas;

	public OcupacionHorariosSql(JdbcClient jdbc, JornadaDeSeccion jornadas) {
		this.jdbc = jdbc;
		this.jornadas = jornadas;
	}

	private record Fila(UUID seccionId, UUID estructuraId, DiaSemana dia, int numeroBloque, UUID docenteId,
			UUID ambienteId, String seccion, String curso, String docente, String ambiente) {
	}

	@Override
	public List<BloqueOcupado> bloquesOcupados(UUID anioId, UUID excluirSeccionId) {
		// "5.° B": número del grado y nombre de la sección. El docente es el de docente_seccion (puede faltar).
		String sql = """
				SELECT hs.seccion_id, hs.estructura_horaria_id, hb.dia, hb.numero_bloque, ds.docente_id,
				       hb.ambiente_id, g.numero || '.° ' || s.nombre AS seccion, c.nombre AS curso,
				       concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS docente,
				       a.nombre AS ambiente
				FROM horarios.horario_bloque hb
				JOIN horarios.horario_seccion hs ON hs.id = hb.horario_seccion_id
				JOIN configuracion.seccion s ON s.id = hs.seccion_id
				JOIN configuracion.grado g ON g.codigo = s.grado_codigo
				JOIN academico.plan_curso pc ON pc.id = hb.plan_curso_id
				JOIN academico.curso c ON c.id = pc.curso_id
				JOIN configuracion.ambiente a ON a.id = hb.ambiente_id
				LEFT JOIN academico.docente_seccion ds ON ds.plan_curso_id = hb.plan_curso_id AND ds.seccion_id = hs.seccion_id
				LEFT JOIN seguridad.usuario u ON u.id = ds.docente_id
				LEFT JOIN personas.persona p ON p.id = u.persona_id
				WHERE s.anio_escolar_id = :anioId""";
		JdbcClient.StatementSpec consulta;
		if (excluirSeccionId == null) {
			consulta = jdbc.sql(sql + " ORDER BY hs.seccion_id, hb.dia, hb.numero_bloque");
		} else {
			consulta = jdbc.sql(sql + " AND hs.seccion_id <> :excluir ORDER BY hs.seccion_id, hb.dia, hb.numero_bloque")
					.param("excluir", excluirSeccionId);
		}
		List<Fila> filas = consulta.param("anioId", anioId)
				.query((r, n) -> new Fila(r.getObject("seccion_id", UUID.class),
						r.getObject("estructura_horaria_id", UUID.class), DiaSemana.valueOf(r.getString("dia")),
						r.getInt("numero_bloque"), r.getObject("docente_id", UUID.class),
						r.getObject("ambiente_id", UUID.class), r.getString("seccion"), r.getString("curso"),
						r.getString("docente"), r.getString("ambiente")))
				.list();

		Map<UUID, Map<Integer, BloqueDeJornada>> jornadaPorEstructura = new HashMap<>();
		List<BloqueOcupado> ocupados = new ArrayList<>();
		for (Fila fila : filas) {
			Map<Integer, BloqueDeJornada> bloques = jornadaPorEstructura.computeIfAbsent(fila.estructuraId(), id -> {
				Map<Integer, BloqueDeJornada> porNumero = new HashMap<>();
				jornadas.deEstructura(id).bloques().forEach(b -> porNumero.put(b.numero(), b));
				return porNumero;
			});
			BloqueDeJornada bloque = bloques.get(fila.numeroBloque());
			if (bloque == null) {
				continue; // la estructura ya no tiene ese bloque: no ocupa ninguna hora
			}
			ocupados.add(new BloqueOcupado(fila.seccionId(), fila.dia(), bloque.inicio(), bloque.fin(),
					fila.docenteId(), fila.ambienteId(), fila.seccion(), fila.curso(), fila.docente(),
					fila.ambiente()));
		}
		return ocupados;
	}
}
