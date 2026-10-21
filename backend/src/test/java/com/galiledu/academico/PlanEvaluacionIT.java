package com.galiledu.academico;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/**
 * Plan de evaluación (Figma CI-17 y CI-20, HU-56). Datos de prueba: el plan 2026 de 1.° de secundaria tiene 16 cursos
 * y 35 horas; la estructura de secundaria 2026 tiene 45 bloques; 2027 está planificado y sin planes.
 */
class PlanEvaluacionIT extends PruebaIntegracion {
	private static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String ANIO_2027 = "30000000-0000-0000-0000-000000002027";
	private static final String MARIA_FERNANDEZ = "10000000-0000-0000-0000-000000000004";

	@Autowired
	private JdbcClient jdbc;

	private String personal;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	private String id(String sql, Object... params) {
		return jdbc.sql(sql).params(params).query(String.class).single();
	}

	private String idCurso(String nombre) {
		return id("SELECT id::text FROM academico.curso WHERE nombre = ?", nombre);
	}

	private String idCompetencia(String nombre) {
		return id("SELECT id::text FROM academico.competencia WHERE nombre = ?", nombre);
	}

	private String idPlan(String curso) {
		return id("""
				SELECT pc.id::text FROM academico.plan_curso pc JOIN academico.curso c ON c.id = pc.curso_id
				WHERE pc.anio_escolar_id = ?::uuid AND pc.grado_codigo = 'SEC-1' AND c.nombre = ?""", ANIO_2026,
				curso);
	}

	private static String competencia(String id, String escala, String regla) {
		return """
				{"competenciaId":"%s","escala":"%s","reglaNota":"%s","consolidacion":"PROMEDIO_PERIODOS"}"""
				.formatted(id, escala, regla);
	}

	// ---------- Consultas ----------

