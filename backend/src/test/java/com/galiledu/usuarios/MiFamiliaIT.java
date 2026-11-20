package com.galiledu.usuarios;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.galiledu.PruebaIntegracion;

/**
 * «Datos de la Familia»: solo el Apoderado ve, sin poder editarla, la familia de sus estudiantes a cargo (Figma ›
 * Apoderado). En los datos de prueba, Luis Paredes (U20260003) es el padre de Mateo y Sofía
 * Paredes Rojas, sin madre ni apoderado registrados y con los mismos dos contactos de emergencia.
 */
class MiFamiliaIT extends PruebaIntegracion {
	private static final String APODERADO = "U20260003";

	@DisplayName("Los hermanos con el mismo padre madre y apoderado son una sola familia")
	@Test
	void losHermanosConElMismoPadreMadreYApoderadoSonUnaSolaFamilia() throws Exception {
		mvc.perform(conToken(get("/auth/mi-familia"), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].nombre").value("Paredes Rojas"))
				.andExpect(jsonPath("$[0].estudiantes[*].nombreCompleto",
						contains("Mateo Alonso Paredes Rojas", "Sofía Paredes Rojas")));
	}

	@DisplayName("Muestra padre madre y apoderado y marca a quien consulta")
	@Test
	void muestraPadreMadreYApoderadoYMarcaAQuienConsulta() throws Exception {
		mvc.perform(conToken(get("/auth/mi-familia"), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].padre.persona.nombres").value("Luis"))
				.andExpect(jsonPath("$[0].padre.persona.numeroDocumento").value("70000003"))
				.andExpect(jsonPath("$[0].padre.esUsted").value(true))
				.andExpect(jsonPath("$[0].madre").value(nullValue()))
				.andExpect(jsonPath("$[0].apoderado").value(nullValue()));
	}

	@DisplayName("Los contactos de emergencia de los hermanos no se repiten")
	@Test
	void losContactosDeEmergenciaDeLosHermanosNoSeRepiten() throws Exception {
		mvc.perform(conToken(get("/auth/mi-familia"), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].contactosEmergencia[*].nombreCompleto",
						contains("Rosa Medina Torres", "Pedro Rojas Medina")))
				.andExpect(jsonPath("$[0].contactosEmergencia[0].parentesco").value("Abuela"))
				.andExpect(jsonPath("$[0].contactosEmergencia[0].celular").value("987654321"));
	}

	@DisplayName("Solo el apoderado puede verla")
	@Test
	void soloElApoderadoPuedeVerla() throws Exception {
		mvc.perform(conToken(get("/auth/mi-familia"), token(DOCENTE))).andExpect(status().isForbidden());
		mvc.perform(get("/auth/mi-familia")).andExpect(status().isUnauthorized());
	}
}
