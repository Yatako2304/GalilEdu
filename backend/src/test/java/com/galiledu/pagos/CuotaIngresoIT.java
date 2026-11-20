package com.galiledu.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * Corrección 4 de la iteración 1: la cuota de ingreso se genera junto con la de matrícula, una sola vez y solo para el
 * estudiante NUEVO. Datos de prueba: Mateo (NUEVO, sección reservada en 2027); tarifario 2027 con cuota de ingreso de
 * S/ 500 y matrícula de S/ 450 que vence el 28/02/2027.
 */
class CuotaIngresoIT extends PruebaIntegracion {
	private static final String MATRICULA_MATEO = "50000000-0000-0000-0000-000000000002";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	private static final String ANIO_2027 = "30000000-0000-0000-0000-000000002027";

	@Autowired
	private EntityManager entityManager;
	@Autowired
	private JdbcClient jdbc;

	private void confirmarTarifario2027(String personal, String montoCuotaIngreso) throws Exception {
		mvc.perform(conToken(post("/configuracion/tarifarios/{id}/confirmar", ANIO_2027), personal, """
				{"montoCuotaIngreso":%s,"montoMatricula":450,"montoPension":620,"diasGracia":5,\
				"recargoPorcentaje":2,"topeRecargoPorcentaje":10}""".formatted(montoCuotaIngreso)))
				.andExpect(status().isOk());
		entityManager.flush();
	}

	private void generarCuotaMatricula(String personal) throws Exception {
		mvc.perform(conToken(post("/pagos/matriculas/{id}/cuota-matricula", MATRICULA_MATEO), personal))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.concepto").value("Matrícula 2027"))
				.andExpect(jsonPath("$.monto").value(450.0));
		entityManager.flush();
	}

	/** Cuotas de ingreso de Mateo en todos sus compromisos. */
	private List<Map<String, Object>> cuotasIngresoDeMateo() {
		return jdbc.sql("""
				SELECT q.nombre, q.monto_base, q.fecha_vencimiento, q.estado
				FROM pagos.cuota q JOIN pagos.compromiso_pago c ON c.id = q.compromiso_id
				WHERE c.estudiante_id = ?::uuid AND q.concepto_codigo = 'CUOTA_INGRESO'""")
				.param(MATEO).query().listOfRows();
	}

	@DisplayName("Al estudiante nuevo se le genera la cuota de ingreso con la de matrícula")
	@Test
	void alEstudianteNuevoSeLeGeneraLaCuotaDeIngresoConLaDeMatricula() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		confirmarTarifario2027(personal, "500");
		generarCuotaMatricula(personal);

		List<Map<String, Object>> ingreso = cuotasIngresoDeMateo();
		assertThat(ingreso).hasSize(1);
		assertThat(ingreso.getFirst().get("nombre")).isEqualTo("Cuota de ingreso");
		assertThat((BigDecimal) ingreso.getFirst().get("monto_base")).isEqualByComparingTo("500");
		assertThat(ingreso.getFirst().get("fecha_vencimiento").toString()).isEqualTo(LocalDate.of(2027, 2, 28)
				.toString());
		assertThat(ingreso.getFirst().get("estado")).isEqualTo("PENDIENTE");
	}

	@DisplayName("Al continuador no se le cobra ingreso")
	@Test
	void alContinuadorNoSeLeCobraIngreso() throws Exception {
		jdbc.sql("UPDATE matricula.matricula SET tipo_estudiante = 'CONTINUADOR' WHERE id = ?::uuid")
				.param(MATRICULA_MATEO).update();
		String personal = token(PERSONAL_ADMINISTRATIVO);
		confirmarTarifario2027(personal, "500");
		generarCuotaMatricula(personal);
		assertThat(cuotasIngresoDeMateo()).isEmpty();
	}

	@DisplayName("Sin monto de ingreso en el tarifario no se genera")
	@Test
	void sinMontoDeIngresoEnElTarifarioNoSeGenera() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		confirmarTarifario2027(personal, "null");
		generarCuotaMatricula(personal);
		assertThat(cuotasIngresoDeMateo()).isEmpty();
	}

	@DisplayName("La cuota de ingreso se cobra una sola vez")
	@Test
	void laCuotaDeIngresoSeCobraUnaSolaVez() throws Exception {
		// Mateo ya tuvo un compromiso 2026 con su cuota de ingreso
		jdbc.sql("""
				INSERT INTO pagos.compromiso_pago (id, estudiante_id, anio_escolar_id, tarifario_id, estado,
				    creado_en, actualizado_en)
				SELECT '59000000-0000-0000-0000-000000000003', ?::uuid, c.anio_escolar_id, c.tarifario_id, 'VIGENTE',
				       now(), now()
				FROM pagos.compromiso_pago c WHERE c.id = '51000000-0000-0000-0000-000000000001'""")
				.param(MATEO).update();
		jdbc.sql("""
				INSERT INTO pagos.cuota (compromiso_id, concepto_codigo, nombre, monto_base, monto_pagado,
				    fecha_vencimiento, estado, fecha_pago)
				VALUES ('59000000-0000-0000-0000-000000000003', 'CUOTA_INGRESO', 'Cuota de ingreso', 500, 500,
				        DATE '2026-02-28', 'PAGADA', DATE '2026-02-10')""").update();
		String personal = token(PERSONAL_ADMINISTRATIVO);
		confirmarTarifario2027(personal, "500");
		generarCuotaMatricula(personal);
		assertThat(cuotasIngresoDeMateo()).hasSize(1);
	}
}
