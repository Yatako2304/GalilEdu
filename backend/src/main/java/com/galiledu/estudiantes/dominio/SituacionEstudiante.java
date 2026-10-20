package com.galiledu.estudiantes.dominio;

/**
 * Lo que muestra el "Listado de estudiantes" de Figma: inscrito, matriculado en su año académico o retirado. Se
 * calcula a partir del estado del estudiante y de su matrícula (ver {@link Estudiante#getSituacion()}).
 */
public enum SituacionEstudiante {
	INSCRITO, MATRICULADO, RETIRADO
}
