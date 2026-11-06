package com.galiledu.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.pagos.aplicacion.PagoEnLineaAprobado;

/** HU-31: al pagarse la cuota de matrícula el estudiante queda MATRICULADO y el apoderado ve su sección. */
@org.springframework.test.context.event.RecordApplicationEvents
class MatriculaPorPagoIT extends PruebaIntegracion {
	private static final String APODERADO = "U20260003";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	private static final String ANIO_2027 = "30000000-0000-0000-0000-000000002027";

	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private ApplicationEventPublisher eventos;
	@Autowired
	private org.springframework.test.context.event.ApplicationEvents eventosRegistrados;

	/** Mateo queda PENDIENTE_PAGO con su cuota de matrícula; devuelve el id de la cuota. */
	private UUID pendienteDePago(String estadoCuota) {
		jdbc.sql("UPDATE matricula.matricula SET estado = 'PENDIENTE_PAGO' WHERE estudiante_id = ?::uuid")
				.param(MATEO).update();
		UUID compromiso = jdbc.sql("""
				INSERT INTO pagos.compromiso_pago (estudiante_id, anio_escolar_id, matricula_id, tarifario_id)
				SELECT ?::uuid, ?::uuid, m.id, (SELECT id FROM pagos.tarifario WHERE anio_escolar_id = ?::uuid)
				FROM matricula.matricula m WHERE m.estudiante_id = ?::uuid AND m.anio_escolar_id = ?::uuid
				RETURNING id""").params(MATEO, ANIO_2027, ANIO_2027, MATEO, ANIO_2027).query(UUID.class).single();
		return jdbc.sql("""
				INSERT INTO pagos.cuota (compromiso_id, concepto_codigo, nombre, monto_base, monto_pagado,
				                         fecha_vencimiento, estado, fecha_pago)
				VALUES (?, 'MATRICULA', 'Matrícula 2027', 450, CASE WHEN ? = 'PAGADA' THEN 450 ELSE 0 END,
				        '2026-12-31', ?, CASE WHEN ? = 'PAGADA' THEN current_date END) RETURNING id""")
				.params(compromiso, estadoCuota, estadoCuota, estadoCuota).query(UUID.class).single();
	}

	private void pagoAprobado(UUID cuota) {
		eventos.publishEvent(
				new PagoEnLineaAprobado(UUID.randomUUID(), "OP-PRUEBA", UUID.randomUUID(), List.of(cuota)));
	}

	private String estadoYCodigo() {
		return jdbc.sql(
				"SELECT estado || '|' || coalesce(codigo, '') FROM matricula.matricula WHERE estudiante_id = ?::uuid")
				.param(MATEO).query(String.class).single();
	}

	@Test
	void alPagarLaCuotaDeMatriculaElEstudianteQuedaMatriculadoYElApoderadoVeLaSeccion() throws Exception {
		UUID cuota = pendienteDePago("PAGADA");
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), token(APODERADO)))
				.andExpect(jsonPath("$.matricula.seccion").doesNotExist());

		pagoAprobado(cuota);

		assertThat(estadoYCodigo()).startsWith("MATRICULADO|MAT-2027-");
		mvc.perform(conToken(get("/matricula/apoderado/estudiantes/{id}/estado", MATEO), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matricula.estado").value("MATRICULADO"))
				.andExpect(jsonPath("$.matricula.seccion").value("1.° A"))
				.andExpect(jsonPath("$.pasoActual").value(4));
	}

	@Test
	void alMatricularsePublicaElEventoQueGeneraLasPensiones() {
		UUID cuota = pendienteDePago("PAGADA");
		pagoAprobado(cuota);
		UUID matricula = jdbc.sql("SELECT id FROM matricula.matricula WHERE estudiante_id = ?::uuid").param(MATEO)
				.query(UUID.class).single();
		assertThat(eventosRegistrados.stream(com.galiledu.matricula.aplicacion.MatriculaConfirmada.class))
				.extracting(com.galiledu.matricula.aplicacion.MatriculaConfirmada::matriculaId)
				.containsExactly(matricula);
	}

	/** El recorrido real: orden de pago del apoderado y aprobación en la pasarela simulada. */
	@Test
	void pagandoLaCuotaPorLaPasarelaElEstudianteQuedaMatriculado() throws Exception {
		UUID cuota = pendienteDePago("PENDIENTE");
		String orden = com.jayway.jsonpath.JsonPath.read(mvc.perform(conToken(
				org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.post("/pagos/pago-en-linea/ordenes"),
				token(APODERADO), "{\"cuotaIds\":[\"" + cuota + "\"]}")).andExpect(status().isCreated()).andReturn()
				.getResponse().getContentAsString(), "$.codigo");

		mvc.perform(conToken(
				org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.post("/pagos/pago-en-linea/simulador/{codigo}", orden),
				token(APODERADO), "{\"aprobar\":true}")).andExpect(status().isOk());

		assertThat(estadoYCodigo()).startsWith("MATRICULADO|MAT-2027-");

		// La orden dice cuál cuota es la de matrícula y de qué estudiante, para el botón "Ver matrícula".
		mvc.perform(conToken(
				org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.get("/pagos/pago-en-linea/ordenes/{codigo}", orden),
				token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.jsonPath("$.cuotas[0].matricula").value(true))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.jsonPath("$.cuotas[0].estudianteId").value(MATEO.toString()));
	}

	@Test
	void unPagoQueNoLlegoAPagarLaCuotaNoMatricula() {
		UUID cuota = pendienteDePago("PENDIENTE");
		pagoAprobado(cuota);
		assertThat(estadoYCodigo()).isEqualTo("PENDIENTE_PAGO|");
	}

	@Test
	void unPagoRepetidoNoCambiaElCodigoYaAsignado() {
		UUID cuota = pendienteDePago("PAGADA");
		pagoAprobado(cuota);
		String primero = estadoYCodigo();
		pagoAprobado(cuota);
		assertThat(estadoYCodigo()).isEqualTo(primero);
	}
}
