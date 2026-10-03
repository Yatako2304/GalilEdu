package com.galiledu.academica.dominio;

/** Archivo que respalda una calificación; el almacenamiento físico corresponde a otro adaptador. */
public record EvidenciaPedagogica(String nombreArchivo, String archivoUrl, String mimeType, Long tamanioBytes) {
	public EvidenciaPedagogica {
		nombreArchivo = texto(nombreArchivo, 150, "nombreArchivo", true);
		archivoUrl = texto(archivoUrl, 255, "archivoUrl", true);
		mimeType = texto(mimeType, 100, "mimeType", false);
		if (tamanioBytes != null && tamanioBytes <= 0) {
			throw new IllegalArgumentException("El tamaño del archivo debe ser positivo");
		}
	}

	private static String texto(String valor, int maximo, String campo, boolean obligatorio) {
		if (valor == null || valor.isBlank()) {
			if (obligatorio) throw new IllegalArgumentException(campo + " es obligatorio");
			return null;
		}
		String limpio = valor.strip();
		if (limpio.length() > maximo) {
			throw new IllegalArgumentException(campo + " admite hasta " + maximo + " caracteres");
		}
		return limpio;
	}
}
