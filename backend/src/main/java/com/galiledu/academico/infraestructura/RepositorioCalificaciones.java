package com.galiledu.academico.infraestructura;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioCalificaciones {
	private final JdbcClient jdbc;

	public RepositorioCalificaciones(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record CursoFila(UUID planCursoId, UUID seccionId, UUID anioEscolarId, int anio, String estadoAnio,
			String curso,
			String area, String gradoCodigo, String grado, String seccion, String escala, String reglaNota,
			String consolidacion) {
	}

	public record PeriodoFila(UUID id, int numero, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
			LocalDate fechaLimiteNotas, int competencias, int items) {
	}

	public record CompetenciaFila(UUID id, String nombre) {
	}

	public record PeriodoContexto(UUID id, UUID anioEscolarId, int anio, String estadoAnio, String escala,
			String reglaNota, String consolidacion, LocalDate fechaInicio, LocalDate fechaFin,
			LocalDate fechaLimiteNotas) {
	}

	public record ItemFila(UUID id, String nombre, String descripcion, String tipo, Instant actualizadoEn) {
	}

	public record ItemCompetenciaFila(UUID itemEvaluacionId, UUID competenciaId, String competencia,
			BigDecimal pesoPorcentaje) {
	}

	public record ItemContexto(UUID id, UUID planCursoId, UUID seccionId, UUID periodoAcademicoId,
			UUID anioEscolarId, int anio, String estadoAnio, String escala, String reglaNota, String consolidacion,
			LocalDate fechaInicio, LocalDate fechaFin, LocalDate fechaLimiteNotas) {
	}

	public record EstudianteFila(UUID matriculaId, UUID estudianteId, String estudiante) {
	}

	public record CalificacionFila(UUID id, UUID matriculaId, UUID estudianteId, String estudiante,
			BigDecimal notaNumerica, String nivelLogro, String evidenciaNombreArchivo, String evidenciaTipoMime,
			Integer evidenciaTamanoBytes, Instant evidenciaSubidoEn) {
	}

	public record EvidenciaFila(String nombreArchivo, String tipoMime, byte[] contenido) {
	}

	public record DetalleReporteFila(UUID matriculaId, UUID estudianteId, String estudiante, UUID itemId,
			String item, UUID competenciaId, String competencia, BigDecimal pesoPorcentaje, BigDecimal notaNumerica,
			String nivelLogro, String evidenciaNombre, Instant ordenItem) {
	}

	public record ParametrosPlan(UUID anioEscolarId, String gradoCodigo, String escala, String reglaNota,
			String consolidacion) {
	}

	public record PeriodoAnio(UUID id, LocalDate fechaInicio) {
	}

	/** Un ítem de la competencia para un estudiante: sin nota ni nivel, todavía no está calificado. */
	public record NotaCompetenciaFila(UUID matriculaId, UUID planCursoId, UUID periodoId, BigDecimal pesoPorcentaje,
			BigDecimal notaNumerica, String nivelLogro, Instant ordenItem) {
	}

	public List<CursoFila> cursos(UUID docenteId) {
		return jdbc.sql("""
				SELECT pc.id AS plan_curso_id, ds.seccion_id, ae.id AS anio_escolar_id, ae.anio,
				       ae.estado AS estado_anio,
				       c.nombre AS curso, a.nombre AS area, pc.grado_codigo, g.nombre AS grado,
				       s.nombre AS seccion, ae.escala_evaluacion AS escala, ae.regla_nota,
				       ae.consolidacion_periodos AS consolidacion
				FROM academico.docente_seccion ds
				JOIN academico.plan_curso pc ON pc.id = ds.plan_curso_id
				JOIN academico.curso c ON c.id = pc.curso_id
				JOIN academico.area a ON a.id = c.area_id
				JOIN configuracion.anio_escolar ae ON ae.id = pc.anio_escolar_id
				JOIN configuracion.grado g ON g.codigo = pc.grado_codigo
				JOIN configuracion.seccion s ON s.id = ds.seccion_id
				WHERE ds.docente_id = ? AND s.estado = 'ACTIVA'
				ORDER BY ae.anio DESC, g.orden, c.nombre, s.nombre""")
				.param(docenteId).query(CursoFila.class).list();
	}

	public List<PeriodoFila> periodos(UUID planCursoId, UUID seccionId) {
		return jdbc.sql("""
				SELECT p.id, p.numero, p.nombre, p.fecha_inicio, p.fecha_fin, p.fecha_limite_notas,
				       (SELECT count(*) FROM academico.plan_competencia pcx
				         WHERE pcx.plan_curso_id = pc.id)::int AS competencias,
				       (SELECT count(*) FROM academico.item_evaluacion ie
				         WHERE ie.plan_curso_id = pc.id AND ie.seccion_id = ?
				           AND ie.periodo_academico_id = p.id)::int AS items
				FROM academico.plan_curso pc
				JOIN configuracion.periodo_academico p ON p.anio_escolar_id = pc.anio_escolar_id
				WHERE pc.id = ?
				ORDER BY p.numero""").params(seccionId, planCursoId).query(PeriodoFila.class).list();
	}

	public List<CompetenciaFila> competencias(UUID planCursoId) {
		return jdbc.sql("""
				SELECT c.id, c.nombre
				FROM academico.plan_competencia pc
				JOIN academico.competencia c ON c.id = pc.competencia_id
				WHERE pc.plan_curso_id = ?
				ORDER BY c.orden, c.nombre""").param(planCursoId).query(CompetenciaFila.class).list();
	}

	public boolean tieneAcceso(UUID docenteId, UUID planCursoId, UUID seccionId) {
		return jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM academico.docente_seccion
				 WHERE docente_id = ? AND plan_curso_id = ? AND seccion_id = ?)""")
				.params(docenteId, planCursoId, seccionId).query(Boolean.class).single();
	}

	public Optional<PeriodoContexto> periodo(UUID planCursoId, UUID periodoId) {
		return jdbc.sql("""
				SELECT p.id, ae.id AS anio_escolar_id, ae.anio, ae.estado AS estado_anio,
				       ae.escala_evaluacion AS escala, ae.regla_nota, ae.consolidacion_periodos AS consolidacion,
				       p.fecha_inicio, p.fecha_fin, p.fecha_limite_notas
				FROM academico.plan_curso pc
				JOIN configuracion.anio_escolar ae ON ae.id = pc.anio_escolar_id
				JOIN configuracion.periodo_academico p ON p.anio_escolar_id = ae.id
				WHERE pc.id = ? AND p.id = ?""").params(planCursoId, periodoId)
				.query(PeriodoContexto.class).optional();
	}

	public List<ItemFila> items(UUID planCursoId, UUID seccionId, UUID periodoId) {
		return jdbc.sql("""
				SELECT id, nombre, descripcion, tipo, actualizado_en
				FROM academico.item_evaluacion
				WHERE plan_curso_id = ? AND seccion_id = ? AND periodo_academico_id = ?
				ORDER BY creado_en, nombre""").params(planCursoId, seccionId, periodoId).query(ItemFila.class).list();
	}

	public List<ItemFila> itemsDeIds(List<UUID> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		return jdbc.sql("""
				SELECT id, nombre, descripcion, tipo, actualizado_en
				FROM academico.item_evaluacion WHERE id IN (:ids)
				ORDER BY creado_en, nombre""").param("ids", ids).query(ItemFila.class).list();
	}

	public List<ItemCompetenciaFila> competenciasDeItems(List<UUID> items) {
		if (items.isEmpty()) {
			return List.of();
		}
		return jdbc.sql("""
				SELECT ic.item_evaluacion_id, ic.competencia_id, c.nombre AS competencia, ic.peso_porcentaje
				FROM academico.item_competencia ic
				JOIN academico.competencia c ON c.id = ic.competencia_id
				WHERE ic.item_evaluacion_id IN (:items)
				ORDER BY c.orden, c.nombre""").param("items", items).query(ItemCompetenciaFila.class).list();
	}

	public Optional<ItemContexto> item(UUID itemId) {
		return jdbc.sql("""
				SELECT ie.id, ie.plan_curso_id, ie.seccion_id, ie.periodo_academico_id,
				       ae.id AS anio_escolar_id, ae.anio, ae.estado AS estado_anio,
				       ae.escala_evaluacion AS escala, ae.regla_nota,
				       ae.consolidacion_periodos AS consolidacion,
				       p.fecha_inicio, p.fecha_fin, p.fecha_limite_notas
				FROM academico.item_evaluacion ie
				JOIN academico.plan_curso pc ON pc.id = ie.plan_curso_id
				JOIN configuracion.anio_escolar ae ON ae.id = pc.anio_escolar_id
				JOIN configuracion.periodo_academico p ON p.id = ie.periodo_academico_id
				WHERE ie.id = ?""").param(itemId).query(ItemContexto.class).optional();
	}

	public boolean existeNombre(UUID planCursoId, UUID seccionId, UUID periodoId, String nombre, UUID excepto) {
		return jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM academico.item_evaluacion
				 WHERE plan_curso_id = ? AND seccion_id = ? AND periodo_academico_id = ?
				   AND lower(nombre) = lower(?) AND id IS DISTINCT FROM ?)""")
				.params(planCursoId, seccionId, periodoId, nombre, excepto).query(Boolean.class).single();
	}

	public void bloquearPlanCurso(UUID planCursoId) {
		jdbc.sql("SELECT id FROM academico.plan_curso WHERE id = ? FOR UPDATE").param(planCursoId)
				.query(UUID.class).single();
	}

	public BigDecimal pesoAsignado(UUID planCursoId, UUID seccionId, UUID periodoId, UUID competenciaId,
			UUID excepto) {
		return jdbc.sql("""
				SELECT COALESCE(sum(ic.peso_porcentaje), 0)
				FROM academico.item_competencia ic
				JOIN academico.item_evaluacion ie ON ie.id = ic.item_evaluacion_id
				WHERE ie.plan_curso_id = ? AND ie.seccion_id = ? AND ie.periodo_academico_id = ?
				  AND ic.competencia_id = ? AND ie.id IS DISTINCT FROM ?""")
				.params(planCursoId, seccionId, periodoId, competenciaId, excepto).query(BigDecimal.class).single();
	}

	public UUID crearItem(UUID planCursoId, UUID seccionId, UUID periodoId, String nombre, String descripcion,
			String tipo, UUID docenteId) {
		return jdbc.sql("""
				INSERT INTO academico.item_evaluacion
				    (plan_curso_id, anio_escolar_id, grado_codigo, seccion_id, periodo_academico_id,
				     nombre, descripcion, tipo, creado_por)
				SELECT pc.id, pc.anio_escolar_id, pc.grado_codigo, ?, ?, ?, ?, ?, ?
				FROM academico.plan_curso pc WHERE pc.id = ?
				RETURNING id""")
				.params(seccionId, periodoId, nombre, descripcion, tipo, docenteId, planCursoId)
				.query(UUID.class).single();
	}

	public void actualizarItem(UUID itemId, String nombre, String descripcion, String tipo) {
		jdbc.sql("""
				UPDATE academico.item_evaluacion
				SET nombre = ?, descripcion = ?, tipo = ?, actualizado_en = now()
				WHERE id = ?""").params(nombre, descripcion, tipo, itemId).update();
	}

	public void reemplazarCompetencias(UUID itemId, UUID planCursoId, List<CompetenciaPeso> competencias) {
		jdbc.sql("DELETE FROM academico.item_competencia WHERE item_evaluacion_id = ?").param(itemId).update();
		for (CompetenciaPeso competencia : competencias) {
			jdbc.sql("""
					INSERT INTO academico.item_competencia
					    (item_evaluacion_id, plan_curso_id, competencia_id, peso_porcentaje)
					VALUES (?, ?, ?, ?)""")
					.params(itemId, planCursoId, competencia.competenciaId(), competencia.pesoPorcentaje()).update();
		}
	}

	public record CompetenciaPeso(UUID competenciaId, BigDecimal pesoPorcentaje) {
	}

	public boolean tieneCalificaciones(UUID itemId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM academico.calificacion_item WHERE item_evaluacion_id = ?)")
				.param(itemId).query(Boolean.class).single();
	}

	public void eliminarItem(UUID itemId) {
		jdbc.sql("DELETE FROM academico.item_evaluacion WHERE id = ?").param(itemId).update();
	}

	public List<EstudianteFila> estudiantes(UUID seccionId) {
		return jdbc.sql("""
				SELECT m.id AS matricula_id, e.id AS estudiante_id,
				       concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS estudiante
				FROM matricula.matricula m
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				WHERE m.seccion_id = ? AND m.estado = 'MATRICULADO' AND NOT e.eliminado
				ORDER BY p.apellido_paterno, p.apellido_materno, p.nombres""").param(seccionId)
				.query(EstudianteFila.class).list();
	}

	public List<CalificacionFila> calificaciones(UUID itemId, UUID seccionId) {
		return jdbc.sql("""
				SELECT ci.id, m.id AS matricula_id, e.id AS estudiante_id,
				       concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS estudiante,
				       ci.nota_numerica, ci.nivel_logro,
				       ev.nombre_archivo AS evidencia_nombre_archivo, ev.tipo_mime AS evidencia_tipo_mime,
				       ev.tamano_bytes AS evidencia_tamano_bytes, ev.subido_en AS evidencia_subido_en
				FROM matricula.matricula m
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				LEFT JOIN academico.calificacion_item ci
				       ON ci.matricula_id = m.id AND ci.item_evaluacion_id = ?
				LEFT JOIN academico.evidencia_calificacion ev ON ev.calificacion_id = ci.id
				WHERE m.seccion_id = ? AND m.estado = 'MATRICULADO' AND NOT e.eliminado
				ORDER BY p.apellido_paterno, p.apellido_materno, p.nombres""").params(itemId, seccionId)
				.query(CalificacionFila.class).list();
	}

	public boolean matriculaPertenece(UUID matriculaId, UUID seccionId) {
		return jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM matricula.matricula
				 WHERE id = ? AND seccion_id = ? AND estado = 'MATRICULADO')""")
				.params(matriculaId, seccionId).query(Boolean.class).single();
	}

	public UUID guardarCalificacion(UUID itemId, UUID matriculaId, BigDecimal nota, String nivel, UUID docenteId) {
		return jdbc.sql("""
				INSERT INTO academico.calificacion_item
				    (item_evaluacion_id, seccion_id, matricula_id, nota_numerica, nivel_logro, registrado_por)
				SELECT ie.id, ie.seccion_id, ?, ?, ?, ?
				FROM academico.item_evaluacion ie WHERE ie.id = ?
				ON CONFLICT (item_evaluacion_id, matricula_id) DO UPDATE
				SET nota_numerica = EXCLUDED.nota_numerica, nivel_logro = EXCLUDED.nivel_logro,
				    registrado_por = EXCLUDED.registrado_por, actualizado_en = now()
				RETURNING id""").params(matriculaId, nota, nivel, docenteId, itemId).query(UUID.class).single();
	}

	public void quitarCalificacion(UUID itemId, UUID matriculaId) {
		jdbc.sql("DELETE FROM academico.calificacion_item WHERE item_evaluacion_id = ? AND matricula_id = ?")
				.params(itemId, matriculaId).update();
	}

	public Optional<UUID> calificacionId(UUID itemId, UUID matriculaId) {
		return jdbc.sql("""
				SELECT id FROM academico.calificacion_item
				WHERE item_evaluacion_id = ? AND matricula_id = ?""").params(itemId, matriculaId)
				.query(UUID.class).optional();
	}

	public void guardarEvidencia(UUID calificacionId, String nombre, String tipo, byte[] contenido) {
		jdbc.sql("""
				INSERT INTO academico.evidencia_calificacion
				    (calificacion_id, nombre_archivo, tipo_mime, tamano_bytes, contenido)
				VALUES (?, ?, ?, ?, ?)
				ON CONFLICT (calificacion_id) DO UPDATE
				SET nombre_archivo = EXCLUDED.nombre_archivo, tipo_mime = EXCLUDED.tipo_mime,
				    tamano_bytes = EXCLUDED.tamano_bytes, contenido = EXCLUDED.contenido, subido_en = now()""")
				.params(calificacionId, nombre, tipo, contenido.length, contenido).update();
	}

	public Optional<EvidenciaFila> evidencia(UUID calificacionId) {
		return jdbc.sql("""
				SELECT nombre_archivo, tipo_mime, contenido
				FROM academico.evidencia_calificacion WHERE calificacion_id = ?""").param(calificacionId)
				.query(EvidenciaFila.class).optional();
	}

	public void quitarEvidencia(UUID calificacionId) {
		jdbc.sql("DELETE FROM academico.evidencia_calificacion WHERE calificacion_id = ?")
				.param(calificacionId).update();
	}

	public List<DetalleReporteFila> detallesReporte(UUID planCursoId, UUID seccionId, UUID periodoId) {
		return jdbc.sql("""
				SELECT m.id AS matricula_id, e.id AS estudiante_id,
				       concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS estudiante,
				       ie.id AS item_id, ie.nombre AS item, ic.competencia_id, c.nombre AS competencia,
				       ic.peso_porcentaje, ci.nota_numerica, ci.nivel_logro,
				       ev.nombre_archivo AS evidencia_nombre, ie.creado_en AS orden_item
				FROM matricula.matricula m
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				JOIN academico.item_evaluacion ie
				  ON ie.plan_curso_id = ? AND ie.seccion_id = m.seccion_id AND ie.periodo_academico_id = ?
				JOIN academico.item_competencia ic ON ic.item_evaluacion_id = ie.id
				JOIN academico.competencia c ON c.id = ic.competencia_id
				LEFT JOIN academico.calificacion_item ci
				  ON ci.item_evaluacion_id = ie.id AND ci.matricula_id = m.id
				LEFT JOIN academico.evidencia_calificacion ev ON ev.calificacion_id = ci.id
				WHERE m.seccion_id = ? AND m.estado = 'MATRICULADO' AND NOT e.eliminado
				ORDER BY p.apellido_paterno, p.apellido_materno, p.nombres, ie.creado_en, c.orden""")
				.params(planCursoId, periodoId, seccionId).query(DetalleReporteFila.class).list();
	}

	public ParametrosPlan parametrosPlan(UUID planCursoId) {
		return jdbc.sql("""
				SELECT pc.anio_escolar_id, pc.grado_codigo, ae.escala_evaluacion AS escala, ae.regla_nota,
				       ae.consolidacion_periodos AS consolidacion
				FROM academico.plan_curso pc
				JOIN configuracion.anio_escolar ae ON ae.id = pc.anio_escolar_id
				WHERE pc.id = ?""").param(planCursoId).query(ParametrosPlan.class).single();
	}

	public List<PeriodoAnio> periodosDelAnio(UUID anioEscolarId) {
		return jdbc.sql("""
				SELECT id, fecha_inicio FROM configuracion.periodo_academico
				WHERE anio_escolar_id = ? ORDER BY numero""").param(anioEscolarId).query(PeriodoAnio.class).list();
	}

	/** Ítems de una competencia en todos los cursos y periodos del año, para los estudiantes de una sección. */
	public List<NotaCompetenciaFila> notasDeCompetencia(UUID anioEscolarId, String gradoCodigo, UUID seccionId,
			UUID competenciaId) {
		return jdbc.sql("""
				SELECT m.id AS matricula_id, ie.plan_curso_id, ie.periodo_academico_id AS periodo_id,
				       ic.peso_porcentaje, ci.nota_numerica, ci.nivel_logro, ie.creado_en AS orden_item
				FROM academico.item_evaluacion ie
				JOIN academico.item_competencia ic ON ic.item_evaluacion_id = ie.id AND ic.competencia_id = ?
				JOIN matricula.matricula m ON m.seccion_id = ie.seccion_id AND m.estado = 'MATRICULADO'
				LEFT JOIN academico.calificacion_item ci
				  ON ci.item_evaluacion_id = ie.id AND ci.matricula_id = m.id
				WHERE ie.anio_escolar_id = ? AND ie.grado_codigo = ? AND ie.seccion_id = ?
				ORDER BY ie.creado_en""").params(competenciaId, anioEscolarId, gradoCodigo, seccionId)
				.query(NotaCompetenciaFila.class).list();
	}

	public Optional<Integer> notaAprobatoria(LocalDate fecha) {
		return jdbc.sql("""
				SELECT n.nota_desde
				FROM academico.tabla_conversion t
				JOIN academico.tabla_conversion_nivel n ON n.tabla_id = t.id AND n.nivel = 'A'
				WHERE t.vigente_desde <= ?
				ORDER BY t.vigente_desde DESC
				LIMIT 1""").param(fecha).query(Integer.class).optional();
	}
}
