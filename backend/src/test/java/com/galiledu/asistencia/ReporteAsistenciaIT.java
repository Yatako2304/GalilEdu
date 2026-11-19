package com.galiledu.asistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.configuracion.PruebaIntegracionConfiguracion;
import com.galiledu.configuracion.RelojPrueba;

/**
 * HU-68 · RF-146: reporte de inasistencias y tardanzas. María Fernández (U20260004) registra el lunes 14 y el martes
 * 15/09/2026 (III Bimestre). Por apellidos: García López (presente y tardanza), Quispe Mendoza (falta justificada y
 * falta) y Torres Vargas (tardanza y tardanza).
 */
class ReporteAsistenciaIT extends PruebaIntegracionConfiguracion {
	private static final String TUTORA = DOCENTE;

	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private RelojPrueba reloj;

	private String seccion;
	private String token;

	@BeforeEach
	void registrarDosDias() throws Exception {
		seccion = jdbc.sql("""
				SELECT s.id::text FROM configuracion.seccion s
				JOIN configuracion.anio_escolar a ON a.id = s.anio_escolar_id
				JOIN seguridad.usuario u ON u.id = s.tutor_id
				WHERE a.anio = 2026 AND u.nombre_usuario = ?""").param(TUTORA).query(String.class).single();
		List<String> m = jdbc.sql("""
				SELECT m.id::text FROM matricula.matricula m
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				WHERE m.seccion_id = ?::uuid AND m.estado = 'MATRICULADO'
				ORDER BY p.apellido_paterno, p.apellido_materno, p.nombres""").param(seccion).query(String.class)
				.list();
		token = token(TUTORA);
		reloj.fijarEn(LocalDate.of(2026, 9, 14));
		registrar("2026-09-14", m, "PRESENTE", "FALTA", "TARDANZA");
		mvc.perform(conToken(multipart(dia("2026-09-14") + "/justificaciones").param("matriculaId", m.get(1))
				.param("motivo", "ENFERMEDAD"), token)).andExpect(status().isOk());
		reloj.fijarEn(LocalDate.of(2026, 9, 15));
		registrar("2026-09-15", m, "TARDANZA", "FALTA", "TARDANZA");
	}

	@Test
	void consolidaElMesPorApellidosConLasJustificadasAparte() throws Exception {
		mvc.perform(conToken(get(reporte()).param("tipo", "MES").param("valor", "2026-09"), token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.etiqueta").value("Setiembre 2026"))
				.andExpect(jsonPath("$.diasRegistrados").value(2))
				.andExpect(jsonPath("$.filas", hasSize(3)))
				.andExpect(jsonPath("$.filas[0].apellidos").value("García López"))
				.andExpect(jsonPath("$.filas[0].tardanzas").value(1))
				.andExpect(jsonPath("$.filas[1].faltas").value(2))
				.andExpect(jsonPath("$.filas[1].faltasJustificadas").value(1))
				.andExpect(jsonPath("$.filas[2].tardanzas").value(2))
				.andExpect(jsonPath("$.filas[2].tardanzasJustificadas").value(0))
				.andExpect(jsonPath("$.totales.faltas").value(2))
				.andExpect(jsonPath("$.totales.tardanzas").value(3));
	}

	@Test
	void porPeriodoCuentaTodoElBimestre() throws Exception {
		mvc.perform(conToken(get(reporte()).param("tipo", "PERIODO").param("valor", "3"), token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.etiqueta").value("III Bimestre"))
				.andExpect(jsonPath("$.desde").value("2026-08-03"))
				.andExpect(jsonPath("$.totales.faltas").value(2))
				.andExpect(jsonPath("$.opciones.periodos", hasSize(3)))
				.andExpect(jsonPath("$.opciones.meses[0].valor").value("2026-09"));
	}

	@Test
	void unMesSinRegistrosSaleEnCero() throws Exception {
		mvc.perform(conToken(get(reporte()).param("tipo", "MES").param("valor", "2026-08"), token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diasRegistrados").value(0))
				.andExpect(jsonPath("$.totales.faltas").value(0))
				.andExpect(jsonPath("$.filas", hasSize(3)));
	}

	@Test
	void sinParametrosEsElMesActual() throws Exception {
		mvc.perform(conToken(get(reporte()), token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("MES"))
				.andExpect(jsonPath("$.valor").value("2026-09"));
	}

	@Test
	void unMesFueraDelAnioSeRechaza() throws Exception {
		mvc.perform(conToken(get(reporte()).param("tipo", "MES").param("valor", "2025-12"), token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El mes no es parte del año escolar 2026."));
	}

	@Test
	void exportaElReporteEnPdf() throws Exception {
		byte[] archivo = mvc.perform(conToken(get(reporte() + "/pdf").param("tipo", "MES").param("valor", "2026-09"),
				token))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						Matchers.containsString("reporte-asistencia-1-de-secundaria-a-2026-09.pdf")))
				.andReturn().getResponse().getContentAsByteArray();
		try (PDDocument pdf = Loader.loadPDF(archivo)) {
			assertThat(new PDFTextStripper().getText(pdf)).contains("Reporte de inasistencias y tardanzas",
					"1.° de secundaria · Sección A", "Setiembre 2026", "2 días con asistencia registrada",
					"García López, Ana Lucía", "Total de la sección", "Página 1 de 1");
		}
	}

	@Test
	void unaSeccionAjenaNoSeVe() throws Exception {
		mvc.perform(conToken(get("/asistencia/secciones/{id}/reporte", "30000000-0000-0000-0000-000000000099"),
				token))
				.andExpect(status().isNotFound());
	}

	private void registrar(String fecha, List<String> m, String... estados) throws Exception {
		StringBuilder json = new StringBuilder("{\"version\":null,\"asistencias\":[");
		for (int i = 0; i < estados.length; i++) {
			json.append(i == 0 ? "" : ",").append("{\"matriculaId\":\"").append(m.get(i)).append("\",\"estado\":\"")
					.append(estados[i]).append('"')
					.append("TARDANZA".equals(estados[i]) ? ",\"horaLlegada\":\"08:10\"" : "")
					.append('}');
		}
		mvc.perform(conToken(put(dia(fecha)), token, json.append("]}").toString())).andExpect(status().isOk());
	}

	private String dia(String fecha) {
		return "/asistencia/secciones/" + seccion + "/dias/" + fecha;
	}

	private String reporte() {
		return "/asistencia/secciones/" + seccion + "/reporte";
	}
}
