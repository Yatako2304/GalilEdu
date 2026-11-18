package com.galiledu.matricula.aplicacion;

/**
 * Por qué se mueve a un estudiante matriculado de sección (RF-56, HU-26). El historial guarda la etiqueta legible en
 * {@code matricula.cambio_seccion.motivo}, seguida de la observación si la hay.
 */
public enum MotivoCambioSeccion {
	REORGANIZACION("Reorganización por cambio de año escolar"), SOLICITUD_APODERADO("Solicitud del apoderado");

	private final String etiqueta;

	MotivoCambioSeccion(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String etiqueta() {
		return etiqueta;
	}

	/** Texto que se guarda: "Solicitud del apoderado" o "Solicitud del apoderado: la familia lo pidió por cercanía". */
	public String texto(String observacion) {
		return observacion == null || observacion.isBlank() ? etiqueta : etiqueta + ": " + observacion.strip();
	}

	/** La observación de un texto guardado con {@link #texto(String)}; null si no tenía. */
	public static String observacionDe(String guardado) {
		if (guardado == null) {
			return null;
		}
		for (MotivoCambioSeccion motivo : values()) {
			String prefijo = motivo.etiqueta + ": ";
			if (guardado.startsWith(prefijo)) {
				return guardado.substring(prefijo.length());
			}
		}
		return null;
	}

	/** La etiqueta del motivo de un texto guardado; los registros anteriores sin motivo devuelven null. */
	public static String etiquetaDe(String guardado) {
		if (guardado == null) {
			return null;
		}
		for (MotivoCambioSeccion motivo : values()) {
			if (guardado.equals(motivo.etiqueta) || guardado.startsWith(motivo.etiqueta + ": ")) {
				return motivo.etiqueta;
			}
		}
		return guardado;
	}
}
