package com.galiledu.configuracion.dominio;

/**
 * Grado del catálogo fijo configuracion.grado (V4): 1.° a 6.° de primaria y 1.° a 5.° de secundaria. No se crea ni se
 * edita; se lee para armar la pantalla de secciones.
 */
public record Grado(String codigo, String nivel, int numero, String nombre, int orden) {
	/** Cómo se rotula una sección de este grado en Figma: "1.° A". */
	public String etiquetaDe(String nombreSeccion) {
		return numero + ".° " + nombreSeccion;
	}
}
