package com.galiledu.pagos.infraestructura;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;

import com.galiledu.pagos.aplicacion.DtosPagos.ConstanciaPago;
import com.galiledu.pagos.aplicacion.DtosPagos.CuotaConstancia;

/**
 * HU-42 · RF-107: PDF de la constancia de un pago (A4 vertical). Solo presenta los datos de la constancia; no consulta
 * ni recalcula nada. Mismo estilo que el reporte de notas (ArchivoReporteCalificaciones): fuente Liberation Sans de
 * PDFBox, encabezado azul y celdas con borde claro.
 */
public final class ArchivoConstanciaPago {
	private static final ZoneId LIMA = ZoneId.of("America/Lima");
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final float MARGEN = 48;
	private static final float ALTO_FILA = 24;
	private static final float PIE = 80;
	private static final Color AZUL = new Color(14, 40, 71);
	private static final Color TEXTO = new Color(30, 41, 59);
	private static final Color GRIS = new Color(85, 101, 124);
	private static final Color BORDE = new Color(222, 230, 239);
	private static final Color CLARO = new Color(238, 243, 251);
	private static final Color VERDE = new Color(32, 119, 61);
	private static final Color AMBAR = new Color(154, 91, 0);
	/** Columnas de la tabla: título y ancho (suman el ancho útil de la página). */
	private static final String[] COLUMNAS = {"Concepto", "Cuota", "Vencimiento", "Monto", "Recargo", "Subtotal"};
	private static final float[] ANCHOS = {110, 135, 75, 63, 54, 62};

	private final PDDocument documento;
	private final PDType0Font fuente;

	private ArchivoConstanciaPago(PDDocument documento) throws IOException {
		this.documento = documento;
		try (InputStream recurso = PDDocument.class.getResourceAsStream(
				"/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
			if (recurso == null)
				throw new IOException("No se encontró la fuente de la constancia.");
			fuente = PDType0Font.load(documento, recurso);
		}
	}

	public static byte[] pdf(ConstanciaPago constancia, OffsetDateTime generado) {
		try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			documento.getDocumentInformation().setTitle("Constancia de pago " + constancia.numeroConstancia());
			documento.getDocumentInformation().setAuthor("GalilEdu");
			new ArchivoConstanciaPago(documento).dibujar(constancia, generado);
			documento.save(salida);
			return salida.toByteArray();
		} catch (IOException excepcion) {
			throw new UncheckedIOException("No se pudo generar el PDF de la constancia.", excepcion);
		}
	}

	private void dibujar(ConstanciaPago c, OffsetDateTime generado) throws IOException {
		float ancho = PDRectangle.A4.getWidth() - 2 * MARGEN;
		int siguiente = 0;
		boolean primera = true;
		do {
			PDPage pagina = new PDPage(PDRectangle.A4);
			documento.addPage(pagina);
			try (PDPageContentStream lienzo = new PDPageContentStream(documento, pagina)) {
				float alto = pagina.getMediaBox().getHeight();
				float y = alto - MARGEN;
				texto(lienzo, "Galiledu", MARGEN, y - 12, 12, AZUL, true);
				derecha(lienzo, "N.° " + c.numeroConstancia(), MARGEN + ancho, y - 12, 12, AZUL, true);
				texto(lienzo, primera ? "Constancia de pago" : "Constancia de pago (continuación)", MARGEN, y - 40,
						20, AZUL, true);
				y -= 60;
				if (primera) {
					y = datos(lienzo, c, y, ancho);
				}
				y = encabezado(lienzo, y);
				while (siguiente < c.cuotas().size() && y - ALTO_FILA >= PIE + (esUltima(c, siguiente) ? 90 : 0)) {
					fila(lienzo, c.cuotas().get(siguiente), y, siguiente % 2 == 1);
					y -= ALTO_FILA;
					siguiente++;
				}
				if (siguiente >= c.cuotas().size()) {
					totales(lienzo, c, y - 12, ancho);
				}
				pie(lienzo, generado, ancho);
			}
			primera = false;
		} while (siguiente < c.cuotas().size());
	}

	private static boolean esUltima(ConstanciaPago c, int indice) {
		return indice == c.cuotas().size() - 1;
	}

	private float datos(PDPageContentStream lienzo, ConstanciaPago c, float y, float ancho) throws IOException {
		String matricula = c.anio() == null
				? "—"
				: c.anio()
						+ (c.grado() == null ? "" : " · " + c.grado() + (c.seccion() == null ? "" : " " + c.seccion()));
		String[][] campos = {
				{"Estudiante", c.estudiante()},
				{"Documento", c.documento()},
				{"Año escolar", matricula},
				{"Fecha de pago", FECHA_HORA.format(c.fechaPago().atZoneSameInstant(LIMA))},
				{"Referencia", c.referencia() == null || c.referencia().isBlank() ? "—" : c.referencia()},
				{"Estado", "CONFIRMADO".equals(c.estado()) ? "Confirmado" : "Observado"}};
		float mitad = ancho / 2;
		for (int indice = 0; indice < campos.length; indice++) {
			float x = MARGEN + (indice % 2) * mitad;
			float fila = y - (indice / 2) * 38;
			texto(lienzo, campos[indice][0], x, fila - 10, 9, GRIS, false);
			Color color = indice == campos.length - 1
					? ("Confirmado".equals(campos[indice][1]) ? VERDE : AMBAR)
					: TEXTO;
			texto(lienzo, recortar(campos[indice][1], mitad - 12, 11), x, fila - 26, 11, color, true);
		}
		y -= (campos.length + 1) / 2 * 38 + 8;
		if (c.observacion() != null && !c.observacion().isBlank()) {
			y = parrafo(lienzo, "Observación: " + c.observacion(), MARGEN, y, ancho, 9, AMBAR) - 8;
		}
		return y - 8;
	}

