package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.galiledu.horarios.aplicacion.ServicioHorarios;

/**
 * HU-45, Fase 2: el PUT de la matriz completa. Cada prueba arma sus datos en 2027 (ver {@link PruebaHorarios}): una
 * estructura principal de secundaria de 6 bloques, la sección 1.° de secundaria "T1" con su aula, y los cursos Álgebra
 * (5 h, con docente), Aritmética (2 h, con docente) y Geometría (3 h, sin docente).
 */
class HorarioEscrituraIT extends PruebaHorarios {
	private static final String OTRA_PERSONA = "Otra persona modificó el horario de 1.° de secundaria T1. Recarga para ver los cambios.";

	@Autowired
	private ServicioHorarios servicio;

	private UUID estructura;
	private UUID aula;
	private UUID docente;
	private UUID seccion;
	private UUID algebra;
	private UUID aritmetica;
	private UUID geometria;

	@BeforeEach
	void escenario() {
		estructura = estructura("EH-T-27", "SECUNDARIA", true, 6);
		aula = ambiente("TMP-A1", "ACTIVO");
		docente = docente();
		seccion = seccion("SEC-1", "T1", aula, null);
		algebra = plan("Álgebra", 5);
		aritmetica = plan("Aritmética", 2);
		geometria = plan("Geometría", 3);
		asignarDocente(algebra, seccion, docente);
		asignarDocente(aritmetica, seccion, docente);
	}

	private Celda celda(String dia, int bloque, UUID plan) {
		return new Celda(dia, bloque, plan, aula);
	}

	private long version(UUID seccionId) {
		return jdbc.sql("SELECT version FROM horarios.horario_seccion WHERE seccion_id = :s").param("s", seccionId)
				.query(Long.class).single();
	}

	private Map<String, UUID> idsDeBloques(UUID seccionId) {
		Map<String, UUID> ids = new java.util.HashMap<>();
		jdbc.sql("""
				SELECT hb.dia || ':' || hb.numero_bloque AS clave, hb.id
				FROM horarios.horario_bloque hb JOIN horarios.horario_seccion hs ON hs.id = hb.horario_seccion_id
				WHERE hs.seccion_id = :s""").param("s", seccionId)
				.query((r, n) -> ids.put(r.getString("clave"), r.getObject("id", UUID.class))).list();
		return ids;
	}

	// ---------- Reglas ----------

	@Test
	void anioCerradoResponde409() throws Exception {
		jdbc.sql("UPDATE configuracion.anio_escolar SET estado = 'CERRADO' WHERE id = :id").param("id", ANIO_2027)
				.update();

		guardar(seccion, COORDINADOR, null, celda("LUN", 1, algebra)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("No se puede modificar el horario de un año cerrado."));
		assertThat(cabeceras()).isZero();
	}

	@Test
	void versionDesactualizadaResponde409() throws Exception {
		cabecera(seccion, estructura);
		jdbc.sql("UPDATE horarios.horario_seccion SET version = 3 WHERE seccion_id = :s").param("s", seccion).update();

		guardar(seccion, COORDINADOR, 1L, celda("LUN", 1, algebra)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(OTRA_PERSONA));
		assertThat(bloquesGuardados()).isZero();
	}

	@Test
	void versionNulaConCabeceraExistenteResponde409() throws Exception {
		cabecera(seccion, estructura);

		guardar(seccion, COORDINADOR, null, celda("LUN", 1, algebra)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(OTRA_PERSONA));
	}

	@Test
	void versionConValorSinCabeceraResponde409() throws Exception {
		guardar(seccion, COORDINADOR, 0L, celda("LUN", 1, algebra)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(OTRA_PERSONA));
		assertThat(cabeceras()).isZero();
	}

	@Test
	void celdaRepetidaResponde400() throws Exception {
		guardar(seccion, COORDINADOR, null, celda("LUN", 1, algebra), celda("LUN", 1, aritmetica))
				.andExpect(status().isBadRequest())
				.andExpect(
						jsonPath("$.message").value("El horario tiene dos cursos en el mismo bloque: LUN, bloque 1."));
	}

