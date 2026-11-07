package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HU-45 en Configuración: una estructura con horarios armados no se elimina, no pierde días ni bloques que los horarios
 * usan y no deja de ser la principal. Se prueba por la API de estructuras horarias (rol Personal administrativo) con
 * datos propios en 2027 (PLANIFICADO): la principal EH-T-27 (6 bloques, lunes a viernes) tiene una sección con
 * bloques el lunes (hasta el 5) y el miércoles (hasta el 2).
 */
class EstructuraConHorariosIT extends PruebaHorarios {
	private static final String SIN_HORARIOS_ELIMINAR = "No se puede eliminar la estructura horaria: hay horarios armados con ella.";
	private static final String SIN_CAMBIAR_PRINCIPAL = "No se puede cambiar la estructura principal de secundaria: hay horarios armados con la actual.";

	private UUID principal;
	private UUID otra;
	private UUID seccion;
	private UUID cabecera;

	@BeforeEach
	void escenario() {
		principal = estructura("EH-T-27", "SECUNDARIA", true, 6);
		otra = estructura("EH-OTRA", "SECUNDARIA", false, 6);
		UUID aula = ambiente("TMP-A1", "ACTIVO");
		seccion = seccion("SEC-1", "T1", aula, null);
		UUID algebra = plan("Álgebra", 5);
		cabecera = cabecera(seccion, principal);
		bloque(cabecera, "LUN", 5, algebra, aula);
		bloque(cabecera, "MIE", 2, algebra, aula);
	}

	private static String edicion(String codigo, String dias, int bloquesPorDia, Boolean esPrincipal) {
		return """
				{"codigo":"%s","descripcion":null,"esPrincipal":%s,"diasLectivos":[%s],"horaEntrada":"08:00",
				 "bloquesPorDia":%d,"duracionBloqueMin":45,"recesos":[]}"""
				.formatted(codigo, esPrincipal, dias, bloquesPorDia);
	}

	private static final String LUN_A_VIE = "\"LUN\",\"MAR\",\"MIE\",\"JUE\",\"VIE\"";

	private ResultActions editar(UUID id, String cuerpo) throws Exception {
		return mvc.perform(conToken(put("/configuracion/estructuras-horarias/" + id), token(PERSONAL_ADMINISTRATIVO),
				cuerpo));
	}

	private ResultActions eliminar(UUID id) throws Exception {
		return mvc.perform(conToken(delete("/configuracion/estructuras-horarias/" + id),
				token(PERSONAL_ADMINISTRATIVO), null));
	}

	// ---------- Eliminar ----------

