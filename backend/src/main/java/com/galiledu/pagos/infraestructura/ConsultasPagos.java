package com.galiledu.pagos.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

	public record Tarifario(UUID id, BigDecimal montoPension, String estado) {
	}

	public record CuotaTarifario(short numero, String nombre, LocalDate fechaVencimiento) {
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

	public Optional<Tarifario> tarifario(UUID anioEscolarId) {
		return jdbc.sql("SELECT id, monto_pension, estado FROM pagos.tarifario WHERE anio_escolar_id = ?")
				.param(anioEscolarId)
				.query((rs, i) -> new Tarifario(rs.getObject(1, UUID.class), rs.getBigDecimal(2), rs.getString(3)))
				.optional();
	}

	public List<CuotaTarifario> cuotasTarifario(UUID tarifarioId) {
		return jdbc.sql("""
				SELECT numero, nombre, fecha_vencimiento FROM pagos.cuota_tarifario
				WHERE tarifario_id = ? ORDER BY numero""")
				.param(tarifarioId)
				.query((rs, i) -> new CuotaTarifario(rs.getShort(1), rs.getString(2),
						rs.getObject(3, LocalDate.class)))
				.list();
	}

	/** Descuento ACTIVO del estudiante que aplica al concepto PENSION (HU-38 lo registra; aquí solo se usa). */
	public Optional<DescuentoActivo> descuentoPension(UUID estudianteId) {
		return jdbc.sql("""
				SELECT da.id, da.porcentaje, da.vigencia_desde, da.vigencia_hasta
				FROM pagos.descuento_aplicado da
				WHERE da.estudiante_id = ? AND da.estado = 'ACTIVO'
				  AND EXISTS (SELECT 1 FROM pagos.descuento_concepto dc
				              WHERE dc.descuento_id = da.descuento_id AND dc.concepto_codigo = 'PENSION')""")
				.param(estudianteId)
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
						           AND q.estado NOT IN ('PAGADA', 'ANULADA') AND q.fecha_vencimiento + t.dias_gracia < :hoy), 0),
						       count(q.id) FILTER (WHERE q.concepto_codigo = 'PENSION'
						           AND q.estado NOT IN ('PAGADA', 'ANULADA') AND q.fecha_vencimiento + t.dias_gracia < :hoy)
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
}
