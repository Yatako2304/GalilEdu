package com.galiledu.horarios;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * HU-49: GET /horarios/docente/anios y /horarios/docente/horario con la BD real. El docente es María Fernández Ríos
 * (U20260004, Docente y Tutor); "otro docente" es el Coordinador (U20260005), porque solo importa que sea otro usuario.
 * 2026 es el año ACTIVO de los datos de prueba y 2027 el PLANIFICADO.
 */
class HorarioDocenteIT extends PruebaHorarios {
	private static final String APODERADO = "U20260003";
	private static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";

	private UUID estructuraSecundaria;
	private UUID aulaUno;
	private UUID aulaDos;
	private UUID algebra;
	private UUID t1;
	private UUID t2;

	/** Secundaria 2027 (08:00): 1.° T1 y 1.° T2 con Álgebra, cada una con su aula. Nadie es titular todavía. */
	private void secundaria2027() {
		estructuraSecundaria = estructura("EH-T-27", "SECUNDARIA", true, 6);
		aulaUno = ambiente("TMP-A1", "ACTIVO");
		aulaDos = ambiente("TMP-A2", "ACTIVO");
		algebra = plan("Álgebra", 5);
		t1 = seccion("SEC-1", "T1", aulaUno, null);
		t2 = seccion("SEC-1", "T2", aulaDos, null);
	}

	private UUID usuario(String nombreUsuario) {
		return jdbc.sql("SELECT id FROM seguridad.usuario WHERE nombre_usuario = :u").param("u", nombreUsuario)
				.query(UUID.class).single();
	}

	/** Un usuario que solo tiene el rol TUTOR y puede iniciar sesión. */
	private String soloTutor() {
		UUID persona = jdbc.sql("""
				INSERT INTO personas.persona (tipo_documento, numero_documento, apellido_paterno, apellido_materno,
				                              nombres, fecha_nacimiento, nacionalidad, sexo)
				VALUES ('DNI', '79999991', 'Tutor', 'Solo', 'Tomás', '1980-01-01', 'Peruana', 'MASCULINO')
				RETURNING id""").query(UUID.class).single();
		UUID id = jdbc.sql("""
				INSERT INTO seguridad.usuario (persona_id, nombre_usuario, contrasena_hash, estado)
				VALUES (:p, 'U20269991', crypt(:clave, gen_salt('bf', 10)), 'ACTIVO') RETURNING id""")
				.param("p", persona).param("clave", CONTRASENA).query(UUID.class).single();
		jdbc.sql("INSERT INTO seguridad.usuario_rol (usuario_id, rol_codigo) VALUES (:u, 'TUTOR')").param("u", id)
				.update();
		return "U20269991";
	}

	/** Una clase de Álgebra de María en 1.° A de 2026 (año ACTIVO), con la estructura EH-SEC-26. */
	private void claseDe2026(UUID ambiente) {
		UUID seccion = jdbc.sql("""
				SELECT id FROM configuracion.seccion
				WHERE anio_escolar_id = CAST(:anio AS uuid) AND grado_codigo = 'SEC-1' AND nombre = 'A'""")
				.param("anio", ANIO_2026).query(UUID.class).single();
		UUID plan = jdbc
				.sql("""
						SELECT pc.id FROM academico.plan_curso pc JOIN academico.curso c ON c.id = pc.curso_id
						WHERE pc.anio_escolar_id = CAST(:anio AS uuid) AND pc.grado_codigo = 'SEC-1' AND c.nombre = 'Álgebra'""")
				.param("anio", ANIO_2026).query(UUID.class).single();
		bloque(cabecera(seccion, UUID.fromString("31000000-0000-0000-0000-000000000002")), "MIE", 2, plan, ambiente);
	}

