package com.galiledu.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * Tablas de matrícula y pagos (V5): las relaciones entre módulos y las reglas que guarda la propia BD.
 */
class EsquemaMatriculaPagosIT extends PruebaIntegracion {
	private static final String SOFIA = "20000000-0000-0000-0000-000000000002";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	private static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String MATRICULA_SOFIA = "50000000-0000-0000-0000-000000000001";
	private static final String COMPROMISO_SOFIA = "51000000-0000-0000-0000-000000000001";
	private static final String DESCUENTO_BECA = "52000000-0000-0000-0000-000000000002";
	private static final String LUIS_PAREDES = "10000000-0000-0000-0000-000000000003";

	@Autowired
	private JdbcClient jdbc;

	@Test
	void elApoderadoLlegaASusHijosSusMatriculasYSuDeudaVencida() {
		// Usuario → persona → vínculo de responsable → estudiante → matrícula/sección y compromiso → cuotas.
		var hijos = jdbc.sql("""
				SELECT p.nombres, g.nombre AS grado, s.nombre AS seccion, m.estado
				FROM seguridad.usuario u
				JOIN estudiantes.vinculo_familiar v ON v.persona_id = u.persona_id AND v.es_responsable_matricula
				JOIN estudiantes.estudiante e ON e.id = v.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				JOIN matricula.matricula m ON m.estudiante_id = e.id
				JOIN configuracion.grado g ON g.codigo = m.grado_codigo
				JOIN configuracion.seccion s ON s.id = m.seccion_id
				WHERE u.id = ?::uuid
				ORDER BY p.nombres""").param(LUIS_PAREDES).query().listOfRows();
		assertThat(hijos).hasSize(2);
		assertThat(hijos.get(0)).containsEntry("nombres", "Mateo Alonso").containsEntry("estado", "SECCION_RESERVADA");
		assertThat(hijos.get(1)).containsEntry("nombres", "Sofía").containsEntry("seccion", "A");

		// Deuda vencida a la fecha de la demo: solo la pensión de septiembre (vencimiento + 5 días de gracia).
		BigDecimal deuda = jdbc.sql("""
				SELECT coalesce(sum(c.monto_final - c.monto_pagado), 0)
				FROM pagos.cuota c
				JOIN pagos.compromiso_pago cp ON cp.id = c.compromiso_id
				JOIN pagos.tarifario t ON t.id = cp.tarifario_id
				JOIN estudiantes.vinculo_familiar v ON v.estudiante_id = cp.estudiante_id AND v.es_responsable_matricula
				JOIN seguridad.usuario u ON u.persona_id = v.persona_id
				WHERE u.id = ?::uuid AND c.estado IN ('PENDIENTE', 'PAGADA_PARCIAL')
				  AND c.fecha_vencimiento + t.dias_gracia < DATE '2026-10-19'""").param(LUIS_PAREDES)
				.query(BigDecimal.class).single();
		assertThat(deuda).isEqualByComparingTo("600");
	}

	@Test
	void elCompromisoTieneLaMatriculaDiezPensionesYUnaConstanciaPorPago() {
		assertThat(contar("SELECT count(*) FROM pagos.cuota WHERE compromiso_id = ?::uuid", COMPROMISO_SOFIA))
				.isEqualTo(11);
		assertThat(contar("""
				SELECT count(DISTINCT p.numero_constancia) FROM pagos.pago p
				JOIN pagos.pago_detalle d ON d.pago_id = p.id
				JOIN pagos.cuota c ON c.id = d.cuota_id
				WHERE c.compromiso_id = ?::uuid""", COMPROMISO_SOFIA)).isEqualTo(7);
	}

	@Test
	void unEstudianteTieneUnaSolaMatriculaPorAnio() {
		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO matricula.matricula (estudiante_id, anio_escolar_id, grado_codigo, tipo_estudiante)
				VALUES (?::uuid, ?::uuid, 'SEC-4', 'CONTINUADOR')""").param(SOFIA).param(ANIO_2026).update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void unaMatriculaMatriculadaNecesitaSuCodigo() {
		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO matricula.matricula (estudiante_id, anio_escolar_id, grado_codigo, tipo_estudiante, estado)
				VALUES (?::uuid, ?::uuid, 'SEC-1', 'NUEVO', 'MATRICULADO')""").param(MATEO).param(ANIO_2026)
				.update()).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void noSeGeneraDosVecesLaCuotaDeMatricula() {
		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO pagos.cuota (compromiso_id, concepto_codigo, nombre, monto_base, fecha_vencimiento)
				VALUES (?::uuid, 'MATRICULA', 'Matrícula 2026', 420, '2026-02-28')""").param(COMPROMISO_SOFIA)
				.update()).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void elMontoFinalEsElMontoBaseMenosElDescuento() {
		jdbc.sql("UPDATE pagos.cuota SET monto_descuento = 60 WHERE compromiso_id = ?::uuid AND numero = 10")
				.param(COMPROMISO_SOFIA).update();
		assertThat(jdbc.sql("SELECT monto_final FROM pagos.cuota WHERE compromiso_id = ?::uuid AND numero = 10")
				.param(COMPROMISO_SOFIA).query(BigDecimal.class).single()).isEqualByComparingTo("540");
	}

	@Test
	void soloHayUnDescuentoActivoPorCompromiso() {
		String aplicar = """
				INSERT INTO pagos.descuento_aplicado (compromiso_id, descuento_id, nombre, porcentaje, vigencia_desde, motivo)
				VALUES (?::uuid, ?::uuid, 'Beca institucional', 50, '2026-03-01', 'Prueba')""";
		jdbc.sql(aplicar).param(COMPROMISO_SOFIA).param(DESCUENTO_BECA).update();
		assertThatThrownBy(() -> jdbc.sql(aplicar).param(COMPROMISO_SOFIA).param(DESCUENTO_BECA).update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void unaTransaccionDeLaPasarelaNoSeRegistraDosVeces() {
		String orden = """
				INSERT INTO pagos.orden_pago (codigo, usuario_id, monto_total, referencia_pasarela)
				VALUES (?, ?::uuid, 600, 'TX999001')""";
		jdbc.sql(orden).param("OP-1").param(LUIS_PAREDES).update();
		assertThatThrownBy(() -> jdbc.sql(orden).param("OP-2").param(LUIS_PAREDES).update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void unDocumentoDeMatriculaNoPuedePesarMasDe10Mb() {
		assertThatThrownBy(() -> jdbc
				.sql("""
						INSERT INTO matricula.documento_matricula (matricula_id, documento_requerido_id, nombre_archivo, tipo_mime,
						                                           tamano_bytes, contenido)
						SELECT ?::uuid, id, 'ficha.pdf', 'application/pdf', 10485761, '\\x00'::bytea
						FROM matricula.documento_requerido LIMIT 1""")
				.param(MATRICULA_SOFIA).update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private int contar(String sql, Object... parametros) {
		var consulta = jdbc.sql(sql);
		for (Object p : parametros) {
			consulta = consulta.param(p);
		}
		return consulta.query(Integer.class).single();
	}
}
