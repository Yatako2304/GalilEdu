package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.galiledu.academico.aplicacion.DtosCalificaciones.ColumnaReporte;
import com.galiledu.academico.aplicacion.DtosCalificaciones.FilaReporte;
import com.galiledu.academico.aplicacion.DtosCalificaciones.ReporteCalificaciones;
import com.galiledu.academico.infraestructura.ArchivoReporteCalificaciones;

class ArchivoReporteCalificacionesTests {
	private static final String CONTEXTO = "Álgebra · 1.° de secundaria · Sección A · Año escolar 2026 · IV Bimestre";
	private static final String PARAMETROS = "Escala: 0 a 20 · Regla del año escolar: Promedio ponderado";

	@DisplayName("Genera una tabla legible con competencias promedios y tildes")
	@Test
	void generaUnaTablaLegibleConCompetenciasPromediosYTildes() throws Exception {
		byte[] archivo = ArchivoReporteCalificaciones.pdf(reporte(3, 2), CONTEXTO, PARAMETROS, "VIGESIMAL");
		assertThat(new String(archivo, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
		try (PDDocument pdf = Loader.loadPDF(archivo)) {
			assertThat(pdf.getNumberOfPages()).isEqualTo(1);
			assertThat(pdf.getPage(0).getMediaBox().getWidth()).isGreaterThan(pdf.getPage(0).getMediaBox().getHeight());
			String texto = new PDFTextStripper().getText(pdf);
			assertThat(texto).contains("Consolidado de notas", "Álgebra", "IV Bimestre", "Regla del año escolar",
					"Práctica 1", "Práctica 2", "Promedio del", "Promedio general", "Ana Lucía García López",
					"Diego José Quispe Mendoza", "Camila Sofía Torres Vargas", "17/20", "A · Logro esperado",
					"B · En proceso",
					"Página 1 de 1");
			Files.write(Path.of("target/reporte-pdf-demo.pdf"), archivo);
			ImageIO.write(new PDFRenderer(pdf).renderImageWithDPI(0, 125), "png",
					Path.of("target/reporte-pdf-demo.png").toFile());
		}
	}

	@DisplayName("Un promedio con registros faltantes muestra el aviso completo sin encimarse")
	@Test
	void unPromedioConRegistrosFaltantesMuestraElAvisoCompletoSinEncimarse() throws Exception {
		UUID competencia = UUID.randomUUID();
		var columnas = List.of(
				new ColumnaReporte("c:i1", "Práctica 1", competencia, "Resuelve problemas de cantidad", false),
				new ColumnaReporte("c:i2", "FSSAF", competencia, "Resuelve problemas de cantidad", false),
				new ColumnaReporte("c:resultado", "Promedio del periodo", competencia,
						"Resuelve problemas de cantidad", true));
		var filas = List.of(
				new FilaReporte(UUID.randomUUID(), "Ana Lucía García López",
						Map.of("c:i1", "12", "c:i2", "8", "c:resultado", "11.8", "c:resultado_estado",
								"B · En proceso")),
				new FilaReporte(UUID.randomUUID(), "Diego José Quispe Mendoza",
						Map.of("c:i1", "15", "c:i2", "Por registrar", "c:resultado", "9.5", "c:resultado_estado",
								"Faltan registros (cuentan como 0) · 70 % del peso calificado")));
		byte[] archivo = ArchivoReporteCalificaciones.pdf(
				new ReporteCalificaciones("Resuelve problemas de cantidad", "Consolidado", columnas, filas), CONTEXTO,
				PARAMETROS, "VIGESIMAL");
		try (PDDocument pdf = Loader.loadPDF(archivo)) {
			ImageIO.write(new PDFRenderer(pdf).renderImageWithDPI(0, 125), "png",
					Path.of("target/reporte-pdf-faltan-registros.png").toFile());
			String texto = new PDFTextStripper().getText(pdf);
			// El aviso se parte en varias líneas dentro de su celda: se lee completo.
			assertThat(texto.replaceAll("\\s+", " "))
					.contains("Faltan registros (cuentan como 0) · 70 % del peso calificado", "9,5/20",
							"Por registrar");
		}
	}

	@DisplayName("Página estudiantes y columnas sin omitir notas ni reducir la letra")
	@Test
	void paginaEstudiantesYColumnasSinOmitirNotasNiReducirLaLetra() throws Exception {
		byte[] archivo = ArchivoReporteCalificaciones.pdf(reporte(45, 4), CONTEXTO, PARAMETROS, "VIGESIMAL");
		try (PDDocument pdf = Loader.loadPDF(archivo)) {
			assertThat(pdf.getNumberOfPages()).isGreaterThan(2);
			PDFTextStripper lector = new PDFTextStripper();
			String texto = lector.getText(pdf);
			assertThat(texto).contains("Estudiante 45", "Bloque 1 de 2", "Bloque 2 de 2", "Promedio general");
			for (int pagina = 1; pagina <= pdf.getNumberOfPages(); pagina++) {
				lector.setStartPage(pagina);
				lector.setEndPage(pagina);
				assertThat(lector.getText(pdf)).contains("Consolidado de notas", "Estudiante", "Promedio general",
						"Página " + pagina + " de " + pdf.getNumberOfPages());
			}
		}
	}

	@DisplayName("Conserva niveles cualitativos y notas pendientes")
	@Test
	void conservaNivelesCualitativosYNotasPendientes() throws Exception {
		ReporteCalificaciones base = reporte(1, 1);
		var reporte = new ReporteCalificaciones(base.titulo(), base.detalle(), base.columnas(), List.of(
				new FilaReporte(UUID.randomUUID(), "Ana Pérez", Map.of("c1:i1", "AD", "c1:i2", "—",
						"c1:resultado", "AD", "c1:resultado_estado", "Logro destacado"))));
		try (PDDocument pdf = Loader.loadPDF(ArchivoReporteCalificaciones.pdf(reporte, CONTEXTO,
				"Escala: AD, A, B, C · Regla del año escolar: Último nivel", "CUALITATIVA"))) {
			assertThat(new PDFTextStripper().getText(pdf)).contains("AD", "—", "Logro destacado").doesNotContain("/20");
		}
	}

	@DisplayName("Exporta sin promedios y conserva el estado vacio")
	@Test
	void exportaSinPromediosYConservaElEstadoVacio() throws Exception {
		ReporteCalificaciones base = reporte(1, 2);
		var reporte = new ReporteCalificaciones(base.titulo(), base.detalle(),
				base.columnas().stream().filter(c -> !c.promedio()).toList(), List.of());
		try (PDDocument pdf = Loader
				.loadPDF(ArchivoReporteCalificaciones.pdf(reporte, CONTEXTO, PARAMETROS, "VIGESIMAL"))) {
			assertThat(new PDFTextStripper().getText(pdf)).contains("No hay estudiantes matriculados")
					.doesNotContain("Promedio del periodo", "Promedio general");
		}
	}

	private static ReporteCalificaciones reporte(int alumnos, int competencias) {
		List<ColumnaReporte> columnas = new ArrayList<>();
		List<FilaReporte> filas = new ArrayList<>();
		String[] nombres = {"Ana Lucía García López", "Diego José Quispe Mendoza", "Camila Sofía Torres Vargas"};
		String[] titulos = {"Resuelve problemas de cantidad",
				"Resuelve problemas de regularidad, equivalencia y cambio",
				"Resuelve problemas de forma, movimiento y localización",
				"Resuelve problemas de gestión de datos e incertidumbre"};
		for (int competencia = 1; competencia <= competencias; competencia++) {
			UUID id = UUID.randomUUID();
			for (int item = 1; item <= 2; item++)
				columnas.add(new ColumnaReporte("c" + competencia + ":i" + item,
						"Práctica " + item, id, titulos[competencia - 1], false));
			columnas.add(new ColumnaReporte("c" + competencia + ":resultado", "Promedio del periodo", id,
					titulos[competencia - 1], true));
		}
		if (competencias > 1)
			columnas.add(new ColumnaReporte("resultado_general", "Promedio general", null, null, true));
		for (int alumno = 0; alumno < alumnos; alumno++) {
			Map<String, String> valores = new LinkedHashMap<>();
			String nota = String.valueOf(17 - (alumno % 3) * 3);
			for (ColumnaReporte columna : columnas) {
				valores.put(columna.clave(), nota);
				if (columna.promedio())
					valores.put(columna.clave() + "_estado", alumno % 3 == 2 ? "B · En proceso" : "A · Logro esperado");
			}
			filas.add(new FilaReporte(UUID.randomUUID(), alumno < 3 ? nombres[alumno] : "Estudiante " + (alumno + 1),
					valores));
		}
		return new ReporteCalificaciones("Competencias seleccionadas", "Consolidado de notas", columnas, filas);
	}
}
