package com.galiledu.academico.infraestructura;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;

import com.galiledu.academico.aplicacion.DtosCalificaciones.ColumnaReporte;
import com.galiledu.academico.aplicacion.DtosCalificaciones.FilaReporte;
import com.galiledu.academico.aplicacion.DtosCalificaciones.ReporteCalificaciones;

/** Presentación del mismo consolidado de la API, sin recalcular ni consultar datos. */
public final class ArchivoReporteCalificaciones {
	private static final float MARGEN = 32;
	private static final float ANCHO_ESTUDIANTE = 174;
	private static final Color AZUL = new Color(14, 40, 71);
	private static final Color TEXTO = new Color(30, 41, 59);
	private static final Color GRIS = new Color(85, 101, 124);
	private static final Color BORDE = new Color(222, 230, 239);
	private static final Color CLARO = new Color(238, 243, 251);
	private static final Color VERDE = new Color(32, 119, 61);
	private static final Color ROJO = new Color(184, 38, 49);
	private final PDDocument documento;
	private final PDType0Font fuente;

	private ArchivoReporteCalificaciones(PDDocument documento) throws IOException {
		this.documento = documento;
		// Fuente Unicode redistribuida por PDFBox; no depende de fuentes del servidor.
		try (InputStream recurso = PDDocument.class.getResourceAsStream(
				"/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
			if (recurso == null)
				throw new IOException("No se encontró la fuente del reporte.");
			fuente = PDType0Font.load(documento, recurso);
		}
	}

	public static byte[] pdf(ReporteCalificaciones reporte, String contexto, String parametros, String escala) {
		try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			documento.getDocumentInformation().setTitle("Consolidado de notas");
			documento.getDocumentInformation().setAuthor("GalilEdu");
			var archivo = new ArchivoReporteCalificaciones(documento);
			List<List<ColumnaReporte>> bloques = bloques(reporte.columnas());
			for (int indice = 0; indice < bloques.size(); indice++) {
				archivo.tabla(reporte, bloques.get(indice), contexto, parametros, escala, indice + 1, bloques.size());
			}
			archivo.numerarPaginas();
			documento.save(salida);
			return salida.toByteArray();
		} catch (IOException excepcion) {
			throw new UncheckedIOException("No se pudo generar el PDF de calificaciones.", excepcion);
		}
	}

	private static List<List<ColumnaReporte>> bloques(List<ColumnaReporte> columnas) {
		ColumnaReporte general = columnas.stream().filter(c -> "resultado_general".equals(c.clave()))
				.findFirst().orElse(null);
		List<ColumnaReporte> detalle = columnas.stream().filter(c -> c != general).toList();
		int limite = general == null ? 7 : 6;
		List<List<ColumnaReporte>> bloques = new ArrayList<>();
		for (int inicio = 0; inicio < Math.max(1, detalle.size()); inicio += limite) {
			List<ColumnaReporte> bloque = new ArrayList<>(
					detalle.subList(inicio, Math.min(inicio + limite, detalle.size())));
			if (general != null)
				bloque.add(general);
			bloques.add(bloque);
		}
		return bloques;
	}

	private void tabla(ReporteCalificaciones reporte, List<ColumnaReporte> columnas, String contexto,
			String parametros, String escala, int bloque, int totalBloques) throws IOException {
		int siguiente = 0;
		do {
			PDPage pagina = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
			documento.addPage(pagina);
			try (PDPageContentStream lienzo = new PDPageContentStream(documento, pagina)) {
				float ancho = pagina.getMediaBox().getWidth() - 2 * MARGEN;
				texto(lienzo, "Galiledu", MARGEN, 568, 11, AZUL, true);
				texto(lienzo, "Consolidado de notas", MARGEN, 546, 20, AZUL, true);
				float y = parrafo(lienzo, contexto, MARGEN, 520, ancho, 10, TEXTO, false) - 8;
				y = parrafo(lienzo, parametros, MARGEN, y, ancho, 9, GRIS, false) - 8;
				if (totalBloques > 1) {
					y = parrafo(lienzo, "Bloque " + bloque + " de " + totalBloques
							+ ". El promedio general corresponde a toda la selección.", MARGEN, y, ancho, 8, GRIS,
							false) - 8;
				}
				float anchoDato = (ancho - ANCHO_ESTUDIANTE) / Math.max(1, columnas.size());
				y = encabezados(lienzo, reporte.titulo(), columnas, y, anchoDato);
				if (reporte.filas().isEmpty()) {
					celda(lienzo, MARGEN, y, ancho, 40, Color.WHITE);
					texto(lienzo, "No hay estudiantes matriculados para mostrar.", MARGEN + 10, y - 24, 10, GRIS,
							false);
				}
				int primeraFila = siguiente;
				while (siguiente < reporte.filas().size()) {
					FilaReporte fila = reporte.filas().get(siguiente);
					float alto = altoFila(fila, columnas, anchoDato, escala);
					if (y - alto < 60) {
						if (siguiente == primeraFila)
							throw new IOException("Una fila excede el espacio disponible en el reporte.");
						break;
					}
					dibujarFila(lienzo, fila, columnas, anchoDato, y, alto, siguiente % 2 == 1, escala);
					y -= alto;
					siguiente++;
				}
			}
		} while (siguiente < reporte.filas().size());
	}

