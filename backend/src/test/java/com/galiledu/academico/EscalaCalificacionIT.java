package com.galiledu.academico;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.galiledu.PruebaIntegracion;

/**
 * Escala de calificación (Figma CI-23, HU-55). Los datos de prueba traen una versión desde 01/03/2025 y otra desde
 * 01/03/2027; las pruebas usan fechas relativas a hoy para no depender del día en que corren.
 */
class EscalaCalificacionIT extends PruebaIntegracion {
	private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Lima"));

	private String personal;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	private static String version(LocalDate desde, int cHasta, int bHasta, int aHasta) {
		return """
				{"vigenteDesde":"%s","niveles":[
				  {"nivel":"AD","notaDesde":%d,"notaHasta":20},
				  {"nivel":"A","notaDesde":%d,"notaHasta":%d},
				  {"nivel":"B","notaDesde":%d,"notaHasta":%d},
				  {"nivel":"C","notaDesde":0,"notaHasta":%d}]}"""
				.formatted(desde, aHasta + 1, bHasta + 1, aHasta, cHasta + 1, bHasta, cHasta);
	}

	@DisplayName("La vigente trae sus niveles de mayor a menor")
	@Test
	void laVigenteTraeSusNivelesDeMayorAMenor() throws Exception {
		mvc.perform(conToken(get("/tablas-conversion/vigente"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("VIGENTE"))
				.andExpect(jsonPath("$.niveles.length()").value(4))
				.andExpect(jsonPath("$.niveles[0].nivel").value("AD"))
				.andExpect(jsonPath("$.niveles[0].notaHasta").value(20))
				.andExpect(jsonPath("$.niveles[3].nivel").value("C"))
				.andExpect(jsonPath("$.niveles[3].notaDesde").value(0))
				.andExpect(jsonPath("$.niveles[3].significado").value("En inicio"));
	}

	@DisplayName("Una versión futura queda programada y aparece primero en la lista")
	@Test
	void unaVersionFuturaQuedaProgramadaYAparecePrimeroEnLaLista() throws Exception {
		LocalDate futura = HOY.plusYears(5);
		mvc.perform(conToken(post("/tablas-conversion"), personal, version(futura, 10, 13, 16)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("PROGRAMADA"))
				.andExpect(jsonPath("$.niveles[1].nivel").value("A"))
				.andExpect(jsonPath("$.niveles[1].notaDesde").value(14))
				.andExpect(jsonPath("$.niveles[1].significado").value("Logro esperado"));

		mvc.perform(conToken(get("/tablas-conversion"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].vigenteDesde").value(futura.toString()))
				.andExpect(jsonPath("$[0].estado").value("PROGRAMADA"))
				.andExpect(jsonPath("$[?(@.estado == 'VIGENTE')]").isNotEmpty());
	}

	@DisplayName("Una versión desde hoy pasa a ser la vigente")
	@Test
	void unaVersionDesdeHoyPasaASerLaVigente() throws Exception {
		mvc.perform(conToken(post("/tablas-conversion"), personal, version(HOY, 10, 13, 17)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("VIGENTE"));
		mvc.perform(conToken(get("/tablas-conversion/vigente"), personal))
				.andExpect(jsonPath("$.vigenteDesde").value(HOY.toString()));
	}

	@DisplayName("Rechaza fechas pasadas y repetidas")
	@Test
	void rechazaFechasPasadasYRepetidas() throws Exception {
		mvc.perform(conToken(post("/tablas-conversion"), personal, version(HOY.minusDays(1), 10, 13, 17)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La fecha de vigencia no puede ser anterior a hoy."));

		LocalDate futura = HOY.plusYears(6);
		mvc.perform(conToken(post("/tablas-conversion"), personal, version(futura, 10, 13, 17)))
				.andExpect(status().isCreated());
		mvc.perform(conToken(post("/tablas-conversion"), personal, version(futura, 9, 13, 17)))
				.andExpect(status().isConflict());
	}

	@DisplayName("Rechaza rangos con vacios o cruces")
	@Test
	void rechazaRangosConVaciosOCruces() throws Exception {
		mvc.perform(conToken(post("/tablas-conversion"), personal, """
				{"vigenteDesde":"%s","niveles":[
				  {"nivel":"AD","notaDesde":18,"notaHasta":20},
				  {"nivel":"A","notaDesde":15,"notaHasta":17},
				  {"nivel":"B","notaDesde":11,"notaHasta":13},
				  {"nivel":"C","notaDesde":0,"notaHasta":10}]}""".formatted(HOY.plusYears(7))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Falta cubrir de 14 a 14 antes de A."));
	}

	@DisplayName("Válida notas y fecha")
	@Test
	void validaNotasYFecha() throws Exception {
		mvc.perform(conToken(post("/tablas-conversion"), personal, """
				{"vigenteDesde":"%s","niveles":[{"nivel":"AD","notaDesde":18,"notaHasta":21}]}"""
				.formatted(HOY.plusYears(8))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Las notas van de 0 a 20."));
		mvc.perform(conToken(post("/tablas-conversion"), personal, """
				{"niveles":[]}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Elige la fecha desde la que rige la versión."));
	}

	@DisplayName("Otros roles reciben403")
	@Test
	void elCoordinadorTambienConsultaYAgregaVersiones() throws Exception {
		String coordinador = token(COORDINADOR);
		mvc.perform(conToken(get("/tablas-conversion/vigente"), coordinador))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("VIGENTE"));
		mvc.perform(conToken(post("/tablas-conversion"), coordinador, version(HOY.plusYears(10), 10, 13, 16)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("PROGRAMADA"));
	}

	@DisplayName("Otros roles reciben403")
	@Test
	void otrosRolesReciben403() throws Exception {
		mvc.perform(conToken(get("/tablas-conversion"), token(DOCENTE))).andExpect(status().isForbidden());
		mvc.perform(conToken(post("/tablas-conversion"), token(ADMIN), version(HOY.plusYears(9), 10, 13, 17)))
				.andExpect(status().isForbidden());
	}
}
