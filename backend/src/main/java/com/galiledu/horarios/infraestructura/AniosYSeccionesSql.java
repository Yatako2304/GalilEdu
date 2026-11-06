package com.galiledu.horarios.infraestructura;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.horarios.aplicacion.AniosYSecciones;

/**
 * Lee configuracion.anio_escolar, grado y seccion por SQL de solo lectura. No importa nada del módulo de
 * configuración: cuando exista un servicio equivalente, solo se reemplaza esta clase.
 */
@Component
public class AniosYSeccionesSql implements AniosYSecciones {
	private final JdbcClient jdbc;

	public AniosYSeccionesSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<AnioLeido> anios() {
		return jdbc.sql("SELECT id, anio, estado FROM configuracion.anio_escolar ORDER BY anio DESC")
				.query((r, fila) -> new AnioLeido(r.getObject("id", UUID.class), r.getInt("anio"),
						r.getString("estado")))
				.list();
	}

	@Override
	public Optional<List<GradoLeido>> gradosDelAnio(UUID anioEscolarId) {
		boolean existe = jdbc.sql("SELECT EXISTS (SELECT 1 FROM configuracion.anio_escolar WHERE id = :anioId)")
				.param("anioId", anioEscolarId).query(Boolean.class).single();
		if (!existe) {
			return Optional.empty();
		}

		// Las secciones del año por grado, ya ordenadas por nombre; los grados sin secciones salen con la lista vacía.
		Map<String, List<SeccionLeida>> porGrado = new LinkedHashMap<>();
		jdbc.sql("""
				SELECT grado_codigo, id, nombre, estado
				FROM configuracion.seccion
				WHERE anio_escolar_id = :anioId
				ORDER BY nombre""")
				.param("anioId", anioEscolarId)
				.query((r, fila) -> porGrado.computeIfAbsent(r.getString("grado_codigo"), g -> new ArrayList<>())
						.add(new SeccionLeida(r.getObject("id", UUID.class), r.getString("nombre"),
								r.getString("estado"))))
				.list();

		return Optional.of(jdbc.sql("SELECT codigo, nombre, nivel, numero FROM configuracion.grado ORDER BY orden")
				.query((r, fila) -> new GradoLeido(r.getString("codigo"), r.getString("nombre"), r.getString("nivel"),
						r.getInt("numero"), porGrado.getOrDefault(r.getString("codigo"), List.of())))
				.list());
	}
}
