package com.galiledu.horarios.infraestructura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.horarios.aplicacion.ConsultaDeReservas;

/**
 * Lee horarios.reserva_ambiente con los nombres de otros módulos (ambiente, sección, grado, usuario) por SQL de solo
 * lectura. La etiqueta de la sección («4.° B») se arma igual que en {@code OcupacionHorariosSql}.
 */
@Component
public class ConsultaDeReservasSql implements ConsultaDeReservas {
	private static final String BASE = """
			SELECT r.id, r.ambiente_id, a.codigo AS ambiente_codigo, a.nombre AS ambiente_nombre, r.actividad,
			       s.anio_escolar_id, s.grado_codigo, r.seccion_id, g.numero || '.° ' || s.nombre AS seccion,
			       r.responsable_id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS responsable,
			       r.fecha, r.hora_inicio, r.hora_fin, r.estado, r.version
			FROM horarios.reserva_ambiente r
			JOIN configuracion.ambiente a ON a.id = r.ambiente_id
			JOIN configuracion.seccion s ON s.id = r.seccion_id
			JOIN configuracion.grado g ON g.codigo = s.grado_codigo
			JOIN seguridad.usuario u ON u.id = r.responsable_id
			JOIN personas.persona p ON p.id = u.persona_id
			""";

	private final JdbcClient jdbc;

	public ConsultaDeReservasSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	private static ReservaLeida leer(ResultSet r, int fila) throws SQLException {
		return new ReservaLeida(r.getObject("id", UUID.class), r.getObject("ambiente_id", UUID.class),
				r.getString("ambiente_codigo"), r.getString("ambiente_nombre"), r.getString("actividad"),
				r.getObject("anio_escolar_id", UUID.class), r.getString("grado_codigo"),
				r.getObject("seccion_id", UUID.class), r.getString("seccion"),
				r.getObject("responsable_id", UUID.class), r.getString("responsable"),
				r.getObject("fecha", LocalDate.class), r.getObject("hora_inicio", LocalTime.class),
				r.getObject("hora_fin", LocalTime.class), r.getString("estado"), r.getLong("version"));
	}

	@Override
	public Optional<ReservaLeida> porId(UUID id) {
		return jdbc.sql(BASE + "WHERE r.id = :id").param("id", id).query(ConsultaDeReservasSql::leer).optional();
	}

	@Override
	public List<ReservaLeida> porIds(Collection<UUID> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		return jdbc.sql(BASE + "WHERE r.id IN (:ids)").param("ids", ids).query(ConsultaDeReservasSql::leer).list();
	}

	@Override
	public List<ReservaLeida> activasEntre(LocalDate desde, LocalDate hasta, int limite, long desplazamiento) {
		return jdbc.sql(BASE + """
				WHERE r.estado = 'ACTIVA' AND r.fecha BETWEEN :desde AND :hasta
				ORDER BY r.fecha DESC, r.hora_inicio DESC, r.id
				LIMIT :limite OFFSET :desplazamiento""").param("desde", desde).param("hasta", hasta)
				.param("limite", limite).param("desplazamiento", desplazamiento)
				.query(ConsultaDeReservasSql::leer).list();
	}

	@Override
	public long contarActivasEntre(LocalDate desde, LocalDate hasta) {
		return jdbc.sql("""
				SELECT count(*) FROM horarios.reserva_ambiente
				WHERE estado = 'ACTIVA' AND fecha BETWEEN :desde AND :hasta""").param("desde", desde)
				.param("hasta", hasta).query(Long.class).single();
	}
}