	@Test
	void elPlanDe1roTraeSusTarjetasYCursos() throws Exception {
		mvc.perform(conToken(get("/planes-evaluacion").param("anioEscolarId", ANIO_2026).param("grado", "SEC-1"),
				personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.grado").value("1.° de secundaria"))
				.andExpect(jsonPath("$.cursos.length()").value(16))
				.andExpect(jsonPath("$.horasAsignadas").value(35))
				.andExpect(jsonPath("$.bloquesDisponibles").value(45))
				.andExpect(jsonPath("$.secciones.length()").value(2))
				.andExpect(jsonPath("$.docentesRequeridos").value(32))
				.andExpect(jsonPath("$.docentesAsignados").value(1))
				.andExpect(jsonPath("$.cursos[0].area").value("Matemática"))
				.andExpect(jsonPath("$.cursos[?(@.curso == 'Comunicación')].escala").value("CUALITATIVA"))
				.andExpect(jsonPath("$.cursos[?(@.curso == 'Física')].escala").value(org.hamcrest.Matchers.contains(
						(Object) null)));
	}

	@Test
	void unPlanVacioNoTieneCursos() throws Exception {
		mvc.perform(conToken(get("/planes-evaluacion").param("anioEscolarId", ANIO_2027).param("grado", "SEC-3"),
				personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estadoAnio").value("PLANIFICADO"))
				.andExpect(jsonPath("$.cursos.length()").value(0));
	}

	@Test
	void elDetalleTraeLasCompetenciasDelAreaYCualesEvalua() throws Exception {
		mvc.perform(conToken(get("/planes-evaluacion/" + idPlan("Aritmética")), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.grado").value("1.° de secundaria"))
				.andExpect(jsonPath("$.anio").value(2026))
				.andExpect(jsonPath("$.competencias.length()").value(4))
				.andExpect(jsonPath("$.competencias[0].seleccionada").value(true))
				.andExpect(jsonPath("$.competencias[1].seleccionada").value(false))
				.andExpect(jsonPath("$.competencias[1].escala").value("VIGESIMAL"));
	}

	@Test
	void losCursosDisponiblesExcluyenLosQueYaEstanEnElPlan() throws Exception {
		mvc.perform(conToken(get("/planes-evaluacion/cursos-disponibles").param("anioEscolarId", ANIO_2026)
				.param("grado", "SEC-1"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(conToken(get("/planes-evaluacion/cursos-disponibles").param("anioEscolarId", ANIO_2027)
				.param("grado", "SEC-3"), personal))
				.andExpect(jsonPath("$.length()").value(16))
				.andExpect(jsonPath("$[?(@.nombre == 'Física')].competencias",
						org.hamcrest.Matchers.contains(org.hamcrest.Matchers.hasSize(3))));
	}

	// ---------- Agregar y configurar ----------

	@Test
	void agregaUnCursoConSusCompetencias() throws Exception {
		mvc.perform(conToken(post("/planes-evaluacion"), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"SEC-3","cursoId":"%s","horasSemanales":2,
				 "competencias":[%s]}""".formatted(ANIO_2027, idCurso("Física"),
				competencia(idCompetencia("Indaga mediante métodos científicos para construir sus conocimientos"),
						"VIGESIMAL", "PROMEDIO_PONDERADO"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.curso").value("Física"))
				.andExpect(jsonPath("$.horasSemanales").value(2))
				.andExpect(jsonPath("$.competencias[0].seleccionada").value(true))
				.andExpect(jsonPath("$.competencias[0].reglaNota").value("PROMEDIO_PONDERADO"));
	}

	@Test
	void rechazaCursosRepetidosDeOtraAreaOQueNoSeDictanEnElGrado() throws Exception {
		mvc.perform(conToken(post("/planes-evaluacion"), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"SEC-1","cursoId":"%s","horasSemanales":2,"competencias":[]}"""
				.formatted(ANIO_2026, idCurso("Álgebra"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Álgebra ya está en el plan de 1.° de secundaria 2026."));
		mvc.perform(conToken(post("/planes-evaluacion"), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"PRI-3","cursoId":"%s","horasSemanales":2,"competencias":[]}"""
				.formatted(ANIO_2027, idCurso("Álgebra"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Álgebra no se dicta en 3.° de primaria. Agrega el grado al curso en el plan curricular."));
		mvc.perform(conToken(post("/planes-evaluacion"), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"SEC-3","cursoId":"%s","horasSemanales":2,"competencias":[%s]}"""
				.formatted(ANIO_2027, idCurso("Álgebra"),
						competencia(idCompetencia("Se comunica oralmente en su lengua materna"), "VIGESIMAL",
								"PROMEDIO_SIMPLE"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La competencia no es del área de Álgebra."));
	}

	@Test
	void laReglaDebeCorresponderALaEscala() throws Exception {
		mvc.perform(conToken(put("/planes-evaluacion/" + idPlan("Aritmética")), personal, """
				{"horasSemanales":2,"competencias":[%s]}""".formatted(
				competencia(idCompetencia("Resuelve problemas de cantidad"), "CUALITATIVA", "PROMEDIO_SIMPLE"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"La agregación de ítems de \"Resuelve problemas de cantidad\" no corresponde a su escala."));
	}

	@Test
	void configuraHorasYCompetencias() throws Exception {
		mvc.perform(conToken(put("/planes-evaluacion/" + idPlan("Aritmética")), personal, """
				{"horasSemanales":3,"competencias":[%s,%s]}""".formatted(
				competencia(idCompetencia("Resuelve problemas de cantidad"), "CUALITATIVA", "ULTIMO_NIVEL"),
				competencia(idCompetencia("Resuelve problemas de regularidad, equivalencia y cambio"), "VIGESIMAL",
						"PROMEDIO_SIMPLE"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.horasSemanales").value(3))
				.andExpect(jsonPath("$.competencias[0].escala").value("CUALITATIVA"))
				.andExpect(jsonPath("$.competencias[1].seleccionada").value(true))
				.andExpect(jsonPath("$.competencias[3].seleccionada").value(false));
		mvc.perform(conToken(get("/planes-evaluacion").param("anioEscolarId", ANIO_2026).param("grado", "SEC-1"),
				personal))
				.andExpect(jsonPath("$.cursos[?(@.curso == 'Aritmética')].escala").value(
						org.hamcrest.Matchers.contains("MIXTA")));
	}

	@Test
	void lasHorasNoSuperanLosBloquesDeLaEstructura() throws Exception {
		// 35 horas; Comunicación pasa de 4 a 15 → 46 > 45.
		mvc.perform(conToken(put("/planes-evaluacion/" + idPlan("Comunicación")), personal, """
				{"horasSemanales":15,"competencias":[]}"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"El grado tendría 46 horas semanales y la estructura horaria solo tiene 45 bloques."));
	}

	@Test
	void quitaUnCursoDelPlan() throws Exception {
		mvc.perform(conToken(delete("/planes-evaluacion/" + idPlan("Computación")), personal))
				.andExpect(status().isNoContent());
		mvc.perform(conToken(get("/planes-evaluacion").param("anioEscolarId", ANIO_2026).param("grado", "SEC-1"),
				personal))
				.andExpect(jsonPath("$.cursos.length()").value(15));
	}

	@Test
	void unAnioCerradoNoSeModifica() throws Exception {
		jdbc.sql("UPDATE configuracion.anio_escolar SET estado = 'CERRADO' WHERE id = ?::uuid").param(ANIO_2026)
				.update();
		mvc.perform(conToken(delete("/planes-evaluacion/" + idPlan("Computación")), personal))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"El año escolar 2026 está cerrado: su plan de evaluación ya no se modifica."));
	}

	// ---------- Docentes ----------

	@Test
	void asignaDocentesPorSeccion() throws Exception {
		String plan = idPlan("Geometría");
		String r = mvc.perform(conToken(get("/planes-evaluacion/" + plan + "/docentes"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].seccion").value("A"))
				.andExpect(jsonPath("$[0].tutor").value("María Fernández Ríos"))
				.andReturn().getResponse().getContentAsString();
		String seccionA = JsonPath.read(r, "$[0].seccionId");
		String seccionB = JsonPath.read(r, "$[1].seccionId");

		mvc.perform(conToken(put("/planes-evaluacion/" + plan + "/docentes"), personal, """
				{"asignaciones":[{"seccionId":"%s","docenteId":"%s"},{"seccionId":"%s","docenteId":null}]}"""
				.formatted(seccionA, MARIA_FERNANDEZ, seccionB)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].docenteId").value(MARIA_FERNANDEZ))
				.andExpect(jsonPath("$[1].docenteId").doesNotExist());
	}

	@Test
	void soloSeAsignanDocentesActivosASeccionesDelGrado() throws Exception {
		String plan = idPlan("Geometría");
		String seccion2do = id("""
				SELECT id::text FROM configuracion.seccion
				WHERE anio_escolar_id = ?::uuid AND grado_codigo = 'SEC-2' AND nombre = 'A'""", ANIO_2026);
		mvc.perform(conToken(put("/planes-evaluacion/" + plan + "/docentes"), personal, """
				{"asignaciones":[{"seccionId":"%s","docenteId":"%s"}]}""".formatted(seccion2do, MARIA_FERNANDEZ)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La sección no es de 1.° de secundaria 2026."));
		String seccionA = id("""
				SELECT id::text FROM configuracion.seccion
				WHERE anio_escolar_id = ?::uuid AND grado_codigo = 'SEC-1' AND nombre = 'A'""", ANIO_2026);
		mvc.perform(conToken(put("/planes-evaluacion/" + plan + "/docentes"), personal, """
				{"asignaciones":[{"seccionId":"%s","docenteId":"10000000-0000-0000-0000-000000000001"}]}"""
				.formatted(seccionA)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Elige un usuario activo con rol Docente."));
	}

	// ---------- Copiar plan ----------

	@Test
	void copiaElPlanAUnGradoVacioSinDocentes() throws Exception {
		mvc.perform(conToken(get("/planes-evaluacion/origenes").param("anioEscolarId", ANIO_2027)
				.param("grado", "SEC-1"), personal))
				.andExpect(jsonPath("$[0].anio").value(2026))
				.andExpect(jsonPath("$[0].cursos").value(16))
				.andExpect(jsonPath("$[0].horas").value(35));

		mvc.perform(conToken(post("/planes-evaluacion/copias"), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"SEC-1","origenAnioEscolarId":"%s","origenGradoCodigo":"SEC-1"}"""
				.formatted(ANIO_2027, ANIO_2026)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cursos.length()").value(16))
				.andExpect(jsonPath("$.horasAsignadas").value(35))
				.andExpect(jsonPath("$.docentesAsignados").value(0));
	}

	@Test
	void soloCopiaAUnPlanVacioYSaltaCursosInactivos() throws Exception {
		mvc.perform(conToken(post("/planes-evaluacion/copias"), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"SEC-1","origenAnioEscolarId":"%s","origenGradoCodigo":"SEC-1"}"""
				.formatted(ANIO_2026, ANIO_2026)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("1.° de secundaria ya tiene cursos en 2026: solo se copia a un plan vacío."));

		jdbc.sql("UPDATE academico.curso SET estado = 'INACTIVO' WHERE nombre = 'Computación'").update();
		mvc.perform(conToken(post("/planes-evaluacion/copias"), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"SEC-2","origenAnioEscolarId":"%s","origenGradoCodigo":"SEC-1"}"""
				.formatted(ANIO_2027, ANIO_2026)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cursos.length()").value(15));
	}

	@Test
	void elCoordinadorTambienGestionaElPlan() throws Exception {
		String coordinador = token(COORDINADOR);
		mvc.perform(conToken(get("/planes-evaluacion").param("anioEscolarId", ANIO_2026).param("grado", "SEC-1"),
				coordinador))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cursos.length()").value(16));
		mvc.perform(conToken(delete("/planes-evaluacion/" + idPlan("Computación")), coordinador))
				.andExpect(status().isNoContent());
	}

	@Test
	void otrosRolesReciben403() throws Exception {
		mvc.perform(conToken(get("/planes-evaluacion/anios"), token(DOCENTE))).andExpect(status().isForbidden());
		mvc.perform(conToken(delete("/planes-evaluacion/" + idPlan("Computación")), token(ADMIN)))
				.andExpect(status().isForbidden());
	}
}
