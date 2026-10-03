package com.galiledu.academica.infraestructura.persistencia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.galiledu.academica.aplicacion.ActorAcademico;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico;
import com.galiledu.academica.dominio.ConfiguracionCompetencia;
import com.galiledu.academica.dominio.EscalaCalificacion;
import com.galiledu.academica.dominio.EstadoPeriodo;
import com.galiledu.academica.dominio.NivelCualitativo;
import com.galiledu.academica.dominio.RangoConversion;
import com.galiledu.academica.dominio.ReglaAgregacionItems;
import com.galiledu.academica.dominio.ReglaConsolidacionPeriodos;
import com.galiledu.academica.dominio.TablaConversion;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

/**
 * Lectura provisional de tablas de Seguridad, Configuración, Matrícula y Personas mientras esos
 * módulos no publiquen sus contratos de aplicación. Nunca escribe en ellas.
 */
@Repository
public class ConsultaContextoAcademicoPostgres implements ConsultaContextoAcademico {
	private final JdbcOperations jdbc;

	public ConsultaContextoAcademicoPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<ActorAcademico> buscarActor(String nombreUsuario) {
		List<FilaActor> filas = jdbc.query("""
			SELECT u.id, u.persona_id, r.nombre AS rol
			FROM seguridad.usuario u
			JOIN personas.persona p ON p.id = u.persona_id AND p.activo = TRUE
			LEFT JOIN seguridad.usuario_rol ur ON ur.usuario_id = u.id
			LEFT JOIN seguridad.rol r ON r.id = ur.rol_id AND r.activo = TRUE
			WHERE u.username = ? AND u.estado = 'ACTIVO'::seguridad.estado_usuario
			""", (rs, row) -> new FilaActor(rs.getObject("id", UUID.class), rs.getObject("persona_id", UUID.class),
			rs.getString("rol")), nombreUsuario);
		if (filas.isEmpty()) return Optional.empty();
		Set<String> roles = filas.stream().map(FilaActor::rol).filter(rol -> rol != null)
			.map(rol -> rol.strip().toUpperCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
		FilaActor primera = filas.getFirst();
		return Optional.of(new ActorAcademico(primera.usuarioId(), primera.personaId(), roles));
	}

	@Override
	public Optional<CargaAcademica> buscarCarga(UUID cargaAcademicaId) {
		return jdbc.query("""
			SELECT c.id, c.oferta_curso_id, c.seccion_id, c.docente_id, o.anio_id,
			       (c.activo AND o.activo AND s.activo AND s.anio_id = o.anio_id) AS activa
			FROM configuracion.carga_academica c
			JOIN configuracion.oferta_curso o ON o.id = c.oferta_curso_id
			JOIN configuracion.seccion s ON s.id = c.seccion_id
			WHERE c.id = ?
			""", (rs, row) -> new CargaAcademica(rs.getObject("id", UUID.class),
			rs.getObject("oferta_curso_id", UUID.class), rs.getObject("seccion_id", UUID.class),
			rs.getObject("docente_id", UUID.class), rs.getObject("anio_id", UUID.class),
			rs.getBoolean("activa")), cargaAcademicaId).stream().findFirst();
	}

	@Override
	public Optional<CompetenciaConfigurada> buscarCompetencia(UUID configuracionCompetenciaId) {
		return jdbc.query("""
			SELECT id, oferta_curso_id, competencia_id, tipo_escala, regla_agregacion,
			       regla_consolidacion, activo
			FROM configuracion.configuracion_competencia
			WHERE id = ?
			""", (rs, row) -> new CompetenciaConfigurada(rs.getObject("id", UUID.class),
			rs.getObject("oferta_curso_id", UUID.class), rs.getObject("competencia_id", UUID.class),
			new ConfiguracionCompetencia(EscalaCalificacion.valueOf(rs.getString("tipo_escala")),
				ReglaAgregacionItems.valueOf(rs.getString("regla_agregacion")),
				ReglaConsolidacionPeriodos.valueOf(rs.getString("regla_consolidacion"))),
			rs.getBoolean("activo")), configuracionCompetenciaId).stream().findFirst();
	}

	@Override
	public Optional<PeriodoAcademico> buscarPeriodo(UUID periodoAcademicoId) {
		return jdbc.query("""
			SELECT id, anio_id, orden, estado, fecha_limite_notas, activo
			FROM configuracion.periodo_academico
			WHERE id = ?
			""", (rs, row) -> periodo(rs.getObject("id", UUID.class), rs.getObject("anio_id", UUID.class),
			rs.getInt("orden"), rs.getString("estado"), rs.getObject("fecha_limite_notas", LocalDateTime.class),
			rs.getBoolean("activo")), periodoAcademicoId).stream().findFirst();
	}

	@Override
	public List<PeriodoAcademico> listarPeriodosDelAnio(UUID anioId) {
		return jdbc.query("""
			SELECT id, anio_id, orden, estado, fecha_limite_notas, activo
			FROM configuracion.periodo_academico
			WHERE anio_id = ?
			ORDER BY orden
			""", (rs, row) -> periodo(rs.getObject("id", UUID.class), rs.getObject("anio_id", UUID.class),
			rs.getInt("orden"), rs.getString("estado"), rs.getObject("fecha_limite_notas", LocalDateTime.class),
			rs.getBoolean("activo")), anioId);
	}

	@Override
	public Optional<MatriculaAcademica> buscarMatricula(UUID matriculaId) {
		return jdbc.query("""
			SELECT id, seccion_id, anio_id,
			       (activo AND estado = 'MATRICULADA'::matricula.estado_matricula) AS vigente
			FROM matricula.matricula
			WHERE id = ?
			""", (rs, row) -> new MatriculaAcademica(rs.getObject("id", UUID.class),
			rs.getObject("seccion_id", UUID.class), rs.getObject("anio_id", UUID.class),
			rs.getBoolean("vigente")), matriculaId).stream().findFirst();
	}

	@Override
	public List<UUID> listarMatriculasVigentes(UUID seccionId, UUID anioId) {
		return jdbc.query("""
			SELECT id FROM matricula.matricula
			WHERE seccion_id = ? AND anio_id = ? AND activo = TRUE
			  AND estado = 'MATRICULADA'::matricula.estado_matricula
			ORDER BY id
			""", (rs, row) -> rs.getObject("id", UUID.class), seccionId, anioId);
	}

	@Override
	public Optional<TablaConversion> buscarTablaConversionVigente() {
		List<RangoConversion> rangos = jdbc.query("""
			SELECT r.nota_desde, r.nota_hasta, r.valor
			FROM configuracion.tabla_conversion t
			JOIN configuracion.rango_conversion r ON r.tabla_conversion_id = t.id AND r.activo = TRUE
			WHERE t.vigente = TRUE AND t.activo = TRUE
			""", (rs, row) -> new RangoConversion(rs.getBigDecimal("nota_desde"),
			rs.getBigDecimal("nota_hasta"), NivelCualitativo.valueOf(rs.getString("valor"))));
		if (rangos.isEmpty()) return Optional.empty();
		try {
			return Optional.of(new TablaConversion(rangos));
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("La tabla de conversión vigente es inconsistente", e);
		}
	}

	@Override
	public boolean coordinaOferta(UUID docenteId, UUID ofertaCursoId) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
			SELECT EXISTS (SELECT 1 FROM configuracion.asignacion_coordinacion
			WHERE docente_id = ? AND oferta_curso_id = ? AND activo = TRUE)
			""", Boolean.class, docenteId, ofertaCursoId));
	}

	@Override
	public boolean puedeConsultarMatricula(UUID personaId, UUID matriculaId) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
			SELECT EXISTS (
			    SELECT 1 FROM matricula.matricula m
			    WHERE m.id = ? AND (
			        m.estudiante_id = ?
			        OR EXISTS (SELECT 1 FROM personas.vinculo_apoderado v
			                   WHERE v.estudiante_id = m.estudiante_id AND v.apoderado_id = ?)
			        OR EXISTS (SELECT 1 FROM configuracion.tutor_seccion t
			                   WHERE t.seccion_id = m.seccion_id AND t.docente_id = ? AND t.activo = TRUE
			                     AND t.fecha_inicio <= CURRENT_DATE
			                     AND (t.fecha_fin IS NULL OR t.fecha_fin >= CURRENT_DATE))))
			""", Boolean.class, matriculaId, personaId, personaId, personaId));
	}

	private static PeriodoAcademico periodo(UUID id, UUID anioId, int orden, String estado,
		LocalDateTime fechaLimiteNotas, boolean activo) {
		return new PeriodoAcademico(id, anioId, orden, EstadoPeriodo.valueOf(estado), fechaLimiteNotas, activo);
	}

	private record FilaActor(UUID usuarioId, UUID personaId, String rol) {}
}
