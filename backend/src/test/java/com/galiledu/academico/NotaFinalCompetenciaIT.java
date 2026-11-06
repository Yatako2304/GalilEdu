package com.galiledu.academico;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;

/** Nota final de una competencia entre los cursos del grado: Álgebra y Geometría evalúan la misma competencia. */
class NotaFinalCompetenciaIT extends PruebaIntegracion {
	private static final String ANIO = "30000000-0000-0000-0000-000000002026";
	private static final String MARIA = "10000000-0000-0000-0000-000000000004";
	private static final String MATRICULA = "50000000-0000-0000-0000-000000000004";

	@Autowired
	private JdbcClient jdbc;

	private String docente;
	private String algebra;
	private String geometria;
	private String seccion;
	private String competencia;
	private String periodo3;
	private String periodo4;

	@BeforeEach
	void preparar() throws Exception {
		docente = token(DOCENTE);
		algebra = planDe("Álgebra");
		geometria = planDe("Geometría");
		seccion = jdbc.sql("""
				SELECT id::text FROM configuracion.seccion
				WHERE grado_codigo = 'SEC-1' AND nombre = 'A' AND anio_escolar_id = ?::uuid""").param(ANIO)
				.query(String.class).single();
		competencia = jdbc
				.sql("SELECT competencia_id::text FROM academico.plan_competencia WHERE plan_curso_id = ?::uuid")
				.param(algebra).query(String.class).single();
		periodo3 = periodo(3);
		periodo4 = periodo(4);
		jdbc.sql("UPDATE academico.plan_competencia SET peso_curso = 40 WHERE plan_curso_id = ?::uuid").param(algebra)
				.update();
		jdbc.sql("""
				INSERT INTO academico.plan_competencia (plan_curso_id, competencia_id, peso_curso)
				VALUES (?::uuid, ?::uuid, 60)""").params(geometria, competencia).update();
		jdbc.sql("""
				INSERT INTO personas.persona
				    (id, tipo_documento, numero_documento, apellido_paterno, apellido_materno, nombres,
				     fecha_nacimiento, nacionalidad, sexo, distrito, direccion)
				VALUES ('00000000-0000-0000-0000-000000000104', 'DNI', '71000004', 'García', 'López', 'Ana Lucía',
				        '2013-04-10', 'Peruana', 'FEMENINO', 'Pueblo Libre', 'Jr. Castilla 120')""").update();
		jdbc.sql("""
				INSERT INTO estudiantes.estudiante
				    (id, persona_id, pais_nacimiento, lengua_materna, anio_academico, grado_codigo,
				     forma_ingreso, modo_salida, estado)
				VALUES ('20000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000104',
				        'Perú', 'Castellano', 2026, 'SEC-1', 'Edad normativa', 'SOLO', 'ACTIVO')""").update();
		jdbc.sql("""
				INSERT INTO matricula.matricula
				    (id, estudiante_id, anio_escolar_id, grado_codigo, seccion_id, tipo_estudiante,
				     estado, codigo, confirmado_en, creado_por)
				VALUES (?::uuid, '20000000-0000-0000-0000-000000000004', ?::uuid, 'SEC-1', ?::uuid, 'CONTINUADOR',
				        'MATRICULADO', 'MAT-2026-0003', '2026-02-11 09:00-05',
				        '10000000-0000-0000-0000-000000000002')""").params(MATRICULA, ANIO, seccion).update();
	}

	private String planDe(String curso) {
		return jdbc.sql("""
				SELECT pc.id::text FROM academico.plan_curso pc
				JOIN academico.curso c ON c.id = pc.curso_id
				WHERE c.nombre = ? AND pc.grado_codigo = 'SEC-1' AND pc.anio_escolar_id = ?::uuid""")
				.params(curso, ANIO)
				.query(String.class).single();
	}

	private String periodo(int numero) {
		return jdbc.sql("""
				SELECT id::text FROM configuracion.periodo_academico WHERE anio_escolar_id = ?::uuid AND numero = ?""")
				.params(ANIO, numero).query(String.class).single();
	}

