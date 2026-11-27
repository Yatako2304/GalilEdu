package com.galiledu.academico.infraestructura;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Consultas de solo lectura para los reportes de notas (HU-63 y HU-64) y la consulta del apoderado (HU-69). Las notas
 * se calculan con {@code ServicioNotasCompetencia}; aquí solo se lee el contexto: años, periodos, grados, cursos,
 * secciones, hijos del apoderado e ítems de un estudiante.
 */
@Repository
public class RepositorioReportesNotas {
	private final JdbcClient jdbc;

	public RepositorioReportesNotas(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record AnioFila(UUID id, int anio, String estado, String escala, String reglaNota,
			String consolidacion) {
	}

	public record PeriodoAnioFila(UUID id, int numero, String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
	}

	public record GradoCursoFila(String gradoCodigo, String grado, String nivel, UUID planCursoId, String curso) {
	}

	public record SeccionFila(UUID id, String nombre) {
	}

	/** Años que ya empezaron (ACTIVO o CERRADO), del más reciente al más antiguo. */
	public List<AnioFila> anios() {
		return jdbc.sql("""
				SELECT id, anio, estado, escala_evaluacion AS escala, regla_nota,
				       consolidacion_periodos AS consolidacion
				FROM configuracion.anio_escolar
				WHERE estado IN ('ACTIVO', 'CERRADO')
				ORDER BY anio DESC""").query(AnioFila.class).list();
	}

	public List<PeriodoAnioFila> periodos(UUID anioEscolarId) {
		return jdbc.sql("""
				SELECT id, numero, nombre, fecha_inicio, fecha_fin
				FROM configuracion.periodo_academico
				WHERE anio_escolar_id = ?
				ORDER BY numero""").param(anioEscolarId).query(PeriodoAnioFila.class).list();
	}

	/** Grados del año con plan de evaluación y sus cursos, en el orden de los grados. */
	public List<GradoCursoFila> gradosYCursos(UUID anioEscolarId) {
		return jdbc.sql("""
				SELECT g.codigo AS grado_codigo, g.nombre AS grado, g.nivel, pc.id AS plan_curso_id,
				       c.nombre AS curso
				FROM academico.plan_curso pc
				JOIN configuracion.grado g ON g.codigo = pc.grado_codigo
				JOIN academico.curso c ON c.id = pc.curso_id
				WHERE pc.anio_escolar_id = ?
				ORDER BY g.orden, c.nombre""").param(anioEscolarId).query(GradoCursoFila.class).list();
	}

	/** Secciones activas de un grado en el año, por nombre. */
	public List<SeccionFila> secciones(UUID anioEscolarId, String gradoCodigo) {
		return jdbc.sql("""
				SELECT id, nombre FROM configuracion.seccion
				WHERE anio_escolar_id = ? AND grado_codigo = ? AND estado = 'ACTIVA'
				ORDER BY nombre""").params(anioEscolarId, gradoCodigo).query(SeccionFila.class).list();
	}

	/**
	 * Una matrícula MATRICULADO o RETIRADO (sus notas se siguen viendo) de un hijo del usuario: padre, madre o
	 * apoderado vinculado y no fallecido, y estudiante
	 * no eliminado (mismo criterio que el horario del apoderado, HU-51).
	 */
	public record MatriculaHijoFila(UUID estudianteId, String estudiante, UUID matriculaId, UUID anioEscolarId,
			int anio, String estadoAnio, String escala, String reglaNota, String consolidacion, String gradoCodigo,
			String grado, UUID seccionId, String seccion) {
	}

	public List<MatriculaHijoFila> matriculasDeHijos(UUID usuarioId) {
		return jdbc.sql("""
				SELECT DISTINCT e.id AS estudiante_id,
				       concat_ws(' ', pe.nombres, pe.apellido_paterno, pe.apellido_materno) AS estudiante,
				       m.id AS matricula_id, a.id AS anio_escolar_id, a.anio, a.estado AS estado_anio,
				       a.escala_evaluacion AS escala, a.regla_nota, a.consolidacion_periodos AS consolidacion,
				       g.codigo AS grado_codigo, g.nombre AS grado, s.id AS seccion_id, s.nombre AS seccion
				FROM seguridad.usuario u
				JOIN personas.persona pa ON pa.id = u.persona_id AND NOT pa.fallecido
				JOIN estudiantes.vinculo_familiar v ON v.persona_id = pa.id
				JOIN estudiantes.estudiante e ON e.id = v.estudiante_id AND NOT e.eliminado
				JOIN personas.persona pe ON pe.id = e.persona_id
				JOIN matricula.matricula m ON m.estudiante_id = e.id AND m.estado IN ('MATRICULADO', 'RETIRADO')
				JOIN configuracion.seccion s ON s.id = m.seccion_id
				JOIN configuracion.grado g ON g.codigo = s.grado_codigo
				JOIN configuracion.anio_escolar a ON a.id = s.anio_escolar_id
				WHERE u.id = ?
				ORDER BY estudiante, a.anio DESC""").param(usuarioId).query(MatriculaHijoFila.class).list();
	}

	/**
	 * Un ítem de la sección asociado a una competencia, con la nota del estudiante si ya la tiene o «No evaluado». No
	 * aparecen los ítems que nadie de la sección tiene registrados.
	 */
	public record ItemEstudianteFila(UUID itemId, String item, String tipo, UUID planCursoId, UUID periodoId,
			UUID competenciaId, BigDecimal pesoPorcentaje, BigDecimal notaNumerica, String nivelLogro,
			Instant ordenItem, boolean noEvaluado) {
	}

	public List<ItemEstudianteFila> itemsDelEstudiante(UUID matriculaId, UUID seccionId) {
		return jdbc.sql("""
				SELECT ie.id AS item_id, ie.nombre AS item, ie.tipo, ie.plan_curso_id,
				       ie.periodo_academico_id AS periodo_id, ic.competencia_id, ic.peso_porcentaje,
				       ci.nota_numerica, ci.nivel_logro, ie.creado_en AS orden_item,
				       coalesce(ci.no_evaluado, false) AS no_evaluado
				FROM academico.item_evaluacion ie
				JOIN academico.item_competencia ic ON ic.item_evaluacion_id = ie.id
				LEFT JOIN academico.calificacion_item ci
				  ON ci.item_evaluacion_id = ie.id AND ci.matricula_id = ?
				WHERE ie.seccion_id = ?
				  AND EXISTS (SELECT 1 FROM academico.calificacion_item usado
				              WHERE usado.item_evaluacion_id = ie.id)
				ORDER BY ie.creado_en, ie.nombre""").params(matriculaId, seccionId)
				.query(ItemEstudianteFila.class).list();
	}

	public record DocenteCursoFila(UUID planCursoId, String docente) {
	}

	/** Una sección de un año ACTIVO o CERRADO, con su grado y los parámetros de evaluación del año. */
	public record SeccionContextoFila(UUID id, String seccion, String gradoCodigo, String grado, UUID anioEscolarId,
			int anio, String estadoAnio, String escala, String reglaNota, String consolidacion) {
	}

	public Optional<SeccionContextoFila> seccion(UUID seccionId) {
		return jdbc.sql("""
				SELECT s.id, s.nombre AS seccion, g.codigo AS grado_codigo, g.nombre AS grado,
				       a.id AS anio_escolar_id, a.anio, a.estado AS estado_anio, a.escala_evaluacion AS escala,
				       a.regla_nota, a.consolidacion_periodos AS consolidacion
				FROM configuracion.seccion s
				JOIN configuracion.grado g ON g.codigo = s.grado_codigo
				JOIN configuracion.anio_escolar a ON a.id = s.anio_escolar_id
				WHERE s.id = ? AND a.estado IN ('ACTIVO', 'CERRADO')""").param(seccionId)
				.query(SeccionContextoFila.class).optional();
	}

	/**
	 * Una matrícula MATRICULADO o RETIRADO cualquiera (para el Coordinador), con los mismos datos que la del apoderado.
	 * Las notas de un retirado se siguen viendo y exportando en la libreta; solo no se pueden modificar.
	 */
	public Optional<MatriculaHijoFila> matricula(UUID matriculaId) {
		return jdbc.sql("""
				SELECT e.id AS estudiante_id,
				       concat_ws(' ', pe.nombres, pe.apellido_paterno, pe.apellido_materno) AS estudiante,
				       m.id AS matricula_id, a.id AS anio_escolar_id, a.anio, a.estado AS estado_anio,
				       a.escala_evaluacion AS escala, a.regla_nota, a.consolidacion_periodos AS consolidacion,
				       g.codigo AS grado_codigo, g.nombre AS grado, s.id AS seccion_id, s.nombre AS seccion
				FROM matricula.matricula m
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id AND NOT e.eliminado
				JOIN personas.persona pe ON pe.id = e.persona_id
				JOIN configuracion.seccion s ON s.id = m.seccion_id
				JOIN configuracion.grado g ON g.codigo = s.grado_codigo
				JOIN configuracion.anio_escolar a ON a.id = s.anio_escolar_id
				WHERE m.id = ? AND m.estado IN ('MATRICULADO', 'RETIRADO')""").param(matriculaId)
				.query(MatriculaHijoFila.class).optional();
	}

	/** Docente de cada curso en la sección (uno por curso). */
	public List<DocenteCursoFila> docentes(UUID seccionId) {
		return jdbc.sql("""
				SELECT ds.plan_curso_id,
				       concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS docente
				FROM academico.docente_seccion ds
				JOIN seguridad.usuario u ON u.id = ds.docente_id
				JOIN personas.persona p ON p.id = u.persona_id
				WHERE ds.seccion_id = ?""").param(seccionId).query(DocenteCursoFila.class).list();
	}
}