	private float encabezados(PDPageContentStream lienzo, String titulo, List<ColumnaReporte> columnas,
			float y, float anchoDato) throws IOException {
		float altoGrupo = 30;
		float altoItem = 32;
		for (int inicio = 0; inicio < columnas.size();) {
			int fin = finGrupo(columnas, inicio);
			String grupo = grupo(columnas.get(inicio), titulo);
			altoGrupo = Math.max(altoGrupo, lineas(grupo, (fin - inicio) * anchoDato - 12, 9).size() * 12 + 12);
			inicio = fin;
		}
		for (ColumnaReporte columna : columnas) {
			altoItem = Math.max(altoItem, lineas(columna.titulo(), anchoDato - 12, 9).size() * 12 + 12);
		}
		celda(lienzo, MARGEN, y, ANCHO_ESTUDIANTE, altoGrupo + altoItem, AZUL);
		texto(lienzo, "Estudiante", MARGEN + 10, y - (altoGrupo + altoItem) / 2, 10, Color.WHITE, true);
		for (int inicio = 0; inicio < columnas.size();) {
			int fin = finGrupo(columnas, inicio);
			float x = MARGEN + ANCHO_ESTUDIANTE + inicio * anchoDato;
			celda(lienzo, x, y, (fin - inicio) * anchoDato, altoGrupo, AZUL);
			parrafo(lienzo, grupo(columnas.get(inicio), titulo), x + 6, y - 8,
					(fin - inicio) * anchoDato - 12, 9, Color.WHITE, true);
			inicio = fin;
		}
		for (int indice = 0; indice < columnas.size(); indice++) {
			float x = MARGEN + ANCHO_ESTUDIANTE + indice * anchoDato;
			celda(lienzo, x, y - altoGrupo, anchoDato, altoItem, CLARO);
			parrafo(lienzo, columnas.get(indice).titulo(), x + 6, y - altoGrupo - 8, anchoDato - 12, 9, AZUL, true);
		}
		return y - altoGrupo - altoItem;
	}

	private static int finGrupo(List<ColumnaReporte> columnas, int inicio) {
		int fin = inicio + 1;
		while (fin < columnas.size() && grupo(columnas.get(inicio), "").equals(grupo(columnas.get(fin), "")))
			fin++;
		return fin;
	}

	private static String grupo(ColumnaReporte columna, String titulo) {
		return columna.competencia() != null
				? columna.competencia()
				: "resultado_general".equals(columna.clave()) ? "Resumen del curso" : titulo;
	}

	private float altoFila(FilaReporte fila, List<ColumnaReporte> columnas, float anchoDato, String escala)
			throws IOException {
		float alto = Math.max(36, lineas(fila.estudiante(), ANCHO_ESTUDIANTE - 20, 9).size() * 12 + 14);
		for (ColumnaReporte columna : columnas) {
			String nota = nota(fila.valores().get(columna.clave()), columna, escala);
			alto = Math.max(alto, lineas(nota, anchoDato - 12, 10).size() * 13 + (columna.promedio() ? 24 : 14));
		}
		return alto;
	}