	/** Crea un ítem de la competencia. Una nota null lo deja sin calificar. */
	private String item(String plan, String periodo, String nombre, int peso, Integer nota, String nivel) {
		String id = UUID.randomUUID().toString();
		jdbc.sql("""
				INSERT INTO academico.item_evaluacion
				    (id, plan_curso_id, anio_escolar_id, grado_codigo, seccion_id, periodo_academico_id,
				     nombre, descripcion, tipo, creado_por)
				VALUES (?::uuid, ?::uuid, ?::uuid, 'SEC-1', ?::uuid, ?::uuid, ?, 'Criterio', 'PRACTICA', ?::uuid)""")
				.params(id, plan, ANIO, seccion, periodo, nombre, MARIA).update();
		jdbc.sql("""
				INSERT INTO academico.item_competencia
				    (item_evaluacion_id, plan_curso_id, competencia_id, peso_porcentaje)
				VALUES (?::uuid, ?::uuid, ?::uuid, ?)""").params(id, plan, competencia, peso).update();
		if (nota != null || nivel != null) {
			jdbc.sql("""
					INSERT INTO academico.calificacion_item
					    (item_evaluacion_id, seccion_id, matricula_id, nota_numerica, nivel_logro, registrado_por)
					VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, ?::uuid)""")
					.params(id, seccion, MATRICULA, nota, nivel, MARIA).update();
		}
		return id;
	}

	private void parametros(String escala, String regla, String consolidacion) {
		jdbc.sql("""
				UPDATE configuracion.anio_escolar
				SET escala_evaluacion = ?, regla_nota = ?, consolidacion_periodos = ?
				WHERE id = ?::uuid""").params(escala, regla, consolidacion, ANIO).update();
	}

	private ResultActions consultar() throws Exception {
		return mvc.perform(conToken(get("/calificaciones/docente/cursos/" + algebra + "/secciones/" + seccion
				+ "/competencias/" + competencia + "/nota-final"), docente));
	}

	/** La fila de la estudiante del escenario: la sección puede tener otros matriculados. */
	private static String fila(String campo) {
		return "$.filas[?(@.matriculaId == '" + MATRICULA + "')]." + campo;
	}

	private static org.hamcrest.Matcher<Iterable<? super Object>> es(Object valor) {
		return org.hamcrest.Matchers.hasItem(valor);
	}

	@Test
	void combinaLosCursosConSuPesoDelPlan() throws Exception {
		item(algebra, periodo4, "Examen", 100, 20, null);
		item(geometria, periodo4, "Examen", 100, 10, null);

		consultar().andExpect(status().isOk())
				.andExpect(jsonPath("$.pesosCompletos").value(true))
				.andExpect(jsonPath("$.cursos.length()").value(2))
				.andExpect(jsonPath(fila("cursos." + algebra)).value(es("20")))
				.andExpect(jsonPath(fila("cursos." + geometria)).value(es("10")))
				.andExpect(jsonPath(fila("notaFinal")).value(es("14")))
				.andExpect(jsonPath(fila("estado")).value(es("Aprobado")));
	}

	@Test
	void unExamenDe15PorCientoYOtroDe100NoDescuadranLaNota() throws Exception {
		item(algebra, periodo4, "Examen", 15, 20, null);
		item(geometria, periodo4, "Examen", 100, 10, null);

		consultar().andExpect(jsonPath(fila("cursos." + algebra)).value(es("20")))
				.andExpect(jsonPath(fila("notaFinal")).value(es("14")));
	}

	@Test
	void promediaLosPeriodosDeCadaCursoAntesDeCombinarlos() throws Exception {
		item(algebra, periodo3, "Examen 3", 100, 10, null);
		item(algebra, periodo4, "Examen 4", 100, 20, null);
		item(geometria, periodo4, "Examen 4", 100, 20, null);

		consultar().andExpect(jsonPath(fila("cursos." + algebra)).value(es("15")))
				.andExpect(jsonPath(fila("cursos." + geometria)).value(es("20")))
				.andExpect(jsonPath(fila("notaFinal")).value(es("18")));
	}

	@Test
	void conUltimoPeriodoSoloCuentaElUltimoConNotasDeCadaCurso() throws Exception {
		parametros("VIGESIMAL", "PROMEDIO_PONDERADO", "ULTIMO_PERIODO");
		item(algebra, periodo3, "Examen 3", 100, 10, null);
		item(algebra, periodo4, "Examen 4", 100, 20, null);
		item(geometria, periodo4, "Examen 4", 100, 20, null);

		consultar().andExpect(jsonPath(fila("cursos." + algebra)).value(es("20")))
				.andExpect(jsonPath(fila("notaFinal")).value(es("20")))
				.andExpect(jsonPath(fila("parcial")).value(es(false)));
	}

