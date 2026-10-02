package com.galiledu.configuracion.dominio;

public record DatosAreaCurricular(String nombre, String descripcion) {
	public DatosAreaCurricular {
		nombre = obligatorio(nombre, 100, "El nombre del área curricular es obligatorio");
		descripcion = obligatorio(descripcion, 255, "La descripción del área curricular es obligatoria");
	}

	static String obligatorio(String valor, int longitudMaxima, String mensaje) {
		if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensaje);
		String limpio = valor.strip();
		if (limpio.length() > longitudMaxima) throw new IllegalArgumentException("El texto excede la longitud permitida");
		return limpio;
	}
}
