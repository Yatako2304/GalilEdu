package com.galiledu.academico;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
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
import org.springframework.test.web.servlet.ResultActions;

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
	private static final String REGULARIDAD = "Resuelve problemas de regularidad, equivalencia y cambio";

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

	private static String competencia(String id) {
		return """
				{"competenciaId":"%s"}""".formatted(id);
	}

	private void crearItemDeAlgebra() {
		String plan = idPlan("Álgebra");
		String competencia = idCompetencia("Resuelve problemas de regularidad, equivalencia y cambio");
		jdbc.sql("""
				INSERT INTO academico.item_evaluacion
				    (id, plan_curso_id, anio_escolar_id, grado_codigo, seccion_id, periodo_academico_id,
				     nombre, descripcion, tipo, creado_por)
				SELECT '80000000-0000-0000-0000-000000000001', ?::uuid, ?::uuid, 'SEC-1', s.id, pa.id,
				       'Práctica 1', 'Operaciones algebraicas', 'PRACTICA', ?::uuid
				FROM configuracion.seccion s
				JOIN configuracion.periodo_academico pa ON pa.anio_escolar_id = s.anio_escolar_id AND pa.numero = 4
				WHERE s.anio_escolar_id = ?::uuid AND s.grado_codigo = 'SEC-1' AND s.nombre = 'A'""")
				.params(plan, ANIO_2026, MARIA_FERNANDEZ, ANIO_2026).update();
		jdbc.sql("""
				INSERT INTO academico.item_competencia
				    (item_evaluacion_id, plan_curso_id, competencia_id, peso_porcentaje)
				VALUES ('80000000-0000-0000-0000-000000000001', ?::uuid, ?::uuid, 100)""")
				.params(plan, competencia).update();
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
				.andExpect(jsonPath("$.cursos[?(@.curso == 'Comunicación')].escala").value("VIGESIMAL"))
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
				.andExpect(jsonPath("$.competencias[1].seleccionada").value(false));
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
				competencia(idCompetencia("Indaga mediante métodos científicos para construir sus conocimientos")))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.curso").value("Física"))
				.andExpect(jsonPath("$.horasSemanales").value(2))
				.andExpect(jsonPath("$.competencias[0].seleccionada").value(true));
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
						competencia(idCompetencia("Se comunica oralmente en su lengua materna")))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La competencia no es del área de Álgebra."));
	}

	@Test
	void configuraHorasYCompetencias() throws Exception {
		mvc.perform(conToken(put("/planes-evaluacion/" + idPlan("Aritmética")), personal, """
				{"horasSemanales":3,"competencias":[%s,%s]}""".formatted(
				competencia(idCompetencia("Resuelve problemas de cantidad")),
				competencia(idCompetencia("Resuelve problemas de regularidad, equivalencia y cambio")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.horasSemanales").value(3))
				.andExpect(jsonPath("$.competencias[1].seleccionada").value(true))
				.andExpect(jsonPath("$.competencias[3].seleccionada").value(false));
		mvc.perform(conToken(get("/planes-evaluacion").param("anioEscolarId", ANIO_2026).param("grado", "SEC-1"),
				personal))
				.andExpect(jsonPath("$.cursos[?(@.curso == 'Aritmética')].escala").value(
						org.hamcrest.Matchers.contains("VIGESIMAL")));
	}

	@Test
	void agregaUnaCompetenciaAAlgebraSinEliminarLaQueYaUsaUnItem() throws Exception {
		crearItemDeAlgebra();
		String plan = idPlan("Álgebra");
		String cantidad = idCompetencia("Resuelve problemas de cantidad");
		String regularidad = idCompetencia("Resuelve problemas de regularidad, equivalencia y cambio");

		mvc.perform(conToken(put("/planes-evaluacion/" + plan), personal, """
				{"horasSemanales":3,"competencias":[%s,%s]}"""
				.formatted(competencia(cantidad), competencia(regularidad))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.competencias[?(@.competenciaId == '%s')].seleccionada".formatted(cantidad))
						.value(org.hamcrest.Matchers.contains(true)))
				.andExpect(jsonPath("$.competencias[?(@.competenciaId == '%s')].seleccionada".formatted(regularidad))
						.value(org.hamcrest.Matchers.contains(true)));
	}

	@Test
	void noQuitaUnaCompetenciaQueYaTieneItemsDeEvaluacion() throws Exception {
		crearItemDeAlgebra();
		String plan = idPlan("Álgebra");
		String cantidad = idCompetencia("Resuelve problemas de cantidad");

		mvc.perform(conToken(put("/planes-evaluacion/" + plan), personal, """
				{"horasSemanales":3,"competencias":[%s]}""".formatted(competencia(cantidad))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"No se puede quitar la competencia \"Resuelve problemas de regularidad, equivalencia y cambio\" porque ya tiene ítems de evaluación."));
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

	// ---------- Peso de cada curso en la competencia ----------

	private static String conPeso(String competenciaId, String peso) {
		return """
				{"competenciaId":"%s","pesoCurso":%s}""".formatted(competenciaId, peso);
	}

	private ResultActions configurar(String curso, int horas, String competencias) throws Exception {
		return mvc.perform(conToken(put("/planes-evaluacion/" + idPlan(curso)), personal, """
				{"horasSemanales":%d,"competencias":[%s]}""".formatted(horas, competencias)));
	}

	private ResultActions pesos(String anio) throws Exception {
		return mvc.perform(conToken(get("/planes-evaluacion/pesos-competencias").param("anioEscolarId", anio)
				.param("grado", "SEC-1"), personal));
	}

	private static String deRegularidad(String campo) {
		return "$[?(@.competencia == '%s')].%s".formatted(REGULARIDAD, campo);
	}

	@Test
	void elDetalleTraeElPesoDelCursoEnCadaCompetencia() throws Exception {
		String regularidad = idCompetencia(REGULARIDAD);
		mvc.perform(conToken(get("/planes-evaluacion/" + idPlan("Álgebra")), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.competencias[?(@.competenciaId == '%s')].pesoCurso".formatted(regularidad))
						.value(contains(100.0)));
	}

	@Test
	void losPesosDeUnaCompetenciaNoSuperan100EntreLosCursosDelGrado() throws Exception {
		String regularidad = idCompetencia(REGULARIDAD);

		configurar("Geometría", 2, conPeso(regularidad, "10"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Los pesos de la competencia \"%s\" no pueden superar 100 %% entre los cursos de 1.° de secundaria. Disponible para este curso: 0 %%."
								.formatted(REGULARIDAD)));

		configurar("Álgebra", 3, conPeso(regularidad, "60")).andExpect(status().isOk());
		configurar("Geometría", 2, conPeso(regularidad, "40.5"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", endsWith("Disponible para este curso: 40 %.")));
		configurar("Geometría", 2, conPeso(regularidad, "40")).andExpect(status().isOk());

		pesos(ANIO_2026).andExpect(status().isOk())
				.andExpect(jsonPath(deRegularidad("total")).value(contains(100.0)))
				.andExpect(jsonPath(deRegularidad("completo")).value(contains(true)))
				.andExpect(jsonPath(deRegularidad("cursos[*].curso")).value(contains("Álgebra", "Geometría")));
	}

	@Test
	void editarNoCuentaDosVecesElPesoDelPropioCurso() throws Exception {
		configurar("Álgebra", 3, conPeso(idCompetencia(REGULARIDAD), "100")).andExpect(status().isOk());
	}

	@Test
	void unCursoSinPesoDejaLaCompetenciaIncompletaAunqueSume100() throws Exception {
		configurar("Geometría", 2, competencia(idCompetencia(REGULARIDAD))).andExpect(status().isOk());

		pesos(ANIO_2026)
				.andExpect(jsonPath(deRegularidad("total")).value(contains(100.0)))
				.andExpect(jsonPath(deRegularidad("completo")).value(contains(false)));
	}

	@Test
	void cambiaElPesoDeUnaCompetenciaQueYaTieneItems() throws Exception {
		crearItemDeAlgebra();
		String regularidad = idCompetencia(REGULARIDAD);
		configurar("Álgebra", 3, conPeso(regularidad, "75"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.competencias[?(@.competenciaId == '%s')].pesoCurso".formatted(regularidad))
						.value(contains(75.0)));
	}

	@Test
	void rechazaPesosFueraDeRango() throws Exception {
		String regularidad = idCompetencia(REGULARIDAD);
		configurar("Álgebra", 3, conPeso(regularidad, "0")).andExpect(status().isBadRequest());
		configurar("Álgebra", 3, conPeso(regularidad, "100.01")).andExpect(status().isBadRequest());
		configurar("Álgebra", 3, conPeso(regularidad, "10.123")).andExpect(status().isBadRequest());
	}

	@Test
	void agregaUnCursoConElPesoDeSuCompetencia() throws Exception {
		String indaga = idCompetencia("Indaga mediante métodos científicos para construir sus conocimientos");
		mvc.perform(conToken(post("/planes-evaluacion"), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"SEC-3","cursoId":"%s","horasSemanales":2,"competencias":[%s]}"""
				.formatted(ANIO_2027, idCurso("Física"), conPeso(indaga, "55.5"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.competencias[?(@.competenciaId == '%s')].pesoCurso".formatted(indaga))
						.value(contains(55.5)));
	}

	@Test
	void copiarElPlanConservaLosPesos() throws Exception {
		configurar("Álgebra", 3, conPeso(idCompetencia(REGULARIDAD), "60")).andExpect(status().isOk());
		mvc.perform(conToken(post("/planes-evaluacion/copias"), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"SEC-1","origenAnioEscolarId":"%s","origenGradoCodigo":"SEC-1"}"""
				.formatted(ANIO_2027, ANIO_2026))).andExpect(status().isCreated());

		pesos(ANIO_2027)
				.andExpect(jsonPath(deRegularidad("total")).value(contains(60.0)))
				.andExpect(jsonPath(deRegularidad("completo")).value(contains(false)));
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
		mvc.perform(conToken(get("/planes-evaluacion/pesos-competencias").param("anioEscolarId", ANIO_2026)
				.param("grado", "SEC-1"), token(DOCENTE))).andExpect(status().isForbidden());
		mvc.perform(conToken(delete("/planes-evaluacion/" + idPlan("Computación")), token(ADMIN)))
				.andExpect(status().isForbidden());
	}
}
