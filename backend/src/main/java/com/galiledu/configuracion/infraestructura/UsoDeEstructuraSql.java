package com.galiledu.configuracion.infraestructura;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.configuracion.aplicacion.UsoDeEstructura;
import com.galiledu.configuracion.dominio.DiaSemana;

/**
 * Lee horarios.horario_seccion y horario_bloque por SQL de solo lectura. No importa nada del módulo de horarios:
 * cuando exista un servicio equivalente, solo se reemplaza esta clase.
 */
@Component
public class UsoDeEstructuraSql implements UsoDeEstructura {
	private final JdbcClient jdbc;

	public UsoDeEstructuraSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Map<DiaSemana, Integer> usoPorDia(UUID estructuraId) {
		// EnumMap: los días quedan en el orden de la semana, para informar siempre el primer caso.
		Map<DiaSemana, Integer> uso = new EnumMap<>(DiaSemana.class);
		jdbc.sql("""
				SELECT hb.dia, max(hb.numero_bloque) AS mayor_bloque
				FROM horarios.horario_bloque hb
				JOIN horarios.horario_seccion hs ON hs.id = hb.horario_seccion_id
				WHERE hs.estructura_horaria_id = :estructuraId
				GROUP BY hb.dia""")
				.param("estructuraId", estructuraId)
				.query((r, fila) -> uso.put(DiaSemana.valueOf(r.getString("dia")), r.getInt("mayor_bloque"))).list();
		return uso;
	}

	@Override
	public boolean tieneHorarios(UUID estructuraId) {
		return jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM horarios.horario_seccion WHERE estructura_horaria_id = :estructuraId)""")
				.param("estructuraId", estructuraId).query(Boolean.class).single();
	}
}
