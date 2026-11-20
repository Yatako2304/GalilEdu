package com.galiledu.asistencia.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.galiledu.asistencia.aplicacion.DtosAsistencia.SeccionAsistencia;
import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.FilaReporte;
import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.OpcionesReporte;
import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.ReporteAsistencia;
import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.TotalesReporte;
import com.galiledu.asistencia.aplicacion.RangoReporte.Tipo;

/** HU-68 · RF-146: contenido y paginación del PDF del reporte de asistencia. */
class ArchivoReporteAsistenciaTests {
	private static final OffsetDateTime GENERADO = OffsetDateTime.of(2026, 11, 19, 20, 0, 0, 0, ZoneOffset.UTC);

	@DisplayName("Incluye la sección el rango las filas y los totales")
	@Test
	void incluyeLaSeccionElRangoLasFilasYLosTotales() throws Exception {
		byte[] archivo = ArchivoReporteAsistencia.pdf(reporte(List.of(
				new FilaReporte(UUID.randomUUID(), "Ana Lucía", "García López", "99006101", 2, 1, 3, 0),
				new FilaReporte(UUID.randomUUID(), "Diego José", "Quispe Mendoza", "99006102", 0, 0, 1, 1))),
				GENERADO);
		try (PDDocument pdf = Loader.loadPDF(archivo)) {
			assertThat(pdf.getNumberOfPages()).isEqualTo(1);
			assertThat(new PDFTextStripper().getText(pdf)).contains("Reporte de inasistencias y tardanzas",
					"1.° de secundaria · Sección A · Año escolar 2026", "Noviembre 2026 (01/11/2026 al 30/11/2026)",
					"12 días con asistencia registrada", "García López, Ana Lucía", "Quispe Mendoza, Diego José",
					"Total de la sección", "Generado el 19/11/2026 15:00.", "Página 1 de 1");
			Files.createDirectories(Path.of("target"));
			ImageIO.write(new PDFRenderer(pdf).renderImageWithDPI(0, 100), "png",
					Path.of("target/reporte-asistencia-demo.png").toFile());
		}
	}

	@DisplayName("Una sección grande continua en otra página con el total al final")
	@Test
	void unaSeccionGrandeContinuaEnOtraPaginaConElTotalAlFinal() throws Exception {
		List<FilaReporte> filas = IntStream.rangeClosed(1, 45)
				.mapToObj(n -> new FilaReporte(UUID.randomUUID(), "Estudiante", "Apellido " + n, "9900" + n, 1, 0, 0,
						0))
				.toList();
		try (PDDocument pdf = Loader.loadPDF(ArchivoReporteAsistencia.pdf(reporte(filas), GENERADO))) {
			assertThat(pdf.getNumberOfPages()).isEqualTo(2);
			PDFTextStripper lector = new PDFTextStripper();
			lector.setStartPage(1);
			lector.setEndPage(1);
			assertThat(lector.getText(pdf)).doesNotContain("Total de la sección").contains("Página 1 de 2");
			lector.setStartPage(2);
			lector.setEndPage(2);
			assertThat(lector.getText(pdf)).contains("Apellido 45", "Total de la sección");
		}
	}

	private static ReporteAsistencia reporte(List<FilaReporte> filas) {
		return new ReporteAsistencia(
				new SeccionAsistencia(UUID.randomUUID(), 2026, "1.° de secundaria", "A", "TUTOR"), Tipo.MES, "2026-11",
				"Noviembre 2026", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 12,
				new OpcionesReporte(List.of(), List.of()), filas,
				new TotalesReporte(filas.stream().mapToInt(FilaReporte::faltas).sum(),
						filas.stream().mapToInt(FilaReporte::faltasJustificadas).sum(),
						filas.stream().mapToInt(FilaReporte::tardanzas).sum(),
						filas.stream().mapToInt(FilaReporte::tardanzasJustificadas).sum()));
	}
}
