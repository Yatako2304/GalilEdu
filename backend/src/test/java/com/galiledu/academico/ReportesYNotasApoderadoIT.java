package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * HU-63 (por curso y grado), HU-64 (institucional) y HU-69 (apoderado). En el III bimestre de 2026, «Resuelve
 * problemas de cantidad» (Aritmética 60 %, Geometría 40 %):
 * <ul>
 * <li>Ana Lucía: Aritmética 16 y Geometría 11 → 16 × 0,6 + 11 × 0,4 = 14 → A.</li>
 * <li>Diego José: 10 y 10 → 10 → C.</li>
 * <li>Camila Sofía: sin esas notas, que sus compañeros sí tienen → pendiente.</li>
 * </ul>
 */
class ReportesYNotasApoderadoIT extends PruebaIntegracion {
	private static final String APODERADO = "U20260003";
	private static final String LUIS_PAREDES = "00000000-0000-0000-0000-000000000003";

	@Autowired
	private JdbcClient jdbc;

	private DatosNotasPrueba datos;
	private String periodo3;

	@BeforeEach
	void preparar() {
		datos = new DatosNotasPrueba(jdbc);
		String cantidad = datos.competencia(DatosNotasPrueba.CANTIDAD);
		String aritmetica = datos.item("Aritmética", 3, cantidad, "Examen de cantidad");
		String geometria = datos.item("Geometría", 3, cantidad, "Examen de cantidad");
		String ana = datos.matriculaDe("Ana Lucía");
		String diego = datos.matriculaDe("Diego José");
		datos.nota(aritmetica, ana, 16);
		datos.nota(geometria, ana, 11);
		datos.nota(aritmetica, diego, 10);
		datos.nota(geometria, diego, 10);
		periodo3 = datos.periodo(3);
	}

	private String fila(String campo) {
		return "$.filas[?(@.competencia == '%s')].%s".formatted(DatosNotasPrueba.CANTIDAD, campo);
	}

	// ---------- HU-63 ----------