	private float encabezado(PDPageContentStream lienzo, float y) throws IOException {
		float x = MARGEN;
		for (int indice = 0; indice < COLUMNAS.length; indice++) {
			celda(lienzo, x, y, ANCHOS[indice], ALTO_FILA, AZUL);
			if (indice >= 3) {
				derecha(lienzo, COLUMNAS[indice], x + ANCHOS[indice] - 6, y - 16, 9, Color.WHITE, true);
			} else {
				texto(lienzo, COLUMNAS[indice], x + 6, y - 16, 9, Color.WHITE, true);
			}
			x += ANCHOS[indice];
		}
		return y - ALTO_FILA;
	}

	private void fila(PDPageContentStream lienzo, CuotaConstancia cuota, float y, boolean alterna) throws IOException {
		String[] valores = {cuota.concepto(), cuota.cuota(),
				cuota.vencimiento() == null ? "—" : FECHA.format(cuota.vencimiento()), soles(cuota.capital()),
				soles(cuota.recargo()), soles(cuota.capital().add(cuota.recargo()))};
		float x = MARGEN;
		for (int indice = 0; indice < valores.length; indice++) {
			celda(lienzo, x, y, ANCHOS[indice], ALTO_FILA, alterna ? CLARO : Color.WHITE);
			if (indice >= 3) {
				derecha(lienzo, valores[indice], x + ANCHOS[indice] - 6, y - 16, 9, TEXTO, indice == 5);
			} else {
				texto(lienzo, recortar(valores[indice], ANCHOS[indice] - 12, 9), x + 6, y - 16, 9, TEXTO, false);
			}
			x += ANCHOS[indice];
		}
	}

	private void totales(PDPageContentStream lienzo, ConstanciaPago c, float y, float ancho) throws IOException {
		float etiqueta = MARGEN + ancho - 200;
		String[][] filas = {
				{"Monto de cuotas", soles(c.capital())},
				{"Recargo por mora", soles(c.recargo())}};
		for (String[] fila : filas) {
			texto(lienzo, fila[0], etiqueta, y - 12, 10, GRIS, false);
			derecha(lienzo, fila[1], MARGEN + ancho - 6, y - 12, 10, TEXTO, false);
			y -= 20;
		}
		celda(lienzo, etiqueta - 8, y - 2, 208, 28, CLARO);
		texto(lienzo, "Total pagado", etiqueta, y - 20, 12, AZUL, true);
		derecha(lienzo, soles(c.montoTotal()), MARGEN + ancho - 6, y - 20, 12, AZUL, true);
	}

	private void pie(PDPageContentStream lienzo, OffsetDateTime generado, float ancho) throws IOException {
		lienzo.setStrokingColor(BORDE);
		lienzo.setLineWidth(0.5f);
		lienzo.moveTo(MARGEN, 60);
		lienzo.lineTo(MARGEN + ancho, 60);
		lienzo.stroke();
		texto(lienzo, "Constancia emitida por GalilEdu como sustento del pago registrado.", MARGEN, 44, 8, GRIS,
				false);
		texto(lienzo, "Generada el " + FECHA_HORA.format(generado.atZoneSameInstant(LIMA)) + ".", MARGEN, 32, 8,
				GRIS, false);
	}

	private static String soles(BigDecimal monto) {
		return String.format(Locale.US, "S/ %,.2f", monto);
	}

	private String recortar(String valor, float ancho, float tamano) throws IOException {
		if (ancho(valor, tamano) <= ancho)
			return valor;
		String texto = valor;
		while (!texto.isEmpty() && ancho(texto + "…", tamano) > ancho)
			texto = texto.substring(0, texto.length() - 1);
		return texto + "…";
	}

	private float parrafo(PDPageContentStream lienzo, String valor, float x, float y, float ancho, float tamano,
			Color color) throws IOException {
		List<String> lineas = new ArrayList<>();
		StringBuilder linea = new StringBuilder();
		for (String palabra : valor.replaceAll("\\s+", " ").strip().split(" ")) {
			String propuesta = linea.isEmpty() ? palabra : linea + " " + palabra;
			if (ancho(propuesta, tamano) > ancho && !linea.isEmpty()) {
				lineas.add(linea.toString());
				linea.setLength(0);
				linea.append(palabra);
			} else {
				linea.setLength(0);
				linea.append(propuesta);
			}
		}
		if (!linea.isEmpty())
			lineas.add(linea.toString());
		for (String texto : lineas) {
			texto(lienzo, texto, x, y - tamano, tamano, color, false);
			y -= tamano + 3;
		}
		return y;
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
