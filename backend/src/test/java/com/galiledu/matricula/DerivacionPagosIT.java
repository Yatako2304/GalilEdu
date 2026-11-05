package com.galiledu.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.galiledu.configuracion.PruebaIntegracionConfiguracion;
import com.galiledu.configuracion.RelojPrueba;
import com.galiledu.matricula.infraestructura.ConsultasMatricula;
import com.galiledu.pagos.aplicacion.PagoEnLineaAprobado;
import com.galiledu.pagos.aplicacion.ServicioCronograma;
import com.galiledu.pagos.aplicacion.ServicioCuotaMatricula;
import com.jayway.jsonpath.JsonPath;

/**
 * HU-31 · Derivación a pagos (criterios CAHU31). Datos de prueba: Luis Paredes (U20260003) con sus hijos Mateo (sección
 * reservada en 1.° A de 2027) y Sofía (pensión de septiembre 2026 vencida). Aquí Mateo pasa a PENDIENTE_PAGO con su
 * cuota de matrícula 2027 (S/ 450) y el reloj está en el 20/10/2026, con el periodo 2027 abierto.
 */
@RecordApplicationEvents
class DerivacionPagosIT extends PruebaIntegracionConfiguracion {
	private static final String APODERADO = "U20260003";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	private static final UUID MATRICULA_MATEO = UUID.fromString("50000000-0000-0000-0000-000000000002");
	private static final UUID PERSONAL = UUID.fromString("10000000-0000-0000-0000-000000000002");

	@Autowired
	private RelojPrueba reloj;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager em;
	@Autowired
	private ServicioCuotaMatricula cuotaMatricula;
	@Autowired
	private ConsultasMatricula consultasMatricula;
	@Autowired
	private ServicioCronograma cronograma;
	@Autowired
	private ApplicationEvents eventos;

	private String token;
	private String cuotaId;

	@BeforeEach
	void mateoPendienteDePago() throws Exception {
		reloj.fijarEn(LocalDate.of(2026, 10, 20));
		jdbc.sql("UPDATE pagos.tarifario SET estado = 'CONFIRMADO' WHERE id = '32000000-0000-0000-0000-000000002027'")
				.update();
		jdbc.sql("UPDATE matricula.matricula SET estado = 'PENDIENTE_PAGO' WHERE id = ?").param(MATRICULA_MATEO)
				.update();
		em.clear();
		cuotaId = cuotaMatricula.generar(MATRICULA_MATEO, PERSONAL).cuotaId().toString();
		// Como en la aplicación, el pago llega en otra petición: se releen los datos ya guardados.
		em.flush();
		em.clear();
		token = token(APODERADO);
	}

	private String estado() {
		return "/matricula/apoderado/estudiantes/" + MATEO + "/estado";
	}

