package com.galiledu.horarios;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HU-46: POST /horarios/secciones/{id}/cruces con el validador, el conversor y la ocupación reales. Las secciones
 * son de 1.° de secundaria (se muestran como "1.° T1") y el docente es María Fernández Ríos.
 */
class CrucesHorarioIT extends PruebaHorarios {
	private static final String DOCENTE_NOMBRE = "María Fernández Ríos";

	private UUID aulaUno;
	private UUID aulaDos;
	private UUID algebra;
	private UUID algebraPrimaria;
	private UUID docente;

	/** Estructura principal de secundaria (08:00), dos aulas, Álgebra en 1.° de secundaria y el docente. */
	private UUID prepararSecundaria() {
		UUID estructura = estructura("EH-T-27", "SECUNDARIA", true, 6);
		aulas();
		algebra = plan("Álgebra", 5);
		docente = docente();
		return estructura;
	}

	private void aulas() {
		aulaUno = ambiente("TMP-A1", "ACTIVO");
		aulaDos = ambiente("TMP-A2", "ACTIVO");
	}

	/** Un docente distinto del de las pruebas: el Coordinador sirve, porque solo importa que sea otro usuario. */
	private UUID otroDocente() {
		return jdbc.sql("SELECT id FROM seguridad.usuario WHERE nombre_usuario = :u").param("u", COORDINADOR)
				.query(UUID.class).single();
	}

	/** La sección T2 de 1.° de primaria, con Álgebra en su plan. Deja {@code algebraPrimaria} y las aulas listas. */
	private UUID seccionDePrimaria() {
		aulas();
		jdbc.sql("""
				INSERT INTO academico.curso_grado (curso_id, grado_codigo)
				SELECT id, 'PRI-1' FROM academico.curso WHERE nombre = 'Álgebra'""").update();
		UUID seccion = seccion("PRI-1", "T2", aulaDos, null);
		algebraPrimaria = plan("PRI-1", "Álgebra", 3);
		return seccion;
	}

	private ResultActions cruces(UUID seccionId, String usuario, String json) throws Exception {
		return mvc.perform(conToken(post("/horarios/secciones/" + seccionId + "/cruces"), token(usuario), json));
	}

	private ResultActions cruces(UUID seccionId, Celda... celdas) throws Exception {
		return cruces(seccionId, COORDINADOR, cuerpoCruces(celdas));
	}

	/** El cuerpo del POST: el del PUT sin la versión. */
	private static String cuerpoCruces(Celda... celdas) {
		String json = cuerpo(null, celdas);
		return "{" + json.substring(json.indexOf("\"celdas\""));
	}

	@Test
	void sinCrucesResponde200YUnaListaVacia() throws Exception {
		UUID estructura = prepararSecundaria();
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);
		UUID t2 = seccion("SEC-1", "T2", aulaDos, null);
		asignarDocente(algebra, t1, docente);
		asignarDocente(algebra, t2, docente);
		bloque(cabecera(t1, estructura), "MAR", 5, algebra, aulaUno);

