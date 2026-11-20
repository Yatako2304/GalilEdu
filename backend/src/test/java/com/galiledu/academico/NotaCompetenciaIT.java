package com.galiledu.academico;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * Peso de cada curso en una competencia (plan de evaluación) y nota oficial de la competencia: una por periodo y una
 * final. Datos de prueba: en 1.° de secundaria 2026, "Resuelve problemas de cantidad" la evalúan Geometría (40 %) y
 * Aritmética (60 %); María Fernández dicta los dos en 1.° A. El año usa promedio ponderado y promedio de periodos.
 */
class NotaCompetenciaIT extends PruebaIntegracion {
	private static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String MARIA_FERNANDEZ = "10000000-0000-0000-0000-000000000004";
	private static final String CANTIDAD = "Resuelve problemas de cantidad";

	@Autowired
	private JdbcClient jdbc;

	private String personal;
	private String docente;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
		docente = token(DOCENTE);
	}

	private String id(String sql, Object... params) {
		return jdbc.sql(sql).params(params).query(String.class).single();
	}

	private String competencia(String nombre) {
		return id("SELECT id::text FROM academico.competencia WHERE nombre = ?", nombre);
	}

	private String plan(String curso) {
		return id("""
				SELECT pc.id::text FROM academico.plan_curso pc JOIN academico.curso c ON c.id = pc.curso_id
				WHERE pc.anio_escolar_id = ?::uuid AND pc.grado_codigo = 'SEC-1' AND c.nombre = ?""", ANIO_2026,
				curso);
	}

	private String seccionA() {
		return id("""
				SELECT id::text FROM configuracion.seccion
				WHERE anio_escolar_id = ?::uuid AND grado_codigo = 'SEC-1' AND nombre = 'A'""", ANIO_2026);
	}

	private String periodo(int numero) {
		return id("""
				SELECT id::text FROM configuracion.periodo_academico
				WHERE anio_escolar_id = ?::uuid AND numero = ?""", ANIO_2026, numero);
	}

	private String matriculaDe(String nombres) {
		return id("""
				SELECT m.id::text FROM matricula.matricula m
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				WHERE m.seccion_id = ?::uuid AND p.nombres = ?""", seccionA(), nombres);
	}

	/** Un ítem del curso en el periodo, que apunta solo a la competencia, con la nota de un estudiante. */
	private void itemConNota(String curso, int numeroPeriodo, String competenciaId, String matriculaId, int nota) {
		String item = item(curso, numeroPeriodo, competenciaId);
		jdbc.sql("""
				INSERT INTO academico.calificacion_item
				    (item_evaluacion_id, seccion_id, matricula_id, nota_numerica, registrado_por)
				VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?::uuid)""")
				.params(item, seccionA(), matriculaId, nota, MARIA_FERNANDEZ).update();
	}

	/** Como itemConNota, pero con un nivel de logro (escala cualitativa). */
	private void itemConNivel(String curso, int numeroPeriodo, String competenciaId, String matriculaId,
			String nivel) {
		String item = item(curso, numeroPeriodo, competenciaId);
		jdbc.sql("""
				INSERT INTO academico.calificacion_item
				    (item_evaluacion_id, seccion_id, matricula_id, nivel_logro, registrado_por)
				VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?::uuid)""")
				.params(item, seccionA(), matriculaId, nivel, MARIA_FERNANDEZ).update();
	}

	private String item(String curso, int numeroPeriodo, String competenciaId) {
		String item = id("""
				INSERT INTO academico.item_evaluacion
				    (plan_curso_id, anio_escolar_id, grado_codigo, seccion_id, periodo_academico_id,
				     nombre, descripcion, tipo, creado_por)
				VALUES (?::uuid, ?::uuid, 'SEC-1', ?::uuid, ?::uuid, ?, 'Ítem de la prueba', 'EXAMEN', ?::uuid)
				RETURNING id::text""", plan(curso), ANIO_2026, seccionA(), periodo(numeroPeriodo),
				"Examen " + curso + " " + numeroPeriodo, MARIA_FERNANDEZ);
		jdbc.sql(
				"""
						INSERT INTO academico.item_competencia (item_evaluacion_id, plan_curso_id, competencia_id, peso_porcentaje)
						VALUES (?::uuid, ?::uuid, ?::uuid, 100)""")
				.params(item, plan(curso), competenciaId).update();
		return item;
	}

	// ---------- Plan de evaluación: pesos ----------

	@DisplayName("El plan muestra los cursos de cada competencia con su peso")
	@Test
	void elPlanMuestraLosCursosDeCadaCompetenciaConSuPeso() throws Exception {
		mvc.perform(conToken(get("/planes-evaluacion/pesos-competencias").param("anioEscolarId", ANIO_2026)
				.param("grado", "SEC-1"), personal))
				.andExpect(status().isOk())
				.andExpect(
						jsonPath("$[?(@.competencia == '%s')].cursos.length()".formatted(CANTIDAD)).value(contains(2)))
				.andExpect(jsonPath("$[?(@.competencia == '%s')].completo".formatted(CANTIDAD)).value(contains(true)))
				.andExpect(jsonPath("$[?(@.competencia == '%s')].cursos[?(@.curso == 'Aritmética')].pesoCurso"
						.formatted(CANTIDAD)).value(contains(60.0)))
				.andExpect(jsonPath("$[?(@.competencia == 'Resuelve problemas de forma, movimiento y localización')]"
						+ ".cursos[0].pesoCurso").value(contains(100.0)));
		mvc.perform(conToken(get("/planes-evaluacion").param("anioEscolarId", ANIO_2026).param("grado", "SEC-1"),
				personal))
				.andExpect(jsonPath("$.reglaNota").value("PROMEDIO_PONDERADO"))
				.andExpect(jsonPath("$.pesosIncompletos").value(0));
	}

	@DisplayName("Guarda los pesos de todos los cursos si suman100")
	@Test
	void guardaLosPesosDeTodosLosCursosSiSuman100() throws Exception {
		mvc.perform(conToken(put("/planes-evaluacion/pesos-competencias/" + competencia(CANTIDAD)), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"SEC-1","pesos":[
				 {"planCursoId":"%s","pesoCurso":30},{"planCursoId":"%s","pesoCurso":70}]}"""
				.formatted(ANIO_2026, plan("Geometría"), plan("Aritmética"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.completo").value(true))
				.andExpect(jsonPath("$.cursos[?(@.curso == 'Geometría')].pesoCurso").value(contains(30.0)));
	}

	@DisplayName("Rechaza pesos que no suman100 o que olvidan un curso")
	@Test
	void rechazaPesosQueNoSuman100OQueOlvidanUnCurso() throws Exception {
		String url = "/planes-evaluacion/pesos-competencias/" + competencia(CANTIDAD);
		mvc.perform(conToken(put(url), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"SEC-1","pesos":[
				 {"planCursoId":"%s","pesoCurso":30},{"planCursoId":"%s","pesoCurso":60}]}"""
				.formatted(ANIO_2026, plan("Geometría"), plan("Aritmética"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Los pesos de \"%s\" deben sumar 100 %%; ahora suman 90 %%.".formatted(CANTIDAD)));
		mvc.perform(conToken(put(url), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"SEC-1","pesos":[{"planCursoId":"%s","pesoCurso":100}]}"""
				.formatted(ANIO_2026, plan("Geometría"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Indica el peso de todos los cursos que evalúan \"%s\".".formatted(CANTIDAD)));
		mvc.perform(conToken(put(url), personal, """
				{"anioEscolarId":"%s","gradoCodigo":"SEC-1","pesos":[
				 {"planCursoId":"%s","pesoCurso":50},{"planCursoId":"%s","pesoCurso":50}]}"""
				.formatted(ANIO_2026, plan("Geometría"), plan("Álgebra"))))
				.andExpect(status().isBadRequest());
	}

	@DisplayName("Un curso nuevo en una competencia deja los pesos incompletos hasta ajustarlos")
	@Test
	void unCursoNuevoEnUnaCompetenciaDejaLosPesosIncompletosHastaAjustarlos() throws Exception {
		mvc.perform(conToken(put("/planes-evaluacion/" + plan("Álgebra")), personal, """
				{"horasSemanales":3,"competencias":[{"competenciaId":"%s"},{"competenciaId":"%s"}]}"""
				.formatted(competencia("Resuelve problemas de regularidad, equivalencia y cambio"),
						competencia(CANTIDAD))))
				.andExpect(status().isOk());
		mvc.perform(conToken(get("/planes-evaluacion").param("anioEscolarId", ANIO_2026).param("grado", "SEC-1"),
				personal))
				.andExpect(jsonPath("$.pesosIncompletos").value(1));
		mvc.perform(conToken(get("/planes-evaluacion/pesos-competencias").param("anioEscolarId", ANIO_2026)
				.param("grado", "SEC-1"), personal))
				.andExpect(jsonPath("$[?(@.competencia == '%s')].completo".formatted(CANTIDAD)).value(contains(false)))
				// La otra competencia de Álgebra sigue siendo solo suya: 100 %.
				.andExpect(jsonPath("$[?(@.competencia == 'Resuelve problemas de regularidad, equivalencia y cambio')]"
						+ ".completo").value(contains(true)));
	}

	@DisplayName("Al quitar una competencia de un curso el que queda solo aporta el100")
	@Test
	void alQuitarUnaCompetenciaDeUnCursoElQueQuedaSoloAportaEl100() throws Exception {
		// Geometría deja de evaluar "cantidad" (sus ítems de la demo apuntan a ella; se quitan para poder hacerlo).
		jdbc.sql("""
				DELETE FROM academico.item_evaluacion WHERE plan_curso_id = ?::uuid""").param(plan("Geometría"))
				.update();
		mvc.perform(conToken(put("/planes-evaluacion/" + plan("Geometría")), personal, """
				{"horasSemanales":2,"competencias":[{"competenciaId":"%s"}]}"""
				.formatted(competencia("Resuelve problemas de forma, movimiento y localización"))))
				.andExpect(status().isOk());
		mvc.perform(conToken(get("/planes-evaluacion/pesos-competencias").param("anioEscolarId", ANIO_2026)
				.param("grado", "SEC-1"), personal))
				.andExpect(jsonPath("$[?(@.competencia == '%s')].cursos[0].pesoCurso".formatted(CANTIDAD))
						.value(contains(100.0)));
	}

	@DisplayName("El docente no edita los pesos")
	@Test
	void elDocenteNoEditaLosPesos() throws Exception {
		mvc.perform(conToken(get("/planes-evaluacion/pesos-competencias").param("anioEscolarId", ANIO_2026)
				.param("grado", "SEC-1"), docente))
				.andExpect(status().isForbidden());
	}

	// ---------- Nota de la competencia ----------

	@DisplayName("La nota de la competencia combina los cursos con su peso")
	@Test
	void laNotaDeLaCompetenciaCombinaLosCursosConSuPeso() throws Exception {
		String cantidad = competencia(CANTIDAD);
		String ana = matriculaDe("Ana Lucía");
		itemConNota("Geometría", 3, cantidad, ana, 16);
		itemConNota("Aritmética", 3, cantidad, ana, 12);
		itemConNota("Geometría", 4, cantidad, ana, 18);
		itemConNota("Aritmética", 4, cantidad, ana, 14);

		String filaAna = "$.filas[?(@.matriculaId == '%s')].competencias[0]".formatted(ana);
		mvc.perform(conToken(get("/calificaciones/docente/cursos/%s/secciones/%s/notas-competencia"
				.formatted(plan("Geometría"), seccionA())).param("competenciaIds", cantidad), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reglaNota").value("PROMEDIO_PONDERADO"))
				.andExpect(jsonPath("$.periodos.length()").value(4))
				.andExpect(jsonPath("$.competencias[0].competencia").value(CANTIDAD))
				.andExpect(jsonPath("$.competencias[0].pesosCompletos").value(true))
				.andExpect(jsonPath("$.competencias[0].cursos.length()").value(2))
				// III bimestre: 16 × 40 % + 12 × 60 % = 13,6. IV: 18 × 40 % + 14 × 60 % = 15,6.
				.andExpect(jsonPath(filaAna + ".periodos[2].nota").value(contains("13.6")))
				.andExpect(jsonPath(filaAna + ".periodos[2].logro").value(contains("Logro esperado")))
				.andExpect(jsonPath(filaAna + ".periodos[3].nota").value(contains("15.6")))
				.andExpect(jsonPath(filaAna + ".periodos[3].aportes[*].nota").value(hasItem("18")))
				.andExpect(jsonPath(filaAna + ".periodos[0].nota").value(contains("—")))
				// Final: promedio de los periodos con nota. El I y el II bimestre no tienen ítems: no la dejan pendiente.
				.andExpect(jsonPath(filaAna + ".notaFinal.nota").value(contains("14.6")))
				// 14,6 → 15 → A (14–17) en la tabla vigente; el III bimestre 13,6 → 14 también es A.
				.andExpect(jsonPath(filaAna + ".notaFinal.nivel").value(contains("A")))
				.andExpect(jsonPath(filaAna + ".periodos[2].nivel").value(contains("A")))
				.andExpect(jsonPath(filaAna + ".periodos[0].nivel").value(contains((Object) null)))
				.andExpect(jsonPath(filaAna + ".notaFinal.pendiente").value(contains(false)))
				// Diego no tiene las notas que Ana sí: su nota oficial espera esos registros.
				.andExpect(jsonPath("$.filas[?(@.matriculaId == '%s')].competencias[0].periodos[3].pendiente"
						.formatted(matriculaDe("Diego José"))).value(contains(true)))
				.andExpect(jsonPath("$.filas[?(@.matriculaId == '%s')].competencias[0].periodos[3].nota"
						.formatted(matriculaDe("Diego José"))).value(contains("—")));

		// El PDF del IV bimestre termina con la nota de la competencia de ese periodo, no con la del año.
		byte[] pdf = mvc.perform(conToken(get("/calificaciones/docente/reportes/exportar-pdf")
				.param("tipo", "COMPETENCIA").param("planCursoId", plan("Geometría"))
				.param("seccionId", seccionA()).param("periodoId", periodo(4)).param("competenciaIds", cantidad),
				docente)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
		try (var documento = org.apache.pdfbox.Loader.loadPDF(pdf)) {
			org.assertj.core.api.Assertions.assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(documento))
					.contains("Nota de la competencia en el periodo", "Promedio del", "A · 15,6/20",
							"Generado el")
					.doesNotContain("Nota final", "13,6/20", "14,6/20");
		}
	}

	@DisplayName("El resumen del año muestra el aporte final del curso y la nota de la competencia")
	@Test
	void elResumenDelAnoMuestraElAporteFinalDelCursoYLaNotaDeLaCompetencia() throws Exception {
		String cantidad = competencia(CANTIDAD);
		String ana = matriculaDe("Ana Lucía");
		itemConNota("Geometría", 3, cantidad, ana, 16);
		itemConNota("Aritmética", 3, cantidad, ana, 12);
		itemConNota("Geometría", 4, cantidad, ana, 18);
		itemConNota("Aritmética", 4, cantidad, ana, 14);

		String valores = "$.filas[?(@.matriculaId == '%s')].valores['%s:%s']";
		mvc.perform(conToken(get("/calificaciones/docente/cursos/%s/secciones/%s/resumen-anual"
				.formatted(plan("Geometría"), seccionA())).param("competenciaIds", cantidad), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columnas.length()").value(10))
				.andExpect(jsonPath("$.columnas[4].titulo").value("Aporte final"))
				.andExpect(jsonPath("$.columnas[4].competencia").value("Aporte de Geometría · " + CANTIDAD))
				.andExpect(jsonPath("$.columnas[9].titulo").value("Nota final"))
				// Geometría aporta 16 y 18: su aporte final es 17. La competencia: 13,6 y 15,6 → 14,6.
				.andExpect(jsonPath(valores.formatted(ana, cantidad, "aporte:" + periodo(3))).value(contains("16")))
				.andExpect(jsonPath(valores.formatted(ana, cantidad, "aporte:final")).value(contains("17")))
				.andExpect(jsonPath(valores.formatted(ana, cantidad, "aporte:final_estado"))
						.value(contains("A · Logro esperado")))
				.andExpect(jsonPath(valores.formatted(ana, cantidad, "oficial:" + periodo(3))).value(contains("13.6")))
				.andExpect(jsonPath(valores.formatted(ana, cantidad, "oficial:final")).value(contains("14.6")));

		byte[] pdf = mvc.perform(conToken(get("/calificaciones/docente/cursos/%s/secciones/%s/resumen-anual/pdf"
				.formatted(plan("Geometría"), seccionA())).param("competenciaIds", cantidad), docente))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
		try (var documento = org.apache.pdfbox.Loader.loadPDF(pdf)) {
			org.assertj.core.api.Assertions.assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(documento))
					.contains("Resumen del año", "Aporte final", "Nota final", "17/20", "14,6/20",
							"no es el promedio de los aportes finales", "Generado el");
		}
	}

	@DisplayName("En escala de letras cada curso vota con su peso")
	@Test
	void enEscalaDeLetrasCadaCursoVotaConSuPeso() throws Exception {
		jdbc.sql("""
				UPDATE configuracion.anio_escolar
				SET escala_evaluacion = 'CUALITATIVA', regla_nota = 'NIVEL_MAS_FRECUENTE'
				WHERE id = ?::uuid""").param(ANIO_2026).update();
		String cantidad = competencia(CANTIDAD);
		String ana = matriculaDe("Ana Lucía");
		// Aritmética (60 %) da B y Geometría (40 %) da AD: gana B por peso, no AD por ser mayor.
		itemConNivel("Aritmética", 4, cantidad, ana, "B");
		itemConNivel("Geometría", 4, cantidad, ana, "AD");

		String filaAna = "$.filas[?(@.matriculaId == '%s')].competencias[0]".formatted(ana);
		mvc.perform(conToken(get("/calificaciones/docente/cursos/%s/secciones/%s/notas-competencia"
				.formatted(plan("Geometría"), seccionA())).param("competenciaIds", cantidad), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.escala").value("CUALITATIVA"))
				.andExpect(jsonPath("$.competencias[0].usaPesos").value(true))
				.andExpect(jsonPath(filaAna + ".periodos[3].nota").value(contains("B")))
				.andExpect(jsonPath(filaAna + ".periodos[3].nivel").value(contains("B")))
				.andExpect(jsonPath(filaAna + ".periodos[3].logro").value(contains("En proceso")))
				.andExpect(jsonPath(filaAna + ".notaFinal.nivel").value(contains("B")));
	}

	@DisplayName("Con pesos incompletos no se calcula la nota de la competencia")
	@Test
	void conPesosIncompletosNoSeCalculaLaNotaDeLaCompetencia() throws Exception {
		String cantidad = competencia(CANTIDAD);
		String ana = matriculaDe("Ana Lucía");
		itemConNota("Geometría", 4, cantidad, ana, 18);
		jdbc.sql("UPDATE academico.plan_competencia SET peso_curso = NULL WHERE plan_curso_id = ?::uuid")
				.param(plan("Aritmética")).update();

		mvc.perform(conToken(get("/calificaciones/docente/cursos/%s/secciones/%s/notas-competencia"
				.formatted(plan("Geometría"), seccionA())).param("competenciaIds", cantidad), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.competencias[0].pesosCompletos").value(false))
				.andExpect(jsonPath("$.filas[?(@.matriculaId == '%s')].competencias[0].periodos[3].nota"
						.formatted(ana)).value(contains("—")));
	}

	@DisplayName("El docente solo ve las competencias de su curso")
	@Test
	void elDocenteSoloVeLasCompetenciasDeSuCurso() throws Exception {
		mvc.perform(conToken(get("/calificaciones/docente/cursos/%s/secciones/%s/notas-competencia"
				.formatted(plan("Álgebra"), seccionA())).param("competenciaIds", competencia(CANTIDAD)), docente))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Una competencia no pertenece al plan del curso."));
		mvc.perform(conToken(get("/calificaciones/docente/cursos/%s/secciones/%s/notas-competencia"
				.formatted(plan("Comunicación"), seccionA())), docente))
				.andExpect(status().isNotFound());
	}
}