	private void dibujarFila(PDPageContentStream lienzo, FilaReporte fila, List<ColumnaReporte> columnas,
			float anchoDato, float y, float alto, boolean alterna, String escala) throws IOException {
		Color fondo = alterna ? new Color(247, 249, 252) : Color.WHITE;
		celda(lienzo, MARGEN, y, ANCHO_ESTUDIANTE, alto, fondo);
		parrafo(lienzo, fila.estudiante(), MARGEN + 10, y - 10, ANCHO_ESTUDIANTE - 20, 9, TEXTO, false);
		for (int indice = 0; indice < columnas.size(); indice++) {
			ColumnaReporte columna = columnas.get(indice);
			float x = MARGEN + ANCHO_ESTUDIANTE + indice * anchoDato;
			celda(lienzo, x, y, anchoDato, alto, columna.promedio() ? CLARO : fondo);
			String valor = nota(fila.valores().get(columna.clave()), columna, escala);
			float base = parrafo(lienzo, valor, x + 6, y - 10, anchoDato - 12, 10, AZUL, columna.promedio());
			String estado = fila.valores().get(columna.clave() + "_estado");
			if (columna.promedio() && estado != null) {
				texto(lienzo, estado, x + 6, base - 4, 7.5f,
						"Aprobado".equals(estado) ? VERDE : "Desaprobado".equals(estado) ? ROJO : GRIS, false);
			}
		}
	}

	private static String nota(String valor, ColumnaReporte columna, String escala) {
		if (valor == null || valor.isBlank())
			return "—";
		if ("VIGESIMAL".equals(escala) && !List.of("estado", "evidencia").contains(columna.clave())
				&& valor.matches("\\d+(\\.\\d+)?"))
			return valor.replace('.', ',') + "/20";
		return valor;
	}

	private void numerarPaginas() throws IOException {
		for (int indice = 0; indice < documento.getNumberOfPages(); indice++) {
			PDPage pagina = documento.getPage(indice);
			try (PDPageContentStream lienzo = new PDPageContentStream(documento, pagina,
					PDPageContentStream.AppendMode.APPEND, true, true)) {
				texto(lienzo, "Calificaciones registradas de los ítems seleccionados. —: pendiente.", MARGEN, 30, 8,
						GRIS, false);
				texto(lienzo, "Página " + (indice + 1) + " de " + documento.getNumberOfPages(), 710, 30, 8, GRIS,
						false);
			}
		}
	}

	private static void celda(PDPageContentStream lienzo, float x, float y, float ancho, float alto, Color color)
			throws IOException {
		lienzo.setNonStrokingColor(color);
		lienzo.setStrokingColor(BORDE);
		lienzo.setLineWidth(0.5f);
		lienzo.addRect(x, y - alto, ancho, alto);
		lienzo.fillAndStroke();
	}

	private float parrafo(PDPageContentStream lienzo, String valor, float x, float y, float ancho,
			float tamano, Color color, boolean negrita) throws IOException {
		for (String linea : lineas(valor, ancho, tamano)) {
			texto(lienzo, linea, x, y - tamano, tamano, color, negrita);
			y -= tamano + 3;
		}
		return y;
	}

	private List<String> lineas(String valor, float ancho, float tamano) throws IOException {
		List<String> lineas = new ArrayList<>();
		StringBuilder linea = new StringBuilder();
		for (String palabra : valor.replaceAll("\\s+", " ").strip().split(" ")) {
			String propuesta = linea.isEmpty() ? palabra : linea + " " + palabra;
			if (fuente.getStringWidth(propuesta) * tamano / 1000 > ancho && !linea.isEmpty()) {
				lineas.add(linea.toString());
				linea.setLength(0);
			}
			if (!linea.isEmpty())
				linea.append(' ');
			for (int codigo : palabra.codePoints().toArray()) {
				String caracter = Character.toString(codigo);
				if (fuente.getStringWidth(linea + caracter) * tamano / 1000 > ancho && !linea.isEmpty()) {
					lineas.add(linea.toString());
					linea.setLength(0);
				}
				linea.append(caracter);
			}
		}
		if (!linea.isEmpty())
			lineas.add(linea.toString());
		return lineas;
	}

	private void texto(PDPageContentStream lienzo, String valor, float x, float y, float tamano,
			Color color, boolean negrita) throws IOException {
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
