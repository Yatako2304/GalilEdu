package com.galiledu.horarios;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;

/**
 * HU-45: los selectores de año, grado y sección de la pantalla del Coordinador. Configuración los ofrece solo al
 * Personal administrativo, así que Horarios tiene los suyos. Datos de prueba: 2026 (ACTIVO) con las secciones A y B de
 * 1.° a 5.° de secundaria, y 2027 (PLANIFICADO).
 */
class SelectoresHorariosIT extends PruebaIntegracion {
	private static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String ANIO_2027 = "30000000-0000-0000-0000-000000002027";

	@Autowired
	private JdbcClient jdbc;

	private ResultActions pedir(String ruta, String usuario) throws Exception {
		return mvc.perform(conToken(get(ruta), token(usuario), null));
	}

	@Test
	void elCoordinadorVeLosAniosConSuEstado() throws Exception {
		pedir("/horarios/anios-escolares", COORDINADOR).andExpect(status().isOk())
				// Del más reciente al más antiguo.
				.andExpect(jsonPath("$[0].anio").value(2027)).andExpect(jsonPath("$[0].estado").value("PLANIFICADO"))
				.andExpect(jsonPath("$[0].id").value(ANIO_2027))
				.andExpect(jsonPath("$[?(@.anio==2026)].id", contains(ANIO_2026)))
				.andExpect(jsonPath("$[?(@.anio==2026)].estado", contains("ACTIVO")));
	}

	@Test
	void elCoordinadorVeLosGradosConSusSeccionesDelAnio() throws Exception {
		pedir("/horarios/anios-escolares/" + ANIO_2026 + "/secciones", COORDINADOR).andExpect(status().isOk())
				// Los 11 grados del catálogo, en su orden: 6 de primaria y 5 de secundaria.
				.andExpect(jsonPath("$", hasSize(11))).andExpect(jsonPath("$[0].codigo").value("PRI-1"))
				.andExpect(jsonPath("$[0].nombre").value("1.° de primaria"))
				.andExpect(jsonPath("$[0].nivel").value("PRIMARIA"))
				.andExpect(jsonPath("$[0].numero").value(1))
				// Primaria no tiene secciones en los datos de prueba, pero el grado sale igual.
				.andExpect(jsonPath("$[0].cantidadSeccionesActivas").value(0))
				.andExpect(jsonPath("$[0].secciones", empty()))
				.andExpect(jsonPath("$[10].codigo").value("SEC-5"))
				// Cada grado de secundaria tiene las secciones A y B.
				.andExpect(jsonPath("$[?(@.codigo=='SEC-1')].secciones[*].nombre", contains("A", "B")))
				.andExpect(jsonPath("$[?(@.codigo=='SEC-3')].secciones[*].nombre", contains("A", "B")))
				.andExpect(jsonPath("$[?(@.codigo=='SEC-5')].secciones[*].nombre", contains("A", "B")))
				.andExpect(jsonPath("$[?(@.codigo=='SEC-5')].cantidadSeccionesActivas", contains(2)))
				.andExpect(jsonPath("$[?(@.codigo=='SEC-5')].secciones[*].estado", contains("ACTIVA", "ACTIVA")))
				.andExpect(jsonPath("$[?(@.codigo=='SEC-5')].secciones[0].id").isNotEmpty());
	}

	@Test
	void unAnioSinSeccionesDevuelveLosGradosVacios() throws Exception {
		UUID anioNuevo = jdbc.sql("""
				INSERT INTO configuracion.anio_escolar (anio, fecha_inicio, fecha_fin, regimen)
				VALUES (2030, DATE '2030-03-01', DATE '2030-12-20', 'BIMESTRAL') RETURNING id""")
				.query(UUID.class).single();

		pedir("/horarios/anios-escolares/" + anioNuevo + "/secciones", COORDINADOR).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(11))).andExpect(jsonPath("$[*].secciones[*]", empty()));
	}

	@Test
	void unAnioQueNoExisteResponde404() throws Exception {
		pedir("/horarios/anios-escolares/" + UUID.randomUUID() + "/secciones", COORDINADOR)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	@Test
	void otrosRolesRecibe403EnLasDosRutas() throws Exception {
		for (String usuario : new String[]{DOCENTE, PERSONAL_ADMINISTRATIVO}) {
			pedir("/horarios/anios-escolares", usuario).andExpect(status().isForbidden());
			pedir("/horarios/anios-escolares/" + ANIO_2026 + "/secciones", usuario).andExpect(status().isForbidden());
		}
	}
}
