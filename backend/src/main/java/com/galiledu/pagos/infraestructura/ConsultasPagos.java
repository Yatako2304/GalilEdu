package com.galiledu.pagos.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Lecturas de solo consulta que necesita Pagos para generar el cronograma (HU-37).
 * La matrícula se lee por SQL porque el módulo de Matrícula aún no expone su servicio; cuando exista, reemplazar
 * {@link #matricula(UUID)} por una llamada a ese servicio.
 */
@Repository
public class ConsultasPagos {
	private final JdbcClient jdbc;

	public ConsultasPagos(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record MatriculaParaPagos(UUID estudianteId, UUID anioEscolarId, int anio, String estado,
			OffsetDateTime confirmadoEn) {
	}

	public record DescuentoActivo(UUID id, BigDecimal porcentaje, LocalDate vigenciaDesde, LocalDate vigenciaHasta) {
		public boolean vigenteEn(LocalDate fecha) {
			return !fecha.isBefore(vigenciaDesde) && (vigenciaHasta == null || !fecha.isAfter(vigenciaHasta));
		}
	}

	public Optional<MatriculaParaPagos> matricula(UUID matriculaId) {
		return jdbc.sql("""
				SELECT m.estudiante_id, m.anio_escolar_id, a.anio, m.estado, m.confirmado_en
				FROM matricula.matricula m JOIN configuracion.anio_escolar a ON a.id = m.anio_escolar_id
				WHERE m.id = ?""")
				.param(matriculaId)
				.query((rs, i) -> new MatriculaParaPagos(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
						rs.getInt(3), rs.getString(4), rs.getObject(5, OffsetDateTime.class)))
				.optional();
	}

	/** Descuento ACTIVO del estudiante que aplica al concepto indicado (HU-38 lo registra; aquí solo se usa). */
	public Optional<DescuentoActivo> descuentoPara(UUID estudianteId, String concepto) {
		return jdbc.sql("""
				SELECT da.id, da.porcentaje, da.vigencia_desde, da.vigencia_hasta
				FROM pagos.descuento_aplicado da
				WHERE da.estudiante_id = ? AND da.estado = 'ACTIVO'
				  AND EXISTS (SELECT 1 FROM pagos.descuento_concepto dc
				              WHERE dc.descuento_id = da.descuento_id AND dc.concepto_codigo = ?)""")
				.params(estudianteId, concepto)
				.query((rs, i) -> new DescuentoActivo(rs.getObject(1, UUID.class), rs.getBigDecimal(2),
						rs.getObject(3, LocalDate.class), rs.getObject(4, LocalDate.class)))
				.optional();
	}

	public record FilaEstadoCuenta(UUID compromisoId, UUID estudianteId, String estado, String nombre,
			String numeroDocumento, String apoderado, String seccion, int antiguedad, BigDecimal total,
			BigDecimal pagado, BigDecimal deudaVencida, int cuotasVencidas) {
	}

	/** Año escolar ACTIVO; si no hay, el más reciente con compromisos. */
	public Optional<Integer> anioActivo() {
		return jdbc.sql("""
				SELECT a.anio FROM configuracion.anio_escolar a
				ORDER BY (a.estado = 'ACTIVO') DESC,
				         EXISTS (SELECT 1 FROM pagos.compromiso_pago c WHERE c.anio_escolar_id = a.id) DESC,
				         a.anio DESC
				LIMIT 1""")
				.query(Integer.class)
				.optional();
	}

	/**
	 * Una fila por compromiso del año (P2): datos del alumno, total, pagado y deuda vencida. "Vencida" es la regla de
	 * RF-91 (pensión impaga después del vencimiento más los días de gracia).
	 */
	public List<FilaEstadoCuenta> estadoCuenta(int anio, LocalDate hoy) {
		return jdbc
				.sql("""
						SELECT c.id, c.estudiante_id, c.estado,
						       concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS nombre,
						       p.numero_documento,
						       (SELECT min(concat_ws(' ', pa.nombres, pa.apellido_paterno, pa.apellido_materno))
						        FROM estudiantes.vinculo_familiar v JOIN personas.persona pa ON pa.id = v.persona_id
						        WHERE v.estudiante_id = c.estudiante_id AND v.es_responsable_matricula),
						       (SELECT g.numero || '.° ' || s.nombre
						        FROM matricula.matricula m
						        JOIN configuracion.seccion s ON s.id = m.seccion_id
						        JOIN configuracion.grado g ON g.codigo = s.grado_codigo
						        WHERE m.id = c.matricula_id),
						       (SELECT count(*) FROM matricula.matricula m2
						        JOIN configuracion.anio_escolar a2 ON a2.id = m2.anio_escolar_id
						        WHERE m2.estudiante_id = c.estudiante_id AND m2.estado IN ('MATRICULADO', 'RETIRADO')
						          AND a2.anio <= a.anio),
						       coalesce(sum(q.monto_final) FILTER (WHERE q.estado <> 'ANULADA'), 0),
						       coalesce(sum(q.monto_pagado) FILTER (WHERE q.estado <> 'ANULADA'), 0),
						       coalesce(sum(q.monto_final - q.monto_pagado) FILTER (WHERE q.concepto_codigo = 'PENSION'
						           AND q.estado NOT IN ('PAGADA', 'ANULADA', 'EN_PROCESO') AND q.fecha_vencimiento + t.dias_gracia < :hoy), 0),
						       count(q.id) FILTER (WHERE q.concepto_codigo = 'PENSION'
						           AND q.estado NOT IN ('PAGADA', 'ANULADA', 'EN_PROCESO') AND q.fecha_vencimiento + t.dias_gracia < :hoy)
						FROM pagos.compromiso_pago c
						JOIN configuracion.anio_escolar a ON a.id = c.anio_escolar_id
						JOIN pagos.tarifario t ON t.id = c.tarifario_id
						JOIN estudiantes.estudiante e ON e.id = c.estudiante_id
						JOIN personas.persona p ON p.id = e.persona_id
						LEFT JOIN pagos.cuota q ON q.compromiso_id = c.id
						WHERE a.anio = :anio
						GROUP BY c.id, c.estudiante_id, c.estado, c.matricula_id, a.anio, t.dias_gracia,
						         p.nombres, p.apellido_paterno, p.apellido_materno, p.numero_documento
						ORDER BY nombre""")
				.param("anio", anio)
				.param("hoy", hoy)
				.query((rs, i) -> new FilaEstadoCuenta(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
						rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
						rs.getInt(8), rs.getBigDecimal(9), rs.getBigDecimal(10), rs.getBigDecimal(11), rs.getInt(12)))
				.list();
	}

	public record CuotaVencidaFila(UUID estudianteId, String estudiante, String concepto, LocalDate fechaVencimiento,
			BigDecimal saldo) {
	}

	/**
	 * Pensiones impagas de los estudiantes indicados que ya pasaron su vencimiento más los días de gracia del tarifario
	 * (RF-91). Es la misma regla de {@code Cuota.estaVencida}, pero en SQL para varios estudiantes a la vez.
	 */
	public List<CuotaVencidaFila> cuotasVencidas(Collection<UUID> estudianteIds, LocalDate hoy) {
		if (estudianteIds.isEmpty()) {
			return List.of();
		}
		return jdbc.sql("""
				SELECT c.estudiante_id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno),
				       q.nombre, q.fecha_vencimiento, q.monto_final - q.monto_pagado
				FROM pagos.cuota q
				JOIN pagos.compromiso_pago c ON c.id = q.compromiso_id
				JOIN pagos.tarifario t ON t.id = c.tarifario_id
				JOIN estudiantes.estudiante e ON e.id = c.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				WHERE c.estudiante_id IN (:ids) AND c.estado = 'VIGENTE'
				  AND q.concepto_codigo = 'PENSION' AND q.estado NOT IN ('PAGADA', 'ANULADA', 'EN_PROCESO')
				  AND q.fecha_vencimiento + t.dias_gracia < :hoy
				ORDER BY q.fecha_vencimiento, 2""")
				.param("ids", estudianteIds)
				.param("hoy", hoy)
				.query((rs, i) -> new CuotaVencidaFila(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
						rs.getObject(4, LocalDate.class), rs.getBigDecimal(5)))
				.list();
	}

	public record EstudianteACargo(UUID id, String nombre) {
	}

	/**
	 * HU-41: estudiantes vinculados al usuario como padre, madre o apoderado. Cualquiera de ellos puede pagar (acta del
	 * PO, 30/10: "ambos padres pueden consultar y pagar").
	 */
	public List<EstudianteACargo> estudiantesDelUsuario(UUID usuarioId) {
		return jdbc.sql("""
				SELECT DISTINCT e.id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS nombre
				FROM seguridad.usuario u
				JOIN estudiantes.vinculo_familiar v ON v.persona_id = u.persona_id
				JOIN estudiantes.estudiante e ON e.id = v.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				WHERE u.id = ? AND NOT e.eliminado
				ORDER BY nombre""")
				.param(usuarioId)
				.query((rs, i) -> new EstudianteACargo(rs.getObject(1, UUID.class), rs.getString(2)))
				.list();
	}

	/** OP-2026-000001 */
	public String siguienteCodigoOrden(int anio) {
		long n = jdbc.sql("SELECT nextval('pagos.orden_pago_codigo_seq')").query(Long.class).single();
		return "OP-%d-%06d".formatted(anio, n);
	}

	/** C-2026-00008: siguiente número de constancia del año (correlativo_constancia). */
	public String siguienteConstancia(int anio) {
		int n = jdbc.sql("""
				INSERT INTO pagos.correlativo_constancia (anio, ultimo) VALUES (:anio, 1)
				ON CONFLICT (anio) DO UPDATE SET ultimo = pagos.correlativo_constancia.ultimo + 1
				RETURNING ultimo""")
				.param("anio", anio)
				.query(Integer.class)
				.single();
		return "C-%d-%05d".formatted(anio, n);
	}

	public record FilaCuotaMatricula(UUID cuotaId, UUID compromisoId, UUID estudianteId, int anio, String concepto,
			BigDecimal montoBase, BigDecimal montoDescuento, BigDecimal montoFinal, BigDecimal montoPagado,
			LocalDate fechaVencimiento, String estado, LocalDate fechaPago, String responsable) {
	}

	/** RF-80: la cuota de matrícula (no anulada) del compromiso de esa matrícula. */
	public Optional<FilaCuotaMatricula> cuotaMatricula(UUID matriculaId) {
		return jdbc.sql("""
				SELECT q.id, c.id, c.estudiante_id, a.anio, q.nombre, q.monto_base, q.monto_descuento, q.monto_final,
				       q.monto_pagado, q.fecha_vencimiento, q.estado, q.fecha_pago,
				       (SELECT min(concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno))
				        FROM estudiantes.vinculo_familiar v JOIN personas.persona p ON p.id = v.persona_id
				        WHERE v.estudiante_id = c.estudiante_id AND v.es_responsable_matricula)
				FROM pagos.cuota q
				JOIN pagos.compromiso_pago c ON c.id = q.compromiso_id
				JOIN configuracion.anio_escolar a ON a.id = c.anio_escolar_id
				WHERE c.matricula_id = ? AND q.concepto_codigo = 'MATRICULA' AND q.estado <> 'ANULADA'""")
				.param(matriculaId)
				.query((rs, i) -> new FilaCuotaMatricula(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
						rs.getObject(3, UUID.class), rs.getInt(4), rs.getString(5), rs.getBigDecimal(6),
						rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBigDecimal(9),
						rs.getObject(10, LocalDate.class),
						rs.getString(11), rs.getObject(12, LocalDate.class), rs.getString(13)))
				.optional();
	}

	/** RF-74: de los códigos indicados, los que existen en el catálogo de conceptos de pago. */
	public List<String> conceptosExistentes(Collection<String> codigos) {
		if (codigos.isEmpty()) {
			return List.of();
		}
		return jdbc.sql("SELECT codigo FROM pagos.concepto WHERE codigo IN (:codigos)")
				.param("codigos", codigos)
				.query(String.class)
				.list();
	}

	/**
	 * RF-75, RF-86: estudiantes con matrícula formalizada en ese año que comparten un padre, madre o apoderado con el
	 * estudiante, contándolo a él (Sofía sola = 1).
	 */
	public int hermanosMatriculados(UUID estudianteId, UUID anioEscolarId) {
		return jdbc.sql("""
				SELECT count(DISTINCT m.estudiante_id)
				FROM matricula.matricula m
				WHERE m.anio_escolar_id = :anio AND m.estado = 'MATRICULADO'
				  AND (m.estudiante_id = :estudiante OR m.estudiante_id IN (
				       SELECT h.estudiante_id FROM estudiantes.vinculo_familiar v
				       JOIN estudiantes.vinculo_familiar h ON h.persona_id = v.persona_id
				       WHERE v.estudiante_id = :estudiante AND v.persona_id IS NOT NULL))""")
				.param("anio", anioEscolarId)
				.param("estudiante", estudianteId)
				.query(Integer.class)
				.single();
	}

	/** HU-40: ¿el usuario es padre, madre o apoderado del estudiante? Ambos padres pueden consultar (corrección 2). */
	public boolean esHijoDelUsuario(UUID usuarioId, UUID estudianteId) {
		return jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM seguridad.usuario u
				               JOIN estudiantes.vinculo_familiar v ON v.persona_id = u.persona_id
				               WHERE u.id = ? AND v.estudiante_id = ?)""")
				.params(usuarioId, estudianteId)
				.query(Boolean.class)
				.single();
	}

	public record FilaHijoDeuda(UUID estudianteId, String codigo, String nombres, String apellidos, String grado,
			String seccion, BigDecimal saldo, int cuotasVencidas) {
	}

	/**
	 * RF-95: hijos del usuario con su grado y sección (de la matrícula más reciente), saldo pendiente (cuotas no
	 * pagadas ni anuladas de compromisos vigentes) y pensiones vencidas (RF-91), ordenados por apellido. El código del
	 * estudiante es su número de documento: la tabla de estudiantes no tiene otro código.
	 */
	public List<FilaHijoDeuda> hijosConDeuda(UUID usuarioId, LocalDate hoy) {
		return jdbc.sql("""
				SELECT e.id, p.numero_documento, p.nombres, concat_ws(' ', p.apellido_paterno, p.apellido_materno),
				       ult.grado, ult.seccion,
				       coalesce((SELECT sum(q.monto_final - q.monto_pagado)
				                 FROM pagos.compromiso_pago c JOIN pagos.cuota q ON q.compromiso_id = c.id
				                 WHERE c.estudiante_id = e.id AND c.estado = 'VIGENTE'
				                   AND q.estado NOT IN ('PAGADA', 'ANULADA')), 0),
				       (SELECT count(*)
				        FROM pagos.compromiso_pago c
				        JOIN pagos.tarifario t ON t.id = c.tarifario_id
				        JOIN pagos.cuota q ON q.compromiso_id = c.id
				        WHERE c.estudiante_id = e.id AND c.estado = 'VIGENTE' AND q.concepto_codigo = 'PENSION'
				          AND q.estado NOT IN ('PAGADA', 'ANULADA') AND q.fecha_vencimiento + t.dias_gracia < :hoy)
				FROM (SELECT DISTINCT v.estudiante_id FROM seguridad.usuario u
				      JOIN estudiantes.vinculo_familiar v ON v.persona_id = u.persona_id
				      WHERE u.id = :usuario) h
				JOIN estudiantes.estudiante e ON e.id = h.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				LEFT JOIN LATERAL (
				     SELECT g.nombre AS grado, s.nombre AS seccion
				     FROM matricula.matricula m
				     JOIN configuracion.anio_escolar a ON a.id = m.anio_escolar_id
				     JOIN configuracion.grado g ON g.codigo = m.grado_codigo
				     LEFT JOIN configuracion.seccion s ON s.id = m.seccion_id
				     WHERE m.estudiante_id = e.id AND m.estado <> 'ANULADA'
				     ORDER BY a.anio DESC LIMIT 1) ult ON TRUE
				WHERE NOT e.eliminado
				ORDER BY p.apellido_paterno, p.apellido_materno, p.nombres""")
				.param("usuario", usuarioId)
				.param("hoy", hoy)
				.query((rs, i) -> new FilaHijoDeuda(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
						rs.getString(4), rs.getString(5), rs.getString(6), rs.getBigDecimal(7), rs.getInt(8)))
				.list();
	}

	public record FilaPago(UUID id, String numeroConstancia, OffsetDateTime fechaPago, BigDecimal montoTotal,
			BigDecimal capital, BigDecimal recargo, String estado, List<String> cuotas) {
	}

	/**
	 * RF-97 (y la pestaña "Pagos" de P8): pagos que cubrieron cuotas del compromiso, del más reciente al más antiguo.
	 */
	public List<FilaPago> pagosDelCompromiso(UUID compromisoId) {
		return jdbc.sql("""
				SELECT pg.id, pg.numero_constancia, pg.fecha_pago, pg.monto_total, sum(d.capital), sum(d.recargo),
				       pg.estado, array_agg(q.nombre ORDER BY q.fecha_vencimiento)
				FROM pagos.pago pg
				JOIN pagos.pago_detalle d ON d.pago_id = pg.id
				JOIN pagos.cuota q ON q.id = d.cuota_id
				WHERE q.compromiso_id = ?
				GROUP BY pg.id
				ORDER BY pg.fecha_pago DESC, pg.numero_constancia DESC""")
				.param(compromisoId)
				.query((rs, i) -> new FilaPago(rs.getObject(1, UUID.class), rs.getString(2),
						rs.getObject(3, OffsetDateTime.class), rs.getBigDecimal(4), rs.getBigDecimal(5),
						rs.getBigDecimal(6), rs.getString(7), List.of((String[]) rs.getArray(8).getArray())))
				.list();
	}

	/** P10 "Aplicado por": nombre y primer apellido del usuario (null si no hay usuario). */
	public String nombreDeUsuario(UUID usuarioId) {
		if (usuarioId == null) {
			return null;
		}
		return jdbc.sql("""
				SELECT concat_ws(' ', p.nombres, p.apellido_paterno)
				FROM seguridad.usuario u JOIN personas.persona p ON p.id = u.persona_id WHERE u.id = ?""")
				.param(usuarioId)
				.query(String.class)
				.optional()
				.orElse(null);
	}
}