		cruces(t2, new Celda("LUN", 1, algebra, aulaDos)).andExpect(status().isOk()).andExpect(jsonPath("$", empty()));
		cruces(t2).andExpect(status().isOk()).andExpect(jsonPath("$", empty()));
	}

	@Test
	void elMismoDocenteEnDosSeccionesAlMismoDiaYBloqueEsUnCruceDeDocente() throws Exception {
		UUID estructura = prepararSecundaria();
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);
		UUID t2 = seccion("SEC-1", "T2", aulaDos, null);
		asignarDocente(algebra, t1, docente);
		asignarDocente(algebra, t2, docente);
		bloque(cabecera(t1, estructura), "LUN", 1, algebra, aulaUno);

		cruces(t2, new Celda("LUN", 1, algebra, aulaDos)).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].tipo").value("DOCENTE"))
				.andExpect(jsonPath("$[0].dia").value("LUN")).andExpect(jsonPath("$[0].numeroBloque").value(1))
				.andExpect(jsonPath("$[0].seccion").value("1.° T1")).andExpect(jsonPath("$[0].curso").value("Álgebra"))
				.andExpect(jsonPath("$[0].mensaje").value(DOCENTE_NOMBRE
						+ " ya dicta Álgebra con 1.° T1 el lunes de 8:00 a 8:45."));
	}

	@Test
	void elMismoAmbienteConDocentesDistintosEsUnCruceDeAmbiente() throws Exception {
		UUID estructura = prepararSecundaria();
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);
		UUID t2 = seccion("SEC-1", "T2", aulaUno, null);
		asignarDocente(algebra, t1, docente);
		asignarDocente(algebra, t2, otroDocente());
		bloque(cabecera(t1, estructura), "LUN", 1, algebra, aulaUno);

		cruces(t2, new Celda("LUN", 1, algebra, aulaUno)).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].tipo").value("AMBIENTE"))
				.andExpect(jsonPath("$[0].seccion").value("1.° T1")).andExpect(jsonPath("$[0].curso").value("Álgebra"))
				.andExpect(jsonPath("$[0].mensaje").value(
						"1.° T1 ya usa Aula TMP-A1 para Álgebra el lunes de 8:00 a 8:45."));
	}

	@Test
	void bloquesDeJornadasDistintasQueSeSuperponenSeCruzan() throws Exception {
		// Secundaria entra a las 08:00 (bloque 2: 08:45 a 09:30) y primaria a las 08:30 (bloque 1: 08:30 a 09:15).
		UUID secundaria = estructura("EH-S-27", "SECUNDARIA", true, 6, "08:00");
		estructura("EH-P-27", "PRIMARIA", true, 6, "08:30");
		UUID primaria = seccionDePrimaria();
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);
		algebra = plan("Álgebra", 5);
		asignarDocente(algebra, t1, docente());
		asignarDocente(algebraPrimaria, primaria, docente());
		bloque(cabecera(t1, secundaria), "LUN", 2, algebra, aulaUno);

		cruces(primaria, new Celda("LUN", 1, algebraPrimaria, aulaDos)).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].tipo").value("DOCENTE"))
				.andExpect(jsonPath("$[0].numeroBloque").value(1))
				.andExpect(jsonPath("$[0].mensaje").value(DOCENTE_NOMBRE
						+ " ya dicta Álgebra con 1.° T1 el lunes de 8:45 a 9:30."));
	}

	@Test
	void bloquesDeJornadasDistintasQueSoloSeTocanNoSeCruzan() throws Exception {
		// El bloque 1 de secundaria termina a las 08:45 y el bloque 1 de primaria empieza a las 08:45.
		UUID secundaria = estructura("EH-S-27", "SECUNDARIA", true, 6, "08:00");
		estructura("EH-P-27", "PRIMARIA", true, 6, "08:45");
		UUID primaria = seccionDePrimaria();
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);
		algebra = plan("Álgebra", 5);
		asignarDocente(algebra, t1, docente());
		asignarDocente(algebraPrimaria, primaria, docente());
		bloque(cabecera(t1, secundaria), "LUN", 1, algebra, aulaUno);

		cruces(primaria, new Celda("LUN", 1, algebraPrimaria, aulaUno)).andExpect(status().isOk())
				.andExpect(jsonPath("$", empty()));
	}

	@Test
	void unBloqueDeOtroAnioEscolarNoCruza() throws Exception {
		prepararSecundaria();
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);
		asignarDocente(algebra, t1, docente);
		// 1.° A de 2026 (del seed): mismo docente y mismo ambiente, lunes de 08:15 a 09:00 con su estructura de 07:30.
		UUID seccion2026 = jdbc.sql("""
				SELECT id FROM configuracion.seccion
				WHERE anio_escolar_id = '30000000-0000-0000-0000-000000002026' AND grado_codigo = 'SEC-1'
				  AND nombre = 'A'""").query(UUID.class).single();
		UUID algebra2026 = jdbc.sql("""
				SELECT pc.id FROM academico.plan_curso pc JOIN academico.curso c ON c.id = pc.curso_id
				WHERE pc.anio_escolar_id = '30000000-0000-0000-0000-000000002026' AND pc.grado_codigo = 'SEC-1'
				  AND c.nombre = 'Álgebra'""").query(UUID.class).single();
		bloque(cabecera(seccion2026, UUID.fromString("31000000-0000-0000-0000-000000000002")), "LUN", 2, algebra2026,
				aulaUno);

		cruces(t1, new Celda("LUN", 1, algebra, aulaUno)).andExpect(status().isOk()).andExpect(jsonPath("$", empty()));
	}

	@Test
	void elMismoParDeBloquesEnElMismoAnioSiCruza() throws Exception {
		// Control de la prueba anterior: mismo docente, mismo ambiente y 08:15 a 09:00 contra 08:00 a 08:45, pero con
		// la otra sección en 2027. La estructura de 07:30 no es la principal, que sigue siendo la de las 08:00.
		prepararSecundaria();
		UUID estructuraDeLas0730 = estructura("EH-N-27", "SECUNDARIA", false, 9, "07:30");
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);
		UUID t2 = seccion("SEC-1", "T2", aulaUno, null);
		asignarDocente(algebra, t1, docente);
		asignarDocente(algebra, t2, docente);
		bloque(cabecera(t2, estructuraDeLas0730), "LUN", 2, algebra, aulaUno);

		cruces(t1, new Celda("LUN", 1, algebra, aulaUno)).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].tipo").value("DOCENTE"))
				.andExpect(jsonPath("$[0].seccion").value("1.° T2")).andExpect(jsonPath("$[0].mensaje").value(
						DOCENTE_NOMBRE
								+ " ya dicta Álgebra con 1.° T2 el lunes de 8:15 a 9:00."))
				.andExpect(jsonPath("$[1].tipo").value("AMBIENTE"));
	}

	@Test
	void lasCeldasQueElConversorOmiteNoHacenFallarYSeDevuelvenLosCrucesDelResto() throws Exception {
		UUID estructura = prepararSecundaria();
		UUID aritmetica = plan("Aritmética", 2);
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);
		UUID t2 = seccion("SEC-1", "T2", aulaDos, null);
		asignarDocente(algebra, t1, docente);
		asignarDocente(aritmetica, t1, docente);
		asignarDocente(algebra, t2, docente);
		// En T2 Aritmética no tiene docente: esa celda se omite aunque chocaría de ambiente con T1. El bloque 9 no
		// existe en una jornada de 6 bloques: también se omite.
		UUID cabecera = cabecera(t1, estructura);
		bloque(cabecera, "LUN", 1, algebra, aulaUno);
		bloque(cabecera, "MAR", 2, aritmetica, aulaUno);

		cruces(t2, new Celda("MAR", 2, aritmetica, aulaUno), new Celda("LUN", 9, algebra, aulaDos),
				new Celda("LUN", 1, algebra, aulaDos)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].tipo").value("DOCENTE")).andExpect(jsonPath("$[0].dia").value("LUN"))
				.andExpect(jsonPath("$[0].numeroBloque").value(1));
	}

	@Test
	void unaSeccionInexistenteResponde404() throws Exception {
		cruces(UUID.randomUUID(), new Celda("LUN", 1, UUID.randomUUID(), UUID.randomUUID()))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("La sección no existe."));
	}

	@Test
	void unaCeldaSinDiaResponde400() throws Exception {
		prepararSecundaria();
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);
		String json = "{\"celdas\":[{\"numeroBloque\":1,\"planCursoId\":\"%s\",\"ambienteId\":\"%s\"}]}"
				.formatted(algebra, aulaUno);

		cruces(t1, COORDINADOR, json).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Elige el día de cada bloque."));
	}

	@Test
	void masDe72CeldasResponde400() throws Exception {
		prepararSecundaria();
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);
		Celda[] celdas = new Celda[73];
		for (int i = 0; i < celdas.length; i++) {
			celdas[i] = new Celda("LUN", i + 1, algebra, aulaUno);
		}

		cruces(t1, celdas).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El horario admite hasta 72 bloques."));
	}

	@Test
	void sinElRolDeCoordinadorResponde403() throws Exception {
		prepararSecundaria();
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);

		cruces(t1, DOCENTE, cuerpoCruces(new Celda("LUN", 1, algebra, aulaUno))).andExpect(status().isForbidden());
	}

	@Test
	void sinEstructuraPrincipalNiHorarioGuardadoResponde409() throws Exception {
		aulaUno = ambiente("TMP-A1", "ACTIVO");
		algebra = plan("Álgebra", 5);
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);
		asignarDocente(algebra, t1, docente());

		cruces(t1, new Celda("LUN", 1, algebra, aulaUno)).andExpect(status().isConflict()).andExpect(jsonPath(
				"$.message")
				.value("Configura la estructura horaria principal de secundaria antes de asignar horarios."));
	}
}