	@Test
	void muestraLaCuotaDeMatriculaParaPagarAunqueUnHermanoDeba() throws Exception {
		// CAHU31-1 y CAHU31-2: la pensión vencida es de Sofía, no de Mateo.
		mvc.perform(conToken(get(estado()), token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pasoActual").value(3))
				.andExpect(jsonPath("$.pagoMatricula.cuotaId").value(cuotaId))
				.andExpect(jsonPath("$.pagoMatricula.concepto").value("Matrícula 2027"))
				.andExpect(jsonPath("$.pagoMatricula.monto").value(450.0))
				.andExpect(jsonPath("$.pagoMatricula.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.pagoMatricula.puedePagar").value(true));
	}

	@Test
	void pagarLaMatriculaUsaElPagoEnLineaYAvisaQueMatriculaPagar() throws Exception {
		// CAHU31-3: mismo flujo de HU-41, solo con la cuota de matrícula.
		String respuesta = mvc.perform(conToken(post("/pagos/pago-en-linea/ordenes"), token,
				"{\"cuotaIds\":[\"%s\"]}".formatted(cuotaId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.total").value(450.0))
				.andReturn().getResponse().getContentAsString();
		String codigo = JsonPath.read(respuesta, "$.codigo");

		mvc.perform(conToken(get(estado()), token))
				.andExpect(jsonPath("$.pagoMatricula.puedePagar").value(false))
				.andExpect(jsonPath("$.pagoMatricula.motivo").value(containsString("en proceso")));

		mvc.perform(conToken(post("/pagos/pago-en-linea/simulador/{codigo}", codigo), token, "{\"aprobar\":true}"))
				.andExpect(jsonPath("$.estado").value("APROBADA"));
		assertThat(eventos.stream(PagoEnLineaAprobado.class))
				.singleElement()
				.satisfies(e -> assertThat(e.matriculasPagadas()).containsExactly(MATRICULA_MATEO));
	}

	@Test
	void alConfirmarseElPagoQuedaMatriculadoConCodigoYPensiones() throws Exception {
		// CAHU31-4: lo que hace EscuchaPagoMatricula después de guardar el pago.
		assertThat(consultasMatricula.matricularPorPago(MATRICULA_MATEO, OffsetDateTime.now(reloj)))
				.hasValueSatisfying(codigo -> assertThat(codigo).matches("MAT-2027-\\d{4}"));
		cronograma.generarPensiones(MATRICULA_MATEO, PERSONAL);
		em.flush();

		mvc.perform(conToken(get(estado()), token))
				.andExpect(jsonPath("$.pasoActual").value(4))
				.andExpect(jsonPath("$.matricula.estado").value("MATRICULADO"))
				.andExpect(jsonPath("$.matricula.codigo").value(containsString("MAT-2027-")))
				.andExpect(jsonPath("$.pagoMatricula.puedePagar").value(false));
		int pensiones = jdbc.sql("""
				SELECT count(*) FROM pagos.cuota q JOIN pagos.compromiso_pago c ON c.id = q.compromiso_id
				WHERE c.matricula_id = ? AND q.concepto_codigo = 'PENSION'""")
				.param(MATRICULA_MATEO).query(Integer.class).single();
		assertThat(pensiones).isEqualTo(10);

		// CAHU31-6: un segundo aviso no genera otro código.
		assertThat(consultasMatricula.matricularPorPago(MATRICULA_MATEO, OffsetDateTime.now(reloj))).isEmpty();
	}

	@Test
	void conPensionesVencidasPropiasNoPuedePagarLaMatricula() throws Exception {
		// CAHU31-2: una pensión vencida del propio Mateo sí bloquea.
		jdbc.sql("""
				INSERT INTO pagos.cuota (compromiso_id, concepto_codigo, numero, nombre, monto_base, monto_pagado,
				                         fecha_vencimiento, estado)
				SELECT compromiso_id, 'PENSION', 1, 'Pensión de septiembre', 600, 0, DATE '2026-09-30', 'PENDIENTE'
				FROM pagos.cuota WHERE id = ?::uuid""").param(cuotaId).update();
		em.clear();

		mvc.perform(conToken(get(estado()), token))
				.andExpect(jsonPath("$.pagoMatricula.puedePagar").value(false))
				.andExpect(jsonPath("$.pagoMatricula.motivo").value(containsString("pensiones vencidas de Mateo")));
		mvc.perform(conToken(post("/pagos/pago-en-linea/ordenes"), token,
				"{\"cuotaIds\":[\"%s\"]}".formatted(cuotaId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("Primero paga las pensiones vencidas")));
	}

	@Test
	void sinCuotaDeMatriculaNoHayBloqueDePago() throws Exception {
		jdbc.sql("UPDATE pagos.cuota SET estado = 'ANULADA' WHERE id = ?::uuid").param(cuotaId).update();
		em.clear();
		mvc.perform(conToken(get(estado()), token))
				.andExpect(jsonPath("$.pagoMatricula").doesNotExist());
	}
}
