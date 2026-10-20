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
}
