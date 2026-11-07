package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * HU-45, Fase 1: los dos GET de lectura del horario. Cada prueba arma sus datos por SQL en 2027 (año PLANIFICADO, sin
 * estructuras ni secciones): una estructura de 6 bloques de 45 min desde las 08:00 con un receso de 20 min después del
 * bloque 3, y secciones propias. Todo se deshace al final.
 */
class HorarioLecturaIT extends PruebaHorarios {
	private static final String SIN_PRINCIPAL_SECUNDARIA = "Configura la estructura horaria principal de secundaria antes de asignar horarios.";

	// ---------- GET /horarios/secciones/{id} ----------

	@Test
	void conCabeceraDevuelveLaMatrizConDocenteAmbienteYCurso() throws Exception {
		UUID eh = estructura("EH-T-27", "SECUNDARIA", true, 6);
		UUID aula = ambiente("TMP-A1", "ACTIVO");
		UUID docente = docente();
		UUID seccion = seccion("SEC-1", "T1", aula, docente);
		UUID algebra = plan("Álgebra", 5);
		asignarDocente(algebra, seccion, docente);
		UUID cab = cabecera(seccion, eh);
		bloque(cab, "LUN", 1, algebra, aula);
		bloque(cab, "MIE", 4, algebra, aula);

		pedir("/horarios/secciones/" + seccion, COORDINADOR).andExpect(status().isOk())
				.andExpect(jsonPath("$.seccion.id").value(seccion.toString()))
				.andExpect(jsonPath("$.seccion.nombre").value("T1"))
				.andExpect(jsonPath("$.seccion.gradoNombre").value("1.° de secundaria"))
				.andExpect(jsonPath("$.seccion.nivel").value("SECUNDARIA"))
				.andExpect(jsonPath("$.seccion.anioEscolarId").value(ANIO_2027.toString()))
				.andExpect(jsonPath("$.seccion.ambienteId").value(aula.toString()))
				.andExpect(jsonPath("$.seccion.tutor.id").value(docente.toString()))
				.andExpect(jsonPath("$.seccion.tutor.nombre").value(containsString("María")))
				.andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.editable").value(true))
				.andExpect(jsonPath("$.estructura.id").value(eh.toString()))
				.andExpect(jsonPath("$.estructura.codigo").value("EH-T-27"))
				.andExpect(jsonPath("$.estructura.principal").value(true))
				.andExpect(jsonPath("$.jornada.dias[0]").value("LUN"))
				.andExpect(jsonPath("$.jornada.dias", hasSize(5)))
				.andExpect(jsonPath("$.jornada.bloques", hasSize(6)))
				.andExpect(jsonPath("$.jornada.bloques[0].numero").value(1))
				.andExpect(jsonPath("$.jornada.bloques[0].inicio").value("08:00"))
				.andExpect(jsonPath("$.jornada.bloques[0].fin").value("08:45"))
				.andExpect(jsonPath("$.jornada.bloques[3].inicio").value("10:35"))
				.andExpect(jsonPath("$.jornada.recesos", hasSize(1)))
				.andExpect(jsonPath("$.jornada.recesos[0].despuesDeBloque").value(3))
				.andExpect(jsonPath("$.jornada.recesos[0].inicio").value("10:15"))
				.andExpect(jsonPath("$.jornada.recesos[0].fin").value("10:35"))
				.andExpect(jsonPath("$.celdas", hasSize(2)))
				.andExpect(jsonPath("$.celdas[0].dia").value("LUN"))
				.andExpect(jsonPath("$.celdas[0].numeroBloque").value(1))
				.andExpect(jsonPath("$.celdas[0].planCursoId").value(algebra.toString()))
				.andExpect(jsonPath("$.celdas[0].curso").value("Álgebra"))
				.andExpect(jsonPath("$.celdas[0].docente.id").value(docente.toString()))
				.andExpect(jsonPath("$.celdas[0].docente.nombre").value(containsString("María")))
				.andExpect(jsonPath("$.celdas[0].ambiente.id").value(aula.toString()))
				.andExpect(jsonPath("$.celdas[0].ambiente.nombre").value("Aula TMP-A1"))
				// La celda muestra el código del ambiente (Figma: «A-502»); el nombre va en el selector.
				.andExpect(jsonPath("$.celdas[0].ambiente.codigo").value("TMP-A1"))
				// El validador de cruces es un stub: cada celda llega con la lista vacía.
				.andExpect(jsonPath("$.celdas[0].cruces", empty()))
				.andExpect(jsonPath("$.celdas[1].dia").value("MIE"))
				.andExpect(jsonPath("$.celdas[1].numeroBloque").value(4))
				.andExpect(jsonPath("$.celdas[1].cruces", empty()));
	}

