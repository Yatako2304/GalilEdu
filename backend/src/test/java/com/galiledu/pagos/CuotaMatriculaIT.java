package com.galiledu.pagos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.galiledu.PruebaIntegracion;

/**
 * HU-36: cuota de matrícula. Datos de prueba: Mateo (sección reservada en 2027, tarifario 2027 EN_CONFIGURACION con
 * matrícula S/ 450 que vence el 28/02/2027), Sofía (cuota de matrícula 2026 pagada) y Valeria (ya matriculada en 2026,
 * sin compromiso).
 */
class CuotaMatriculaIT extends PruebaIntegracion {
	private static final String MATRICULA_MATEO = "50000000-0000-0000-0000-000000000002";
	private static final String MATRICULA_SOFIA = "50000000-0000-0000-0000-000000000001";
	private static final String MATRICULA_VALERIA = "50000000-0000-0000-0000-000000000003";
	private static final String APODERADO = "U20260003";
	private static final String ANIO_2027 = "30000000-0000-0000-0000-000000002027";

	@Autowired
	private EntityManager entityManager;

	/** HU-34: confirma el tarifario 2027 con los mismos montos de los datos de prueba. */
	private void confirmarTarifario2027(String personal) throws Exception {
		mvc.perform(conToken(post("/configuracion/tarifarios/{id}/confirmar", ANIO_2027), personal, """
				{"montoCuotaIngreso":500,"montoMatricula":450,"montoPension":620,"diasGracia":5,\
				"recargoPorcentaje":2,"topeRecargoPorcentaje":10}"""))
				.andExpect(status().isOk());
		entityManager.flush();
	}

	@Test
	void generaLaCuotaDeMatriculaConElTarifarioYNoLaDuplica() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(get("/pagos/matriculas/{id}/cuota-matricula", MATRICULA_MATEO), personal))
				.andExpect(status().isNotFound());
		confirmarTarifario2027(personal);

		mvc.perform(conToken(post("/pagos/matriculas/{id}/cuota-matricula", MATRICULA_MATEO), personal))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.anio").value(2027))
				.andExpect(jsonPath("$.concepto").value("Matrícula 2027"))
				.andExpect(jsonPath("$.monto").value(450.0))
				.andExpect(jsonPath("$.fechaVencimiento").value("2027-02-28"))
				.andExpect(jsonPath("$.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.responsable").value("Luis Paredes Silva"));

		// RF-79: un reintento no crea otra cuota
		mvc.perform(conToken(post("/pagos/matriculas/{id}/cuota-matricula", MATRICULA_MATEO), personal))
				.andExpect(status().isConflict());

		// RF-80: ahora se puede consultar
		mvc.perform(conToken(get("/pagos/matriculas/{id}/cuota-matricula", MATRICULA_MATEO), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"));
	}

	@Test
	void noGeneraLaCuotaSiElTarifarioNoEstaConfirmado() throws Exception {
		String personal = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(post("/pagos/matriculas/{id}/cuota-matricula", MATRICULA_MATEO), personal))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El tarifario 2027 aún no está confirmado."));
		mvc.perform(conToken(get("/pagos/matriculas/{id}/cuota-matricula", MATRICULA_MATEO), personal))
				.andExpect(status().isNotFound());
	}

	@Test
	void consultaUnaCuotaDeMatriculaPagada() throws Exception {
		mvc.perform(conToken(get("/pagos/matriculas/{id}/cuota-matricula", MATRICULA_SOFIA),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.monto").value(420.0))
				.andExpect(jsonPath("$.estado").value("PAGADA"))
				.andExpect(jsonPath("$.fechaPago").value("2026-02-12"));
	}

	@Test
	void noGeneraCuotaSiLaMatriculaYaEstaFormalizada() throws Exception {
		mvc.perform(conToken(post("/pagos/matriculas/{id}/cuota-matricula", MATRICULA_VALERIA),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isConflict());
	}

	@Test
	void otroRolNoPuedeGenerarLaCuota() throws Exception {
		mvc.perform(conToken(post("/pagos/matriculas/{id}/cuota-matricula", MATRICULA_MATEO), token(APODERADO)))
				.andExpect(status().isForbidden());
	}
}
