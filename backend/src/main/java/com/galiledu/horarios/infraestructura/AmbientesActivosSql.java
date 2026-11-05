package com.galiledu.horarios.infraestructura;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.horarios.aplicacion.AmbientesActivos;

/** Lee configuracion.ambiente por SQL de solo lectura. Cuando exista un servicio equivalente, se reemplaza. */
@Component
public class AmbientesActivosSql implements AmbientesActivos {
	private final JdbcClient jdbc;

	public AmbientesActivosSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<AmbienteDisponible> activos() {
		return jdbc.sql("""
				SELECT id, nombre, tipo, aforo_maximo
				FROM configuracion.ambiente
				WHERE estado = 'ACTIVO'
				ORDER BY nombre""")
				.query((r, fila) -> new AmbienteDisponible(r.getObject("id", UUID.class), r.getString("nombre"),
						r.getString("tipo"), r.getInt("aforo_maximo")))
				.list();
	}

	@Override
	public Map<UUID, String> nombresPorId(Collection<UUID> ambienteIds) {
		Map<UUID, String> nombres = new HashMap<>();
		if (ambienteIds.isEmpty()) {
			return nombres;
		}
		jdbc.sql("SELECT id, nombre FROM configuracion.ambiente WHERE id IN (:ids)").param("ids", ambienteIds)
				.query((r, fila) -> nombres.put(r.getObject("id", UUID.class), r.getString("nombre"))).list();
		return nombres;
	}
}
