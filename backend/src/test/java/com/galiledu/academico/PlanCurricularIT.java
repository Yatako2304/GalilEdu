package com.galiledu.academico;

import static org.hamcrest.Matchers.containsString;
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

/** Plan curricular (Figma CI-14): áreas (HU-52), competencias (HU-53), cursos (HU-54) y permisos. */
class PlanCurricularIT extends PruebaIntegracion {
	@Autowired
	private JdbcClient jdbc;

	private String personal;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	private String idArea(String nombre) {
		return jdbc.sql("SELECT id::text FROM academico.area WHERE nombre = ?").param(nombre).query(String.class)
				.single();
	}

	private String idCurso(String nombre) {
		return jdbc.sql("SELECT id::text FROM academico.curso WHERE nombre = ?").param(nombre).query(String.class)
				.single();
	}

	// ---------- Consultas ----------

	@Test
	void resumenCuentaActivosDeLosRegistrados() throws Exception {
		mvc.perform(conToken(get("/areas/resumen"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.areasRegistradas").value(12))
				.andExpect(jsonPath("$.areasActivas").value(11))
				.andExpect(jsonPath("$.cursosRegistrados").value(16))
				.andExpect(jsonPath("$.anioActivo").value(2026));
	}

	@Test
	void listaLasAreasEnOrdenConSusConteos() throws Exception {
		mvc.perform(conToken(get("/areas"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(12))
				.andExpect(jsonPath("$[0].nombre").value("Matemática"))
				.andExpect(jsonPath("$[0].competencias").value(4))
				.andExpect(jsonPath("$[0].cursos").value(3))
				.andExpect(jsonPath("$[11].estado").value("INACTIVA"));
	}

	@Test
	void detalleTraeCompetenciasYCursosConSusGrados() throws Exception {
		mvc.perform(conToken(get("/areas/" + idArea("Matemática")), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.competencias.length()").value(4))
				.andExpect(jsonPath("$.competencias[0].nombre").value("Resuelve problemas de cantidad"))
				.andExpect(jsonPath("$.cursos.length()").value(3))
				.andExpect(jsonPath("$.cursos[0].nombre").value("Álgebra"))
				.andExpect(jsonPath("$.cursos[0].grados[0]").value("SEC-1"))
				.andExpect(jsonPath("$.cursos[0].grados.length()").value(5));
	}

	// ---------- Áreas (HU-52) ----------

	@Test
	void creaAreaAlFinalYSinDescripcion() throws Exception {
		mvc.perform(conToken(post("/areas"), personal, """
				{"nombre":"  Tutoría  ","descripcion":"  "}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nombre").value("Tutoría"))
				.andExpect(jsonPath("$.descripcion").doesNotExist())
				.andExpect(jsonPath("$.estado").value("ACTIVA"));
		mvc.perform(conToken(get("/areas"), personal))
				.andExpect(jsonPath("$[12].nombre").value("Tutoría"));
	}

	@Test
	void rechazaAreaConNombreRepetidoSinImportarMayusculas() throws Exception {
		mvc.perform(conToken(post("/areas"), personal, """
				{"nombre":"MATEMÁTICA"}"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ya existe un área con el nombre \"MATEMÁTICA\"."));
	}

	@Test
	void exigeNombreDelArea() throws Exception {
		mvc.perform(conToken(post("/areas"), personal, """
				{"nombre":" "}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa el nombre del área."));
	}

	@Test
	void editaAreaPeroNoConElNombreDeOtra() throws Exception {
		String id = idArea("Comunicación");
		mvc.perform(conToken(put("/areas/" + id), personal, """
				{"nombre":"Comunicación","descripcion":"Lee, escribe y se expresa oralmente."}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.descripcion").value("Lee, escribe y se expresa oralmente."));
		mvc.perform(conToken(put("/areas/" + id), personal, """
				{"nombre":"Matemática"}"""))
				.andExpect(status().isConflict());
	}

	@Test
	void desactivaYReactivaArea() throws Exception {
		String id = idArea("Arte y Cultura");
		mvc.perform(conToken(post("/areas/" + id + "/desactivar"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INACTIVA"));
		mvc.perform(conToken(get("/areas/resumen"), personal))
				.andExpect(jsonPath("$.areasActivas").value(10));
		mvc.perform(conToken(post("/areas/" + id + "/reactivar"), personal))
				.andExpect(jsonPath("$.estado").value("ACTIVA"));
	}

	@Test
	void areaInexistenteDa404() throws Exception {
		mvc.perform(conToken(get("/areas/00000000-0000-0000-0000-000000000000"), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El área no existe."));
	}

	// ---------- Competencias (HU-53) ----------

	@Test
	void agregaCompetenciaAlFinalDelArea() throws Exception {
		String area = idArea("Matemática");
		mvc.perform(conToken(post("/areas/" + area + "/competencias"), personal, """
				{"nombre":"Argumenta afirmaciones matemáticas","descripcion":"Justifica conclusiones."}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orden").value(5))
				.andExpect(jsonPath("$.estado").value("ACTIVA"));
	}

	@Test
	void elNombreDeLaCompetenciaNoSeRepiteEnElArea() throws Exception {
		mvc.perform(conToken(post("/areas/" + idArea("Matemática") + "/competencias"), personal, """
				{"nombre":"resuelve problemas de cantidad"}"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("ya tiene la competencia")));
		// En otra área sí se puede usar el mismo nombre.
		mvc.perform(conToken(post("/areas/" + idArea("Comunicación") + "/competencias"), personal, """
				{"nombre":"Resuelve problemas de cantidad"}"""))
				.andExpect(status().isCreated());
	}

	@Test
	void noAgregaCompetenciasNiCursosAUnAreaInactiva() throws Exception {
		String area = idArea("Formación complementaria");
		mvc.perform(conToken(post("/areas/" + area + "/competencias"), personal, """
				{"nombre":"Nueva"}"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"El área Formación complementaria está inactiva. Reactívala para agregarle competencias."));
		mvc.perform(conToken(post("/areas/" + area + "/cursos"), personal, """
				{"nombre":"Oratoria","grados":["SEC-1"]}"""))
				.andExpect(status().isConflict());
	}

	@Test
	void editaYDesactivaCompetencia() throws Exception {
		String r = mvc.perform(conToken(get("/areas/" + idArea("Matemática")), personal))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(r, "$.competencias[3].id");
		mvc.perform(conToken(put("/competencias/" + id), personal, """
				{"nombre":"Resuelve problemas de gestión de datos","descripcion":null}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Resuelve problemas de gestión de datos"));
		mvc.perform(conToken(post("/competencias/" + id + "/desactivar"), personal))
				.andExpect(jsonPath("$.estado").value("INACTIVA"));
		mvc.perform(conToken(post("/competencias/" + id + "/reactivar"), personal))
				.andExpect(jsonPath("$.estado").value("ACTIVA"));
	}

	// ---------- Cursos (HU-54) ----------

	@Test
	void creaCursoConSusGradosEnOrdenDelCatalogo() throws Exception {
		mvc.perform(conToken(post("/areas/" + idArea("Matemática") + "/cursos"), personal, """
				{"nombre":"Razonamiento matemático","grados":["SEC-2","PRI-6","SEC-1"]}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.grados[0]").value("PRI-6"))
				.andExpect(jsonPath("$.grados[1]").value("SEC-1"))
				.andExpect(jsonPath("$.grados[2]").value("SEC-2"));
	}

	@Test
	void validaNombreYGradosDelCurso() throws Exception {
		String area = idArea("Matemática");
		mvc.perform(conToken(post("/areas/" + area + "/cursos"), personal, """
				{"nombre":"álgebra","grados":["SEC-1"]}"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ya existe un curso con el nombre \"álgebra\"."));
		mvc.perform(conToken(post("/areas/" + area + "/cursos"), personal, """
				{"nombre":"Estadística","grados":[]}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Elige al menos un grado."));
		mvc.perform(conToken(post("/areas/" + area + "/cursos"), personal, """
				{"nombre":"Estadística","grados":["INI-3"]}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El grado \"INI-3\" no existe."));
	}

	@Test
	void noQuitaUnGradoEnElQueElCursoYaEstaPlanificado() throws Exception {
		// Álgebra está en el plan 2026 de 1.° de secundaria (datos de prueba).
		mvc.perform(conToken(put("/cursos/" + idCurso("Álgebra")), personal, """
				{"nombre":"Álgebra","grados":["SEC-2","SEC-3"]}"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"No puedes quitar 1.° de secundaria: el curso ya está en su plan de evaluación."));
		mvc.perform(conToken(put("/cursos/" + idCurso("Álgebra")), personal, """
				{"nombre":"Álgebra","grados":["SEC-1","SEC-2"]}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.grados.length()").value(2));
	}

	@Test
	void desactivaYReactivaCurso() throws Exception {
		String id = idCurso("Computación");
		mvc.perform(conToken(post("/cursos/" + id + "/desactivar"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INACTIVO"));
		mvc.perform(conToken(post("/cursos/" + id + "/reactivar"), personal))
				.andExpect(jsonPath("$.estado").value("ACTIVO"));
	}

	// ---------- Permisos ----------

	@Test
	void otrosRolesReciben403() throws Exception {
		String admin = token(ADMIN);
		String docente = token(DOCENTE);
		mvc.perform(conToken(get("/areas"), admin)).andExpect(status().isForbidden());
		mvc.perform(conToken(post("/areas"), docente, """
				{"nombre":"Otra"}""")).andExpect(status().isForbidden());
		mvc.perform(conToken(post("/cursos/" + idCurso("Álgebra") + "/desactivar"), docente))
				.andExpect(status().isForbidden());
	}

	@Test
	void sinSesionDa401() throws Exception {
		mvc.perform(get("/areas")).andExpect(status().isUnauthorized());
	}
}