	@Test
	void elReporteDelGradoResumeLaNotaOficialPorSeccionYGrado() throws Exception {
		mvc.perform(conToken(get("/calificaciones/reportes/grado").param("anioEscolarId", DatosNotasPrueba.ANIO_2026)
				.param("grado", "SEC-1").param("periodoId", periodo3), token(COORDINADOR)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodo").value("III Bimestre"))
				.andExpect(jsonPath("$.curso").isEmpty())
				.andExpect(jsonPath("$.secciones[*].nombre", hasItem("A")))
				// (14 + 10) / 2 = 12 → B; la mitad tiene A o AD.
				.andExpect(jsonPath(fila("general.promedio")).value(contains("12")))
				.andExpect(jsonPath(fila("general.nivel")).value(contains("B")))
				.andExpect(jsonPath(fila("general.conNota")).value(contains(2)))
				.andExpect(jsonPath(fila("general.porcentajeLogro")).value(contains(50)))
				// Camila no tiene las notas que sus compañeros sí: su nota está pendiente y no entra al promedio.
				.andExpect(jsonPath(fila("general.pendientes")).value(contains(1)));
	}

	@Test
	void conUnCursoMuestraLoQueEseCursoAporta() throws Exception {
		mvc.perform(conToken(get("/calificaciones/reportes/grado").param("anioEscolarId", DatosNotasPrueba.ANIO_2026)
				.param("grado", "SEC-1").param("planCursoId", datos.plan("Aritmética"))
				.param("periodoId", periodo3), token(COORDINADOR)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.curso").value("Aritmética"))
				// Aritmética: (16 + 10) / 2 = 13.
				.andExpect(jsonPath(fila("general.promedio")).value(contains("13")));
	}

	@Test
	void sinPeriodoUsaLaNotaFinalYSeDescargaEnPdf() throws Exception {
		String coordinador = token(COORDINADOR);
		mvc.perform(conToken(get("/calificaciones/reportes/grado").param("anioEscolarId", DatosNotasPrueba.ANIO_2026)
				.param("grado", "SEC-1"), coordinador))
				.andExpect(jsonPath("$.periodo").value("Nota final"))
				.andExpect(jsonPath(fila("general.promedio")).value(contains("12")));

		byte[] pdf = mvc.perform(conToken(get("/calificaciones/reportes/grado/pdf")
				.param("anioEscolarId", DatosNotasPrueba.ANIO_2026).param("grado", "SEC-1"), coordinador))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andReturn().getResponse().getContentAsByteArray();
		assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
	}

	@Test
	void elCursoDebeSerDelGrado() throws Exception {
		mvc.perform(conToken(get("/calificaciones/reportes/grado").param("anioEscolarId", DatosNotasPrueba.ANIO_2026)
				.param("grado", "SEC-2").param("planCursoId", datos.plan("Aritmética")), token(COORDINADOR)))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void elCoordinadorVeLaNotaDeCadaEstudianteDeUnaSeccionYSuLibreta() throws Exception {
		String coordinador = token(COORDINADOR);
		String cantidad = datos.competencia(DatosNotasPrueba.CANTIDAD);
		String competencia = "$.notas.filas[?(@.matriculaId == '%s')].competencias[?(@.competenciaId == '%s')]";
		String ana = datos.matriculaDe("Ana Lucía");
		String camila = datos.matriculaDe("Camila Sofía");

		mvc.perform(conToken(get("/calificaciones/reportes/secciones/" + datos.seccionA() + "/notas"), coordinador))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.seccion").value("A"))
				.andExpect(jsonPath("$.grado").value("1.° de secundaria"))
				.andExpect(jsonPath("$.notas.competencias[*].competencia", hasItem(DatosNotasPrueba.CANTIDAD)))
				.andExpect(jsonPath(competencia.formatted(ana, cantidad) + ".periodos[2].nota").value(contains("14")))
				.andExpect(jsonPath(competencia.formatted(ana, cantidad) + ".periodos[2].nivel").value(contains("A")))
				// Camila no tiene las notas que sus compañeros sí: pendiente, sin número.
				.andExpect(jsonPath(competencia.formatted(camila, cantidad) + ".periodos[2].pendiente")
						.value(contains(true)));

		byte[] pdf = mvc.perform(conToken(get("/calificaciones/reportes/matriculas/" + ana + "/libreta"), coordinador))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsByteArray();
		assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
	}

	@Test
	void soloElCoordinadorVeElDetalleDeUnaSeccion() throws Exception {
		String seccion = "/calificaciones/reportes/secciones/" + datos.seccionA() + "/notas";
		String libreta = "/calificaciones/reportes/matriculas/" + datos.matriculaDe("Ana Lucía") + "/libreta";
		for (String usuario : new String[]{ADMIN, DOCENTE, PERSONAL_ADMINISTRATIVO}) {
			String token = token(usuario);
			mvc.perform(conToken(get(seccion), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(get(libreta), token)).andExpect(status().isForbidden());
		}
		mvc.perform(conToken(get("/calificaciones/reportes/secciones/" + java.util.UUID.randomUUID() + "/notas"),
				token(COORDINADOR))).andExpect(status().isNotFound());
	}

	// ---------- HU-64 ----------

	@Test
	void elReporteInstitucionalCuentaCadaNotaDeCompetenciaUnaVez() throws Exception {
		String admin = token(ADMIN);
		mvc.perform(conToken(get("/calificaciones/reportes/institucional")
				.param("anioEscolarId", DatosNotasPrueba.ANIO_2026).param("periodoId", periodo3), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total.ad").value(0))
				.andExpect(jsonPath("$.total.a").value(1))
				.andExpect(jsonPath("$.total.b").value(0))
				.andExpect(jsonPath("$.total.c").value(1))
				.andExpect(jsonPath("$.total.porcentajeLogro").value(50))
				.andExpect(jsonPath("$.total.pendientes").value(1))
				.andExpect(jsonPath("$.filas[?(@.gradoCodigo == 'SEC-1' && @.seccion == 'A')].distribucion.a")
						.value(contains(1)))
				.andExpect(jsonPath("$.areas[*].area", hasItem("Matemática")));

		mvc.perform(conToken(get("/calificaciones/reportes/institucional/pdf")
				.param("anioEscolarId", DatosNotasPrueba.ANIO_2026), admin))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF));
	}

	@Test
	void permisosDeLosReportes() throws Exception {
		mvc.perform(conToken(get("/calificaciones/reportes/anios"), token(ADMIN))).andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.anio == 2026)].grados[*].codigo", hasItem("SEC-1")));
		mvc.perform(conToken(get("/calificaciones/reportes/grado").param("anioEscolarId", DatosNotasPrueba.ANIO_2026)
				.param("grado", "SEC-1"), token(ADMIN))).andExpect(status().isForbidden());
		mvc.perform(conToken(get("/calificaciones/reportes/institucional")
				.param("anioEscolarId", DatosNotasPrueba.ANIO_2026), token(DOCENTE)))
				.andExpect(status().isForbidden());
	}

	// ---------- HU-69 ----------

	/** Luis Paredes (apoderado de Sofía) pasa a ser también apoderado de Ana Lucía, solo en esta prueba. */
	private String apoderadoDeAna() throws Exception {
		jdbc.sql("""
				INSERT INTO estudiantes.vinculo_familiar (estudiante_id, persona_id, parentesco)
				VALUES (?::uuid, ?::uuid, 'APODERADO')""").params(datos.estudianteDe("Ana Lucía"), LUIS_PAREDES)
				.update();
		return token(APODERADO);
	}

	@Test
	void elApoderadoVeLasNotasDeSuHijoPorCursoYCompetencia() throws Exception {
		String apoderado = apoderadoDeAna();
		String ana = datos.estudianteDe("Ana Lucía");

		mvc.perform(conToken(get("/calificaciones/apoderado/estudiantes"), apoderado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].estudiante", hasItem("Ana Lucía García López")));

		String competencia = "$.competencias[?(@.competencia == '%s')]".formatted(DatosNotasPrueba.CANTIDAD);
		String aritmetica = "$.cursos[?(@.curso == 'Aritmética')].competencias[?(@.competencia == '%s')]"
				.formatted(DatosNotasPrueba.CANTIDAD);
		mvc.perform(conToken(get("/calificaciones/apoderado/estudiantes/" + ana + "/notas").param("anio", "2026"),
				apoderado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.grado").value("1.° de secundaria"))
				.andExpect(jsonPath("$.seccion").value("A"))
				.andExpect(jsonPath("$.periodos[2].estado").value("CERRADO"))
				.andExpect(jsonPath(competencia + ".periodos[2].nota").value(contains("14")))
				.andExpect(jsonPath(competencia + ".periodos[2].nivel").value(contains("A")))
				.andExpect(jsonPath(competencia + ".periodos[2].logro").value(contains("Logro esperado")))
				.andExpect(jsonPath("$.cursos[?(@.curso == 'Aritmética')].docente")
						.value(hasItem(containsString("María"))))
				.andExpect(jsonPath(aritmetica + ".periodos[2].aporte.nota").value(contains("16")))
				.andExpect(jsonPath(aritmetica + ".periodos[2].items[0].item").value(contains("Examen de cantidad")));

		byte[] pdf = mvc.perform(conToken(get("/calificaciones/apoderado/estudiantes/" + ana + "/libreta"), apoderado))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsByteArray();
		assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
	}

	@Test
	void elApoderadoNoVeAEstudiantesQueNoSonSuyos() throws Exception {
		String apoderado = apoderadoDeAna();

		mvc.perform(conToken(get("/calificaciones/apoderado/estudiantes/" + datos.estudianteDe("Diego José")
				+ "/notas"), apoderado))
				.andExpect(status().isNotFound());
		mvc.perform(conToken(get("/calificaciones/apoderado/estudiantes/" + datos.estudianteDe("Ana Lucía")
				+ "/notas").param("anio", "2025"), apoderado))
				.andExpect(status().isNotFound());
		mvc.perform(conToken(get("/calificaciones/apoderado/estudiantes"), token(DOCENTE)))
				.andExpect(status().isForbidden());
	}
}