	@Test
	void diaNoLectivoResponde400() throws Exception {
		guardar(seccion, COORDINADOR, null, celda("SAB", 1, algebra)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("El día SAB no es lectivo en la estructura horaria de la sección."));
	}

	@Test
	void bloqueFueraDeLaJornadaResponde400() throws Exception {
		// La jornada tiene los bloques 1 a 6; los recesos no tienen número, así que ningún número los representa.
		guardar(seccion, COORDINADOR, null, celda("LUN", 7, algebra)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El bloque 7 no existe en la jornada de la sección."));
	}

	@Test
	void bloqueCeroResponde400() throws Exception {
		guardar(seccion, COORDINADOR, null, celda("LUN", 0, algebra)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El número de bloque empieza en 1."));
	}

	@Test
	void cursoDeOtroGradoOAnioResponde400() throws Exception {
		UUID deOtroGrado = plan("SEC-2", "Álgebra", 4);
		UUID deOtroAnio = jdbc
				.sql("""
						SELECT id FROM academico.plan_curso WHERE anio_escolar_id <> :anio AND grado_codigo = 'SEC-1' LIMIT 1""")
				.param("anio", ANIO_2027).query(UUID.class).single();
		String mensaje = "El curso elegido no pertenece al plan de 1.° de secundaria de este año.";

		guardar(seccion, COORDINADOR, null, celda("LUN", 1, deOtroGrado)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(mensaje));
		guardar(seccion, COORDINADOR, null, celda("LUN", 1, deOtroAnio)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(mensaje));
		guardar(seccion, COORDINADOR, null, celda("LUN", 1, UUID.randomUUID())).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(mensaje));
	}

	@Test
	void ambienteInactivoOInexistenteResponde400() throws Exception {
		UUID inactivo = ambiente("TMP-INA", "INACTIVO");
		String mensaje = "El ambiente elegido no existe o no está activo.";

		guardar(seccion, COORDINADOR, null, new Celda("LUN", 1, algebra, inactivo)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(mensaje));
		guardar(seccion, COORDINADOR, null, new Celda("LUN", 1, algebra, UUID.randomUUID()))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(mensaje));
	}

	@Test
	void cursoSinDocenteResponde409() throws Exception {
		guardar(seccion, COORDINADOR, null, celda("LUN", 1, geometria)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Asigna un docente a Geometría en Plan de evaluación antes de ubicarlo en el horario."));
	}

	@Test
	void horasSemanalesExcedidasResponde409() throws Exception {
		guardar(seccion, COORDINADOR, null, celda("LUN", 1, aritmetica), celda("MAR", 1, aritmetica),
				celda("MIE", 1, aritmetica)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Aritmética ya tiene sus 2 horas semanales asignadas."));
		assertThat(cabeceras()).isZero();
	}

	@Test
	void sinEstructuraPrincipalResponde409() throws Exception {
		jdbc.sql("DELETE FROM configuracion.estructura_horaria WHERE id = :id").param("id", estructura).update();

		guardar(seccion, COORDINADOR, null, celda("LUN", 1, algebra)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Configura la estructura horaria principal de secundaria antes de asignar horarios."));
	}

	@Test
	void otroRolRecibe403() throws Exception {
		guardar(seccion, PERSONAL_ADMINISTRATIVO, null, celda("LUN", 1, algebra)).andExpect(status().isForbidden());
	}

	@Test
	void seccionInexistenteResponde404() throws Exception {
		guardar(UUID.randomUUID(), COORDINADOR, null, celda("LUN", 1, algebra)).andExpect(status().isNotFound());
	}

	@Test
	void unCuerpoSinCeldasOConUnDiaInvalidoResponde400() throws Exception {
		mvc.perform(conToken(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.put("/horarios/secciones/" + seccion), token(COORDINADOR), "{\"version\":null}"))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.put("/horarios/secciones/" + seccion), token(COORDINADOR),
				"{\"version\":null,\"celdas\":[{\"dia\":\"DOM\",\"numeroBloque\":1,\"planCursoId\":\"" + algebra
						+ "\",\"ambienteId\":\"" + aula + "\"}]}"))
				.andExpect(status().isBadRequest());
	}

	// ---------- Comportamiento ----------

	@Test
	void primerGuardadoCreaLaCabeceraConLaEstructuraPrincipalYVersionCero() throws Exception {
		guardar(seccion, COORDINADOR, null, celda("LUN", 1, algebra), celda("MIE", 4, algebra))
				.andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0))
				.andExpect(jsonPath("$.estructura.id").value(estructura.toString()))
				.andExpect(jsonPath("$.celdas", hasSize(2)))
				.andExpect(jsonPath("$.celdas[0].curso").value("Álgebra"))
				.andExpect(jsonPath("$.celdas[0].docente.id").value(docente.toString()))
				.andExpect(jsonPath("$.celdas[0].cruces", empty()));

