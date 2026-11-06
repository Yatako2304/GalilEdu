package com.galiledu.configuracion.dominio;

import java.util.Locale;

/**
 * Plantilla opcional de un documento requerido: el apoderado la descarga, la completa o firma y la vuelve a subir.
 * Formatos: PDF, DOCX, JPG o PNG, máx. 10 MB. La BD no restringe el tipo ni el tamaño, así que se valida aquí: la
 * extensión y que el contenido sea de ese tipo. El tipo que declara el navegador no se usa; se guarda el que
 * corresponde a la extensión.
 *
 * <p>
 * Las reglas lanzan {@link IllegalArgumentException} (400).
 */
public final class PlantillaDocumento {
	public static final int TAMANO_MAXIMO_BYTES = 10 * 1024 * 1024;
	/** Columna plantilla_nombre_archivo: VARCHAR(200). */
	public static final int LONGITUD_MAXIMA_NOMBRE = 200;

	private static final String MENSAJE_FORMATO = "La plantilla debe ser un archivo PDF, DOCX, JPG o PNG.";

	private static final byte[] FIRMA_PDF = {'%', 'P', 'D', 'F'};
	private static final byte[] FIRMA_DOCX = {'P', 'K', 3, 4};
	private static final byte[] FIRMA_JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G'};

	/** Una plantilla ya validada, lista para guardar. */
	public record Validada(String nombreArchivo, String tipoMime, byte[] contenido) {
	}

	private PlantillaDocumento() {
	}

	public static Validada validar(String nombreArchivo, byte[] contenido) {
		if (contenido == null || contenido.length == 0) {
			throw new IllegalArgumentException("Selecciona un archivo para la plantilla.");
		}
		if (contenido.length > TAMANO_MAXIMO_BYTES) {
			throw new IllegalArgumentException("La plantilla no puede pesar más de 10 MB.");
		}
		String nombre = soloElNombre(nombreArchivo);
		if (nombre.isEmpty()) {
			throw new IllegalArgumentException("El archivo de la plantilla no tiene nombre.");
		}
		if (nombre.length() > LONGITUD_MAXIMA_NOMBRE) {
			throw new IllegalArgumentException(
					"El nombre del archivo no puede pasar de " + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
		}
		if (!nombre.contains(".")) {
			throw new IllegalArgumentException(MENSAJE_FORMATO);
		}
		String extension = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		return switch (extension) {
			case "pdf" -> new Validada(nombre, "application/pdf", exigirFirma(contenido, FIRMA_PDF, "PDF"));
			case "docx" -> new Validada(nombre,
					"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
					exigirFirma(contenido, FIRMA_DOCX, "DOCX"));
			case "jpg", "jpeg" -> new Validada(nombre, "image/jpeg", exigirFirma(contenido, FIRMA_JPG, "JPG"));
			case "png" -> new Validada(nombre, "image/png", exigirFirma(contenido, FIRMA_PNG, "PNG"));
			default -> throw new IllegalArgumentException(MENSAJE_FORMATO);
		};
	}

	/** Algunos navegadores envían la ruta completa; se queda con el nombre del archivo. */
	private static String soloElNombre(String nombreArchivo) {
		if (nombreArchivo == null) {
			return "";
		}
		String limpio = nombreArchivo.strip();
		int corte = Math.max(limpio.lastIndexOf('/'), limpio.lastIndexOf('\\'));
		return limpio.substring(corte + 1).strip();
	}

	private static byte[] exigirFirma(byte[] contenido, byte[] firma, String tipo) {
		if (contenido.length < firma.length) {
			throw contenidoNoCoincide(tipo);
		}
		for (int i = 0; i < firma.length; i++) {
			if (contenido[i] != firma[i]) {
				throw contenidoNoCoincide(tipo);
			}
		}
		return contenido;
	}

	private static IllegalArgumentException contenidoNoCoincide(String tipo) {
		return new IllegalArgumentException("El contenido del archivo no corresponde a un " + tipo + " válido.");
	}
}