	@Test
	void sinCabeceraConPrincipalDevuelveLaJornadaSinGuardarNada() throws Exception {
		UUID eh = estructura("EH-T-27", "SECUNDARIA", true, 6);
		UUID seccion = seccion("SEC-1", "T1", null, null);
		long antes = cabeceras();

		pedir("/horarios/secciones/" + seccion, COORDINADOR).andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value(nullValue()))
				.andExpect(jsonPath("$.editable").value(true))
				.andExpect(jsonPath("$.estructura.id").value(eh.toString()))
				.andExpect(jsonPath("$.estructura.principal").value(true))
				.andExpect(jsonPath("$.seccion.ambienteId").value(nullValue()))
				.andExpect(jsonPath("$.seccion.tutor").value(nullValue()))
				.andExpect(jsonPath("$.jornada.bloques", hasSize(6)))
				.andExpect(jsonPath("$.celdas", empty()));

		assertThat(cabeceras()).isEqualTo(antes);
	}

	@Test
	void sinCabeceraYSinPrincipalResponde409ConElNivelDeLaSeccion() throws Exception {
		UUID secundaria = seccion("SEC-1", "T1", null, null);
		UUID primaria = seccion("PRI-1", "T1", null, null);

		pedir("/horarios/secciones/" + secundaria, COORDINADOR).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(SIN_PRINCIPAL_SECUNDARIA));
		pedir("/horarios/secciones/" + primaria, COORDINADOR).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Configura la estructura horaria principal de primaria antes de asignar horarios."));
	}

	@Test
	void conCabeceraUsaLaEstructuraGuardadaAunqueYaNoSeaLaPrincipal() throws Exception {
		estructura("EH-NUEVA", "SECUNDARIA", true, 6);
		UUID vieja = estructura("EH-VIEJA", "SECUNDARIA", false, 4);
		UUID seccion = seccion("SEC-1", "T1", null, null);
		cabecera(seccion, vieja);

		pedir("/horarios/secciones/" + seccion, COORDINADOR).andExpect(status().isOk())
				.andExpect(jsonPath("$.estructura.id").value(vieja.toString()))
				.andExpect(jsonPath("$.estructura.codigo").value("EH-VIEJA"))
				.andExpect(jsonPath("$.estructura.principal").value(false))
				.andExpect(jsonPath("$.jornada.bloques", hasSize(4)));
	}

	@Test
	void conElAnioCerradoResponde200ConEditableFalso() throws Exception {
		estructura("EH-T-27", "SECUNDARIA", true, 6);
		UUID seccion = seccion("SEC-1", "T1", null, null);
		jdbc.sql("UPDATE configuracion.anio_escolar SET estado = 'CERRADO' WHERE id = :id").param("id", ANIO_2027)
				.update();

		pedir("/horarios/secciones/" + seccion, COORDINADOR).andExpect(status().isOk())
				.andExpect(jsonPath("$.editable").value(false));
	}

	@Test
	void seccionInexistenteResponde404() throws Exception {
		pedir("/horarios/secciones/" + UUID.randomUUID(), COORDINADOR).andExpect(status().isNotFound());
		pedir("/horarios/secciones/" + UUID.randomUUID() + "/opciones", COORDINADOR).andExpect(status().isNotFound());
	}

	@Test
	void unaCeldaSinDocenteLlegaConDocenteNulo() throws Exception {
		UUID eh = estructura("EH-T-27", "SECUNDARIA", true, 6);
		UUID aula = ambiente("TMP-A1", "ACTIVO");
		UUID seccion = seccion("SEC-1", "T1", aula, null);
		UUID geometria = plan("Geometría", 3);
		UUID cab = cabecera(seccion, eh);
		bloque(cab, "MAR", 2, geometria, aula);

		pedir("/horarios/secciones/" + seccion, COORDINADOR).andExpect(status().isOk())
				.andExpect(jsonPath("$.celdas", hasSize(1)))
				.andExpect(jsonPath("$.celdas[0].curso").value("Geometría"))
				.andExpect(jsonPath("$.celdas[0].docente").value(nullValue()))
				.andExpect(jsonPath("$.celdas[0].cruces", empty()));
	}

	@Test
	void otroRolRecibe403EnLosDosEndpoints() throws Exception {
		UUID seccion = seccion("SEC-1", "T1", null, null);

		pedir("/horarios/secciones/" + seccion, PERSONAL_ADMINISTRATIVO).andExpect(status().isForbidden());
		pedir("/horarios/secciones/" + seccion + "/opciones", PERSONAL_ADMINISTRATIVO)
				.andExpect(status().isForbidden());
	}

	// ---------- GET /horarios/secciones/{id}/opciones ----------

	@Test
	void opcionesTraeLosCursosConSusHorasAsignadasYDocente() throws Exception {
		UUID eh = estructura("EH-T-27", "SECUNDARIA", true, 6);
		UUID aula = ambiente("TMP-A1", "ACTIVO");
		UUID docente = docente();
		UUID seccion = seccion("SEC-1", "T1", aula, null);
		UUID algebra = plan("Álgebra", 5);
		UUID geometria = plan("Geometría", 3);
		asignarDocente(algebra, seccion, docente);
		UUID cab = cabecera(seccion, eh);
		bloque(cab, "LUN", 1, algebra, aula);
		bloque(cab, "MAR", 1, algebra, aula);

		pedir("/horarios/secciones/" + seccion + "/opciones", COORDINADOR).andExpect(status().isOk())
				.andExpect(jsonPath("$.ambientePorDefectoId").value(aula.toString()))
				.andExpect(jsonPath("$.cursos", hasSize(2)))
				.andExpect(jsonPath("$.cursos[?(@.nombre=='Álgebra')].planCursoId", contains(algebra.toString())))
				.andExpect(jsonPath("$.cursos[?(@.nombre=='Álgebra')].horasSemanales", contains(5)))
				.andExpect(jsonPath("$.cursos[?(@.nombre=='Álgebra')].horasAsignadas", contains(2)))
				.andExpect(jsonPath("$.cursos[?(@.nombre=='Álgebra')].docente.id", contains(docente.toString())))
				.andExpect(jsonPath("$.cursos[?(@.nombre=='Geometría')].planCursoId", contains(geometria.toString())))
				.andExpect(jsonPath("$.cursos[?(@.nombre=='Geometría')].horasSemanales", contains(3)))
				.andExpect(jsonPath("$.cursos[?(@.nombre=='Geometría')].horasAsignadas", contains(0)))
				.andExpect(jsonPath("$.cursos[?(@.nombre=='Geometría')].docente", contains(nullValue())));
	}

	@Test
	void opcionesSoloOfreceAmbientesActivosYSinAulaNoHayPorDefecto() throws Exception {
		UUID activo = ambiente("TMP-ACT", "ACTIVO");
		UUID inactivo = ambiente("TMP-INA", "INACTIVO");
		UUID seccion = seccion("SEC-1", "T1", null, null);
		long activos = jdbc.sql("SELECT count(*) FROM configuracion.ambiente WHERE estado = 'ACTIVO'")
				.query(Long.class).single();

		pedir("/horarios/secciones/" + seccion + "/opciones", COORDINADOR).andExpect(status().isOk())
				.andExpect(jsonPath("$.ambientePorDefectoId").value(nullValue()))
				.andExpect(jsonPath("$.cursos", empty()))
				.andExpect(jsonPath("$.ambientes", hasSize((int) activos)))
				.andExpect(jsonPath("$.ambientes[*].id", hasItem(activo.toString())))
				.andExpect(jsonPath("$.ambientes[*].id", not(hasItem(inactivo.toString()))))
				.andExpect(jsonPath("$.ambientes[?(@.id=='" + activo + "')].codigo", contains("TMP-ACT")))
				.andExpect(jsonPath("$.ambientes[?(@.id=='" + activo + "')].tipo", contains("AULA_REGULAR")))
				.andExpect(jsonPath("$.ambientes[?(@.id=='" + activo + "')].aforoMaximo", contains(30)));
	}
}
