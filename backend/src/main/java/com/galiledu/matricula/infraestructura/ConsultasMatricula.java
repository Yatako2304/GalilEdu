package com.galiledu.matricula.infraestructura;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Lecturas de solo consulta para la matrícula del apoderado (HU-27, HU-30).
 * Los datos del estudiante y su familia se leen por SQL porque esos módulos aún no exponen un servicio para esto;
 * cuando lo hagan, reemplazar {@link #hijosDe(UUID)} y {@link #personaDeUsuario(UUID)} por esas llamadas.
 */
@Repository
public class ConsultasMatricula {
	private final JdbcClient jdbc;

	public ConsultasMatricula(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record HijoFila(UUID estudianteId, String nombre, String grado) {
	}

	public record PeriodoFila(UUID id, UUID anioEscolarId, int anio, OffsetDateTime inicio, OffsetDateTime cierre,
			String estado, boolean bloquearPorMorosidad) {
	}

	public record MatriculaFila(UUID id, String estado, String grado, String seccion, String codigo) {
	}

	public Optional<UUID> personaDeUsuario(UUID usuarioId) {
		return jdbc.sql("SELECT persona_id FROM seguridad.usuario WHERE id = ?")
				.param(usuarioId)
				.query(UUID.class)
				.optional();
	}

	/** Estudiantes vinculados a la persona (como padre, madre o apoderado): sus hijos, que entre sí son hermanos. */
	public List<HijoFila> hijosDe(UUID personaId) {
		return jdbc.sql("""
				SELECT DISTINCT e.id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS nombre,
				       g.nombre AS grado
				FROM estudiantes.vinculo_familiar v
				JOIN estudiantes.estudiante e ON e.id = v.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				LEFT JOIN configuracion.grado g ON g.codigo = e.grado_codigo
				WHERE v.persona_id = ? AND NOT e.eliminado
				ORDER BY nombre""")
				.param(personaId)
				.query((rs, i) -> new HijoFila(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)))
				.list();
	}

	/** RF-58: el periodo vigente es el HABILITADO; si no hay ninguno, el del año más reciente. */
	public Optional<PeriodoFila> periodoVigente() {
		return jdbc.sql("""
				SELECT pm.id, a.id, a.anio, pm.inicio, pm.cierre, pm.estado, pm.bloquear_por_morosidad
				FROM matricula.periodo_matricula pm
				JOIN configuracion.anio_escolar a ON a.id = pm.anio_escolar_id
				ORDER BY (pm.estado = 'HABILITADO') DESC, a.anio DESC
				LIMIT 1""")
				.query((rs, i) -> new PeriodoFila(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
						rs.getInt(3),
						rs.getObject(4, OffsetDateTime.class), rs.getObject(5, OffsetDateTime.class), rs.getString(6),
						rs.getBoolean(7)))
				.optional();
	}

	public Optional<MatriculaFila> matricula(UUID estudianteId, UUID anioEscolarId) {
		return jdbc.sql("""
				SELECT m.id, m.estado, g.nombre, g.numero || '.° ' || s.nombre, m.codigo
				FROM matricula.matricula m
				JOIN configuracion.grado g ON g.codigo = m.grado_codigo
				LEFT JOIN configuracion.seccion s ON s.id = m.seccion_id
				WHERE m.estudiante_id = ? AND m.anio_escolar_id = ?""")
				.params(estudianteId, anioEscolarId)
				.query((rs, i) -> new MatriculaFila(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
						rs.getString(4), rs.getString(5)))
				.optional();
	}

	public int documentosRequeridos(UUID periodoId) {
		return jdbc.sql("SELECT count(*) FROM matricula.documento_requerido WHERE periodo_matricula_id = ?")
				.param(periodoId)
				.query(Integer.class)
				.single();
	}

	/** Documentos subidos que no fueron observados (en revisión o aprobados). */
	public int documentosEntregados(UUID matriculaId) {
		return jdbc.sql("""
				SELECT count(*) FROM matricula.documento_matricula
				WHERE matricula_id = ? AND estado <> 'OBSERVADO'""")
				.param(matriculaId)
				.query(Integer.class)
				.single();
	}
}