	@Test
	void unCursoSinNotasNoCuentaYLaNotaQuedaParcial() throws Exception {
		item(algebra, periodo4, "Examen", 100, 18, null);

		consultar().andExpect(jsonPath(fila("cursos." + geometria)).value(es("—")))
				.andExpect(jsonPath(fila("notaFinal")).value(es("18")))
				.andExpect(jsonPath(fila("parcial")).value(es(true)));
	}

	@Test
	void unItemPendienteDejaLaNotaParcial() throws Exception {
		parametros("VIGESIMAL", "PROMEDIO_PONDERADO", "ULTIMO_PERIODO");
		item(algebra, periodo4, "Examen", 50, 20, null);
		item(algebra, periodo4, "Práctica", 50, null, null);
		item(geometria, periodo4, "Examen", 100, 20, null);

		consultar().andExpect(jsonPath(fila("notaFinal")).value(es("20")))
				.andExpect(jsonPath(fila("parcial")).value(es(true)));
	}

	@Test
	void conPesosIncompletosNoCalculaLaNotaFinalPeroMuestraLosCursos() throws Exception {
		jdbc.sql("UPDATE academico.plan_competencia SET peso_curso = NULL WHERE plan_curso_id = ?::uuid")
				.param(geometria).update();
		item(algebra, periodo4, "Examen", 100, 18, null);

		consultar().andExpect(jsonPath("$.pesosCompletos").value(false))
				.andExpect(jsonPath(fila("cursos." + algebra)).value(es("18")))
				.andExpect(jsonPath(fila("notaFinal")).value(es("—")))
				.andExpect(jsonPath(fila("estado")).value(es("Pendiente")))
				.andExpect(jsonPath(fila("parcial")).value(es(false)));
	}

	@Test
	void sinNotasLaFilaQuedaPendiente() throws Exception {
		consultar().andExpect(jsonPath(fila("notaFinal")).value(es("—")))
				.andExpect(jsonPath(fila("estado")).value(es("Pendiente")));
	}

	@Test
	void conPromedioSimpleLosCursosPesanIgualAunqueLosPesosEstenIncompletos() throws Exception {
		parametros("VIGESIMAL", "PROMEDIO_SIMPLE", "PROMEDIO_PERIODOS");
		jdbc.sql("UPDATE academico.plan_competencia SET peso_curso = NULL WHERE plan_curso_id = ?::uuid")
				.param(geometria).update();
		item(algebra, periodo4, "Examen", 100, 20, null);
		item(geometria, periodo4, "Examen", 100, 10, null);

		consultar().andExpect(jsonPath("$.pesosCompletos").value(false))
				.andExpect(jsonPath(fila("notaFinal")).value(es("15")));
	}

	@Test
	void enLaEscalaCualitativaCombinaLosNiveles() throws Exception {
		parametros("CUALITATIVA", "NIVEL_MAS_FRECUENTE", "ULTIMO_PERIODO");
		item(algebra, periodo4, "Examen", 100, null, "AD");
		item(geometria, periodo4, "Examen", 100, null, "C");

		consultar().andExpect(jsonPath("$.escala").value("CUALITATIVA"))
				.andExpect(jsonPath(fila("cursos." + geometria)).value(es("C")))
				.andExpect(jsonPath(fila("notaFinal")).value(es("AD")))
				.andExpect(jsonPath(fila("estado")).value(es("Aprobado")));
	}

	@Test
	void noMuestraUnCursoAsignadoAOtroDocente() throws Exception {
		mvc.perform(conToken(get("/calificaciones/docente/cursos/" + geometria + "/secciones/" + seccion
				+ "/competencias/" + competencia + "/nota-final"), docente)).andExpect(status().isNotFound());
	}

	@Test
	void noMuestraUnaCompetenciaFueraDelPlanDelCurso() throws Exception {
		mvc.perform(conToken(get("/calificaciones/docente/cursos/" + algebra + "/secciones/" + seccion
				+ "/competencias/" + UUID.randomUUID() + "/nota-final"), docente)).andExpect(status().isNotFound());
	}

	@Test
	void unRolAdministrativoNoLaConsulta() throws Exception {
		mvc.perform(conToken(get("/calificaciones/docente/cursos/" + algebra + "/secciones/" + seccion
				+ "/competencias/" + competencia + "/nota-final"), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isForbidden());
	}
}
