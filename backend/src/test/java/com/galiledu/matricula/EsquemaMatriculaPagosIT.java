package com.galiledu.matricula;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * Tablas de matrícula y pagos (V5 y V7): las relaciones entre módulos y las reglas que guarda la propia BD. Cada prueba
 * de rechazo usa un dato válido en todo lo demás y exige la restricción exacta que protege la regla.
 */
class EsquemaMatriculaPagosIT extends PruebaIntegracion {
	private static final String SOFIA = "20000000-0000-0000-0000-000000000002";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	private static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String MATRICULA_SOFIA = "50000000-0000-0000-0000-000000000001";
	private static final String COMPROMISO_SOFIA = "51000000-0000-0000-0000-000000000001";
	private static final String DESCUENTO_BECA = "52000000-0000-0000-0000-000000000002";
	private static final String DESCUENTO_ACUERDO = "52000000-0000-0000-0000-000000000003";
	private static final String LUIS_PAREDES = "10000000-0000-0000-0000-000000000003";

	/** Sección de 2026 del grado indicado (A), para que las matrículas de prueba sean válidas en todo lo demás. */
	private static final String SECCION_2026 = """
			(SELECT id FROM configuracion.seccion
			 WHERE anio_escolar_id = '30000000-0000-0000-0000-000000002026' AND grado_codigo = ? AND nombre = 'A')""";

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
		// Matrícula completa y válida (con sección); solo falla porque Sofía ya está matriculada en 2026.
		rechazaPor("uq_matricula_estudiante_anio",
				() -> jdbc
						.sql("""
								INSERT INTO matricula.matricula (estudiante_id, anio_escolar_id, grado_codigo, seccion_id, tipo_estudiante)
								VALUES (?::uuid, ?::uuid, 'SEC-4', %s, 'CONTINUADOR')"""
								.formatted(SECCION_2026))
						.param(SOFIA).param(ANIO_2026).param("SEC-4").update());
	}

	@Test
	void unaMatriculaMatriculadaNecesitaSuCodigo() {
		// Mateo no tiene matrícula en 2026: el único problema es que está MATRICULADO sin código.
		rechazaPor("ck_matricula_codigo",
				() -> jdbc
						.sql("""
								INSERT INTO matricula.matricula (estudiante_id, anio_escolar_id, grado_codigo, seccion_id, tipo_estudiante,
								                                 estado)
								VALUES (?::uuid, ?::uuid, 'SEC-1', %s, 'NUEVO', 'MATRICULADO')"""
								.formatted(SECCION_2026))
						.param(MATEO).param(ANIO_2026).param("SEC-1").update());
	}

	@Test
	void unaMatriculaConSuCodigoSiSeRegistra() {
		// Contraprueba: el mismo dato con código se acepta (la regla anterior no rechaza todo).
		int filas = jdbc
				.sql("""
						INSERT INTO matricula.matricula (estudiante_id, anio_escolar_id, grado_codigo, seccion_id, tipo_estudiante,
						                                 estado, codigo)
						VALUES (?::uuid, ?::uuid, 'SEC-1', %s, 'NUEVO', 'MATRICULADO', 'MAT-2026-9999')"""
						.formatted(SECCION_2026))
				.param(MATEO).param(ANIO_2026).param("SEC-1").update();
		assertThat(filas).isEqualTo(1);
	}

	@Test
	void noSeGeneraDosVecesLaCuotaDeMatricula() {
		rechazaPor("ux_cuota_una_matricula", () -> jdbc.sql("""
				INSERT INTO pagos.cuota (compromiso_id, concepto_codigo, nombre, monto_base, fecha_vencimiento)
				VALUES (?::uuid, 'MATRICULA', 'Matrícula 2026', 420, '2026-02-28')""").param(COMPROMISO_SOFIA)
				.update());
	}

	@Test
	void unDescuentoDeVariosAniosRebajaLasCuotasYElMontoFinalSeCalculaSolo() {
		// V7: el descuento es del estudiante, con vigencia de julio 2026 a diciembre 2028 ("por 3 años" en Figma).
		String descuento = aplicarDescuento(DESCUENTO_ACUERDO, "2028-12-31");
		jdbc.sql("""
				UPDATE pagos.cuota SET monto_descuento = 60, descuento_aplicado_id = ?::uuid
				WHERE compromiso_id = ?::uuid AND numero = 10""").param(descuento).param(COMPROMISO_SOFIA).update();
		assertThat(jdbc.sql("SELECT monto_final FROM pagos.cuota WHERE compromiso_id = ?::uuid AND numero = 10")
				.param(COMPROMISO_SOFIA).query(BigDecimal.class).single()).isEqualByComparingTo("540");
	}

	@Test
	void unaCuotaRebajadaDiceQueDescuentoLaRebajo() {
		rechazaPor("ck_cuota_descuento", () -> jdbc.sql("""
				UPDATE pagos.cuota SET monto_descuento = 60 WHERE compromiso_id = ?::uuid AND numero = 10""")
				.param(COMPROMISO_SOFIA).update());
	}

	@Test
	void soloHayUnDescuentoActivoPorEstudiante() {
		aplicarDescuento(DESCUENTO_BECA, null);
		rechazaPor("ux_descuento_aplicado_activo", () -> aplicarDescuento(DESCUENTO_ACUERDO, null));
	}

	@Test
	void unaTransaccionDeLaPasarelaNoSeRegistraDosVeces() {
		String orden = """
				INSERT INTO pagos.orden_pago (codigo, usuario_id, monto_total, referencia_pasarela)
				VALUES (?, ?::uuid, 600, 'TX999001')""";
		jdbc.sql(orden).param("OP-1").param(LUIS_PAREDES).update();
		// Otra orden (OP-2) con la misma transacción de la pasarela.
		rechazaPor("orden_pago_referencia_pasarela_key",
				() -> jdbc.sql(orden).param("OP-2").param(LUIS_PAREDES).update());
	}

	@Test
	void unDocumentoDeMatriculaNoPuedePesarMasDe10Mb() {
		rechazaPor("ck_documento_matricula_tamano", () -> jdbc.sql("""
				INSERT INTO matricula.documento_matricula (matricula_id, documento_requerido_id, nombre_archivo,
				                                           tipo_mime, tamano_bytes, contenido)
				SELECT ?::uuid, d.id, 'ficha.pdf', 'application/pdf', 10485761, '\\x00'::bytea
				FROM matricula.documento_requerido d
				JOIN matricula.periodo_matricula p ON p.id = d.periodo_matricula_id
				WHERE p.anio_escolar_id = ?::uuid AND d.orden = 1""").param(MATRICULA_SOFIA).param(ANIO_2026)
				.update());
	}

	private String aplicarDescuento(String descuentoId, String vigenciaHasta) {
		return jdbc.sql("""
				INSERT INTO pagos.descuento_aplicado (estudiante_id, descuento_id, nombre, porcentaje, vigencia_desde,
				                                      vigencia_hasta, motivo)
				SELECT ?::uuid, d.id, d.nombre, 10, '2026-07-01', ?::date, 'Prueba'
				FROM pagos.descuento d WHERE d.id = ?::uuid
				RETURNING id::text""").param(SOFIA).param(vigenciaHasta).param(descuentoId).query(String.class)
				.single();
	}

	private int contar(String sql, Object... parametros) {
		var consulta = jdbc.sql(sql);
		for (Object p : parametros) {
			consulta = consulta.param(p);
		}
		return consulta.query(Integer.class).single();
	}
}
