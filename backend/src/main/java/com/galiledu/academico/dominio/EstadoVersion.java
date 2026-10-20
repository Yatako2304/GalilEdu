package com.galiledu.academico.dominio;

/**
 * Estado de una versión de la tabla de conversión respecto de hoy. No se guarda: se calcula con la fecha de vigencia
 * ("Si eliges una fecha futura, la versión quedará programada").
 */
public enum EstadoVersion {
	PROGRAMADA, VIGENTE, ANTERIOR
}
