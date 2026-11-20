package com.galiledu.asistencia.infraestructura;

import java.sql.Array;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.asistencia.aplicacion.CalendarioEscolar;

/**
 * Lee configuracion.anio_escolar, periodo_academico y la estructura horaria principal del nivel por SQL de solo
 * lectura (como {@code CalendarioDelAnioSql} de Horarios).
 */
@Component
public class CalendarioEscolarSql implements CalendarioEscolar {
	private static final Map<String, DayOfWeek> DIAS = Map.of("LUN", DayOfWeek.MONDAY, "MAR", DayOfWeek.TUESDAY,
			"MIE", DayOfWeek.WEDNESDAY, "JUE", DayOfWeek.THURSDAY, "VIE", DayOfWeek.FRIDAY, "SAB", DayOfWeek.SATURDAY);

	private final JdbcClient jdbc;

	public CalendarioEscolarSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<CalendarioDelNivel> de(UUID anioEscolarId, String nivel) {
		List<Periodo> periodos = jdbc.sql("""
				SELECT numero, nombre, fecha_inicio, fecha_fin
				FROM configuracion.periodo_academico
				WHERE anio_escolar_id = :anioId
				ORDER BY numero""").param("anioId", anioEscolarId)
				.query((rs, i) -> new Periodo(rs.getInt(1), rs.getString(2), rs.getObject(3, LocalDate.class),
						rs.getObject(4, LocalDate.class)))
				.list();
		Set<DayOfWeek> dias = jdbc.sql("""
				SELECT dias_lectivos
				FROM configuracion.estructura_horaria
				WHERE anio_escolar_id = :anioId AND nivel = :nivel AND es_principal""")
				.param("anioId", anioEscolarId).param("nivel", nivel)
				.query((rs, i) -> dias(rs.getArray(1)))
				.optional().orElse(EnumSet.noneOf(DayOfWeek.class));
		return jdbc.sql("""
				SELECT anio, estado, fecha_inicio, fecha_fin
				FROM configuracion.anio_escolar
				WHERE id = :anioId""").param("anioId", anioEscolarId)
				.query((rs, i) -> new CalendarioDelNivel(rs.getInt(1), rs.getString(2),
						rs.getObject(3, LocalDate.class), rs.getObject(4, LocalDate.class), periodos, dias))
				.optional();
	}

	private static Set<DayOfWeek> dias(Array arreglo) throws SQLException {
		Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
		for (Object dia : (Object[]) arreglo.getArray()) {
			DayOfWeek valor = DIAS.get(String.valueOf(dia));
			if (valor != null) {
				dias.add(valor);
			}
		}
		return dias;
	}
}
