package com.galiledu.horarios.infraestructura.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.galiledu.horarios.aplicacion.puertos.RepositorioHorarios;
import com.galiledu.horarios.dominio.AsignacionHoraria;
import com.galiledu.horarios.dominio.DiaSemana;
import com.galiledu.horarios.dominio.EstadoHorario;
import com.galiledu.horarios.dominio.HorarioSeccion;
import com.galiledu.horarios.dominio.Subgrupo;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioHorariosPostgres implements RepositorioHorarios {
	private static final String COLUMNAS_ASIGNACION = """
		SELECT id, horario_seccion_id, carga_academica_id, bloque_horario_id, espacio_fisico_id,
		       subgrupo_id, tipo_sesion, dia::text AS dia, activo
		FROM horarios.asignacion_horaria
		""";
	private static final RowMapper<HorarioSeccion> HORARIO = (rs, fila) -> new HorarioSeccion(
		rs.getObject("id", UUID.class), rs.getObject("seccion_id", UUID.class), rs.getInt("version"),
		EstadoHorario.valueOf(rs.getString("estado")), rs.getBoolean("activo"));
	private static final RowMapper<Subgrupo> SUBGRUPO = (rs, fila) -> new Subgrupo(
		rs.getObject("id", UUID.class), rs.getObject("seccion_id", UUID.class),
		rs.getString("nombre"), rs.getBoolean("activo"));

	private final JdbcOperations jdbc;

	public RepositorioHorariosPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public HorarioSeccion crearBorrador(UUID seccionId) {
		bloquearTransaccion("horarios.horario_seccion:" + seccionId);
		Integer version = jdbc.queryForObject("""
			SELECT COALESCE(MAX(version), 0) + 1 FROM horarios.horario_seccion WHERE seccion_id = ?
			""", Integer.class, seccionId);
		HorarioSeccion horario = HorarioSeccion.nuevoBorrador(seccionId, version);
		jdbc.update("""
			INSERT INTO horarios.horario_seccion (id, seccion_id, version, estado, activo)
			VALUES (?, ?, ?, ?, TRUE)
			""", horario.id(), seccionId, horario.version(), horario.estado().name());
		return horario;
	}

	@Override
	public Optional<HorarioSeccion> buscarHorario(UUID id) {
		return jdbc.query("""
			SELECT id, seccion_id, version, estado, activo FROM horarios.horario_seccion WHERE id = ?
			""", HORARIO, id).stream().findFirst();
	}

	@Override
	public List<HorarioSeccion> listarHorarios(UUID seccionId) {
		return jdbc.query("""
			SELECT id, seccion_id, version, estado, activo FROM horarios.horario_seccion
			WHERE seccion_id = ? AND activo = TRUE ORDER BY version DESC
			""", HORARIO, seccionId);
	}

	@Override
	public boolean desactivarHorario(UUID id) {
		return jdbc.update("UPDATE horarios.horario_seccion SET activo = FALSE WHERE id = ? AND activo = TRUE", id) == 1;
	}

	@Override
	public void serializarBloque(UUID bloqueHorarioId, DiaSemana dia) {
		bloquearTransaccion("horarios.bloque:" + bloqueHorarioId + ":" + dia);
	}

	@Override
	public void guardarAsignacion(AsignacionHoraria asignacion) {
		jdbc.update("""
			INSERT INTO horarios.asignacion_horaria
			(id, horario_seccion_id, carga_academica_id, bloque_horario_id, espacio_fisico_id,
			 subgrupo_id, tipo_sesion, dia, activo)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?::configuracion.dia_semana, TRUE)
			""", asignacion.id(), asignacion.horarioSeccionId(), asignacion.cargaAcademicaId(),
			asignacion.bloqueHorarioId(), asignacion.espacioFisicoId(), asignacion.subgrupoId(),
			asignacion.tipoSesion(), asignacion.dia().name());
		insertarPeriodos(asignacion);
	}

	@Override
	public boolean actualizarAsignacion(AsignacionHoraria asignacion) {
		int filas = jdbc.update("""
			UPDATE horarios.asignacion_horaria
			SET carga_academica_id = ?, bloque_horario_id = ?, espacio_fisico_id = ?, subgrupo_id = ?,
			    tipo_sesion = ?, dia = ?::configuracion.dia_semana
			WHERE id = ? AND activo = TRUE
			""", asignacion.cargaAcademicaId(), asignacion.bloqueHorarioId(), asignacion.espacioFisicoId(),
			asignacion.subgrupoId(), asignacion.tipoSesion(), asignacion.dia().name(), asignacion.id());
		if (filas == 1) {
			jdbc.update("DELETE FROM horarios.asignacion_periodo WHERE asignacion_horaria_id = ?", asignacion.id());
			insertarPeriodos(asignacion);
		}
		return filas == 1;
	}

	@Override
	public Optional<AsignacionHoraria> buscarAsignacion(UUID id) {
		return jdbc.query(COLUMNAS_ASIGNACION + " WHERE id = ?", this::mapearAsignacion, id)
			.stream().findFirst();
	}

	@Override
	public List<AsignacionHoraria> listarAsignaciones(UUID horarioSeccionId) {
		return jdbc.query(COLUMNAS_ASIGNACION + " WHERE horario_seccion_id = ? AND activo = TRUE ORDER BY id",
			this::mapearAsignacion, horarioSeccionId);
	}

	@Override
	public boolean desactivarAsignacion(UUID id) {
		return jdbc.update("UPDATE horarios.asignacion_horaria SET activo = FALSE WHERE id = ? AND activo = TRUE", id) == 1;
	}

	@Override
	public void guardarSubgrupo(Subgrupo subgrupo) {
		jdbc.update("""
			INSERT INTO horarios.subgrupo (id, seccion_id, nombre, activo) VALUES (?, ?, ?, ?)
			""", subgrupo.id(), subgrupo.seccionId(), subgrupo.nombre(), subgrupo.activo());
	}

	@Override
	public boolean existeSubgrupo(UUID seccionId, String nombre) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
			SELECT EXISTS (SELECT 1 FROM horarios.subgrupo WHERE seccion_id = ? AND nombre = ?)
			""", Boolean.class, seccionId, nombre));
	}

	@Override
	public Optional<Subgrupo> buscarSubgrupo(UUID id) {
		return jdbc.query("SELECT id, seccion_id, nombre, activo FROM horarios.subgrupo WHERE id = ?",
			SUBGRUPO, id).stream().findFirst();
	}

	@Override
	public List<Subgrupo> listarSubgrupos(UUID seccionId) {
		return jdbc.query("""
			SELECT id, seccion_id, nombre, activo FROM horarios.subgrupo
			WHERE seccion_id = ? AND activo = TRUE ORDER BY nombre
			""", SUBGRUPO, seccionId);
	}

	@Override
	public boolean desactivarSubgrupo(UUID id) {
		return jdbc.update("UPDATE horarios.subgrupo SET activo = FALSE WHERE id = ? AND activo = TRUE", id) == 1;
	}

	@Override
	public boolean tieneAsignacionesActivas(UUID subgrupoId) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
			SELECT EXISTS (
			    SELECT 1 FROM horarios.asignacion_horaria a
			    JOIN horarios.horario_seccion h ON h.id = a.horario_seccion_id
			    WHERE a.subgrupo_id = ? AND a.activo = TRUE AND h.activo = TRUE)
			""", Boolean.class, subgrupoId));
	}

	@Override
	public void agregarMiembro(UUID subgrupoId, UUID matriculaId) {
		jdbc.update("""
			INSERT INTO horarios.miembro_subgrupo (subgrupo_id, matricula_id) VALUES (?, ?)
			ON CONFLICT (subgrupo_id, matricula_id)
			DO UPDATE SET activo = TRUE, fecha_asignacion = CURRENT_DATE
			""", subgrupoId, matriculaId);
	}

	@Override
	public boolean retirarMiembro(UUID subgrupoId, UUID matriculaId) {
		return jdbc.update("""
			UPDATE horarios.miembro_subgrupo SET activo = FALSE
			WHERE subgrupo_id = ? AND matricula_id = ? AND activo = TRUE
			""", subgrupoId, matriculaId) == 1;
	}

	@Override
	public List<UUID> listarMiembros(UUID subgrupoId) {
		return jdbc.query("""
			SELECT matricula_id FROM horarios.miembro_subgrupo
			WHERE subgrupo_id = ? AND activo = TRUE ORDER BY matricula_id
			""", (rs, fila) -> rs.getObject("matricula_id", UUID.class), subgrupoId);
	}

	private AsignacionHoraria mapearAsignacion(ResultSet rs, int fila) throws SQLException {
		UUID id = rs.getObject("id", UUID.class);
		Set<UUID> periodos = new HashSet<>(jdbc.query("""
			SELECT periodo_academico_id FROM horarios.asignacion_periodo WHERE asignacion_horaria_id = ?
			""", (fp, n) -> fp.getObject("periodo_academico_id", UUID.class), id));
		return new AsignacionHoraria(id, rs.getObject("horario_seccion_id", UUID.class),
			rs.getObject("carga_academica_id", UUID.class), rs.getObject("bloque_horario_id", UUID.class),
			rs.getObject("espacio_fisico_id", UUID.class), rs.getObject("subgrupo_id", UUID.class),
			rs.getString("tipo_sesion"), DiaSemana.valueOf(rs.getString("dia")), periodos,
			rs.getBoolean("activo"));
	}

	private void insertarPeriodos(AsignacionHoraria asignacion) {
		for (UUID periodoId : asignacion.periodos()) {
			jdbc.update("""
				INSERT INTO horarios.asignacion_periodo (asignacion_horaria_id, periodo_academico_id)
				VALUES (?, ?)
				""", asignacion.id(), periodoId);
		}
	}

	/** Candado de aviso que PostgreSQL libera solo al terminar la transacción. */
	private void bloquearTransaccion(String clave) {
		jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
			(RowCallbackHandler) rs -> { }, clave);
	}
}
