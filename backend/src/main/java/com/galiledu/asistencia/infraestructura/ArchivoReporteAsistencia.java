package com.galiledu.asistencia.infraestructura;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;

import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.FilaReporte;
import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.ReporteAsistencia;

/**
 * HU-68 · RF-146: PDF del consolidado de inasistencias y tardanzas (A4 vertical). Presenta el mismo reporte de la
 * API, sin recalcular. Mismo estilo que el reporte de notas y la constancia de pago.
 */
public final class ArchivoReporteAsistencia {
	private static final ZoneId LIMA = ZoneId.of("America/Lima");
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final float MARGEN = 40;
	private static final float ALTO_FILA = 22;
	private static final float PIE = 70;
	private static final Color AZUL = new Color(14, 40, 71);
	private static final Color TEXTO = new Color(30, 41, 59);
	private static final Color GRIS = new Color(85, 101, 124);
	private static final Color BORDE = new Color(222, 230, 239);
	private static final Color CLARO = new Color(238, 243, 251);
	private static final String[] COLUMNAS = {"N.°", "Estudiante", "Documento", "Faltas", "Just.", "Tardanzas",
			"Just."};
	private static final float[] ANCHOS = {30, 201, 70, 54, 50, 60, 50};

	private final PDDocument documento;
	private final PDType0Font fuente;

	private ArchivoReporteAsistencia(PDDocument documento) throws IOException {
		this.documento = documento;
		try (InputStream recurso = PDDocument.class.getResourceAsStream(
				"/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
			if (recurso == null)
				throw new IOException("No se encontró la fuente del reporte.");
			fuente = PDType0Font.load(documento, recurso);
		}
	}

	public static byte[] pdf(ReporteAsistencia reporte, OffsetDateTime generado) {
		try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			documento.getDocumentInformation().setTitle("Reporte de inasistencias y tardanzas");
			documento.getDocumentInformation().setAuthor("GalilEdu");
			var archivo = new ArchivoReporteAsistencia(documento);
			archivo.dibujar(reporte);
			archivo.numerar(generado);
			documento.save(salida);
			return salida.toByteArray();
		} catch (IOException excepcion) {
			throw new UncheckedIOException("No se pudo generar el PDF del reporte de asistencia.", excepcion);
		}
	}

	private void dibujar(ReporteAsistencia r) throws IOException {
		int siguiente = 0;
		do {
			PDPage pagina = new PDPage(PDRectangle.A4);
			documento.addPage(pagina);
			try (PDPageContentStream lienzo = new PDPageContentStream(documento, pagina)) {
				float y = pagina.getMediaBox().getHeight() - MARGEN;
				texto(lienzo, "Galiledu", MARGEN, y - 12, 11, AZUL, true);
				texto(lienzo, "Reporte de inasistencias y tardanzas", MARGEN, y - 38, 18, AZUL, true);
				texto(lienzo, r.seccion().grado() + " · Sección " + r.seccion().nombre() + " · Año escolar "
						+ r.seccion().anio(), MARGEN, y - 58, 10, TEXTO, false);
				texto(lienzo, r.etiqueta() + " (" + FECHA.format(r.desde()) + " al " + FECHA.format(r.hasta())
						+ ") · " + r.diasRegistrados() + (r.diasRegistrados() == 1 ? " día" : " días")
						+ " con asistencia registrada", MARGEN, y - 74, 10, GRIS, false);
				y -= 92;
				y = encabezado(lienzo, y);
				if (r.filas().isEmpty()) {
					texto(lienzo, "La sección no tiene estudiantes matriculados.", MARGEN + 6, y - 16, 9, GRIS, false);
				}
				while (siguiente < r.filas().size() && y - ALTO_FILA >= PIE + ALTO_FILA) {
					fila(lienzo, siguiente + 1, r.filas().get(siguiente), y, siguiente % 2 == 1);
					y -= ALTO_FILA;
					siguiente++;
				}
				if (siguiente >= r.filas().size()) {
					totales(lienzo, r, y);
				}
			}
		} while (siguiente < r.filas().size());
	}

	private float encabezado(PDPageContentStream lienzo, float y) throws IOException {
		float x = MARGEN;
		for (int i = 0; i < COLUMNAS.length; i++) {
			celda(lienzo, x, y, ANCHOS[i], ALTO_FILA, AZUL);
			if (i >= 3) {
				derecha(lienzo, COLUMNAS[i], x + ANCHOS[i] - 6, y - 15, 9, Color.WHITE, true);
			} else {
				texto(lienzo, COLUMNAS[i], x + 6, y - 15, 9, Color.WHITE, true);
			}
			x += ANCHOS[i];
		}
		return y - ALTO_FILA;
	}