	@Test
	void docenteConClasesEnDosSeccionesDeSecundariaRecibeUnHorarioSecundaria() throws Exception {
		secundaria2027();
		asignarDocente(algebra, t1, docente());
		asignarDocente(algebra, t2, docente());
		bloque(cabecera(t1, estructuraSecundaria), "LUN", 1, algebra, aulaUno);
		bloque(cabecera(t2, estructuraSecundaria), "MAR", 2, algebra, aulaDos);

		pedir("/horarios/docente/horario", DOCENTE).andExpect(status().isOk())
				.andExpect(jsonPath("$.anio.anio").value(2027))
				.andExpect(jsonPath("$.horarios", hasSize(1)))
				.andExpect(jsonPath("$.horarios[0].nivel").value("SECUNDARIA"))
				.andExpect(jsonPath("$.horarios[0].titulo").value("Secundaria"))
				.andExpect(jsonPath("$.horarios[0].clases", hasSize(2)))
				.andExpect(jsonPath("$.horarios[0].clases[0].dia").value("LUN"))
				.andExpect(jsonPath("$.horarios[0].clases[0].numeroBloque").value(1))
				.andExpect(jsonPath("$.horarios[0].clases[0].curso").value("Álgebra"))
				.andExpect(jsonPath("$.horarios[0].clases[0].seccion").value("1.° T1"))
				.andExpect(jsonPath("$.horarios[0].clases[0].ambiente").value("Aula TMP-A1"))
				.andExpect(jsonPath("$.horarios[0].clases[1].dia").value("MAR"))
				.andExpect(jsonPath("$.horarios[0].clases[1].seccion").value("1.° T2"))
				.andExpect(jsonPath("$.horarios[0].clases[1].ambiente").value("Aula TMP-A2"))
				.andExpect(jsonPath("$.horarios[0].jornada.dias[0]").value("LUN"))
				.andExpect(jsonPath("$.horarios[0].jornada.dias", hasSize(5)))
				.andExpect(jsonPath("$.horarios[0].jornada.bloques", hasSize(6)))
				.andExpect(jsonPath("$.horarios[0].jornada.bloques[0].numero").value(1))
				.andExpect(jsonPath("$.horarios[0].jornada.bloques[0].inicio").value("08:00"))
				.andExpect(jsonPath("$.horarios[0].jornada.bloques[0].fin").value("08:45"))
				.andExpect(jsonPath("$.horarios[0].jornada.recesos", hasSize(1)))
				.andExpect(jsonPath("$.horarios[0].jornada.recesos[0].despuesDeBloque").value(3))
				.andExpect(jsonPath("$.horarios[0].jornada.recesos[0].inicio").value("10:15"))
				.andExpect(jsonPath("$.horarios[0].jornada.recesos[0].fin").value("10:35"));
	}

	@Test
	void docenteConPrimariaYSecundariaDeDistintaHoraRecibeDosHorariosConPrimariaPrimero() throws Exception {
		secundaria2027();
		UUID estructuraPrimaria = estructura("EH-P-27", "PRIMARIA", true, 6, "08:30");
		jdbc.sql("""
				INSERT INTO academico.curso_grado (curso_id, grado_codigo)
				SELECT id, 'PRI-1' FROM academico.curso WHERE nombre = 'Álgebra'""").update();
		UUID algebraPrimaria = plan("PRI-1", "Álgebra", 3);
		UUID p1 = seccion("PRI-1", "T3", aulaDos, null);
		asignarDocente(algebra, t1, docente());
		asignarDocente(algebraPrimaria, p1, docente());
		bloque(cabecera(t1, estructuraSecundaria), "LUN", 1, algebra, aulaUno);
		bloque(cabecera(p1, estructuraPrimaria), "LUN", 1, algebraPrimaria, aulaDos);

		pedir("/horarios/docente/horario", DOCENTE).andExpect(status().isOk())
				.andExpect(jsonPath("$.horarios", hasSize(2)))
				.andExpect(jsonPath("$.horarios[0].nivel").value("PRIMARIA"))
				.andExpect(jsonPath("$.horarios[0].titulo").value("Primaria"))
				.andExpect(jsonPath("$.horarios[0].jornada.bloques[0].inicio").value("08:30"))
				.andExpect(jsonPath("$.horarios[0].clases", hasSize(1)))
				.andExpect(jsonPath("$.horarios[0].clases[0].seccion").value("1.° T3"))
				.andExpect(jsonPath("$.horarios[1].nivel").value("SECUNDARIA"))
				.andExpect(jsonPath("$.horarios[1].titulo").value("Secundaria"))
				.andExpect(jsonPath("$.horarios[1].jornada.bloques[0].inicio").value("08:00"))
				.andExpect(jsonPath("$.horarios[1].clases", hasSize(1)))
				.andExpect(jsonPath("$.horarios[1].clases[0].seccion").value("1.° T1"));
	}

	@Test
	void noVeLasClasesDeOtroDocente() throws Exception {
		secundaria2027();
		asignarDocente(algebra, t1, docente());
		asignarDocente(algebra, t2, usuario(COORDINADOR));
		bloque(cabecera(t1, estructuraSecundaria), "LUN", 1, algebra, aulaUno);
		bloque(cabecera(t2, estructuraSecundaria), "LUN", 2, algebra, aulaDos);

		pedir("/horarios/docente/horario", DOCENTE).andExpect(status().isOk())
				.andExpect(jsonPath("$.horarios[0].clases", hasSize(1)))
				.andExpect(jsonPath("$.horarios[0].clases[0].seccion").value("1.° T1"));
	}

