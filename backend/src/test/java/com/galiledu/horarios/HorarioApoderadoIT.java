package com.galiledu.horarios;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * HU-51: el apoderado consulta el horario de clases de la sección de su hijo. Datos de prueba: Luis Paredes
 * (U20260003) es padre de Sofía (matriculada en 4.° de secundaria A de 2026, sin horario guardado) y de Mateo (con la
 * sección reservada en 2027). Gabriela Ferrara es madre de otro estudiante (no es hijo de Luis).
 */
class HorarioApoderadoIT extends PruebaHorarios {
	private static final String APODERADO = "U20260003";
	private static final UUID MATEO = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID SOFIA = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final UUID HIJO_DE_OTRA_FAMILIA = UUID.fromString("20000000-0000-0000-0000-000000000003");
	private static final UUID MATRICULA_MATEO_2027 = UUID.fromString("50000000-0000-0000-0000-000000000002");

	/** Mateo pasa a estar MATRICULADO en la sección indicada de 2027 (dentro de la transacción de la prueba). */
	private void matricularAMateoEn(UUID seccionId) {
		jdbc.sql("""
				UPDATE matricula.matricula SET seccion_id = :s, estado = 'MATRICULADO', codigo = 'MAT-2027-9001',
				       confirmado_en = now()
				WHERE id = :m""").param("s", seccionId).param("m", MATRICULA_MATEO_2027).update();
	}

	@Test
	void listaSoloLosHijosMatriculados() throws Exception {
		// Mateo solo tiene la sección reservada: no aparece.
		pedir("/horarios/apoderado/estudiantes", APODERADO).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].estudianteId").value(SOFIA.toString()))
				.andExpect(jsonPath("$[0].matriculas", hasSize(1)))
				.andExpect(jsonPath("$[0].matriculas[0].anio").value(2026))
				.andExpect(jsonPath("$[0].matriculas[0].grado").value(containsString("4.°")))
				.andExpect(jsonPath("$[0].matriculas[0].nivel").value("SECUNDARIA"))
				.andExpect(jsonPath("$[0].matriculas[0].seccion").value("A"));
	}

	@Test
	void muestraElHorarioDeLaSeccionDelHijoConSuTutor() throws Exception {
		UUID eh = estructura("EH-AP-27", "SECUNDARIA", true, 6);
		UUID aula = ambiente("TMP-AP1", "ACTIVO");
		UUID docente = docente();
		UUID seccion = seccion("SEC-1", "T1", aula, docente);
		UUID algebra = plan("Álgebra", 5);
		asignarDocente(algebra, seccion, docente);
		UUID cab = cabecera(seccion, eh);
		bloque(cab, "LUN", 1, algebra, aula);
		bloque(cab, "MIE", 4, algebra, aula);
		matricularAMateoEn(seccion);

		pedir("/horarios/apoderado/estudiantes/" + MATEO + "/horario?anio=2027", APODERADO)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estudianteId").value(MATEO.toString()))
				.andExpect(jsonPath("$.anio").value(2027))
				.andExpect(jsonPath("$.grado").value("1.° de secundaria"))
				.andExpect(jsonPath("$.seccion").value("T1"))
				.andExpect(jsonPath("$.tutor.nombre").value(containsString("María")))
				.andExpect(jsonPath("$.tutor.celular").value("987000004"))
				.andExpect(jsonPath("$.tutor.correo").value("maria.fernandez@galiledu.edu.pe"))
				.andExpect(jsonPath("$.disponible").value(true))
				.andExpect(jsonPath("$.jornada.bloques", hasSize(6)))
				.andExpect(jsonPath("$.jornada.bloques[0].inicio").value("08:00"))
				.andExpect(jsonPath("$.jornada.recesos", hasSize(1)))
				.andExpect(jsonPath("$.clases", hasSize(2)))
				.andExpect(jsonPath("$.clases[0].dia").value("LUN"))
				.andExpect(jsonPath("$.clases[0].numeroBloque").value(1))
				.andExpect(jsonPath("$.clases[0].curso").value("Álgebra"))
				.andExpect(jsonPath("$.clases[0].docente").value(containsString("María")))
				.andExpect(jsonPath("$.clases[0].aula").value("Aula TMP-AP1"));

		// Ahora Mateo también aparece en el selector.
		pedir("/horarios/apoderado/estudiantes", APODERADO).andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void sinAnioUsaElMasRecienteYSinHorarioGuardadoNoEstaDisponible() throws Exception {
		pedir("/horarios/apoderado/estudiantes/" + SOFIA + "/horario", APODERADO).andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(2026))
				.andExpect(jsonPath("$.seccion").value("A"))
				.andExpect(jsonPath("$.disponible").value(false))
				.andExpect(jsonPath("$.jornada").value(nullValue()))
				.andExpect(jsonPath("$.clases", empty()));
	}

	@Test
	void noMuestraElHorarioDeQuienNoEsSuHijo() throws Exception {
		pedir("/horarios/apoderado/estudiantes/" + HIJO_DE_OTRA_FAMILIA + "/horario", APODERADO)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("No encontramos el horario de este estudiante."));
	}

	@Test
	void noMuestraUnAnioEnQueNoEstaMatriculado() throws Exception {
		pedir("/horarios/apoderado/estudiantes/" + SOFIA + "/horario?anio=2027", APODERADO)
				.andExpect(status().isNotFound());
		// Con la sección solo reservada tampoco.
		pedir("/horarios/apoderado/estudiantes/" + MATEO + "/horario?anio=2027", APODERADO)
				.andExpect(status().isNotFound());
	}

	@Test
	void soloElApoderadoPuedeConsultarlo() throws Exception {
		pedir("/horarios/apoderado/estudiantes", DOCENTE).andExpect(status().isForbidden());
		pedir("/horarios/apoderado/estudiantes/" + SOFIA + "/horario", COORDINADOR).andExpect(status().isForbidden());
	}
}