	@Test
	void eliminarUnaEstructuraConHorariosResponde409() throws Exception {
		eliminar(principal).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(SIN_HORARIOS_ELIMINAR));
	}

	@Test
	void eliminarUnaEstructuraConCabeceraSinBloquesResponde409ConElMismoMensaje() throws Exception {
		jdbc.sql("DELETE FROM horarios.horario_bloque").update();

		eliminar(principal).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(SIN_HORARIOS_ELIMINAR));
		assertThat(bloquesGuardados()).isZero();
		assertThat(cabeceras()).isEqualTo(1);
	}

	@Test
	void eliminarUnaEstructuraSinHorariosSigueFuncionando() throws Exception {
		eliminar(otra).andExpect(status().isNoContent());
	}

	@Test
	void eliminarLaPrincipalCuandoYaNoTieneHorariosSigueSujetaASusReglasDeSiempre() throws Exception {
		jdbc.sql("DELETE FROM horarios.horario_seccion").update();

		// Quedan otras del nivel: la regla de HU-44 sigue respondiendo su propio 409.
		eliminar(principal).andExpect(status().isConflict()).andExpect(jsonPath("$.message")
				.value(org.hamcrest.Matchers.containsString("No se puede eliminar la estructura principal")));
	}

	// ---------- Quitar días ----------

	@Test
	void quitarUnDiaQueUsanLosHorariosResponde409() throws Exception {
		editar(principal, edicion("EH-T-27", "\"MAR\",\"MIE\",\"JUE\",\"VIE\"", 6, null))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("No se puede quitar el lunes de la estructura: hay horarios que lo usan."));
		editar(principal, edicion("EH-T-27", "\"LUN\",\"MAR\",\"JUE\",\"VIE\"", 6, null))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("No se puede quitar el miércoles de la estructura: hay horarios que lo usan."));
	}

	@Test
	void quitarDiasQueNadaUsaFunciona() throws Exception {
		editar(principal, edicion("EH-T-27", "\"LUN\",\"MIE\"", 6, null)).andExpect(status().isOk())
				.andExpect(jsonPath("$.estructura.diasLectivos.length()").value(2));
	}

	// ---------- Reducir bloques ----------

	@Test
	void reducirLosBloquesPorDebajoDelMayorUsadoResponde409ConElDiaYElBloque() throws Exception {
		editar(principal, edicion("EH-T-27", LUN_A_VIE, 4, null)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"No se puede reducir a 4 bloques por día: hay horarios que usan hasta el bloque 5 el lunes."));
	}

	@Test
	void reducirJustoHastaElMayorUsadoFunciona() throws Exception {
		editar(principal, edicion("EH-T-27", LUN_A_VIE, 5, null)).andExpect(status().isOk())
				.andExpect(jsonPath("$.estructura.bloquesPorDia").value(5));
	}

	@Test
	void agregarDiasOBloquesNuncaSeRechaza() throws Exception {
		editar(principal, edicion("EH-T-27", LUN_A_VIE + ",\"SAB\"", 8, null)).andExpect(status().isOk())
				.andExpect(jsonPath("$.estructura.bloquesPorDia").value(8))
				.andExpect(jsonPath("$.estructura.diasLectivos.length()").value(6));
	}

	// ---------- Cambiar la principal ----------

	@Test
	void marcarOtraComoPrincipalMientrasLaActualTieneHorariosResponde409() throws Exception {
		editar(otra, edicion("EH-OTRA", LUN_A_VIE, 6, true)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(SIN_CAMBIAR_PRINCIPAL));
		assertThat(jdbc.sql("SELECT es_principal FROM configuracion.estructura_horaria WHERE id = :id")
				.param("id", principal).query(Boolean.class).single()).isTrue();
	}

	@Test
	void crearUnaNuevaPrincipalMientrasLaActualTieneHorariosTambienResponde409() throws Exception {
		mvc.perform(conToken(post("/configuracion/anios-escolares/" + ANIO_2027 + "/estructuras-horarias"),
				token(PERSONAL_ADMINISTRATIVO),
				"""
						{"codigo":"EH-NUEVA","descripcion":null,"nivel":"SECUNDARIA","esPrincipal":true,
						 "diasLectivos":[%s],"horaEntrada":"08:00","bloquesPorDia":6,"duracionBloqueMin":45,"recesos":[]}"""
						.formatted(LUN_A_VIE)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(SIN_CAMBIAR_PRINCIPAL));
	}

	@Test
	void marcarOtraComoPrincipalSinHorariosEnLaActualFunciona() throws Exception {
		jdbc.sql("DELETE FROM horarios.horario_seccion").update();

		editar(otra, edicion("EH-OTRA", LUN_A_VIE, 6, true)).andExpect(status().isOk())
				.andExpect(jsonPath("$.estructura.esPrincipal").value(true));
	}

	@Test
	void editarLaPrincipalSinCambiarSuMarcaConHorariosFunciona() throws Exception {
		editar(principal, edicion("EH-T-27-B", LUN_A_VIE, 6, true)).andExpect(status().isOk())
				.andExpect(jsonPath("$.estructura.codigo").value("EH-T-27"));
	}

	// ---------- Otros años y otras estructuras ----------

	@Test
	void losHorariosDeOtroAnioNiDeOtraEstructuraAfectan() throws Exception {
		UUID anio2028 = jdbc.sql("""
				INSERT INTO configuracion.anio_escolar (anio, fecha_inicio, fecha_fin, regimen)
				VALUES (2028, DATE '2028-03-01', DATE '2028-12-20', 'BIMESTRAL') RETURNING id""")
				.query(UUID.class).single();
		UUID de2028 = jdbc.sql("""
				INSERT INTO configuracion.estructura_horaria
				    (anio_escolar_id, codigo, dias_lectivos, hora_entrada, bloques_por_dia, duracion_bloque_min, nivel,
				     es_principal)
				VALUES (:anio, 'EH-T-28', ARRAY['LUN','MAR']::varchar[], TIME '08:00', 6, 45, 'SECUNDARIA', true)
				RETURNING id""").param("anio", anio2028).query(UUID.class).single();

		// La de 2028 no tiene horarios: quitar el lunes y reducir bloques funciona aunque la de 2027 los tenga.
		editar(de2028, edicion("EH-T-28", "\"MAR\"", 2, null)).andExpect(status().isOk());
		eliminar(de2028).andExpect(status().isNoContent());
		// Y la de 2027 sigue protegida.
		eliminar(principal).andExpect(status().isConflict());
	}
}