	@Test
	void sinAnioIdDevuelveElAnioActivoSiTieneClasesYConAnioIdDevuelveEseAnio() throws Exception {
		secundaria2027();
		asignarDocente(algebra, t1, docente());
		bloque(cabecera(t1, estructuraSecundaria), "LUN", 1, algebra, aulaUno);
		claseDe2026(aulaUno);

		pedir("/horarios/docente/horario", DOCENTE).andExpect(status().isOk())
				.andExpect(jsonPath("$.anio.anio").value(2026))
				.andExpect(jsonPath("$.anio.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.horarios[0].clases", hasSize(1)))
				.andExpect(jsonPath("$.horarios[0].clases[0].dia").value("MIE"))
				.andExpect(jsonPath("$.horarios[0].clases[0].seccion").value("1.° A"))
				.andExpect(jsonPath("$.horarios[0].titulo").value("Secundaria"))
				.andExpect(jsonPath("$.horarios[0].jornada.bloques[0].inicio").value("07:30"));

		pedir("/horarios/docente/horario?anioId=" + ANIO_2027, DOCENTE).andExpect(status().isOk())
				.andExpect(jsonPath("$.anio.anio").value(2027))
				.andExpect(jsonPath("$.anio.estado").value("PLANIFICADO"))
				.andExpect(jsonPath("$.horarios[0].clases[0].seccion").value("1.° T1"));
	}

	@Test
	void conAnioIdSinClasesDevuelveLosDatosDelAnioYHorariosVacio() throws Exception {
		secundaria2027();
		asignarDocente(algebra, t1, docente());
		claseDe2026(aulaUno);

		pedir("/horarios/docente/horario?anioId=" + ANIO_2027, DOCENTE).andExpect(status().isOk())
				.andExpect(jsonPath("$.anio.id").value(ANIO_2027.toString()))
				.andExpect(jsonPath("$.anio.anio").value(2027))
				.andExpect(jsonPath("$.anio.estado").value("PLANIFICADO"))
				.andExpect(jsonPath("$.horarios", empty()));
	}

	@Test
	void docenteSinClasesEnNingunAnioRecibeAnioNullYHorariosVacio() throws Exception {
		pedir("/horarios/docente/horario", DOCENTE).andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.horarios", empty()));
	}

	@Test
	void aniosListaSoloLosAniosConClasesDelMasRecienteAlMasAntiguo() throws Exception {
		secundaria2027();
		asignarDocente(algebra, t1, docente());
		bloque(cabecera(t1, estructuraSecundaria), "LUN", 1, algebra, aulaUno);

		pedir("/horarios/docente/anios", DOCENTE).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].anio").value(2027));

		claseDe2026(aulaUno);

		pedir("/horarios/docente/anios", DOCENTE).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id").value(ANIO_2027.toString()))
				.andExpect(jsonPath("$[0].anio").value(2027))
				.andExpect(jsonPath("$[0].estado").value("PLANIFICADO"))
				.andExpect(jsonPath("$[1].id").value(ANIO_2026))
				.andExpect(jsonPath("$[1].anio").value(2026))
				.andExpect(jsonPath("$[1].estado").value("ACTIVO"));
	}

	@Test
	void unUsuarioSoloTutorQueEsTitularPuedeConsultarYVeSusClases() throws Exception {
		secundaria2027();
		String tutor = soloTutor();
		asignarDocente(algebra, t1, usuario(tutor));
		bloque(cabecera(t1, estructuraSecundaria), "JUE", 3, algebra, aulaUno);

		pedir("/horarios/docente/horario", tutor).andExpect(status().isOk())
				.andExpect(jsonPath("$.horarios[0].clases", hasSize(1)))
				.andExpect(jsonPath("$.horarios[0].clases[0].dia").value("JUE"))
				.andExpect(jsonPath("$.horarios[0].clases[0].seccion").value("1.° T1"));
		pedir("/horarios/docente/anios", tutor).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void unTutorQueNoDictaVeElEstadoVacio() throws Exception {
		String tutor = soloTutor();

		pedir("/horarios/docente/horario", tutor).andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.horarios", empty()));
		pedir("/horarios/docente/anios", tutor).andExpect(status().isOk()).andExpect(jsonPath("$", empty()));
	}

	@Test
	void coordinadorYApoderadoRecibenAcceso403EnLosDosEndpoints() throws Exception {
		for (String usuario : new String[]{COORDINADOR, APODERADO}) {
			pedir("/horarios/docente/horario", usuario).andExpect(status().isForbidden());
			pedir("/horarios/docente/anios", usuario).andExpect(status().isForbidden());
		}
	}

	@Test
	void sinSesionResponde401() throws Exception {
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.get("/horarios/docente/horario")).andExpect(status().isUnauthorized());
	}

	@Test
	void conUnAnioQueNoExisteResponde404() throws Exception {
		pedir("/horarios/docente/horario?anioId=" + UUID.randomUUID(), DOCENTE).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	@Test
	void conUnAnioIdQueNoEsUuidResponde400() throws Exception {
		pedir("/horarios/docente/horario?anioId=no-es-un-uuid", DOCENTE).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La solicitud tiene un formato inválido"));
	}
}
