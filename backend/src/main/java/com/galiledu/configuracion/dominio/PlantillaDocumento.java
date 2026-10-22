package com.galiledu.configuracion.dominio;

import java.util.Locale;

/**
 * Plantilla opcional de un documento requerido (pantalla 13: "XLSX, XLS o CSV, máx. 10 MB"). La BD no restringe el
 * tipo ni el tamaño, así que se valida aquí: la extensión y que el contenido sea de ese tipo. El tipo que declara el
 * navegador no se usa (para un CSV suele llegar como application/octet-stream); se guarda el tipo que corresponde a la
 * extensión.
 *
 * <p>
 * Las reglas lanzan {@link IllegalArgumentException} (400).
 */
public final class PlantillaDocumento {
	public static final int TAMANO_MAXIMO_BYTES = 10 * 1024 * 1024;
	/** Columna plantilla_nombre_archivo: VARCHAR(200). */
	public static final int LONGITUD_MAXIMA_NOMBRE = 200;

	private static final byte[] FIRMA_XLSX = {'P', 'K', 3, 4};
	private static final byte[] FIRMA_XLS = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1,
			0x1A, (byte) 0xE1};

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
		String extension = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		boolean tieneExtension = nombre.contains(".");
		if (!tieneExtension) {
			throw new IllegalArgumentException("La plantilla debe ser un archivo XLSX, XLS o CSV.");
		}
		return switch (extension) {
			case "xlsx" -> new Validada(nombre,
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					exigirFirma(contenido, FIRMA_XLSX, "XLSX"));
			case "xls" -> new Validada(nombre, "application/vnd.ms-excel", exigirFirma(contenido, FIRMA_XLS, "XLS"));
			case "csv" -> new Validada(nombre, "text/csv", exigirTexto(contenido));
			default -> throw new IllegalArgumentException("La plantilla debe ser un archivo XLSX, XLS o CSV.");
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

	/** Un CSV es texto: no puede traer bytes nulos. */
	private static byte[] exigirTexto(byte[] contenido) {
		for (byte b : contenido) {
			if (b == 0) {
				throw contenidoNoCoincide("CSV");
			}
		}
		return contenido;
	}

	private static IllegalArgumentException contenidoNoCoincide(String tipo) {
		return new IllegalArgumentException("El contenido del archivo no corresponde a un " + tipo + " válido.");
	}
}