	private void fila(PDPageContentStream lienzo, int numero, FilaReporte f, float y, boolean alterna)
			throws IOException {
		String[] valores = {String.valueOf(numero), f.apellidos() + ", " + f.nombres(), f.documento(),
				String.valueOf(f.faltas()), String.valueOf(f.faltasJustificadas()), String.valueOf(f.tardanzas()),
				String.valueOf(f.tardanzasJustificadas())};
		float x = MARGEN;
		for (int i = 0; i < valores.length; i++) {
			celda(lienzo, x, y, ANCHOS[i], ALTO_FILA, alterna ? CLARO : Color.WHITE);
			if (i >= 3) {
				derecha(lienzo, valores[i], x + ANCHOS[i] - 6, y - 15, 9, TEXTO, false);
			} else {
				texto(lienzo, recortar(valores[i], ANCHOS[i] - 12, 9), x + 6, y - 15, 9, TEXTO, false);
			}
			x += ANCHOS[i];
		}
	}

	private void totales(PDPageContentStream lienzo, ReporteAsistencia r, float y) throws IOException {
		String[] valores = {"", "Total de la sección", "", String.valueOf(r.totales().faltas()),
				String.valueOf(r.totales().faltasJustificadas()), String.valueOf(r.totales().tardanzas()),
				String.valueOf(r.totales().tardanzasJustificadas())};
		float x = MARGEN;
		for (int i = 0; i < valores.length; i++) {
			celda(lienzo, x, y, ANCHOS[i], ALTO_FILA, CLARO);
			if (i >= 3) {
				derecha(lienzo, valores[i], x + ANCHOS[i] - 6, y - 15, 9, AZUL, true);
			} else {
				texto(lienzo, valores[i], x + 6, y - 15, 9, AZUL, true);
			}
			x += ANCHOS[i];
		}
	}

	private void numerar(OffsetDateTime generado) throws IOException {
		int total = documento.getNumberOfPages();
		for (int i = 0; i < total; i++) {
			try (PDPageContentStream lienzo = new PDPageContentStream(documento, documento.getPage(i),
					PDPageContentStream.AppendMode.APPEND, true, true)) {
				texto(lienzo, "Faltas y tardanzas incluyen las justificadas (Just.). Generado el "
						+ FECHA_HORA.format(generado.atZoneSameInstant(LIMA)) + ".", MARGEN, 36, 8, GRIS, false);
				derecha(lienzo, "Página " + (i + 1) + " de " + total, PDRectangle.A4.getWidth() - MARGEN, 36, 8,
						GRIS, false);
			}
		}
	}

	private String recortar(String valor, float ancho, float tamano) throws IOException {
		if (ancho(valor, tamano) <= ancho)
			return valor;
		String texto = valor;
		while (!texto.isEmpty() && ancho(texto + "…", tamano) > ancho)
			texto = texto.substring(0, texto.length() - 1);
		return texto + "…";
	}

	private float ancho(String valor, float tamano) throws IOException {
		return fuente.getStringWidth(valor) * tamano / 1000;
	}

	private void derecha(PDPageContentStream lienzo, String valor, float x, float y, float tamano, Color color,
			boolean negrita) throws IOException {
		texto(lienzo, valor, x - ancho(valor, tamano), y, tamano, color, negrita);
	}

	private static void celda(PDPageContentStream lienzo, float x, float y, float ancho, float alto, Color color)
			throws IOException {
		lienzo.setNonStrokingColor(color);
		lienzo.setStrokingColor(BORDE);
		lienzo.setLineWidth(0.5f);
		lienzo.addRect(x, y - alto, ancho, alto);
		lienzo.fillAndStroke();
	}

	private void texto(PDPageContentStream lienzo, String valor, float x, float y, float tamano, Color color,
			boolean negrita) throws IOException {
		lienzo.saveGraphicsState();
		lienzo.setNonStrokingColor(color);
		lienzo.setStrokingColor(color);
		lienzo.setLineWidth(0.25f);
		lienzo.beginText();
		lienzo.setRenderingMode(negrita ? RenderingMode.FILL_STROKE : RenderingMode.FILL);
		lienzo.setFont(fuente, tamano);
		lienzo.newLineAtOffset(x, y);
		lienzo.showText(valor);
		lienzo.endText();
		lienzo.restoreGraphicsState();
	}
}