		assertThat(cabeceras()).isEqualTo(1);
		assertThat(bloquesGuardados()).isEqualTo(2);
		assertThat(jdbc.sql("SELECT estructura_horaria_id FROM horarios.horario_seccion WHERE seccion_id = :s")
				.param("s", seccion).query(UUID.class).single()).isEqualTo(estructura);
		// El GET devuelve lo mismo que respondió el PUT.
		pedir("/horarios/secciones/" + seccion, COORDINADOR).andExpect(jsonPath("$.version").value(0))
				.andExpect(jsonPath("$.celdas", hasSize(2)));
	}

	@Test
	void cadaGuardadoConCambiosSubeLaVersionExactamenteUno() throws Exception {
		guardar(seccion, COORDINADOR, null, celda("LUN", 1, algebra)).andExpect(jsonPath("$.version").value(0));

		// Cambian los bloques (se agrega uno): la versión sube 1.
		guardar(seccion, COORDINADOR, 0L, celda("LUN", 1, algebra), celda("MAR", 2, aritmetica))
				.andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
		assertThat(version(seccion)).isEqualTo(1);

		// Solo cambia el ambiente de un bloque existente (la fila de la cabecera no cambia): también sube 1.
		UUID otraAula = ambiente("TMP-A2", "ACTIVO");
		guardar(seccion, COORDINADOR, 1L, new Celda("LUN", 1, algebra, otraAula), celda("MAR", 2, aritmetica))
				.andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2))
				.andExpect(jsonPath("$.celdas[0].ambiente.id").value(otraAula.toString()));
		assertThat(version(seccion)).isEqualTo(2);
	}

	@Test
	void unGuardadoIdenticoNoSubeLaVersion() throws Exception {
		guardar(seccion, COORDINADOR, null, celda("LUN", 1, algebra), celda("MAR", 2, aritmetica));
		java.time.Instant actualizado = jdbc
				.sql("SELECT actualizado_en FROM horarios.horario_seccion WHERE seccion_id = :s").param("s", seccion)
				.query(java.time.Instant.class).single();

		guardar(seccion, COORDINADOR, 0L, celda("MAR", 2, aritmetica), celda("LUN", 1, algebra))
				.andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0))
				.andExpect(jsonPath("$.celdas", hasSize(2)));

		assertThat(version(seccion)).isZero();
		assertThat(jdbc.sql("SELECT actualizado_en FROM horarios.horario_seccion WHERE seccion_id = :s")
				.param("s", seccion).query(java.time.Instant.class).single()).isEqualTo(actualizado);
	}

	@Test
	void guardarRenuevaActualizadoEn() throws Exception {
		guardar(seccion, COORDINADOR, null, celda("LUN", 1, algebra));
		jdbc.sql("UPDATE horarios.horario_seccion SET actualizado_en = TIMESTAMPTZ '2020-01-01 00:00:00+00'")
				.update();

		guardar(seccion, COORDINADOR, 0L, celda("LUN", 1, algebra), celda("MAR", 2, aritmetica));

		assertThat(jdbc.sql("SELECT actualizado_en FROM horarios.horario_seccion WHERE seccion_id = :s")
				.param("s", seccion).query(java.time.Instant.class).single())
				.isAfter(java.time.Instant.parse("2021-01-01T00:00:00Z"));
	}

	@Test
	void losBloquesSinCambioConservanSuId() throws Exception {
		guardar(seccion, COORDINADOR, null, celda("LUN", 1, algebra), celda("MAR", 2, algebra),
				celda("MIE", 3, aritmetica));
		Map<String, UUID> antes = idsDeBloques(seccion);

		// LUN:1 igual; MAR:2 cambia de curso; MIE:3 se elimina; JUE:4 es nuevo.
		guardar(seccion, COORDINADOR, 0L, celda("LUN", 1, algebra), celda("MAR", 2, aritmetica),
				celda("JUE", 4, algebra)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.celdas", hasSize(3)));
		Map<String, UUID> despues = idsDeBloques(seccion);

		assertThat(despues).containsOnlyKeys("LUN:1", "MAR:2", "JUE:4");
		assertThat(despues.get("LUN:1")).isEqualTo(antes.get("LUN:1"));
		assertThat(despues.get("MAR:2")).isEqualTo(antes.get("MAR:2"));
		assertThat(despues.get("JUE:4")).isNotIn(antes.values());
		assertThat(jdbc.sql("""
				SELECT plan_curso_id FROM horarios.horario_bloque WHERE id = :id""").param("id", despues.get("MAR:2"))
				.query(UUID.class).single()).isEqualTo(aritmetica);
	}

	@Test
	void celdasVaciasSinCabeceraNoCreanNada() throws Exception {
		guardar(seccion, COORDINADOR, null).andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value(nullValue()))
				.andExpect(jsonPath("$.celdas", empty()));

		assertThat(cabeceras()).isZero();
	}

	@Test
	void unaCabeceraExistenteConservaSuEstructuraAunqueYaNoSeaLaPrincipal() throws Exception {
		UUID vieja = estructura("EH-VIEJA", "SECUNDARIA", false, 4);
		jdbc.sql("UPDATE configuracion.estructura_horaria SET es_principal = false WHERE id = :id")
				.param("id", estructura).update();
		cabecera(seccion, vieja);

		guardar(seccion, COORDINADOR, 0L, celda("LUN", 1, algebra)).andExpect(status().isOk())
				.andExpect(jsonPath("$.estructura.id").value(vieja.toString()))
				.andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.jornada.bloques", hasSize(4)));
		assertThat(jdbc.sql("SELECT estructura_horaria_id FROM horarios.horario_seccion WHERE seccion_id = :s")
				.param("s", seccion).query(UUID.class).single()).isEqualTo(vieja);
		// Con esa jornada de 4 bloques, el bloque 5 ya no existe.
		guardar(seccion, COORDINADOR, 1L, celda("LUN", 5, algebra)).andExpect(status().isBadRequest());
	}

	@Test
	void vaciarElHorarioEliminaTodosLosBloquesYSubeLaVersion() throws Exception {
		guardar(seccion, COORDINADOR, null, celda("LUN", 1, algebra), celda("MAR", 2, algebra));

		guardar(seccion, COORDINADOR, 0L).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.celdas", empty()));
		assertThat(bloquesGuardados()).isZero();
		assertThat(cabeceras()).isEqualTo(1);
	}

	// ---------- bloquesDeCurso ----------

	@Test
	void bloquesDeCursoCuentaLosBloquesDelCursoEnLaSeccion() {
		assertThat(servicio.bloquesDeCurso(algebra, seccion)).isZero();

		UUID cab = cabecera(seccion, estructura);
		bloque(cab, "LUN", 1, algebra, aula);
		bloque(cab, "MAR", 1, algebra, aula);
		bloque(cab, "MIE", 1, aritmetica, aula);
		UUID otra = seccion("SEC-1", "T2", aula, null);
		bloque(cabecera(otra, estructura), "LUN", 1, algebra, aula);

		assertThat(servicio.bloquesDeCurso(algebra, seccion)).isEqualTo(2);
		assertThat(servicio.bloquesDeCurso(aritmetica, seccion)).isEqualTo(1);
		assertThat(servicio.bloquesDeCurso(geometria, seccion)).isZero();
		assertThat(servicio.bloquesDeCurso(algebra, otra)).isEqualTo(1);
		assertThat(List.of(servicio.bloquesDeCurso(algebra, UUID.randomUUID()))).containsExactly(0);
	}
}
