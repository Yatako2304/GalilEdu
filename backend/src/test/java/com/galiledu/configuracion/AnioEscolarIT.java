package com.galiledu.configuracion;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/** HU-74: listar, ver y crear años escolares con sus periodos, y permisos. */
class AnioEscolarIT extends PruebaIntegracion {
	private static final String ID_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String RUTA = "/configuracion/anios-escolares";

	@Autowired
	private EntityManager entityManager;

	private String personal;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	private static String solicitud(int anio, String inicio, String fin, String regimen) {
		return """
				{"anio":%d,"fechaInicio":"%s","fechaFin":"%s","regimen":"%s"}""".formatted(anio, inicio, fin, regimen);
	}

	@Test
	void creaUnAnioBimestralPlanificadoConCuatroPeriodos() throws Exception {
		mvc.perform(conToken(post(RUTA), personal, solicitud(2028, "2028-03-01", "2028-12-15", "BIMESTRAL")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.anio").value(2028))
				.andExpect(jsonPath("$.estado").value("PLANIFICADO"))
				.andExpect(jsonPath("$.regimen").value("BIMESTRAL"))
				.andExpect(jsonPath("$.periodos.length()").value(4))
				.andExpect(jsonPath("$.periodos[0].numero").value(1))
				.andExpect(jsonPath("$.periodos[0].nombre").value("I Bimestre"))
				.andExpect(jsonPath("$.periodos[0].fechaInicio").value("2028-03-01"))
				.andExpect(jsonPath("$.periodos[0].estado").value("PLANIFICADO"))
				.andExpect(jsonPath("$.periodos[3].nombre").value("IV Bimestre"))
				.andExpect(jsonPath("$.periodos[3].fechaFin").value("2028-12-15"))
				.andExpect(jsonPath("$.periodos[3].fechaLimiteNotas").value("2028-12-20"));
	}

	@Test
	void creaUnAnioTrimestralConTresPeriodos() throws Exception {
		mvc.perform(conToken(post(RUTA), personal, solicitud(2029, "2029-03-01", "2029-12-14", "TRIMESTRAL")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.periodos.length()").value(3))
				.andExpect(jsonPath("$.periodos[2].nombre").value("III Trimestre"))
				.andExpect(jsonPath("$.periodos[2].fechaFin").value("2029-12-14"));
	}

	@Test
	void elAnioCreadoApareceEnElListadoYEnElDetalle() throws Exception {
		String creado = mvc.perform(conToken(post(RUTA), personal,
				solicitud(2028, "2028-03-01", "2028-12-15", "BIMESTRAL")))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(creado, "$.id");
		// La prueba corre dentro de una transacción que no se confirma, así que el INSERT de JPA sigue pendiente en
		// memoria. El listado lee con JdbcClient (otra consulta SQL) y no lo vería; en producción crear() hace commit.
		entityManager.flush();

		mvc.perform(conToken(get(RUTA), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].anio").value(2028))
				.andExpect(jsonPath("$[0].cantidadPeriodos").value(4));
		mvc.perform(conToken(get(RUTA + "/" + id), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodos.length()").value(4));
	}

	@Test
	void listaLosAniosDeLosDatosDePruebaDelMasRecienteAlMasAntiguo() throws Exception {
		mvc.perform(conToken(get(RUTA), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].anio").value(2027))
				.andExpect(jsonPath("$[0].regimen").value("TRIMESTRAL"))
				.andExpect(jsonPath("$[0].cantidadPeriodos").value(3))
				.andExpect(jsonPath("$[1].anio").value(2026))
				.andExpect(jsonPath("$[1].estado").value("ACTIVO"))
				.andExpect(jsonPath("$[1].cantidadPeriodos").value(4));
	}

	@Test
	void elDetalleTraeLosPeriodosOrdenadosPorNumero() throws Exception {
		mvc.perform(conToken(get(RUTA + "/" + ID_2026), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(2026))
				.andExpect(jsonPath("$.periodos.length()").value(4))
				.andExpect(jsonPath("$.periodos[0].nombre").value("I Bimestre"))
				.andExpect(jsonPath("$.periodos[2].estado").value("EN_CURSO"));
	}

	@Test
	void detalleDeUnAnioQueNoExisteDa404() throws Exception {
		mvc.perform(conToken(get(RUTA + "/00000000-0000-0000-0000-000000000000"), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	@Test
	void rechazaUnAnioDuplicado() throws Exception {
		mvc.perform(conToken(post(RUTA), personal, solicitud(2027, "2027-03-01", "2027-12-17", "BIMESTRAL")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ya existe un año escolar 2027."));
	}

	@Test
	void rechazaFechaDeFinNoPosteriorALaDeInicio() throws Exception {
		mvc.perform(conToken(post(RUTA), personal, solicitud(2028, "2028-12-15", "2028-03-01", "BIMESTRAL")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La fecha de fin debe ser posterior a la fecha de inicio."));
		mvc.perform(conToken(post(RUTA), personal, solicitud(2028, "2028-03-01", "2028-03-01", "BIMESTRAL")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rechazaUnAnioMuyCortoParaElRegimen() throws Exception {
		mvc.perform(conToken(post(RUTA), personal, solicitud(2028, "2028-03-01", "2028-03-05", "BIMESTRAL")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("4 periodos")));
	}

	@Test
	void validaElRangoDelAnioYLosCamposObligatorios() throws Exception {
		mvc.perform(conToken(post(RUTA), personal, solicitud(1999, "2028-03-01", "2028-12-15", "BIMESTRAL")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El año escolar debe estar entre 2000 y 2100."));
		mvc.perform(conToken(post(RUTA), personal, solicitud(2101, "2028-03-01", "2028-12-15", "BIMESTRAL")))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(post(RUTA), personal, "{\"anio\":2028}"))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(post(RUTA), personal, solicitud(2028, "2028-03-01", "2028-12-15", "SEMESTRAL")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void soloElPersonalAdministrativoPuedeUsarLaConfiguracion() throws Exception {
		String cuerpo = solicitud(2028, "2028-03-01", "2028-12-15", "BIMESTRAL");
		for (String usuario : new String[]{DOCENTE, ADMIN}) {
			String token = token(usuario);
			mvc.perform(conToken(get(RUTA), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(get(RUTA + "/" + ID_2026), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(post(RUTA), token, cuerpo)).andExpect(status().isForbidden());
		}
		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
	}
}
